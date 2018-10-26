package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SIGNALK_API;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SIGNALK_HISTORY_API;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SK_TOKEN;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
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
import nz.co.fortytwo.signalk.artemis.util.Util;

@Path("/signalk/v1/history")
@Tag(name = "History API")
public class SignalkHistoryService extends BaseApiService {


	private static Logger logger = LogManager.getLogger(SignalkHistoryService.class);
	@Context
	private AtmosphereResource resource;

	public SignalkHistoryService() throws Exception{
	}

	@Override
	protected void initSession(String tempQ) throws Exception {
		try{
			super.initSession(tempQ);
			super.setConsumer(resource, true);
			addCloseListener(resource);
		}catch(Exception e){
			logger.error(e,e);
			throw e;
		}
	}

	@Operation(summary = "Request signalk historic data", description = "Request Signalk history and receive data")
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
	@Path("{path:[^?]*}")
//	public Response get(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie, 
	public String get(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie, 
			@Parameter( in = ParameterIn.PATH, description = "A signalk path", example="/vessel/self/navigation")@PathParam(value="path") String path,
			@Parameter( description = "An ISO 8601 format date/time string, defaults to current time -4h", example="2015-03-07T12:37:10.523Z" ) @QueryParam("fromTime")String fromTime,
			@Parameter( description = "An ISO 8601 format date/time string, defaults to current time", example="2016-03-07T12:37:10.523Z") @QueryParam("toTime")String toTime,
			@Parameter( description = "Returned data will be sorted (default asc)", example="asc") @QueryParam("sort")String sort,
			@Parameter( description = "Returned data will be aggregated by 'resolution', with one data point returned per resolution/timeslice. Supports s,m,h,d abbreviations (default 10m)", example="10m") @QueryParam("resolution")String timeSlice,
			@Parameter( description = "The aggregation method for the data in a timeSlice.(average|mean|sum|count|max|min) (default 'mean')", example="mean") @QueryParam("aggregation")String aggregation)
	{
		try {

			Map<String,String> params = new HashMap<String,String>();
			if (fromTime != null) {
				params.put("fromTime", fromTime);
				params.put("toTime", toTime);
				params.put("sort", sort);
				params.put("timeSlice", timeSlice);
				params.put("aggregation", aggregation);
			}
			// Need this piece to unmangle the parameters
			
			path=path.replace("?", "%3F");
			path=path.replace(",", "%3F").replace("+", "&").replace("_", "=").replace("%20", " ");
			String _b[] = path.split("%3F");
			if (_b.length == 2 && _b[1] != null) {
			
				String _b1[] = _b[1].split("&");
				
				for (int i=0;i<_b1.length; i++) {
					String _b2[] = _b1[i].split("=");
					params.put(_b2[0], _b2[1]);
				}
			}
			path = _b[0];
			
			getPath(path,cookie, params);
			return "";
		} 
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return "";
	}
	
	
	@Operation(summary = "Request signalk historic data", description = "Post a Signalk HISTORY message and receive data")
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
	public Response post(@Parameter(in = ParameterIn.COOKIE, name = SK_TOKEN) @CookieParam(SK_TOKEN) Cookie cookie,
			@Parameter( name="body", description = "A signalk HISTORY message") String body) {
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

	private void getPath(String path, Cookie cookie, Map<String,String> params) throws Exception {
		String correlation = java.util.UUID.randomUUID().toString();
		initSession(correlation);

		path=StringUtils.defaultIfBlank(path,"*");
		if (logger.isDebugEnabled())
			logger.debug("get raw: {}",path);
		
		path = StringUtils.removeStart(path,SIGNALK_HISTORY_API);
		path = StringUtils.removeStart(path,"/");
		// In case "api" still there remove it
		path = StringUtils.removeStart(path,"api");
		path = path.replace('/', '.');

		
		// handle /vessels.* etc
		path = Util.fixSelfKey(path);
		if (logger.isDebugEnabled())
			logger.debug("get path: {}",path);
		//String jwtToken = (String) resource.getRequest().getAttribute(SignalKConstants.JWT_TOKEN);
		if (logger.isDebugEnabled()) {//
			logger.debug("JwtToken: {}", getToken(cookie));
		}

		if (!params.isEmpty()) 
			sendMessage(Util.getJsonGetRequest(path,getToken(cookie), params).toString(),correlation);
		else	
			sendMessage(Util.getJsonGetRequest(path,getToken(cookie)).toString(),correlation);
	}
	@Deprecated
	private void get(String path, HttpServletRequest req) throws Exception {
		String correlation = java.util.UUID.randomUUID().toString();
		initSession(correlation);

		path=StringUtils.defaultIfBlank(path,"*");
		if (logger.isDebugEnabled())
			logger.debug("get raw: {} for {}",path,req.getRemoteUser());

		Map<String,String> params=new HashMap<String,String>();
		req.getParameterMap();

		path=path.replace("?", "%3F");
		path=path.replace(",", "%3F").replace("+", "&").replace("_", "=").replace("%20", " ");
		String _b[] = path.split("%3F");
		if (_b.length == 2 && _b[1] != null) {
		
			String _b1[] = _b[1].split("&");
			
			for (int i=0;i<_b1.length; i++) {
				String _b2[] = _b1[i].split("=");
				params.put(_b2[0], _b2[1]);
			}
		}

		path = _b[0];
		path = StringUtils.removeStart(path,SIGNALK_API);
		path = StringUtils.removeStart(path,"/");
		path = path.replace('/', '.');
				
		// handle /vessels.* etc
		path = Util.fixSelfKey(path);
		if (logger.isDebugEnabled())
			logger.debug("get path: {}",path);
		//String jwtToken = (String) resource.getRequest().getAttribute(SignalKConstants.JWT_TOKEN);
		if (logger.isDebugEnabled()) {//
			logger.debug("JwtToken: {}", getToken(req));
		}
		
		if (!params.isEmpty()) {
			sendMessage(Util.getJsonGetRequest(path,getToken(req), params).toString(),correlation);
		}
		else {
			sendMessage(Util.getJsonGetRequest(path,getToken(req)).toString(),correlation);
		}
	}


}
