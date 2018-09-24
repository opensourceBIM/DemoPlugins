package org.bimserver.demoplugins.outerarea;

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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.geometry.Vector;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.models.geometry.GeometryData;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.models.ifc2x3tc1.IfcSpace;
import org.bimserver.models.ifc2x3tc1.IfcWallStandardCase;
import org.bimserver.models.ifc2x3tc1.Tristate;
import org.bimserver.plugins.services.AbstractAddExtendedDataService;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.utils.IfcUtils;

import com.google.common.base.Charsets;

public class OuterAreaServicePlugin extends AbstractAddExtendedDataService {

	public OuterAreaServicePlugin() {
		super("outerareanamespace");
	}

	@Override
	public void newRevision(RunningService runningService, BimServerClientInterface bimServerClientInterface, long poid,
			long roid, String userToken, long soid, SObjectType settings) throws Exception {
		
		SProject project = bimServerClientInterface.getServiceInterface().getProjectByPoid(poid);
		IfcModelInterface model = bimServerClientInterface.getModel(project, roid, true, false, true);
		
		int nrTriangles = 0;
		int nrVertices = 0;
		int nrUsedProducts = 0;
		float totalArea = 0f;
		
		Map<String, AtomicInteger> map = new HashMap<>();

		float totalSpaceM3 = 0;
		
		for (IfcSpace ifcSpace : model.getAll(IfcSpace.class)) {
			Double volume = IfcUtils.getIfcQuantityVolume(ifcSpace, "Net Volume");
			if (volume != null) {
				totalSpaceM3 += volume;
			}
		}
		
		List<IfcProduct> allWithSubTypes = model.getAllWithSubTypes(IfcProduct.class);
		for (IfcProduct ifcProduct : allWithSubTypes) {
			if (!map.containsKey(ifcProduct.eClass().getName())) {
				map.put(ifcProduct.eClass().getName(), new AtomicInteger(0));
			}
			if (!(ifcProduct instanceof IfcWallStandardCase)) {
				continue;
			}
			
			
			Tristate isExternal = IfcUtils.getBooleanProperty(ifcProduct, "IsExternal");
			if (isExternal == Tristate.TRUE) {
				GeometryInfo geometryInfo = ifcProduct.getGeometry();
				if (geometryInfo != null) {
					GeometryData geometryData = geometryInfo.getData();
					if (geometryData != null) {
						map.get(ifcProduct.eClass().getName()).incrementAndGet();
						nrUsedProducts++;
						
						ByteBuffer indicesBuffer = ByteBuffer.wrap(geometryData.getIndices().getData());
						indicesBuffer.order(ByteOrder.LITTLE_ENDIAN);
						IntBuffer indices = indicesBuffer.asIntBuffer();

						ByteBuffer verticesBuffer = ByteBuffer.wrap(geometryData.getVertices().getData());
						verticesBuffer.order(ByteOrder.LITTLE_ENDIAN);
						FloatBuffer vertices = verticesBuffer.asFloatBuffer();

						nrTriangles += (indices.capacity() / 3);
						nrVertices += (vertices.capacity() / 3);

						float objectAream2 = 0;
						
						for (int i=0; i<indices.capacity(); i+=3) {
							float[][] triangle = new float[3][3];
							for (int x=0; x<3; x++) {
								triangle[x] = new float[]{
									vertices.get(indices.get(i + x) * 3),
									vertices.get(indices.get(i + x) * 3 + 1),
									vertices.get(indices.get(i + x) * 3 + 2)
								};
							}
							
							// Dividing by 2 is a very rough approximation, ignoring the width of objects
							float areaTriangle = Vector.getArea(triangle) / 2;
							objectAream2 += areaTriangle / 1000000f;
						}
						totalArea += objectAream2;
					}
				}
			}
		}
		
		StringBuilder results = new StringBuilder();
		results.append("All IfcProduct subtypes (excluding IfcRailing) which are external\n");
		for (String type : map.keySet()) {
			int count = map.get(type).get();
			if (count > 0) {
				results.append("\t" + type + ": " + count + "\n");
			}
		}
		results.append("Total nr products: " + nrUsedProducts + "\n");
		results.append("Nr triangles: " + nrTriangles + "\n");
		results.append("Nr vertices: " + nrVertices + "\n");
		results.append("Total IfcSpace Net Volumes: " + totalSpaceM3 + "m3\n");
		results.append("Total area (rough estimation): " + totalArea + " m2\n");
		
		addExtendedData(results.toString().getBytes(Charsets.UTF_8), "test.txt", "Test", "text/plain", bimServerClientInterface, roid);
	}
}