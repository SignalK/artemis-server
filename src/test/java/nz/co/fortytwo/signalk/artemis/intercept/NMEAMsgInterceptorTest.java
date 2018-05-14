package nz.co.fortytwo.signalk.artemis.intercept;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.script.ScriptException;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ICoreMessage;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.core.message.impl.CoreMessage;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.SessionSendMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import mjson.Json;
import static nz.co.fortytwo.signalk.artemis.util.Config.*;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.*;
import nz.co.fortytwo.signalk.artemis.util.Util;

public class NMEAMsgInterceptorTest extends BaseMsgInterceptorTest {
	private static Logger logger = LogManager.getLogger(NMEAMsgInterceptorTest.class);
	private String RMC = "$GPRMC,144629.20,A,5156.91111,N,00434.80385,E,0.295,,011113,,,A*78";
	
	
    private NMEAMsgInterceptor interceptor ;// 1

    @Before
    public void before(){
    	try {
			interceptor  = new NMEAMsgInterceptor();
		} catch (FileNotFoundException | NoSuchMethodException | ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	@Test
	public void shouldProcessRMC() throws ActiveMQException {
		
		ClientMessage message = getClientMessage(RMC, _0183, false); 
		SessionSendMessage packet = new SessionSendMessage((CoreMessage) message);

		assertTrue(interceptor.intercept(packet, null));
		
		HashMap<String,Object> map=new HashMap<>();
		map.put(nav_speedOverGround,0.151761149557269d);
		map.put(nav_courseOverGroundTrue,0d);
		checkConversion(packet,map);
		
	}
	private void checkConversion(SessionSendMessage packet, HashMap<String, Object> map) {
		ICoreMessage msg = packet.getMessage();
		assertEquals(JSON_DELTA,msg.getStringProperty(AMQ_CONTENT_TYPE));
//		{"context":"vessels.self","updates":[{"values":[{"path":"navigation.position","value":{"latitude":51.9485185,"longitude":4.580064166666666}},{"path":"navigation.courseOverGroundTrue","value":0},{"path":"navigation.speedOverGround","value":0.151761149557269},{"path":"navigation.magneticVariation","value":0},{"path":"navigation.magneticVariationAgeOfService","value":1383317189},{"path":"navigation.datetime","value":"2013-11-01T14:46:29.000Z"}],"source":{"sentence":"RMC","talker":"GP","type":"NMEA0183"},"timestamp":"2013-11-01T14:46:29.000Z"}]}"
		String content = Util.readBodyBufferToString(msg);;
		logger.debug("NMEA converted message: {}",content);
		Json json = Json.read(content);
		
		assertTrue(Util.isDelta(json));
		
		assertEquals(vessels+dot+self_str, json.at(CONTEXT).asString());
		
		assertTrue(json.has(UPDATES));
		
		List<Json> list = json.at(UPDATES).asJsonList().get(0).at(values).asJsonList();
		for(Json v:list){
			for(Entry<String, Object> e:map.entrySet()){
				if(e.getKey().equals(v.at(PATH))){
					if(e.getValue() instanceof Double){
						assertEquals((Double)e.getValue(),v.at(value).asDouble(),0.0001);
					}
					if(e.getValue() instanceof String){
						assertEquals((String)e.getValue(),v.at(value).asString());
					}
				}
			
			}

		}
		
	}
}
