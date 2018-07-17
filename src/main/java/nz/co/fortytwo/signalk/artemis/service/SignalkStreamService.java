package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.FORMAT_DELTA;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.PERIOD;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.POLICY_IDEAL;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SUBSCRIBE;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.annotation.Suspend;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceSession;
import org.atmosphere.cpr.AtmosphereResourceSessionFactory;
import org.atmosphere.websocket.WebSocket;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Util;

@Path("/signalk/v1/stream")
public class SignalkStreamService extends BaseApiService {

	private static final long PING_PERIOD = 5000;
	private static Logger logger = LogManager.getLogger(SignalkStreamService.class);
	@Context
	private AtmosphereResource resource;

	private static Timer timer = new Timer();

	@Suspend(contentType = MediaType.APPLICATION_JSON)
	@GET
	public String getWS(@QueryParam("subscribe")String subscribe) throws Exception {

		if (logger.isDebugEnabled())
			logger.debug("get : ws for {}, subscribe={}", resource.getRequest().getRemoteUser(),subscribe);
		if("all".equals(subscribe)) {
			return getWebsocket(Util.getSubscriptionJson("vessels.self","*",1000,1000,FORMAT_DELTA,POLICY_IDEAL).toString());
		}else{
			return getWebsocket(Util.getSubscriptionJson("vessels.self",subscribe,1000,1000,FORMAT_DELTA,POLICY_IDEAL).toString());
		}
		//return "";
	}

	@Suspend(contentType = MediaType.APPLICATION_JSON)
	@POST
	public String post() {
		try {
			String body = Util.readString(resource.getRequest().getInputStream(),
					resource.getRequest().getCharacterEncoding());
			return getWebsocket(body);
		} catch (IOException e) {
			logger.error(e,e);
			return "";
		}
		
	}

	private String getWebsocket(String body) {
		try {
			String correlationId = "stream-" + resource.uuid(); // UUID.randomUUID().toString();

			// resource.suspend();

			
			if (logger.isDebugEnabled())
				logger.debug("Correlation: {}, Post: {}", correlationId, body);
			String user = resource.getRequest().getHeader("X-User");
			String pass = resource.getRequest().getHeader("X-Pass");
			if (logger.isDebugEnabled()) {
				logger.debug("User:{}:{}", user, pass);
			}
			
			initSession(correlationId);
			if(setConsumer(resource, false)) {
				addCloseListener(resource);
				setConnectionWatcher(PING_PERIOD);
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

	private void setConnectionWatcher(long period) {
		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				AtmosphereResourceSessionFactory factory = resource.getAtmosphereConfig().framework().sessionFactory();
				AtmosphereResourceSession session = factory.getSession(resource);
				Long lastPing = (Long) session.getAttribute("lastPing");

				if (logger.isDebugEnabled())logger.debug("Get lastPing {}={}", resource.uuid(),lastPing );
				try {
					ping(resource);
					
					if (logger.isDebugEnabled())
						logger.debug("Checking broadcast age < {}", period*3);
					if (lastPing!=null && System.currentTimeMillis() - lastPing > period * 3) {
						
							if (logger.isDebugEnabled())
								logger.debug("Checking ping failed: {} , closing...",
										System.currentTimeMillis() - lastPing);
							resource.close();
							cancel();
							timer.purge();
					}
				}catch (Exception e) {
					cancel();
					timer.purge();
				}
			}

			
		};
		
		timer.schedule(task, period, period);

	}

	private void ping(AtmosphereResource resource) {
		if (logger.isDebugEnabled())
			logger.debug("Sending a ping to {}", resource.uuid());
		// send a ping
			WebSocket ws = resource.getAtmosphereConfig().websocketFactory().find(resource.uuid());
			ws.sendPing("XX".getBytes());
		
	}
	
	

}
