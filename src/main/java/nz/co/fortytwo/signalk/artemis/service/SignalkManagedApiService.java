package nz.co.fortytwo.signalk.artemis.service;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.config.service.Get;
import org.atmosphere.config.service.ManagedService;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceFactory;

@ManagedService(path = "/signalk/v1/api")
public class SignalkManagedApiService extends SignalkApiService {

	private static Logger logger = LogManager.getLogger(SignalkManagedApiService.class);

	@Inject
	private AtmosphereResourceFactory resourceFactory;

	@Get
	public void get(AtmosphereResource resource) {
		logger.debug("onMessage:"+resource);
		
			// Here we need to find the suspended AtmosphereResource
			super.get(resource, resource.getRequest().getRequestURI().toString());
		
	}


}
