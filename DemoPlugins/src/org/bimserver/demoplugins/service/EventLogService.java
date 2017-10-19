package org.bimserver.demoplugins.service;

/******************************************************************************
 * Copyright (C) 2009-2017  BIMserver.org
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

import org.bimserver.demoplugins.service.planner.EventLog;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.models.store.ObjectDefinition;
import org.bimserver.models.store.ParameterDefinition;
import org.bimserver.models.store.PrimitiveDefinition;
import org.bimserver.models.store.PrimitiveEnum;
import org.bimserver.models.store.StoreFactory;
import org.bimserver.models.store.StringType;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.services.AbstractAddExtendedDataService;
import org.bimserver.plugins.services.BimServerClientInterface;

import com.google.common.base.Charsets;

public class EventLogService extends AbstractAddExtendedDataService {
	private static final String NAMESPACE = "http://bimserver.org/eventlog";

	public EventLogService() {
		super(NAMESPACE);
	}
	
	@Override
	public void newRevision(RunningService runningService, BimServerClientInterface bimServerClientInterface, long poid, long roid, String userToken, long soid, SObjectType settings) throws Exception {
		SProject project = bimServerClientInterface.getServiceInterface().getProjectByPoid(poid);
		IfcModelInterface model = bimServerClientInterface.getModel(project, roid, true, false, true);
		
		PluginConfiguration pluginConfiguration = new org.bimserver.plugins.PluginConfiguration(settings);
		
		String nlsfbType = pluginConfiguration.getString("nlsfb");
		String materialType = pluginConfiguration.getString("material");

		EventLog eventLog = new EventLog(model, nlsfbType, materialType);
		
		String csvString = eventLog.toCsvString();
		
		addExtendedData(csvString.getBytes(Charsets.UTF_8), "eventlog.csv", "Eventlog", "text/csv", bimServerClientInterface, roid);
	}
	
	@Override
	public ObjectDefinition getSettingsDefinition() {
		ObjectDefinition settings = StoreFactory.eINSTANCE.createObjectDefinition();
		
		PrimitiveDefinition stringType = StoreFactory.eINSTANCE.createPrimitiveDefinition();
		stringType.setType(PrimitiveEnum.STRING);
		
		StringType nlsfbDefaultValue = StoreFactory.eINSTANCE.createStringType();
		nlsfbDefaultValue.setValue("[ArchiCADProperties]Layer");

		StringType materialDefaultValue = StoreFactory.eINSTANCE.createStringType();
		materialDefaultValue.setValue("[ArchiCADProperties]Building Material / Composite / Profile / Fill");
		
		ParameterDefinition nlsfb = StoreFactory.eINSTANCE.createParameterDefinition();
		nlsfb.setName("nlsfb");
		nlsfb.setDefaultValue(nlsfbDefaultValue);
		nlsfb.setType(stringType);

		ParameterDefinition material = StoreFactory.eINSTANCE.createParameterDefinition();
		material.setName("material");
		material.setDefaultValue(materialDefaultValue);
		material.setType(stringType);
		
		settings.getParameters().add(nlsfb);
		settings.getParameters().add(material);
		
		return settings;
	}
}