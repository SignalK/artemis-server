package nz.co.fortytwo.signalk.artemis.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

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
import nz.co.fortytwo.signalk.artemis.util.SecurityUtils;

@Path("/signalk/v1/auth")
@Tag(name = "Authentication API")
public class AuthService extends BaseApiService{

	private static Logger logger = LogManager.getLogger(AuthService.class);
	
	@Context 
	private AtmosphereResource resource;
	
	public AuthService() throws Exception{
		if(logger.isDebugEnabled())logger.debug("AuthService started");
	}
	
	@Override
	protected void initSession(String tempQ) throws Exception {
		try{
			String correlation = java.util.UUID.randomUUID().toString();
			super.initSession(correlation);
			super.setConsumer(resource, true);
			//addWebsocketCloseListener(resource);
		}catch(Exception e){
			logger.error(e,e);
			throw e;
		}
	}
	
	@Operation(summary = "Login (json)", description = "Login with username and password as json data, return token as json")
	@ApiResponses( value = {
			@ApiResponse(responseCode = "200", 
					content = @Content(
	                        mediaType = "application/json",
	                        schema = @Schema(example = "{\n" + 
	                        		"  \"requestId\": \"1234-45653-343454\",\n" + 
	                        		"  \"state\": \"COMPLETED\",\n" + 
	                        		"  \"result\": 200,\n" + 
	                        		"  \"login\": {\n" + 
	                        		"    \"token\": \"eyJhbGciOiJIUzI1NiIsI...ibtv41fOnJObT4RdOyZ_UI9is8\"\n" + 
	                        		"  }\n" + 
	                        		"}")
	                        )
	                ),
			@ApiResponse(responseCode = "500", description = "Internal server error"),
		    @ApiResponse(responseCode = "401", 
		    	description = "Unauthorized",
		    	content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(example = "{\n" + 
                        		"  \"requestId\": \"1234-45653-343454\",\n" + 
                        		"  \"state\": \"COMPLETED\",\n" + 
                        		"  \"result\": 401\n" + 
                        		"}")
                        )
                ),
		    @ApiResponse(responseCode = "501", description = "Not Implemented")
		})
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@POST
	@Suspend(contentType = MediaType.APPLICATION_JSON)
	@Path("login")
	public String login( 
			@Parameter(required=true, 
					schema = @Schema(
						example = "{\n" + 
								"  \"requestId\": \"1234-45653-343454\",\n" + 
								"  \"login\": {\n" + 
								"    \"username\": \"john_doe\",\n" + 
								"    \"password\": \"password\"\n" + 
								"  }\n" + 
								"}")) String body) {
		if (logger.isDebugEnabled())
			logger.debug("Post: {}" , body);
		Json json = Json.read(body);
		try {
			sendMessage(getTempQ(),body,json.at("requestId").asString(),(String)null);
			return "";
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Json.object()
					//.set("requestId", json.at("requestId").asString())
					.set("state", "FAILED")
					.set("result", 500).toString();
		}
		
		
	}
	
	@POST
	@Operation(summary = "Logout", description = "Logout, expires and returns the token",
			parameters = @Parameter(in = ParameterIn.COOKIE, name = "SK-TOKEN", required=true))
	@ApiResponses( value = {
			@ApiResponse(responseCode = "200", description = "OK", 
					content = @Content(
	                        mediaType = "application/json",
	                        schema = @Schema(example = "{\n" + 
	                        		"  \"requestId\": \"1234-45653-343454\",\n" + 
	                        		"  \"state\": \"COMPLETED\",\n" + 
	                        		"  \"result\": 200,\n" + 
	                        		"}")
	                        )
	                ),
			@ApiResponse(responseCode = "500", description = "Internal server error"),
		    @ApiResponse(responseCode = "501", description = "Not Implemented"),
		    @ApiResponse(responseCode = "400", description = "No token")
	})
	@Path("logout")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Json logout( 
			@Parameter(required=true, 
					schema = @Schema(
							example = "{\n" + 
									"  \"requestId\": \"1234-45653-343454\",\n" + 		
									"  \"token\": \"eyJhbGciOiJIUzI1NiIsI...ibtv41fOnJObT4RdOyZ_UI9is8\"\n" + 
									"}"))String body) {
		if (logger.isDebugEnabled())
			logger.debug("Post: {}" , body);
		Json json = Json.read(body);
		try {
			String token = json.at("token").asString();
			SecurityUtils.invalidateToken(token);
			return json.set("state", "COMPLETED")
					.set("result", 200);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return json
					.set("state", "FAILED")
					.set("result", 500);
		}
		
		
		
	}
	
	@POST
	@Operation(summary = "Validate", 
		description = "Validates the token, returning the token or an updated replacement.")
	@ApiResponses( value = {
			@ApiResponse(responseCode = "200", description = "OK" , 
					content = @Content(
	                        mediaType = "application/json",
	                        schema = @Schema(example = "{\n" + 
	                        		"  \"requestId\": \"1234-45653-343454\",\n" + 
	                        		"  \"state\": \"COMPLETED\",\n" + 
	                        		"  \"result\": 200,\n" + 
	                        		"  \"token\": \"eyJhbGciOiJIUzI1NiIsI...ibtv41fOnJObT4RdOyZ_UI9is8\"\n" + 
	                        		"}")
	                        )),
			@ApiResponse(responseCode = "500", description = "Internal server error"),
		    @ApiResponse(responseCode = "403", description = "No permission", 
					content = @Content(
	                        mediaType = "application/json",
	                        schema = @Schema(example = "{\n" + 
	                        		"  \"requestId\": \"1234-45653-343454\",\n" + 
	                        		"  \"state\": \"FAILED\",\n" + 
	                        		"  \"result\": 403,\n" + 
	                        		"}")
	                        )),
		    @ApiResponse(responseCode = "400", description = "No token", 
					content = @Content(
	                        mediaType = "application/json",
	                        schema = @Schema(example = "{\n" + 
	                        		"  \"requestId\": \"1234-45653-343454\",\n" + 
	                        		"  \"state\": \"FAILED\",\n" + 
	                        		"  \"result\": 400,\n" + 
	                        		"}")
	                        ))
					})
			
	@Path("validate")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Json validate(@Parameter(required=true, 
			schema = @Schema(
					example = "{\n" + 
							"  \"requestId\": \"1234-45653-343454\",\n" + 
							"  \"token\": \"eyJhbGciOiJIUzI1NiIsI...ibtv41fOnJObT4RdOyZ_UI9is8\"\n" + 
							"}"))String body) {
	
		Json json = Json.read(body);
		try {
			String requestId = json.at("requestId").asString();
			if(!json.has("token")) {
				return Json.object()
						.set("requestId", requestId)
						.set("state", "FAILED")
						.set("result", 400);
			} 
			try {
				String token = json.at("token").asString();
				token = SecurityUtils.validateToken(token);
				return Json.object()
						.set("requestId", requestId)
						.set("state", "COMPLETED")
						.set("result", 200)
						.set("token",token);
			}catch (Exception e) {
				return Json.object()
						.set("requestId", requestId)
						.set("state", "FAILED")
						.set("result", 403);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Json.object()
					.set("state", "FAILED")
					.set("result", 500);
		}
		
	}
	
}
