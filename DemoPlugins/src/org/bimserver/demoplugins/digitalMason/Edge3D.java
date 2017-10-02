package org.bimserver.demoplugins.digitalMason;

import java.util.LinkedList;


public class Edge3D {

	private	int[] vector = new int[3];
		//	float     length = 0.0;

	public Corner3D[] corner = new Corner3D[2];
	public LinkedList<Polygon3D> ofPolygon = new LinkedList<Polygon3D>();

	public Edge3D(Edge3D _edge)	{
		corner[0] = _edge.corner[0];
		corner[1] = _edge.corner[1];
		vector[0] = 0;
		vector[1] = 0;
		vector[2] = 0;
	}

	public Edge3D(Corner3D _p1, Corner3D _p2) // 2 points indices in array 
	{
		corner[0] = (_p1.lessThan(_p2)) ? _p1 : _p2;
		corner[1] = (_p1.lessThan(_p2)) ? _p2 : _p1;
		vector[0] = 0;
		vector[1] = 0;
		vector[2] = 0;
	}

	public boolean lessThan(Edge3D edge) {
		if (corner[0].equals(edge.corner[0])) {
			return corner[1].lessThan(edge.corner[1]);
		}
		return corner[0].lessThan(edge.corner[0]);
	}
	
	public double[] getVector() {
		if (vector[0] == 0 && vector[1] == 0 && vector[2] == 0)	{
			vector[0] = corner[1].x - corner[0].x;
			vector[1] = corner[1].y - corner[0].y;
			vector[2] = corner[1].z - corner[0].z;
		}
		return new double[] {vector[0], vector[1], vector[2]};
	}
	
	@Override
	public boolean equals (Object o) {
		if (o == this) return true;
		
		if (!(o instanceof Edge3D)) return false;
		
		Edge3D edge = (Edge3D) o;
		return edge.corner[0].equals(corner[0]) && edge.corner[1].equals(corner[1]);
	}
	
	@Override 
	public int hashCode() {
        int result = 19; 
        result = 113 * result + corner[0].hashCode();
        result = 113 * result + corner[1].hashCode();
        return result;
	}	
}
