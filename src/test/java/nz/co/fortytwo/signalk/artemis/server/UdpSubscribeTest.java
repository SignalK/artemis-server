package nz.co.fortytwo.signalk.artemis.server;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.FORMAT_DELTA;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.POLICY_FIXED;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;

public class UdpSubscribeTest extends BaseServerTest{
	
	private static Logger logger = LogManager.getLogger(UdpSubscribeTest.class);
	private int port = Config.getConfigPropertyInt(ConfigConstants.UDP_PORT);
	
	@Test
	public void checkSelfSubscribe() throws Exception {

		DatagramSocket clientSocket = new DatagramSocket();
		clientSocket.setSoTimeout(5000);
		InetAddress host = InetAddress.getByName("localhost");
		byte[] b = "Hello".getBytes();
		 DatagramPacket  dp = new DatagramPacket(b , b.length , host , port);
		 clientSocket.send(dp);
		 
		 byte[] buffer = new byte[65536];
         DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
         clientSocket.receive(reply);
		 String recv = new String(reply.getData());
		logger.debug("rcvd message = " + recv);
		
		try {

			byte[] msg = getSubscriptionJson("vessels." + SignalKConstants.self, "navigation", 1000, 0, FORMAT_DELTA, POLICY_FIXED).toString().getBytes();
			DatagramPacket  dp1 = new DatagramPacket(msg , msg.length , host , port);
			clientSocket.send(dp1);
			 
			byte[] line = "$GPRMC,144629.20,A,5156.91111,N,00434.80385,E,0.295,,011113,,,A*78".getBytes();
			DatagramPacket  dp2 = new DatagramPacket(line , line.length , host , port);
			clientSocket.send(dp2);
			
			CountDownLatch latch = new CountDownLatch(2);
			latch.await(1, TimeUnit.SECONDS);
			int c=0;
			while(c<5){
				reply = new DatagramPacket(buffer, buffer.length);
				 clientSocket.receive(reply);
				 recv = new String(reply.getData());
				logger.debug("rcvd sub message = " + recv);
				latch.countDown();
				c++;
			}
			// assertEquals("Hello", recv);
			assertNotNull(recv);
		
		} finally {
			clientSocket.close();
		}
	}
	
	@Test
	public void checkMultiClientSubscribe() throws Exception {

		DatagramSocket clientSocket1 = new DatagramSocket();
		DatagramSocket clientSocket2 = new DatagramSocket();

		InetAddress host = InetAddress.getByName("localhost");
		 byte[] b = "Hello".getBytes();
		 
		 DatagramPacket  dp = new DatagramPacket(b , b.length , host , port);
		 clientSocket1.send(dp);
		 clientSocket2.send(dp);
		 
		 byte[] buffer1 = new byte[65536];
         DatagramPacket reply1 = new DatagramPacket(buffer1, buffer1.length);
         clientSocket1.receive(reply1);
		 String recv1 = new String(reply1.getData());
		logger.debug("rcvd message = " + recv1);
		
		 byte[] buffer2 = new byte[65536];
		DatagramPacket reply2 = new DatagramPacket(buffer2, buffer2.length);
		clientSocket2.receive(reply2);
		 String recv2 = new String(reply2.getData());
		logger.debug("rcvd message = " + recv2);
		
		try {

			byte[] msg1 = getSubscriptionJson("vessels." + SignalKConstants.self, "navigation.position", 1000, 0, FORMAT_DELTA, POLICY_FIXED).toString().getBytes();
			DatagramPacket  dp1 = new DatagramPacket(msg1 , msg1.length , host , port);
			clientSocket1.send(dp1);
			
			byte[] msg2 = getSubscriptionJson("vessels." + SignalKConstants.self, "navigation.headingMagnetic", 1000, 0, FORMAT_DELTA, POLICY_FIXED).toString().getBytes();
			DatagramPacket  dp2 = new DatagramPacket(msg2 , msg2.length , host , port);
			clientSocket2.send(dp2);
			 
			byte[] line = "$GPRMC,144629.20,A,5156.91111,N,00434.80385,E,0.295,,011113,,,A*78".getBytes();
			DatagramPacket  dp3 = new DatagramPacket(line , line.length , host , port);
			clientSocket1.send(dp3);
			
			CountDownLatch latch = new CountDownLatch(1);
			latch.await(1, TimeUnit.SECONDS);
			int c=0;
			while(c<5){
				reply1 = new DatagramPacket(buffer1, buffer1.length);
				 clientSocket1.receive(reply1);
				 recv1 = new String(reply1.getData());
				logger.debug("rcvd1 sub message = " + recv1);
				
				reply2 = new DatagramPacket(buffer2, buffer2.length);
				 clientSocket2.receive(reply2);
				 recv2 = new String(reply2.getData());
				logger.debug("rcvd2 sub message = " + recv2);
				c++;
			}
			// assertEquals("Hello", recv);
			assertNotNull(recv1);
			assertNotNull(recv2);
			assertNotEquals(recv1,recv2);
		} finally {
			clientSocket1.close();
			clientSocket2.close();
		}
	}



}
