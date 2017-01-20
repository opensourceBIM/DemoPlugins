package org.bimserver.demoplugins.pathchecker;

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
