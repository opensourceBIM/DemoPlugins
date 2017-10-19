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

import java.nio.file.Files;
import java.nio.file.Path;

import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.plugins.services.AbstractAddExtendedDataService;
import org.bimserver.plugins.services.BimServerClientInterface;

public class HtmlService extends AbstractAddExtendedDataService {
	private static final String NAMESPACE = "htmldemo";

	public HtmlService() {
		super(NAMESPACE);
	}

	@Override
	public void newRevision(RunningService runningService, BimServerClientInterface bimServerClientInterface, long poid, long roid, String userToken, long soid, SObjectType settings) throws Exception {
		Path path = getPluginContext().getRootPath().resolve("data").resolve("example.html");
		addExtendedData(Files.readAllBytes(path), "example.html", "HTML Demo Results", "text/html", bimServerClientInterface, roid);
	}
}