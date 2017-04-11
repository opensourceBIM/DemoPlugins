package org.bimserver.demoplugins.service.planner;

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

public class Task implements Comparable<Task> {
	private String code;
	private String description;
	private String uniqueKey;
	private int totalOccurances;
	private int totalOnTime;
	private int totalNotOnTime;

	public Task(String uniqueKey, String code, String description) {
		this.uniqueKey = uniqueKey;
		this.code = code;
		this.description = description;
	}
	
	public void addOnTime() {
		totalOccurances++;
		totalOnTime++;
	}
	
	public void addNotOnTime() {
		totalOccurances++;
		totalNotOnTime++;
	}
	
	public void addUnknown() {
		totalOccurances++;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uniqueKey == null) ? 0 : uniqueKey.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Task other = (Task) obj;
		if (uniqueKey == null) {
			if (other.uniqueKey != null)
				return false;
		} else if (!uniqueKey.equals(other.uniqueKey))
			return false;
		return true;
	}

	@Override
	public int compareTo(Task o) {
		return uniqueKey.compareTo(o.uniqueKey);
	}
	
	@Override
	public String toString() {
		return code;
	}

	public String getCode() {
		return code;
	}
	
	public String getDescription() {
		return description;
	}

	public int getPercentOnTime() {
		return (int) (100.0 * totalOnTime / totalOccurances);
	}

	public int getPercentTooLate() {
		return (int) (100.0 * totalNotOnTime / totalOccurances);
	}

	public int getPercentUnknown() {
		return 100 - (getPercentOnTime() + getPercentTooLate());
	}
}