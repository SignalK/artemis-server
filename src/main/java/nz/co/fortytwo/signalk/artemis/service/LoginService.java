package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.SECURITY_SSL_ENABLE;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.AUTH_COOKIE_NAME;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SK_TOKEN;

import java.net.URI;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.config.service.AtmosphereService;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;
@AtmosphereService(
		dispatch = true,
		interceptors = {AtmosphereResourceLifecycleInterceptor.class, TrackMessageSizeInterceptor.class},
		path = "/signalk/authenticate",
		servlet = "org.glassfish.jersey.servlet.ServletContainer")
@Path("/signalk/authenticate")
@Tag(name = "Authentication API")
public class LoginService extends BaseApiService{

	private static Logger logger = LogManager.getLogger(LoginService.class);
	
	@Context
	private HttpServletRequest request;
	
	private String scheme;

	public LoginService() throws Exception{
		if(logger.isDebugEnabled())logger.debug("LoginService started");
		scheme = Config.getConfigPropertyBoolean(SECURITY_SSL_ENABLE)?"https":"http";
		
	}

	@Override
	protected void initSession(String tempQ) throws Exception {
		
		try{
			super.initSession(tempQ);
			MessageHandler handler = new MessageHandler() {

				@Override
				public void onMessage(ClientMessage message) {
					String requestUri = request.getRequestURL().toString();
					URI uri = UriBuilder.fromUri(requestUri)
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
						
						
						String target = request.getParameter("target");
						
						
						Json tokenJson =  Json.read(recv);
						if(tokenJson.at("result").asInteger()>399) {
							getResponse(request).sendRedirect(uri.toASCIIString());
							return;
						}
						String token = tokenJson.at("login").at("token").asString();
						
						getResource(request).getRequest().localAttributes().put(SignalKConstants.JWT_TOKEN, token);
						javax.servlet.http.Cookie c = new javax.servlet.http.Cookie(AUTH_COOKIE_NAME,token);
						c.setMaxAge(3600);
						c.setHttpOnly(false);
						c.setPath("/");
						
						getResponse(request).addCookie(c);
						
						//login valid, redirect to initial page if we have a target.
						if(StringUtils.isNotBlank(target) && !StringUtils.equals("undefined", target)) {
							uri = UriBuilder.fromUri(requestUri)
									.scheme(scheme)
									.replacePath(target)
									.replaceQuery(null).build();
							if(logger.isDebugEnabled())logger.debug("Authenticated, redirect to {}", uri.toASCIIString());
							getResponse(request).setHeader("Location", getResponse(request).encodeRedirectUrl(uri.toASCIIString()));
							getResponse(request).setStatus(Status.SEE_OTHER.getStatusCode());
							getResource(request).resume();
						}else {
							getResponse(request).setStatus(Status.OK.getStatusCode());
							getResource(request).write(recv);
							getResource(request).resume();
						}
								
					} catch (Exception e) {
						//failed try again
						logger.error(e,e);
						if(logger.isDebugEnabled())logger.debug("Unauthenticated, redirect to {}", uri.toASCIIString());
						
						getResponse(request).setHeader("Location", getResponse(request).encodeRedirectUrl(uri.toASCIIString()));
						getResponse(request).setStatus(Status.SEE_OTHER.getStatusCode());
						getResource(request).resume();
						
					}
				}
			};
			super.setConsumer(getResource(request), true, handler);
			//addWebsocketCloseListener(getResource(request));
		}catch(Exception e){
			logger.error(e,e);
			throw e;
		}
	}
	
	//@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@POST
	//@Suspend()
	@Operation( hidden=true, summary= "Login with redirect to target,return token as Cookie")
	public String authenticate( 
			@Context UriInfo uriInfo, 
			@FormParam("username") String username, 
			@FormParam("password") String password, 
			@FormParam("target") String target) throws Exception {
		
		if(logger.isDebugEnabled())logger.debug("Authentication request, {}", uriInfo.getRequestUri());

		initSession(getClass().getSimpleName()+"-"+java.util.UUID.randomUUID().toString());
		// Validate the Authorization header	
		logger.debug("username: {}",username);
			
		Json loginJson = Json.object("requestId", UUID.randomUUID().toString(), 
									"login",Json.object(
											"username", username, 
											"password", password));
		sendMessage(getTempQ(), loginJson.toString(), loginJson.at("requestId").asString(), (String) null);
		getResource(request).suspend();
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
	
}
