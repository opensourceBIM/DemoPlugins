package org.bimserver.demoplugins.tect;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.bimserver.emf.IfcModelInterface;
import org.bimserver.interfaces.objects.SActionState;
import org.bimserver.interfaces.objects.SDeserializerPluginConfiguration;
import org.bimserver.interfaces.objects.SLongActionState;
import org.bimserver.interfaces.objects.SObjectType;
import org.bimserver.interfaces.objects.SProgressTopicType;
import org.bimserver.interfaces.objects.SProject;
import org.bimserver.interfaces.objects.SRevision;
import org.bimserver.interfaces.objects.SService;
import org.bimserver.plugins.deserializers.Deserializer;
import org.bimserver.plugins.deserializers.DeserializerPlugin;
import org.bimserver.plugins.serializers.Serializer;
import org.bimserver.plugins.serializers.SerializerPlugin;
import org.bimserver.plugins.services.AbstractModifyRevisionService;
import org.bimserver.plugins.services.BimServerClientInterface;
import org.bimserver.plugins.services.Flow;
import org.bimserver.shared.exceptions.UserException;
import org.bimserver.utils.Formatters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnergyCalculationServicePlugin extends AbstractModifyRevisionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(EnergyCalculationServicePlugin.class);
	private static int count = 0;

	public EnergyCalculationServicePlugin() {
		super();
		// TODO Auto-generated constructor stub
	}
				
	@Override
	public ProgressType getProgressType() {
		return ProgressType.UNKNOWN;
	}	
	
	@Override
	public void newRevision(RunningService runningService, BimServerClientInterface bimServerClientInterface, long poid,
			long roid, String userToken, long soid, SObjectType settings) throws Exception {
		
		SRevision revision = bimServerClientInterface.getServiceInterface().getRevision(roid);
		if (revision.getComment().equals("Added energy needs")) {
			LOGGER.info("Skipping new revision because seems to be generated by TECT plugin");
			return;
		}
		Date startDate = new Date();
		Long topicId = bimServerClientInterface.getRegistry().registerProgressOnRevisionTopic(SProgressTopicType.RUNNING_SERVICE, poid, roid, "Running Energy Calculation");
		SLongActionState state = new SLongActionState();
		state.setTitle("Energy Calculation");
		state.setState(SActionState.STARTED);
		state.setProgress(-1);
		state.setStart(startDate);
		bimServerClientInterface.getRegistry().updateProgressTopic(topicId, state);
		
		SService service = bimServerClientInterface.getServiceInterface().getService(soid);
		
		SProject project = bimServerClientInterface.getServiceInterface().getProjectByPoid(poid);
		final IfcModelInterface model = bimServerClientInterface.getModel(project, roid, true, false);
		
		SerializerPlugin serializerPlugin = getPluginContext().getSerializerPlugin("org.bimserver.ifc.step.serializer.Ifc2x3tc1StepSerializerPlugin");
		
		String Name = "energyModel" + count;
		String DataDir = getPluginContext().getRootPath().resolve("data").toString();
		count++;
		
		Serializer serializer = serializerPlugin.createSerializer(null);
		serializer.init(model, null, true);
		Path originalFile = getPluginContext().getRootPath().resolve("data").resolve(Name + ".ifc"); 
		OutputStream resourceAsOutputStream = Files.newOutputStream(originalFile, java.nio.file.StandardOpenOption.CREATE);	
		serializer.writeToOutputStream(resourceAsOutputStream, null);
		resourceAsOutputStream.close();

		Path tectExe = getPluginContext().getRootPath().resolve("data").resolve("TECTcommandLine.exe");
		Process process = new ProcessBuilder(
				tectExe.toString(), 
				DataDir + "\\" + Name + ".ifc", 
				DataDir + "\\Climate\\FRA_Paris.Orly.071490_IWEC.epw", 
				DataDir + "\\System\\SystemData.txt", 
				DataDir + "\\Envelope\\EnvelopeData.txt", 
				DataDir + "\\" + Name).start();
		
		InputStream is = process.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;

		while ((line = br.readLine()) != null) {
		  LOGGER.info(line);
		}
		
		DeserializerPlugin deserializerPlugin = getPluginContext().getDeserializerPlugin("org.bimserver.ifc.step.deserializer.Ifc2x3tc1StepDeserializerPlugin", true);
		Deserializer deserializer = deserializerPlugin.createDeserializer(null);
		deserializer.init(model.getPackageMetaData());
		Path resultPath = getPluginContext().getRootPath().resolve("data").resolve(Name).resolve(Name + ".ifc");
		File f = resultPath.toFile();
		if (f.exists() && !f.isDirectory()) {
	
			SDeserializerPluginConfiguration deserializerForExtension = bimServerClientInterface.getServiceInterface().getSuggestedDeserializerForExtension("ifc", project.getOid());
			System.out.println("Checking in " + f.toString() + " - " + Formatters.bytesToString(f.length()));
			try {
				bimServerClientInterface.checkin(project.getOid(), "", deserializerForExtension.getOid(), false, Flow.SYNC, resultPath);
			} catch (UserException e) {
				e.printStackTrace();
			}		
			FileUtils.deleteDirectory(new File(getPluginContext().getRootPath().resolve("data").resolve(Name).toString()));
		}
	}
}