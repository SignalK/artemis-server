package nz.co.fortytwo.signalk.artemis.service;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.config.service.Get;
import org.atmosphere.config.service.ManagedService;
import org.atmosphere.config.service.Ready;
import org.atmosphere.cpr.AtmosphereResource;

import nz.co.fortytwo.signalk.artemis.util.Config;

@ManagedService(path = "/signalk")
public class SignalkManagedEndpointService {

	private static Logger logger = LogManager.getLogger(SignalkManagedEndpointService.class);

	@Ready
	public void onReady(final AtmosphereResource r) {
		logger.debug("onReady:"+r);
	}

	@Get
	public void onMessage(AtmosphereResource resource) {
		logger.debug("onMessage:"+resource);
		try {
			// Here we need to find the suspended AtmosphereResource
			resource.getResponse().getWriter().write(Config.getDiscoveryMsg(resource.getRequest().getLocalName()).toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
