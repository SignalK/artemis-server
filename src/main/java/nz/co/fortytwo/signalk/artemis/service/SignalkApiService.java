package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SIGNALK_API;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.annotation.Suspend;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.websocket.WebSocketEventListenerAdapter;

import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

@Path("/signalk/v1/api")
public class SignalkApiService extends BaseApiService {

	private static Logger logger = LogManager.getLogger(SignalkApiService.class);
	
	@Context 
	private AtmosphereResource resource;

	public SignalkApiService() throws Exception{
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

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("self")
	public Response getSelf(@Context HttpServletRequest req) throws Exception {
		String path = req.getPathInfo();
		if (logger.isDebugEnabled())
			logger.debug("get :{} for {}",path, req.getRemoteUser());
		// handle /self
			return Response.ok().entity("\""+Config.getConfigProperty(ConfigConstants.UUID)+"\"").build();

		}
	
	/**
	 * @param resource
	 * @param path
	 * @throws Exception
	 */
	@Suspend(contentType = MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	public String getAll(@Context HttpServletRequest req) throws Exception {
		get(null,req);
		return "";
	}
	
	/**
	 * @param resource
	 * @param path
	 * @throws Exception
	 */
	@Suspend(contentType = MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	@Path( "{file:[^?]*}")
	public String get(@Context HttpServletRequest req) throws Exception {
		String path = req.getPathInfo();
		get(path,req);
		return "";
	}
	
	private void get(String path, HttpServletRequest req) throws Exception {
		String correlation = java.util.UUID.randomUUID().toString();
		initSession(correlation);

		path=StringUtils.defaultIfBlank(path,"*");
		if (logger.isDebugEnabled())
			logger.debug("get raw: {} for {}",path,req.getRemoteUser());
		
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
		
		sendMessage(Util.getJsonGetRequest(path,getToken(req)).toString(),correlation);
		
	}

	

	@Consumes(MediaType.APPLICATION_JSON)
	@POST
	public Response post(@Context HttpServletRequest req) {
		try {
			String body = Util.readString(req.getInputStream(),req.getCharacterEncoding());
			if (logger.isDebugEnabled())
				logger.debug("Post: {}" , body);
			
			sendMessage(addToken(body, req));
			return Response.status(HttpStatus.SC_ACCEPTED).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.serverError().build();
		}
	}
	
	@Consumes(MediaType.APPLICATION_JSON)
	@PUT
	public Response put(@Context HttpServletRequest req) {
		try {
			String body = Util.readString(req.getInputStream(),req.getCharacterEncoding());
			if (logger.isDebugEnabled())
				logger.debug("Post:" + body);
		
			sendMessage(body);
			return Response.status(HttpStatus.SC_ACCEPTED).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.serverError().build();
		}
	}
	
	protected void addCloseListener(AtmosphereResource resource) {
		resource.addEventListener( new WebSocketEventListenerAdapter() {
			
			@Override
			public void onResume(AtmosphereResourceEvent event) {
				if (logger.isDebugEnabled()) 
					logger.debug("onResume: {}",event);
				super.onResume(event);
				try {
					resource.close();
				} catch (IOException e) {
					logger.error(e,e);
				}
			}

		});
		super.addCloseListener(resource);
		
	}
}
