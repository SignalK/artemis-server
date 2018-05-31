package nz.co.fortytwo.signalk.artemis.service;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.config.service.Get;
import org.atmosphere.config.service.ManagedService;
import org.atmosphere.config.service.Post;
import org.atmosphere.config.service.Put;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceFactory;

//@ManagedService(path = "/signalk/v1/api")
public class SignalkManagedApiService extends SignalkApiService {

	public SignalkManagedApiService() throws Exception {
		super();
	}

	private static Logger logger = LogManager.getLogger(SignalkManagedApiService.class);

	@Inject
	private AtmosphereResourceFactory resourceFactory;

	@Get
	public void get(AtmosphereResource resource) {
		if(logger.isDebugEnabled())logger.debug("onMessage-get:"+resource);
		try{
			// Here we need to find the suspended AtmosphereResource
			super.get(resource, resource.getRequest().getRequestURI().toString());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			try {
				resource.getResponse().sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			} catch (IOException e1) {
				logger.error(e1.getMessage(), e1);
			}
		}
		
	}
	
	@Post
	public void post(AtmosphereResource resource) {
		if(logger.isDebugEnabled())logger.debug("onMessage-post:"+resource);
		
			// Here we need to find the suspended AtmosphereResource
			super.post(resource, resource.getRequest().getRequestURI().toString());
		
	}
	
	@Put
	public void put(AtmosphereResource resource) {
		if(logger.isDebugEnabled())logger.debug("onMessage-post:"+resource);
		
		// Here we need to find the suspended AtmosphereResource
		super.put(resource, resource.getRequest().getRequestURI().toString());
	
	}


}
