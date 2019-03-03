package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SK_TOKEN;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.config.service.AtmosphereService;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import nz.co.fortytwo.signalk.artemis.util.SecurityUtils;
@AtmosphereService(
		dispatch = true,
		interceptors = {AtmosphereResourceLifecycleInterceptor.class, TrackMessageSizeInterceptor.class},
		path = "/signalk/v1/security",
		servlet = "org.glassfish.jersey.servlet.ServletContainer")
@Path("/signalk/v1/security")
public class SignalkSecurityApiService {

	private static Logger logger = LogManager.getLogger(SignalkSecurityApiService.class);
	
	@Context 
	private AtmosphereResource resource;

	public SignalkSecurityApiService() throws Exception{
	}

	/**

	 * @param path
	 * @throws Exception
	 */

	@Produces(MediaType.APPLICATION_JSON)
	@GET
	@Path("config")
	public Response getAll(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie ) throws Exception {
		
		return getSecurityConf();
	}
	
	
	private Response getSecurityConf(){
		try {

			return Response.status(HttpStatus.SC_OK)
					.type(MediaType.APPLICATION_JSON)
					.entity(SecurityUtils.getSecurityConfAsBytes())
					.build();
			
		}catch(NoSuchFileException | FileNotFoundException nsf){
			logger.warn(nsf.getMessage());
			return Response.status(HttpStatus.SC_NOT_FOUND).build();
		}
		catch (IOException e) {
			logger.error(e,e);
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
		}
		
	}
	

	@Consumes(MediaType.APPLICATION_JSON)
	@POST
	@Path("config")
	public Response post(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie,
			@Parameter( name="body", description = "A signalk message") String body
			) {
		try {
			if (logger.isDebugEnabled())
				logger.debug("Post: {}" , body);
			
			SecurityUtils.save(body);
			return Response.status(HttpStatus.SC_ACCEPTED).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.serverError().build();
		}
	}
	
}
