package nz.co.fortytwo.signalk.artemis.service;

import java.io.IOException;
import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.http.HttpStatus;
import org.atmosphere.annotation.Suspend;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;

import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;

@Path("/signalk/v1/stream")
public class SignalkStreamService extends BaseApiService {

	@Context 
	private AtmosphereResource resource;
	
	@Context
	BroadcasterFactory broadCasterFactory;
	
	public SignalkStreamService() throws Exception{
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
				String correlation = message.getStringProperty(Config.AMQ_CORR_ID);
				broadCasterFactory.lookup(correlation).broadcast(recv == null ? "{}" : recv);
				logger.debug(broadCasterFactory.lookup(correlation).getAtmosphereResources());
			}
		});
		}catch(Exception e){
			logger.error(e,e);
			throw e;
		}
	}

	@GET
	@Suspend(contentType= MediaType.APPLICATION_JSON)
	public String getWS() throws Exception {
		
		if (logger.isDebugEnabled())
			logger.debug("get : ws for " + resource.getRequest().getRemoteUser());
		return "";
		}

	@Suspend(contentType= MediaType.APPLICATION_JSON)
	@POST
	public String post() {
		try {
			String body = Util.readString(resource.getRequest().getInputStream(),resource.getRequest().getCharacterEncoding());
			if (logger.isDebugEnabled())
				logger.debug("Post:" + body);
			String user = resource.getRequest().getHeader("X-User");
			String pass = resource.getRequest().getHeader("X-Pass");
			if (logger.isDebugEnabled()) {
				logger.debug("User:" + user + ":" + pass);
			}
			String correlationId = UUID.randomUUID().toString();
			Broadcaster bCaster = broadCasterFactory.get(correlationId);
			bCaster.addAtmosphereResource(resource);
			sendMessage(body, correlationId);
		
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			try {
				resource.getResponse().sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			} catch (IOException e1) {
				logger.error(e.getMessage(), e);
			}
		}
		return "";
	}
	
	
}
