package nz.co.fortytwo.signalk.artemis.intercept;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ICoreMessage;
import org.apache.activemq.artemis.api.core.Interceptor;
import org.apache.activemq.artemis.core.protocol.core.Packet;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.SessionReceiveMessage;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nz.co.fortytwo.signalk.artemis.util.Util;

/**
 * Convert a map of messages to delta format
 * 
 * @author robert
 *
 */
public class MapToDeltaInterceptor implements Interceptor {

	private static Logger logger = LogManager.getLogger(MapToDeltaInterceptor.class);

	@Override
	public boolean intercept(Packet packet, RemotingConnection connection) throws ActiveMQException {
		if(!packet.isResponse())return true;
		//we only want outgoing packets
		if (logger.isDebugEnabled())
			logger.debug("Outgoing Msg is:" + packet.getClass());
		//NullResponseMessage!
		if (packet instanceof SessionReceiveMessage) {
			SessionReceiveMessage realPacket = (SessionReceiveMessage) packet;

			ICoreMessage msg = realPacket.getMessage();

			//String msgType = getContentType(msg);
			if (logger.isDebugEnabled())
				logger.debug("Msg body is:" + Util.readBodyBufferToString(msg));
//
//			if (msgType != null) {
//				msg.putStringProperty(Config.AMQ_CONTENT_TYPE, msgType);
//			} else {
//				// msg.getBodyBuffer().writeString("{}");
//			}
//
//			return true;
		}
		return true;
	}

	

}
