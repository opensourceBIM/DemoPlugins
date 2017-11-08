package org.bimserver.demoplugins.spacearea;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.List;

import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcClassificationNotationSelect;
import org.bimserver.models.ifc2x3tc1.IfcClassificationReference;
import org.bimserver.models.ifc2x3tc1.IfcSpace;
import org.bimserver.plugins.SchemaName;
import org.bimserver.plugins.services.BimBotAbstractService;
import org.bimserver.utils.IfcUtils;

import com.google.common.base.Charsets;
import com.opencsv.CSVWriter;

public class SpaceAreaService extends BimBotAbstractService {

	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, SObjectType settings) throws BimBotsException {
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