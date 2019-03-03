package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SK_TOKEN;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
@AtmosphereService(
		dispatch = true,
		interceptors = {AtmosphereResourceLifecycleInterceptor.class, TrackMessageSizeInterceptor.class},
		path = "/signalk/v1/snapshot/",
		servlet = "org.glassfish.jersey.servlet.ServletContainer")
@Path("/signalk/v1/snapshot/")
@Tag(name = "REST Snapshot API")
public class SignalkSnapshotService extends BaseApiService {

	
	private static Logger logger = LogManager.getLogger(SignalkSnapshotService.class);
	
	@Context
	private HttpServletRequest request;
	
	public SignalkSnapshotService() throws Exception {
		super();
	}

	@Override
	protected void initSession(String tempQ) throws Exception {
		try{
			super.initSession(tempQ);
			super.setConsumer(getResource(request), true);
			addWebsocketCloseListener(getResource(request));
		}catch(Exception e){
			logger.error(e,e);
			throw e;
		}
	}

	@Operation(summary = "Request self uuid", description = "Returns the uuid of this vessel at 'time' to obtain the historic value.  ")
	@ApiResponses ({
		@ApiResponse(responseCode = "200", description = "OK", 
	    		content = @Content(
                        mediaType = MediaType.TEXT_PLAIN, 
                        schema = @Schema(example = "\"vessels.urn:mrn:signalk:uuid:a8fb07c0-1ffd-4663-899c-f16c2baf8270\"")                       		
                        )
                ),
	    @ApiResponse(responseCode = "501", description = "Snapshot not implemented if time parameter is understood but not implemented"),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission"),
	    @ApiResponse(responseCode = "400", description = "Bad request if time parameter is not understood")
	    })
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("self")
	public Response getSelfSnapshot(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie,
			@Parameter( description = "Optional: An ISO 8601 format date/time string", example="2015-03-07T12:37:10.523Z") @QueryParam("time")String time) throws Exception {
		//String path = req.getPathInfo();
		if (logger.isDebugEnabled())
			logger.debug("get :{} ","self");
		// handle /self
			return Response.ok().entity("\""+Config.getConfigProperty(ConfigConstants.UUID)+"\"").build();

		}
	
	/**
	 * @param resource
	 * @param path
	 * @throws Exception
	 */
	@Operation(summary = "Request all signalk data", description = "Returns the full signalk data tree as json in full format, valid at 'time' to obtain the historic value. ")
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
	    @ApiResponse(responseCode = "501", description = "History not implemented if time parameter is understood but not implemented"),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission"),
	    @ApiResponse(responseCode = "400", description = "Bad request if time parameter is not understood")
	    })
	//@Suspend(contentType = MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	public String getAllSnapshot(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie, 
			@Parameter( description = "An ISO 8601 format date/time string", example="2015-03-07T12:37:10.523Z") @QueryParam("time")String time) throws Exception {
		getPath(null,cookie, time);
		getResource(request).suspend();
		return "";
	}
	
	/**
	 * @param resource
	 * @param path
	 * @throws Exception
	 */
	@Operation(summary = "Request signalk data at path", description = "Returns the signalk data tree found at path as json in full format at 'time' to obtain the historic value. ")
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
	    @ApiResponse(responseCode = "501", description = "History not implemented if time parameter is understood but not implemented"),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission"),
	    @ApiResponse(responseCode = "400", description = "Bad request if time parameter is not understood")
	    })
	//@Suspend(contentType = MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	@Path( "{path:[^?]*}")
	public String getSnapshot(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie, 
			@Parameter( description = "A signalk path, eg /vessels/self/navigation", example="/vessel/self/navigation") @PathParam(value = "path") String path,
			@Parameter( description = "An ISO 8601 format date/time string", example="2015-03-07T12:37:10.523Z") @QueryParam("time")String time) throws Exception {
		//String path = req.getPathInfo();
		getPath(path,cookie, time);
		getResource(request).suspend();
		return "";
	}
	
	
}
