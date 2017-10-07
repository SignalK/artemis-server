package nz.co.fortytwo.signalk.artemis.server;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.ning.http.util.Base64;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class BaseServerTest {
	ArtemisServer server;
	private static Logger logger = LogManager.getLogger(BaseServerTest.class);

	@Before
	public void startServer() throws Exception {
		//remove self file so we have clean model
		FileUtils.writeStringToFile(new File(Util.SIGNALK_MODEL_SAVE_FILE), "{}", StandardCharsets.UTF_8);
		server = new ArtemisServer();
	}

	@After
	public void stopServer() throws Exception {
		if(server!=null)server.stop();
	}

	protected Json getSubscriptionJson(String context, String path, int period, int minPeriod, String format, String policy) {
		Json json = Json.read("{\"context\":\"" + context + "\", \"subscribe\": []}");
		Json sub = Json.object();
		sub.set("path", path);
		sub.set("period", period);
		sub.set("minPeriod", minPeriod);
		sub.set("format", format);
		sub.set("policy", policy);
		json.at("subscribe").add(sub);
		logger.debug("Created json sub: " + json);
		return json;
	}

	protected Json getUrlAsJson(String path,int restPort) throws InterruptedException, ExecutionException, IOException {
		return Json.read(getUrlAsString(path, null, null, restPort));
	}
	protected Json getUrlAsJson(String path, String user, String pass,int restPort) throws InterruptedException, ExecutionException, IOException {
		return Json.read(getUrlAsString(path, user, pass, restPort));
	}
	
	protected String getUrlAsString(String path,int restPort) throws InterruptedException, ExecutionException, IOException {
		return getUrlAsString(path, null,null, restPort);
	}
	protected String getUrlAsString(String path, String user, String pass, int restPort) throws InterruptedException, ExecutionException, IOException {
		final AsyncHttpClient c = new AsyncHttpClient();
		try {
			// get a sessionid
			Response r2 = null;
			if(user!=null){
				String auth = Base64.encode((user+":"+pass).getBytes());
				r2 = c.prepareGet("http://localhost:" + restPort + path).setHeader("Authorization", "Basic "+auth).execute().get();
			}else{
				r2 = c.prepareGet("http://localhost:" + restPort + path).execute().get();
			}
			
			String response = r2.getResponseBody();
			logger.debug("Endpoint string:" + response);
			return response;
		} finally {
			c.close();
		}
	}
}
