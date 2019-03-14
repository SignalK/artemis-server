package nz.co.fortytwo.signalk.artemis.service;

import java.io.File;
import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.config.service.AtmosphereService;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;

import mjson.Json;
@AtmosphereService(
		dispatch = true,
		interceptors = {AtmosphereResourceLifecycleInterceptor.class, TrackMessageSizeInterceptor.class},
		path = "/signalk/v1/logger",
		servlet = "org.glassfish.jersey.servlet.ServletContainer")

@Path("/signalk/v1/logger")
public class LoggerService {

	public LoggerService() throws Exception {
		super();
	}

	private static Logger logger = LogManager.getLogger(LoggerService.class);
	
	@GET
	@Path("config")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getData() {
		
		try {
			//send back the log4j.json
			return Response.status(HttpStatus.SC_OK)
					.entity(FileUtils.readFileToString(new File("./conf/log4j2.json")))
					.type(MediaType.APPLICATION_JSON).build();
		} catch (IOException e) {
			logger.error(e,e);
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@POST
	@Path("config")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response postData( String json) {
		
		try{
			//save it
			FileUtils.writeStringToFile(new File("./conf/log4j2.json"),json);
			return Response.status(HttpStatus.SC_ACCEPTED).build(); 
		}catch(Exception e){
			logger.error(e,e);
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
		}
		
	}
	@GET
	@Path("listLogs")
	public Response onGet(@QueryParam("logDir")String logDir) {
		
		try {
			//send back the log list
			if(StringUtils.isBlank(logDir)){
				File dir = new File("signalk-static/logs");
				return Response.status(HttpStatus.SC_OK)
						.entity(Json.array(dir.list()).toString())
						.type(MediaType.APPLICATION_JSON).build();
			}else{
				File dir = new File("signalk-static/logs/"+logDir);
				return Response.status(HttpStatus.SC_OK)
						.entity(Json.array(dir.list()).toString())
						.type(MediaType.APPLICATION_JSON).build();
			}
			
		} catch (Exception e) {
			logger.error(e,e);
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@GET
	@Path("getLogs")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getLog(
			@QueryParam("logFile")String logFile, 
			@QueryParam("logDir")String logDir) {
		
		try {
			
			//String logFile = resource.getRequest().getParameter("logFile");
			
			if(logFile.contains("/")){
				logFile=StringUtils.substringAfterLast(logFile,"/");
			}
			if(StringUtils.isBlank(logFile)){
				return Response.status(HttpStatus.SC_BAD_REQUEST).build();
			}
			File dir;
			if(StringUtils.isBlank(logDir)){
				dir = new File("signalk-static/logs/"+logFile);
			}else{
				dir = new File("signalk-static/logs/"+logDir+"/"+logFile);
			}
			if(logFile.endsWith(".log")){
				return Response.status(HttpStatus.SC_OK)
						.entity(FileUtils.readFileToString(dir))
						.type(MediaType.TEXT_PLAIN).build();
			}
			if(logFile.endsWith(".gz")){
				return Response.status(HttpStatus.SC_OK)
						.entity(FileUtils.readFileToByteArray(dir))
						.type("application/gzip").build();
			}
			
		} catch (IOException e) {
			logger.error(e,e);
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
		}
		return Response.status(HttpStatus.SC_NOT_FOUND).build();
	}
	



}
