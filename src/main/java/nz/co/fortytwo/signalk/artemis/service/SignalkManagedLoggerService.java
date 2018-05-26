package nz.co.fortytwo.signalk.artemis.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.config.service.Get;
import org.atmosphere.config.service.ManagedService;
import org.atmosphere.config.service.Post;
import org.atmosphere.config.service.Put;
import org.atmosphere.config.service.Ready;
import org.atmosphere.cpr.AtmosphereResource;

@ManagedService(path = "/signalk/v1/logger/config")
public class SignalkManagedLoggerService extends BaseApiService {

	public SignalkManagedLoggerService() throws Exception {
		super();
	}

	private static Logger logger = LogManager.getLogger(SignalkManagedLoggerService.class);

	@Ready
	public void onReady(final AtmosphereResource r) {
		if(logger.isDebugEnabled())logger.debug("onReady:"+r);
	}

	@Get
	public void onMessage(AtmosphereResource resource) {
		if(logger.isDebugEnabled())logger.debug("onMessage:"+resource);
		try {
			//send back the log4j.json
			String log4j2 = FileUtils.readFileToString(new File("./conf/log4j2.json"));
			resource.getResponse().getWriter().write(log4j2);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Post
	public void post(AtmosphereResource resource) {
		if(logger.isDebugEnabled())logger.debug("onMessage-post:"+resource);
		try{
			//save it
			List<String> lines = IOUtils.readLines(resource.getRequest().getReader());
			FileUtils.writeLines(new File("./conf/log4j2.json"),lines);
		}catch(Exception e){
			logger.error(e,e);
			try {
				resource.getResponse().sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			} catch (IOException e1) {
				logger.error(e1.getMessage(), e1);
			}
		}
		
	}
	



}
