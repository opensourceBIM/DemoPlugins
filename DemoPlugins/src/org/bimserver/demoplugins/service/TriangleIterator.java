package org.bimserver.demoplugins.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.bimserver.models.geometry.GeometryData;

public class TriangleIterator {

	private GeometryData data;
	private int currentTriangle = 0;
	private IntBuffer indicesIntBuffer;
	private FloatBuffer verticesFloatBuffer;

	public TriangleIterator(GeometryData data) {
		this.data = data;

		ByteBuffer indicesBuffer = ByteBuffer.wrap(data.getIndices());
		indicesBuffer.order(ByteOrder.LITTLE_ENDIAN);
		indicesIntBuffer = indicesBuffer.asIntBuffer();
		
		ByteBuffer verticesBuffer = ByteBuffer.wrap(data.getVertices());
		verticesBuffer.order(ByteOrder.LITTLE_ENDIAN);
		verticesFloatBuffer = verticesBuffer.asFloatBuffer();
	}

	public boolean hasNext() {
		return currentTriangle < this.data.getIndices().length / 12;
	}

	public Triangle next() {
		Triangle triangle = new Triangle(indicesIntBuffer, verticesFloatBuffer, currentTriangle);
		currentTriangle++;
		return triangle;
	}
}
