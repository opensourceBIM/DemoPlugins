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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.List;

import javax.activation.DataHandler;

import org.bimserver.interfaces.objects.SActionState;
import org.bimserver.interfaces.objects.SDeserializerPluginConfiguration;
import org.bimserver.interfaces.objects.SInternalServicePluginConfiguration;
import org.bimserver.interfaces.objects.SLongActionState;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.interfaces.objects.SProgressTopicType;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.interfaces.objects.SSerializerPluginConfiguration;
import org.bimserver.models.log.AccessMethod;
import org.bimserver.models.store.ObjectDefinition;
import org.bimserver.models.store.ParameterDefinition;
import org.bimserver.models.store.PrimitiveDefinition;
import org.bimserver.models.store.PrimitiveEnum;
import org.bimserver.models.store.ServiceDescriptor;
import org.bimserver.models.store.StoreFactory;
import org.bimserver.models.store.StringType;
import org.bimserver.models.store.Trigger;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.PluginContext;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.plugins.services.NewRevisionHandler;
import org.bimserver.plugins.services.ServicePlugin;
import org.bimserver.shared.ChannelConnectionException;
import org.bimserver.shared.UserTokenAuthentication;
import org.bimserver.shared.exceptions.BimServerClientException;
import org.bimserver.shared.exceptions.PluginException;
import org.bimserver.shared.exceptions.PublicInterfaceNotFoundException;
import org.bimserver.shared.exceptions.ServerException;
import org.bimserver.shared.exceptions.ServiceException;
import org.bimserver.shared.exceptions.UserException;
import org.bimserver.utils.InputStreamDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloneService extends ServicePlugin {

	private static final Logger LOGGER = LoggerFactory.getLogger(CloneService.class);

	@Override
	public void init(PluginContext pluginContext) throws PluginException {
		super.init(pluginContext);
	}

	@Override
	public ObjectDefinition getSettingsDefinition() {
		ObjectDefinition objectDefinition = StoreFactory.eINSTANCE.createObjectDefinition();
		
		PrimitiveDefinition stringType = StoreFactory.eINSTANCE.createPrimitiveDefinition();
		stringType.setType(PrimitiveEnum.STRING);
		
		StringType defaultProjectName = StoreFactory.eINSTANCE.createStringType();
		defaultProjectName.setValue("");
		
		ParameterDefinition projectNameParameter = StoreFactory.eINSTANCE.createParameterDefinition();
		projectNameParameter.setName("projectName");
		projectNameParameter.setDescription("Name of the local project to put the revisions in");
		projectNameParameter.setRequired(true);
		projectNameParameter.setType(stringType);
		projectNameParameter.setDefaultValue(defaultProjectName);
		
		objectDefinition.getParameters().add(projectNameParameter);
		
		return objectDefinition;
	}

	@Override
	public void register(long uoid, SInternalServicePluginConfiguration internalServicePluginConfiguration, final PluginConfiguration pluginConfiguration) {
		ServiceDescriptor serviceDescriptor = StoreFactory.eINSTANCE.createServiceDescriptor();
		serviceDescriptor.setProviderName("BIMserver");
		serviceDescriptor.setIdentifier("" + internalServicePluginConfiguration.getOid());
		serviceDescriptor.setName("Clone Service");
		serviceDescriptor.setDescription("Clone Service");
		serviceDescriptor.setReadRevision(true);
		serviceDescriptor.setNotificationProtocol(AccessMethod.INTERNAL);
		serviceDescriptor.setTrigger(Trigger.NEW_REVISION);
		registerNewRevisionHandler(uoid, serviceDescriptor, new NewRevisionHandler() {
			@Override
			public void newRevision(BimServerClientInterface bimServerClientInterface, long poid, long roid, String userToken, long soid, SObjectType settings) throws ServerException, UserException {
				Date startDate = new Date();
				Long topicId = null;
				try {
					topicId = bimServerClientInterface.getRegistry().registerProgressOnRevisionTopic(SProgressTopicType.RUNNING_SERVICE, poid, roid,
							"Running Clone Service");
					SLongActionState state = new SLongActionState();
					state.setTitle("Clone Service");
					state.setState(SActionState.STARTED);
					state.setProgress(-1);
					state.setStart(startDate);
					bimServerClientInterface.getRegistry().updateProgressTopic(topicId, state);

					SSerializerPluginConfiguration stepSerializerRemote = bimServerClientInterface.getServiceInterface().getSerializerByContentType("application/ifc");
					
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					bimServerClientInterface.download(roid, stepSerializerRemote.getOid(), outputStream);
					
					BimServerClientInterface localClient = getLocalBimServerClientInterface(new UserTokenAuthentication(userToken));

					PluginConfiguration pluginConfiguration = new org.bimserver.plugins.PluginConfiguration(settings);
					
					String localProjectName = pluginConfiguration.getString("projectName");
					
					List<SProject> projectsByName = localClient.getServiceInterface().getProjectsByName(localProjectName);
					if (projectsByName.isEmpty()) {
						throw new UserException("No project with name \"" + localProjectName + "\" was found");
					}
					SProject localProject = projectsByName.get(0);
					
					SDeserializerPluginConfiguration localDeserializer = localClient.getServiceInterface().getDeserializerByName("IfcStepDeserializer");
					localClient.getServiceInterface().checkin(localProject.getOid(), "Blaat", localDeserializer.getOid(), (long) outputStream.size(), "filename.ifc", new DataHandler(new InputStreamDataSource(new ByteArrayInputStream(outputStream.toByteArray()))), true, true);
				} catch (PublicInterfaceNotFoundException e) {
					LOGGER.error("", e);
				} catch (ServiceException e) {
					LOGGER.error("", e);
				} catch (ChannelConnectionException e) {
					LOGGER.error("", e);
				} catch (BimServerClientException e) {
					LOGGER.error("", e);
				} finally {
					SLongActionState state = new SLongActionState();
					state.setProgress(100);
					state.setTitle("Clone Service");
					state.setState(SActionState.FINISHED);
					state.setStart(startDate);
					state.setEnd(new Date());
					try {
						bimServerClientInterface.getRegistry().updateProgressTopic(topicId, state);
						bimServerClientInterface.getRegistry().unregisterProgressTopic(topicId);
					} catch (PublicInterfaceNotFoundException e) {
						LOGGER.error("", e);
					}
				}
			}
		});
	}

	@Override
	public void unregister(SInternalServicePluginConfiguration internalService) {
	}
}