package org.bimserver.demoplugins.bresaer;


public class Plane {
	public int normalAxis;
	public int normalAxisDist;     // distance on the normal axis   
	
	public Plane(int _normalAxis, Coordinate _origin) {
		normalAxis = _normalAxis;
		normalAxisDist = _origin.v[normalAxis];
	}
	
	public boolean ContainsPoint(Coordinate corn) {
		return corn.v[normalAxis] == normalAxisDist;
	}
	
	@Override 
	public boolean equals(Object o) {
		if (o == this) return true;
		if (!(o instanceof Plane)) {
	            return false;
	    }
		Plane plane = (Plane) o;
		
		return plane.normalAxis == normalAxis && plane.normalAxisDist == normalAxisDist;
	}		
		
	@Override 
	public int hashCode() {
        int result = 19; 
        result = 67 * result + Integer.hashCode(normalAxis);
        result = 67 * result + Integer.hashCode(normalAxisDist);
        return result;
	}	
}

