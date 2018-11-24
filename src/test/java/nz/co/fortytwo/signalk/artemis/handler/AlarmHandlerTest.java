package nz.co.fortytwo.signalk.artemis.handler;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.env_depth_belowKeel;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.nav_anchor_currentRadius;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.nav_anchor_maxRadius;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.nav_anchor_position_latitude;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.nav_anchor_position_longitude;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.nav_position_latitude;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.nav_position_longitude;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.notifications;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.uuid;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.server.BaseServerTest;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SecurityUtils;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class AlarmHandlerTest extends BaseServerTest {
	
	private static Logger logger = LogManager.getLogger(AlarmHandlerTest.class);
	
	@Rule
    public EasyMockRule rule = new EasyMockRule(this);

    @Mock
    private AlarmHandler handler;

    @Before
    public void before(){
    	handler = partialMockBuilder(AlarmHandler.class)
	    	.addMockedMethod("sendJson")
    			.createMock(); 
    	
    }
	@Test
	public void shouldStoreKey() throws Exception {
		
		///artemis-server/src/test/resources/samples/full/signalk-depth-meta-attr.json
		String depthMeta = FileUtils.readFileToString(new File("./src/test/resources/samples/full/signalk-depth-meta-attr.json"));
		depthMeta=StringUtils.replace(depthMeta, "urn:mrn:signalk:uuid:b7590868-1d62-47d9-989c-32321b349fb9", uuid);
		logger.debug(depthMeta);
		
		setupMetaKeys(depthMeta);
		String token = SecurityUtils.authenticateUser("admin", "admin");
		//normal
		ClientMessage normal = getMessage("{\"value\":4.5,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",env_depth_belowKeel,"nmea1.II",token);
		
		//warn
		ClientMessage warn = getMessage("{\"value\":2.1,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",env_depth_belowKeel,"nmea1.II",token);
	
		//alarm
		ClientMessage alarm = getMessage("{\"value\":1,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",env_depth_belowKeel,"nmea1.II",token);
		
		//normal
		ClientMessage normal1 = getMessage("{\"value\":5.3,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",env_depth_belowKeel,"nmea1.II",token);
		 
		handler.sendJson(normal,"vessels."+uuid+dot+notifications+dot+env_depth_belowKeel+".values.nmea1.II",Json.read("{\"value\":{\"method\":null,\"state\":null,\"message\":null}}"));
		handler.sendJson(warn,"vessels."+uuid+dot+notifications+dot+env_depth_belowKeel+".values.nmea1.II",Json.read("{\"value\":{\"method\":[\"visual\"],\"state\":\"warn\",\"message\":\"Shallow water!\"}}"));
		handler.sendJson(alarm,"vessels."+uuid+dot+notifications+dot+env_depth_belowKeel+".values.nmea1.II",Json.read("{\"value\":{\"method\":[\"sound\"],\"state\":\"alarm\",\"message\":\"Running aground!\"}}"));
		handler.sendJson(normal1,"vessels."+uuid+dot+notifications+dot+env_depth_belowKeel+".values.nmea1.II",Json.read("{\"value\":{\"method\":null,\"state\":null,\"message\":null}}"));
		
		replayAll();
		
		handler.consume(normal);
		handler.consume(warn);
		handler.consume(alarm);
		handler.consume(normal1);
		
//		CountDownLatch latch = new CountDownLatch(1);
//		latch.await(2, TimeUnit.SECONDS);
		
		verifyAll();
	}
	
	private void setupMetaKeys(String data) throws Exception{
		try (ClientSession session = Util.getLocalhostClientSession("admin", "admin");
				ClientProducer producer = session.createProducer();	
				ClientConsumer consumer = session.createConsumer(Config.INTERNAL_KV);){
			String token = SecurityUtils.authenticateUser("admin", "admin");
			sendMessage(session, producer, data, token);
			
		} 
	}
	

}
