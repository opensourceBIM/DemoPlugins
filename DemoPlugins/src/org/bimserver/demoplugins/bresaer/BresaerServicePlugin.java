package org.bimserver.demoplugins.bresaer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.bimserver.demoplugins.bresaer.Panel.PanelType;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcBuildingElementProxy;
import org.bimserver.models.ifc2x3tc1.IfcOpeningElement;
import org.bimserver.plugins.services.AbstractAddExtendedDataService;
import org.bimserver.plugins.services.BimServerClientInterface;

import com.google.common.base.Charsets;

public class BresaerServicePlugin extends AbstractAddExtendedDataService {
	private HashMap<Coordinate, List<Panel>>[] panelsAt = new HashMap[3];
	private List<Panel>                        unpositionedPanels;
	private EnumMap<Panel.PanelType, HashMap<PanelSize, Integer>> nrOfPanelsByTypeAndSize;
	private HashMap<PanelSize, Integer> nrOfUlmaPanels = new HashMap<PanelSize, Integer>();		
	private HashMap<PanelSize, Integer> nrOfStamPanels = new HashMap<PanelSize, Integer>();
	private HashMap<PanelSize, Integer> nrOfSolarPanels = new HashMap<PanelSize, Integer>();	
	private HashMap<PanelSize, Integer> nrOfEurecatPanels = new HashMap<PanelSize, Integer>();	
	private HashMap<PanelSize, Integer> nrOfUnknownPanels = new HashMap<PanelSize, Integer>();	

	
	public BresaerServicePlugin() {
		super("BRESAER_OUTPUT");
		// TODO Auto-generated constructor stub
	}

		
	private void AddPanelToList(HashMap<Coordinate, List<Panel>> map, Coordinate coor, Panel panel) {
		List<Panel> panels;
		if (!map.containsKey(coor))	{
			panels = new ArrayList<Panel>();
			map.put(coor, panels);
		}
		else
			panels = map.get(coor);
		
		panels.add(panel);
	}			
		
	
	private void GetPanelFromBIM(IfcModelInterface model)	{
			
		// Panels are stored as IfcBuildingElementProxy, so get all of them and loop through them for analysing each panel
		List<IfcBuildingElementProxy> allWithSubTypes = model.getAllWithSubTypes(IfcBuildingElementProxy.class);
		for (IfcBuildingElementProxy ifcProxy : allWithSubTypes) {
			//determine if the proxy is a panel (contains "initial" and "family" in the type string) or not
			if (!ifcProxy.getObjectType().contains("Initial") || !ifcProxy.getObjectType().contains("family"))
				continue; // no panel so continue to the next proxy 
						
			//create a listing of the panels based on each corner => a list contains neighbouring panel
			Panel curPanel = new Panel(ifcProxy);
			AddPanelToList(panelsAt[curPanel.normal], curPanel.min, curPanel);
			if (curPanel.normal == 0) {
				AddPanelToList(panelsAt[curPanel.normal], new Coordinate(curPanel.min.v[0], curPanel.min.v[1], curPanel.max.v[2]), curPanel);
				AddPanelToList(panelsAt[curPanel.normal], new Coordinate(curPanel.min.v[0], curPanel.max.v[1], curPanel.min.v[2]), curPanel);
				AddPanelToList(panelsAt[curPanel.normal], new Coordinate(curPanel.min.v[0], curPanel.max.v[1], curPanel.max.v[2]), curPanel);
			}	
			else if (curPanel.normal == 1) {
				AddPanelToList(panelsAt[curPanel.normal], new Coordinate(curPanel.min.v[0], curPanel.min.v[1], curPanel.max.v[2]), curPanel);
				AddPanelToList(panelsAt[curPanel.normal], new Coordinate(curPanel.max.v[0], curPanel.min.v[1], curPanel.min.v[2]), curPanel);
				AddPanelToList(panelsAt[curPanel.normal], new Coordinate(curPanel.max.v[0], curPanel.min.v[1], curPanel.max.v[2]), curPanel);
			}
			else if (curPanel.normal == 2) {
				AddPanelToList(panelsAt[curPanel.normal], new Coordinate(curPanel.min.v[0], curPanel.max.v[1], curPanel.min.v[2]), curPanel);
				AddPanelToList(panelsAt[curPanel.normal], new Coordinate(curPanel.max.v[0], curPanel.min.v[1], curPanel.min.v[2]), curPanel);
				AddPanelToList(panelsAt[curPanel.normal], new Coordinate(curPanel.max.v[0], curPanel.max.v[1], curPanel.min.v[2]), curPanel);				
			}
			
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
		int widthDir[] = { 1, 0, 0};
		int heightDir[] = { 2, 2, 1};
		
		// count nr of Aluskit vertical profiles with length X
		for (int i = 0; i < 2; i++)	{		
			HashMap<Integer, LinkedList<Integer>> lengthAt = new HashMap<Integer, LinkedList<Integer>>();
			for (Entry<Coordinate,List<Panel>> entry : panelsAt[i].entrySet()) {
				for (Panel panel : entry.getValue()) {
					LinkedList<Integer> fromTo; //even is start, odd is end of fillings with a panel
					
					//new y position add the panel height
					if (!lengthAt.containsKey(entry.getKey().v[widthDir[i]])) {
						fromTo = new LinkedList<Integer>();
						lengthAt.put(entry.getKey().v[widthDir[i]], fromTo);
						fromTo.add(panel.min.v[heightDir[i]]);
						fromTo.add(panel.max.v[heightDir[i]]);
//						output += entry.getKey().v[widthDir[i]] + " : new " + panel.min.v[2]/1000 + "->" + panel.max.v[2]/1000 + "\n";  
						break;
					}
	
					//existing y position find connecting panel height
//					output += entry.getKey().v[widthDir[i]] + " : ";
					fromTo = lengthAt.get(entry.getKey().v[widthDir[i]]);
//					for (int n = 0; n < (fromTo.size() - 1); n += 2)
//						output += fromTo.get(n)/1000 + "->" + fromTo.get(n + 1)/1000 + ", ";  

//					output += "+" + panel.min.v[2]/1000 + "->" + panel.max.v[2]/1000 + " = ";  

					//search the index above the current panel.min.z
					int index;
					for (index = 0;  index < fromTo.size() && fromTo.get(index) < panel.min.v[2]; index++);
	
					//insert if index is even and shift to the next index
					if ((index & 1) == 0) { //even
						fromTo.add(index, panel.min.v[2]);
						index++;
					}
					
					//remove all points till the end or the index points to a point larger or equal to panel.max.z 
					while (index < fromTo.size() && fromTo.get(index) <= panel.max.v[2]) {
						fromTo.remove(index);
					}
					
					//add the max of the panel if an uneven nr of points is in the list
					if ((fromTo.size() & 1) == 1)
						fromTo.add(index, panel.max.v[2]);				

//					for (int n = 0; n < (fromTo.size() - 1); n += 2)
//						output += fromTo.get(n)/1000 + "->" + fromTo.get(n + 1)/1000 + ", ";
//					output += "\n"; 
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
		for (int i = 0; i < 2; i++)	{
			for (Entry<Coordinate,List<Panel>> entry : panelsAt[i].entrySet()) {
				boolean foundUlma = false;
				for (Panel panel : entry.getValue()) {
					if (panel.type == PanelType.ULMA) {
						foundUlma = true;
						if (entry.getKey().v[i ^ 1] == panel.min.v[i ^ 1]) {
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
		for (int i = 0; i < 2; i++)	{
			for (Entry<Coordinate,List<Panel>> entry : panelsAt[0].entrySet()) {
				for (Panel panel : entry.getValue()) {
					if (panel.type == PanelType.STAM && entry.getKey().v[i ^ 1] == panel.min.v[i ^ 1]) {
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
		for (int i = 0; i < 2; i++)	{
			for (Entry<Coordinate,List<Panel>> entry : panelsAt[0].entrySet()) {
				for (Panel panel : entry.getValue()) {
					if (panel.type == PanelType.STAM && entry.getKey().v[i ^ 1] == panel.min.v[i ^ 1]) {
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
	public void newRevision(RunningService runningService, BimServerClientInterface bimServerClientInterface, long poid,
			long roid, String userToken, long soid, SObjectType settings) throws Exception {
		
		//Get the bimModel to count panels of 
		SProject project = bimServerClientInterface.getServiceInterface().getProjectByPoid(poid);
		IfcModelInterface model = bimServerClientInterface.getModel(project, roid, true, false, true);
		

		for (int i = 0; i < 3; i++)
			panelsAt[i] = new HashMap<Coordinate, List<Panel>>();
		
	
		GetPanelFromBIM(model);
		String output = WriteParts();
		
		GetIntersections(model);

		addExtendedData(output.getBytes(Charsets.UTF_8), "MaterialListing.txt", "Materials used for Bresaer envelope", "text/plain", bimServerClientInterface, roid);
	}
}

