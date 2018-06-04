package nz.co.fortytwo.signalk.artemis.service;

import java.io.IOException;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.annotation.Suspend;
import org.atmosphere.config.service.Ready;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceFactory;
import org.atmosphere.cpr.AtmosphereResourceSession;
import org.atmosphere.cpr.AtmosphereResourceSessionFactory;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.BroadcasterLifeCyclePolicy;
import org.atmosphere.cpr.DefaultAtmosphereResourceSessionFactory;
import org.atmosphere.inject.AtmosphereResourceSessionFactoryInjectable;

import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;

@Path("/signalk/v1/stream")
public class SignalkStreamService extends BaseApiService {

	private static Logger logger = LogManager.getLogger(SignalkStreamService.class);
	@Context
	private AtmosphereResource resource;
	
	public SignalkStreamService() throws Exception {

	}

	@GET
	@Suspend(contentType = MediaType.APPLICATION_JSON)
	public String getWS() throws Exception {

		if (logger.isDebugEnabled())
			logger.debug("get : ws for " + resource.getRequest().getRemoteUser());
		return "";
	}

	@Suspend(contentType = MediaType.APPLICATION_JSON)
	@POST
	public String post() {
		try {
			String correlationId = resource.uuid(); // UUID.randomUUID().toString();
			resourceSession=resource.getAtmosphereConfig().sessionFactory().getSession(resource,false);
			if(resourceSession == null){
				logger.debug("Initialising for : {}", correlationId);
				initSession(resource.uuid(),resource.getAtmosphereConfig().sessionFactory().getSession(resource));
				addCloseListener(resource);
			}
			
			String body = Util.readString(resource.getRequest().getInputStream(),
					resource.getRequest().getCharacterEncoding());
			if (logger.isDebugEnabled())
				logger.debug("Correlation: {}, Post: {}", correlationId, body);
			String user = resource.getRequest().getHeader("X-User");
			String pass = resource.getRequest().getHeader("X-Pass");
			if (logger.isDebugEnabled()) {
				logger.debug("User:" + user + ":" + pass);
			}
			
			Broadcaster bCaster = broadCasterFactory.lookup(correlationId);
			if (bCaster == null) {
				logger.debug("Creating broadcaster for : {}", correlationId);
				bCaster = broadCasterFactory.get(correlationId);
			}
			if (!bCaster.getAtmosphereResources().contains(resource)) {
				logger.debug("Adding resource to existing broadcaster for : {}", correlationId);
				bCaster.addAtmosphereResource(resource);
			}
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

	@Override
	protected void initSession(String tempQ, AtmosphereResourceSession atmosphereResourceSession) throws Exception {
		try {
			super.initSession(tempQ, atmosphereResourceSession);
			
			getConsumer().setMessageHandler(new MessageHandler() {

				@Override
				public void onMessage(ClientMessage message) {
					String recv = message.getBodyBuffer().readString();
					try {
						message.acknowledge();
					} catch (ActiveMQException e) {
						logger.error(e, e);
					}
					logger.debug("onMessage = " + recv);
					String correlation = message.getStringProperty(Config.AMQ_CORR_ID);
					Broadcaster bc = broadCasterFactory.lookup(correlation);
					if (bc == null){
						try {
							logger.debug("Closing: {}"+resource);
							resource.close();
						} catch (IOException e) {
							logger.error(e,e);
						}
						return;
					}
						
					if (bc.getAtmosphereResources().size() == 0) {
						// kill it
						broadCasterFactory.remove(correlation);
						return;
					}
					bc.broadcast(recv == null ? "{}" : recv);
					logger.debug(bc.getAtmosphereResources());
				}
			});
		} catch (Exception e) {
			logger.error(e, e);
			throw e;
		}

	}

}
