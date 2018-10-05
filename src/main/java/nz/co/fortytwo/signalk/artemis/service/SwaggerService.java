package nz.co.fortytwo.signalk.artemis.service;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableSet;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.integration.JaxrsApplicationAndAnnotationScanner;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;


@Path( "/swagger")
public class SwaggerService {

	
	private static OpenApiContext ctx;

	public SwaggerService() throws Exception {
		super();
		if(ctx==null)init();
	}
	
	public void init() {
        try {
        	Info info = new Info()
                    .title("Signalk Web API's")
                    .description("Signalk servers support a set of REST API's to enable various funtionality")
                    //.termsOfService("http://swagger.io/terms/")
                    .contact(new Contact()
                            .email("info@signalk.org"))
                    .license(new License()
                            .name("Apache 2.0")
                            .url("http://www.apache.org/licenses/LICENSE-2.0.html"));

        	
            SwaggerConfiguration cfg = new SwaggerConfiguration()
            	.readAllResources(true)
            	.resourceClasses(
            		ImmutableSet.of(
            				SignalkStreamService.class.getCanonicalName(), 
            				AppsService.class.getCanonicalName(),
            				LoginService.class.getCanonicalName(),
            				SignalkHistoryService.class.getCanonicalName(),
            				SignalkApiService.class.getCanonicalName()
            				))
            	.openAPI(new OpenAPI().info(info))
            	.scannerClass(JaxrsApplicationAndAnnotationScanner.class.getName());
            
            ctx = new JaxrsOpenApiContextBuilder<JaxrsOpenApiContextBuilder>().openApiConfiguration(cfg).buildContext(true);
            logger.debug("SwaggerService started");
            //logger.debug(Json.pretty(ctx.read()));
            
        } catch (Exception e) {
           logger.error(e, e);
        }
	}

	private static Logger logger = LogManager.getLogger(SwaggerService.class);
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@Context HttpServletRequest req) {
		return getResponse("openapi.json",req);
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path( "openapi.json")
	public Response getJson(@Context HttpServletRequest req) {
			return getResponse("openapi.json",req);
	}
	private Response getResponse(String targetPath,HttpServletRequest req){
		
        return Response.ok().entity(Json.pretty(ctx.read())).build();

	}
	
	
}
