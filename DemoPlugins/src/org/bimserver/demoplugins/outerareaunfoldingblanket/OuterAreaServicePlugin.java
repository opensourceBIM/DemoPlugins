package org.bimserver.demoplugins.outerareaunfoldingblanket;

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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.geometry.Matrix;
import org.bimserver.geometry.Vector;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.models.geometry.GeometryData;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.models.ifc2x3tc1.IfcSpace;
import org.bimserver.plugins.services.AbstractAddExtendedDataService;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.utils.IfcUtils;

import com.google.common.base.Charsets;

public class OuterAreaServicePlugin extends AbstractAddExtendedDataService {

	public OuterAreaServicePlugin() {
		super("outerareanamespace");
	}

	public class Corner3D implements Comparable<Corner3D>
	{
		public float x;
		public float y;
		public float z;
		
		public Corner3D(float _x, float _y, float _z)
		{
			x = _x;
			y = _y;
			z = _z;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + Float.floatToIntBits(x);
			result = prime * result + Float.floatToIntBits(y);
			result = prime * result + Float.floatToIntBits(z);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Corner3D other = (Corner3D) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (Float.floatToIntBits(x) != Float.floatToIntBits(other.x))
				return false;
			if (Float.floatToIntBits(y) != Float.floatToIntBits(other.y))
				return false;
			if (Float.floatToIntBits(z) != Float.floatToIntBits(other.z))
				return false;
			return true;
		}

		private OuterAreaServicePlugin getOuterType() {
			return OuterAreaServicePlugin.this;
		}		

		@Override
		public int compareTo(Corner3D o) 
		{	
			if (x == o.x)
			{
				if (y == o.y)
				{
					if (z == o.z)
						return 0;
					else
						return (z < o.z) ? -1: 1;
				}
				else
					return (y < o.y) ? -1: 1;
			}
			else 
				return (x < o.x) ? -1: 1;
		}	
		
		public boolean lessThan(Corner3D o)
		{
			return compareTo(o) < 0;
		}
		
		public boolean moreThan(Corner3D o)
		{
			return compareTo(o) > 0;
		}
	
	}
	
	public class Edge3D implements Comparable<Edge3D>
	{
		public Corner3D[] corner = new Corner3D[2];
		private Float length = null;
		
		public Edge3D(Corner3D _p1, Corner3D _p2) // 2 points indices in array 
		{
			corner[0] = (_p1.lessThan(_p2) ? _p1 : _p2);
			corner[1] = (_p1.lessThan(_p2) ? _p2 : _p1);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + corner[0].hashCode();
			result = prime * result + corner[1].hashCode();
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Edge3D other = (Edge3D) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if ((corner[0] != other.corner[0] || corner[1] != other.corner[1]))
				return false;

			return true;
		}
		
		private OuterAreaServicePlugin getOuterType() {
			return OuterAreaServicePlugin.this;
		}

		public float getLength()
		{
			if (length == null)
				length = new Float((float) Math.sqrt((corner[1].x - corner[0].x) * (corner[1].x - corner[0].x) + 
						                             (corner[1].y - corner[0].y) * (corner[1].y - corner[0].y) + 
						                             (corner[1].z - corner[0].z) * (corner[1].z - corner[0].z)));
				
			return length; 
		}
		
		@Override
		public int compareTo(Edge3D o) {
			
			if (corner[0] == o.corner[0]) 
				return corner[1].compareTo(o.corner[1]);
			else 
				return corner[0].compareTo(o.corner[0]);
		}	
		
		public boolean lessThan(Edge3D o)
		{
			return compareTo(o) < 0;
		}
		
		public boolean moreThan(Edge3D o)
		{
			return compareTo(o) > 0;
		}
		
	}
	
	
	public class Triangle3D
	{
		public Edge3D[] edge = new Edge3D[3];
		public Corner3D normal = null;
		public float area = 0.0f;
		public boolean isUsed = false;
		public boolean flippedNormal = false;
		
