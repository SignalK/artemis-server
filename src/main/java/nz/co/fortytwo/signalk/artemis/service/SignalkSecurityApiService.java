package nz.co.fortytwo.signalk.artemis.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.annotation.Suspend;
import org.atmosphere.cpr.AtmosphereResource;

import nz.co.fortytwo.signalk.artemis.util.Util;

@Path("/signalk/v1/security")
public class SignalkSecurityApiService {

	private static Logger logger = LogManager.getLogger(SignalkSecurityApiService.class);
	
	java.nio.file.Path target = Paths.get("./conf/security-conf.json");
	@Context 
	private AtmosphereResource resource;

	public SignalkSecurityApiService() throws Exception{
	}

	/**
	 * @param resource
	 * @param path
	 * @throws Exception
	 */

	@Produces(MediaType.APPLICATION_JSON)
	@GET
	public Response getAll(@Context HttpServletRequest req) throws Exception {
		
		return getSecurityConf( req);
	}
	
	
	private Response getSecurityConf(HttpServletRequest req){
		try {

			return Response.status(HttpStatus.SC_OK)
					.type(MediaType.APPLICATION_JSON)
					.entity(FileUtils.readFileToByteArray(target.toFile()))
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
	public Response post(@Context HttpServletRequest req) {
		try {
			String body = Util.readString(req.getInputStream(),req.getCharacterEncoding());
			if (logger.isDebugEnabled())
				logger.debug("Post: {}" , body);
			
			FileUtils.writeStringToFile(target.toFile(), body);
			return Response.status(HttpStatus.SC_ACCEPTED).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.serverError().build();
		}
	}
	
}
