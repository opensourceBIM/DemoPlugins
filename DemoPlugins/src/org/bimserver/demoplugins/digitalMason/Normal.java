package org.bimserver.demoplugins.digitalMason;

public class Normal {
	public double x; // coordinates in 0.01 mm (input value float is in mm)
	public double y; // coordinates in 0.01 mm (input value float is in mm)
	public double z; // coordinates in 0.01 mm (input value float is in mm)
	public int azimuth; // rotation from x axis towards y axis in 0.001 degrees  (0 to 360000)
	public int tilt;    // rotation from xy plane towards z axis in 0.001 degrees (-90000 to 90000) 
	
	public Normal(double[] normal) {
		if (normal[0] < 0.0)
		{
			x = -normal[0];
			y = -normal[1];
			z = -normal[2];
		}
		else if (normal[0] > 0)	{
			x = normal[0];
			y = normal[1];
			z = normal[2];
		}
		else 
		{
			x = 0;
			if (normal[1] < 0.0 ) {
				y = -normal[1];
				z = -normal[2];
			}
			else if (normal[1] > 0.0 ) {
				y = normal[1];
				z = normal[2];
			}
			else
			{
				y = 0;
				z = Math.abs(normal[2]);
			}
		}
			
		azimuth = (int)Math.round(Math.atan2(y, x) * 180000.0 / Math.PI);
        tilt = (int)Math.round(Math.asin(z) * 180000.0 / Math.PI);
	}	
	
	public Normal(Normal normal) {
		x = normal.x;
		y = normal.y;
		z = normal.z;
		
		azimuth = normal.azimuth;
        tilt = normal.tilt;
	}		
	
	boolean lessThan(Normal normal) {
		if (azimuth == normal.azimuth) {
			return tilt < normal.tilt;
		}
		return azimuth < normal.azimuth;
	}	
	
	
	@Override
	public boolean equals (Object o) {
		if (o == this) return true;
		
		if (!(o instanceof Normal)) return false;
		
		Normal normal = (Normal) o;
		return normal.azimuth == azimuth && normal.tilt == tilt;
	}
	
	
	@Override 
	public int hashCode() {
        int result = 7; 
        result = 71 * result + Integer.hashCode(azimuth);
        result = 71 * result + Integer.hashCode(tilt);
        return result;
	}	
		
}	
