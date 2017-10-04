package org.bimserserver.demoplugins.polygonize;

import java.util.LinkedList;

public class Plane {
	public Normal                normal;	
	public LinkedList<Layer>     layers = new LinkedList<Layer>();
	
	public Plane(Normal _normal) {
		normal = _normal;
	}

	public Layer LayerAt(int index) {
		return layers.get(index);
	}
}

