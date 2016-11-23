package org.bimserver.demoplugins.pathchecker;

import java.util.List;

import org.apache.log4j.chainsaw.Main;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.models.ifc2x3tc1.IfcClassificationNotationSelect;
import org.bimserver.models.ifc2x3tc1.IfcClassificationReference;
import org.bimserver.models.ifc2x3tc1.IfcSpace;
import org.bimserver.shared.ChannelConnectionException;
import org.bimserver.shared.UsernamePasswordAuthenticationInfo;
import org.bimserver.shared.exceptions.BimServerClientException;
import org.bimserver.shared.exceptions.ServiceException;
import org.bimserver.utils.IfcUtils;

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
