package org.bimserver.demoplugins.outerarea;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Random;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.models.geometry.GeometryData;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcBoolean;
import org.bimserver.models.ifc2x3tc1.IfcObject;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.models.ifc2x3tc1.IfcProperty;
import org.bimserver.models.ifc2x3tc1.IfcPropertySet;
import org.bimserver.models.ifc2x3tc1.IfcPropertySetDefinition;
import org.bimserver.models.ifc2x3tc1.IfcPropertySingleValue;
import org.bimserver.models.ifc2x3tc1.IfcRailing;
import org.bimserver.models.ifc2x3tc1.IfcRelDefines;
import org.bimserver.models.ifc2x3tc1.IfcRelDefinesByProperties;
import org.bimserver.models.ifc2x3tc1.Tristate;
import org.bimserver.plugins.services.AbstractAddExtendedDataService;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.plugins.services.Geometry;

import com.google.common.base.Charsets;

public class OuterAreaServicePlugin extends AbstractAddExtendedDataService {

	public OuterAreaServicePlugin() {
		super("outerareanamespace");
	}

	public Tristate getProperty(IfcObject ifcObject) {
		for (IfcRelDefines ifcRelDefines : ifcObject.getIsDefinedBy()) {
			if (ifcRelDefines instanceof IfcRelDefinesByProperties) {
				IfcRelDefinesByProperties ifcRelDefinesByProperties = (IfcRelDefinesByProperties)ifcRelDefines;
				IfcPropertySetDefinition propertySetDefinition = ifcRelDefinesByProperties.getRelatingPropertyDefinition();
				if (propertySetDefinition instanceof IfcPropertySet) {
					IfcPropertySet ifcPropertySet = (IfcPropertySet)propertySetDefinition;
					for (IfcProperty ifcProperty : ifcPropertySet.getHasProperties()) {
						if (ifcProperty instanceof IfcPropertySingleValue) {
							IfcPropertySingleValue propertyValue = (IfcPropertySingleValue)ifcProperty;
							if (ifcProperty.getName().equals("IsExternal")) {
								IfcBoolean label = (IfcBoolean)propertyValue.getNominalValue();
								return label.getWrappedValue();
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	@Override
	public void newRevision(RunningService runningService, BimServerClientInterface bimServerClientInterface, long poid,
			long roid, String userToken, long soid, SObjectType settings) throws Exception {
		
		SProject project = bimServerClientInterface.getServiceInterface().getProjectByPoid(poid);
		IfcModelInterface model = bimServerClientInterface.getModel(project, roid, true, false);
		
		int nrTriangles = 0;
		int nrVertices = 0;
		int nrUsedProducts = 0;
		
		List<IfcProduct> allWithSubTypes = model.getAllWithSubTypes(IfcProduct.class);
		int i=0;
		for (IfcProduct ifcProduct : allWithSubTypes) {
//			System.out.println((100 * i / allWithSubTypes.size()) + "%");
			i++;
			if (ifcProduct instanceof IfcRailing) {
				continue;
			}
			
			Tristate isExternal = getProperty(ifcProduct);
			if (isExternal == Tristate.TRUE) {
				GeometryInfo geometryInfo = ifcProduct.getGeometry();
				if (geometryInfo != null) {
					nrTriangles += geometryInfo.getPrimitiveCount();
					GeometryData data = geometryInfo.getData();
					if (data != null) {
						nrVertices += data.getVertices().length / 12;
					}
					nrUsedProducts++;
				}

				Geometry geometry = bimServerClientInterface.getGeometry(roid, ifcProduct);
				
				if (geometry == null) {
					continue;
				}


				IntBuffer indices = geometry.getIndices();
				FloatBuffer vertices = geometry.getVertices();
				
				nrTriangles += (indices.capacity() / 3);
				nrVertices += (vertices.capacity() / 3);
				
//				for (int i=0; i<indices.capacity(); i+=3) {
//					float[][] triangle = new float[3][3];
//					for (int x=0; x<3; x++) {
//						for (int y=0; y<3; y++) {
//							triangle[x] = new float[]{
//								vertices.get(indices.get(i + x) * 3),
//								vertices.get(indices.get(i + x) * 3 + 1),
//								vertices.get(indices.get(i + x) * 3 + 2)
//							};
//						}
//					}
//				}
			}
			
		}
		float area = new Random().nextFloat();
		
		StringBuilder results = new StringBuilder();
		results.append("All IfcProduct subtypes (excluding IfcRailing) which are external\n");
		results.append("Nr products: " + nrUsedProducts + "\n");
		results.append("Nr triangles: " + nrTriangles + "\n");
		results.append("Nr vertices: " + nrVertices + "\n");
		results.append("Total area (rough estimation / random): " + area + "\n");
		
		addExtendedData(results.toString().getBytes(Charsets.UTF_8), "test.txt", "Test", "text", bimServerClientInterface, roid);
	}
}