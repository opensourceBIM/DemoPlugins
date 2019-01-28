package org.bimserver.demoplugins.pathchecker;

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

public class RunStandalone {
	public static void main(String[] args) {
//		new RunStandalone().start();
	}

//	private void start() {
//		try {
//			JsonBimServerClientFactory factory = new JsonBimServerClientFactory("http://localhost:8080");
//			BimServerClient client = factory.create(new UsernamePasswordAuthenticationInfo("admin@bimserver.org", "admin"));
//			SProject project = client.getServiceInterface().getTopLevelProjectByName("Dorm");
//			model = client.getModel(project, project.getLastRevisionId(), true, false, true);
//			for (IfcSpace ifcSpace : model.getAll(IfcSpace.class)) {
//				boolean check = false;
//				List<IfcClassificationNotationSelect> classifications = IfcUtils.getClassifications(ifcSpace, model);
//				for (IfcClassificationNotationSelect ifcClassificationNotationSelect : classifications) {
//					if (ifcClassificationNotationSelect instanceof IfcClassificationReference) {
//						IfcClassificationReference ifcClassificationReference = (IfcClassificationReference)ifcClassificationNotationSelect;
//						if (ifcClassificationReference.getItemReference().equals("13-25 00 00")) {
//							check = true;
//							break;
//						}
//					}
//				}
//				if (IfcUtils.hasProperty(ifcSpace, "HandicapAccessible")) {
//					check = true;
//				}
//				if (check) {
//					checkSpace(ifcSpace);
//				}
//			}
//		} catch (ServiceException e) {
//			e.printStackTrace();
//		} catch (ChannelConnectionException e) {
//			e.printStackTrace();
//		} catch (BimServerClientException e) {
//			e.printStackTrace();
//		}		
//	}
}
