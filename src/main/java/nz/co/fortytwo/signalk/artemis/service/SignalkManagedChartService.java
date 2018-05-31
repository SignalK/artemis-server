package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.MAP_DIR;
import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.STATIC_DIR;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.CONTEXT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.PATH;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.PUT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.attr;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.name;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.resources;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.value;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.sun.jersey.core.header.ContentDisposition;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;
import nz.co.fortytwo.signalk.artemis.util.ZipUtils;


@Path("/signalk/v1/upload")
public class SignalkManagedChartService extends BaseApiService {

	private static Logger logger = LogManager.getLogger(SignalkManagedChartService.class);
	//private static boolean reloaded = false;
	protected static TDBService influx = new InfluxDbService();

	public SignalkManagedChartService() {
		logger.info("Startup SignalkManagedChartService");
		try {
			reloadCharts();
		} catch (Exception e) {
			logger.error(e,e);
		}
	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces("application/json")
	
	public Response post(FormDataMultiPart form) throws Exception {
		if(logger.isDebugEnabled())logger.debug("Uploading file..");
		List<String> contentRange = form.getHeaders().get("Content-Range");
		FormDataBodyPart filePart = form.getField("file[]");
		ContentDisposition header =filePart.getContentDisposition();
		InputStream fileInputStream = filePart.getValueAs(InputStream.class);
		String fileName = header.getFileName();
		
		File dest = new File(Config.getConfigProperty(STATIC_DIR) + Config.getConfigProperty(MAP_DIR) + fileName);
		
		if(contentRange!=null&& contentRange.get(0)!=null){
			String range = contentRange.get(0);
			range=StringUtils.remove(range,"bytes ");
			long total = Long.valueOf(range.split("/")[1]);
			range=StringUtils.substringBefore(range,"/");
			long start = Long.valueOf(range.split("-")[0]);
			long end = Long.valueOf(range.split("-")[1]);
			
			java.nio.file.Path destFile=Paths.get(dest.toURI());
			if(start==0){
				if(logger.isDebugEnabled())logger.debug("Uploading new file: {}", dest.getAbsoluteFile());
				FileUtils.deleteQuietly(destFile.toFile());
				FileUtils.touch(destFile.toFile());
			}
			Files.write(destFile,IOUtils.toByteArray(fileInputStream), StandardOpenOption.APPEND);
			if(total==end)install(dest);
		}else{
			FileUtils.copyInputStreamToFile(fileInputStream, dest);
			if(logger.isDebugEnabled())logger.debug("Uploading to file: {}", dest.getAbsoluteFile());
			install(dest);
		}

		Json f = Json.object();
		f.set("name", fileName);
		f.set("size", dest.length());
		

		return Response.status(200).entity(f.toString()).build();

	}

	public static void reloadCharts() throws Exception {
		
		logger.debug("Reload charts at startup");
		String staticDir = Config.getConfigProperty(STATIC_DIR);
		if (!staticDir.endsWith("/")) {
			staticDir = staticDir + "/";
		}
		NavigableMap<String, Json> map = new ConcurrentSkipListMap<>();
		Map<String, String> query = new HashMap<>();
		query.put("skey", "charts");
		// get current charts
		influx.loadResources(map, query, "signalk");
		logger.debug("Existing charts: Quan:{} : {}", map.size() / 2, map);
		File mapDir = new File(staticDir + Config.getConfigProperty(MAP_DIR));
		logger.debug("Reloading charts from: " + mapDir.getAbsolutePath());
		if (mapDir == null || !mapDir.exists() || mapDir.listFiles() == null)
			return;
		// TreeMap<String, Object> treeMap = new TreeMap<String,
		// Object>(signalkModel.getSubMap("resources.charts"));
		// get a list of current charts

		for (File chart : mapDir.listFiles()) {
			if (chart.isDirectory()) {
				boolean newChart = true;
				for (Entry<String, Json> e : map.entrySet()) {
					if (e.getKey().endsWith(attr))
						continue;
					logger.trace("Checking chart: {} :{}", chart.getName(), e.getValue());
					if (chart.getName().equals(e.getValue().at("identifier").asString())) {
						logger.debug("Existing chart: {}", chart.getName());
						newChart = false;
					}
				}
				if (newChart) {
					Json chartJson = SignalkManagedChartService.loadChart(chart.getName());
					logger.debug("Reloading: {}= {}", chart.getName(), chartJson);
					try {
						Util.sendRawMessage("admin", "admin", chartJson.toString());
					} catch (Exception e) {
						logger.warn(e.getMessage());
					}
				}

			}
		}
		logger.debug("Chart resources updated");

	}

	private void install(File zipFile) throws Exception {
		if (!zipFile.getName().endsWith(".zip"))
			return;
		// unzip here
		logger.debug("Unzipping file:" + zipFile);
		try {
			// File zipFile = destination.toFile();
			String f = zipFile.getName();
			f = f.substring(0, f.indexOf("."));
			File destDir = new File(Config.getConfigProperty(STATIC_DIR) + Config.getConfigProperty(MAP_DIR) + f);
			if (!destDir.exists()) {
				destDir.mkdirs();
			}
			ZipUtils.unzip(destDir, zipFile);
			logger.debug("Unzipped file:" + destDir);
			// now add a reference in resources
			sendChartMessage(loadChart(f).toString());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	public static Json loadChart(String chartName) throws Exception {
		try {
			File destDir = new File(
					Config.getConfigProperty(STATIC_DIR) + Config.getConfigProperty(MAP_DIR) + chartName);
			SAXReader reader = new SAXReader();
			Document document = reader.read(new File(destDir, "tilemapresource.xml"));

			String title = document.getRootElement().element("Title").getText();
			String scale = "250000";
			if (document.getRootElement().element("Metadata") != null) {
				scale = document.getRootElement().element("Metadata").attribute("scale").getText();
			}
			double maxRes = 0.0;
			double minRes = Double.MAX_VALUE;
			int maxZoom = 0;
			int minZoom = 99;
			Element tileSets = document.getRootElement().element("TileSets");
			for (Object o : tileSets.elements("TileSet")) {
				Element e = (Element) o;
				int href = Integer.parseInt(e.attribute("href").getValue());
				maxZoom = Math.max(href, maxZoom);
				minZoom = Math.min(href, minZoom);
				double units = Double.parseDouble(e.attribute("units-per-pixel").getValue());
				maxRes = Math.max(units, maxRes);
				minRes = Math.min(units, minRes);
			}
			// now make an entry in resources
			Json resource = createChartMsg(chartName, title, scale);
			return resource;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	private static Json createChartMsg(String f, String title, String scale) {
		Json val = Json.object();
		val.set(PATH, "charts." + "urn:mrn:signalk:uuid:" + java.util.UUID.randomUUID().toString());
		Json currentChart = Json.object();
		val.set(value, currentChart);
		String time = Util.getIsoTimeString();
		time = time.substring(0, time.indexOf("."));
		currentChart.set("identifier", f);
		currentChart.set(name, title);
		currentChart.set("description", title);
		currentChart.set("tilemapUrl", "/" + Config.getConfigProperty(MAP_DIR) + f);
		try {
			int scaleInt = Integer.valueOf(scale);
			currentChart.set("scale", scaleInt);
		} catch (Exception e) {
			currentChart.set("scale", 0);
		}

		// Json update = Json.object();

		// update.set(value, val);

		Json updates = Json.array();
		updates.add(val);
		Json msg = Json.object();
		msg.set(CONTEXT, resources);
		msg.set(PUT, updates);

		if (logger.isDebugEnabled())
			logger.debug("Created new chart msg:" + msg);
		return msg;
	}

	private void sendChartMessage(String body) throws ActiveMQException {
		ClientMessage message = txSession.createMessage(false);
		message.getBodyBuffer().writeString(body);
		// message.putStringProperty(Config.AMQ_REPLY_Q, tempQ);

		// producer = txSession.createProducer();
		producer.send(Config.INCOMING_RAW, message);

	}

}
