package org.bimserver.demoplugins.bresaer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;


import org.bimserver.demoplugins.bresaer.Plane;
import org.bimserver.models.geometry.GeometryData;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcBuildingElementProxy;
import org.bimserver.utils.math.Vector;

public class Panel {
	//All sizes are in 1/100 mm 
	private static final int[] UlmaOffset = { 4000, 4000, 4650, 2350}; //L, R, T, B
	private static final int[] StamOffset = { 4000, 4000, 8230, 2100}; //L, R, T, B
	private static final int[] SolarWallOffset = { 4000, 4000, 5200, 5200}; //L, R, T, B
	private static final int[] EurecatOffset = { 13500, 13500, 40200, 10000}; //L, R, T, B
	private static final int InsulationThickness = 4000;
	private static final int PVThickness        =  6000;
	private static final int PanelThickness     = 29000; //including 4cm of insulation
	private static final int EurecatThickness   = 54640;
//	private static final int UlmaThickness      = 29000;
//	private static final int StamThickness      = 29000;
//	private static final int SolarWallThickness = 29000;

	
	public enum PanelType {
		   ULMA,
		   STAM, 
		   SOLARWALL,
		   EURECAT,
		   UNKNOWN
		}
	
	public Coordinate min, max;
	public PanelType  type;
	public PanelSize  size;
	public boolean    hasPV = false;
	public boolean    isOpening = false;
	public int        thickness;
	public int     	  normalAxis;
	public boolean    positiveNormal; // => use min value when true else max value
	public int        upAxis;
	public String 	  id;
	

	public int widthAxis() {
		return 3 - upAxis - normalAxis;
	}
	

	private int getLength(Coordinate corn1, Coordinate corn2, int axisId) {
		return Math.abs(corn1.v[axisId] - corn2.v[axisId]);
	}
	
	private int AxisId(Coordinate corn1, Coordinate corn2) {
		int axis = (corn2.v[0] - corn1.v[0]) == 0 ? 0 : 1 +
				   (corn2.v[1] - corn1.v[1]) == 0 ? 0 : 2 +
				   (corn2.v[2] - corn1.v[2]) == 0 ? 0 : 4;
		
		switch(axis)
		{
		case 1: return 0; //on x-axis
		case 2: return 1; //on y-axis
		case 4: return 2; //on z-axis
		default: return -1;
		}
	}	
	
	
	
