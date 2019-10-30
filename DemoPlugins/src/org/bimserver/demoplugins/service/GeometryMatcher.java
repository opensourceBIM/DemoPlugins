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
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bimserver.emf.IdEObject;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.geometry.AxisAlignedBoundingBox;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.interfaces.objects.SProjectSmall;
import org.bimserver.models.ifc2x3tc1.IfcElement;
import org.bimserver.models.ifc2x3tc1.IfcLabel;
import org.bimserver.models.ifc2x3tc1.IfcOpeningElement;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.models.ifc2x3tc1.IfcProperty;
import org.bimserver.models.ifc2x3tc1.IfcPropertySet;
import org.bimserver.models.ifc2x3tc1.IfcPropertySetDefinition;
import org.bimserver.models.ifc2x3tc1.IfcPropertySingleValue;
import org.bimserver.models.ifc2x3tc1.IfcRelDefines;
import org.bimserver.models.ifc2x3tc1.IfcRelDefinesByProperties;
import org.bimserver.models.ifc2x3tc1.IfcRelFillsElement;
import org.bimserver.models.ifc2x3tc1.IfcText;
import org.bimserver.models.ifc2x3tc1.IfcValue;
import org.bimserver.models.ifc2x3tc1.IfcWindow;
import org.bimserver.plugins.services.AbstractAddExtendedDataService;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.plugins.services.Geometry;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

import com.opencsv.CSVWriter;

public class GeometryMatcher extends AbstractAddExtendedDataService {
	private static final String NAMESPACE = "geometrymatching";

	public GeometryMatcher() {
		super(NAMESPACE);
	}

	@Override
	public void newRevision(RunningService runningService, BimServerClientInterface bimServerClientInterface, long poid, long roid, String userToken, long soid, SObjectType settings) throws Exception {
		super.newRevision(runningService, bimServerClientInterface, poid, roid, userToken, soid, settings);
		Map<SProjectSmall, Map<AxisAlignedBoundingBox, IfcOpeningElement>> map = new HashMap<>();
		
		Map<Long, String> projectNames = new LinkedHashMap<>();
		
		List<SProjectSmall> allRelatedProjects = bimServerClientInterface.getServiceInterface().getAllRelatedProjects(poid);
		for (SProjectSmall project : allRelatedProjects) {
			if (project.getLastRevisionId() != -1 && project.getNrSubProjects() == 0) {
				projectNames.put(project.getOid(), project.getName());
				IfcModelInterface model = bimServerClientInterface.getModel(null, project.getLastRevisionId(), false, false); // TODO
				Map<AxisAlignedBoundingBox, IfcOpeningElement> boundingBoxes = new HashMap<>();
				map.put(project, boundingBoxes);
				for (IfcOpeningElement ifcOpeningElement : model.getAll(IfcOpeningElement.class)) {
					
					Geometry geometry = bimServerClientInterface.getGeometry(project.getLastRevisionId(), ifcOpeningElement);
					
//					IntBuffer indexBuffer = geometry.getIndices();
//					FloatBuffer vertexBuffer = geometry.getVertices();
//					
//					for (int i=0; i<indexBuffer.capacity(); i+=3) {
//						int index1 = indexBuffer.get(i);
//						int index2 = indexBuffer.get(i + 1);
//						int index3 = indexBuffer.get(i + 2);
//						
//						boundingBox.process(new float[]{vertexBuffer.get(index1), vertexBuffer.get(index1 + 1), vertexBuffer.get(index1 + 2)});
//						boundingBox.process(new float[]{vertexBuffer.get(index2), vertexBuffer.get(index2 + 1), vertexBuffer.get(index2 + 2)});
//						boundingBox.process(new float[]{vertexBuffer.get(index3), vertexBuffer.get(index3 + 1), vertexBuffer.get(index3 + 2)});
//					}
					
					AxisAlignedBoundingBox boundingBox = new AxisAlignedBoundingBox(new float[]{geometry.getMinX(), geometry.getMinY(), geometry.getMinZ()}, new float[]{geometry.getMaxX(), geometry.getMaxY(), geometry.getMaxZ()});
					
					boundingBoxes.put(boundingBox, ifcOpeningElement);
				}
			}
		}
		
		List<Hit> hits = analyze(map);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(byteArrayOutputStream));

		Iterator<String> projectIter = projectNames.values().iterator();
		csvWriter.writeNext(new String[]{"GUID in " + projectIter.next(), "GUID in " + projectIter.next(), "Mark"});
		for (Hit hit : hits) {
			String mark = "NOT MATCHED";
			for (IfcOpeningElement ifcOpeningElement : new IfcOpeningElement[]{hit.el1, hit.el2}) {
				for (IfcRelFillsElement ifcRelFillsElement : ifcOpeningElement.getHasFillings()) {
					IfcElement relatedBuildingElement = ifcRelFillsElement.getRelatedBuildingElement();
					if (relatedBuildingElement instanceof IfcWindow) {
						dump(relatedBuildingElement);
						String m = getStringProperty(relatedBuildingElement, "Mark");
						if (m != null) {
							mark = m;
						}
					}
				}
			}
			System.out.println(mark);
			csvWriter.writeNext(new String[]{hit.el1.getGlobalId(), hit.el2.getGlobalId(), mark});
		}
		csvWriter.close();

