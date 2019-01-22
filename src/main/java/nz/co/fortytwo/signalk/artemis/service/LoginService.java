package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.SECURITY_SSL_ENABLE;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.AUTH_COOKIE_NAME;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.authenticateUser;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.validateToken;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SK_TOKEN;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.activemq.artemis.api.core.ActiveMQException;
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
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

@Path("/signalk/authenticate")
@Tag(name = "Authentication API")
public class LoginService extends BaseApiService{

	private static Logger logger = LogManager.getLogger(LoginService.class);
	
	@Context 
	private AtmosphereResource resource;

	private String scheme;

	public LoginService() throws Exception{
		if(logger.isDebugEnabled())logger.debug("LoginService started");
		scheme = Config.getConfigPropertyBoolean(SECURITY_SSL_ENABLE)?"https":"http";
		String correlation = java.util.UUID.randomUUID().toString();
		initSession(correlation);
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
						
						
						String target = resource.getRequest().getParameter("target");
						
						
						Json tokenJson =  Json.read(recv);
						if(tokenJson.at("result").asInteger()>399) {
							resource.getResponse().sendRedirect(uri.toASCIIString());
						}
						String token = tokenJson.at("login").at("token").asString();
						
						resource.getRequest().localAttributes().put(SignalKConstants.JWT_TOKEN, token);
						javax.servlet.http.Cookie c = new javax.servlet.http.Cookie(AUTH_COOKIE_NAME,token);
						c.setMaxAge(3600);
						c.setHttpOnly(false);
						c.setPath("/");
						
						resource.getResponse().addCookie(c);
						
						//login valid, redirect to initial page if we have a target.
						if(StringUtils.isNotBlank(target) && !StringUtils.equals("undefined", target)) {
							uri = UriBuilderImpl.fromUri(requestUri)
									.scheme(scheme)
									.replacePath(target)
									.replaceQuery(null).build();
							if(logger.isDebugEnabled())logger.debug("Authenticated, redirect to {}", uri.toASCIIString());
							resource.getResponse().setHeader("Location", resource.getResponse().encodeRedirectUrl(uri.toASCIIString()));
							resource.getResponse().setStatus(Status.SEE_OTHER.getStatusCode());
							resource.resume();
						}else {
							resource.getResponse().setStatus(Status.OK.getStatusCode());
							resource.write(recv);
							resource.resume();
						}
								
					} catch (Exception e) {
						//failed try again
						logger.error(e,e);
						if(logger.isDebugEnabled())logger.debug("Unauthenticated, redirect to {}", uri.toASCIIString());
						
						resource.getResponse().setHeader("Location", resource.getResponse().encodeRedirectUrl(uri.toASCIIString()));
						resource.getResponse().setStatus(Status.SEE_OTHER.getStatusCode());
						resource.resume();
						
					}
				}
			};
			super.setConsumer(resource, true, handler);
			//addWebsocketCloseListener(resource);
		}catch(Exception e){
			logger.error(e,e);
			throw e;
		}
	}
	
	//@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@POST
	@Suspend()
	@Operation( hidden=true, summary= "Login with redirect to target,return token as Cookie")
	public String authenticate( 
			@Context UriInfo uriInfo, 
			@FormParam("username") String username, 
			@FormParam("password") String password, 
			@FormParam("target") String target) throws Exception {
		
		if(logger.isDebugEnabled())logger.debug("Authentication request, {}", uriInfo.getRequestUri());

			
		// Validate the Authorization header	
		logger.debug("username: {}",username);
			
		Json loginJson = Json.object("requestId", UUID.randomUUID().toString(), 
									"login",Json.object(
											"username", username, 
											"password", password));
		sendMessage(getTempQ(), loginJson.toString(), loginJson.at("requestId").asString(), (String) null);

		return "";	
			
		
	}
	

	
	@GET
	@Operation(summary = "Logout", description = "Logout, returns an expired token in a Cookie",
			parameters = @Parameter(in = ParameterIn.COOKIE, name = "SK-TOKEN", required=true))
	@ApiResponses( value = {
			@ApiResponse(responseCode = "200", description = "OK", headers = @Header(name="Set-Cookie",description  = "The cookie is expired and returned.")),
			@ApiResponse(responseCode = "500", description = "Internal server error"),
		    @ApiResponse(responseCode = "501", description = "Not Implemented"),
		    @ApiResponse(responseCode = "400", description = "No token")
	})
	@Path("logout")
	public Response logout( @Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie) {
		try {
			if(cookie==null) {
				return Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST).build();
			}
			//TODO: should keep and refuse this token
			NewCookie c = new NewCookie(AUTH_COOKIE_NAME,cookie.getValue(),"/","","",1,false);
			return Response.ok()
					.cookie(c)
					.build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.serverError().build();
		}
		
		
	}
	
	@GET
	@Operation(summary = "Validate", 
		description = "Validates the token if provided in a cookie, returning the token or an updated replacement (in a cookie). Returns 400 if no cookie is not provided",
				parameters = @Parameter(in = ParameterIn.COOKIE, name = "SK-TOKEN", required=true))
	@ApiResponses( value = {
			@ApiResponse(responseCode = "200", description = "OK", headers = @Header(name="Set-Cookie",description  = "The cookie is renewed and returned.")),
			@ApiResponse(responseCode = "500", description = "Internal server error"),
		    @ApiResponse(responseCode = "403", description = "No permission", headers = @Header(name="Set-Cookie",description  = "The cookie is expired and returned.")),
		    @ApiResponse(responseCode = "400", description = "No token")
		})
	@Path("validate")
	public Response validate(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie) {
		try {
			if(cookie==null) {
				return Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST).build();
			}
			if (logger.isDebugEnabled())
				logger.debug("Cookie: {}, {}", cookie.getName(), cookie.getValue());

			try {
				// Validate and update the token
				return Response.ok()
						.cookie(new NewCookie(AUTH_COOKIE_NAME, validateToken(cookie.getValue()),"/","","",3600,false))
						.build();
			} catch (Exception e) {
				logger.error(e.getMessage());
				return Response.status(javax.ws.rs.core.Response.Status.FORBIDDEN)
						.cookie(new NewCookie(AUTH_COOKIE_NAME, cookie.getValue(),"/","","",1,false))
						.build();

			}
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.serverError().build();
		}
		
		
	}
	
}