	public Panel(IfcBuildingElementProxy proxy) {
		
		switch (proxy.getObjectType()) {
		 // ULMA
		case "Ulma_frame_with_PV":
			hasPV = true;
		case "Ulma_frame":
			type = PanelType.ULMA;
			thickness = PanelThickness + (hasPV ? PVThickness : 0);  
			break;
			
		// STAM	
		case "Stam_frame_with_PV":
			hasPV = true;
		case "Stam_frame":
			type = PanelType.STAM;
			thickness = PanelThickness + (hasPV ? PVThickness : 0);  
			break;
			
		// SolarWall	
		case "SolarWall_frame":
			type = PanelType.SOLARWALL;
			thickness = PanelThickness + (hasPV ? PVThickness : 0);  
			break;
			
		// Eurecat	
		case "Eurecat_blind_and_window_full":
			isOpening = true;
			type = PanelType.EURECAT;
			thickness = EurecatThickness;  
			break;	

		// Unknown	
		default:
			type = PanelType.UNKNOWN;
			break;
		}

		id = proxy.getGlobalId();
		normalAxis = -1;
		upAxis = -1;		
		
		GeometryInfo gInfo = proxy.getGeometry();
		if (gInfo != null) {
			min  = new Coordinate(gInfo.getMinBounds().getX(), 
					   			  gInfo.getMinBounds().getY(),
					   			  gInfo.getMinBounds().getZ());
			max  = new Coordinate(gInfo.getMaxBounds().getX(), 
					   			  gInfo.getMaxBounds().getY(),
					   			  gInfo.getMaxBounds().getZ());
			int dxyz[] = {max.v[0] - min.v[0], max.v[1] - min.v[1], max.v[2] - min.v[2]};
			
			// Eurocat elements only occur on walls and have no offset (the area of the boundingbox is also the covered area)
			if (type == PanelType.EURECAT)
			{
				upAxis = 2; //z is up
				
				if (dxyz[0] == EurecatThickness) {
					normalAxis = 0;
					size = new PanelSize(dxyz[1],dxyz[2]);
				}
				else { // if (dxyz[1] == EurecatThickness)
					normalAxis = 1;
					size = new PanelSize(dxyz[0],dxyz[2]);
				}
				return;
			}
			
			GeometryData gData = gInfo.getData();
			if (gData != null)
			{
				ByteBuffer indicesBytes = ByteBuffer.wrap(gData.getIndices());
				indicesBytes.order(ByteOrder.LITTLE_ENDIAN);
				IntBuffer indices = indicesBytes.asIntBuffer();

				ByteBuffer verticesBytes = ByteBuffer.wrap(gData.getVertices());
				verticesBytes.order(ByteOrder.LITTLE_ENDIAN);
				FloatBuffer vertices = verticesBytes.asFloatBuffer();	
						
				Coordinate[] corn = new Coordinate[3]; 
			    Coordinate begin = null;
			    Coordinate end = null;
							
				// Find the normal pointing out of the surface and the direction of the 
			    // aluskit vertical profiles (normally up).
			    // -The implemented method only works for axis aligned panels (assumed to be always the case)
			    // -This allows a simplified method which searches for a triangle matching one of the sides of the
			    //  insulation layer (fixed thickness of 4 or 5 cm). This size does not occur elsewhere. The normal
			    //  will always point in the direction of the edge defining the thickness. The remaining 2 edges of 
			    //  the triangle will be the side way or upwards direction. The sideway edge will have a width
			    //  equal to the width of the whole system minus 2 half widths of an aluskit vertical profile (8 cm).			    
				for (int i = 0; i < indices.limit() && normalAxis == -1; i += 3) {
					// Get the indices
					int i1 = indices.get(i);
					int i2 = indices.get(i + 1);
					int i3 = indices.get(i + 2);
					
					// create the new corners of the by indices defined triangle
					corn[0] = new Coordinate(vertices.get(i1 * 3), vertices.get((i1 * 3) + 1), vertices.get((i1 * 3) + 2));
					corn[1] = new Coordinate(vertices.get(i2 * 3), vertices.get((i2 * 3) + 1), vertices.get((i2 * 3) + 2));
					corn[2] = new Coordinate(vertices.get(i3 * 3), vertices.get((i3 * 3) + 1), vertices.get((i3 * 3) + 2));

					// loop through the edges of the triangle
					for (int n = 0; n < 3; n++) {
						// find the axis alignment of the edge (-1 = not aligned;  0 = x-axis;  1 = y-axis;  2 = z-axis)  
						int normalAxis = AxisId(corn[n], corn[(n+1)%3]);
						
						// only check axis aligned edges for a length matching the InsulationThickness = normal direction 
						if (normalAxis != -1 && getLength(corn[n], corn[(n+1)%3], normalAxis) == InsulationThickness)
						{
							// normal goes from min to max => true
							positiveNormal = corn[n].v[normalAxis] == min.v[normalAxis] || corn[(n+1)%3].v[normalAxis] == min.v[normalAxis];						

							// determine up direction by the remaining triangle edges (loop for both remaining edges)
							for (int j = 0; j < 2; j++) {
								// find the axis alignment of the edge (-1 = not aligned;  0 = x-axis;  1 = y-axis;  2 = z-axis)  								
								int triAxis = AxisId(corn[(n + j) % 3], corn[(n+2)%3]);
								
								// only check axis aligned edges
								if (triAxis != -1) {
									// if the length of this edges is equal to the total width - 8000 (2 half aluskit-profiles)
									// this axis is pointing from one aluskit vertical profile to another (up is the remaining direction)
									if (getLength(corn[(n + j) % 3], corn[(n+2)%3], triAxis) == dxyz[triAxis] - 8000) { 
										upAxis = 3  - normalAxis - triAxis;
										size = new PanelSize(dxyz[triAxis], dxyz[3  - normalAxis - triAxis]);
									}
									else {
										upAxis = triAxis;
										size = new PanelSize(dxyz[3  - normalAxis - triAxis], dxyz[triAxis]);
									}
									break;
								}
							}
							break;
						}
					}
				}			
			}
		}		
	}

	
/* more complete code to find the orientation of a panel allowing all directions not only matching the grid
	
	private Normal CalcNormal(Coordinate[] coordinates) {
		double[] ab = {coordinates[1].v[0] - coordinates[0].v[0],  coordinates[1].v[1] - coordinates[0].v[1], coordinates[1].v[2] - coordinates[0].v[2] };
		double[] ac = {coordinates[2].v[0] - coordinates[0].v[0],  coordinates[2].v[1] - coordinates[0].v[1], coordinates[2].v[2] - coordinates[0].v[2] };
		double[] cross = Vector.crossProduct(ab, ac);

		double area = Math.sqrt(cross[0] * cross[0] + cross[1] * cross[1] + cross[2] * cross[2]);
		double inv = 1.0f / area;

		cross[0] = cross[0] * inv;
		cross[1] = cross[1] * inv;
		cross[2] = cross[2] * inv;

		return new Normal(cross);
	}	
	
	
	private int getLength(Coordinate corn1, Coordinate corn2) {
		double res = 0;
		for (int i = 0; i < 3; i++)
			res += (corn2.v[i] - corn1.v[i]) * (corn2.v[i] - corn1.v[i]);
		return (int) Math.round(Math.sqrt(res));
	}
	
	
	private boolean IsPointBetweenLinePoints(Coordinate l1, Coordinate l2, Coordinate p) {
		if (l1.equals(p) || l2.equals(p) || l1.equals(l2))
			return false;
		
		int[] v1 = {l2.v[0] - l1.v[0], l2.v[1] - l1.v[1], l2.v[2] - l1.v[2]};
		int[] v2 = {p.v[0] - l1.v[0], p.v[1] - l1.v[1], p.v[2] - l1.v[2]};
		
		if (v2[0] > v1[0] || v2[1] > v1[1] || v2[2] > v1[2])
			return false;
		
		double div = (v2[0] > 0 ? (double)v1[0] / v2[0] : (v2[1] > 0 ? (double)v1[1] / v2[1] : (double)v1[2] / v2[2])); 
		
		return (((int)(div * v1[0]) ==  v2[0]) && ((int)(div * v1[1]) ==  v2[1]) && ((int)(div * v1[2]) ==  v2[2]));  
	}	
	





				//find the normal pointing out of the surface (vertical profile direction not yet defined [for roof])
				for (int i = 0; i < indices.limit(); i += 3) {
					int i1 = indices.get(i);
					int i2 = indices.get(i + 1);
					int i3 = indices.get(i + 2);
					
					// Add the new corners
					corn[0] = new Coordinate(vertices.get(i1 * 3), vertices.get((i1 * 3) + 1), vertices.get((i1 * 3) + 2));
					corn[1] = new Coordinate(vertices.get(i2 * 3), vertices.get((i2 * 3) + 1), vertices.get((i2 * 3) + 2));
					corn[2] = new Coordinate(vertices.get(i3 * 3), vertices.get((i3 * 3) + 1), vertices.get((i3 * 3) + 2));
					
					

					if (getLength(corn[0], corn[1]) == InsulationThickness) {
						normal = new Normal(new double[]{corn[1].v[0] - corn[0].v[0], corn[1].v[1] - corn[0].v[1], corn[1].v[2] - corn[0].v[2]});
						begin = corn[0];
						end = corn[1];
						break;
					}
					else if (getLength(corn[0], corn[2]) == InsulationThickness) {
						normal = new Normal(new double[]{corn[2].v[0] - corn[0].v[0], corn[2].v[1] - corn[0].v[1], corn[2].v[2] - corn[0].v[2]});
						begin = corn[0];
						end = corn[2];
						break;
					}
					else if (getLength(corn[1], corn[2]) == InsulationThickness) {
						normal = new Normal(new double[]{corn[2].v[0] - corn[1].v[0], corn[2].v[1] - corn[1].v[1], corn[2].v[2] - corn[1].v[2]});
						begin = corn[1];
						end = corn[2];
						break;
					}
				}
				 
				//find the side
				if (begin != null)
				{
					for (int i = 0; i < indices.limit(); i += 3) {
						int i1 = indices.get(i);
						int i2 = indices.get(i + 1);
						int i3 = indices.get(i + 2);
						
						// Add the new corners
						corn[0] = new Coordinate(vertices.get(i1 * 3), vertices.get((i1 * 3) + 1), vertices.get((i1 * 3) + 2));
						corn[1] = new Coordinate(vertices.get(i2 * 3), vertices.get((i2 * 3) + 1), vertices.get((i2 * 3) + 2));
						corn[2] = new Coordinate(vertices.get(i3 * 3), vertices.get((i3 * 3) + 1), vertices.get((i3 * 3) + 2));
	
						if (getLength(corn[0], corn[1]) == 8000) {
							if (IsPointBetweenLinePoints(corn[0], corn[1], end)) {
								Coordinate tmp = begin;
								begin = end;
						        end  = tmp;
								side = new Normal(new double[]{corn[1].v[0] - corn[0].v[0], corn[1].v[1] - corn[0].v[1], corn[1].v[2] - corn[0].v[2]});
								break;
						    }
							else if (IsPointBetweenLinePoints(corn[0], corn[1], end))  {
								side = new Normal(new double[]{corn[1].v[0] - corn[0].v[0], corn[1].v[1] - corn[0].v[1], corn[1].v[2] - corn[0].v[2]});
								break;
							}
						}
						else if (getLength(corn[0], corn[2]) == 8000) {  
							if (IsPointBetweenLinePoints(corn[0], corn[2], begin)) {
								Coordinate tmp = begin;
								begin = end;
						        end  = tmp;
						        side = new Normal(new double[]{corn[2].v[0] - corn[0].v[0], corn[2].v[1] - corn[0].v[1], corn[2].v[2] - corn[0].v[2]});
								break;
						    }
							else if (IsPointBetweenLinePoints(corn[0], corn[2], end))  {
								side = new Normal(new double[]{corn[2].v[0] - corn[0].v[0], corn[2].v[1] - corn[0].v[1], corn[2].v[2] - corn[0].v[2]});
								break;
							}
						}
						else if (getLength(corn[1], corn[2]) == 8000) {
							if (IsPointBetweenLinePoints(corn[1], corn[2], begin)) {
								Coordinate tmp = begin;
								begin = end;
						        end  = tmp;
								side = new Normal(new double[]{corn[2].v[0] - corn[1].v[0], corn[2].v[1] - corn[1].v[1], corn[2].v[2] - corn[1].v[2]});
								break;
						    }
							else if (IsPointBetweenLinePoints(corn[1], corn[2], end))  {
								side = new Normal(new double[]{corn[2].v[0] - corn[1].v[0], corn[2].v[1] - corn[1].v[1], corn[2].v[2] - corn[1].v[2]});
								break;
							}
						}
					}				
				}	


	*/
	
	
}
