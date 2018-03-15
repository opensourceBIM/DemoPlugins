package org.bimserver.demoplugins.service.planner;

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

import java.util.GregorianCalendar;

public class Event implements Comparable<Event> {
	public static enum Timing {
		UNKNOWN,
		ON_TIME,
		TOO_LATE
	}
	
	private String guid;
	private String type;
	private String nlSfb;
	private String material;
	private String task;
	private String resource;
	private String taskName;
	private GregorianCalendar taskStart;
	private GregorianCalendar taskFinish;
	private Timing timing = Timing.UNKNOWN;
	private String buildingGuid;

	public String getGuid() {
		return guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getNlSfb() {
		return nlSfb;
	}

	public void setNlSfb(String nlSfb) {
		this.nlSfb = nlSfb;
	}

	public String getMaterial() {
		return material;
	}

	public void setMaterial(String material) {
		this.material = material;
	}

	public String getTask() {
		return task;
	}

	public void setTask(String task) {
		this.task = task;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public GregorianCalendar getTaskStart() {
		return taskStart;
	}

	public void setTaskStart(GregorianCalendar taskStart) {
		this.taskStart = taskStart;
	}

	public GregorianCalendar getTaskFinish() {
		return taskFinish;
	}

	public void setTaskFinish(GregorianCalendar taskFinish) {
		this.taskFinish = taskFinish;
	}

	@Override
	public int compareTo(Event o) {
		return taskStart.compareTo(o.taskStart);
	}

	public Event copy() {
		Event event = new Event();
		event.setGuid(guid);
		event.setMaterial(material);
		event.setNlSfb(nlSfb);
		event.setResource(resource);
		event.setTask(task);
		event.setTaskFinish(taskFinish);
		event.setTaskStart(taskStart);
		event.setType(type);
		event.setTaskName(taskName);
		return event;
	}

	public Timing getTiming() {
		return timing;
	}

	public void setTiming(Timing timing) {
		this.timing = timing;
	}

	public void setBuildingGuid(String buildingGuid) {
		this.buildingGuid = buildingGuid;
	}
	
	public String getBuildingGuid() {
		return buildingGuid;
	}
}