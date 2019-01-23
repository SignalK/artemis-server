package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.AUTH_COOKIE_NAME;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SK_TOKEN;

import java.net.URI;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.annotation.Suspend;
import org.atmosphere.cpr.AtmosphereResource;

import com.sun.jersey.api.uri.UriBuilderImpl;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

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
			super.initSession(tempQ);
			MessageHandler handler = new MessageHandler() {

				@Override
				public void onMessage(ClientMessage message) {
					String requestUri = resource.getRequest().getRequestURL().toString();
					URI uri = UriBuilderImpl.fromUri(requestUri)
							.scheme(scheme)
							.replacePath("/login.html")
							.replaceQuery(null).build();
					try {
						if (logger.isDebugEnabled())
							logger.debug("onMessage for client from {} : {}", requestUri, message);
						String recv = Util.readBodyBufferToString(message);
						message.acknowledge();
						
						if (StringUtils.isBlank(recv))
							recv = "{}";

						if (logger.isDebugEnabled())
							logger.debug("onMessage for client at {}, {}", getTempQ(), recv);
						
						Json tokenJson =  Json.read(recv);
						if(tokenJson.at("result").asInteger()>399) {
							redirect(uri.toASCIIString());
						}
						String token = tokenJson.at("login").at("token").asString();
						
						resource.getRequest().localAttributes().put(SignalKConstants.JWT_TOKEN, token);
						javax.servlet.http.Cookie c = new javax.servlet.http.Cookie(AUTH_COOKIE_NAME,token);
						c.setMaxAge(3600);
						c.setHttpOnly(false);
						c.setPath("/");
						
						resource.getResponse().addCookie(c);
						resource.getResponse().setStatus(Status.OK.getStatusCode());
						resource.write(recv);
						resource.resume();
						
								
					} catch (Exception e) {
						//failed try again
						logger.error(e,e);
						if(logger.isDebugEnabled())logger.debug("Unauthenticated, redirect to {}", uri.toASCIIString());
						redirect(uri.toASCIIString());
					}
				}

				private void redirect(String uri) {
					resource.getResponse().setHeader("Location", resource.getResponse().encodeRedirectUrl(uri));
					resource.getResponse().setStatus(Status.SEE_OTHER.getStatusCode());
					resource.resume();
				}
			};
			super.setConsumer(resource, true, handler);
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
								"    \"username\": \"john_doe\",\n" + 
								"    \"password\": \"password\"\n" + 
								"}")) String body) {
		if (logger.isDebugEnabled())
			logger.debug("Post: {}" , body);
		String requestId = UUID.randomUUID().toString();
		try {
			Json json = Json.object("requestId", requestId,
					"login",Json.read(body));
			
				sendMessage(getTempQ(),json.toString(),requestId,null);
				return "";
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return error(requestId,"FAILED",500,e.getMessage()).toString();
		}
		
		
	}
	
	@PUT
	@Operation(summary = "Logout", description = "Logout, invalidates and expires the token",
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
	@Produces(MediaType.APPLICATION_JSON)
	public String logout(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie) {
		if (logger.isDebugEnabled())
			logger.debug("PUT token: {}" , cookie.getValue());
		String requestId = UUID.randomUUID().toString();
		try {
			Json json = Json.object("requestId", requestId,
					"logout",Json.object("token",cookie.getValue()));
			
				sendMessage(getTempQ(),json.toString(),requestId,cookie.getValue());
				return "";
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return error(requestId,"FAILED",500,e.getMessage()).toString();
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
	@Produces(MediaType.APPLICATION_JSON)
	public String validate(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie) {
		String requestId = UUID.randomUUID().toString();
	
		try {
			Json json = Json.object("requestId", requestId,
					"login",Json.object("token",cookie.getValue()));
			
				sendMessage(getTempQ(),json.toString(),requestId,cookie.getValue());
				return "";
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return error(requestId,"FAILED",500,e.getMessage()).toString();
		}
		
	}
	
}
