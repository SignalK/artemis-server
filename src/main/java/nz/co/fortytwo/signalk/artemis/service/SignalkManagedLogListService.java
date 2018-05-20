package nz.co.fortytwo.signalk.artemis.service;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.config.service.Get;
import org.atmosphere.config.service.ManagedService;
import org.atmosphere.config.service.Ready;
import org.atmosphere.cpr.AtmosphereResource;

import mjson.Json;

@ManagedService(path = "/signalk/v1/logger/listLogs")
public class SignalkManagedLogListService extends SignalkApiService {

	private static Logger logger = LogManager.getLogger(SignalkManagedLogListService.class);

	@Ready
	public void onReady(final AtmosphereResource r) {
		if(logger.isDebugEnabled())logger.debug("onReady:"+r);
	}

	@Get
	public void onMessage(AtmosphereResource resource) {
		if(logger.isDebugEnabled())logger.debug("onMessage:"+resource);
		if(logger.isDebugEnabled())logger.debug("queryString:"+resource.getRequest().queryStringsMap());
		
		try {
			//send back the log list
			String[] logDir = resource.getRequest().queryStringsMap().get("logDir");
			
			if(StringUtils.isBlank(logDir[0])){
				File dir = new File("signalk-static/logs");
				resource.getResponse().getWriter().write(Json.array(dir.list()).toString());
			}else{
				File dir = new File("signalk-static/logs/"+logDir[0]);
				resource.getResponse().getWriter().write(Json.array(dir.list()).toString());
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
}
