package nz.co.fortytwo.signalk.artemis.server;

import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.FORMAT_DELTA;
import static nz.co.fortytwo.signalk.artemis.util.SignalKConstants.POLICY_FIXED;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;
import nz.co.fortytwo.signalk.artemis.util.SignalKConstants;

public class TcpSubscribeTest extends BaseServerTest{
	
	private static Logger logger = LogManager.getLogger(TcpSubscribeTest.class);

	@Before
	public void startServer() throws Exception {
		//remove self file so we have clean model
		FileUtils.writeStringToFile(new File(Util.SIGNALK_MODEL_SAVE_FILE), "{}", StandardCharsets.UTF_8);
		server = new ArtemisServer();
	
		ClientSession session = Util.getVmSession("admin", "admin");
		session.start();
	
		ClientProducer producer = session.createProducer();
	
		for (String line : FileUtils.readLines(new File("./src/test/resources/samples/signalkKeesLog.txt"))) {
			line=line.replaceAll("urn:mrn:imo:mmsi:123456789","self");
			ClientMessage message = session.createMessage(true);
			message.getBodyBuffer().writeString(line);
			producer.send(Config.INCOMING_RAW, message);
			if (logger.isDebugEnabled())
				logger.debug("Sent:" + message.getMessageID() + ":" + line);
		}
	}


	@Test
	public void checkSelfSubscribe() throws Exception {

		Socket clientSocket = new Socket("127.0.0.1", 55555);

		DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

		String recv = inFromServer.readLine();
		logger.debug("rcvd message = " + recv);
		
		//String sentence = inFromUser.readLine();

		//outToServer.writeBytes(sentence + '\n');

		//String modifiedSentence = inFromServer.readLine();

		//System.out.println("FROM SERVER: " + modifiedSentence);
		try {

			Json msg = getSubscriptionJson("vessels." + SignalKConstants.self, "navigation", 1000, 0, FORMAT_DELTA, POLICY_FIXED);
			outToServer.writeBytes(msg.toString() + '\n');
			String line = "$GPRMC,144629.20,A,5156.91111,N,00434.80385,E,0.295,,011113,,,A*78";
			outToServer.writeBytes(line + '\n');
			outToServer.flush();
			CountDownLatch latch = new CountDownLatch(1);
			latch.await(3, TimeUnit.SECONDS);
			int c=0;
			while(c<5){
				recv = inFromServer.readLine();
				logger.debug("rcvd sub message = " + recv);
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

		Socket clientSocket1 = new Socket("127.0.0.1", 55555);
		DataOutputStream outToServer1 = new DataOutputStream(clientSocket1.getOutputStream());
		BufferedReader inFromServer1 = new BufferedReader(new InputStreamReader(clientSocket1.getInputStream()));
		
		Socket clientSocket2 = new Socket("127.0.0.1", 55555);
		DataOutputStream outToServer2 = new DataOutputStream(clientSocket2.getOutputStream());
		BufferedReader inFromServer2 = new BufferedReader(new InputStreamReader(clientSocket2.getInputStream()));

		String recv1 = inFromServer1.readLine();
		logger.debug("rcvd1 message = " + recv1);
		
		String recv2 = inFromServer2.readLine();
		logger.debug("rcvd2 message = " + recv2);
		
		try {
			//assumes these exist!
			Json msg1 = getSubscriptionJson("vessels." + SignalKConstants.self, "navigation.position", 1000, 0, FORMAT_DELTA, POLICY_FIXED);
			outToServer1.writeBytes(msg1.toString() + '\n');
			
			Json msg2 = getSubscriptionJson("vessels." + SignalKConstants.self, "navigation.headingMagnetic", 1000, 0, FORMAT_DELTA, POLICY_FIXED);
			outToServer2.writeBytes(msg2.toString() + '\n');
			
			String line = "$GPRMC,144629.20,A,5156.91111,N,00434.80385,E,0.295,,011113,,,A*78";
			outToServer1.writeBytes(line + '\n');
			outToServer1.flush();
			
			CountDownLatch latch = new CountDownLatch(1);
			latch.await(3, TimeUnit.SECONDS);
			int c=0;
			while(c<5){
				recv1 = inFromServer1.readLine();
				recv2 = inFromServer2.readLine();
				logger.debug("rcvd1 sub message = " + recv1);
				logger.debug("rcvd2 sub message = " + recv2);
				c++;
			}
			// assertEquals("Hello", recv);
			assertNotNull(recv1);
			assertNotNull(recv2);
			assertNotEquals(recv1, recv2);
		
		} finally {
			clientSocket1.close();
			clientSocket2.close();
		}
	}



	

}
