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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.bimserver.models.ifc2x3tc1.IfcProduct;

public class PlanningAdvice {

	private int databaseCount;
	private int modelCount = 0;
	private Map<Planning, AtomicInteger> uniquePlannings = new HashMap<>();
	private List<Planning> plannings = new ArrayList<>();
	private Map<Planning, Integer> percentages = null;
	private Set<IfcProduct> relatedProducts = new HashSet<>();

	public void incrementModelCount(IfcProduct ifcProduct) {
		relatedProducts.add(ifcProduct);
		modelCount++;
	}

	public Set<IfcProduct> getRelatedProducts() {
		return relatedProducts;
	}
	
	public void setDatabaseCount(int databaseCount) {
		this.databaseCount = databaseCount;
		
	}

	public void addVariant(Planning planning) {
		plannings.add(planning);
		if (uniquePlannings.containsKey(planning)) {
			uniquePlannings.get(planning).incrementAndGet();
		} else {
			uniquePlannings.put(planning, new AtomicInteger(1));
		}
	}
	
	public int getDatabaseCount() {
		return databaseCount;
	}
	
	public int getModelCount() {
		return modelCount;
	}

	public Set<Planning> getUniquePlannings() {
		return uniquePlannings.keySet();
	}
	
	public int getPercentage(Planning planning) {
		if (percentages == null) {
			calculatePercentages();
		}
		return percentages.get(planning);
	}

	private void calculatePercentages() {
		percentages = new HashMap<>();
		Map<Planning, Float> remainders = new HashMap<>();
		int total = 0;
		float biggestRemainer = -1f;
		Planning biggestRemainerPlanning = null;
		for (Planning planning : uniquePlannings.keySet()) {
			int percentage = (int)(100f * uniquePlannings.get(planning).get() / plannings.size());
			total += percentage;
			float remainder = (100f * uniquePlannings.get(planning).get() / plannings.size()) - percentage;
			if (remainder > biggestRemainer) {
				biggestRemainer = remainder;
				biggestRemainerPlanning = planning;
			}
			remainders.put(planning, remainder);
			percentages.put(planning, percentage);
		}
		if (total < 100) {
			percentages.put(biggestRemainerPlanning, percentages.get(biggestRemainerPlanning) + 1);
		}
	}
}