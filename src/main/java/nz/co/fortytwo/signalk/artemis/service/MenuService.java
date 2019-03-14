package nz.co.fortytwo.signalk.artemis.service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
			
			return Response.status(HttpStatus.SC_OK).entity(getAppList().toString()).build();

		} catch (Exception e) {
			logger.error(e, e);
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
	}
	

	
}
