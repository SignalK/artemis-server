package nz.co.fortytwo.signalk.artemis.service;

import java.io.File;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;

@Path("/signalk/v1/logger/listLogs")
public class LogListService  {

	public LogListService() throws Exception {
		super();
	}

	private static Logger logger = LogManager.getLogger(LogListService.class);


	@GET
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
	
}