		public Triangle3D(Edge3D[] _lines)
		{
			if (_lines[0].lessThan(_lines[1]))
			{
				if (_lines[1].lessThan(_lines[2]))
				{
					edge[0] = _lines[0];
					edge[1] = _lines[1];
					edge[2] = _lines[2];
				}
				else if (_lines[0].lessThan(_lines[2]))
				{
					edge[0] = _lines[0];
					edge[1] = _lines[2];
					edge[2] = _lines[1];
				}
				else
				{
					edge[0] = _lines[2];
					edge[1] = _lines[0];
					edge[2] = _lines[1];
				}
			}
			else
			{
				if (_lines[0].lessThan(_lines[2]))
				{
					edge[0] = _lines[1];
					edge[1] = _lines[0];
					edge[2] = _lines[2];
				}
				else if (_lines[1].lessThan(_lines[2]))
				{
					edge[0] = _lines[1];
					edge[1] = _lines[2];
					edge[2] = _lines[0];
				}
				else
				{
					edge[0] = _lines[2];
					edge[1] = _lines[1];
					edge[2] = _lines[0];
				}				
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + edge[0].hashCode();
			result = prime * result + edge[1].hashCode();
			result = prime * result + edge[2].hashCode();
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Triangle3D other = (Triangle3D) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if ((edge[0] != other.edge[0] || edge[1] != other.edge[1] || edge[2] != other.edge[2]))
				return false;
			return true;
		}
		
/*		
		private Corner3D getCorner(int cornerNr)
		{
			 //binairy mapping of triangle cornerNr to edge and corner combination 0 -> 0, 0;  1 -> 0, 1;  2-> 2, 1			
			return (edge[cornerNr & 2].corner[(cornerNr & 1) | (cornerNr & 2 >> 2)]);
			
			 // trinair mapping of triangle cornerNr to edge and corner combination 0 -> 0, 0;  1 -> 0, 1;  2-> 2, 1
//			return (cornerNr >= 2 ? edge[2].corner[1] : edge[0].corner[cornerNr];		
		}
*/
		private OuterAreaServicePlugin getOuterType() {
			return OuterAreaServicePlugin.this;
		}	
	}

	
    private Map<Corner3D, Corner3D> corners = new HashMap<Corner3D, Corner3D>();
    private Map<Corner3D, Map<Edge3D, Edge3D>> edges = new HashMap<Corner3D, Map<Edge3D, Edge3D>>();
    private Map<Edge3D, Set<Triangle3D>> triangles = new HashMap<Edge3D, Set<Triangle3D>>();

    private void CalcTriangle(Triangle3D triangle)
    {
		float[] ab = new float[] {triangle.edge[0].corner[1].x - triangle.edge[0].corner[0].x, 
                				  triangle.edge[0].corner[1].y - triangle.edge[0].corner[0].y,
                				  triangle.edge[0].corner[1].z - triangle.edge[0].corner[0].z};
		float[] ac = new float[] {triangle.edge[1].corner[1].x - triangle.edge[1].corner[0].x, 
						  		  triangle.edge[1].corner[1].y - triangle.edge[1].corner[0].y,
						  		  triangle.edge[1].corner[1].z - triangle.edge[1].corner[0].z};
		
		float[] cross = Vector.crossProduct(ab, ac);
		triangle.area = (float) Math.sqrt(cross[0] * cross[0] + cross[1] * cross[1] + cross[2] * cross[2]);
		triangle.normal = new Corner3D (cross[0] / triangle.area, cross[1] / triangle.area, cross[2] / triangle.area); 
		triangle.area *= 0.5;
    }
    
