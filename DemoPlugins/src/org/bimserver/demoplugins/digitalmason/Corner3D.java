package org.bimserver.demoplugins.digitalmason;

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

public class Corner3D {
	public int x; // coordinates in 0.01 mm (input value float is in mm)
	public int y; // coordinates in 0.01 mm (input value float is in mm)
	public int z; // coordinates in 0.01 mm (input value float is in mm)
	
	public Corner3D(Corner3D corner) {
		x = corner.x;
		y = corner.y;
		z = corner.z;
	}	
	
	public Corner3D(float _x, float _y, float _z) {
		x = Math.round(_x * 100.0f);
		y = Math.round(_y * 100.0f);
		z = Math.round(_z * 100.0f);
	}

	public Corner3D(double _x, double _y, double _z) {
		x = (int) Math.round(_x * 100.0);
		y = (int) Math.round(_y * 100.0);
		z = (int) Math.round(_z * 100.0);
	}	
	
	boolean lessThan(Corner3D corner) {
		if (x == corner.x) {
			if (y == corner.y) {
				return z < corner.z;
			}
			return y < corner.y;
		}
		return x < corner.x;
	}	
	
	public double getRealX() {
		return x * 0.01;
	}

	public double getRealY() {
		return y * 0.01;
	}
	
	public double getRealZ() {
		return z * 0.01;
	}
	
	@Override
	public boolean equals (Object o) {
		if (o == this) return true;
		
		if (!(o instanceof Corner3D)) return false;
		
		Corner3D corn = (Corner3D) o;
		return corn.x == x && corn.y == y && corn.z == z;
	}
	
	
	@Override 
	public int hashCode() {
        int result = 17; 
        result = 71 * result + Integer.hashCode(x);
        result = 71 * result + Integer.hashCode(y);
        result = 71 * result + Integer.hashCode(z);
        return result;
	}	
		
}	

