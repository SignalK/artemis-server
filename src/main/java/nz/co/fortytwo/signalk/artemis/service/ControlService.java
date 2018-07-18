package nz.co.fortytwo.signalk.artemis.service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.SystemUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Path("/signalk/control")
public class ControlService {

	public ControlService() throws Exception {
		super();
		logger.debug("ControlService starting..");
	}

	private static Logger logger = LogManager.getLogger(ControlService.class);

	@GET
	@Path("shutdown")
	public Response get() {
		return getResponse("poweroff");
	}

	@GET
	@Path("restart")
	public Response execute() {
		return getResponse("reboot");
	}

	private Response getResponse(String task){
		try {
			
			if(SystemUtils.IS_OS_LINUX && System.getProperty("os.arch").startsWith("arm")) {
				if (logger.isDebugEnabled())logger.debug("Perform {}",task);
				
				ProcessBuilder builder = new ProcessBuilder( "sudo", task );
				builder.start();
			
				return Response.status(HttpStatus.SC_OK).entity("Performing "+task+" now (may take a few minutes)")
						.build();
			}
			if (logger.isDebugEnabled())logger.debug("Cannot  {}, not a Raspberry Pi!",task);
			return Response.status(HttpStatus.SC_BAD_REQUEST).entity("Shutdown and Reboot are only for Raspberry Pi!")
					.build();
		}catch (Exception e) {
			logger.error(e,e);
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
		
	}
	
	
}
