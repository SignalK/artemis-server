package nz.co.fortytwo.signalk.artemis.service;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.config.service.AtmosphereService;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;

import nz.co.fortytwo.signalk.artemis.tdb.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.tdb.TDBService;
@AtmosphereService(
		dispatch = true,
		interceptors = {AtmosphereResourceLifecycleInterceptor.class, TrackMessageSizeInterceptor.class},
		path = "/signalk/control/",
		servlet = "org.glassfish.jersey.servlet.ServletContainer")
@Path("/signalk/control")
//@Api(value = "Signalk Server Control")
public class ControlService {

	private static Logger logger = LogManager.getLogger(ControlService.class);
	protected static TDBService influx = new InfluxDbService();

	public ControlService() throws Exception {
		super();
		logger.debug("ControlService starting..");
	}

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
	
	@GET
	@Path("clearDbFuture")
	public Response clearDbFuture() {
		influx.clearDbFuture();
		return Response.status(HttpStatus.SC_OK).entity("All records with dates in the future cleared from db")
				.build();
	}
	
	@GET
	@Path("clearDbVessels")
	public Response clearDb() {
		influx.clearDbVessels();
		return Response.status(HttpStatus.SC_OK).entity("All vessel records cleared from db")
				.build();
	}

	@POST
	@Path("setTime")
	public Response time(String time) {
		if (logger.isDebugEnabled())
			logger.debug("Post: {}", time);
		if (StringUtils.isNotBlank(time)) {
			// set system time
			// sudo date -s 2018-08-11T17:52:51+12:00
			try {
				String cmd = "sudo date -s '" + time + "'";
				logger.info("Executing date setting command: {}", cmd);

				Runtime.getRuntime().exec(cmd.split(" "));
				logger.info("Executed date setting command: {}", cmd);
				influx.setWrite(true);
			} catch (Exception e) {
				logger.error(e, e);
				return Response.status(HttpStatus.SC_BAD_REQUEST).entity("Only for Linux. Failed to set time: "+time)
						.build();
			}
		}
		return Response.status(HttpStatus.SC_OK).entity("Time set: "+time)
				.build();
	}

	private Response getResponse(String task) {
		try {

			if (SystemUtils.IS_OS_LINUX && System.getProperty("os.arch").startsWith("arm")) {
				if (logger.isDebugEnabled())
					logger.debug("Perform {}", task);

				ProcessBuilder builder = new ProcessBuilder("sudo", task);
				builder.start();

				return Response.status(HttpStatus.SC_OK).entity("Performing " + task + " now (may take a few minutes)")
						.build();
			}
			if (logger.isDebugEnabled())
				logger.debug("Cannot  {}, not a Raspberry Pi!", task);
			return Response.status(HttpStatus.SC_BAD_REQUEST).entity("Shutdown and Reboot are only for Raspberry Pi!")
					.build();
		} catch (Exception e) {
			logger.error(e, e);
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}

	}

}
