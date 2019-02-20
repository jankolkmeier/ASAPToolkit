package asap.toolkit.starters;

import nl.utwente.hmi.middleware.loader.GenericMiddlewareLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.stream.Collectors;

import asap.bml.ext.bmlt.BMLTInfo;
import asap.environment.AsapEnvironment;
import asap.realizerembodiments.SharedPortLoader;
import hmi.audioenvironment.AudioEnvironment;
import hmi.environmentbase.ClockDrivenCopyEnvironment;
import hmi.environmentbase.Environment;
import hmi.mixedanimationenvironment.MixedAnimationEnvironment;
import hmi.physicsenvironment.OdePhysicsEnvironment;
import hmi.unityembodiments.loader.SharedMiddlewareLoader;
import hmi.worldobjectenvironment.WorldObjectEnvironment;
import saiba.bml.BMLInfo;
import saiba.bml.core.FaceLexemeBehaviour;
import saiba.bml.core.HeadBehaviour;
import saiba.bml.core.PostureShiftBehaviour;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ASAPToolkitStarter {

	public static String ReadFile(String path) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream(path);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
    		return br.lines().collect(Collectors.joining(System.lineSeparator()));
    	} catch (IOException e) {
			return null;
		}
	}
	
    public static void main(String[] args) throws IOException {
        ASAPToolkitStarter asapToolkit = new ASAPToolkitStarter();
    	String launch = ReadFile("launch.json");
    	if (launch != null) {
    		ObjectMapper mapper = new ObjectMapper();
    		asapToolkit.init(mapper.readTree(launch));
    	} else {
    		asapToolkit.init(null);
    	}
    }

    public ASAPToolkitStarter() {
    }

    public void init(JsonNode jsonNode) throws IOException {
        String shared_port = "environment/shared_port.xml";
        String shared_middleware = "environment/shared_middleware.xml";
        String resources = "";
        
        MixedAnimationEnvironment mae = new MixedAnimationEnvironment();
        final OdePhysicsEnvironment ope = new OdePhysicsEnvironment();
        WorldObjectEnvironment we = new WorldObjectEnvironment();
        AudioEnvironment aue = new AudioEnvironment("LJWGL_JOAL");

        BMLTInfo.init();
        BMLInfo.addCustomFloatAttribute(FaceLexemeBehaviour.class, "http://asap-project.org/convanim", "repetition");
        BMLInfo.addCustomStringAttribute(HeadBehaviour.class, "http://asap-project.org/convanim", "spindirection");
        BMLInfo.addCustomFloatAttribute(PostureShiftBehaviour.class, "http://asap-project.org/convanim", "amount");

        ArrayList<Environment> environments = new ArrayList<Environment>();
        final AsapEnvironment ee = new AsapEnvironment();
        
        ClockDrivenCopyEnvironment ce = new ClockDrivenCopyEnvironment(1000 / 30);

        ce.init();
        ope.init();
        mae.init(ope, 0.002f);
        we.init();
        aue.init();
        environments.add(ee);
        environments.add(ope);
        environments.add(mae);
        environments.add(we);

        environments.add(ce);
        environments.add(aue);

        SharedMiddlewareLoader sml = new SharedMiddlewareLoader();
        sml.load(resources, shared_middleware);
        environments.add(sml);
        
        SharedPortLoader spl = new SharedPortLoader();
        spl.load(resources, shared_port, ope.getPhysicsClock());
        environments.add(spl);

        ee.init(environments, spl.getSchedulingClock());
        ope.addPrePhysicsCopyListener(ee);
        
        JsonNode agents = jsonNode.get("agents");
        try {
        	for (JsonNode agent : agents) {
        		String title = String.format("%s: %s", jsonNode.get("instance").asText(), agent.get("charId").asText());
                ee.loadVirtualHuman(agent.get("charId").asText(), resources, agent.get("spec").asText(), title);
        		
        	}
            ope.startPhysicsClock();
        } catch (Exception e) {
        	System.out.println("Failed to load agents: "+e);
        }
    }
    
    /*  
import com.github.jknack.handlebars.*;
        Handlebars handlebars = new Handlebars();

    	String templateFile = jsonNode.get("template").asText();
    	Template template = handlebars.compile(templateFile);
    	Context context = Context
    			  .newBuilder(jsonNode)
    			  .resolver(JsonNodeValueResolver.INSTANCE)
    			  .build();
    	
    	System.out.println(template.apply(context));
    */
}

