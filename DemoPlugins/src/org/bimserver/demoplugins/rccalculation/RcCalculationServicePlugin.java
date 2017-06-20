package org.bimserver.demoplugins.rccalculation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.models.geometry.GeometryData;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcColourOrFactor;
import org.bimserver.models.ifc2x3tc1.IfcColourRgb;
import org.bimserver.models.ifc2x3tc1.IfcConversionBasedUnit;
import org.bimserver.models.ifc2x3tc1.IfcMaterial;
import org.bimserver.models.ifc2x3tc1.IfcMaterialDefinitionRepresentation;
import org.bimserver.models.ifc2x3tc1.IfcMaterialLayer;
import org.bimserver.models.ifc2x3tc1.IfcMaterialLayerSet;
import org.bimserver.models.ifc2x3tc1.IfcMaterialLayerSetUsage;
import org.bimserver.models.ifc2x3tc1.IfcMaterialList;
import org.bimserver.models.ifc2x3tc1.IfcMaterialSelect;
import org.bimserver.models.ifc2x3tc1.IfcNamedUnit;
import org.bimserver.models.ifc2x3tc1.IfcNormalisedRatioMeasure;
import org.bimserver.models.ifc2x3tc1.IfcPresentationStyleAssignment;
import org.bimserver.models.ifc2x3tc1.IfcPresentationStyleSelect;
import org.bimserver.models.ifc2x3tc1.IfcProject;
import org.bimserver.models.ifc2x3tc1.IfcRelAssociates;
import org.bimserver.models.ifc2x3tc1.IfcRelAssociatesMaterial;
import org.bimserver.models.ifc2x3tc1.IfcRepresentation;
import org.bimserver.models.ifc2x3tc1.IfcRepresentationItem;
import org.bimserver.models.ifc2x3tc1.IfcSIPrefix;
import org.bimserver.models.ifc2x3tc1.IfcSIUnit;
import org.bimserver.models.ifc2x3tc1.IfcStyledItem;
import org.bimserver.models.ifc2x3tc1.IfcStyledRepresentation;
import org.bimserver.models.ifc2x3tc1.IfcSurfaceStyle;
import org.bimserver.models.ifc2x3tc1.IfcSurfaceStyleElementSelect;
import org.bimserver.models.ifc2x3tc1.IfcSurfaceStyleRendering;
import org.bimserver.models.ifc2x3tc1.IfcSurfaceStyleShading;
import org.bimserver.models.ifc2x3tc1.IfcUnit;
import org.bimserver.models.ifc2x3tc1.IfcWall;
import org.bimserver.plugins.services.AbstractAddExtendedDataService;
import org.bimserver.plugins.services.BimServerClientInterface;

import com.google.common.base.Charsets;

public class RcCalculationServicePlugin extends AbstractAddExtendedDataService {

	public RcCalculationServicePlugin() {
		super("RC_CALC_OUTPUT");
		// TODO Auto-generated constructor stub
	}
	
    private float GetSIPrefixScaling(IfcSIPrefix prefix)
    {
       if (prefix == null) 
    	  return 1;
    	
       switch (prefix.getName().toUpperCase(Locale.ROOT))
       {
          case "ATTO":  return 1e-18f;
          case "FEMTO": return 1e-15f;
          case "PICO":  return 1e-12f;
          case "NANO":  return 1e-9f;
          case "MICRO": return 1e-6f;
          case "MILLI": return 1e-3f;
          case "CENTI": return 1e-2f;
          case "DECI":  return 1e-1f;
          case "DECA":  return 1e1f;
          case "HECTO": return 1e2f;
          case "KILO":  return 1e3f;
          case "MEGA":  return 1e6f;
          case "GIGA":  return 1e9f;
          case "TERA":  return 1e12f;
          case "PETA":  return 1e15f;
          case "EXA":   return 1e18f;
       }
       return 1;
    }
	
    private float LengthToM = 1.0f;
    
