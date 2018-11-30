package nz.co.fortytwo.signalk.artemis.handler;

import static nz.co.fortytwo.signalk.artemis.util.Config.INTERNAL_KV;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.env_depth_belowKeel;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.notifications;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import nz.co.fortytwo.signalk.artemis.server.BaseServerTest;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SecurityUtils;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class AlarmHandlerIntegrationTest extends BaseServerTest{
	
	private static Logger logger = LogManager.getLogger(AlarmHandlerIntegrationTest.class);

	
	@Test
	public void shouldReadKvQueue() throws Exception {
		readPartialKeys("admin", 10);
	}
	
	

	private void readPartialKeys(String user, int expected) throws Exception{
		try (ClientSession session = Util.getLocalhostClientSession("admin", "admin");
				ClientProducer producer = session.createProducer();	
				){

			String qName=Config.INTERNAL_KV+dot+UUID.randomUUID().toString();
			session.createQueue(INTERNAL_KV, RoutingType.MULTICAST, qName);
			ClientConsumer consumer = session.createConsumer(qName);
			List<ClientMessage> replies = createListener(session, consumer, qName);
			
			
			String depthMeta = FileUtils.readFileToString(new File("./src/test/resources/samples/full/signalk-depth-meta-attr.json"));
			depthMeta=StringUtils.replace(depthMeta, "urn:mrn:signalk:uuid:b7590868-1d62-47d9-989c-32321b349fb9", uuid);
			logger.debug(depthMeta);
			String token = SecurityUtils.authenticateUser("admin", "admin");
			sendMessage(session, producer, depthMeta, token);	
			
			ClientMessage normal = getMessage("{\"value\":4.5,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",env_depth_belowKeel,"nmea1.II", token);
			producer.send(INTERNAL_KV, normal);
			//warn
			ClientMessage warn = getMessage("{\"value\":2.1,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",env_depth_belowKeel,"nmea1.II", token);
			producer.send(INTERNAL_KV, warn);
			//alarm
			ClientMessage alarm = getMessage("{\"value\":1,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",env_depth_belowKeel,"nmea1.II", token);
			producer.send(INTERNAL_KV, alarm);
			//normal
			ClientMessage normal1 = getMessage("{\"value\":5.3,\"timestamp\":\"2018-11-14T04:14:04.257Z\"}",env_depth_belowKeel,"nmea1.II", token);
			producer.send(INTERNAL_KV, normal1);
			
			
			logger.debug("Input sent");
		
			logger.debug("Receive started");
			listen(replies, 10, 100);
			
			
			//there should be a message with AMQ_INFLUX_KEY=vessels.self.environment.wind.directionTrue
			// and AMQ_INFLUX_KEY=vessels.self.environment.wind.speedTrue
			int c=0;
			for(ClientMessage m:replies) {
				if(m.getStringProperty(Config.AMQ_INFLUX_KEY).contains(notifications+dot+env_depth_belowKeel)) {
					c++;
				}
			}
			assertTrue(c>=4);
			
		} 
	}


}
