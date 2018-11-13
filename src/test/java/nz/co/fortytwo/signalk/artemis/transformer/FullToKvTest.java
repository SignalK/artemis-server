package nz.co.fortytwo.signalk.artemis.transformer;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import nz.co.fortytwo.signalk.artemis.server.BaseServerTest;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class FullToKvTest extends BaseServerTest{
	
	private static Logger logger = LogManager.getLogger(FullToKvTest.class);

	
	@Test
	public void shouldReadKvQueue() throws Exception {
		readPartialKeys("admin", 2);
	}
	
	

	private void readPartialKeys(String user, int expected) throws Exception{
		try (ClientSession session = Util.getLocalhostClientSession("admin", "admin");
				ClientProducer producer = session.createProducer();	){
			session.start();
			
			String body = FileUtils.readFileToString(new File("./src/test/resources/samples/full/docs-data_model_multiple_values.json"));
			
			sendMessage(session, producer, body);
			
			logger.debug("Input sent");
		
			logger.debug("Receive started");
			List<ClientMessage> replies = listen(session, Config.INTERNAL_KV,3,20);
			//assertEquals(expected, replies.size());
			logger.debug("Received {} replies", replies.size());
			
			for(ClientMessage m: replies) {
				logger.debug("Received {}", m);
			}
			assertTrue(replies.size()>=expected);
		} 
	}

	

	

}