package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.util.SignalKConstants.CONFIG;
import static nz.co.fortytwo.signalk.util.SignalKConstants.SIGNALK_API;
import static nz.co.fortytwo.signalk.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.util.SignalKConstants.resources;
import static nz.co.fortytwo.signalk.util.SignalKConstants.source;
import static nz.co.fortytwo.signalk.util.SignalKConstants.sources;
import static nz.co.fortytwo.signalk.util.SignalKConstants.timestamp;
import static nz.co.fortytwo.signalk.util.SignalKConstants.value;
import static nz.co.fortytwo.signalk.util.SignalKConstants.vessels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.servlet.http.HttpServlet;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQPropertyConversionException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.protocol.HTTP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.cpr.AtmosphereResource;

import ch.qos.logback.core.status.Status;
import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;
import nz.co.fortytwo.signalk.util.ConfigConstants;
import nz.co.fortytwo.signalk.util.JsonSerializer;

public abstract class SignalkApiService {

	private static Logger logger = LogManager.getLogger(SignalkApiService.class);

	public SignalkApiService() {
	}

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
		String queue = getQueue(path);
		if (logger.isDebugEnabled())
			logger.debug("queue:" + queue);
		path = sanitizePath(path);
		if (logger.isDebugEnabled())
			logger.debug("sanitized path:" + path);
		String filter = getFilter(path);
		if (logger.isDebugEnabled())
			logger.debug("filter:" + filter);

		String user = resource.getRequest().getHeader("X-User");
		String pass = resource.getRequest().getHeader("X-Pass");
		if (logger.isDebugEnabled()) {
			logger.debug("User:" + user + ":" + pass);
		}

		SortedMap<String, Object> msgs = Util.readAllMessages(user, pass, queue, filter);
		// new ConcurrentSkipListMap<>();
		if (logger.isDebugEnabled())
			logger.debug("map = " + msgs.toString());
		resource.getResponse().setContentType("application/json");
		if (msgs.size() > 0) {
			Json json = Util.mapToJson(msgs);
			//for REST we only send back the sub-node, so find it
			if (StringUtils.isNotBlank(path) && !path.startsWith(CONFIG))
				json = Util.findNode(json, path);
			if(json==null){
				path = StringUtils.substring(path, 0, path.lastIndexOf("."));
				json = Util.findNode(Util.mapToJson(msgs), path);
			}
			if (logger.isDebugEnabled())
				logger.debug("json = " + json.toString());
			resource.getResponse().write(json.toString());
		} else {
			resource.getResponse().write("{}");
		}

	}

	private String sanitizePath(String path) {
		String queue = StringUtils.substringBefore(path, ".");
		queue = queue.replace("*", "");
		if (StringUtils.isBlank(queue) || (!vessels.equals(queue) && vessels.startsWith(queue))) {
			path = "";
		}
		if (!sources.equals(queue) && sources.startsWith(queue)) {
			path = "";
		}
		if (!resources.equals(queue) && resources.startsWith(queue)) {
			path = "";
		}
		if (!CONFIG.equals(queue) && CONFIG.startsWith(queue)) {
			path = "";
		}
	
		path = StringUtils.removeEnd(path, "*");
		//path = StringUtils.substring(path, 0, path.lastIndexOf("."));
		return path;
	}

	private String getQueue(String path) {
		String queue = StringUtils.substringBefore(path, ".");
		queue = queue.replace("*", "");
		if (StringUtils.isBlank(queue) || (!vessels.equals(queue) && vessels.startsWith(queue))) {
			queue = vessels;

		}
		if (!sources.equals(queue) && sources.startsWith(queue)) {
			queue = sources;
		}
		if (!resources.equals(queue) && resources.startsWith(queue)) {
			queue = resources;
		}
		if (!CONFIG.equals(queue) && CONFIG.startsWith(queue)) {
			queue = CONFIG;
		}
		return queue;
	}

	private String getFilter(String path) {
		String filter = null;
		if (StringUtils.isBlank(path)) {
			filter = "_AMQ_LVQ_NAME like '%'";
		} else {
			filter = "_AMQ_LVQ_NAME like '" + path + "%'";
		}
		return filter;
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