	private float Unfold(Triangle3D unfoldFromTriangle)
	{
		if (unfoldFromTriangle.isUsed)
			return 0.0f;
		
		unfoldFromTriangle.isUsed = true;

		if (unfoldFromTriangle.normal == null)
			CalcTriangle(unfoldFromTriangle);
				
		// find all folding triangles for each edge (line) of the triangle
		float area = unfoldFromTriangle.area;
		float[] M_tri = new float[16], 
				M_z = new float[16], 
				M_star = new float[16],
				T = new float[16];

		//to transform all triangles to for finding the smallest angle between triangles 
		M_z[0] = -1.0f;
		M_z[1] = 0.0f;
		M_z[2] = 0.0f;
		M_z[3] = 1.0f;
		M_z[4] = 0.0f;
		M_z[5] = 0.0f;
		M_z[6] = 0.0f;
		M_z[7] = 1.0f;
		M_z[8] = 0.0f;
		M_z[9] = -1.0f;
		M_z[10] = 0.0f;
		M_z[11] = 1.0f;
		M_z[12] = 0.0f;
		M_z[13] = 0.0f;
		M_z[14] = -1.0f;
		M_z[15] = 1.0f;		
		
		for (int edgeNr = 0; edgeNr < 3; edgeNr++)
		{
			// find first unwrapping triangle attached to the current line 
			Triangle3D unfoldToTriangle = null;
			float minUnfoldAngle = 0;
			
			int lp = edgeNr & 1;     //index of low point in edge array [0 or 1]
			int hp = (~edgeNr) & 1;  //index of high point in edge array [0 or 1]
			
			// Calculate transformation of unfoldFromTriangle to z plane = 0 and normal towards -z
			M_tri[0] = unfoldFromTriangle.edge[(edgeNr + 1) % 3].corner[hp].x;
			M_tri[1] = unfoldFromTriangle.edge[(edgeNr + 1) % 3].corner[hp].y;
			M_tri[2] = unfoldFromTriangle.edge[(edgeNr + 1) % 3].corner[hp].z;
			M_tri[3] = 1.0f;
			M_tri[4] = unfoldFromTriangle.edge[edgeNr].corner[unfoldFromTriangle.flippedNormal ? hp : lp].x;
			M_tri[5] = unfoldFromTriangle.edge[edgeNr].corner[unfoldFromTriangle.flippedNormal ? hp : lp].y;
			M_tri[6] = unfoldFromTriangle.edge[edgeNr].corner[unfoldFromTriangle.flippedNormal ? hp : lp].z;
			M_tri[7] = 1.0f;
			M_tri[8] = unfoldFromTriangle.edge[edgeNr].corner[unfoldFromTriangle.flippedNormal ? lp : hp].x;
			M_tri[9] = unfoldFromTriangle.edge[edgeNr].corner[unfoldFromTriangle.flippedNormal ? lp : hp].y;
			M_tri[10] = unfoldFromTriangle.edge[edgeNr].corner[unfoldFromTriangle.flippedNormal ? lp : hp].z;
			M_tri[11] = 1.0f;
			M_tri[12] = unfoldFromTriangle.normal.x;
			M_tri[13] = unfoldFromTriangle.normal.y;
			M_tri[14] = unfoldFromTriangle.normal.z;
			M_tri[15] = 1.0f;			
					
			Matrix.invertM(M_star, 0, M_tri, 0);
			Matrix.multiplyMM(T, 0, M_star, 0, M_z, 0);		
			
			for (Triangle3D triangle : triangles.get(unfoldFromTriangle.edge[edgeNr]))
			{
		
				//get the matching edge of the triangles (shared line)
				int mappedEdgeNr = 0;
				for (mappedEdgeNr = 0; mappedEdgeNr < 3 && triangle.edge[mappedEdgeNr]!= unfoldFromTriangle.edge[edgeNr]; mappedEdgeNr++); 

				if (triangle.normal == null)
				{
					CalcTriangle(triangle);
					
					//correct the normal orientation if needed (check depends on the order of corners in the edge (at edge 1 they are swapped))
					if (((((edgeNr & 1) == (mappedEdgeNr & 1))  && unfoldFromTriangle.edge[edgeNr].corner[0] == triangle.edge[mappedEdgeNr].corner[0]) || 
					    (((edgeNr & 1) != (mappedEdgeNr & 1))  && unfoldFromTriangle.edge[edgeNr].corner[0] == triangle.edge[mappedEdgeNr].corner[1])) 
					    != unfoldFromTriangle.flippedNormal) 
					{
						triangle.normal.x = -triangle.normal.x;
						triangle.normal.y = -triangle.normal.y;
						triangle.normal.z = -triangle.normal.z;
						triangle.flippedNormal = true;
					}
				}	
				
				//transform normal
				float[] transformedNormal = Matrix.multiplyV(T, new float[] {triangle.normal.x, triangle.normal.y, triangle.normal.z, 0.0f});
				
						
				//calculate angle between planes (take into consideration the orientation of the normals)
				float unfoldAngle = (float) Math.atan2(-transformedNormal[0], transformedNormal[2]);
				if (unfoldAngle <= 0.0f)
					unfoldAngle += Math.PI;
				
				if (unfoldToTriangle == null || unfoldAngle < minUnfoldAngle)
				{
					unfoldToTriangle = triangle;
					minUnfoldAngle = unfoldAngle;
				}
			}
			area += Unfold(unfoldToTriangle);
		}
		return area;
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
		
		corners.clear();
		edges.clear();
		triangles.clear();
		
		Map<String, AtomicInteger> map = new HashMap<>();

		float totalSpaceM3 = 0;
		
		for (IfcSpace ifcSpace : model.getAll(IfcSpace.class)) {
			Double volume = IfcUtils.getIfcQuantityVolume(ifcSpace);
			if (volume != null) {
				totalSpaceM3 += volume;
			}
		}
		
		Corner3D minCorner = null;
		int duplicateCorners = 0;
		int duplicateEdges = 0;
		int duplicateTriangles = 0;
		
		List<IfcProduct> allWithSubTypes = model.getAllWithSubTypes(IfcProduct.class);
		for (IfcProduct ifcProduct : allWithSubTypes) {
			if (!map.containsKey(ifcProduct.eClass().getName())) {
				map.put(ifcProduct.eClass().getName(), new AtomicInteger(0));
			}
			GeometryInfo geometryInfo = ifcProduct.getGeometry();
			if (geometryInfo == null) 
				continue;
			
			GeometryData geometryData = geometryInfo.getData();
			if (geometryData == null)
				continue;
			
			map.get(ifcProduct.eClass().getName()).incrementAndGet();
			nrUsedProducts++;

			ByteBuffer indicesBuffer = ByteBuffer.wrap(geometryData.getIndices().getData());
			indicesBuffer.order(ByteOrder.LITTLE_ENDIAN);
			ShortBuffer indices = indicesBuffer.asShortBuffer();

			ByteBuffer verticesBuffer = ByteBuffer.wrap(geometryData.getVertices().getData());
			verticesBuffer.order(ByteOrder.LITTLE_ENDIAN);
			FloatBuffer vertices = verticesBuffer.asFloatBuffer();

			nrTriangles += (indices.capacity() / 3);
			nrVertices += (vertices.capacity() / 3);

			Corner3D[] triangleCorner = new Corner3D[3]; 

			// Add points, lines and triangles		
			for (int i=0; i < indices.capacity(); i+=3) 
			{	
				//add corners
				for (int j = 0; j < 3; j++) 
				{
					triangleCorner[j] = new Corner3D(vertices.get(indices.get(i + j) * 3),
					      				   vertices.get(indices.get(i + j) * 3 + 1),
										   vertices.get(indices.get(i + j) * 3 + 2));
					if (corners.containsKey(triangleCorner[j]))
					{
						triangleCorner[j] = corners.get(triangleCorner[j]);
						duplicateCorners++;
					}
					else 
					{
						corners.put(triangleCorner[j], triangleCorner[j]);
						edges.put(triangleCorner[j], new HashMap<Edge3D, Edge3D>());
					}	
					
					//set the minimal point index => starting point for finding surrounding lines/triangles
					if (minCorner == null || 
					    triangleCorner[j].x < minCorner.x ||
						(triangleCorner[j].x == minCorner.x && triangleCorner[j].y < minCorner.y) ||
						(triangleCorner[j].x == minCorner.x && triangleCorner[j].y == minCorner.y && triangleCorner[j].z < minCorner.z))
						minCorner = triangleCorner[j];
				}	
				
				//add edges
				Edge3D[] curEdge = new Edge3D[3];
				for (int j = 0; j < 3; j++)
				{
					int k = (j + 1) % 3;
					curEdge[j] = new Edge3D(triangleCorner[j], triangleCorner[k]);
					if (edges.get(triangleCorner[j]).containsKey(curEdge[j]))
					{
						curEdge[j] = edges.get(triangleCorner[j]).get(curEdge[j]);
						duplicateEdges++;
					}
					else
					{
						edges.get(triangleCorner[j]).put(curEdge[j], curEdge[j]);						
						edges.get(triangleCorner[k]).put(curEdge[j], curEdge[j]);
						triangles.put(curEdge[j], new HashSet<Triangle3D>());
					}						
				}
				
				//add triangles
				Triangle3D tri =  new Triangle3D(curEdge);
				for (int j = 0; j < 3; j++)
				{
					if (triangles.get(curEdge[j]).contains(tri))
						duplicateTriangles++;
					else 
						triangles.get(curEdge[j]).add(tri);					
				}
			}
		}
		
		// selection of outer shell
		//get minEdge from minCorner (line with the smallest deviation of the x-axis)
		Edge3D minEdge = null;
		float minAngleEdge = 0.0f;
		Map<Edge3D, Edge3D> curEdges = edges.get(minCorner);
		for (Edge3D edge : curEdges.values())
		{
			float angle = (float) Math.atan2(edge.corner[1].x - edge.corner[0].x, edge.corner[1].y - edge.corner[0].y);
			if (minEdge == null || Math.abs(angle) < Math.abs(minAngleEdge))
			{
				minEdge = edge;
				minAngleEdge = angle;
			}
		}
		
		//get minTriangle from minEdge (triangle with the smallest deviation of the x-axis)
		Triangle3D minTriangle = null;
		float minAngleTriangle = 0.0f;
		Set<Triangle3D> curTriangles = triangles.get(minEdge);
		for (Triangle3D triangle : curTriangles)
		{
			float angle;
			// calculate angle deviation of x-axis of the second line of the triangle (if this line is parallel to the z-axis the angle is similar the angle of the first line) 
			if (triangle.edge[1].corner[1].x == triangle.edge[1].corner[0].x && triangle.edge[1].corner[1].y == triangle.edge[1].corner[0].y)
				angle = minAngleEdge;
			else 
				angle = (float) Math.atan2(triangle.edge[1].corner[1].x - triangle.edge[1].corner[0].x, triangle.edge[1].corner[1].y - triangle.edge[1].corner[0].y);
			
			if (minTriangle == null || Math.abs(angle) < Math.abs(minAngleTriangle))
			{
				minTriangle = triangle;
				minAngleTriangle = angle;
			}
		}
		
		//unfolding blanket algorithm
		totalArea = Unfold(minTriangle);
	
		
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

		results.append("Duplicate Corners: " + duplicateCorners + "\n");
		results.append("Duplicate Edges: " + duplicateEdges + "\n");
		results.append("Dublicate Triangles: " + duplicateTriangles + "\n");
		
		
		addExtendedData(results.toString().getBytes(Charsets.UTF_8), "test.txt", "Test", "text/plain", bimServerClientInterface, roid);
	}
}