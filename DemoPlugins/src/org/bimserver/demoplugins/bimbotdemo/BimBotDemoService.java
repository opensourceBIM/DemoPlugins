package org.bimserver.demoplugins.bimbotdemo;

/******************************************************************************
 * Copyright (C) 2009-2018  BIMserver.org
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see {@literal<http://www.gnu.org/licenses/>}.
 *****************************************************************************/

import java.util.List;

import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.emf.IdEObject;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.models.ifc2x3tc1.IfcRoot;
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
		int ifcRoot = 0;
		for (IdEObject idEObject : model.getValues()) {
			if (idEObject instanceof IfcRoot) {
				ifcRoot++;
			}
		}
		for (IfcProduct ifcProduct : products) {
			GeometryInfo geometry = ifcProduct.getGeometry();
			if (geometry != null) {
				totalPrimitives += geometry.getPrimitiveCount();
			}
		}
		
		sb.append("Number of triangles: " + totalPrimitives + "\n");
		sb.append("Number of IfcRoot objects (with GlobalId): " + ifcRoot + "\n");
		sb.append("Number of objects: " + model.size() + "\n");
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