package nz.co.fortytwo.signalk.artemis.service;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.config.service.AtmosphereService;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import nz.co.fortytwo.signalk.artemis.util.Config;

@AtmosphereService(
		dispatch = true,
		interceptors = {AtmosphereResourceLifecycleInterceptor.class, TrackMessageSizeInterceptor.class},
		path = "/signalk",
		servlet = "org.glassfish.jersey.servlet.ServletContainer")
@Path("/signalk")
@Tag(name = "Signalk Root Endpoint")
public class SignalkEndpointService {

	private static Logger logger = LogManager.getLogger(SignalkEndpointService.class);

	@Operation(summary = "Request signalk endpoints and server details", description = "Returns the json endpoints message for this server")
	@ApiResponses ({
	    @ApiResponse(responseCode = "200", description = "OK",
	    		content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(example = "{\n" + 
                        		"  \"endpoints\": {\n" + 
                        		"    \"v1\": {\n" + 
                        		"      \"version\": \"1.0.0-alpha1\",\n" + 
                        		"      \"signalk-http\": \"http://localhost:3000/signalk/v1/api/\",\n" + 
                        		"      \"signalk-ws\": \"ws://localhost:3000/signalk/v1/stream\"\n" + 
                        		"    },\n" + 
                        		"    \"v3\": {\n" + 
                        		"      \"version\": \"3.0.0\",\n" + 
                        		"      \"signalk-http\": \"http://localhost/signalk/v3/api/\",\n" + 
                        		"      \"signalk-ws\": \"ws://localhost/signalk/v3/stream\",\n" + 
                        		"      \"signalk-tcp\": \"tcp://localhost:8367\"\n" + 
                        		"    }\n" + 
                        		"  },\n" + 
                        		"  \"server\": {\n" + 
                        		"    \"id\": \"signalk-server-node\",\n" + 
                        		"    \"version\": \"0.1.33\"\n" + 
                        		"  }\n" + 
                        		"}")
                )),
	    @ApiResponse(responseCode = "500", description = "Internal server error")
	    })
	@GET
	@Produces(MediaType.APPLICATION_JSON+";charset=UTF-8")
	public Response onGet(@Context HttpServletRequest req) {
		try {
			return Response.status(HttpStatus.SC_OK)
			.entity(Config.getDiscoveryMsg(req.getLocalAddr()).toString()).build();
			
		} catch (Exception e) {
			logger.error(e,e);
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
		}
	}

}
