package org.bimserver.demoplugins.digitalmason;


import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SActionState;
import org.bimserver.interfaces.objects.SDeserializerPluginConfiguration;
import org.bimserver.interfaces.objects.SLongActionState;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.interfaces.objects.SProgressTopicType;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.interfaces.objects.SRevision;
import org.bimserver.models.geometry.GeometryData;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcBoolean;
import org.bimserver.models.ifc2x3tc1.IfcConversionBasedUnit;
import org.bimserver.models.ifc2x3tc1.IfcNamedUnit;
import org.bimserver.models.ifc2x3tc1.IfcProject;
import org.bimserver.models.ifc2x3tc1.IfcProperty;
import org.bimserver.models.ifc2x3tc1.IfcPropertySet;
import org.bimserver.models.ifc2x3tc1.IfcPropertySetDefinition;
import org.bimserver.models.ifc2x3tc1.IfcPropertySingleValue;
import org.bimserver.models.ifc2x3tc1.IfcRelDefines;
import org.bimserver.models.ifc2x3tc1.IfcRelDefinesByProperties;
import org.bimserver.models.ifc2x3tc1.IfcSIPrefix;
import org.bimserver.models.ifc2x3tc1.IfcSIUnit;
import org.bimserver.models.ifc2x3tc1.IfcUnit;
import org.bimserver.models.ifc2x3tc1.IfcValue;
import org.bimserver.models.ifc2x3tc1.IfcWall;
import org.bimserver.models.ifc2x3tc1.Tristate;
import org.bimserver.plugins.deserializers.Deserializer;
import org.bimserver.plugins.deserializers.DeserializerPlugin;
import org.bimserver.plugins.services.AbstractModifyRevisionService;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.plugins.services.Flow;
import org.bimserver.shared.exceptions.UserException;
import org.bimserver.utils.Formatters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MasonryPlugin extends AbstractModifyRevisionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MasonryPlugin.class);
	public MasonryPlugin() {
		super();
		// TODO Auto-generated constructor stub
	}
				
	@Override
	public ProgressType getProgressType() {
		return ProgressType.UNKNOWN;
	}	
		
	private boolean isExternal(IfcWall wall) {
		List<IfcRelDefines> definedByes = wall.getIsDefinedBy();
		for (IfcRelDefines definedBy : definedByes) {
			if (definedBy instanceof IfcRelDefinesByProperties)	{
				IfcPropertySetDefinition relPropDef = ((IfcRelDefinesByProperties)definedBy).getRelatingPropertyDefinition();
				if (relPropDef instanceof IfcPropertySet) {
					List<IfcProperty> properties = ((IfcPropertySet) relPropDef).getHasProperties();
					for (IfcProperty property : properties) {
						if (property instanceof IfcPropertySingleValue &&  property.getName().equals("IsExternal")) {
							IfcValue value = ((IfcPropertySingleValue)property).getNominalValue();
							if (value instanceof IfcBoolean) {
								return ((IfcBoolean)value).getWrappedValue() == Tristate.TRUE;
							}
						}	
					}					
				}
			}
		}
		return false;
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
	

	public void LayerOrder() {
	}
	
	////////////////////////////////////////////////////////////////////////////////////
	//Determine the (smallest/perpendicular) distance between a point and a plane 
	//The plane can be given as a origin and normal or as a Triangle3D or Polygon3D
	private double PointToPlaneDistance(Corner3D point, Corner3D planeOrigin, Normal planeNormal)
	{
		return planeNormal.x * (point.x - planeOrigin.x) +
			   planeNormal.y * (point.y - planeOrigin.y) +
			   planeNormal.z * (point.z - planeOrigin.z);
	}


/* public void ReadBrickFile(BimServerClientInterface bimServerClientInterface, IfcModelInterface model, SProject project) throws Exception {
		SerializerPlugin serializerPlugin = getPluginContext().getSerializerPlugin("org.bimserver.ifc.step.deserializer.Ifc2x3tc1StepSerializerPlugin");
		Serializer serializer = serializerPlugin.createSerializer(null);
		serializer.init(model, projectInfo, pluginManager, normalizeOids);.init(model.getPackageMetaData());
		Path brickFilePath = getPluginContext().getRootPath().resolve("data").resolve("Waalformaat.ifc");
		
		File f = brickFilePath.toFile();
		if (f.exists() && !f.isDirectory()) {
	
			SDeserializerPluginConfiguration deserializerForExtension = bimServerClientInterface.getServiceInterface().getSuggestedDeserializerForExtension("ifc", project.getOid());
			
			System.out.println("Checking in " + f.toString() + " - " + Formatters.bytesToString(f.length()));
			try {
				bimServerClientInterface.checkin(project.getOid(), "", deserializerForExtension.getOid(), false, Flow.SYNC, resultPath);
				bimServerClientInterface.commit(model, "Added bricks");
			} catch (UserException e) {
				e.printStackTrace();
			}		
		}			
	}
*/	
	
	public Polygon3D GetBrickPolygon(IfcWall ifcWall, ModelGeometry geom) {		
		//Get representation of layers (multiple representations?)
		List<Layer> layers = new LinkedList<Layer>();
		Layer[] orderedLayers= new Layer[4];
		
		List<Plane> planes = new LinkedList<Plane>();
		GeometryInfo gInfo = ifcWall.getGeometry();
		if (gInfo != null) {
			GeometryData gData = gInfo.getData();				
			if (gData != null) {		
				ByteBuffer materialsBytes = ByteBuffer.wrap(gData.getMaterials());
				materialsBytes.order(ByteOrder.LITTLE_ENDIAN);
				FloatBuffer colors = materialsBytes.asFloatBuffer();		
				
				if (colors == null || colors.limit() == 0)
					return null;
				
				ByteBuffer indicesBytes = ByteBuffer.wrap(gData.getIndices());
				indicesBytes.order(ByteOrder.LITTLE_ENDIAN);
				IntBuffer indices = indicesBytes.asIntBuffer();

				ByteBuffer verticesBytes = ByteBuffer.wrap(gData.getVertices());
				verticesBytes.order(ByteOrder.LITTLE_ENDIAN);
				FloatBuffer vertices = verticesBytes.asFloatBuffer();					
						
				Layer curLayer;
				Plane curPlane = null;
				Polygon3D  curPoly;
				Normal normal;
				Corner3D corn[] = new Corner3D[3];
				Edge3D   edges[] = new Edge3D[3];
				
				for (int i = 0; i < indices.limit(); i += 3) {
					int i1 = indices.get(i);
					int i2 = indices.get(i + 1);
					int i3 = indices.get(i + 2);
					//get all corner colors (single color triangle)
					float triColor[][] = {{colors.get(i1 * 4), colors.get((i1 * 4) + 1), colors.get((i1 * 4) + 2)}, 
										  {colors.get(i2 * 4), colors.get((i2 * 4) + 1), colors.get((i2 * 4) + 2)},
										  {colors.get(i3 * 4), colors.get((i3 * 4) + 1), colors.get((i3 * 4) + 2)}};

					// check the triangle to have only one color (material)
					if (!equalColors(triColor))
						continue;
					
					// Add the new corners
					corn[0] = geom.NewCorner(vertices.get(i1 * 3), vertices.get((i1 * 3) + 1), vertices.get((i1 * 3) + 2));
					corn[1] = geom.NewCorner(vertices.get(i2 * 3), vertices.get((i2 * 3) + 1), vertices.get((i2 * 3) + 2));
					corn[2] = geom.NewCorner(vertices.get(i3 * 3), vertices.get((i3 * 3) + 1), vertices.get((i3 * 3) + 2));

					// Add the new edges
					edges[0] = geom.NewEdge(corn[0], corn[1]);
					edges[1] = geom.NewEdge(corn[0], corn[2]);
					edges[2] = geom.NewEdge(corn[1], corn[2]);
				
					// Get the normal of the current triangle (created of edges)
					normal = geom.CalcNormal(edges);

					// find the matching layer by color
					curLayer = null;
					for (Layer lay : layers) {	
						if (lay.color[0] == triColor[0][0] &&
							lay.color[1] == triColor[0][1] &&
							lay.color[2] == triColor[0][2]) {
							curLayer = lay;
							break;
						}
					}			
						
					// create new layer if no matching layer exists
					if (curLayer == null) {
						curLayer = new Layer();
						curLayer.color[0] = triColor[0][0]; 
						curLayer.color[1] = triColor[0][1];
						curLayer.color[2] = triColor[0][2];
						layers.add(curLayer);
					}

					// create a new list of polygons for the normal if this does not exist
					if (!curLayer.polygons.containsKey(normal))
						curLayer.polygons.put(normal, new LinkedList<Polygon3D>());
					
					// Get the list of polygons for this normal (or create if not exists)
					LinkedList<Polygon3D> list = geom.polygons.get(normal);
					if (list == null) {
						list = new LinkedList<Polygon3D>();
						geom.polygons.put(normal, list);
					}

					// Find the polygon to add the edges
					curPoly = null;
					curPlane = null;
					for (Polygon3D poly : list) {
						// Check if planes are the same (not parallel so, any point of polygon should be on plane) 
						if (geom.IsPointOnPlane(edges[0].corner[0], poly)) {
							curPlane = poly.onPlane;
							if (poly.ofLayer == curLayer) {
								geom.InsertTriangleToPolygon(poly, edges, normal); 
								curPoly = poly;
								break;
							}
						}
					}

					// the current polygon, became empty (by adding the already existing lines) 
					if (curPoly!= null && curPoly.edges.size() == 0) {
						//remove the polygon from the layer and plane if only reference to plane 
						curPoly.ofLayer.polygons.get(normal).remove(curPoly);
						if (curPlane.layers.size() == 1)
							planes.remove(curPlane);
						continue;
					}
					
					// no plane found, so create a new plane
					if (curPlane == null) {
						curPlane = new Plane(normal);
						curPlane.layers.add(curLayer);
						planes.add(curPlane);
					}
					
					// no polygon found to add the edges, so create a new polygon
					if (curPoly == null) {
						curPoly = geom.NewPolygon(edges, normal);
						list.addLast(curPoly);
						curPoly.ofLayer = curLayer;
						curPoly.onPlane = curPlane;
						curPlane.layers.add(curLayer);
					}					
				
					// add the polygon to the normal list if it does not yet exist.
					if (!curLayer.polygons.get(normal).contains(curPoly))
						curLayer.polygons.get(normal).add(curPoly);				
				}
				
				if (layers.size() != 4) {
					System.out.println(ifcWall.getName() + "  doesn't have 4 layers needed for this plugin!");
					return null;
				}
				
					
				//remove all planes that do not have 2 layers or where a layer does not contain 2 polygons for the planes normal 
				Iterator<Plane> it = planes.iterator();
				while (it.hasNext()) {
				    Plane plane = it.next();
				    if (plane.layers.size() != 2 || 
				        plane.layers.get(0).polygons.get(plane.normal).size() != 2 ||
				        plane.layers.get(1).polygons.get(plane.normal).size() != 2)
				    	it.remove();
				}

				// fix the polygons
				for (Layer lay : layers) {
					for (Map.Entry<Normal, LinkedList<Polygon3D>> entry : lay.polygons.entrySet()) {
						for (Polygon3D poly : entry.getValue())	{
							poly.Correct(geom);
						}
					}
				}				
				
				// assign layer orders
				if (planes.size() != 3) {
					System.out.println("Expected only 3 planes for an object");
					return null;
				}
				
				//find the order of layers
				curPlane = planes.get(0);
				curLayer = curPlane.layers.get(0);
				normal = curPlane.normal;
				
				//traverse to first/last layer
				while (curPlane.layers.size() == 2) {
					curPlane = curLayer.PolygonAt(normal, (curLayer.PolygonAt(normal, 0).onPlane != curPlane) ? 0 : 1).onPlane;
					curLayer = curPlane.layers.get((curLayer != curPlane.layers.get(0)) ? 0 : 1);
				}
				
				//add order by traversing back
				curLayer = curPlane.layers.get(0);
				int order = 0;
				while (order == 0 || curPlane.layers.size() == 2) {
					orderedLayers[order] = curLayer;
					order++;
					curLayer.thickness = ModelGeometry.PointToPlaneDistance(curLayer.PolygonAt(normal, 0).EdgeAt(0).corner[0], 
							                                                curLayer.PolygonAt(normal, 1).EdgeAt(0).corner[0],
							                                                normal);
					curPlane = curLayer.PolygonAt(normal, (curLayer.PolygonAt(normal, 0).onPlane != curPlane) ? 0 : 1).onPlane;								
					curLayer = curPlane.layers.get((curLayer != curPlane.layers.get(0)) ? 0 : 1);					
				}		
				
				// return the outside layer by thickness of inner 2 layer (outside to inside => thin to thick)
				if (orderedLayers[1].thickness < orderedLayers[2].thickness)
					return orderedLayers[0].PolygonAt(normal, (orderedLayers[0].PolygonAt(normal, 0).onPlane.layers.size() == 1) ? 0 : 1);
				else
					return orderedLayers[3].PolygonAt(normal, (orderedLayers[3].PolygonAt(normal, 0).onPlane.layers.size() == 1) ? 0 : 1);
			}
		}
		return null;
	}
	
	
	public void AddBricks(IfcModelInterface model) {
		
	}
	
	
	@Override
	public void newRevision(RunningService runningService, BimServerClientInterface bimServerClientInterface, long poid,
			long roid, String userToken, long soid, SObjectType settings) throws Exception {
		
		SRevision revision = bimServerClientInterface.getServiceInterface().getRevision(roid);
		if (revision.getComment().equals("Added masonry")) {
			LOGGER.info("Skipping new revision because seems to be generated by the digitalMason plugin");
			return;
		}	
		Date startDate = new Date();
		Long topicId = bimServerClientInterface.getRegistry().registerProgressOnRevisionTopic(SProgressTopicType.RUNNING_SERVICE, poid, roid, "Running digital mason");
		SLongActionState state = new SLongActionState();
		state.setTitle("Added masonry");
		state.setState(SActionState.STARTED);
		state.setProgress(-1);
		state.setStart(startDate);
		bimServerClientInterface.getRegistry().updateProgressTopic(topicId, state);
		
//		SService service = bimServerClientInterface.getServiceInterface().getService(soid);
		
		SProject project = bimServerClientInterface.getServiceInterface().getProjectByPoid(poid);
		final IfcModelInterface model = bimServerClientInterface.getModel(project, roid, true, false, true);
		
		List<IfcProject> ifcProjects = model.getAll(IfcProject.class);
		
		if (ifcProjects == null || ifcProjects.size() != 1) {
			System.out.println("Ifc file misses or has duplicate ifcProject entities.");
			return;
		}
		
		if (!ifcProjects.get(0).getGlobalId().equals("344O7vICcwH8qAEnwJDjSU")) {
			System.out.println("Wall definition not supported (yet).");
			return;
		}
/*		
		GetLengthMeasure(model);
		ModelGeometry modelGeom = new ModelGeometry();	
				
		// find and go through each wall object
		List<IfcWall> allWithSubTypes = model.getAllWithSubTypes(IfcWall.class);
		int i = 0;
		for (IfcWall ifcWall : allWithSubTypes) {
			//exclude internal walls not active for current model
//			if (!isExternal(ifcWall))
//				continue;
			
			Polygon3D polygon; // = GetBrickPolygon(ifcWall, modelGeom);
			
			//temp fixed solution for  getting the polygons
			Corner3D[] corner = new Corner3D[8];
			Edge3D[]   edge = new Edge3D[8];					
			
			switch(i) {
			case 0:
				corner[0] = modelGeom.NewCorner(1804.52, 17214.43, 0);
				corner[1] = modelGeom.NewCorner(1804.52, 17214.43, 2700);
				corner[0] = modelGeom.NewCorner(1804.52, 22214.43, 0);
				corner[1] = modelGeom.NewCorner(1804.52, 22214.43, 2700);
				edge[0] = modelGeom.NewEdge(corner[0], corner[1]);
				polygon.edges.add(edge[0]);
				break;			 
			}

			if (polygon!=null) {
/*				for (Edge3D edge : polygon.edges) {
					System.out.println("[" + edge.corner[0].x + ", " + 
						                 edge.corner[0].y + ", " + 
						                 edge.corner[0].z + "] -> [" +
						                 edge.corner[1].x + ", " + 
						                 edge.corner[1].y + ", " + 
						                 edge.corner[1].z + "]" );
				}				
*/
				// Add rollaag
				//TODO later
					
				
						
/*					
				// Add bricks
				model.createAndAdd(IfcObject);
				#53 = IFCPRODUCTDEFINITIONSHAPE($, $, (#54));
				#54 = IFCSHAPEREPRESENTATION(#20, 'Body', 'Brep', (#82));
				#55 = IFCCLOSEDSHELL((#62, #67, #72, #75, #78, #81));
				#56 = IFCPOLYLOOP((#57, #58, #59, #60));
				#57 = IFCCARTESIANPOINT((0., 100., 0.));
				#58 = IFCCARTESIANPOINT((0., 0., 0.));
				#59 = IFCCARTESIANPOINT((0., 0., 50.00000000000001));
				#60 = IFCCARTESIANPOINT((0., 100., 50.00000000000001));
				#61 = IFCFACEOUTERBOUND(#56, .T.);
				#62 = IFCFACE((#61));
				#63 = IFCPOLYLOOP((#64, #65, #60, #59));
				#64 = IFCCARTESIANPOINT((209.9999999999999, 0., 50.00000000000001));
				#65 = IFCCARTESIANPOINT((209.9999999999999, 100., 50.00000000000001));
				#66 = IFCFACEOUTERBOUND(#63, .T.);
				#67 = IFCFACE((#66));
				#68 = IFCPOLYLOOP((#69, #70, #65, #64));
				#69 = IFCCARTESIANPOINT((209.9999999999999, 0., 0.));
				#70 = IFCCARTESIANPOINT((209.9999999999999, 100., 0.));
				#71 = IFCFACEOUTERBOUND(#68, .T.);
				#72 = IFCFACE((#71));
				#73 = IFCPOLYLOOP((#70, #57, #60, #65));
				#74 = IFCFACEOUTERBOUND(#73, .T.);
				#75 = IFCFACE((#74));
				#76 = IFCPOLYLOOP((#58, #69, #64, #59));
				#77 = IFCFACEOUTERBOUND(#76, .T.);
				#78 = IFCFACE((#77));
				#79 = IFCPOLYLOOP((#70, #69, #58, #57));
				#80 = IFCFACEOUTERBOUND(#79, .T.);
				#81 = IFCFACE((#80));
				//first color
				#82 = IFCFACETEDBREP(#55);
				#83 = IFCSTYLEDITEM(#82, (#84), $);
				#84 = IFCPRESENTATIONSTYLEASSIGNMENT((#85));
				#85 = IFCSURFACESTYLE($, .POSITIVE., (#86));
				#86 = IFCSURFACESTYLESHADING(#87);
				#87 = IFCCOLOURRGB($, 1., 1., 1.);
				//second color
				#102 = IFCFACETEDBREP(#55);
				#103 = IFCSTYLEDITEM(#102, (#104), $);
				#104 = IFCPRESENTATIONSTYLEASSIGNMENT((#105));
				#105 = IFCSURFACESTYLE($, .POSITIVE., (#106));
				#106 = IFCSURFACESTYLESHADING(#107);
				#107 = IFCCOLOURRGB($, 1., 0., 0.);
				
				//Add correct bricks
				#170 = IFCBUILDINGELEMENTPROXY('16eDGBvezDC90nukMwid5A', #2, 'Waalformaat strek', 'Description of Object', $, #171, #176, $, $);
				#171 = IFCLOCALPLACEMENT(#38, #172);
				#172 = IFCAXIS2PLACEMENT3D(#173, #174, #175);
				#173 = IFCCARTESIANPOINT((0., 319.9999999999999, 0.));
				#174 = IFCDIRECTION((0., 0., 1.));
				#175 = IFCDIRECTION((8.326672684688674E-17, -1., 0.));
				#176 = IFCPRODUCTDEFINITIONSHAPE($, $, (#177));
				#177 = IFCSHAPEREPRESENTATION(#20, 'Body', 'Brep', (#82));
				
				//Add wrong bricks
				#170 = IFCBUILDINGELEMENTPROXY('16eDGBvezDC90nukMwid5A', #2, 'Waalformaat strek', 'Description of Object', $, #171, #176, $, $);
				#171 = IFCLOCALPLACEMENT(#38, #172);
				#172 = IFCAXIS2PLACEMENT3D(#173, #174, #175);
				#173 = IFCCARTESIANPOINT((0., 319.9999999999999, 0.));
				#174 = IFCDIRECTION((0., 0., 1.));
				#175 = IFCDIRECTION((8.326672684688674E-17, -1., 0.));
				#176 = IFCPRODUCTDEFINITIONSHAPE($, $, (#177));
				#177 = IFCSHAPEREPRESENTATION(#20, 'Body', 'Brep', (#82));
*				
				i++;
			}
		}
		*/
		DeserializerPlugin deserializerPlugin = getPluginContext().getDeserializerPlugin("org.bimserver.ifc.step.deserializer.Ifc2x3tc1StepDeserializerPlugin", true);
		Deserializer deserializer = deserializerPlugin.createDeserializer(null);
		deserializer.init(model.getPackageMetaData());
		Path resultPath = getPluginContext().getRootPath().resolve("data").resolve("BrickedModel.ifc");
		File f = resultPath.toFile();
		if (f.exists() && !f.isDirectory()) {
	
			SDeserializerPluginConfiguration deserializerForExtension = bimServerClientInterface.getServiceInterface().getSuggestedDeserializerForExtension("ifc", project.getOid());
			System.out.println("Checking in " + f.toString() + " - " + Formatters.bytesToString(f.length()));
			try {
				bimServerClientInterface.checkin(project.getOid(), "", deserializerForExtension.getOid(), false, Flow.SYNC, resultPath);
			} catch (UserException e) {
				e.printStackTrace();
			}		
		}

	}
}