    private void GetLengthMeasure(IfcModelInterface model) {
	   List<IfcProject> projects = model.getAllWithSubTypes(IfcProject.class);

	   // no checking for multiple projects or missing projects in ifc file
		
	   List<IfcUnit> units = projects.get(0).getUnitsInContext().getUnits();
       for (IfcUnit unit : units)
       {
    	  if (unit instanceof IfcNamedUnit && ((IfcNamedUnit)unit).getUnitType().getName().equals("LENGTHUNIT"))
    	  {
             if (unit instanceof IfcSIUnit)
                LengthToM = GetSIPrefixScaling(((IfcSIUnit)unit).getPrefix());
             else if (unit instanceof IfcConversionBasedUnit) {
                switch (((IfcConversionBasedUnit)unit).getName().toLowerCase(Locale.ROOT)) {
                   case "inch": LengthToM = 0.0254f; break;
                   case "foot": LengthToM = 0.3048f; break;
                   case "yard": LengthToM = 0.9144f; break;
                   case "mile": LengthToM = 1609.344f; break;
                }
             }
             break;
    	  }
       }
    }    
    

	public boolean equalColors(float[][] colors)
	{
		for (int i = 0; i < 3; i++) {
			if (colors[0][i] != colors[1][i] && colors[0][i] != colors[2][i])
				return false;
		}
		return true;
	}
	
	public boolean equalColors(float[] color1, float[] color2)
	{
		for (int i = 0; i < 3; i++) {
			if (color1[i] != color2[i])
				return false;
		}
		return true;
	}
	
	public class Material {
		public String  name;
		public int     styleExpressId;
		public Float   thickness;
		public Float   lambda; // Thermal conductivity in W/mK
		public Float   Cp;     // Specific heat capacity in J/kgK
		public Float   rho;    // Volumetric mass density in kg/m3
//		public Float   muWet;  // Water vapour resistance factor wet in -
//		public Float   muDry;  // Water vapour resistance factor dry in -
		public float[] color = new float[3];	
	}
	
	public Material GetMaterial(IfcMaterial ifcMaterial) {
		Material material = new Material();
		material.name = ifcMaterial.getName();
		
		List<IfcMaterialDefinitionRepresentation> matDefRepresentations = ifcMaterial.getHasRepresentation(); 
		if (matDefRepresentations.size() == 1) {
			List<IfcRepresentation> representations = matDefRepresentations.get(0).getRepresentations();
			for (IfcRepresentation representation : representations) {
				if (!(representation instanceof IfcStyledRepresentation)) {
					// not allowed representation type used
					continue;
				}
				if (!representation.getContextOfItems().getContextType().equalsIgnoreCase("model")) {
					// Wrong type of representation context 
					continue;
				}
				
				List<IfcRepresentationItem> representationItems = representation.getItems();
				for (IfcRepresentationItem representationItem : representationItems) {
					if (!(representationItem instanceof IfcStyledItem)) {
						// not allowed representation type used
						continue;
					}
					
					List<IfcPresentationStyleAssignment> presStyleAssigns = ((IfcStyledItem)representationItem).getStyles();
					material.styleExpressId = presStyleAssigns.get(0).getExpressId();
					List<IfcPresentationStyleSelect> presStyleSelects = presStyleAssigns.get(0).getStyles();
					for (IfcPresentationStyleSelect presStyleSelect : presStyleSelects ) {
						if (presStyleSelect instanceof IfcSurfaceStyle)	{
							List<IfcSurfaceStyleElementSelect> surfaceStyleSelects = ((IfcSurfaceStyle) presStyleSelect).getStyles();
							for (IfcSurfaceStyleElementSelect surfaceStyleSelect : surfaceStyleSelects) {
								if (surfaceStyleSelect instanceof IfcSurfaceStyleRendering)
								{
									IfcColourOrFactor colourOrFactor = ((IfcSurfaceStyleRendering)surfaceStyleSelect).getDiffuseColour();
									if (colourOrFactor instanceof IfcColourRgb) {
										material.color[0] = (float) ((IfcColourRgb)colourOrFactor).getRed();
										material.color[1] = (float) ((IfcColourRgb)colourOrFactor).getGreen();
										material.color[2] = (float) ((IfcColourRgb)colourOrFactor).getBlue();										
									}
									else {
										double factor = (colourOrFactor instanceof IfcNormalisedRatioMeasure) ? ((IfcNormalisedRatioMeasure) colourOrFactor).getWrappedValue() : 1; 
										IfcColourRgb color = ((IfcSurfaceStyleRendering)surfaceStyleSelect).getSurfaceColour();
										material.color[0] = (float) (color.getRed() * factor);
										material.color[1] = (float) (color.getGreen() * factor);
										material.color[2] = (float) (color.getBlue() * factor);										
									}
								}
								else if (surfaceStyleSelect instanceof IfcSurfaceStyleShading) {
									IfcColourRgb color = ((IfcSurfaceStyleRendering)surfaceStyleSelect).getSurfaceColour();
									material.color[0] = (float) color.getRed();
									material.color[1] = (float) color.getGreen();
									material.color[2] = (float) color.getBlue();
								}	
							}
						}
					}
				}				
			}
		}
		return material;
	}

