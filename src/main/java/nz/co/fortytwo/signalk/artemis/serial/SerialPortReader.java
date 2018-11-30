/*
 *
 * Copyright (C) 2012-2014 R T Huitema. All Rights Reserved.
 * Web: www.42.co.nz
 * Email: robert@42.co.nz
 * Author: R T Huitema
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
 * WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package nz.co.fortytwo.signalk.artemis.serial;

import static nz.co.fortytwo.signalk.artemis.util.Config.AMQ_USER_TOKEN;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortPacketListener;

import nz.co.fortytwo.signalk.artemis.handler.MessageSupport;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.SecurityUtils;

/**
 * Wrapper to read serial port via jSerialComm, then fire messages into the artemis
 * queue
 * 
 * @author robert
 * 
 */
public class SerialPortReader extends MessageSupport{

	private static Logger logger = LogManager.getLogger(SerialPortReader.class);
	private String portName;
	private File portFile;

	private ClientConsumer consumer;
	private boolean running = true;
	//private boolean mapped = false;
	private String deviceType = null;
	private SerialPort serialPort = null;

	private LinkedBlockingQueue<String> queue;
	private SerialReader serialReader;
	

	public SerialPortReader() throws Exception {
		super();
		queue = new LinkedBlockingQueue<String>(100);
		consumer = getTxSession().createConsumer(Config.INCOMING_RAW);
	}

	/**
	 * Opens a connection to the serial port, and starts two threads, one to
	 * read, one to write. A background thread looks for new/lost USB devices
	 * and (re)attaches them
	 * 
	 * 
	 * @param portName
	 * @throws Exception
	 */
	void connect(String portName, int baudRate) throws Exception {
		this.portName = portName;
		if (!SystemUtils.IS_OS_WINDOWS) {
			this.portFile = new File(portName);
		}
		// CommPortIdentifier portid = SerialPort.getCommPort(portName);
		SerialPort[] ports = SerialPort.getCommPorts();
		if(logger.isDebugEnabled()){
			for (SerialPort p : ports) {
				logger.debug("Found: {}", p.getSystemPortName());
				logger.debug("     : {}", p.getPortDescription());
			}
		}
		serialPort = SerialPort.getCommPort(portName);
		
		if(logger.isDebugEnabled())logger.debug("Opening {}", serialPort.getPortDescription());
		serialPort.setBaudRate(baudRate);
		serialPort.setNumDataBits(8);
		serialPort.setNumStopBits(1);
		serialPort.setParity(SerialPort.NO_PARITY);
		serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
		// serialPort.setSerialPortParams(baudRate, 8, 1, 0);
		serialReader = new SerialReader();
		// serialPort.enableReceiveTimeout(1000);
		// serialPort.notifyOnDataAvailable(true);
		serialPort.addDataListener(serialReader);
		serialPort.openPort();
		if(logger.isDebugEnabled())logger.debug("Is open : {}", serialPort.isOpen());
		// (new Thread(new SerialWriter())).start();

	}

	// public class SerialWriter implements Runnable {
	//
	// BufferedOutputStream out;
	//
	// public SerialWriter() throws Exception {
	//
	// this.out = new BufferedOutputStream(serialPort.getOutputStream());
	//
	// }
	//
	// public void run() {
	//
	// try {
	// while (running) {
	// String msg = queue.poll(5, TimeUnit.SECONDS);
	// if (StringUtils.isNotBlank(msg)) {
	// out.write((msg + "\r\n").getBytes());
	// out.flush();
	// }
	// }
	// } catch (IOException e) {
	//
	// logger.error(portName + ":" + e.getMessage());
	// logger.debug(e.getMessage(), e);
	// } catch (InterruptedException e) {
	// // do nothing
	// } finally {
	// // clean up
	// running = false;
	// try {
	// out.close();
	// } catch (Exception e1) {
	// logger.error(portName + ":" + e1.getMessage());
	// // logger.debug(e1.getMessage(),e1);
	// }
	// try {
	// session.close();
	// } catch (ActiveMQException e) {
	// logger.error(portName + ":" + e.getMessage());
	// //logger.error(e.getMessage(),e);
	// }
	//
	// }
	// }
	//
	// }

	/** */
	public class SerialReader implements SerialPortPacketListener {

		StringBuilder lineBuf = new StringBuilder();
		private boolean enableSerial = true;
		

		public SerialReader() throws Exception {

			if (logger.isDebugEnabled())
				logger.info("Setup serialReader on :{}" , portName);
			enableSerial = Config.getConfigPropertyBoolean(ConfigConstants.ENABLE_SERIAL);
		}

