package org.bimserserver.demoplugins.polygonize;

import java.util.HashMap;
import java.util.LinkedList;

import org.bimserver.demoplugins.rccalculation.RcCalculationServicePlugin.Material;

public class Layer {
	public double    thickness;
	public float[]   color = new float[3]; 
//	public Corner3D  min;
//	public Corner3D  max;
	HashMap<Normal, LinkedList<Polygon3D>>  polygons = new HashMap<Normal, LinkedList<Polygon3D>>(); //normal sorted	
	
	public Polygon3D PolygonAt(Normal normal, int index) {
		return polygons.get(normal).get(index);
	}
}