	public Material[] GetMaterial(IfcMaterialList ifcMaterialList) {
		int i= 0;
		List<IfcMaterial> ifcMaterials = ifcMaterialList.getMaterials();
		Material[] materials = new Material[ifcMaterials.size()];
		for (IfcMaterial ifcMaterial : ifcMaterials)
		{
			materials[i] = GetMaterial(ifcMaterial);
			i++;
		}
		return materials;
	}
	
	public Material[] GetMaterial(IfcMaterialLayerSetUsage ifcMaterialLayerSetUsage) {
		return GetMaterial(ifcMaterialLayerSetUsage.getForLayerSet());
	}

	public Material[] GetMaterial(IfcMaterialLayerSet ifcMaterialLayerSet) {
		int i= 0;
		List<IfcMaterialLayer> ifcMaterialLayers = ifcMaterialLayerSet.getMaterialLayers();
		Material[] materials = new Material[ifcMaterialLayers.size()];
		for (IfcMaterialLayer ifcMaterialLayer : ifcMaterialLayers)
		{
			if (ifcMaterialLayer.getMaterial() != null)
				materials[i] = GetMaterial(ifcMaterialLayer.getMaterial());
			
			else
				return null;
			materials[i].thickness = (float) ifcMaterialLayer.getLayerThickness() * LengthToM;
			i++;
		}
		return materials;
	}	
	
