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

import java.util.Map;

import org.bimserver.emf.IfcModelInterface;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.plugins.services.BimServerClientInterface;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;

public class PlanningConsultServiceVisualization extends AbstractPlanningConsultService {

	private static final String NAMESPACE = "http://bimserver.org/3dvisualizationeffects";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public PlanningConsultServiceVisualization() {
		super(NAMESPACE);
	}

	@Override
	protected void createExtendedData(Planner planner, IfcModelInterface model, BimServerClientInterface bimServerClientInterface, long roid) {
		Map<String, PlanningAdvice> suggestedPlanningsPerMaterial = planner.getSuggestedPlanningsPerMaterial(model);

		
		ObjectNode visNode = OBJECT_MAPPER.createObjectNode();
		visNode.put("name", "Estimated on time");
		ArrayNode changes = OBJECT_MAPPER.createArrayNode();
		visNode.set("changes", changes);
		
		ObjectNode greenChange = OBJECT_MAPPER.createObjectNode();
		changes.add(greenChange);
		ObjectNode greenSelector = OBJECT_MAPPER.createObjectNode();
		greenChange.set("selector", greenSelector);
		ArrayNode greenGuids = OBJECT_MAPPER.createArrayNode();
		greenSelector.set("guids", greenGuids);

		ObjectNode orangeChange = OBJECT_MAPPER.createObjectNode();
		changes.add(orangeChange);
		ObjectNode orangeSelector = OBJECT_MAPPER.createObjectNode();
		orangeChange.set("selector", orangeSelector);
		ArrayNode orangeGuids = OBJECT_MAPPER.createArrayNode();
		orangeSelector.set("guids", orangeGuids);

		ObjectNode redChange = OBJECT_MAPPER.createObjectNode();
		changes.add(redChange);
		ObjectNode redSelector = OBJECT_MAPPER.createObjectNode();
		redChange.set("selector", redSelector);
		ArrayNode redGuids = OBJECT_MAPPER.createArrayNode();
		redSelector.set("guids", redGuids);

		ObjectNode greenEffect = OBJECT_MAPPER.createObjectNode();
		greenChange.set("effect", greenEffect);
		ObjectNode orangeEffect = OBJECT_MAPPER.createObjectNode();
		orangeChange.set("effect", orangeEffect);
		ObjectNode redEffect = OBJECT_MAPPER.createObjectNode();
		redChange.set("effect", redEffect);
		
		ObjectNode green = OBJECT_MAPPER.createObjectNode();
		greenEffect.set("color", green);
		green.put("r", 0);
		green.put("g", 1);
		green.put("b", 0);
		green.put("a", 1f);

		ObjectNode orange = OBJECT_MAPPER.createObjectNode();
		orangeEffect.set("color", orange);
		orange.put("r", 1);
		orange.put("g", 0.647);
		orange.put("b", 0);
		orange.put("a", 1f);

		ObjectNode red = OBJECT_MAPPER.createObjectNode();
		redEffect.set("color", red);
		red.put("r", 1);
		red.put("g", 0);
		red.put("b", 0);
		red.put("a", 1f);

		for (String material : suggestedPlanningsPerMaterial.keySet()) {
			PlanningAdvice planningAdvice = suggestedPlanningsPerMaterial.get(material);
			for (Planning planning : planningAdvice.getUniquePlannings()) {
				int totalTasks = 0;
				int totalPercentage = 0;
				for (Task task : planning.getTasks()) {
					totalTasks++;
					totalPercentage += task.getPercentOnTime();
				}
				int percentageOnTime = totalPercentage / totalTasks;
				for (IfcProduct ifcProduct : planningAdvice.getRelatedProducts()) {
					if (percentageOnTime >= 99) {
						greenGuids.add(ifcProduct.getGlobalId());
					} else if (percentageOnTime > 50) {
						orangeGuids.add(ifcProduct.getGlobalId());
					} else {
						redGuids.add(ifcProduct.getGlobalId());
					}
				}
			}
		}		
		
		addExtendedData(visNode.toString().getBytes(Charsets.UTF_8), "visualizationinfo.json", "Planning Consult Results (JSON Visualization)", "application/json", bimServerClientInterface, roid);
	}
}