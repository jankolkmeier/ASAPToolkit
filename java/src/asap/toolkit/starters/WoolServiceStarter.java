package asap.toolkit.starters;

import utwente.hmi.woolservice.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.woolplatform.webservice.Configuration;
import eu.woolplatform.wool.model.WoolProject;
import hmi.xml.XMLTokenizer;
import nl.utwente.hmi.middleware.Middleware;
import nl.utwente.hmi.middleware.MiddlewareListener;
import nl.utwente.hmi.middleware.loader.GenericMiddlewareLoader;


public class WoolServiceStarter implements MiddlewareListener {
	
	Middleware serviceMiddleware;
	WoolProject woolProject;
	JSONDataController dc;


	public WoolServiceStarter(String dialogueRoot, String dataStore, String middlewareSpec) throws IOException {
		init(dialogueRoot, dataStore, middlewareSpec);
	}
	
	public WoolServiceStarter(JsonNode config) throws IOException {
		JsonNode woolNode = config.get("wool");
		String dialogueRoot = woolNode.get("dialogueRoot").asText();
		String dataStore = woolNode.get("dataStore").asText(); 
		String middlewareSpec = woolNode.get("middlewareSpec").asText();
		init(dialogueRoot, dataStore, middlewareSpec);
	}
	
	public void init(String dialogueRoot, String dataStore, String middlewareSpec) throws IOException {
		serviceMiddleware = GenericMiddlewareLoader.load(
			XMLTokenizer.forResource("", middlewareSpec)
		);
		
		Path workingDirectory = Paths.get(System.getProperty("user.dir"));
		
		Path dialogueRootPath = Paths.get(dialogueRoot);
		if (!dialogueRootPath.isAbsolute()) {
			dialogueRootPath = Paths.get(workingDirectory.toString(), dialogueRootPath.toString());
		}

		Path dataStorePath = Paths.get(dataStore);
		if (!dataStorePath.isAbsolute()) {
			dataStorePath = Paths.get(workingDirectory.toString(), dataStorePath.toString());
		}

		
		Configuration.getInstance().put(Configuration.DATA_DIR, dataStorePath.toString());
		dc = new JSONDataController(new EmbeddedWoolApplication(dialogueRootPath.toString()));
		serviceMiddleware.addListener(this);
		System.out.println("DialoguePath: "+dialogueRootPath.toString());
		System.out.println("DataPath: "+dataStorePath.toString());
	}
	
	public static void main(String args[]) {
		String launchFile = args[0];
	   	String launch = ASAPToolkitStarter.ReadFile(launchFile);
	   	
    	if (launch == null) {
    		System.out.println("Could not find launchfile "+launchFile);
    		return;
    	}

		ObjectMapper mapper = new ObjectMapper();
		try {
			WoolServiceStarter wss = new WoolServiceStarter(mapper.readTree(launch));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void receiveData(JsonNode jn) {
		JsonNode response = null;
		try {
			// TODO: check here if indeed wool request?
			System.out.println("Handle request... ");
			response = dc.handleRequest(jn);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Exception handling WoolRequest: "+e.getMessage());
			response = null;
		}
		
		if (response != null) {
			serviceMiddleware.sendData(response);
		} else {
			System.out.println("wool responded with null (no response sent)... ");
		}
	}
	
}
