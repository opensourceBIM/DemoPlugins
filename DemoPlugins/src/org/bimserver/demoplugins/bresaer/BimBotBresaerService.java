package org.bimserver.demoplugins.bresaer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.demoplugins.bresaer.Panel.PanelType;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcBuildingElementProxy;
import org.bimserver.models.ifc2x3tc1.IfcOpeningElement;
import org.bimserver.plugins.SchemaName;
import org.bimserver.plugins.services.BimBotAbstractService;

import com.google.common.base.Charsets;

public class BimBotBresaerService extends BimBotAbstractService {

	private HashMap<Plane, HashMap<Coordinate, List<Panel>>> panelsByPlaneAndPosition  = new HashMap<Plane, HashMap<Coordinate, List<Panel>>>();
	private HashMap<Plane, HashSet<Panel>>     panelsByPlane  = new HashMap<Plane, HashSet<Panel>>();
//	private HashMap<Plane, List<Panel>>[] panelsAt = new HashMap[3];
	private List<Panel>                        EurecatPanels;
	private EnumMap<Panel.PanelType, HashMap<PanelSize, Integer>> nrOfPanelsByTypeAndSize;
	private HashMap<PanelSize, Integer> nrOfUlmaPanels = new HashMap<PanelSize, Integer>();		
	private HashMap<PanelSize, Integer> nrOfStamPanels = new HashMap<PanelSize, Integer>();
	private HashMap<PanelSize, Integer> nrOfSolarPanels = new HashMap<PanelSize, Integer>();	
	private HashMap<PanelSize, Integer> nrOfEurecatPanels = new HashMap<PanelSize, Integer>();	
	private HashMap<PanelSize, Integer> nrOfUnknownPanels = new HashMap<PanelSize, Integer>();	
	
	
	
