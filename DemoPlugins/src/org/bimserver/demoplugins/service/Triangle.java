package org.bimserver.demoplugins.service;

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