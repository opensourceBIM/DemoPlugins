package org.bimserver.demoplugins.bresaer;

public class Coordinate {
	public int[] v = new int[3]; 
	
	public Coordinate(double _x, double _y, double _z) {
		v[0] = (int)Math.round(_x * 100.0); // in 0.01 mm 
		v[1] = (int)Math.round(_y * 100.0); // in 0.01 mm
		v[2] = (int)Math.round(_z * 100.0); // in 0.01 mm
	}

	public Coordinate(int _x, int _y, int _z) {
		v[0] = _x; // in 0.01 mm 
		v[1] = _y; // in 0.01 mm
		v[2] = _z; // in 0.01 mm
	}		
	
	public Coordinate(Coordinate coor) {
		v[0] = coor.v[0]; // in 0.01 mm 
		v[1] = coor.v[1]; // in 0.01 mm
		v[2] = coor.v[2]; // in 0.01 mm
	}	
	
	@Override 
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof Coordinate)) {
	            return false;
	    }
		Coordinate coor = (Coordinate) o;
		 
		return coor.v[0] == v[0] && coor.v[1] == v[1] && coor.v[2] == v[2];
	}
		
	@Override 
	public int hashCode() {
        int result = 17; 
        result = 31 * result + Integer.hashCode(v[0]);
        result = 31 * result + Integer.hashCode(v[1]);
        result = 31 * result + Integer.hashCode(v[2]);
        return result;
	}
}