		addExtendedData(byteArrayOutputStream.toByteArray(), "geometrymatching.csv", "Geometry Matching Results", "text/csv", bimServerClientInterface, roid);
	}


	private String getStringProperty(IfcProduct ifcProduct, String propertyName) {
		for (IfcRelDefines ifcRelDefines : ifcProduct.getIsDefinedBy()) {
			if (ifcRelDefines instanceof IfcRelDefinesByProperties) {
				IfcRelDefinesByProperties ifcRelDefinesByProperties = (IfcRelDefinesByProperties)ifcRelDefines;
				IfcPropertySetDefinition relatingPropertyDefinition = ifcRelDefinesByProperties.getRelatingPropertyDefinition();
				if (relatingPropertyDefinition instanceof IfcPropertySet) {
					IfcPropertySet ifcProperySet = ((IfcPropertySet)relatingPropertyDefinition);
					for (IfcProperty ifcProperty : ifcProperySet.getHasProperties()) {
						if (propertyName.equals(ifcProperty.getName())) {
							if (ifcProperty instanceof IfcPropertySingleValue) {
								IfcPropertySingleValue ifcPropertySingleValue = (IfcPropertySingleValue)ifcProperty;
								IfcValue nominalValue = ifcPropertySingleValue.getNominalValue();
								if (nominalValue instanceof IfcLabel) {
									IfcLabel ifcLabel = (IfcLabel)nominalValue;
									return ifcLabel.getWrappedValue();
								} else if (nominalValue instanceof IfcText) {
									IfcText ifcText = (IfcText)nominalValue;
									return ifcText.getWrappedValue();
								}
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	@SuppressWarnings("rawtypes")
	private void dump(IdEObject object) {
		System.out.println(object.eClass().getName() + " " + object.getOid());
		for (EStructuralFeature eStructuralFeature : object.eClass().getEAllStructuralFeatures()) {
			System.out.print(eStructuralFeature.getName() + ": ");
			Object val = object.eGet(eStructuralFeature);
			if (eStructuralFeature instanceof EReference) {
				if (eStructuralFeature.isMany()) {
					List list = (List)val;
					for (Object o : list) {
						IdEObject ref = (IdEObject)o;
						System.out.print(ref.eClass().getName() + " " + ref.getOid() + ", ");
					}
				} else {
					if (val != null) {
						IdEObject ref = ((IdEObject)val);
						System.out.print(ref.eClass().getName() + " " + ref.getOid());
					}
				}
			} else {
				if (eStructuralFeature.isMany()) {
					System.out.print("values...");
				} else {
					System.out.print(val);
				}
			}
			System.out.println();
		}
	}

	private List<Hit> analyze(Map<SProjectSmall, Map<AxisAlignedBoundingBox, IfcOpeningElement>> map) {
		Set<IfcOpeningElement> matched = new HashSet<>();
		List<Hit> hits = new ArrayList<>();
		for (SProjectSmall project : map.keySet()) {
			Map<AxisAlignedBoundingBox, IfcOpeningElement> m = map.get(project);
			for (AxisAlignedBoundingBox b : m.keySet()) {
				for (SProjectSmall project2 : map.keySet()) {
					if (project2.getOid() != project.getOid()) {
						Map<AxisAlignedBoundingBox, IfcOpeningElement> m2 = map.get(project2);
						for (AxisAlignedBoundingBox b2 : m2.keySet()) {
							if (b.closeTo(b2)) {
								if (!matched.contains(m.get(b)) && !matched.contains(m2.get(b2))) {
									hits.add(new Hit(project, project2, b, b2, m.get(b), m2.get(b2)));
									matched.add(m.get(b));
									matched.add(m2.get(b2));
								}
							}
						}
					}
				}
			}
		}
		return hits;
	}
	
	public static class Hit {
		public IfcOpeningElement el1;
		public IfcOpeningElement el2;
		public AxisAlignedBoundingBox b1;
		public AxisAlignedBoundingBox b2;
		public SProjectSmall secondProject;
		public SProjectSmall firstProject;

		public Hit(SProjectSmall firstProject, SProjectSmall secondProject, AxisAlignedBoundingBox b1, AxisAlignedBoundingBox b2, IfcOpeningElement el1, IfcOpeningElement el2) {
			this.firstProject = firstProject;
			this.secondProject = secondProject;
			this.b1 = b1;
			this.b2 = b2;
			this.el1 = el1;
			this.el2 = el2;
		}
	}
}