	private void AddPanelToList(HashMap<Coordinate, List<Panel>> panelsAtPos, Coordinate coor, Panel panel) {			
		List<Panel> panels;
		if (!panelsAtPos.containsKey(coor))	{
			panels = new ArrayList<Panel>();
			panelsAtPos.put(coor, panels);
		}
		else
			panels = panelsAtPos.get(coor);	
		panels.add(panel);
	}			
		
	
	private void GetPanelsFromBIM(IfcModelInterface model)	{
			
		// Panels are stored as IfcBuildingElementProxy, so get all of them and loop through them for analysing each panel
		List<IfcBuildingElementProxy> allWithSubTypes = model.getAllWithSubTypes(IfcBuildingElementProxy.class);
		for (IfcBuildingElementProxy ifcProxy : allWithSubTypes) {
			//determine if the proxy is a panel (contains "initial" and "family" in the type string) or not
			if (!ifcProxy.getObjectType().contains("frame"))
				continue; // no panel so continue to the next proxy 
						
			//create a listing of the panels based on each corner => a list contains neighbouring panel
			Panel curPanel = new Panel(ifcProxy);
			switch (curPanel.type) {
			case ULMA: 
				nrOfUlmaPanels.put(curPanel.size, nrOfUlmaPanels.containsKey(curPanel.size) ? nrOfUlmaPanels.get(curPanel.size) + 1 : 1);
				break;
			case STAM: 
				nrOfStamPanels.put(curPanel.size, nrOfStamPanels.containsKey(curPanel.size) ? nrOfStamPanels.get(curPanel.size) + 1 : 1);
				break;
			case SOLARWALL: 
				nrOfSolarPanels.put(curPanel.size, nrOfSolarPanels.containsKey(curPanel.size) ? nrOfSolarPanels.get(curPanel.size) + 1 : 1);
				break;
			case EURECAT: 
				nrOfEurecatPanels.put(curPanel.size, nrOfEurecatPanels.containsKey(curPanel.size) ? nrOfEurecatPanels.get(curPanel.size) + 1 : 1);
				break;
			default:
				nrOfUnknownPanels.put(curPanel.size, nrOfUlmaPanels.containsKey(curPanel.size) ? nrOfUlmaPanels.get(curPanel.size) + 1 : 1);
			}			
			
			if (curPanel.type == PanelType.EURECAT) 
				continue;
			
			Coordinate[] coor = new Coordinate[2];
			coor[0] = new Coordinate(curPanel.positiveNormal ? curPanel.min : curPanel.max);
			coor[1] = new Coordinate(curPanel.positiveNormal ? curPanel.min : curPanel.max);
			
			// Create a plane object for the current plane, with the origin depening on the normals direction +/-
			Plane plane = new Plane(curPanel.normalAxis, coor[0]); 

			// Find listing of the panels for the current plane
			HashMap<Coordinate, List<Panel>> panelsByPosition;	
			if (!panelsByPlaneAndPosition.containsKey(plane)) {
				panelsByPosition  = new HashMap<Coordinate, List<Panel>>();
				panelsByPlaneAndPosition.put(plane, panelsByPosition);
				panelsByPlane.put(plane, new HashSet<Panel>());
			}
			else
				panelsByPosition = panelsByPlaneAndPosition.get(plane);			
			
			boolean first = true;
			for (int i = 0; i < 2; i++)	{
				if (i != curPanel.normalAxis) {
					if (first)	{
						coor[0].v[i] = curPanel.min.v[i];
						coor[1].v[i] = curPanel.max.v[i];
					}
					else {
						coor[0] = new Coordinate(coor[0]);
						coor[1] = new Coordinate(coor[1]);
						coor[0].v[i] = curPanel.positiveNormal ? curPanel.max.v[i] : curPanel.min.v[i];
						coor[1].v[i] = curPanel.positiveNormal ? curPanel.max.v[i] : curPanel.min.v[i];
					}				
					AddPanelToList(panelsByPosition, coor[0], curPanel);
					AddPanelToList(panelsByPosition, coor[1], curPanel);
				}
			}
			panelsByPlane.get(plane).add(curPanel);
		}
	}
	
	
	private void GetIntersections(IfcModelInterface model) {
		Coordinate min, max;
	
		// Panels are stored as IfcBuildingElementProxy, so get all of them and loop through them for analysing each panel
		List<IfcOpeningElement> openings = model.getAllWithSubTypes(IfcOpeningElement.class);
		for (IfcOpeningElement opening : openings) {
			/*
			// Only voids in the external walls are relevant 
			IfcElement inElement = opening.getVoidsElements().getRelatingBuildingElement();
			*/
			GeometryInfo gInfo = opening.getGeometry();
			if (gInfo != null) {
				min  = new Coordinate(gInfo.getMinBounds().getX(), 
						   			  gInfo.getMinBounds().getY(),
						   			  gInfo.getMinBounds().getZ());
				max  = new Coordinate(gInfo.getMaxBounds().getX(), 
						   			  gInfo.getMaxBounds().getY(),
						   			  gInfo.getMaxBounds().getZ());

				// find the matching plane by checking each plane
				for (HashSet<Panel> panels : panelsByPlane.values()) {
					
					// get a panel from the list to have the corresponding axis definition
					Panel refPanel = panels.iterator().next();
					
					if ((refPanel.positiveNormal && min.v[refPanel.normalAxis] <= refPanel.min.v[refPanel.normalAxis] && 
							                     max.v[refPanel.normalAxis] >= refPanel.min.v[refPanel.normalAxis]) || 
						(!refPanel.positiveNormal && min.v[refPanel.normalAxis] <= refPanel.max.v[refPanel.normalAxis] && 
		                                          max.v[refPanel.normalAxis] >= refPanel.max.v[refPanel.normalAxis])) {
						// the current plane interferes with the current opening
						for (Panel panel : panels) {
							if (panel.min.v[panel.widthAxis()] < max.v[panel.widthAxis()] && 
							    panel.max.v[panel.widthAxis()] > min.v[panel.widthAxis()] &&
								panel.min.v[panel.upAxis] >= max.v[panel.upAxis] &&
								panel.max.v[panel.upAxis] <= min.v[panel.upAxis]) {
								panel.coversOpening = true;
							}
						}
						break;
					}
				}
				
			}
		}
	}	
	
