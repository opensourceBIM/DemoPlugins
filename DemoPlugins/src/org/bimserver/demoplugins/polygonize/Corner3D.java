package org.bimserver.demoplugins.polygonize;

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

