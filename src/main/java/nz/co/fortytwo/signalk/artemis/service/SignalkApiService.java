package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SK_TOKEN;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.annotation.Suspend;
import org.atmosphere.cpr.AtmosphereResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mjson.Json;
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
                        schema = @Schema(example = "\"vessels.urn:mrn:signalk:uuid:a8fb07c0-1ffd-4663-899c-f16c2baf8270\"")                       		
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
                        mediaType = "application/json",
                        		schema = @Schema(example = "{ \"self\":\"urn:mrn:imo:mmsi:366982330\",\n" + 
                        				"  \"vessels\": {\n" + 
                        				"    \"urn:mrn:imo:mmsi:366982330\": {\n" + 
                        				"      \"mmsi\": \"230099999\",\n" + 
                        				"      \"navigation\": {\n" + 
                        				"        \"position\": {\n" + 
                        				"          \"value\": {\n" + 
                        				"            \"longitude\": 173.1693,\n" + 
                        				"            \"latitude\": -41.156426,\n" + 
                        				"            \"altitude\": 0\n" + 
                        				"          },\n" + 
                        				"          \"timestamp\": \"2015-01-25T12:01:01.000Z\",\n" + 
                        				"          \"$source\": \"a.suitable.path\"\n" + 
                        				"        },\n" + 
                        				"        \"courseOverGroundTrue\": {\n" + 
                        				"          \"value\": 245.69,\n" + 
                        				"          \"timestamp\": \"2015-01-25T12:01:01.000Z\",\n" + 
                        				"          \"$source\": \"a.suitable.path\"\n" + 
                        				"        }\n" + 
                        				"      }\n" + 
                        				"    }\n" + 
                        				"  },\n" + 
                        				"  \"version\": \"1.0.0\"\n" + 
                        				"}")
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
                        mediaType = "application/json",
                        schema = @Schema(example = "{\n" + 
                        		"        \"position\": {\n" + 
                        		"          \"value\": {\n" + 
                        		"            \"longitude\": 173.1693,\n" + 
                        		"            \"latitude\": -41.156426,\n" + 
                        		"            \"altitude\": 0\n" + 
                        		"          },\n" + 
                        		"          \"timestamp\": \"2015-01-25T12:01:01.000Z\",\n" + 
                        		"          \"$source\": \"a.suitable.path\"\n" + 
                        		"        },\n" + 
                        		"        \"courseOverGroundTrue\": {\n" + 
                        		"          \"value\": 245.69,\n" + 
                        		"          \"timestamp\": \"2015-01-25T12:01:01.000Z\",\n" + 
                        		"          \"$source\": \"a.suitable.path\"\n" + 
                        		"        }\n" + 
                        		"      }")
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
			@Parameter( description = "A signalk path, eg /vessels/self/navigation", example="/vessels/self/navigation") @PathParam(value = "path") String path) throws Exception {
		//String path = req.getPathInfo();
		getPath(path,cookie, null);
		return "";
	}
	
		
	@Operation(summary = "Post a signalk GET message", description = " Post a signalk GET message. Has the same result as using non-http transport. This is a 'fire-and-forget' method,"
			+ " see PUT ")
	@ApiResponses ({
	    @ApiResponse(responseCode = "200", description = "OK", 
	    		content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(example = "{\n" + 
                        		"        \"position\": {\n" + 
                        		"          \"value\": {\n" + 
                        		"            \"longitude\": 173.1693,\n" + 
                        		"            \"latitude\": -41.156426,\n" + 
                        		"            \"altitude\": 0\n" + 
                        		"          },\n" + 
                        		"          \"timestamp\": \"2015-01-25T12:01:01.000Z\",\n" + 
                        		"          \"$source\": \"a.suitable.path\"\n" + 
                        		"        },\n" + 
                        		"        \"courseOverGroundTrue\": {\n" + 
                        		"          \"value\": 245.69,\n" + 
                        		"          \"timestamp\": \"2015-01-25T12:01:01.000Z\",\n" + 
                        		"          \"$source\": \"a.suitable.path\"\n" + 
                        		"        }\n" + 
                        		"      }")
                        )
                ),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission"),
	    @ApiResponse(responseCode = "400", description = "Bad request if message is not understood")
	    })
	@Consumes(MediaType.APPLICATION_JSON)
	@POST
	public Response post(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie, 
			@Parameter( name="body", 
				description = "A signalk message",
				schema = @Schema(
						example = "{\n" + 
								"  \"context\": \"vessels.self\",\n" + 
								"  \"requestId\": \"184743-434373-348483\",\n" + 
								"  \"get\": {\n" + 
								"    \"path\": \"navigation\",\n" + 
								"  }\n" + 
								"}")) String body) {
		try {
			
			if (logger.isDebugEnabled())
				logger.debug("Post: {}" , body);
			
			sendMessage(getTempQ(),addToken(body, cookie),null,getToken(cookie));
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
                        mediaType = "application/json",
                        schema = @Schema(example = "{\n" + 
                        		"   \"requestId\": \"123345-23232-232323\",\n" + 
                        		"   \"state\": \"COMPLETED\",\n" + 
                        		"   \"statusCode\": 200\n" + 
                        		"}")
                        )
                ),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission"),
	    @ApiResponse(responseCode = "400", description = "Bad request if message is not understood")
	    })
	@Consumes(MediaType.APPLICATION_JSON)
	@PUT
	@Path( "{path:[^?]*}")
	public Response put(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie,
			@Parameter( description = "A signalk path to a leaf, eg /vessel/self/navigation/anchor/maxRadius", example="/vessel/self/navigation/anchor/maxRadius") @PathParam(value = "path") String path,
			@Parameter( name = "body", 
				description = "A signalk value to set for this key", 
				schema = @Schema(
					example = "{\n" + 
						"   \"value\": 75.0\n" + 
						"}"
					)) String body) {
		//requestId, context, state, code (result), message (optional)
		try {
			if (logger.isDebugEnabled())
				logger.debug("Put:" + body);
			//make a full message now
			Json msg = Util.getJsonPutRequest(sanitizeApiPath(path),Json.read(body));
			sendMessage(getTempQ(),addToken(msg, cookie),null,getToken(cookie));
			return Response.status(HttpStatus.SC_ACCEPTED).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.serverError().build();
		}
	}
	
	
}
