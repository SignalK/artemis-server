package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.FORMAT_DELTA;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.POLICY_IDEAL;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SK_TOKEN;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.config.service.AtmosphereService;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import nz.co.fortytwo.signalk.artemis.util.Util;
@AtmosphereService(
		dispatch = true,
		interceptors = {AtmosphereResourceLifecycleInterceptor.class, TrackMessageSizeInterceptor.class},
		path = "/signalk/v1/stream/",
		servlet = "org.glassfish.jersey.servlet.ServletContainer")
@Path("/signalk/v1/stream")
@Tag(name = "Websocket Stream API")
public class SignalkStreamService extends BaseApiService {

	
	private static Logger logger = LogManager.getLogger(SignalkStreamService.class);
	@Context
	private HttpServletRequest request;
	
	@Operation(summary = "Request a websocket stream", description = "Submit a Signalk path and receive a stream of UPDATE messages. ")
	@ApiResponses ({
	    @ApiResponse(responseCode = "101", description = "Switching to websocket",
	    		content = @Content(
                        mediaType = "application/json",
                        schema = @Schema( example = "{\n" + 
                        		"  \"context\": \"vessels.366982330.navigation\",\n" + 
                        		"  \"updates\": [\n" + 
                        		"    {\n" + 
                        		"      \"values\": [\n" + 
                        		"        \n" + 
                        		"        {\n" + 
                        		"          \"path\": \"position\",\n" + 
                        		"          \"value\": {\n" + 
                        		"            \"longitude\": 173.1693,\n" + 
                        		"            \"latitude\": -41.156426,\n" + 
                        		"            \"altitude\": 0\n" + 
                        		"          }\n" + 
                        		"        },\n" + 
                        		"        {\n" + 
                        		"          \"path\": \"courseOverGroundTrue\",\n" + 
                        		"          \"value\": 245.69\n" + 
                        		"        }\n" + 
                        		"      ],\n" + 
                        		"      \"$source\": \"sources.gps_0183_RMC\",\n" + 
                        		"      \"timestamp\":  \"2015-03-07T12:37:10.523Z\"\n" + 
                        		"    }\n" + 
                        		"  ]\n" + 
                        		"}")
                )),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission")
	    })
	//@Suspend()
	//@Produces(MediaType.APPLICATION_JSON)
	@GET
	public String getWS(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie,
			@Parameter( description = "Subscribe mode ( none | self | all )", 
					example = "self" ) 
					@QueryParam("subscribe")String subscribe) throws Exception {
		
		if (logger.isDebugEnabled())
			logger.debug("get : ws for {}, subscribe={}", request.getRemoteUser(),subscribe);
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
		
		getWebsocket(getResource(request), Util.getSubscriptionJson(ctx,path,1000,1000,FORMAT_DELTA,POLICY_IDEAL).toString(),cookie);
		getResource(request).suspend();
		return "";
		
	}

	@Operation(summary = "Request a websocket stream", description = "Post a Signalk SUBSCRIBE message and receive a stream of UPDATE messages. ")
	@ApiResponses ({
	    @ApiResponse(responseCode = "101", description = "Switching to websocket",
	    		content = @Content(
                        mediaType = "application/json",
                        schema = @Schema( example = "{\n" + 
                        		"  \"context\": \"vessels.366982330.navigation\",\n" + 
                        		"  \"updates\": [\n" + 
                        		"    {\n" + 
                        		"      \"values\": [\n" + 
                        		"        \n" + 
                        		"        {\n" + 
                        		"          \"path\": \"position\",\n" + 
                        		"          \"value\": {\n" + 
                        		"            \"longitude\": 173.1693,\n" + 
                        		"            \"latitude\": -41.156426,\n" + 
                        		"            \"altitude\": 0\n" + 
                        		"          }\n" + 
                        		"        },\n" + 
                        		"        {\n" + 
                        		"          \"path\": \"courseOverGroundTrue\",\n" + 
                        		"          \"value\": 245.69\n" + 
                        		"        }\n" + 
                        		"      ],\n" + 
                        		"      \"$source\": \"sources.gps_0183_RMC\",\n" + 
                        		"      \"timestamp\":  \"2015-03-07T12:37:10.523Z\"\n" + 
                        		"    }\n" + 
                        		"  ]\n" + 
                        		"}")
                )),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission")
	    })
	//@Suspend(contentType = MediaType.APPLICATION_JSON)
	@Consumes(value = MediaType.APPLICATION_JSON)
	//@Produces(MediaType.APPLICATION_JSON)
	@POST
	public String post(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie,
			@Parameter( name="body", 
				description = "A signalk SUBSCRIBE message",
				schema = @Schema(example = "{\n" + 
					"  \"context\": \"vessels.self\",\n" + 
					"  \"subscribe\": [\n" + 
					"    {\n" + 
					"      \"path\": \"navigation.speedThroughWater\",\n" + 
					"      \"period\": 1000,\n" + 
					"      \"format\": \"delta\",\n" + 
					"      \"policy\": \"ideal\",\n" + 
					"      \"minPeriod\": 200\n" + 
					"    },\n" + 
					"    {\n" + 
					"      \"path\": \"navigation.logTrip\",\n" + 
					"      \"period\": 10000\n" + 
					"    }\n" + 
					"  ]\n" + 
					"}")) 
				String body) {
			
			getWebsocket(getResource(request), body,cookie);
			getResource(request).suspend();
			return "";
		
		
	}

	
	
	

}