	public Material[] GetMaterial(IfcMaterialSelect matSelect) {
		// RelatingMaterial (IfcMaterialSelect) as IfcMaterial
		if (matSelect instanceof IfcMaterial) {
			Material[] materials = new Material[1];
			materials[0] = GetMaterial((IfcMaterial)matSelect);
			return materials;
		}

		// RelatingMaterial (IfcMaterialSelect) as IfcMaterialList
		else if (matSelect instanceof IfcMaterialList){
			return GetMaterial((IfcMaterialList)matSelect);	
		}

		// RelatingMaterial (IfcMaterialSelect) as IfcMaterialLayerSetUsage
		else if (matSelect instanceof IfcMaterialLayerSetUsage){
			return GetMaterial((IfcMaterialLayerSetUsage)matSelect);	
		}

		// RelatingMaterial (IfcMaterialSelect) as IfcMaterialLayerSet
		else if (matSelect instanceof IfcMaterialLayerSet){
			return GetMaterial((IfcMaterialLayerSet)matSelect);	
		}
		
		// RelatingMaterial (IfcMaterialSelect) as IfcMaterialLayer
		else if (matSelect instanceof IfcMaterialLayer){
			return GetMaterial((IfcMaterialLayer)matSelect);	
		}
			
		return null;
	}
	
	
	public Material[] GetMaterial(IfcWall ifcWall) {
		//Get Material from object	
		List<IfcRelAssociates> associates = ifcWall.getHasAssociations();
		for (IfcRelAssociates associate : associates)
		{
			if (!(associate instanceof IfcRelAssociatesMaterial))
				continue;
			IfcRelAssociatesMaterial relAsMaterial = (IfcRelAssociatesMaterial) associate;
			
			// Ignoring (if exists) other IfcRelAssociatesMaterial(s)
			return GetMaterial(relAsMaterial.getRelatingMaterial());
		}
		
		// Get material from object type 
		// TODO
		
		return null;
	}
	
	
	public void GetLayerThicknesses(IfcWall ifcWall, Material[] materials) {		
		//Get representation of layers (multiple representations?)
		GeometryInfo gInfo = ifcWall.getGeometry();
		if (gInfo != null) {
			GeometryData gData = gInfo.getData();				
			if (gData != null) {
				ByteBuffer materialsBuffer = ByteBuffer.wrap(gData.getMaterials());
				materialsBuffer.order(ByteOrder.LITTLE_ENDIAN);
				FloatBuffer color = materialsBuffer.asFloatBuffer();
				
				if (color == null || color.limit() == 0)
					return;
				
				ByteBuffer indicesBuffer = ByteBuffer.wrap(gData.getIndices());
				indicesBuffer.order(ByteOrder.LITTLE_ENDIAN);
				IntBuffer indices = indicesBuffer.asIntBuffer();

				ByteBuffer verticesBuffer = ByteBuffer.wrap(gData.getVertices());
				verticesBuffer.order(ByteOrder.LITTLE_ENDIAN);
				FloatBuffer vertices = verticesBuffer.asFloatBuffer();					

				int nrTriangles = indices.capacity() / 3;
									
				for (int t=0; t < nrTriangles; t++) {
					float[][] triangle = new float[3][3];
					float[][] triColor = new float[3][3];
					for (int i = 0; i < 3; i++) {
						int index =  t * 3 + i;
						triangle[i] = new float[]{
							vertices.get(indices.get(index) * 3),
							vertices.get(indices.get(index) * 3 + 1),
							vertices.get(indices.get(index) * 3 + 2)};
						triColor[i] = new float[]{
							color.get(indices.get(index) * 4),
							color.get(indices.get(index) * 4 + 1),
							color.get(indices.get(index) * 4 + 2)};
					}
					// check the triangle to have only one color (material)
					if (!equalColors(triColor))
						continue;
					
					// Get the material of the current triangle by its color
					Material curMat = null;
					for (Material mat : materials) {
						if (equalColors(triColor[0], mat.color)) {
							curMat = mat;
							break;
						}
					}
					if (curMat == null)
						continue;

					// Set the minimal edge length as thickness
					for (int i = 1; i < 3; i++) {
						float dx = triangle[i][0] - triangle[(i + 1)%3][0];
						float dy = triangle[i][1] - triangle[(i + 1)%3][1];
						float dz = triangle[i][2] - triangle[(i + 1)%3][2];						
						if (curMat.thickness == null)
							curMat.thickness = (float)Math.sqrt(dx * dx + dy * dy + dz * dz) / 1000.0f;
						else 
							curMat.thickness = Math.min(curMat.thickness, (float)Math.sqrt(dx * dx + dy * dy + dz * dz) / 1000.0f);
					}
				}
			} 
		}
	}
	
	public void GetLambdaCpRho(Material[] materials) {
		for (Material mat : materials) 
		{
			String lookUpName = mat.name.replace(',', ';');
			if (!MaterialLibrary.containsKey(lookUpName))
				continue;
			mat.rho = MaterialLibrary.get(lookUpName)[0];
			mat.lambda = MaterialLibrary.get(lookUpName)[1];
			mat.Cp = MaterialLibrary.get(lookUpName)[2];
		}
	}

	private HashMap<String, float[]> MaterialLibrary = new HashMap<String, float[]>();

	
	public void ReadMaterialLibraryFromFile() throws Exception {
		Path materialFile = getPluginContext().getRootPath().resolve("data").resolve("Materials.csv");	
		BufferedReader br = new BufferedReader(new FileReader(materialFile.toString()));

		//Read header row => no chenking since this is a fixed file
	    String line =  br.readLine();
	    
	    //Read data 
	    while((line = br.readLine()) != null) {
	        String str[] = line.split(",");
	        float[] data = new float[5];
	        
	        MaterialLibrary.put(str[0], data);
	        for (int i = 0; i < 5; i++)
	        	data[i] = Float.valueOf(str[i + 1]);
	    }
	    br.close();
	}
	
	
	
