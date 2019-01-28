package org.bimserver.demoplugins.digitalmason;

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
