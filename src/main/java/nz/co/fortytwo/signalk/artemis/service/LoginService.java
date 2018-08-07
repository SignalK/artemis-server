package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.AUTH_COOKIE_NAME;
import static nz.co.fortytwo.signalk.artemis.util.SecurityUtils.authenticateUser;

import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
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
import org.joda.time.DateTime;

import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;

@Path("/signalk/authenticate")
public class LoginService {

	private static Logger logger = LogManager.getLogger(LoginService.class);
	
	@Context 
	private AtmosphereResource resource;

	public LoginService() throws Exception{
	}

	//@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@POST
	public Response authenticate( 
			@Context UriInfo uriInfo, 
			@FormParam("username") String username, 
			@FormParam("password") String password, 
			@FormParam("target") String target) throws Exception {
		
		
		//no auth, return unauthorised
		if( StringUtils.isBlank(username)||StringUtils.isBlank(password)) {
			URI uri = uriInfo.getBaseUriBuilder().path("/login.html").build();
			return Response.temporaryRedirect(uri)
					.build();
		}
		// Validate the Authorization header	
		logger.debug("username: {}",username);
			
		try {
			String token = authenticateUser(username, password);
			resource.getRequest().localAttributes().put(SignalKConstants.JWT_TOKEN, token);
			
			//login valid, redirect to initial page.
			URI uri = uriInfo.getBaseUriBuilder().path(target).build();
			if(logger.isDebugEnabled())logger.debug("Authenticated, redirect to {}", uri.toString());
			
			NewCookie c = new NewCookie(AUTH_COOKIE_NAME,token,"/","","",3600,false);
			
			return Response.seeOther(uri)
					.cookie(c)
					.build();
		} catch (Exception e) {
			//failed try again
			logger.error(e,e);
			URI uri = uriInfo.getBaseUriBuilder().path("/login.html").build();
			if(logger.isDebugEnabled())logger.debug("Unauthenticated, redirect to {}", uri.toString());
			
			return Response.temporaryRedirect(uri)
					.build();
		}
		
	}
	
	
}
