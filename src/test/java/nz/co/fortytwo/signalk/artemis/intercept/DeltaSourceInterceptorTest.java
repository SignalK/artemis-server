package nz.co.fortytwo.signalk.artemis.intercept;

import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_CONTENT_TYPE;
import static nz.co.fortytwo.signalk.artemis.util.Config.INCOMING_RAW;
import static nz.co.fortytwo.signalk.artemis.util.Config.JSON_DELTA;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.CONFIG;
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

public class DeltaSourceInterceptorTest extends BaseMsgInterceptorTest {

	private static Logger logger = LogManager.getLogger(DeltaSourceInterceptorTest.class);
	private Json update ;
	private Json put = Json.read("{\"context\":\"vessels.urn:mrn:imo:mmsi:234567890\",\"put\":[{\"source\":{\"label\":\"actisense\",\"type\":\"NMEA2000\",\"src\":\"115\",\"pgn\":128267},\"timestamp\":\"2010-01-07T07:18:44.000Z\",\"values\":[{\"path\":\"propulsion.0.boostPressure\",\"value\":45500.0}]}]}");
	private Json config = Json.read("{\"context\":\"vessels.urn:mrn:imo:mmsi:234567890\",\"config\":[{\"source\":{\"label\":\"actisense\",\"type\":\"NMEA2000\",\"src\":\"115\",\"pgn\":128267},\"timestamp\":\"2010-01-07T07:18:44.000Z\",\"values\":[{\"path\":\"propulsion.0.boostPressure\",\"value\":45500.0}]}]}");
	
	@Rule
    public EasyMockRule rule = new EasyMockRule(this);

    @Mock
    private DeltaSourceInterceptor interceptor;// 1

   
    public DeltaSourceInterceptorTest() {
		interceptor = new DeltaSourceInterceptor();
	}
    
    @Before
    public void before(){
    	interceptor = partialMockBuilder(DeltaSourceInterceptor.class)
	    	.addMockedMethod("sendKvMessage")
	    	.createMock(); 
		try {
			update=Json.read(FileUtils.readFileToString(new File("./src/test/resources/samples/delta/docs-data_model_multiple_values.json")));
		} catch (IOException e) {
			logger.debug(e,e);
		}
		
	}

	@Test
	public void shouldProcessUpdate() throws ActiveMQException {
		//{sources.NMEA0183.GPS-1.label="GPS-1", sources.NMEA0183.GPS-1.label._attr={"owner":"admin","grp":"admin"}, sources.NMEA0183.GPS-1.sentence="RMC", sources.NMEA0183.GPS-1.sentence._attr={"owner":"admin","grp":"admin"}, sources.NMEA0183.GPS-1.talker="GP", sources.NMEA0183.GPS-1.talker._attr={"owner":"admin","grp":"admin"}, sources.NMEA0183.GPS-1.type="NMEA0183", sources.NMEA0183.GPS-1.type._attr={"owner":"admin","grp":"admin"}}
//		interceptor.saveSource(Json.read("{\"sources\":{\"NMEA0183\":{\"GPS-1\":{\"sentence\":\"RMC\",\"talker\":\"GP\",\"label\":\"GPS-1\",\"type\":\"NMEA0183\"}}}}"));
//		interceptor.saveSource(Json.read("{\"sources\":{\"NMEA2000\":{\"actisense\":{\"src\":\"115\",\"pgn\":128267,\"label\":\"actisense\",\"type\":\"NMEA2000\"}}}}"));
		
		ClientMessage message = getClientMessage(update.toString(), JSON_DELTA, false);
		interceptor.sendKvMessage( anyObject(message.getClass()), anyString(), anyObject(Json.class));
		expectLastCall().times(8);
		replayAll();
		
		message.setAddress(INCOMING_RAW);
		SessionSendMessage packet = new SessionSendMessage((CoreMessage) message);

		assertTrue(interceptor.intercept(packet, null));
		ICoreMessage msg = packet.getMessage();
		assertEquals(JSON_DELTA, msg.getStringProperty(AMQ_CONTENT_TYPE));
		// {"context":"vessels.self","updates":[{"values":[{"path":"navigation.position","value":{"latitude":51.9485185,"longitude":4.580064166666666}},{"path":"navigation.courseOverGroundTrue","value":0},{"path":"navigation.speedOverGround","value":0.151761149557269},{"path":"navigation.magneticVariation","value":0},{"path":"navigation.magneticVariationAgeOfService","value":1383317189},{"path":"navigation.datetime","value":"2013-11-01T14:46:29.000Z"}],"source":{"sentence":"RMC","talker":"GP","type":"NMEA0183"},"timestamp":"2013-11-01T14:46:29.000Z"}]}"
		String content = Util.readBodyBufferToString(msg);
		
		verifyAll();
		//{"context":"vessels.urn:mrn:signalk:uuid:c0d79334-4e25-4245-8892-54e8ccc8021d","updates":[{"values":[{"path":"navigation.courseOverGroundTrue","value":3.61562407843144}],"source":{"sentence":"RMC","talker":"GP","label":"GPS-1","type":"NMEA0183"},"timestamp":"2017-04-03T06:14:04.451Z"},{"values":[{"path":"navigation.courseOverGroundTrue","value":3.615624078431453}],"source":{"src":"115","pgn":128267,"label":"actisense","type":"NMEA2000"},"timestamp":"2017-04-03T06:14:04.451Z"}]}
		logger.debug("converted message: {}", content);
		Json after = Json.read(content);
		assertTrue(Util.isDelta(after));
		
		for ( Json j: after.at(UPDATES).asJsonList()){
			assertFalse(j.toString(), j.has(source));
			assertTrue(j.toString(), j.has(sourceRef));
		}	
		
	}
	
