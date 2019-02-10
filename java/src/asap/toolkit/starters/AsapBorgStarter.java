package asap.toolkit.starters;

import nl.utwente.hmi.middleware.loader.GenericMiddlewareLoader;
import java.io.IOException;
import java.util.ArrayList;
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

public class AsapBorgStarter {

    public static void main(String[] args) throws IOException {
        AsapBorgStarter cds = new AsapBorgStarter();
        cds.init();
    }

    public AsapBorgStarter() {
    }

    public void init() throws IOException {
        String shared_port = "environment/shared_port.xml";
        String shared_middleware = "environment/shared_middleware.xml";
        String[] specs = new String[] {
        		"agentspecs/UMA.xml"
        };
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
        
        for (String spec : specs) {
            ee.loadVirtualHuman(resources, spec, "AsapRealizer: "+spec);
        }

        ope.startPhysicsClock();
    }
}

