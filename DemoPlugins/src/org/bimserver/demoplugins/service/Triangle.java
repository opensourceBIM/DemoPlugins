package org.bimserver.demoplugins.service;

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

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class Triangle {

	private IntBuffer indicesIntBuffer;
	private FloatBuffer verticesFloatBuffer;
	private int currentTriangle;

	public Triangle(IntBuffer indicesIntBuffer, FloatBuffer verticesFloatBuffer, int currentTriangle) {
		this.indicesIntBuffer = indicesIntBuffer;
		this.verticesFloatBuffer = verticesFloatBuffer;
		this.currentTriangle = currentTriangle;
	}

	//http://math.stackexchange.com/a/128999
	public float area() {
		return (float) (0.5f * 
			Math.sqrt(
				Math.pow(((getX2() * getY3()) - (getX3() * getY2())), 2) +
				Math.pow(((getX3() * getY1()) - (getX1() * getY3())), 2) +
				Math.pow(((getX1() * getY2()) - (getX2() * getY1())), 2)));
	}
	
	public float getX1() {
		return verticesFloatBuffer.get(3 * indicesIntBuffer.get(currentTriangle * 3));
	}
	
	public float getY1() {
		return verticesFloatBuffer.get(3 * indicesIntBuffer.get(currentTriangle * 3) + 1);
	}
	
	public float getZ1() {
		return verticesFloatBuffer.get(3 * indicesIntBuffer.get(currentTriangle * 3) + 2);
	}

	public float getX2() {
		return verticesFloatBuffer.get(3 * indicesIntBuffer.get(currentTriangle * 3 + 1));
	}
	
	public float getY2() {
		return verticesFloatBuffer.get(3 * indicesIntBuffer.get(currentTriangle * 3 + 1) + 1);
	}
	
	public float getZ2() {
		return verticesFloatBuffer.get(3 * indicesIntBuffer.get(currentTriangle * 3 + 1) + 2);
	}
	
	public float getX3() {
		return verticesFloatBuffer.get(3 * indicesIntBuffer.get(currentTriangle * 3 + 2));
	}
	
	public float getY3() {
		return verticesFloatBuffer.get(3 * indicesIntBuffer.get(currentTriangle * 3 + 2) + 1);
	}
	
	public float getZ3() {
		return verticesFloatBuffer.get(3 * indicesIntBuffer.get(currentTriangle * 3 + 2) + 2);
	}
}