	@Test
	public void shouldProcessPut() throws ActiveMQException {
		
//		interceptor.saveSource(Json.read("{\"sources\":{\"NMEA2000\":{\"actisense\":{\"src\":\"115\",\"pgn\":128267,\"label\":\"actisense\",\"type\":\"NMEA2000\"}}}}"));
		
		
		ClientMessage message = getClientMessage(put.toString(), JSON_DELTA, false);
		interceptor.sendKvMessage( anyObject(message.getClass()), anyString(), anyObject(Json.class));
		expectLastCall().times(4);
		replayAll();
		message.setAddress(INCOMING_RAW);
		SessionSendMessage packet = new SessionSendMessage((CoreMessage) message);

		assertTrue(interceptor.intercept(packet, null));
		ICoreMessage msg = packet.getMessage();
		assertEquals(JSON_DELTA, msg.getStringProperty(AMQ_CONTENT_TYPE));
		// {"context":"vessels.self","updates":[{"values":[{"path":"navigation.position","value":{"latitude":51.9485185,"longitude":4.580064166666666}},{"path":"navigation.courseOverGroundTrue","value":0},{"path":"navigation.speedOverGround","value":0.151761149557269},{"path":"navigation.magneticVariation","value":0},{"path":"navigation.magneticVariationAgeOfService","value":1383317189},{"path":"navigation.datetime","value":"2013-11-01T14:46:29.000Z"}],"source":{"sentence":"RMC","talker":"GP","type":"NMEA0183"},"timestamp":"2013-11-01T14:46:29.000Z"}]}"
		String content = Util.readBodyBufferToString(msg);
		
		verifyAll();
		//{"context":"vessels.urn:mrn:signalk:uuid:c0d79334-4e25-4245-8892-54e8ccc8021d","updates":[{"values":[{"path":"navigation.courseOverGroundTrue","value":3.61562407843144}],"source":{"sentence":"RMC","talker":"GP","label":"GPS-1","type":"NMEA0183"},"timestamp":"2017-04-03T06:14:04.451Z"},{"values":[{"path":"navigation.courseOverGroundTrue","value":3.615624078431453}],"source":{"src":"115","pgn":128267,"label":"actisense","type":"NMEA2000"},"timestamp":"2017-04-03T06:14:04.451Z"}]}
		logger.debug("converted message: {}", content);
		Json after = Json.read(content);
		assertTrue(Util.isDelta(after));
		
		for ( Json j: after.at(PUT).asJsonList()){
			assertFalse(j.toString(), j.has(source));
			assertTrue(j.toString(), j.has(sourceRef));
		}	
		
	}
	
	@Test
	public void shouldProcessConfig() throws ActiveMQException {
		
//		interceptor.saveSource(Json.read("{\"sources\":{\"NMEA2000\":{\"actisense\":{\"src\":\"115\",\"pgn\":128267,\"label\":\"actisense\",\"type\":\"NMEA2000\"}}}}"));
		
		ClientMessage message = getClientMessage(config.toString(), JSON_DELTA, false);
		interceptor.sendKvMessage( anyObject(message.getClass()), anyString(), anyObject(Json.class));
		expectLastCall().times(4);
		replayAll();
		
		message.setAddress(INCOMING_RAW);
		SessionSendMessage packet = new SessionSendMessage((CoreMessage) message);

		assertTrue(interceptor.intercept(packet, null));
		ICoreMessage msg = packet.getMessage();
		assertEquals(JSON_DELTA, msg.getStringProperty(AMQ_CONTENT_TYPE));
		// {"context":"vessels.self","updates":[{"values":[{"path":"navigation.position","value":{"latitude":51.9485185,"longitude":4.580064166666666}},{"path":"navigation.courseOverGroundTrue","value":0},{"path":"navigation.speedOverGround","value":0.151761149557269},{"path":"navigation.magneticVariation","value":0},{"path":"navigation.magneticVariationAgeOfService","value":1383317189},{"path":"navigation.datetime","value":"2013-11-01T14:46:29.000Z"}],"source":{"sentence":"RMC","talker":"GP","type":"NMEA0183"},"timestamp":"2013-11-01T14:46:29.000Z"}]}"
		String content = Util.readBodyBufferToString(msg);
		
		verifyAll();
		//{"context":"vessels.urn:mrn:signalk:uuid:c0d79334-4e25-4245-8892-54e8ccc8021d","updates":[{"values":[{"path":"navigation.courseOverGroundTrue","value":3.61562407843144}],"source":{"sentence":"RMC","talker":"GP","label":"GPS-1","type":"NMEA0183"},"timestamp":"2017-04-03T06:14:04.451Z"},{"values":[{"path":"navigation.courseOverGroundTrue","value":3.615624078431453}],"source":{"src":"115","pgn":128267,"label":"actisense","type":"NMEA2000"},"timestamp":"2017-04-03T06:14:04.451Z"}]}
		logger.debug("converted message: {}", content);
		Json after = Json.read(content);
		assertTrue(Util.isDelta(after));
		
		for ( Json j: after.at(CONFIG).asJsonList()){
			assertFalse(j.toString(), j.has(source));
			assertTrue(j.toString(), j.has(sourceRef));
		}	
		
	}
}
