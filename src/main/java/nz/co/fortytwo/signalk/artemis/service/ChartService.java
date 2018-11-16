package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.MAP_DIR;
import static nz.co.fortytwo.signalk.artemis.util.ConfigConstants.STATIC_DIR;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.CONTEXT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.PATH;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.PUT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.attr;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.name;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.resources;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.skey;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;
import nz.co.fortytwo.signalk.artemis.util.ZipUtils;


@Path("/signalk/v1/upload")
//@Api( value="Signalk Chart Management API")
public class ChartService  {

	private static Logger logger = LogManager.getLogger(ChartService.class);
	//private static boolean reloaded = false;
	protected static TDBService influx = new InfluxDbService();

	public ChartService() {
		
	}

	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	//@Produces("application/json")
//	@ApiOperation(value = "Upload a  TMS chart", notes = "Accepts a zipfile created by https://github.com/rob42/freeboard-installer ", response = String.class)
	public Response post(FormDataMultiPart form) throws Exception {
		if(logger.isDebugEnabled())logger.debug("Uploading file..");
		List<String> contentRange = form.getHeaders().get("Content-Range");
		FormDataBodyPart filePart = form.getField("file[]");
		ContentDisposition header =filePart.getContentDisposition();
		InputStream fileInputStream = filePart.getValueAs(InputStream.class);
		String fileName = header.getFileName();
		if(!fileName.endsWith(".zip")) {
			return Response.status(HttpStatus.SC_BAD_REQUEST).entity(fileName +": Only zip files allowed!").build();
		}
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
				if(logger.isDebugEnabled())logger.debug("Uploading new file: {}, size:{}", dest.getAbsoluteFile(),total);
				FileUtils.deleteQuietly(destFile.toFile());
				FileUtils.touch(destFile.toFile());
			}else {
				if(logger.isDebugEnabled())logger.debug("Uploading continuation: {} : size:{}, this:{}-{}", dest.getAbsoluteFile(),total, start, end);
			}
			Files.write(destFile,IOUtils.toByteArray(fileInputStream), StandardOpenOption.APPEND);
			if(total == end+1) {
				install(dest);
			}
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
		
		logger.info("Reload charts at startup");
		String staticDir = Config.getConfigProperty(STATIC_DIR);
		if (!staticDir.endsWith("/")) {
			staticDir = staticDir + "/";
		}
		NavigableMap<String, Json> map = getCharts();
		
		logger.info("Existing charts: Quan:{}", map.size() / 2);
		logger.debug("Existing charts: Quan:{} : {}", map.size() / 2, map);
		File mapDir = new File(staticDir + Config.getConfigProperty(MAP_DIR));
		logger.info("Reloading charts from: " + mapDir.getAbsolutePath());
		if (mapDir == null || !mapDir.exists() || mapDir.listFiles() == null)
			return;
		// TreeMap<String, Object> treeMap = new TreeMap<String,
		// Object>(signalkModel.getSubMap("resources.charts"));
		// get a list of current charts

