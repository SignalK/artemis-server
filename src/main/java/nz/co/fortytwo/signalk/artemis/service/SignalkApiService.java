package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.util.SignalKConstants.CONFIG;
import static nz.co.fortytwo.signalk.util.SignalKConstants.SIGNALK_API;
import static nz.co.fortytwo.signalk.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.util.SignalKConstants.source;
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
import org.apache.http.protocol.HTTP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.cpr.AtmosphereResource;

import ch.qos.logback.core.status.Status;
import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;
import nz.co.fortytwo.signalk.util.JsonSerializer;

public abstract class SignalkApiService {

	private static Logger logger = LogManager.getLogger(SignalkApiService.class);

	public SignalkApiService() {
	}

	public void get(AtmosphereResource resource, String path) {
		if(logger.isDebugEnabled())logger.debug("get:"+path+" for "+resource.getRequest().getRemoteUser());
		//handle /self
		path=path.substring(SIGNALK_API.length()+1);
		if(path.equals("self"))path="vessels/self/uuid";
		path=path.replace('/', '.');
		
		//handle /vessels.* etc
		path=Util.fixSelfKey(path);
		if(logger.isDebugEnabled())logger.debug("get:"+path);
		String queue = path;
		if(queue.contains("."))
			queue=queue.substring(0,queue.indexOf("."));
		//TODO: no security yet!
		String user = "admin";
		try {
				
				SortedMap< String, Object> msgs = Util.readAllMessages(user, user, queue, "_AMQ_LVQ_NAME = '"+path+"' or _AMQ_LVQ_NAME like '"+path+".%'");
					//new ConcurrentSkipListMap<>();
				
				resource.getResponse().setContentType("application/json");
				if(msgs.size()>0){
					Json json = Util.mapToJson(msgs);
					if(!path.startsWith(CONFIG))
						json = Util.findNode(json, path);
					if(logger.isDebugEnabled())logger.debug("json = "+json.toString());
					resource.getResponse().write(json.toString());
				}else{
					resource.getResponse().write("{}");
				}
			
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	
	public void post(AtmosphereResource resource, String path) {
		String body = resource.getRequest().body().asString();
		if(logger.isDebugEnabled())logger.debug("Post:"+body);
		String user = "admin";
		try {
			Util.sendRawMessage(user, user, body);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			try {
				resource.getResponse().sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			} catch (IOException e1) {
				logger.error(e1.getMessage(),e1);
			}
		}
	}
	
	public void put(AtmosphereResource resource, String path) {
		//resource.getAtmosphereHandler().onRequest();
		//resource.getResponse().sendError();
	}

	
}
