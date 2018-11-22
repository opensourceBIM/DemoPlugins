package org.bimserver.demoplugins.geometrysimplifier;

/******************************************************************************
 * Copyright (C) 2009-2018  BIMserver.org
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.bimserver.bimbots.BimBotContext;
import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.models.geometry.GeometryData;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcCartesianPoint;
import org.bimserver.models.ifc2x3tc1.IfcClosedShell;
import org.bimserver.models.ifc2x3tc1.IfcFace;
import org.bimserver.models.ifc2x3tc1.IfcFaceBound;
import org.bimserver.models.ifc2x3tc1.IfcFaceOuterBound;
import org.bimserver.models.ifc2x3tc1.IfcLoop;
import org.bimserver.models.ifc2x3tc1.IfcMappedItem;
import org.bimserver.models.ifc2x3tc1.IfcPolyLoop;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.models.ifc2x3tc1.IfcProductRepresentation;
import org.bimserver.models.ifc2x3tc1.IfcRepresentation;
import org.bimserver.models.ifc2x3tc1.IfcRepresentationItem;
import org.bimserver.models.ifc2x3tc1.IfcRepresentationMap;
import org.bimserver.models.ifc2x3tc1.IfcShell;
import org.bimserver.models.ifc2x3tc1.IfcShellBasedSurfaceModel;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.SchemaName;
import org.bimserver.plugins.services.BimBotAbstractService;
import org.eclipse.emf.common.util.EList;

import com.google.common.base.Charsets;

public class GeometrySimplifier extends BimBotAbstractService  {

	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, BimBotContext bimBotContext, PluginConfiguration pluginConfiguration) throws BimBotsException {
		IfcModelInterface model = input.getIfcModel();

		int nrProducts = 0;
		int nrTriangles = 0;

		Map<Integer, Set<IfcProduct>> map = new TreeMap<>();
		Map<Long, Set<GeometryInfo>> dataToInfo = new HashMap<>();

		for (IfcProduct product : model.getAllWithSubTypes(IfcProduct.class)) {
			GeometryInfo geometry = product.getGeometry();
			if (geometry != null) {
				Set<IfcProduct> set = map.get(geometry.getPrimitiveCount());
				if (set == null) {
					set = new HashSet<>();
					map.put(geometry.getPrimitiveCount(), set);
				}
				set.add(product);
				nrProducts++;
				nrTriangles += geometry.getPrimitiveCount();
				
				
				GeometryData geometryData = geometry.getData();
				if (geometryData != null) {
					Set<GeometryInfo> infoSet = dataToInfo.get(geometryData.getOid());
					if (infoSet == null) {
						infoSet = new HashSet<>();
						dataToInfo.put(geometryData.getOid(), infoSet);
					}
					if (infoSet != null) {
						infoSet.add(geometry);
					}
				}
			}
		}
		
		StringBuilder sb = new StringBuilder();
		for (IfcProduct product : model.getAllWithSubTypes(IfcProduct.class)) {
			GeometryInfo geometry = product.getGeometry();
			if (geometry != null) {
				GeometryData geometryData = geometry.getData();
				sb.append(product.getGlobalId() + " " + geometry.getPrimitiveCount() + "\n");
				
				int faces = 0;
				double[] min = null;
				IfcProductRepresentation representation = product.getRepresentation();
				for (IfcRepresentation ifcRepresentation : representation.getRepresentations()) {
					for (IfcRepresentationItem ifcRepresentationItem : ifcRepresentation.getItems()) {
						if (ifcRepresentationItem instanceof IfcMappedItem) {
							IfcRepresentationMap mappingSource = ((IfcMappedItem) ifcRepresentationItem).getMappingSource();
							IfcRepresentation mappedRepresentation = mappingSource.getMappedRepresentation();
							EList<IfcRepresentationItem> items = mappedRepresentation.getItems();
							for (IfcRepresentationItem ifcRepresentationItem2 : items) {
								if (ifcRepresentationItem2 instanceof IfcShellBasedSurfaceModel) {
									EList<IfcShell> sbsmBoundary = ((IfcShellBasedSurfaceModel) ifcRepresentationItem2).getSbsmBoundary();
									for (IfcShell ifcShell : sbsmBoundary) {
										if (ifcShell instanceof IfcClosedShell) {
											IfcClosedShell ifcClosedShell = (IfcClosedShell)ifcShell;
											EList<IfcFace> cfsFaces = ifcClosedShell.getCfsFaces();
											faces += cfsFaces.size();
											for (IfcFace ifcFace : cfsFaces) {
												for (IfcFaceBound ifcFaceBound : ifcFace.getBounds()) {
													if (ifcFaceBound instanceof IfcFaceOuterBound) {
														IfcLoop bound = ifcFaceBound.getBound();
														if (bound instanceof IfcPolyLoop) {
															for (IfcCartesianPoint ifcCartesianPoint : ((IfcPolyLoop) bound).getPolygon()) {
																EList<Double> coords = ifcCartesianPoint.getCoordinates();
																if (min == null || (coords.get(0) < min[0] && coords.get(1) < min[1] && coords.get(2) < min[2])) {
																	min = new double[]{coords.get(0), coords.get(1), coords.get(2)};
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
					if (min != null) {
//						sb.append("Min: " + min[0] + "," + min[1] + "," + min[2] + "\n");
					}
				}
//				sb.append("Faces: " + faces + "\n");
				if (geometryData != null) {
					sb.append("Reuse: " + dataToInfo.get(geometryData.getOid()).size() + "\n");
				}
			}
		}

		for (Integer key : map.keySet()) {
			Set<IfcProduct> set = map.get(key);
			sb.append(key + ": " + set.size() + "\n");
		}

		sb.append("\n");
		sb.append("" + nrProducts + " products\n");
		sb.append("" + nrTriangles + " triangles\n");
		sb.append("" + (nrTriangles / nrProducts) + " triangles per product on average\n");
		
		BimBotsOutput output = new BimBotsOutput(SchemaName.UNSTRUCTURED_UTF8_TEXT_1_0, sb.toString().getBytes(Charsets.UTF_8));
		output.setContentType("text/plain");
		return output;
	}
	
	public String getOutputSchema() {
		return SchemaName.UNSTRUCTURED_UTF8_TEXT_1_0.name();
	}
}