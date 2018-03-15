package org.bimserver.demoplugins.digitalmason;

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
import java.util.LinkedList;

import org.bimserver.utils.math.Vector;

public class ModelGeometry {

	public HashMap<Corner3D, Corner3D> corners = new HashMap<Corner3D, Corner3D>();
	public HashMap<Edge3D, Edge3D>     edges = new HashMap<Edge3D, Edge3D>();
	public HashMap<Normal, LinkedList<Polygon3D>> polygons = new HashMap<Normal, LinkedList<Polygon3D>>(); //normal sorted

//	HashMap<Integer, S3DObject>           m_3DObjectsById;

	/*******************************************************************************/
	/* Corner manipulation *********************************************************/

	public Corner3D NewCorner(float _x, float _y, float _z) {
		Corner3D newCorner = new Corner3D (_x, _y, _z);
		Corner3D retCorner = corners.putIfAbsent(newCorner, newCorner) ;
		return retCorner == null ? newCorner : retCorner;
	}

	/*******************************************************************************/
	/* Edge manipulation ***********************************************************/

	public Edge3D NewEdge(Corner3D p1, Corner3D p2) {
		Edge3D newEdge = new Edge3D (p1, p2);
		Edge3D retEdge = edges.putIfAbsent(newEdge, newEdge);
		return retEdge == null ? newEdge : retEdge;
	}

	/*******************************************************************************/

	Polygon3D NewPolygon(Edge3D[] edges, Normal normal) {
		Polygon3D pol = new Polygon3D();
		if (!polygons.containsKey(normal))
			polygons.put(normal, new LinkedList<Polygon3D>());
		
		polygons.get(normal).addLast(pol);

		// add edges to polygon and link polygon to each edge
		for (int i = 0; i < 3; ++i) {
			pol.edges.addLast(edges[i]);
			edges[i].ofPolygon.addLast(pol);
		}

		//set the normal of the polygon
		pol.normal = new Normal(normal);
		return pol;
	}

	/*******************************************************************************/

	public Normal CalcNormal(Edge3D[] edges) {
		double[] ab = edges[0].getVector();
		double[] ac = edges[1].getVector();
		double[] cross = Vector.crossProduct(ab, ac);

		double area = Math.sqrt(cross[0] * cross[0] + cross[1] * cross[1] + cross[2] * cross[2]);
		double inv = 1.0f / area;

		cross[0] = cross[0] * inv;
		cross[1] = cross[1] * inv;
		cross[2] = cross[2] * inv;

		return new Normal(cross);
	}

	public static double PointToPlaneDistance(Corner3D point, Corner3D planeOrigin, Normal planeNormal) {
		return planeNormal.x * (point.x - planeOrigin.x) +
			   planeNormal.y * (point.y - planeOrigin.y) +
			   planeNormal.z * (point.z - planeOrigin.z);
	}	
	
	public boolean IsPointOnPlane(Corner3D point, Polygon3D poly) {
		return (Math.abs(PointToPlaneDistance(point, poly.edges.getFirst().corner[0], poly.normal)) < 0.0005); // ACCURACY of 0.0001 is not working well enough
	}	
	
	
	/*******************************************************************************/

	public void InsertTriangleToPolygon(Polygon3D pol, Edge3D[] edges, Normal normal)
	{
		// add or remove edges of the polygon (each edge should only occur once) 
		// edges existing twice are only used for the triangulation and can be removed.
		for (int i = 0; i < 3; ++i)	{
			boolean foundEdge = false;
			for (Edge3D edge : pol.edges) {
				if (edge == edges[i]) {
					foundEdge = true;
					pol.edges.remove(edge);
					edge.ofPolygon.remove(pol);
					break;
				}
			}
			if (!foundEdge) {
				pol.edges.addLast(edges[i]);
				edges[i].ofPolygon.addLast(pol);
			}
		}

		//check if polygon is empty
		if (pol.edges.size() == 0) 
			polygons.get(pol.normal).remove(pol);
	}
	
}
