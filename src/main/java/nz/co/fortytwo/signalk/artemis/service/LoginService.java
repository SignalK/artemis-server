package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.SECURITY_SSL_ENABLE;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.AUTH_COOKIE_NAME;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.authenticateUser;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.validateToken;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SK_TOKEN;

import java.net.URI;

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
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.cpr.AtmosphereResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;

@Path("/signalk/authenticate")
@Tag(name = "Authentication API")
public class LoginService {

	private static Logger logger = LogManager.getLogger(LoginService.class);
	
	@Context 
	private AtmosphereResource resource;

	private String scheme;

	public LoginService() throws Exception{
		if(logger.isDebugEnabled())logger.debug("LoginService started");
		scheme = Config.getConfigPropertyBoolean(SECURITY_SSL_ENABLE)?"https":"http";
	}

	//@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@POST
	@Operation( hidden=true, summary= "Login with redirect to target,return token as Cookie")
	public Response authenticate( 
			@Context UriInfo uriInfo, 
			@FormParam("username") String username, 
			@FormParam("password") String password, 
			@FormParam("target") String target) throws Exception {
		
		if(logger.isDebugEnabled())logger.debug("Authentication request, {}", uriInfo.getRequestUri());
		//no auth, return unauthorised
		if( StringUtils.isBlank(username)||StringUtils.isBlank(password)) {
			URI uri = uriInfo.getRequestUriBuilder().scheme(scheme).replacePath("/login.html").replaceQuery(null).build();
			
			return Response.temporaryRedirect(uri)
					.build();
		}
		// Validate the Authorization header	
		logger.debug("username: {}",username);
			
		try {
			String token = authenticateUser(username, password);
			resource.getRequest().localAttributes().put(SignalKConstants.JWT_TOKEN, token);
			NewCookie c = new NewCookie(AUTH_COOKIE_NAME,token,"/","","",3600,false);
			
			//login valid, redirect to initial page if we have a target.
			if(StringUtils.isNotBlank(target)) {
				URI uri = uriInfo.getRequestUriBuilder().scheme(scheme).replacePath(target).replaceQuery(null).build();
				
				if(logger.isDebugEnabled())logger.debug("Authenticated, redirect to {}", uri.toString());
				
				return Response.seeOther(uri)
						.cookie(c)
						.build();
			}
			return Response.ok()
					.cookie(c)
					.build();
		} catch (Exception e) {
			//failed try again
			logger.error(e,e);
			URI uri = uriInfo.getRequestUriBuilder().scheme(scheme).replacePath("/login.html").replaceQuery(null).build();
			
			if(logger.isDebugEnabled())logger.debug("Unauthenticated, redirect to {}", uri.toString());
			
			return Response.seeOther(uri)
					.build();
		}
		
	}
	
	
	@Operation(summary = "Login (json)", description = "Login with username and password as json data, return token as Cookie",
			requestBody = @RequestBody( 
					content = {@Content(mediaType = MediaType.APPLICATION_FORM_URLENCODED, 
					
									examples = @ExampleObject( value = "username",
										name = "username", summary = "form encoded username&password")),
								@Content(mediaType = MediaType.APPLICATION_JSON, 
									examples = @ExampleObject( value = "username",
										name = "json"))
						}
				) )
	
	@ApiResponses( value = {
			@ApiResponse(responseCode = "200", description = "OK", headers = @Header(name="Set-Cookie",description  = "The new cookie is returned.")),
			@ApiResponse(responseCode = "500", description = "Internal server error"),
		    @ApiResponse(responseCode = "403", description = "No permission")
		})
	@Consumes(MediaType.APPLICATION_JSON)
	@POST
	@Path("login")
	public Response loginJson( 
			@Context UriInfo uriInfo, 
			@Parameter(required=true, example = "\"{\\\"username\\\": \\\"test\\\", \\\"password\\\":\\\"test\\\"}\"") String body) {
		try {
			//String body = Util.readString(req.getInputStream(),req.getCharacterEncoding());
			if (logger.isDebugEnabled())
				logger.debug("Post: {}" , body);
			Json json = Json.read(body);
			String username = json.at("username").asString();
			String password = json.at("password").asString();
			
			return authenticate(uriInfo, username, password, null);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.serverError().build();
		}
		
		
	}
	
	
	@Operation( summary = "Login (url-encoded)", description = "Login with username and password with form-encoded data, return token as Cookie")
	
	@ApiResponses( value = {
			@ApiResponse(responseCode = "200", description = "OK", headers = @Header(name="Set-Cookie",description  = "The new cookie is returned.")),
			@ApiResponse(responseCode = "500", description = "Internal server error"),
		    @ApiResponse(responseCode = "403", description = "No permission")
		})
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@POST
	@Path("login")
	public Response login( 
			@Context UriInfo uriInfo, 
			@FormParam("username") String username, 
			@FormParam("password") String password) throws Exception {
		return authenticate(uriInfo, username, password, null);
		
	}
	
	
	
	@GET
	@Operation(summary = "Logout", description = "Logout, returns an expired token in a Cookie",
			parameters = @Parameter(in = ParameterIn.COOKIE, name = "SK-TOKEN", required=true))
	@ApiResponses( value = {
			@ApiResponse(responseCode = "200", description = "OK", headers = @Header(name="Set-Cookie",description  = "The cookie is expired and returned.")),
			@ApiResponse(responseCode = "500", description = "Internal server error"),
		    @ApiResponse(responseCode = "403", description = "No permission"),
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
