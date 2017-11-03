package org.bimserver.demoplugins.bimbotdemo;

import java.util.List;

import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.plugins.SchemaName;
import org.bimserver.plugins.services.BimBotAbstractService;

import com.google.common.base.Charsets;

public class BimBotDemoService extends BimBotAbstractService {

	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, SObjectType settings) throws BimBotsException {
		IfcModelInterface model = input.getIfcModel();

		StringBuilder sb = new StringBuilder();
		sb.append("Number of objects: " + model.size() + "\n");

		int totalPrimitives = 0;
		List<IfcProduct> products = model.getAllWithSubTypes(IfcProduct.class);
		sb.append("Number of products: " + products.size() + "\n");
		for (IfcProduct ifcProduct : products) {
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

	@Override
	public String getOutputSchema() {
		return SchemaName.UNSTRUCTURED_UTF8_TEXT_1_0.name();
	}
}