package utwente.hmi.woolservice;

import java.io.File;

import eu.woolplatform.webservice.DefaultServiceManagerConfig;
import eu.woolplatform.webservice.dialogue.ServiceManager;
import eu.woolplatform.webservice.dialogue.ServiceManagerConfig;
import eu.woolplatform.wool.parser.WoolDirectoryFileLoader;

public class EmbeddedWoolApplication implements WoolApplication {

	private ServiceManager sm;
	
	public EmbeddedWoolApplication(String wpr) {
		ServiceManagerConfig serviceManagerConfig = new DefaultServiceManagerConfig();
		ServiceManagerConfig.setInstance(serviceManagerConfig);
		sm = new ServiceManager(new WoolDirectoryFileLoader(new File(wpr)));
	}
	
	@Override
	public ServiceManager getServiceManager() {
		return sm;
	}
	

}
