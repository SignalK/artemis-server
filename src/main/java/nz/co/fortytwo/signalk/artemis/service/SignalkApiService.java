package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SIGNALK_API;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.vessels;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.cpr.AtmosphereResource;

import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

public abstract class SignalkApiService {

	private static Logger logger = LogManager.getLogger(SignalkApiService.class);

	public SignalkApiService() {
		
	}

	/**
	 * @param resource
	 * @param path
	 * @throws Exception
	 */
	public void get(AtmosphereResource resource, String path) throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("get raw:" + path + " for " + resource.getRequest().getRemoteUser());
		// handle /self
		
		path = StringUtils.removeStart(path,SIGNALK_API);
		path = StringUtils.removeStart(path,"/");
		if (path.equals("self")){
			resource.getResponse().write("\""+Config.getConfigProperty(ConfigConstants.UUID)+"\"");
			return;
		}
		path = path.replace('/', '.');

		// handle /vessels.* etc
		path = Util.fixSelfKey(path);
		if (logger.isDebugEnabled())
			logger.debug("get path:" + path);
	

		String user = resource.getRequest().getHeader("X-User");
		String pass = resource.getRequest().getHeader("X-Pass");
		if (logger.isDebugEnabled()) {
			logger.debug("User:" + user + ":" + pass);
		}
		if(StringUtils.isBlank(path)){
			path = vessels;
		}
		Util.readAll(user, pass, path, resource);
		
	}


	public void post(AtmosphereResource resource, String path) {
		String body = resource.getRequest().body().asString();
		if (logger.isDebugEnabled())
			logger.debug("Post:" + body);
		String user = resource.getRequest().getHeader("X-User");
		String pass = resource.getRequest().getHeader("X-Pass");
		if (logger.isDebugEnabled()) {
			logger.debug("User:" + user + ":" + pass);
		}
		try {
			Util.sendRawMessage(user, pass, body);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			try {
				resource.getResponse().sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			} catch (IOException e1) {
				logger.error(e1.getMessage(), e1);
			}
		}
	}

	public void put(AtmosphereResource resource, String path) {
		String body = resource.getRequest().body().asString();
		if (logger.isDebugEnabled())
			logger.debug("Post:" + body);
		String user = resource.getRequest().getHeader("X-User");
		String pass = resource.getRequest().getHeader("X-Pass");
		if (logger.isDebugEnabled()) {
			logger.debug("User:" + user + ":" + pass);
		}
		try {
			Util.sendRawMessage(user, pass, body);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			try {
				resource.getResponse().sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			} catch (IOException e1) {
				logger.error(e1.getMessage(), e1);
			}
		}
	}

}
