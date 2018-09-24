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

		ByteBuffer indicesBuffer = ByteBuffer.wrap(data.getIndices().getData());
		indicesBuffer.order(ByteOrder.LITTLE_ENDIAN);
		indicesIntBuffer = indicesBuffer.asIntBuffer();
		
		ByteBuffer verticesBuffer = ByteBuffer.wrap(data.getVertices().getData());
		verticesBuffer.order(ByteOrder.LITTLE_ENDIAN);
		verticesFloatBuffer = verticesBuffer.asFloatBuffer();
	}

	public boolean hasNext() {
		return currentTriangle < this.data.getNrIndices() / 3;
	}

	public Triangle next() {
		Triangle triangle = new Triangle(indicesIntBuffer, verticesFloatBuffer, currentTriangle);
		currentTriangle++;
		return triangle;
	}
}
