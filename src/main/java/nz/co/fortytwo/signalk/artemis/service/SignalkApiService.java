package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SIGNALK_API;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.activemq.artemis.api.core.client.SendAcknowledgementHandler;
import org.apache.activemq.artemis.core.client.impl.ClientProducerImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListener;
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

public abstract class SignalkApiService extends BaseApiService {

	
	private ConcurrentHashMap<String,AtmosphereResource> corrHash = new ConcurrentHashMap<>();
	
	public SignalkApiService() throws Exception{
		try{

		consumer.setMessageHandler(new MessageHandler() {

			@Override
			public void onMessage(ClientMessage message) {
				String recv = message.getBodyBuffer().readString();
				try {
					message.acknowledge();
				} catch (ActiveMQException e) {
					logger.error(e,e);
				}
				logger.debug("onMessage = " + recv);
				Json json = Json.read(recv);

				String correlation = message.getStringProperty(Config.AMQ_CORR_ID);
				if(correlation==null){
					logger.warn("Message received for REST API request with no correlation");
					return;
				}
				AtmosphereResource resource = corrHash.get(correlation);
				if (logger.isDebugEnabled()) {
					logger.debug("Found resource: {}, {}" ,correlation,resource);
				}
				if(resource==null ){
					logger.warn("Message received for closed REST API request");
					return;
				}
				
				resource.getResponse().setContentType("application/json");
				resource.getResponse().write(json == null ? "{}" : json.toString());
				resource.resume();
			}
		});
		}catch(Exception e){
			logger.error(e,e);
			throw e;
		}
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
		
		//add resource to correlationHash
		String correlation = java.util.UUID.randomUUID().toString();
		resource.addEventListener(new AtmosphereResourceEventListenerAdapter.OnClose() {
			
			@Override
			public void onClose(AtmosphereResourceEvent event) {
				if (logger.isDebugEnabled()) {
					logger.debug("Remove on close:" + correlation);
				}
				corrHash.remove(correlation);
			}	
		});
		resource.suspend(20000);
		corrHash.put(correlation,resource);
		sendMessage(Util.getJsonGetRequest(path).toString(),correlation);
		
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
			sendMessage(body);
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
			sendMessage(body);
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
