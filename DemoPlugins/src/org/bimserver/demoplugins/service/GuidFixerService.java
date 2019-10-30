package org.bimserver.demoplugins.service;

/******************************************************************************
 * Copyright (C) 2009-2019  BIMserver.org
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.models.ifc2x3tc1.IfcRoot;
import org.bimserver.plugins.services.AbstractModifyRevisionService;
import org.bimserver.plugins.services.BimServerClientInterface;

public class GuidFixerService extends AbstractModifyRevisionService {

	public GuidFixerService() {
		super();
	}

	@Override
	public void newRevision(RunningService runningService, BimServerClientInterface bimServerClientInterface, long poid, long roid, String userToken, long soid, SObjectType settings) throws Exception {
		SProject project = bimServerClientInterface.getServiceInterface().getProjectByPoid(poid);
		IfcModelInterface model = bimServerClientInterface.getModel(project, roid, false, false);
		Map<String, List<IfcRoot>> guids = new HashMap<String, List<IfcRoot>>();
		int fixed = 0;
		// Iterate over all objects that can have a GUID
		for (IfcRoot ifcRoot : model.getAllWithSubTypes(IfcRoot.class)) {
			if (ifcRoot.getGlobalId() != null) {
				if (!guids.containsKey(ifcRoot.getGlobalId())) {
					guids.put(ifcRoot.getGlobalId(), new ArrayList<IfcRoot>());
				}
				guids.get(ifcRoot.getGlobalId()).add(ifcRoot);
			}
		}
		for (String guid : guids.keySet()) {
			List<IfcRoot> list = guids.get(guid);
			if (list.size() > 1) {
				int c = 1;
				for (int i=1; i<list.size(); i++) {
					String newGuid = guid + "." + c;
					while (guids.containsKey(newGuid)) {
						c++;
						newGuid = guid + "." + c;
					}
					fixed++;
					list.get(i).setGlobalId(newGuid);
				}
			}
		}
		if (fixed > 0) {
			model.commit("Fixed " + fixed + " GUIDs");
		}		
	}

	@Override
	public ProgressType getProgressType() {
		return ProgressType.UNKNOWN;
	}
}