package org.bimserver.demoplugins.service;

/******************************************************************************
 * Copyright (C) 2009-2019  BIMserver.org
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see {@literal<http://www.gnu.org/licenses/>}.
 *****************************************************************************/

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.bimserver.emf.IdEObject;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.emf.IfcModelInterfaceException;
import org.bimserver.emf.MetaDataManager;
import org.bimserver.emf.OidProvider;
import org.bimserver.ifc.Scaler;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.interfaces.objects.SRevision;
import org.bimserver.interfaces.objects.SService;
import org.bimserver.models.ifc2x3tc1.Ifc2x3tc1Package;
import org.bimserver.models.ifc2x3tc1.IfcArbitraryClosedProfileDef;
import org.bimserver.models.ifc2x3tc1.IfcAxis2Placement3D;
import org.bimserver.models.ifc2x3tc1.IfcBuildingStorey;
import org.bimserver.models.ifc2x3tc1.IfcCartesianPoint;
import org.bimserver.models.ifc2x3tc1.IfcCurve;
import org.bimserver.models.ifc2x3tc1.IfcExtrudedAreaSolid;
import org.bimserver.models.ifc2x3tc1.IfcFurnishingElement;
import org.bimserver.models.ifc2x3tc1.IfcLocalPlacement;
import org.bimserver.models.ifc2x3tc1.IfcObjectDefinition;
import org.bimserver.models.ifc2x3tc1.IfcOwnerHistory;
import org.bimserver.models.ifc2x3tc1.IfcPolyline;
import org.bimserver.models.ifc2x3tc1.IfcProductDefinitionShape;
import org.bimserver.models.ifc2x3tc1.IfcProfileDef;
import org.bimserver.models.ifc2x3tc1.IfcRelContainedInSpatialStructure;
import org.bimserver.models.ifc2x3tc1.IfcRelDecomposes;
import org.bimserver.models.ifc2x3tc1.IfcRelDefines;
import org.bimserver.models.ifc2x3tc1.IfcRepresentation;
import org.bimserver.models.ifc2x3tc1.IfcRepresentationItem;
import org.bimserver.models.ifc2x3tc1.IfcShapeRepresentation;
import org.bimserver.models.ifc2x3tc1.IfcSpace;
import org.bimserver.plugins.ModelHelper;
import org.bimserver.plugins.deserializers.Deserializer;
import org.bimserver.plugins.deserializers.DeserializerPlugin;
import org.bimserver.plugins.services.AbstractModifyRevisionService;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.shared.GuidCompressor;
import org.bimserver.utils.CollectionUtils;
import org.bimserver.utils.DeserializerUtils;
import org.bimserver.utils.IfcUtils;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FurniturePlacerServicePlugin extends AbstractModifyRevisionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(FurniturePlacerServicePlugin.class);

	public FurniturePlacerServicePlugin() {
		super();
	}

	@Override
	public void newRevision(RunningService runningService, BimServerClientInterface bimServerClientInterface, long poid, long roid, String userToken, long soid, SObjectType settings) throws Exception {
		SRevision revision = bimServerClientInterface.getServiceInterface().getRevision(roid);
		if (revision.getComment().equals("Added furniture")) {
			LOGGER.info("Skipping new revision because seems to be generated by Furniture Placer");
			return;
		}

		runningService.updateProgress(0);
		
		SService service = bimServerClientInterface.getServiceInterface().getService(soid);
		
		SProject project = bimServerClientInterface.getServiceInterface().getProjectByPoid(poid);
		final IfcModelInterface model = bimServerClientInterface.getModel(project, roid, true, true);
		
		DeserializerPlugin deserializerPlugin = getPluginContext().getDeserializerPlugin("org.bimserver.ifc.step.deserializer.Ifc2x3tc1StepDeserializerPlugin", true);
		
		Deserializer deserializer = deserializerPlugin.createDeserializer(null);
		deserializer.init(model.getPackageMetaData());
		Path pickNickTableFile = getPluginContext().getRootPath().resolve("data").resolve("picknicktable.ifc");
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try (InputStream resourceAsInputStream = Files.newInputStream(pickNickTableFile)) {
			IOUtils.copy(resourceAsInputStream, byteArrayOutputStream);
		}
		
		IfcModelInterface furnishingModel = DeserializerUtils.readFromBytes(deserializer, byteArrayOutputStream.toByteArray(), "picknicktable.ifc");

		float lengthUnitPrefix = IfcUtils.getLengthUnitPrefix(model);
		
		LOGGER.info("Length scalar: " + lengthUnitPrefix);
		
		IfcFurnishingElement picknick = (IfcFurnishingElement) furnishingModel.getByName(Ifc2x3tc1Package.eINSTANCE.getIfcFurnishingElement(), "Picknik Bank");
		
		if (lengthUnitPrefix != 1) {
			// Picknick model is in meter, target model in millis, so we have to convert the picknick model first
			Scaler scaler = new Scaler(furnishingModel);
			scaler.scale(lengthUnitPrefix);
		}

		MetaDataManager metaDataManager = getPluginContext().getMetaDataManager();
		ModelHelper modelHelper = new ModelHelper(metaDataManager, model);

		modelHelper.setTargetModel(model);
		modelHelper.setObjectFactory(model);
		OidProvider oidProvider = new OidProvider() {
			@Override
			public long newOid(EClass eClass) {
				try {
					IdEObject object = model.create(eClass);
					return object.getOid();
				} catch (IfcModelInterfaceException e) {
					e.printStackTrace();
				}
				return -1;
			}
		};
		modelHelper.setOidProvider(oidProvider);
		
		IfcProductDefinitionShape representation = (IfcProductDefinitionShape) picknick.getRepresentation();
		IfcRepresentation surfaceModel = null;
		IfcRepresentation boundingBox = null;
		for (IfcRepresentation ifcRepresentation : representation.getRepresentations()) {
			IfcShapeRepresentation ifcShapeRepresentation = (IfcShapeRepresentation)ifcRepresentation;
			if (ifcShapeRepresentation.getRepresentationType().equals("SurfaceModel")) {
				surfaceModel = (IfcRepresentation) modelHelper.copy(ifcShapeRepresentation, true);
			} else if (ifcShapeRepresentation.getRepresentationType().equals("BoundingBox")) {
				boundingBox	= (IfcRepresentation) modelHelper.copy(ifcShapeRepresentation, true);
			}
		}
		
		List<IfcRelDefines> newDefines = new ArrayList<>();
		for (IfcRelDefines ifcRelDefines : picknick.getIsDefinedBy()) {
			newDefines.add((IfcRelDefines) modelHelper.copy(ifcRelDefines, true));
		}

		IfcOwnerHistory ownerHistory = null;
		List<IfcOwnerHistory> all = model.getAll(IfcOwnerHistory.class);
		if (all.size() > 0) {
			 ownerHistory = all.get(0);
		}
		int newFurniture = 0;
		double spaceWidth = -1;
		double spaceDepth = -1;
		for (IfcBuildingStorey ifcBuildingStorey : model.getAll(IfcBuildingStorey.class)) {
			for (IfcRelDecomposes ifcRelDecomposes : ifcBuildingStorey.getIsDecomposedBy()) {
				for (IfcObjectDefinition ifcObjectDefinition : ifcRelDecomposes.getRelatedObjects()) {
					if (ifcObjectDefinition instanceof IfcSpace) {
						IfcSpace ifcSpace = (IfcSpace)ifcObjectDefinition;
						
						EList<IfcRepresentation> representations = ifcSpace.getRepresentation().getRepresentations();
						for (IfcRepresentation ifcRepresentation : representations) {
							for (IfcRepresentationItem ifcRepresentationItem : ifcRepresentation.getItems()) {
								if (ifcRepresentationItem instanceof IfcExtrudedAreaSolid) {
									IfcProfileDef sweptArea = ((IfcExtrudedAreaSolid) ifcRepresentationItem).getSweptArea();
									if (sweptArea instanceof IfcArbitraryClosedProfileDef) {
										IfcCurve outerCurve = ((IfcArbitraryClosedProfileDef) sweptArea).getOuterCurve();
										if (outerCurve instanceof IfcPolyline) {
											double[] min = new double[]{Double.MAX_VALUE, Double.MAX_VALUE};
											double[] max = new double[]{-Double.MAX_VALUE, -Double.MAX_VALUE};
											EList<IfcCartesianPoint> points = ((IfcPolyline) outerCurve).getPoints();
											for (IfcCartesianPoint ifcCartesianPoint : points) {
												Double x = ifcCartesianPoint.getCoordinates().get(0);
												Double y = ifcCartesianPoint.getCoordinates().get(1);
												if (x > max[0]) {
													max[0] = x;
												}
												if (x < min[0]) {
													min[0] = x;
												}
												if (y > max[1]) {
													max[1] = y;
												}
												if (y < min[1]) {
													min[1] = y;
												}
											}
											spaceWidth = max[0] - min[0];
											spaceDepth = max[1] - max[1];
										}
									}
								}
							}
						}
						
						IfcFurnishingElement newFurnishing = model.create(IfcFurnishingElement.class);
						newFurnishing.setName("ADDED FURNITURE");
						
						newFurnishing.setGlobalId(GuidCompressor.getNewIfcGloballyUniqueId());
						newFurnishing.setOwnerHistory(ownerHistory);
						IfcProductDefinitionShape definitionShape = model.create(IfcProductDefinitionShape.class);
						newFurnishing.setRepresentation(definitionShape);
						
						definitionShape.getRepresentations().add(boundingBox);
						definitionShape.getRepresentations().add(surfaceModel);
						
						for (IfcRelDefines ifcRelDefines : newDefines) {
							newFurnishing.getIsDefinedBy().add(ifcRelDefines);
						}
						
						IfcLocalPlacement localPlacement = model.create(IfcLocalPlacement.class);
						localPlacement.setPlacementRelTo(ifcSpace.getObjectPlacement());
						IfcAxis2Placement3D axis2Placement3D = model.create(IfcAxis2Placement3D.class);
						localPlacement.setRelativePlacement(axis2Placement3D);
						
						IfcCartesianPoint pos = model.create(IfcCartesianPoint.class);
						if (spaceWidth == -1) {
							pos.getCoordinates().add(0d);
							pos.getCoordinates().add(0d);
							pos.getCoordinates().add(0d);
						} else {
							pos.getCoordinates().add(-2 + -spaceWidth / 2);
							pos.getCoordinates().add(1 + -spaceDepth / 2);
							pos.getCoordinates().add(0d);
						}
						axis2Placement3D.setLocation(pos);
						
						if (ifcSpace.getContainsElements().size() > 0) {
							IfcRelContainedInSpatialStructure rel = ifcSpace.getContainsElements().get(0);
							rel.getRelatedElements().add(newFurnishing);
						} else {
							IfcRelContainedInSpatialStructure decomposes = model.create(IfcRelContainedInSpatialStructure.class);
							decomposes.setGlobalId(GuidCompressor.getNewIfcGloballyUniqueId());
							decomposes.setOwnerHistory(ownerHistory);
							decomposes.getRelatedElements().add(newFurnishing);
							decomposes.setRelatingStructure(ifcSpace);
						}
						
						newFurnishing.setObjectPlacement(localPlacement);
						
						newFurniture++;
					}
				}
			}
		}
		LOGGER.info("New furniture: " + newFurniture);

		runningService.updateProgress(100);
		
		if (service.getWriteRevisionId() != -1 && service.getWriteRevisionId() != project.getOid()) {
			model.checkin(service.getWriteRevisionId(), "Added furniture");
		} else {
			model.commit("Added furniture");
		}		
	}

	@Override
	public ProgressType getProgressType() {
		return ProgressType.UNKNOWN;
	}
}