		for (File chart : mapDir.listFiles()) {
			if (chart.isDirectory()) {
				if (hasChart(map, chart.getName())==null) {
					Json chartJson = ChartService.loadChart(hasChart(map, chart.getName()),chart.getName());
					logger.info("Loading new chart: {}= {}", chart.getName(), chartJson);
					try {
						Util.sendRawMessage(Config.getConfigProperty(Config.ADMIN_USER),
								Config.getConfigProperty(Config.ADMIN_PWD), chartJson.toString());
					} catch (Exception e) {
						logger.warn(e.getMessage());
					}
				}

			}
		}
		logger.info("Chart resources updated");

	}

	public static String hasChart(NavigableMap<String, Json> map, String name) {
		for (Entry<String, Json> e : map.entrySet()) {
			if (e.getKey().endsWith(attr))
				continue;
			if (logger.isDebugEnabled())
				logger.debug("Checking chart: {} : {} : {}", name, e.getKey(), e.getValue());
			if (name.equals(e.getValue().at("identifier").asString())) {
				logger.info("Existing chart: {} = {}", name, e.getKey());
				return e.getKey();
			}
		}
		return null;
	}


	public static NavigableMap<String, Json> getCharts() {
		NavigableMap<String, Json> map = new ConcurrentSkipListMap<>();
		Map<String, String> query = new HashMap<>();
		query.put(skey, "charts");
		// get current charts
		influx.loadResources(map, query);
		return map;
	}


	private void install(File zipFile) throws Exception {
		if (!zipFile.getName().endsWith(".zip"))
			return;
		// unzip here
		if (logger.isDebugEnabled())logger.debug("Unzipping file:" + zipFile);
		try {
			// File zipFile = destination.toFile();
			String f = zipFile.getName();
			f = f.substring(0, f.indexOf("."));
			File destDir = new File(Config.getConfigProperty(STATIC_DIR) + Config.getConfigProperty(MAP_DIR) + f);
			if (!destDir.exists()) {
				destDir.mkdirs();
			}
			ZipUtils.unzip(destDir, zipFile);
			if (logger.isDebugEnabled())logger.debug("Unzipped file:" + destDir);
			// now add a reference in resources
			sendChartMessage(loadChart(hasChart(getCharts(), f),f).toString());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	public static Json loadChart(String key, String chartName) throws Exception {
		try {
			File destDir = new File(
					Config.getConfigProperty(STATIC_DIR) + Config.getConfigProperty(MAP_DIR) + chartName);
			SAXReader reader = new SAXReader();
			Document document = reader.read(new File(destDir, "tilemapresource.xml"));

			String title = document.getRootElement().element("Title").getText();
			String scale = "250000";
			double[] bounds = {0.0,0.0,0.0,0.0};
			if (document.getRootElement().element("Metadata") != null) {
				scale = document.getRootElement().element("Metadata").attribute("scale").getText();
			}
			if (document.getRootElement().element("BoundingBox") != null) {
				//<BoundingBox minx="170.63201130808108" miny="-43.799956482598866" maxx="179.99879176919154" maxy="-32.49905360842772"/>
				Element box = document.getRootElement().element("BoundingBox");
				
				bounds[0]=Double.valueOf(box.attribute("minx").getText());
				bounds[1]=Double.valueOf(box.attribute("miny").getText());
				bounds[2]=Double.valueOf(box.attribute("maxx").getText());
				bounds[3]=Double.valueOf(box.attribute("maxy").getText());
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
			
			Json resource = createChartMsg(key, chartName, title, scale, minZoom, maxZoom, bounds);
			return resource;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw e;
		}
	}

	/*
	 * {
			"MBTILES_01": {  //artemis uses uri format, since we could get charts from many sources we dont want name collisions
				"identifier": "MBTILES_01",  
				"name": "MBTILES_01",
				"description": "NORTH ALASKA",
				"bounds": [-202.5, 56.1700229829, -112.5, 75.4971573189],  //need to add
				"minzoom": 3,  //need to add
				"maxzoom": 19, //need to add
				"format": "png", //need to add
				"type": "tilelayer", // we should reference a std here, there are lots of formats
				"tilemapUrl": "/signalk/v1/api/resources/charts/MBTILES_01/{z}/{x}/{y}", //artemis returns the dir, not the extension. Probably the extension here makes sense, more control to the chart maker.
				"scale": "250000"
			}
		}
		*/
	private static Json createChartMsg(String key, String f, String title, String scale, int minZoom, int maxZoom, double[] bounds) {
		Json val = Json.object();
		
		if(key!=null) {
			//existing chart
			val.set(PATH, key);
		}else {
			//new chart
			val.set(PATH, "charts." + "urn:mrn:signalk:uuid:" + java.util.UUID.randomUUID().toString());
		}
		Json currentChart = Json.object();
		val.set(value, currentChart);
		String time = Util.getIsoTimeString();
		time = time.substring(0, time.indexOf("."));
		currentChart.set("identifier", f);
		currentChart.set(name, title);
		currentChart.set("description", title);
		currentChart.set("minzoom", minZoom);
		currentChart.set("maxzoom", maxZoom);
		currentChart.set("bounds", bounds);
		currentChart.set("format", "png");
		currentChart.set("type","tilelayer");
		currentChart.set("tilemapUrl", "/" + Config.getConfigProperty(MAP_DIR) + f+"/{z}/{x}/{-y}.png");
		try {
			int scaleInt = Integer.valueOf(scale);
			currentChart.set("scale", scaleInt);
		} catch (Exception e) {
			currentChart.set("scale", 0);
		}

		Json updates = Json.array();
		updates.add(val);
		Json msg = Json.object();
		msg.set(CONTEXT, resources);
		msg.set(PUT, updates);

		if (logger.isDebugEnabled())
			logger.debug("Created new chart msg:{}", msg);
		return msg;
	}

	private void sendChartMessage(String body) throws Exception {
		Util.sendRawMessage(Config.getConfigProperty(Config.ADMIN_USER),
				Config.getConfigProperty(Config.ADMIN_PWD), body);
		
	}

}
