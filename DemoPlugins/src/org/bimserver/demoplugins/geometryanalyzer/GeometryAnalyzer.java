package org.bimserver.demoplugins.geometryanalyzer;

import org.bimserver.bimbots.BimBotContext;
import org.bimserver.bimbots.BimBotsException;
import org.bimserver.bimbots.BimBotsInput;
import org.bimserver.bimbots.BimBotsOutput;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.emf.Schema;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.models.store.ExtendedDataSchemaType;
import org.bimserver.plugins.SchemaName;
import org.bimserver.plugins.services.BimBotAbstractService;

public class GeometryAnalyzer extends BimBotAbstractService {

	@Override
	public BimBotsOutput runBimBot(BimBotsInput input, BimBotContext bimBotContext, SObjectType settings) throws BimBotsException {
		IfcModelInterface model = input.getIfcModel();
		return null;
	}

	@Override
	public String getOutputSchema() {
		return SchemaName.CSV_TABLE_1_0.name();
	}
}
