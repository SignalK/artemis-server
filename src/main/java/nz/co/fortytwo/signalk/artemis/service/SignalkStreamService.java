package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.PERIOD;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SUBSCRIBE;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.annotation.Suspend;
import org.atmosphere.cpr.AtmosphereResource;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Util;

@Path("/signalk/v1/stream")
public class SignalkStreamService extends BaseApiService {

	private static Logger logger = LogManager.getLogger(SignalkStreamService.class);
	@Context
	private AtmosphereResource resource;
	
	private Timer timer = new Timer();
	

	@GET
	@Suspend(contentType = MediaType.APPLICATION_JSON)
	public String getWS() throws Exception {

		if (logger.isDebugEnabled())
			logger.debug("get : ws for {}", resource.getRequest().getRemoteUser());
		//return getWebsocket();
		return "";
	}

	@Suspend(contentType = MediaType.APPLICATION_JSON)
	@POST
	public String post() {
		return getWebsocket();
	}
	
	private String getWebsocket(){
		try {
			String correlationId = "stream-"+resource.uuid(); // UUID.randomUUID().toString();
			
			//resource.suspend();
			
			String body = Util.readString(resource.getRequest().getInputStream(),
					resource.getRequest().getCharacterEncoding());
			if (logger.isDebugEnabled())
				logger.debug("Correlation: {}, Post: {}", correlationId, body);
			String user = resource.getRequest().getHeader("X-User");
			String pass = resource.getRequest().getHeader("X-Pass");
			if (logger.isDebugEnabled()) {
				logger.debug("User:{}:{}", user, pass);
			}
			//if(StringUtils.isBlank(body)) return "";
			
			initSession(correlationId);
			setConsumer(resource, false);
			addCloseListener(resource);
			
			Json json = Json.read(body);
			long period = getLongestPeriod(json);
			setConnectionWatcher(period);
			
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
				if (logger.isDebugEnabled())logger.debug("Checking broadcast age < {}",period );
				if(System.currentTimeMillis()-lastBroadcast>period){
					try {
						if (logger.isDebugEnabled())logger.debug("Checking broadcast failed: {} , closing...",System.currentTimeMillis()-lastBroadcast );
						resource.close();
						timer.cancel();
						cancel();
					} catch (IOException e) {
						logger.error(e,e);
					}
				}
			}
		};
		timer.schedule(task, period, period);
		
	}

	private long getLongestPeriod(Json json) {
		long period = 5000;
		if (json.has(SUBSCRIBE)) {
			if (json.at(SUBSCRIBE).isArray()) {
				for (Json subscription : json.at(SUBSCRIBE).asJsonList()) {
					if (subscription.at(PERIOD) != null)
						period = Math.max(period,subscription.at(PERIOD).asLong());
				}
			}
		}
		return period;
	}

}
