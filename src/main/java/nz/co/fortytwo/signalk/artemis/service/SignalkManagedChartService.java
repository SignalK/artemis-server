package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.MAP_DIR;
import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.STATIC_DIR;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.CONTEXT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.PATH;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.PUT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.name;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.resources;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.value;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.values;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;
import nz.co.fortytwo.signalk.artemis.util.ZipUtils;




@Path("/signalk/v1/")
public class SignalkManagedChartService  {

	private static Logger logger = LogManager.getLogger(SignalkManagedChartService.class);
	private ClientSession txSession;
	private ClientProducer producer;

	
	public SignalkManagedChartService() throws Exception {
		txSession = Util.getVmSession("admin", "admin");
		producer = txSession.createProducer();

	}



	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(
		@FormDataParam("file") InputStream uploadedInputStream,
		@FormDataParam("file") FormDataContentDisposition fileDetail) {

		// save it
		java.nio.file.Path destination = Paths.get(Config.getConfigProperty(STATIC_DIR)+Config.getConfigProperty(MAP_DIR)+fileDetail.getName());
		long len;
		try {
			len = Files.copy(uploadedInputStream, destination, StandardCopyOption.REPLACE_EXISTING);
			Json f = Json.object();
			f.set("name",fileDetail.getName());
			f.set("size",len);
			install(destination);
	
			String output = "File uploaded to : " + destination;
	
			return Response.status(200).entity(output).build();
		} catch (Exception e) {
			logger.error(e,e);
			return Response.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}

	}

	

	private void install(java.nio.file.Path destination) throws Exception {
		if(!destination.toString().endsWith(".zip"))return;
		//unzip here
		logger.debug("Unzipping file:"+destination);
		try{
			File zipFile = destination.toFile();
			String f = destination.toFile().getName();
			f= f.substring(0,f.indexOf("."));
			File destDir = new File(Config.getConfigProperty(STATIC_DIR)+Config.getConfigProperty(MAP_DIR)+f);
			if(!destDir.exists()){
				destDir.mkdirs();
			}
			ZipUtils.unzip(destDir, zipFile);
			logger.debug("Unzipped file:"+destDir);
			//now add a reference in resources
			sendMessage(loadChart(f).toString());
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			throw e;
		}
	}
	
	public static Json loadChart(String chartName) throws Exception{
		try{
			 File destDir = new File(Config.getConfigProperty(STATIC_DIR)+Config.getConfigProperty(MAP_DIR)+chartName);
			 SAXReader reader = new SAXReader();
		     Document document = reader.read(new File(destDir, "tilemapresource.xml"));
		     
		     String title = document.getRootElement().element("Title").getText();
		     String scale = "250000";
		     if(document.getRootElement().element("Metadata")!=null){
		    	 scale = document.getRootElement().element("Metadata").attribute("scale").getText();
		     }
		     double maxRes = 0.0;
		     double minRes = Double.MAX_VALUE;
		     int maxZoom = 0;
		     int minZoom = 99;
		     Element tileSets = document.getRootElement().element("TileSets");
		     for(Object o: tileSets.elements("TileSet")){
		    	 Element e = (Element)o;
		    	 int href = Integer.parseInt(e.attribute("href").getValue());
		    	 maxZoom=Math.max(href, maxZoom);
		    	 minZoom=Math.min(href, minZoom);
		    	 double units = Double.parseDouble(e.attribute("units-per-pixel").getValue());
		    	 maxRes=Math.max(units, maxRes);
		    	 minRes=Math.min(units, minRes);
		     }
		     //now make an entry in resources
		     Json resource = createChartMsg(chartName, title, scale);
		     return resource;
		}catch(Exception e){
			logger.error(e.getMessage(),e);
			throw e;
		}
	}
	
	private static Json createChartMsg(String f, String title, String scale){
		Json val = Json.object();
		val.set(PATH, "charts." + "urn:mrn:signalk:uuid:"+java.util.UUID.randomUUID().toString());
		Json currentChart = Json.object();
		val.set(value, currentChart);
		String time = Util.getIsoTimeString();
		time = time.substring(0, time.indexOf("."));
		currentChart.set("identifier", f);
		currentChart.set(name, title);
		currentChart.set("description", title);
		currentChart.set("tilemapUrl", "/"+Config.getConfigProperty(MAP_DIR)+f);
		try{
			int scaleInt = Integer.valueOf(scale);
			currentChart.set("scale", scaleInt);
		}catch(Exception e){
			currentChart.set("scale", 0);
		}
	
		//Json update = Json.object();
		
		//update.set(value, val);

		Json updates = Json.array();
		updates.add(val);
		Json msg = Json.object();
		msg.set(CONTEXT, resources);
		msg.set(PUT, updates);
		
		if(logger.isDebugEnabled())logger.debug("Created new chart msg:"+msg);
		return msg;
	}
	private void sendMessage(String body) throws ActiveMQException {
		ClientMessage message = txSession.createMessage(false);
		message.getBodyBuffer().writeString(body);
		//message.putStringProperty(Config.AMQ_REPLY_Q, tempQ);
		
		//producer = txSession.createProducer();
		producer.send(Config.INCOMING_RAW, message);
		
	}
	
	@Override
	protected void finalize() throws Throwable {
		
		if(producer!=null){
			try{
				producer.close();
			} catch (ActiveMQException e) {
				logger.warn(e,e);
			}
		}
		if(txSession!=null){
			try{
				txSession.close();
			} catch (ActiveMQException e) {
				logger.warn(e,e);
			}
		}
		super.finalize();
	}
}

