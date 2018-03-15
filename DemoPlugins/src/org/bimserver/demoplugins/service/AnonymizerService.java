package org.bimserver.demoplugins.service;

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

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.emf.ModelMetaData;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.interfaces.objects.SService;
import org.bimserver.models.ifc2x3tc1.IfcAddressTypeEnum;
import org.bimserver.models.ifc2x3tc1.IfcOrganization;
import org.bimserver.models.ifc2x3tc1.IfcPerson;
import org.bimserver.models.ifc2x3tc1.IfcPostalAddress;
import org.bimserver.models.store.IfcHeader;
import org.bimserver.plugins.services.AbstractModifyRevisionService;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.shared.exceptions.BimServerClientException;
import org.bimserver.shared.exceptions.PublicInterfaceNotFoundException;
import org.bimserver.shared.exceptions.ServerException;
import org.bimserver.shared.exceptions.UserException;

public class AnonymizerService extends AbstractModifyRevisionService {

	private static final String ANONYMIZED = "[ANONYMIZED]";

	public AnonymizerService() {
		super();
	}

	@Override
	public void newRevision(RunningService runningService, BimServerClientInterface bimServerClientInterface, long poid, long roid, String userToken, long soid, SObjectType settings) throws ServerException, UserException, PublicInterfaceNotFoundException, BimServerClientException {
		SProject project = bimServerClientInterface.getServiceInterface().getProjectByPoid(poid);
		final IfcModelInterface model = bimServerClientInterface.getModel(project, roid, true, false);

		SService service = bimServerClientInterface.getServiceInterface().getService(soid);

		for (IfcPerson ifcPerson : model.getAll(IfcPerson.class)) {
			ifcPerson.setFamilyName(ANONYMIZED);
			ifcPerson.setGivenName(ANONYMIZED);
			ifcPerson.setId(ANONYMIZED);
		}
		for (IfcOrganization ifcOrganization : model.getAll(IfcOrganization.class)) {
			ifcOrganization.setDescription(ANONYMIZED);
			ifcOrganization.setId(ANONYMIZED);
			ifcOrganization.setName(ANONYMIZED);
		}
		for (IfcPostalAddress ifcPostalAddress : model.getAll(IfcPostalAddress.class)) {
			ifcPostalAddress.setCountry(ANONYMIZED);
			ifcPostalAddress.setDescription(ANONYMIZED);
			ifcPostalAddress.setInternalLocation(ANONYMIZED);
			ifcPostalAddress.setPostalBox(ANONYMIZED);
			ifcPostalAddress.setPurpose(IfcAddressTypeEnum.NULL);
			ifcPostalAddress.setPostalCode(ANONYMIZED);
			ifcPostalAddress.setRegion(ANONYMIZED);
			ifcPostalAddress.setTown(ANONYMIZED);
			ifcPostalAddress.setUserDefinedPurpose(ANONYMIZED);
		}
		ModelMetaData modelMetaData = model.getModelMetaData();
		modelMetaData.setAuthorizedUser(ANONYMIZED);
		modelMetaData.setName(ANONYMIZED);
		IfcHeader ifcHeader = model.getModelMetaData().getIfcHeader();
		ifcHeader.setAuthorization(ANONYMIZED);
		ifcHeader.setFilename(ANONYMIZED);
		ifcHeader.setOriginatingSystem(ANONYMIZED);
		ifcHeader.setPreProcessorVersion(ANONYMIZED);
		for (int i=0; i<ifcHeader.getAuthor().size(); i++) {
			ifcHeader.getAuthor().set(i, ANONYMIZED);
		}
		for (int i=0; i<ifcHeader.getOrganization().size(); i++) {
			ifcHeader.getOrganization().set(i, ANONYMIZED);
		}
		for (int i=0; i<ifcHeader.getDescription().size(); i++) {
			ifcHeader.getDescription().set(i, ANONYMIZED);
		}

		if (service.getWriteRevisionId() != -1 && service.getWriteRevisionId() != project.getOid()) {
			model.checkin(service.getWriteRevisionId(), "Anonymized");
		} else {
			model.commit("Anonymized");
		}
	}

	@Override
	public ProgressType getProgressType() {
		return ProgressType.UNKNOWN;
	}
}