	@Override
	public void newRevision(RunningService runningService, BimServerClientInterface bimServerClientInterface, long poid,
			long roid, String userToken, long soid, SObjectType settings) throws Exception {

		SProject project = bimServerClientInterface.getServiceInterface().getProjectByPoid(poid);
		IfcModelInterface model = bimServerClientInterface.getModel(project, roid, true, false, true);

		ReadMaterialLibraryFromFile();
		GetLengthMeasure(model);

		String output = "Name\tThickness\tDensity\tThermal conductivity\tSpecific heat capacity\tThermal resistance\tHeat capacity\n";
		output += "(material)\t(mm)\trho (kg/m3)\tlambda (W/mK)\tCp (J/kgK)\tRc or Rm (m2K/W)\tKappa (J/m2K)\n";
		
		// find and go through each wall object
		List<IfcWall> allWithSubTypes = model.getAllWithSubTypes(IfcWall.class);
		for (IfcWall ifcWall : allWithSubTypes) {
			
			// Get Material
			Material[]  materials = GetMaterial(ifcWall);
			if (materials == null) {
				output += ifcWall.getGlobalId() + "\t" + ifcWall.getName() + "\t missing material definition\n\n";			
				continue;
			}

			// Get the layer thickness from geometry if not found in the material
//			if (materials[0].thickness == null)
			GetLayerThicknesses(ifcWall, materials);
			
			// Get properties (lambda, Cp and rho) of materials. If not available, skip this element 
			GetLambdaCpRho(materials);
			
			// Aggregate the layers to a single Rc value and heat capacity
			float Rc = 0.0f;
			float Kappa = 0.0f;
			float Thickness = 0.0f;
			boolean correctThickness = true;
		    boolean correctMaterialDef = true;
		    
			int i = 1;
			for (Material mat : materials) 
			{
				Float Rm = (mat.thickness == null || mat.lambda == null) ? null : mat.thickness / mat.lambda;
				Float Kappa_m = (mat.thickness == null || mat.Cp == null) ? null : mat.thickness * mat.Cp * mat.rho;
				
				//Write data of each layer
				output += "   Layer" + String.format("%02d", i) + " (" + mat.name + ")\t";
				output += mat.thickness == null ? "missing\t" : String.format("%.1f", mat.thickness * 1000) + "\t";
				output += mat.rho == null ? "missing\t" : String.format("%.0f", mat.rho) + "\t";
				output += mat.lambda == null ? "missing\t" : String.format("%.3f", mat.lambda) + "\t";
				output += mat.Cp == null ? "missing\t" : String.format("%.0f", mat.Cp) + "\t";
				output += Rm == null ? "NC\t" : String.format("%.3f", Rm) + "\t"; 
				output += Kappa_m == null ? "NC\n" : String.format("%.0f", Kappa_m) + "\n";
 
				correctThickness &= mat.thickness != null;
				correctMaterialDef &= mat.Cp != null;
				
				Thickness += mat.thickness == null ? 0 : mat.thickness;
				Rc += Rm == null ? 0 : Rm;
				Kappa += Kappa_m == null ? 0 : Kappa_m;				
				i++;
			}
			output += ifcWall.getName() + " ("+ ifcWall.getGlobalId() + ")\t";
			output += correctThickness ? String.format("%.1f", Thickness * 1000) + "\t" : "NC\t";
			output += "\t\t\t";
			output += correctMaterialDef ? String.format("%.3f", Rc) + "\t" : "NC\t"; 
			output += correctMaterialDef ? String.format("%.0f", Kappa) + "\n" : "NC\n";
			output += "\n"; 
		}
		addExtendedData(output.getBytes(Charsets.UTF_8), "ThermalProperties.txt", "Thermal properties of walls", "text/csv", bimServerClientInterface, roid);
	}
}
