package nz.co.fortytwo.signalk.artemis.handler;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import nz.co.fortytwo.signalk.artemis.server.BaseServerTest;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class TrueWindIntegrationTest extends BaseServerTest{
	
	private static Logger logger = LogManager.getLogger(TrueWindIntegrationTest.class);

	
	@Test
	public void shouldReadKvQueue() throws Exception {
		readPartialKeys("admin", 10);
	}
	
	

	private void readPartialKeys(String user, int expected) throws Exception{
		try (ClientSession session = Util.getLocalhostClientSession("admin", "admin");
				ClientProducer producer = session.createProducer();	
				ClientConsumer consumer = session.createConsumer(Config.INTERNAL_KV);){

		//"$GPRMC,144629.20,A,5156.91111,N,00434.80385,E,0.295,56.4,011113,,,A*78"
		//"$IIMWV,041.5,R,24.3,N,A*08"	
			
			sendMessage(session, producer, "$GPRMC,144629.20,A,5156.91111,N,00434.80385,E,0.295,56.4,011113,,,A*78");
			sendMessage(session, producer, "$IIMWV,041.5,R,24.3,N,A*08");
			
			logger.debug("Input sent");
		
			logger.debug("Receive started");
			List<ClientMessage> replies = listen(session, consumer, Config.INTERNAL_KV, 10, 100);
			
			logger.debug("Received {} replies", replies.size());
			replies.forEach((m)->{
				logger.debug("Received {}", m);
			});
			assertTrue(replies.size()>=expected);
			//there should be a message with AMQ_INFLUX_KEY=vessels.self.environment.wind.directionTrue
			// and AMQ_INFLUX_KEY=vessels.self.environment.wind.speedTrue
			for(ClientMessage m:replies) {
				if(m.getStringProperty(Config.AMQ_INFLUX_KEY).contains(SignalKConstants.env_wind_directionTrue)) {
					return;
				}
			}
			fail();
		} 
	}


}
