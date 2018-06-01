package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.SIGNALK_API;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.atmosphere.annotation.Broadcast;
import org.atmosphere.annotation.Suspend;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.BroadcasterFactory;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

@Path("/signalk/v1/api")
public class SignalkApiService extends BaseApiService {

	
	//private ConcurrentHashMap<String,AtmosphereResource> corrHash = new ConcurrentHashMap<>();
	
	@Context 
	private AtmosphereResource resource;
	
	public SignalkApiService() throws Exception{
		try{

		consumer.setMessageHandler(new MessageHandler() {

			@Override
			public void onMessage(ClientMessage message) {
				String recv = message.getBodyBuffer().readString();
				try {
					message.acknowledge();
				} catch (ActiveMQException e) {
					logger.error(e,e);
				}
				logger.debug("onMessage = " + recv);
				
				resource.write(recv == null ? "{}" : recv);
				resource.resume();
				
			}
		});
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
			logger.debug("get :" + path + " for " + req.getRemoteUser());
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
	public void getAll(@Context HttpServletRequest req) throws Exception {
		get(null,req);
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
	public void get(@Context HttpServletRequest req) throws Exception {
		String path = req.getPathInfo();
		get(path,req);
	}
	
	public void get(String path, HttpServletRequest req) throws Exception {
	
		path=StringUtils.defaultIfBlank(path,"*");
		if (logger.isDebugEnabled())
			logger.debug("get raw:" + path + " for " + req.getRemoteUser());
		// handle /self
		
		path = StringUtils.removeStart(path,SIGNALK_API);
		path = StringUtils.removeStart(path,"/");
		path = path.replace('/', '.');

		// handle /vessels.* etc
		path = Util.fixSelfKey(path);
		if (logger.isDebugEnabled())
			logger.debug("get path:" + path);
	

		String user = req.getHeader("X-User");
		String pass = req.getHeader("X-Pass");
		if (logger.isDebugEnabled()) {
			logger.debug("User:" + user + ":" + pass);
		}
		
		//add resource to correlationHash
		String correlation = java.util.UUID.randomUUID().toString();
		
		//corrHash.put(correlation,resource);
		sendMessage(Util.getJsonGetRequest(path).toString(),correlation);
		
	}

	@Consumes(MediaType.APPLICATION_JSON)
	@POST
	public Response post(@Context HttpServletRequest req) {
		try {
			String body = Util.readString(req.getInputStream(),req.getCharacterEncoding());
			if (logger.isDebugEnabled())
				logger.debug("Post:" + body);
			String user = req.getHeader("X-User");
			String pass = req.getHeader("X-Pass");
			if (logger.isDebugEnabled()) {
				logger.debug("User:" + user + ":" + pass);
			}
		
			sendMessage(body);
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
			String user = req.getHeader("X-User");
			String pass = req.getHeader("X-Pass");
			if (logger.isDebugEnabled()) {
				logger.debug("User:" + user + ":" + pass);
			}
		
			sendMessage(body);
			return Response.status(HttpStatus.SC_ACCEPTED).build();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.serverError().build();
		}
	}
	
}