		@Override
		public int getListeningEvents() {
			return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
		}

		@Override
		public int getPacketSize() {
			return 20;
		}

		@Override
		public void serialEvent(SerialPortEvent event) {

			try {
				if (running && event.getEventType() == SerialPort.LISTENING_EVENT_DATA_RECEIVED) {

					byte[] newData = event.getReceivedData();
					if (logger.isDebugEnabled())
						logger.info("Reading : {}", new String(newData));
					if (newData == null)
						return;
					lineBuf.append(new String(newData));
					int x = lineBuf.indexOf("\r");
					if (logger.isDebugEnabled())
						logger.info(lineBuf.toString());
					while(x>0){
						sendMsg(lineBuf.substring(0,x+1));
						lineBuf.delete(0,x+1);
						x = lineBuf.indexOf("\r");
					}
					//limit overall length to 256K
					if(lineBuf.length()>1024*256){
						sendMsg(lineBuf.toString());
						lineBuf=new StringBuilder();
					}
				}

			} catch (Exception e) {
				if (logger.isDebugEnabled())
					logger.debug(e, e);
				running = false;
				stopReader();
				logger.error(portName + ":" + e.getMessage());

			}

		}

		private void sendMsg(String buffer) throws Exception {
			if (StringUtils.isNotBlank(buffer)) {
				if (logger.isDebugEnabled())
					logger.debug("{} :Serial Received:{}",portName, buffer);
				// its not empty!
				if (buffer.length() > 0) {
					// send it
					if (enableSerial && getTxSession() != null && !getTxSession().isClosed()) {
						ClientMessage txMsg = getTxSession().createMessage(true);
						txMsg.getBodyBuffer().writeString(buffer);
						txMsg.putStringProperty(Config.MSG_SRC_BUS, portName);
						txMsg.putStringProperty(Config.MSG_SRC_TYPE, Config.SERIAL);
						txMsg.putStringProperty(AMQ_USER_TOKEN, SerialPortManager.getToken());
						
						txMsg.putStringProperty(Config.AMQ_USER_ROLES, SecurityUtils.getRoles(SerialPortManager.getToken()).toString());
						
						getProducer().send(new SimpleString(Config.INCOMING_RAW), txMsg);
						if (logger.isDebugEnabled())
							logger.debug("json = {}", buffer);

					} else {
						if (logger.isDebugEnabled())
							logger.debug("enableSerial false: {}", buffer);
					}
				}
				
			}
			
		}

		protected void stopReader() {
			try {
				serialPort.removeDataListener();
				serialPort.closePort();
				serialPort=null;
			} catch (Exception e1) {
				logger.error("{}:{}" ,portName , e1.getMessage());
				if (logger.isDebugEnabled())
					logger.debug(e1);
			}

			stopSession();
		}

	}


	/**
	 * True if the serial port read/write threads are running
	 * 
	 * @return
	 */
	public boolean isRunning() {
		if(!portFile.exists())return false;
		if (serialPort != null) {
			return serialPort.isOpen();
		}
		return running;
	}

	/**
	 * Set to false to stop the serial port read/write threads. You must
	 * connect() to restart.
	 * 
	 * @param running
	 */
	public void setRunning(boolean running) {
		this.running = running;
		if (!running) {
			serialReader.stopReader();
		}

	}

	public String getPortName() {
		return portName;
	}

	public void setPortName(String portName) {
		this.portName = portName;
	}

	/*
	 * Handles the messages to be delivered to the device attached to this port.
	 * 
	 * @see org.apache.camel.Processor#process(org.apache.camel.Exchange)
	 */
	public void process(String message) throws Exception {
		// send to device
		//TODO: rework sending to serial
		if (logger.isDebugEnabled())logger.debug( "{}:msg received for device:{}" ,portName,message);
		if (StringUtils.isNotBlank(message)) {
			// check its valid for this device
			if (running && deviceType == null || message.contains(ConfigConstants.UID + ":" + deviceType)) {
				if (logger.isDebugEnabled())
					logger.debug("{}:wrote out to device:{}",portName,message);
				// queue them and write in background
				if (!queue.offer(message)) {
					if (logger.isDebugEnabled())
						logger.debug("Output queue id full for {}", portName);
				}
			}
		}
	}
	

	@Override
	protected void finalize() throws Throwable {
		
		stopSession();
		super.finalize();
	}

	private void stopSession() {
		
		if (consumer != null) {
			try {
				consumer.close();
			} catch (ActiveMQException e) {
				logger.error(e, e);
			}
		}
		super.stop();
		
	}

}
