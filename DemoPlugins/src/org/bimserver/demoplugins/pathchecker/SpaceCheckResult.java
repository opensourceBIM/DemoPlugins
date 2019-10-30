package org.bimserver.demoplugins.pathchecker;

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

import java.awt.image.BufferedImage;

import org.bimserver.models.ifc2x3tc1.IfcSpace;

public class SpaceCheckResult {
	private BufferedImage image;
	private float height;
	private float heightMustBe;
	private float widthMustBe;
	private float smallestWidth = Float.MAX_VALUE;
	private boolean isValid;
	private IfcSpace ifcSpace;
	private String extraDescription;

	public BufferedImage getImage() {
		return image;
	}

	public void setImage(BufferedImage image) {
		this.image = image;
	}

	public float getHeight() {
		return height;
	}

	public void setHeight(float height) {
		this.height = height;
	}

	public float getHeightMustBe() {
		return heightMustBe;
	}

	public void setHeightMustBe(float heightMustBe) {
		this.heightMustBe = heightMustBe;
	}

	public float getWidthMustBe() {
		return widthMustBe;
	}

	public void setWidthMustBe(float widthMustBe) {
		this.widthMustBe = widthMustBe;
	}

	public float getSmallestWidth() {
		return smallestWidth;
	}

	public void setSmallestWidth(float smallestWidth) {
		if (smallestWidth < this.smallestWidth) {
			this.smallestWidth = smallestWidth;
		}
	}

	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	public IfcSpace getIfcSpace() {
		return ifcSpace;
	}

	public void setIfcSpace(IfcSpace ifcSpace) {
		this.ifcSpace = ifcSpace;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		if (!isValid) {
			result.append("Object is not valid\n");
		}
		if (height < heightMustBe) {
			result.append("Minimum headroom of " + heightMustBe + " was not met (" + height + ")\n");
		}
		if (smallestWidth < widthMustBe) {
			result.append("Minimum corridor clear width of " + widthMustBe + " was not met (" + smallestWidth + ")\n");
		}
		if (extraDescription != null) {
			result.append(extraDescription + "\n");
		}
		return result.toString();
	}

	public void setExtraDescription(String extraDescription) {
		this.extraDescription = extraDescription;
	}
}
