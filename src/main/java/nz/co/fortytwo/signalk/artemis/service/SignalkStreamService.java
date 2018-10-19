package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.FORMAT_DELTA;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.POLICY_IDEAL;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SK_TOKEN;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.annotation.Suspend;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceSession;
import org.atmosphere.cpr.AtmosphereResourceSessionFactory;
import org.atmosphere.websocket.WebSocket;
import org.signalk.schema.subscribe.SignalkSubscribe;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import nz.co.fortytwo.signalk.artemis.util.Util;

@Path("/signalk/v1/stream")
@Tag(name = "Websocket Stream API")
public class SignalkStreamService extends BaseApiService {

	
	private static Logger logger = LogManager.getLogger(SignalkStreamService.class);
	@Context
	private AtmosphereResource resource;
	
	@Operation(summary = "Request a websocket stream", description = "Submit a Signalk path and receive a stream of UPDATE messages. ")
	@ApiResponses ({
	    @ApiResponse(responseCode = "101", description = "Switching to websocket"),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission")
	    })
	@Suspend()
	//@Produces(MediaType.APPLICATION_JSON)
	@GET
	public Response getWS(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie,
			@Parameter( description = "A signalk path", example="/vessel/self/navigation") @QueryParam("subscribe")String subscribe) throws Exception {
		
		if (logger.isDebugEnabled())
			logger.debug("get : ws for {}, subscribe={}", resource.getRequest().getRemoteUser(),subscribe);
		String ctx = null;
		String path = null;
		if(StringUtils.isBlank(subscribe)|| "self".equals(subscribe)) {
			ctx="vessels.self";
			path="*";
		}else if( "all".equals(subscribe)) {
			ctx="vessels";
			path="*";
		}else if( "none".equals(subscribe)) {
			ctx="vessels.self";
			path="*";
		}else {
			ctx=Util.getContext(subscribe);
			if(subscribe.length()>ctx.length()) {
				path=subscribe.substring(ctx.length());
			}else {
				path="*";
			}
				
		}
		
		getWebsocket(resource, Util.getSubscriptionJson(ctx,path,1000,1000,FORMAT_DELTA,POLICY_IDEAL).toString(),cookie);
		return Response.ok().build();
		
	}

	@Operation(summary = "Request a websocket stream", description = "Post a Signalk SUBSCRIBE message and receive a stream of UPDATE messages. ")
	@ApiResponses ({
	    @ApiResponse(responseCode = "101", description = "Switching to websocket"),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission")
	    })
	@Suspend(contentType = MediaType.APPLICATION_JSON)
	@Consumes(value = MediaType.APPLICATION_JSON)
	//@Produces(MediaType.APPLICATION_JSON)
	@POST
	public String post(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie,
			@Parameter( name="body", description = "A signalk SUBSCRIBE message",schema = @Schema(implementation=SignalkSubscribe.class)) String body) {
		
			return getWebsocket(resource, body,cookie);
		
		
	}

	
	
	

}
