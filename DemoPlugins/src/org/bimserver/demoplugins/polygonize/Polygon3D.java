package org.bimserver.demoplugins.polygonize;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import org.bimserver.utils.math.Vector;

public class Polygon3D {
	public LinkedList<Edge3D> edges = new LinkedList<Edge3D>();
	public Normal             normal;
	
//	public LinkedList<Object3D> ofObject = new LinkedList<Object3D>();
	public LinkedList<Polygon3D> innerPolygon = new LinkedList<Polygon3D>();
//	public Polygon3D connectedPolygon = null;
	public Plane onPlane;
	public Layer ofLayer;
	
	public Polygon3D() {
		normal = null;
	}
	
	public Edge3D EdgeAt(int index) {
		return edges.get(index);
	}
	
	boolean InnerPolygon(Polygon3D pol1, Polygon3D pol2)
	{
		int nr = 0;
		Edge3D edge2 = pol2.edges.peekFirst();

		double[] db = edge2.getVector();

		for (Edge3D edge1 : pol1.edges) {
			double[] da = edge1.getVector();
			double dc[] = { edge2.corner[0].x - edge1.corner[0].x, edge2.corner[0].y - edge1.corner[0].y, edge2.corner[0].z - edge1.corner[0].z };
			double crossDaDb[];
			crossDaDb = Vector.crossProduct(da, db);
			double norm = Vector.dot(crossDaDb, crossDaDb);

			double s = Vector.dot(Vector.crossProduct(dc, db), crossDaDb) / norm;
			double t = Vector.dot(Vector.crossProduct(dc, da), crossDaDb) / norm;

			//check if lines intersect and not at an end point
			if (s >= 0.0 && s <= 1.0 && t > 1.0)
				++nr;
		}
		return ((nr & 1) == 1);
	}

	public void Correct(ModelGeometry model) {	
		int nrPolygons = SortEdges();
		if (nrPolygons > 1) {
			Polygon3D[] splitPolygon = new Polygon3D[100];
			boolean[]   isInnerPolygon = new boolean[100];
	
			splitPolygon[0] = this;
			Corner3D beginPoint = edges.peekFirst().corner[0];
			int polygonId = 0;
			boolean first = true;
			for (Iterator<Edge3D> itEdge = edges.iterator(); itEdge.hasNext();) {
				Edge3D edge = itEdge.next();
				
				if (polygonId != 0) {
					splitPolygon[polygonId].edges.addLast(edge);
					edges.remove(edge);
					model.polygons.get(normal).addLast(splitPolygon[polygonId]);
				}
	
				if (!first && (beginPoint == edge.corner[0] || beginPoint == edge.corner[1])) {
					//set all polygons to be not inner => detected later
					isInnerPolygon[polygonId] = false;
					++polygonId;
	
					if (polygonId < nrPolygons) {
						splitPolygon[polygonId] = new Polygon3D();
						splitPolygon[polygonId].normal = new Normal(normal);
	
						// Add the list of objects to the new polygon
//						for (Object3D obj : ofObject)
//							splitPolygon[polygonId].ofObject.add(obj);
	
						//set the new begin point of the next polygon
						beginPoint = edge.corner[0];
					}
					first = true;
				}
				else
					first = false;
			}
	
			// find and assign innerpolygons
			for (int k = 0; k < nrPolygons; ++k) {				
				for (int l = 0; l < nrPolygons; ++l) {
					if (k == l)
						continue;
	
					if (InnerPolygon(splitPolygon[k], splitPolygon[l])) {
						isInnerPolygon[l] = true;
						splitPolygon[k].innerPolygon.addLast(splitPolygon[l]);
					}
				}
			}
				
			// remove the first polygon if it is an inner polygon
			if (isInnerPolygon[0])
				model.polygons.get(normal).remove(splitPolygon[0]);
	
			// add polygons to the list if they are not innerpolygons starting from the second polygon
			for (int k = 1; k < nrPolygons; ++k) {
				if (!isInnerPolygon[k]) {
					model.polygons.get(normal).addFirst(splitPolygon[k]);
				}
			}
		}
	}
	
	public int SortEdges() {
		ListIterator<Edge3D> itF, itB, itLoop;
		int nrPolygons = 0;

		itF = itB = itLoop = edges.listIterator();
		int cornerPos = 1;
		while (itB.hasNext())
		{
			Edge3D loopEdge = itLoop.next();
			itLoop.previous();
			
			// check if itLoop is a connected edge
			Edge3D bEdge = itB.next();
			if (bEdge.equals(loopEdge) || bEdge.corner[cornerPos].equals(loopEdge.corner[1]))
			{
				// set the new cornerPos to use for checking
				if (bEdge.corner[cornerPos].equals(loopEdge.corner[0]))
					cornerPos = 1;
				else
					cornerPos = 0;

				//swap edges if found further as the next element
				if (itB != itLoop)
				{
					itB.set(bEdge);
					itLoop.set(loopEdge);
					itLoop = itB;
				}

				//check if the sorted polygon edges, up till now are closing a polygon
				Edge3D fEdge = itF.next();
				itF.previous();
				if (fEdge.corner[0].equals(bEdge.corner[cornerPos]))
				{
					itF = itLoop = edges.listIterator(itB.nextIndex());
					++nrPolygons;
					cornerPos = 1;
				}
			} 
			else
				itB.previous();
		}
		return nrPolygons;

	}
}
