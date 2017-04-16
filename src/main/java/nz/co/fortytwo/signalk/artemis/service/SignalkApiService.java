package nz.co.fortytwo.signalk.artemis.service;

import static nz.co.fortytwo.signalk.util.SignalKConstants.CONFIG;
import static nz.co.fortytwo.signalk.util.SignalKConstants.SIGNALK_API;
import static nz.co.fortytwo.signalk.util.SignalKConstants.dot;
import static nz.co.fortytwo.signalk.util.SignalKConstants.source;
import static nz.co.fortytwo.signalk.util.SignalKConstants.timestamp;
import static nz.co.fortytwo.signalk.util.SignalKConstants.value;
import static nz.co.fortytwo.signalk.util.SignalKConstants.vessels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.protocol.HTTP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.cpr.AtmosphereResource;

import ch.qos.logback.core.status.Status;
import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Util;
import nz.co.fortytwo.signalk.util.JsonSerializer;

public abstract class SignalkApiService {

	private static Logger logger = LogManager.getLogger(SignalkApiService.class);

	public SignalkApiService() {
	}

	public void get(AtmosphereResource resource, String path) {
		if(logger.isDebugEnabled())logger.debug("get:"+path+" for "+resource.getRequest().getRemoteUser());
	
		path=path.substring(SIGNALK_API.length()+1);
		if(path.equals("self"))path="vessels/self/uuid";
		path=path.replace('/', '.');
		
		path=Util.fixSelfKey(path);
		if(logger.isDebugEnabled())logger.debug("get:"+path);
		String queue = path;
		if(queue.contains("."))
			queue=queue.substring(0,queue.indexOf("."));
		//TODO: no security yet!
		String user = "admin";
		try {
			ClientSession rxSession = null;
			ClientConsumer consumer = null;
			try {
				//start polling consumer.
				rxSession = Util.getVmSession(user, user);
				consumer = rxSession.createConsumer(queue,
						"_AMQ_LVQ_NAME = '"+path+"' or _AMQ_LVQ_NAME like '"+path+".%'", true);
				
				ClientMessage msgReceived = null;
				SortedMap< String, Object> msgs = new ConcurrentSkipListMap<>();
				while ((msgReceived = consumer.receive(10)) != null) {
					String key = msgReceived.getAddress().toString();
					if(logger.isDebugEnabled())logger.debug("message = "  + msgReceived.getMessageID()+":" + key );
					String ts =msgReceived.getStringProperty(timestamp);
					String src =msgReceived.getStringProperty(source);
					if(ts!=null)
						msgs.put(key+dot+timestamp,ts);
					if(src!=null)
						msgs.put(key+dot+source,src);
					if(ts==null&& src==null){
						msgs.put(key, Util.readBodyBuffer(msgReceived));
					}else{
						msgs.put(key+dot+value, Util.readBodyBuffer(msgReceived));
					}
					
				}
				resource.getResponse().setContentType("application/json");
				if(msgs.size()>0){
					JsonSerializer ser = new  JsonSerializer();
					Json json = Json.read(ser.write(msgs));
					if(!path.startsWith(CONFIG))
						json = Util.findNode(json, path);
					if(logger.isDebugEnabled())logger.debug("json = "+json.toString());
					resource.getResponse().write(json.toString());
				}else{
					resource.getResponse().write("{}");
				}
				
				consumer.close();
				
			} catch (Exception e) {
				logger.error(e.getMessage(),e);
			
			}finally {
				if(consumer!=null){
					try {
						consumer.close();
					} catch (ActiveMQException e) {
						logger.error(e);
					}
				}
				if(rxSession!=null){
					try {
						rxSession.close();
					} catch (ActiveMQException e) {
						logger.error(e);
					}
				}
			}
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	

	
}
