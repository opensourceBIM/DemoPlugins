//package org.bimserver.demoplugins.service.planner;

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

//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//
//public class TestPlanner {
//	public static void main(String[] args) {
//		EventLog eventLog;
//		try {
//			eventLog = new EventLog(new FileInputStream(new File("D:/Dropbox/Process Mining BIMserver/Ifc met planningsdata/eventlog.csv")));
//			Planner planner = new Planner();
//			planner.feedTrainingData(eventLog);
//			planner.analyze();
//			for (Planning planning : planner.getPlanningsForMaterial("IFC_dakplaat_195_overstek_ongeisoleerd")) {
//				System.out.println(planning);
//			}
//		} catch (FileNotFoundException | PlanningException e) {
//			e.printStackTrace();
//		}
//	}
//}
