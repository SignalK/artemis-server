package nz.co.fortytwo.signalk.artemis.service;

import java.io.File;
import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Path("/signalk/v1/logger/config")
public class SignalkManagedLoggerService extends BaseApiService {

	public SignalkManagedLoggerService() throws Exception {
		super();
	}

	private static Logger logger = LogManager.getLogger(SignalkManagedLoggerService.class);

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getData() {
		
		try {
			//send back the log4j.json
			String log4j2 = FileUtils.readFileToString(new File("./conf/log4j2.json"));
			return Response.status(HttpStatus.SC_OK)
					.entity(FileUtils.readFileToString(new File("./conf/log4j2.json")))
					.type(MediaType.APPLICATION_JSON).build();
		} catch (IOException e) {
			logger.error(e,e);
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@POST
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
	



}
