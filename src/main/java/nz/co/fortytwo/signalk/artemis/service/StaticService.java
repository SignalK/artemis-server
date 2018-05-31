package nz.co.fortytwo.signalk.artemis.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;


@Path( "/")
public class StaticService extends BaseApiService {

	public StaticService() throws Exception {
		super();
		logger.debug("StaticService starting..");
	}

	private static Logger logger = LogManager.getLogger(StaticService.class);

	private static java.nio.file.Path staticDir = Paths.get(Config.getConfigProperty(ConfigConstants.STATIC_DIR));
	
	@GET
	@Path( "{file:[^?]+?}")
	public Response getResource(@Context HttpServletRequest req) {
		try {
			String targetPath = req.getPathInfo();
			if(StringUtils.isBlank(targetPath)|| targetPath.equals("/")){
				targetPath="/index.html";
			}
			logger.debug("serve {}",targetPath);
			java.nio.file.Path target = Paths.get(Config.getConfigProperty(ConfigConstants.STATIC_DIR),targetPath);
			
			if (!target.startsWith(staticDir)) {
				logger.warn("Forbidden request for {} from {}",target,req);
				return Response.status(HttpStatus.SC_FORBIDDEN).build();
			}
			return Response.status(HttpStatus.SC_OK)
						.entity(Files.newInputStream(target))
						.type(Files.probeContentType(target))
						.build();
			
		} catch (IOException e) {
			logger.error(e,e);
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).build();
		}
		
	}
	
	
}
