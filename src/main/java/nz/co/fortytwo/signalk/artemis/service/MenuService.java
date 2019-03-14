package nz.co.fortytwo.signalk.artemis.service;

import java.io.File;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mjson.Json;
@AtmosphereService(
		dispatch = true,
		interceptors = {AtmosphereResourceLifecycleInterceptor.class, TrackMessageSizeInterceptor.class},
		path = "/signalk/v1/menu",
		servlet = "org.glassfish.jersey.servlet.ServletContainer")
@Path("/signalk/v1/menu")
@Tag(name = "Webapp management API")
public class MenuService extends BaseApiService {
	
	private static Logger logger = LogManager.getLogger(MenuService.class);

	public MenuService() throws Exception {
		super();
		logger.debug("MenuService starting..");

	}

	

	@GET

	@Operation(summary = "Get json data for the menus", description = "Returns a list of signalk webapp names and urls")
	@ApiResponses ({
	    @ApiResponse(responseCode = "200", description = "Successful retrieval of data"),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission")
	    })
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMenuHtml() {
		
		try {
			Json list = Json.array();
			for (File f : staticDir.listFiles()) {
				if (f.isFile())
					continue;
				if(f.getName().equals("mapcache"))continue;
				if(f.getName().equals("logs"))continue;
				if(f.getName().equals("js"))continue;
				if(f.getName().equals("fonts"))continue;
				if(f.getName().equals("download"))continue;
				if(f.getName().equals("docs"))continue;
				if(f.getName().equals("css"))continue;
				if(f.getName().equals("config"))continue;
				addApp(f,list);

			}
			return Response.status(HttpStatus.SC_OK).entity(list.toString()).build();

		} catch (Exception e) {
			logger.error(e, e);
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}
	private void addApp(File f, Json list) throws Exception {
		File p = new File(f, "package.json");
		if (p.exists()) {
			Json pkg = Json.read(FileUtils.readFileToString(p));
			String name = pkg.at("name").asString();
			name=StringUtils.removePattern(name, "^.*/");
			name = StringUtils.capitalize(name);
			File i = new File(f, "index.html");
			if(!i.exists()) {
				i = new File(f, "public/index.html");
			}
			if(!i.exists()) {
				i = new File(f, "public/index.html");
			}
			String s = FileUtils.readFileToString(p);
			//signalk-static/
			String url = i.getPath();
			url=StringUtils.replaceOnce(url, "/signalk-static/", "./");
			list.add(Json.object("href",url ,"name", name));
		}else {
			for (File d : f.listFiles()) {
				if (d.isFile())
					continue;
				addApp(d, list);
			}
		}
		
	}

	
}
