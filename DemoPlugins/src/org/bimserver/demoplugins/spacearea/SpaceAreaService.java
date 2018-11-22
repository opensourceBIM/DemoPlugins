package org.bimserver.demoplugins.spacearea;

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

import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.List;

import org.bimserver.bimbots.BimBotContext;
import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcClassificationNotationSelect;
import org.bimserver.models.ifc2x3tc1.IfcClassificationReference;
import org.bimserver.models.ifc2x3tc1.IfcSpace;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.SchemaName;
import org.bimserver.plugins.services.BimBotAbstractService;
import org.bimserver.utils.IfcUtils;

import com.google.common.base.Charsets;
import com.opencsv.CSVWriter;

public class SpaceAreaService extends BimBotAbstractService {

	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, BimBotContext bimBotContext, PluginConfiguration pluginConfiguration) throws BimBotsException {
		IfcModelInterface model = input.getIfcModel();

		StringWriter stringWriter = new StringWriter();
		try (CSVWriter csvWriter = new CSVWriter(stringWriter)) {
			csvWriter.writeNext(new String[]{
				"GUID",
				"Name",
				"Classifications",
				"Area (m2)"
			});
			
			DecimalFormat df = new DecimalFormat();
			df.setMaximumFractionDigits(2);
			
			for (IfcSpace ifcSpace : model.getAll(IfcSpace.class)) {
				List<IfcClassificationNotationSelect> classifications = IfcUtils.getClassifications(ifcSpace, model);
				StringBuilder classificationsString = new StringBuilder();
				for (IfcClassificationNotationSelect ifcClassificationNotationSelect : classifications) {
					if (ifcClassificationNotationSelect instanceof IfcClassificationReference) {
						classificationsString.append(((IfcClassificationReference) ifcClassificationNotationSelect).getName() + ", ");
					}
				}
				GeometryInfo geometry = ifcSpace.getGeometry();
				double area = 0;
				if (geometry != null) {
					area = geometry.getArea();
				}
				csvWriter.writeNext(new String[]{
					ifcSpace.getGlobalId(),
					ifcSpace.getName(),
					classificationsString.toString(),
					df.format(area)
				});
			}			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		BimBotsOutput output = new BimBotsOutput(SchemaName.CSV_TABLE_1_0, stringWriter.toString().getBytes(Charsets.UTF_8));
		output.setTitle("Space/Area calculator service");
		output.setContentType("text/csv");
		return output;
	}

	@Override
	public String getOutputSchema() {
		return SchemaName.CSV_TABLE_1_0.name();
	}
}