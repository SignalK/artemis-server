package nz.co.fortytwo.signalk.artemis.intercept;

import static nz.co.fortytwo.signalk.artemis.util.Config.INTERNAL_KV;

import java.util.ArrayList;
import java.util.NavigableMap;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQPropertyConversionException;
import org.apache.activemq.artemis.api.core.ICoreMessage;
import org.apache.activemq.artemis.api.core.Interceptor;
import org.apache.activemq.artemis.core.protocol.core.Packet;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.SessionSendMessage;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.SecurityUtils;
import nz.co.fortytwo.signalk.artemis.util.Util;

/**
 * Drop any messages for keys that are not in the messages user write role
 * 
 * @author robert
 *
 */
public class SecurityWriteInterceptor extends BaseInterceptor implements Interceptor {

	private static Logger logger = LogManager.getLogger(SecurityWriteInterceptor.class);

	@Override
	public boolean intercept(Packet packet, RemotingConnection connection) throws ActiveMQException {

		if (packet instanceof SessionSendMessage) {
			SessionSendMessage realPacket = (SessionSendMessage) packet;

			ICoreMessage msg = realPacket.getMessage();

			if (!StringUtils.equals(msg.getAddress(), INTERNAL_KV))
				return true;

			if (StringUtils.isBlank(msg.getStringProperty(Config.AMQ_USER_TOKEN))) {
				logger.error("Message has no {}: {}", Config.AMQ_USER_TOKEN, msg);
				return false;
			}
			
			// check allowed first
			try {
				ArrayList<String> allowed = SecurityUtils.getAllowedWritePaths(msg.getStringProperty(Config.AMQ_USER_TOKEN));
				if(allowed==null||allowed.size()==0) return false;
				for (String key : allowed) {
					if (logger.isDebugEnabled())
						logger.debug("Check allowed key {} = {}", key, msg.getStringProperty(Config.AMQ_INFLUX_KEY));
					if (key.equals("all")) {
						return true;
					}
					if (msg.getStringProperty(Config.AMQ_INFLUX_KEY).contains(key))
						return true;
				}
			} catch (Exception e) {
				logger.error(e, e);
			}
			
			//check denied
			try {
				ArrayList<String> denied = SecurityUtils.getDeniedWritePaths(msg.getStringProperty(Config.AMQ_USER_TOKEN));
				if(denied==null||denied.size()==0) return true;
				for (String key : denied) {
					if (logger.isDebugEnabled())
						logger.debug("Check denied key {} = {}", key, msg.getStringProperty(Config.AMQ_INFLUX_KEY));
					if (key.equals("all")) {
						return false;
					}
					if (msg.getStringProperty(Config.AMQ_INFLUX_KEY).contains(key))
						return false;
				}
			} catch (Exception e) {
				logger.error(e, e);
			}
			return false;
		}
		return true;
	}

}
