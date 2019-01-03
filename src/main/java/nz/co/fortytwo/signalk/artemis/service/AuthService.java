package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.authenticateUser;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
public class AuthService {

	private static Logger logger = LogManager.getLogger(AuthService.class);
	
	public AuthService() throws Exception{
		if(logger.isDebugEnabled())logger.debug("AuthService started");
	}
	
	
		private Json authenticate( Json authRequest) throws Exception {
			if( authRequest==null || !authRequest.has("requestId")
					|| !authRequest.has("login")
					|| !authRequest.at("login").has("username") 
					|| !authRequest.at("login").has("password") ) {
				throw new IllegalArgumentException("Must have requestId, username and password");
			}
			String requestId = authRequest.at("requestId").asString();
			String username = authRequest.at("login").at("username").asString();
			String password = authRequest.at("login").at("password").asString();
			if(logger.isDebugEnabled())logger.debug("Authentication request, {} : {}", requestId, username);
			//no auth, return unauthorised
			
			if( StringUtils.isBlank(requestId)||StringUtils.isBlank(username)||StringUtils.isBlank(password)) {
				throw new IllegalArgumentException("requestId, username or password cannot be blank");
			}
			// Validate the Authorization header			
			try {
				String token = authenticateUser(username, password);
				//create the reply
				return Json.object()
						.set("requestId", requestId)
						.set("state", "COMPLETED")
						.set("result", 200)
						.set("login", Json.object("token",token));
			} catch (Exception e) {
				return Json.object()
						.set("requestId", requestId)
						.set("state", "COMPLETED")
						.set("result", 401);
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
	@Path("login")
	public Json login( 
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
			return authenticate(json);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Json.object()
					//.set("requestId", json.at("requestId").asString())
					.set("state", "FAILED")
					.set("result", 500);
		}
		
		
	}
	
	@GET
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
	
	@GET
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
