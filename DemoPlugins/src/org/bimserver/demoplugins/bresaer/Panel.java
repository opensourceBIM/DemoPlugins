package org.bimserver.demoplugins.bresaer;

import org.bimserver.demoplugins.bresaer.BresaerServicePlugin.Coordinate;
import org.bimserver.demoplugins.bresaer.BresaerServicePlugin.PanelSize;
import org.bimserver.demoplugins.bresaer.BresaerServicePlugin.PanelType;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcBuildingElementProxy;

public class Panel {

	public enum PanelType {
		   ULMA,
		   STAM, 
		   SOLARWALL,
		   EURECAT,
		   UNKNOWN
		}
	
	public Coordinate min, max;
	public PanelType  type;
	public Coordinate pos;
	public PanelSize  size;
	public boolean    hasPV = false;
	public boolean    isOpening = false;
	public int        thickness;
	public short      normal = -1 ; //0 = x, 1 = y, 2 = z
	public String id;
	
	public Panel(IfcBuildingElementProxy proxy) {
		
		switch (proxy.getObjectType()) {
		 // ULMA
		case "Ulma_frame_with_PV":
			hasPV = true;
		case "Ulma_frame":
			type = PanelType.ULMA;
			thickness = hasPV ? 36300 : 302000;  
			break;
			
		// STAM	
		case "Stam_frame_with_PV":
			hasPV = true;
		case "Stam_frame":
			type = PanelType.STAM;
			thickness = hasPV ? 35300 : 29200;  
			break;
			
		// SolarWall	
		case "SolarWall_frame":
			type = PanelType.SOLARWALL;
			thickness = 29600;  
			break;
			
		// Eurecat	
		case "Eurecat_blind_and_window_full":
			isOpening = true;
			type = PanelType.EURECAT;
			thickness = 54600;  
			break;	

		// Unknown	
		default:
			type = PanelType.UNKNOWN;
			break;
		}

		id = proxy.getGlobalId();
		
		GeometryInfo gInfo = proxy.getGeometry();
		if (gInfo != null) {
			pos = min  = new Coordinate(gInfo.getMinBounds().getX(), 
					   			  gInfo.getMinBounds().getY(),
					   			  gInfo.getMinBounds().getZ());
			max  = new Coordinate(gInfo.getMaxBounds().getX(), 
					   			  gInfo.getMaxBounds().getY(),
					   			  gInfo.getMaxBounds().getZ());
			int dx = max.v[0] - min.v[0];
			int dy = max.v[1] - min.v[1];
			int dz = max.v[2] - min.v[2];
			if (thickness == dx &&  dx != dy && dx != dz) {
				size  = new PanelSize(max.v[1] - min.v[1], max.v[2] - min.v[2]);
				normal = 0;
			}
			else if (thickness == dy &&  dy != dx && dy != dz) {
				size  = new PanelSize(max.v[0] - min.v[0], max.v[2] - min.v[2]);
				normal = 1;
			}
			else if (thickness == dz &&  dz != dx && dz != dy) {
				size  = new PanelSize(max.v[0] - min.v[0], max.v[1] - min.v[1]);
				normal = 2;				
			} 			
		}		
	}

}
