package org.bimserver.demoplugins.digitalMason;

import java.util.HashMap;
import java.util.LinkedList;


public class Layer {
	public double    thickness;
	public float[]   color = new float[3]; 
	HashMap<Normal, LinkedList<Polygon3D>>  polygons = new HashMap<Normal, LinkedList<Polygon3D>>(); //normal sorted	
	
	public Polygon3D PolygonAt(Normal normal, int index) {
		return polygons.get(normal).get(index);
	}
}
