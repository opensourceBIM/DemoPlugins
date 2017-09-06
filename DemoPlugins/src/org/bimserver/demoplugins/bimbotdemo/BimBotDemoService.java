package org.bimserver.demoplugins.bimbotdemo;

import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.models.store.ObjectType;
import org.bimserver.plugins.SchemaName;
import org.bimserver.plugins.services.BimBotAbstractService;

import com.google.common.base.Charsets;

public class BimBotDemoService extends BimBotAbstractService {

	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, ObjectType settings) throws BimBotsException {
		IfcModelInterface model = input.getIfcModel();
		
		StringBuilder sb = new StringBuilder();
		sb.append("Number of objects: " + model.size() + "\n");
		
		int totalPrimitives = 0;
		for (IfcProduct ifcProduct : model.getAllWithSubTypes(IfcProduct.class)) {
			GeometryInfo geometry = ifcProduct.getGeometry();
			if (geometry != null) {
				totalPrimitives += geometry.getPrimitiveCount();
			}
		}
		
		sb.append("Number of triangles: " + totalPrimitives + "\n");
		BimBotsOutput output = new BimBotsOutput(SchemaName.UNSTRUCTURED_UTF8_TEXT_1_0, sb.toString().getBytes(Charsets.UTF_8));
		output.setTitle("BimBotDemoService Results");
		output.setContentType("text/plain");
		return output;
	}
}