	private String WriteParts() {
		
		//count elements
		//generic parts
		HashMap<Integer, Integer> nrOfVertAluskitProfile = new HashMap<Integer, Integer>();		
		int nrOfFixedBracket = 0;
		int nrOfSlideBracket = 0;
		int nrOfM8HammerBolts = 0;
		int nrOfNuts = 0;
		int nrOfWashers = 0;
		double totalCost = 0;
		
		//ulma panel parts
		
		HashMap<Integer, Integer>    nrOfUlmaHorizRail = new HashMap<Integer, Integer>();	
		int nrOfL70s = 0;
		int nrOfUlmaConnectors = 0;
		int nrOfBars = 0;
		double totalUlmaSurface = 0.0;

		//stam parts
		HashMap<Integer, Integer>    nrOfHorizAluskitProfiles = new HashMap<Integer, Integer>();	
		HashMap<Integer, Integer>    nrOfStamAnchors          = new HashMap<Integer, Integer>();	
		int nrOfL90s = 0;
		double totalStamSurface = 0.0;

		//solarwall panel parts
		HashMap<Integer, Integer>    nrOfOmegaProfiles = new HashMap<Integer, Integer>();	
		double totalSolarSurface = 0.0;
		
		//eurecat panel parts
		double totalEurecatSurface = 0.0;		
	
		String output = "";		
		
		// count nr of Aluskit vertical profiles with length X
		for (HashMap<Coordinate, List<Panel>> panelsAt : panelsByPlaneAndPosition.values()) {		
			HashMap<Integer, LinkedList<Integer>> lengthAt = new HashMap<Integer, LinkedList<Integer>>();
			for (Entry<Coordinate,List<Panel>> entry : panelsAt.entrySet()) {
				for (Panel panel : entry.getValue()) {
					LinkedList<Integer> fromTo; //even is start, odd is end of fillings with a panel
					
					//new y position add the panel height
					if (!lengthAt.containsKey(entry.getKey().v[panel.widthAxis()])) {
						fromTo = new LinkedList<Integer>();
						lengthAt.put(entry.getKey().v[panel.widthAxis()], fromTo);
						fromTo.add(panel.min.v[panel.upAxis]);
						fromTo.add(panel.max.v[panel.upAxis]);
						break;
					}
					//existing y position find connecting panel height
					else
						fromTo = lengthAt.get(entry.getKey().v[panel.widthAxis()]);

					//search the index above the current panel.min.z
					int index;
					for (index = 0;  index < fromTo.size() && fromTo.get(index) < panel.min.v[panel.upAxis]; index++);
	
					//insert if index is even and shift to the next index
					if ((index & 1) == 0) { //even
						fromTo.add(index, panel.min.v[panel.upAxis]);
						index++;
					}
					
					//remove all points till the end or the index points to a point larger or equal to panel.max.z 
					while (index < fromTo.size() && fromTo.get(index) <= panel.max.v[panel.upAxis]) {
						fromTo.remove(index);
					}
					
					//add the max of the panel if an uneven nr of points is in the list
					if ((fromTo.size() & 1) == 1)
						fromTo.add(index, panel.max.v[panel.upAxis]);				
				}
			}	
			
			for (LinkedList<Integer> fromTo : lengthAt.values()) {
				Integer length;
				for (int n = 0; n < fromTo.size(); n+=2) {
					length = fromTo.get(n + 1) - fromTo.get(n);
					nrOfVertAluskitProfile.put(length, nrOfVertAluskitProfile.containsKey(length) ? nrOfVertAluskitProfile.get(length) + 1 : 1);
					nrOfFixedBracket++;
					nrOfSlideBracket++;
				}				
			}
		}

		
		// ulma panel numbers	
		// get the connectors for ulma one per coordinate used by ulma panels
//		for (int i = 0; i < 2; i++)	{
		for (HashMap<Coordinate, List<Panel>> panelsAt : panelsByPlaneAndPosition.values()) {			
			for (Entry<Coordinate,List<Panel>> entry : panelsAt.entrySet()) {
				boolean foundUlma = false;
				for (Panel panel : entry.getValue()) {
					if (panel.type == PanelType.ULMA) {
						foundUlma = true;
						if (entry.getKey().v[panel.widthAxis()] == panel.min.v[panel.widthAxis()]) {
							nrOfUlmaHorizRail.put(panel.size.width, nrOfUlmaHorizRail.containsKey(panel.size.width) ? nrOfUlmaHorizRail.get(panel.size.width) + 1 : 1);
							break;
						}
					}
				}
				if (foundUlma)
					nrOfUlmaConnectors++;
			}				
		}
			
		nrOfL70s += 2 * nrOfUlmaConnectors;
		nrOfM8HammerBolts += 2 * nrOfL70s;
		nrOfBars += 2 * nrOfUlmaConnectors;			
		nrOfNuts += 2 * nrOfL70s + 2 * nrOfBars;
		nrOfWashers += 2 * nrOfL70s + 2 * nrOfBars;

		// stam panel numbers	
		// get the connectors for stam one per left side (minimum X or Y value) coordinate used by STAM panels
		for (HashMap<Coordinate, List<Panel>> panelsAt : panelsByPlaneAndPosition.values()) {			
			for (Entry<Coordinate,List<Panel>> entry : panelsAt.entrySet()) {
				for (Panel panel : entry.getValue()) {
					if (panel.type == PanelType.STAM && entry.getKey().v[panel.widthAxis()] == panel.min.v[panel.widthAxis()]) {
						Integer width = panel.size.width - 8000; 
						nrOfHorizAluskitProfiles.put(width, nrOfHorizAluskitProfiles.containsKey(width) ? nrOfHorizAluskitProfiles.get(width) + 1 : 1);
						nrOfStamAnchors.put(panel.size.width, nrOfStamAnchors.containsKey(panel.size.width) ? nrOfStamAnchors.get(panel.size.width) + 1 : 1);
						nrOfL90s += 4;
						int holesInAnchor = ((int)(panel.size.width - 5000) / 12000) + 1; // distance from sides is 25 mm distance between is 120 mm
						nrOfM8HammerBolts +=  2 * holesInAnchor;
						nrOfNuts += 2 * holesInAnchor;
						nrOfWashers += 2 * holesInAnchor;
						break;
					}
				}
			}				
		}
			
		nrOfM8HammerBolts += 8 * nrOfL90s;
		nrOfNuts += 8 * nrOfL90s;
		nrOfWashers += 8 * nrOfL90s;

		// solarwall panel numbers	
		// get the connectors for ulma one per coordinate used by ulma panels
		for (HashMap<Coordinate, List<Panel>> panelsAt : panelsByPlaneAndPosition.values()) {			
			for (Entry<Coordinate,List<Panel>> entry : panelsAt.entrySet()) {
				for (Panel panel : entry.getValue()) {
					if (panel.type == PanelType.STAM && entry.getKey().v[panel.widthAxis()] == panel.min.v[panel.widthAxis()]) {
						nrOfOmegaProfiles.put(panel.size.width, nrOfOmegaProfiles.containsKey(panel.size.width) ? nrOfOmegaProfiles.get(panel.size.width) + 1 : 1);
						nrOfM8HammerBolts += 8;
						nrOfNuts += 8;
						nrOfWashers += 8;
						break;
					}
				}
			}				
		}		
		
		// eurecat panel numbers	
		// get the connectors for ulma one per coordinate used by ulma panels
		
	
		//Write the output to file
		output += "MaterialListing:\n";
		output += "Nr of AlusKitProfile vertical (per length):\n";
		for (Entry<Integer, Integer> entry : nrOfVertAluskitProfile.entrySet())
			output += "  * " + entry.getKey() * 0.01 + ":\t" + entry.getValue() + "\n";
		output += "Nr of Fixed Brackets:\t" + nrOfFixedBracket + "\n";
		output += "Nr of Slide Brackets:\t" + nrOfSlideBracket + "\n";
		output += "Nr of M80 Hamerbolds:\t" + nrOfM8HammerBolts + "\n";
		output += "Nr of Nuts:\t" + nrOfNuts + "\n";
		output += "Nr of Washers:\t" + nrOfWashers + "\n";
		output += "\n";
		
		//ulma parts
		output += "Nr of Ulma panels (per width x height):\n";
		for (Entry<PanelSize, Integer> entry : nrOfUlmaPanels.entrySet()) {
			output += " *" + entry.getKey().width * 0.01 + " x " + entry.getKey().height * 0.01 + ":\t" + entry.getValue() + "\n";
			totalUlmaSurface += entry.getKey().width * 0.00001 * entry.getKey().height * 0.00001 * entry.getValue();
		}
		totalCost += totalUlmaSurface * 106;
		output += "Total surface Ulma panels:\t" + String.format("%.2f", totalUlmaSurface) + "\n";
		output += "Nr of UlmaConnectors:\t" + nrOfUlmaConnectors + "\n";		
		output += "Nr of L70s:\t" + nrOfL70s + "\n";
		output += "Nr of Bars:\t" + nrOfBars + "\n";
		output += "Nr of Ulma Horizontal rails (per length):\n";
		for (Entry<Integer, Integer> rail : nrOfUlmaHorizRail.entrySet())
			output += "  * " + rail.getKey() * 0.01 + ":\t" + rail.getValue() + "\n";
		output += "Ulma part cost:" + String.format("%.2f", totalUlmaSurface * 106)  + "\n";		
		output += "\n";
		
		//Stam parts
		output += "Nr of Stam panels (per width x height):\n";
		for (Entry<PanelSize, Integer> panel : nrOfStamPanels.entrySet()) {
			output += "  * " + panel.getKey().width * 0.01 + " x " + panel.getKey().height * 0.01 + ":\t" + panel.getValue() + "\n";
			totalStamSurface += panel.getKey().width * 0.00001 * panel.getKey().height * 0.00001 * panel.getValue();
		}
		totalCost += totalStamSurface * 408;
		output += "Total surface Stam panels:\t" + String.format("%.2f", totalStamSurface) +  "\n";
		output += "Nr of StamAnchers (per length):\n";
		for (Entry<Integer, Integer> anchor : nrOfStamAnchors.entrySet())
			output += "  * " + anchor.getKey() + ":\t" + anchor.getValue() + "\n";
		output += "Nr of L90s:\t" + nrOfL90s + "\n";
		output += "Stam part cost:" + String.format("%.2f", totalStamSurface * 408)  + "\n";		
		output += "\n";

		//Solarwall parts
		output += "Nr of Solarwall panels (per width x height):\n";
		for (Entry<PanelSize, Integer> panel : nrOfSolarPanels.entrySet()) {
			output += "  * " + panel.getKey().width * 0.01 + " x " + panel.getKey().height * 0.01 + ":\t" + panel.getValue() + "\n";
			totalSolarSurface += panel.getKey().width * 0.00001 * panel.getKey().height * 0.00001 * panel.getValue();
		}
		totalCost += totalSolarSurface * 72; //TODO unclear what cost value to use
		output += "Total surface Solarwall panels:\t" + String.format("%.2f", totalSolarSurface) +  "\n";
		output += "Nr of Omega profiles (per length):\n";
		for (Entry<Integer, Integer> omega : nrOfOmegaProfiles.entrySet())
			output += "  * " + omega.getKey() * 0.01 + ":\t" + omega.getValue() + "\n";
		output += "Solarwall part cost:" + String.format("%.2f", totalSolarSurface * 72)  + "\n"; //TODO unclear what cost value to use 		

		//Eurecat parts
		output += "Nr of Eurecat panels (per width x height):\n";
		for (Entry<PanelSize, Integer> panel : nrOfSolarPanels.entrySet()) {
			output += "  * " + panel.getKey().width * 0.01 + " x " + panel.getKey().height * 0.01 + ":\t" + panel.getValue() + "\n";
			totalEurecatSurface += panel.getKey().width * 0.00001 * panel.getKey().height * 0.00001 * panel.getValue();
		}
		totalCost += totalEurecatSurface * 250; //TODO unclear what cost value to use
		output += "Total surface Eurecat panels:\t" + String.format("%.2f", totalSolarSurface) +  "\n";
		output += "Eurecat part cost:" + String.format("%.2f", totalSolarSurface * 250)  + "\n"; //TODO unclear what cost value to use 		

		output += "\n";
		output += "Total cost: " + String.format("%.2f", totalCost);
		
		//Unknown panels
		output += "Nr of unknown panels (per width x height):\n";
		for (Entry<PanelSize, Integer> panel : nrOfUnknownPanels.entrySet()) {
			output += "  * " + panel.getKey().width * 0.01 + " x " + panel.getKey().height * 0.01 + ":\t" + panel.getValue() + "\n";
		}
		
		return output;		
	}

	
	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, SObjectType settings) throws BimBotsException {
		IfcModelInterface model = input.getIfcModel();
		
		GetPanelsFromBIM(model);
		String outputString = WriteParts();
		
		GetIntersections(model);

		BimBotsOutput output = new BimBotsOutput(SchemaName.UNSTRUCTURED_UTF8_TEXT_1_0, outputString.getBytes(Charsets.UTF_8));
		output.setTitle("Materials used for Bresaer envelope");
		output.setContentType("text/plain");		
		return output;
	}
	

	@Override
	public SchemaName getOutputSchema() {
		return SchemaName.UNSTRUCTURED_UTF8_TEXT_1_0;
	}
}