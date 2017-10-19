package org.bimserver.demoplugins.pathchecker;

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

import java.io.ByteArrayOutputStream;
import java.util.GregorianCalendar;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.xml.datatype.DatatypeFactory;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.interfaces.objects.SUser;
import org.bimserver.plugins.SchemaName;
import org.bimserver.plugins.services.AbstractAddExtendedDataService;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.opensourcebim.bcf.BcfFile;
import org.opensourcebim.bcf.TopicFolder;
import org.opensourcebim.bcf.markup.Topic;
import org.opensourcebim.bcf.markup.ViewPoint;

public class PathCheckerService extends AbstractAddExtendedDataService {

	public PathCheckerService() {
		super(SchemaName.BCF_ZIP_2_0.name());
	}

	@Override
	public void newRevision(RunningService runningService, BimServerClientInterface bimServerClientInterface, long poid, long roid, String userToken, long soid, SObjectType settings) throws Exception {
		SUser loggedInUser = bimServerClientInterface.getAuthInterface().getLoggedInUser();
		BcfFile bcf = new BcfFile();
		SProject project = bimServerClientInterface.getServiceInterface().getProjectByPoid(poid);
		IfcModelInterface model = bimServerClientInterface.getModel(project, roid, true, false, true);

		PathCheckerSettings pathSettings = new PathCheckerSettings();
//		pathSettings.setMinWidth(500);
		PathChecker pathChecker = new PathChecker(pathSettings);
		Set<SpaceCheckResult> set = pathChecker.check(model);
		for (SpaceCheckResult spaceCheckResult : set) {
			if (!spaceCheckResult.isValid()) {
				TopicFolder topicFolder = bcf.createTopicFolder();
				topicFolder.getMarkup();
				if (spaceCheckResult.getImage() != null) {
					ByteArrayOutputStream imageBuffer = new ByteArrayOutputStream();
					ImageIO.write(spaceCheckResult.getImage(), "png", imageBuffer);
					topicFolder.setDefaultSnapShot(imageBuffer.toByteArray());

					ViewPoint viewPoint = new ViewPoint();
					viewPoint.setSnapshot("snapshot.png");
					viewPoint.setGuid(topicFolder.getUuid().toString());
					viewPoint.setViewpoint(UUID.randomUUID().toString());
					topicFolder.getMarkup().getViewpoints().add(viewPoint);
					
					topicFolder.addSnapShot("snapshot.png", imageBuffer.toByteArray());
				} else {
					topicFolder.setDefaultSnapShotToDummy();
				}
				Topic topic = topicFolder.createTopic();
				topic.setGuid(topicFolder.getUuid().toString());
				topic.setTopicType("Issue");
				topic.setTopicStatus("Open");
				topic.setCreationAuthor(loggedInUser.getName() + " (" + loggedInUser.getUsername() + ")");
				topic.setCreationDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
				topic.setDescription(spaceCheckResult.toString());
				topic.setTitle("Space check result " + spaceCheckResult.getIfcSpace().getName());
			}
		}
		
		addExtendedData(bcf.toBytes(), project.getName() + ".bcfzip", " (BCF)", "application/zip", bimServerClientInterface, roid);

	}
}
