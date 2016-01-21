package org.bimserver.demoplugins.modelcheckers;

import org.bimserver.models.store.ObjectDefinition;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.PluginManagerInterface;
import org.bimserver.plugins.modelchecker.ModelChecker;
import org.bimserver.plugins.modelchecker.ModelCheckerPlugin;
import org.bimserver.shared.exceptions.PluginException;

public class LcieModelCheckerPlugin implements ModelCheckerPlugin {

	@Override
	public void init(PluginManagerInterface pluginManager) throws PluginException {
	}

	@Override
	public String getDefaultName() {
		return "LCie Model Checker";
	}

	@Override
	public ObjectDefinition getSettingsDefinition() {
		return null;
	}

	@Override
	public ModelChecker createModelChecker(PluginConfiguration pluginConfiguration) {
		return new LcieModelChecker();
	}
}