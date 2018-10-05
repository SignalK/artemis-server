package nz.co.fortytwo.signalk.artemis.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.annotation.Suspend;
import org.atmosphere.cpr.AtmosphereResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/signalk/v1/history")
@Tag(name = "History API")
public class SignalkHistoryService extends BaseApiService {


	private static Logger logger = LogManager.getLogger(SignalkHistoryService.class);
	@Context
	private AtmosphereResource resource;


	@Operation(summary = "Request signalk historic data", description = "Request Signalk history and receive data",
			parameters = { @Parameter(in = ParameterIn.COOKIE, name = "SK-TOKEN"),
					@Parameter( in = ParameterIn.PATH, description = "A signalk path", example="/vessel/self/navigation"),
							})
	@ApiResponses ({
	    @ApiResponse(responseCode = "200", description = "OK", 
	    		content = {@Content(mediaType = MediaType.APPLICATION_JSON, 
	    			examples = @ExampleObject(name="update", value = "{\"test\"}"))
	    				}),
	    @ApiResponse(responseCode = "501", description = "History not supported"),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission")
	    })
	@Suspend(contentType = MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	@Path("{file:[^?]*}")
	public Response get(@HeaderParam(HttpHeaders.COOKIE) Cookie cookie, 
			@Parameter( description = "An ISO 8601 format date/time string, defaults to current time -4h", example="2015-03-07T12:37:10.523Z" ) @QueryParam("fromTime")String fromTime,
			@Parameter( description = "An ISO 8601 format date/time string, defaults to current time", example="2016-03-07T12:37:10.523Z") @QueryParam("toTime")String toTime,
			@Parameter( description = "Returned data will be aggregated by 'timeSlice', with one data point returned per timeslice. Supports s,m,h,d abbreviations (default 10m)", example="10m") @QueryParam("timeSlice")Integer timeSlice,
			@Parameter( description = "The aggregation method for the data in a timeSlice.(average|mean|sum|count|max|min) (default 'mean')", example="mean") @QueryParam("aggregation")String aggregation)
	{
		try {
			//TODO: actually make this work!
			
			
			sendMessage(addToken("", cookie));
			return Response.status(HttpStatus.SC_ACCEPTED).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.serverError().build();
		}
		
		
	}
	
	
	@Operation(summary = "Request signalk historic data", description = "Post a Signalk HISTORY message and receive data",
			parameters = { @Parameter(in = ParameterIn.COOKIE, name = "SK-TOKEN"),
					@Parameter( name="body", description = "A signalk HISTORY message")
							})
	@ApiResponses ({
	    @ApiResponse(responseCode = "200", description = "OK", 
	    		content = {@Content(mediaType = MediaType.APPLICATION_JSON, 
	    			examples = @ExampleObject(name="update", value = "{\"test\"}"))
	    				}),
	    @ApiResponse(responseCode = "501", description = "History not supported"),
	    @ApiResponse(responseCode = "500", description = "Internal server error"),
	    @ApiResponse(responseCode = "403", description = "No permission")
	    })
	@Suspend(contentType = MediaType.APPLICATION_JSON)
	@Consumes(value = MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@POST
	public Response post(@HeaderParam(HttpHeaders.COOKIE) Cookie cookie, 
			 String body) {
		try {
			
			if (logger.isDebugEnabled())
				logger.debug("Post: {}" , body);
			
			sendMessage(addToken(body, cookie));
			return Response.status(HttpStatus.SC_ACCEPTED).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.serverError().build();
		}
		
		
	}

}
