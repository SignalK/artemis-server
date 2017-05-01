package nz.co.fortytwo.signalk.artemis.intercept;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Interceptor;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.core.protocol.core.Packet;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.SessionSendMessage;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nz.co.fortytwo.signalk.artemis.server.ArtemisServer;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;

/**
 * A way to acquire the session in a message
 */
public class SessionInterceptor implements Interceptor {

	private static Logger logger = LogManager.getLogger(SessionInterceptor.class);

	@Override
	public boolean intercept(final Packet packet, final RemotingConnection connection) throws ActiveMQException {
		// System.out.println("SessionInterceptor gets called!");
		 System.out.println("Packet: " + packet.getClass().getName());
		// System.out.println("Packet: " + packet.toString());
		// System.out.println("RemotingConnection: " +
		// connection.getRemoteAddress());
		// System.out.println("TransportConnection: " +
		// connection.getTransportConnection().toString());
		// System.out.println("Sessions
		// count:"+ArtemisServer.embedded.getActiveMQServer().getSessions().size());
		if(packet.isResponse())return true;
		if (packet instanceof SessionSendMessage) {
			SessionSendMessage realPacket = (SessionSendMessage) packet;
	
			Message msg = realPacket.getMessage();
			if(msg.getStringProperty(Config.MSG_SRC_BUS)==null)
				msg.putStringProperty(Config.MSG_SRC_BUS, connection.getRemoteAddress());
			
			for (ServerSession s : ArtemisServer.getActiveMQServer().getSessions(connection.getID().toString())) {
				if (s.getConnectionID().equals(connection.getID())) {
					if (logger.isDebugEnabled())
						logger.debug("Session is:" + s.getConnectionID() + ", name:" + s.getName());
					msg.putStringProperty(Config.AMQ_SESSION_ID, s.getName());
				} else {
					if (logger.isDebugEnabled())
						logger.debug("Session not found for:" + s.getConnectionID() + ", name:" + s.getName());
				}
			}

		} else {
			if (logger.isDebugEnabled())
				logger.debug("Packet is:" + packet.getClass() + ", contents:" + packet.toString());
		}
		// We return true which means "call next interceptor" (if there is one)
		// or target.
		// If we returned false, it means "abort call" - no more interceptors
		// would be called and neither would the target
		return true;
	}

}
