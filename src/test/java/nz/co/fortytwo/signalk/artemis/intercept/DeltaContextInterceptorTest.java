package nz.co.fortytwo.signalk.artemis.intercept;

import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_CONTENT_TYPE;
import static nz.co.fortytwo.signalk.artemis.util.Config.INCOMING_RAW;
import static nz.co.fortytwo.signalk.artemis.util.Config.JSON_DELTA;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.CONFIG;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.CONTEXT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.PUT;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.UPDATES;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.source;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.sourceRef;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ICoreMessage;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.core.message.impl.CoreMessage;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.SessionSendMessage;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class DeltaContextInterceptorTest extends BaseMsgInterceptorTest {

	private static Logger logger = LogManager.getLogger(DeltaContextInterceptorTest.class);
	private Json put = Json.read("{\"put\":[{\"source\":{\"label\":\"actisense\",\"type\":\"NMEA2000\",\"src\":\"115\",\"pgn\":128267},\"timestamp\":\"2010-01-07T07:18:44.000Z\",\"values\":[{\"path\":\"propulsion.0.boostPressure\",\"value\":45500.0}]}]}");
	private Json update = Json.read("{\"updates\":[{\"source\":{\"label\":\"actisense\",\"type\":\"NMEA2000\",\"src\":\"115\",\"pgn\":128267},\"timestamp\":\"2010-01-07T07:18:44.000Z\",\"values\":[{\"path\":\"propulsion.0.boostPressure\",\"value\":45500.0}]}]}");
	private Json config = Json.read("{\"config\":[{\"timestamp\":\"2010-01-07T07:18:44.000Z\",\"values\":[{\"path\":\"server.demo.start\",\"value\":true}]}]}");
	
	
	private Json put1 = Json.read("{\"context\":\"vessels.urn:mrn:imo:mmsi:234567890\",\"put\":[{\"source\":{\"label\":\"actisense\",\"type\":\"NMEA2000\",\"src\":\"115\",\"pgn\":128267},\"timestamp\":\"2010-01-07T07:18:44.000Z\",\"values\":[{\"path\":\"propulsion.0.boostPressure\",\"value\":45500.0}]}]}");
	private Json update1 = Json.read("{\"context\":\"vessels.urn:mrn:imo:mmsi:234567890\",\"updates\":[{\"source\":{\"label\":\"actisense\",\"type\":\"NMEA2000\",\"src\":\"115\",\"pgn\":128267},\"timestamp\":\"2010-01-07T07:18:44.000Z\",\"values\":[{\"path\":\"propulsion.0.boostPressure\",\"value\":45500.0}]}]}");
	private Json config1 = Json.read("{\"context\":\"vessels.urn:mrn:imo:mmsi:234567890\",\"config\":[{\"timestamp\":\"2010-01-07T07:18:44.000Z\",\"values\":[{\"path\":\"server.demo.start\",\"value\":true}]}]}");
	
	
    private DeltaContextInterceptor interceptor;// 1

   
    public DeltaContextInterceptorTest() {
		interceptor = new DeltaContextInterceptor();
	}
    

	@Test
	public void shouldProcessUpdate() throws ActiveMQException {
		
		ClientMessage message = getClientMessage(update.toString(), JSON_DELTA, false);
		Json after = process(message);
		assertTrue(Util.isDelta(after));
		assertTrue(after.has(CONTEXT));
		
	}
	
	@Test
	public void shouldProcessUpdate1() throws ActiveMQException {
		
		ClientMessage message = getClientMessage(update1.toString(), JSON_DELTA, false);
		Json after = process(message);
		assertTrue(Util.isDelta(after));
		assertTrue(after.has(CONTEXT));
		
	}
	


	@Test
	public void shouldProcessPut() throws ActiveMQException {
		
		ClientMessage message = getClientMessage(put.toString(), JSON_DELTA, false);
		Json after = process(message);
		assertTrue(Util.isDelta(after));
		assertTrue(after.has(CONTEXT));
		
	}
	
	@Test
	public void shouldProcessPut1() throws ActiveMQException {
		
		ClientMessage message = getClientMessage(put1.toString(), JSON_DELTA, false);
		Json after = process(message);
		assertTrue(Util.isDelta(after));
		assertTrue(after.has(CONTEXT));
		
	}
	
	
	@Test
	public void shouldProcessConfig() throws ActiveMQException {
		
		ClientMessage message = getClientMessage(config.toString(), JSON_DELTA, false);
		Json after = process(message);
		assertTrue(Util.isDelta(after));
		assertFalse(after.has(CONTEXT));
	}
	
	@Test
	public void shouldProcessConfig1() throws ActiveMQException {
		
		ClientMessage message = getClientMessage(config1.toString(), JSON_DELTA, false);
		Json after = process(message);
		assertTrue(Util.isDelta(after));
		assertFalse(after.has(CONTEXT));
	}
	
	private Json process(ClientMessage message) throws ActiveMQException {
		message.setAddress(INCOMING_RAW);
		SessionSendMessage packet = new SessionSendMessage((CoreMessage) message);

		assertTrue(interceptor.intercept(packet, null));
		ICoreMessage msg = packet.getMessage();
		assertEquals(JSON_DELTA, msg.getStringProperty(AMQ_CONTENT_TYPE));
		String content = Util.readBodyBufferToString(msg);
		logger.debug("converted message: {}", content);
		return Json.read(content);
	}
}
