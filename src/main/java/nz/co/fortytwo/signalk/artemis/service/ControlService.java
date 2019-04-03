package nz.co.fortytwo.signalk.artemis.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.config.service.AtmosphereService;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;

import io.swagger.v3.oas.annotations.Parameter;
import mjson.Json;
import nz.co.fortytwo.signalk.artemis.tdb.InfluxDbService;
import nz.co.fortytwo.signalk.artemis.tdb.TDBService;
import nz.co.fortytwo.signalk.artemis.util.SecurityUtils;
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
	private static java.nio.file.Path network = Paths.get("./conf/network-conf.json");
	public ControlService() throws Exception {
		super();
		logger.debug("ControlService starting..");
	}

	@GET
	@Path("shutdown")
	public Response get() {
		return getResponse("Power off","poweroff");
	}

	@GET
	@Path("restart")
	public Response execute() {
		return getResponse("Reboot","reboot");
	}
	
	@GET
	@Path("clearDbFuture")
	public Response clearDbFuture() {
		influx.clearDbFuture();
		return Response.status(HttpStatus.SC_OK).entity("All records with dates in the future cleared from db")
				.build();
	}
	
	@GET
	@Path("loadNetwork")
	public Response loadNetwork() {
		
			try {

				return Response.status(HttpStatus.SC_OK)
						.type(MediaType.APPLICATION_JSON)
						.entity(FileUtils.readFileToByteArray(network.toFile()))
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
	@POST
	@Path("configureNetwork")
	public Response configureNetwork(String body) {
		
		Json cfg = Json.read(body);
		try {
			FileUtils.writeStringToFile(network.toFile(),cfg.toString());
		} catch (IOException e) {
			logger.error(e,e);
			return Response.status(HttpStatus.SC_BAD_REQUEST).build();
		}
		Json networks = cfg.at("configuration").at("roam").at("networks");
		StringBuilder str = new StringBuilder();
		for(Json ntwk : networks.asJsonList()) {
			str.append("network={\n     ssid=\"");
			str.append(ntwk.at("ssid").asString());
			str.append("\"\n     psk=\"");
			str.append(ntwk.at("password").asString());
			str.append("\"\n     key_mgmt=WPA-PSK\n}\n");
		}
		logger.debug(str.toString());
		cfg=cfg.at("configuration");
		Response networkResp = getResponse("Network  Config","perl -0777 -i -pe \"s/(network|$).*/"+str.toString()+"/s\" /etc/wpa_supplicant/wpa_supplicant.conf");
		if(networkResp.getStatus()<399)return networkResp;
		//ok
		return getResponse("Network  Config","./setup_network", new String[] {cfg.at("hostname").asString(), cfg.at("mode").asString(),cfg.at("ssid").asString(),cfg.at("password").asString() });
		
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

	private Response getResponse(String name,String task) {
		return getResponse(name, task, (String)null);
	}
	private Response getResponse(String name, String task, String... args ) {
		try {

			if (SystemUtils.IS_OS_LINUX && System.getProperty("os.arch").startsWith("arm")) {
				if (logger.isDebugEnabled())
					logger.debug("Perform {}", task);
				List<String> cmds = new ArrayList<>();
				cmds.add("sudo");
				cmds.add(task);
				if(args!=null) {
					for(String s:args) {
						cmds.add(s);
					}
				}
				ProcessBuilder builder = new ProcessBuilder(cmds);
				builder.start();
				return Response.status(HttpStatus.SC_OK).entity("Performing " + task + " now (may take a few minutes)")
						.build();
			}
			if (logger.isDebugEnabled())
				logger.debug("Cannot  {}, not a Raspberry Pi!", task);
			return Response.status(HttpStatus.SC_BAD_REQUEST).entity(name + " is only for the Raspberry Pi!")
					.build();
		} catch (Exception e) {
			logger.error(e, e);
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}

	}

}
