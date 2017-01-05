package nz.co.fortytwo.signalk.artemis.intercept;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Interceptor;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.protocol.core.Packet;
import org.apache.activemq.artemis.core.protocol.core.impl.wireformat.SessionSendMessage;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;

import nz.co.fortytwo.signalk.artemis.server.ArtemisServer;
 
/**
 * A way to acquire the session in a message
 */
public class SessionInterceptor implements Interceptor {
 
   @Override
   public boolean intercept(final Packet packet, final RemotingConnection connection) throws ActiveMQException {
//      System.out.println("SessionInterceptor gets called!");
//      System.out.println("Packet: " + packet.getClass().getName());
//      System.out.println("Packet: " + packet.toString());
//      System.out.println("RemotingConnection: " + connection.getRemoteAddress());
//      System.out.println("TransportConnection: " + connection.getTransportConnection().toString());
//      System.out.println("Sessions count:"+ArtemisServer.embedded.getActiveMQServer().getSessions().size());
      
      if (packet instanceof SessionSendMessage) {
          SessionSendMessage realPacket = (SessionSendMessage) packet;
          //System.out.println("SessionSendMessage: " + realPacket.toString());
          Message msg = realPacket.getMessage();
          //ServerSession sess = ArtemisServer.embedded.getActiveMQServer().getSessions().get()
    	  for(ServerSession s:ArtemisServer.embedded.getActiveMQServer().getSessions(connection.getID().toString())){
    		  if(s.getConnectionID().equals(connection.getID())){
    			  //System.out.println("Session is:"+s.getConnectionID()+", name:"+s.getName());
    			  msg.putStringProperty("AMQ_session_id", s.getName());
    		  }else{
    			  //System.out.println("Session not found for:"+s.getConnectionID()+", name:"+s.getName());
    		  }
    	  }
         
      }
      // We return true which means "call next interceptor" (if there is one) or target.
      // If we returned false, it means "abort call" - no more interceptors would be called and neither would
      // the target
      return true;
   }
 
}