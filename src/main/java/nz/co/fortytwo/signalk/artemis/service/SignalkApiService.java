package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SIGNALK_API;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SK_TOKEN;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

@Path("/signalk/v1/api/")
@Tag(name = "REST API")
public class SignalkApiService extends BaseApiService {

	private static Logger logger = LogManager.getLogger(SignalkApiService.class);
	
	@Context 
	private AtmosphereResource resource;

	public SignalkApiService() throws Exception{
	}
	@Override
	protected void initSession(String tempQ) throws Exception {
		try{
			super.initSession(tempQ);
			super.setConsumer(resource, true);
			addWebsocketCloseListener(resource);
		}catch(Exception e){
			logger.error(e,e);
			throw e;
		}
	}

	@Operation(summary = "Request self uuid", description = "Returns the uuid of this vessel.")
	@ApiResponses ({
	    @ApiResponse(responseCode = "200", description = "OK", 
	    		content = @Content(
                        mediaType = MediaType.TEXT_PLAIN, 
                        examples = @ExampleObject(name = "self", value = "\"urn:mrn:signalk:uuid:c0d79334-4e25-4245-8892-54e8ccc8021d\"")                       		
                        )
                ),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission"),
	    })
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("self")
	public Response getSelf(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie) throws Exception {
		//String path = req.getPathInfo();
		if (logger.isDebugEnabled())
			logger.debug("get :{} ","self");
		// handle /self
			return Response.ok().entity("\"vessels."+Config.getConfigProperty(ConfigConstants.UUID)+"\"").build();

		}
	
	/**
	 * @param resource
	 * @param path
	 * @throws Exception
	 */
	@Operation(summary = "Request all signalk data", description = "Returns the full signalk data tree as json in full format. ")
	@ApiResponses ({
	    @ApiResponse(responseCode = "200", description = "OK", 
	    		content = @Content(
                        mediaType = "application/json"                        		
                        )
                ),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission"),
	    })
	@Suspend(contentType = MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	public String getAll(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie) throws Exception {
		getPath(null,cookie, null);
		return "";
	}
	
	/**
	 * @param resource
	 * @param path
	 * @throws Exception
	 */
	@Operation(summary = "Request signalk data at path", description = "Returns the signalk data tree found at path as json in full format.")
	@ApiResponses ({
	    @ApiResponse(responseCode = "200", description = "OK", 
	    		content = @Content(
                        mediaType = "application/json"                        		
                        )
                ),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission"),
	    })
	@Suspend(contentType = MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	@Path( "{path:[^?]*}")
	public String get(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie, 
			@Parameter( description = "A signalk path", example="/vessel/self/navigation") @PathParam(value = "path") String path) throws Exception {
		//String path = req.getPathInfo();
		getPath(path,cookie, null);
		return "";
	}
	
		
	@Operation(summary = "Post a signalk message", description = " Post a signalk message. Has the same result as using non-http transport. This is a 'fire-and-forget' method,"
			+ " see PUT ")
	@ApiResponses ({
	    @ApiResponse(responseCode = "200", description = "OK", 
	    		content = @Content(
                        mediaType = "application/json"                        		
                        )
                ),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission"),
	    @ApiResponse(responseCode = "400", description = "Bad request if message is not understood")
	    })
	@Consumes(MediaType.APPLICATION_JSON)
	@POST
	public Response post(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie, 
			@Parameter( name="body", description = "A signalk message")String body) {
		try {
			
			if (logger.isDebugEnabled())
				logger.debug("Post: {}" , body);
			
			sendMessage(addToken(body, cookie));
			return Response.status(HttpStatus.SC_ACCEPTED).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.serverError().build();
		}
	}
	
	@Operation(summary = "PUT a signalk message", description = " PUT a signalk message. Processes the message, with request/response semantics, "
			+ "use this in preference to POST if you require confirmation of the message processing outcome.")
	@ApiResponses ({
	    @ApiResponse(responseCode = "200", description = "OK", 
	    		content = @Content(
                        mediaType = "application/json"                        		
                        )
                ),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission"),
	    @ApiResponse(responseCode = "400", description = "Bad request if message is not understood")
	    })
	@Consumes(MediaType.APPLICATION_JSON)
	@PUT
	public Response put(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie,
			@Parameter( name="body", description = "A signalk message") String body) {
		//requestId, context, state, code (result), message (optional)
		try {
			if (logger.isDebugEnabled())
				logger.debug("Post:" + body);
		
			sendMessage(addToken(body, cookie));
			return Response.status(HttpStatus.SC_ACCEPTED).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.serverError().build();
		}
	}
	
	
}
