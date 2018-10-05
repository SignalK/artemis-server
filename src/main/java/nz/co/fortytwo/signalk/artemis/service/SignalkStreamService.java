package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.FORMAT_DELTA;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.POLICY_IDEAL;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.annotation.Suspend;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceSession;
import org.atmosphere.cpr.AtmosphereResourceSessionFactory;
import org.atmosphere.websocket.WebSocket;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import nz.co.fortytwo.signalk.artemis.util.Util;

@Path("/signalk/v1/stream")
@Tag(name = "Websocket Stream API")
public class SignalkStreamService extends BaseApiService {

	private static final long PING_PERIOD = 5000;
	private static Logger logger = LogManager.getLogger(SignalkStreamService.class);
	@Context
	private AtmosphereResource resource;

	private static Timer timer = new Timer();

	
	@Operation(summary = "Request a websocket stream", description = "Submit a Signalk path and receive a stream of UPDATE messages. Optionally supply startTime and playbackRate to replay history. ",
			parameters = @Parameter(in = ParameterIn.COOKIE, name = "SK-TOKEN"))
	@ApiResponses ({
	    @ApiResponse(responseCode = "101", description = "Switching to websocket", 
	    		content = @Content(
                        mediaType = "application/json"                        		
                        )
                ),
	    @ApiResponse(responseCode = "501", description = "History not implemented"),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission")
	    })
	@Suspend(contentType = MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	public String getWS(@HeaderParam(HttpHeaders.COOKIE) Cookie cookie, 
			@Parameter( description = "A signalk path", example="/vessel/self/navigation") @QueryParam("subscribe")String subscribe,
			@Parameter( description = "An ISO 8601 format date/time string", example="2015-03-07T12:37:10.523Z") @QueryParam("startTime")String startTime,
			@Parameter( description = "Playback rate multiplier, eg '2' = twice normal speed", example="2") @QueryParam("playbackRate")Double playbackRate) throws Exception {
		
		if (logger.isDebugEnabled())
			logger.debug("get : ws for {}, subscribe={}", resource.getRequest().getRemoteUser(),subscribe);
		if(StringUtils.isBlank(subscribe)|| "all".equals(subscribe)) {
			return getWebsocket(Util.getSubscriptionJson("vessels.self","*",1000,1000,FORMAT_DELTA,POLICY_IDEAL).toString(),cookie);
		}else{
			return getWebsocket(Util.getSubscriptionJson("vessels.self",subscribe,1000,1000,FORMAT_DELTA,POLICY_IDEAL).toString(),cookie);
		}
		//return "";
	}

	@Operation(summary = "Request a websocket stream", description = "Post a Signalk SUBSCRIBE message and receive a stream of UPDATE messages ",
			parameters = { @Parameter(in = ParameterIn.COOKIE, name = "SK-TOKEN"),
					@Parameter( name="body", description = "A signalk SUBSCRIBE message")
							})
	@ApiResponses ({
	    @ApiResponse(responseCode = "101", description = "Switching to websocket", 
	    		content = {@Content(mediaType = MediaType.APPLICATION_JSON, 
	    			examples = @ExampleObject(name="update", value = "{\"test\"}"))
	    				}),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission")
	    })
	@Suspend(contentType = MediaType.APPLICATION_JSON)
	@Consumes(value = MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@POST
	public String post(@HeaderParam(HttpHeaders.COOKIE) Cookie cookie, 
			 String body) {
		
			return getWebsocket(body,cookie);
		
		
	}

	private String getWebsocket(String body, Cookie cookie) {
		try {
			String correlationId = "stream-" + resource.uuid(); // UUID.randomUUID().toString();

			// resource.suspend();
			
			
			if (logger.isDebugEnabled())
				logger.debug("Correlation: {}, Post: {}", correlationId, body);
			
			initSession(correlationId);
			if(setConsumer(resource, false)) {
				addCloseListener(resource);
				setConnectionWatcher(PING_PERIOD);
			}
			sendMessage(addToken(body, cookie), correlationId);

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
