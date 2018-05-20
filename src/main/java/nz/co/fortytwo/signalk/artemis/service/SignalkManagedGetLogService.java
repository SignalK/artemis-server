package nz.co.fortytwo.signalk.artemis.service;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.config.service.Get;
import org.atmosphere.config.service.ManagedService;
import org.atmosphere.config.service.Ready;
import org.atmosphere.cpr.AtmosphereResource;

@ManagedService(path = "/signalk/v1/getLogs")
public class SignalkManagedGetLogService extends SignalkApiService {

	private static Logger logger = LogManager.getLogger(SignalkManagedGetLogService.class);

	@Ready
	public void onReady(final AtmosphereResource r) {
		if(logger.isDebugEnabled())logger.debug("onReady:"+r);
	}

	@Get
	public void onMessage(AtmosphereResource resource) {
		if(logger.isDebugEnabled())logger.debug("onMessage:"+resource);
		try {
			logger.debug("getLog: request: {}", resource.getRequest());
			logger.debug("getLog: request: {}", resource.getRequest().getParameterMap());
			logger.debug("getLog: request: {}", resource.getRequest().getAttributeNames());
			String[] logFile = resource.getRequest().queryStringsMap().get("logFile");
			String[] logDir = resource.getRequest().queryStringsMap().get("logDir");
			//String logFile = resource.getRequest().getParameter("logFile");
			if(logFile[0].contains("/")){
				logFile[0]=logFile[0].substring(logFile[0].lastIndexOf("/")+1, logFile[0].length());
			}
			if(StringUtils.isBlank(logFile[0])){
				resource.getResponse().sendError(HttpStatus.SC_BAD_REQUEST);
			}
			File dir;
			if(StringUtils.isBlank(logDir[0])){
				dir = new File("signalk-static/logs/"+logFile[0]);
			}else{
				dir = new File("signalk-static/logs/"+logDir+"/"+logFile[0]);
			}
			if(logFile[0].endsWith(".log")){
				resource.getResponse().setContentType("text/plain");
				resource.getResponse().getWriter().write(FileUtils.readFileToString(dir));
			}
			if(logFile[0].endsWith(".gz")){
				resource.getResponse().setContentType("application/gzip");
				resource.getResponse().write(FileUtils.readFileToByteArray(dir));
			}
			
		} catch (IOException e) {
			logger.error(e,e);
		}
	}
	
	
}
