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
package nz.co.fortytwo.signalk.artemis.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.Util;
import nz.co.fortytwo.signalk.util.ConfigConstants;
import nz.co.fortytwo.signalk.util.SignalKConstants;

import purejavacomm.CommPortIdentifier;
import purejavacomm.SerialPort;
import purejavacomm.SerialPortEvent;
import purejavacomm.SerialPortEventListener;

/**
 * Wrapper to read serial port via rxtx, then fire messages into the camel route
 * via the seda queue.
 * 
 * @author robert
 * 
 */
public class SerialPortReader implements Processor {

	
	private static Logger logger = LogManager.getLogger(SerialPortReader.class);
	private String portName;
	private File portFile;
	private ClientProducer producer;

	private boolean running = true;
	private boolean mapped = false;
	private String deviceType = null;
	private SerialPort serialPort = null;

	private LinkedBlockingQueue<String> queue;
	private SerialReader serialReader;
	private ClientSession session;

	public SerialPortReader() throws Exception {
		super();
		queue = new LinkedBlockingQueue<String>(100);
		session = Util.getVmSession(Config.getConfigProperty(Config.ADMIN_USER),
				Config.getConfigProperty(Config.ADMIN_PWD));
		producer=session.createProducer();
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
		CommPortIdentifier portid = CommPortIdentifier.getPortIdentifier(portName);
		serialPort = (SerialPort) portid.open("FreeboardSerialReader", 100);
		
		serialPort.setSerialPortParams(baudRate, 8, 1, 0);
		serialReader = new SerialReader();
		serialPort.enableReceiveTimeout(1000);
		serialPort.notifyOnDataAvailable(true);
		serialPort.addEventListener(serialReader);
		// (new Thread(new SerialReader())).start();
		(new Thread(new SerialWriter())).start();

	}

	public class SerialWriter implements Runnable {

		BufferedOutputStream out;

		public SerialWriter() throws Exception {

			this.out = new BufferedOutputStream(serialPort.getOutputStream());

		}

		public void run() {

			try {
				while (running) {
					String msg = queue.poll(5, TimeUnit.SECONDS);
					if (StringUtils.isNotBlank(msg)) {
						out.write((msg + "\r\n").getBytes());
						out.flush();
					}
				}
			} catch (IOException e) {

				logger.error(portName + ":" + e.getMessage());
				logger.debug(e.getMessage(), e);
			} catch (InterruptedException e) {
				// do nothing
			} finally {
				// clean up
				running = false;
				try {
					out.close();
				} catch (Exception e1) {
					logger.error(portName + ":" + e1.getMessage());
					// logger.debug(e1.getMessage(),e1);
				}
				try {
					session.close();
				} catch (ActiveMQException e) {
					logger.error(portName + ":" + e.getMessage());
					//logger.error(e.getMessage(),e);
				}
				
			}
		}

	}

	/** */
	public class SerialReader implements SerialPortEventListener {

		// BufferedReader in;

		private Pattern uid;
		// List<String> lines = new ArrayList<String>();
		String line = null;
		StringBuffer lineBuf = new StringBuffer();
		private boolean enableSerial = true;
		private boolean complete;
		protected InputStream in;
		byte[] buff = new byte[256];
		int x = 0;
		Map<String, Object> headers = new HashMap<String, Object>();

		public SerialReader() throws Exception {

			// this.in = new BufferedReader(new
			// InputStreamReader(serialPort.getInputStream()));
			this.in = new BufferedInputStream(serialPort.getInputStream());
			headers.put(SignalKConstants.MSG_TYPE, SignalKConstants.SERIAL);
			headers.put(SignalKConstants.MSG_SERIAL_PORT, portName);
			headers.put(SignalKConstants.MSG_SRC_BUS, portName);
			uid = Pattern.compile(ConfigConstants.UID + ":");
			if (logger.isDebugEnabled())
				logger.info("Setup serialReader on :" + portName);
			enableSerial = Config.getConfigPropertyBoolean(ConfigConstants.ENABLE_SERIAL);
		}

		// @Override
		public void serialEvent(SerialPortEvent event) {
			// if(logger.isTraceEnabled())logger.trace("SerialEvent:"+event.getEventType());
			try {
				if (running && event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {

					int r = 0;

					while ((r > -1) && in.available() > 0) {
						if (!running)
							break;
						try {
							r = in.read();
							buff[x] = (byte) r;
							x++;

							// 10=LF, 13=CR, lines should end in CR/LF
							if (r == 10 || x == 256) {
								if (r == 10) {
									complete = true;
								}
								lineBuf.append(new String(buff));
								buff = new byte[256];
								x = 0;
							}

						} catch (IOException e) {
							logger.error(portName + ":" + e.getMessage());
							logger.debug(e.getMessage(), e);
							return;
						}
						// we have a line ending in CR/LF
						if (complete && StringUtils.isNotBlank(lineBuf)) {
							line = lineBuf.toString().trim();
							if (logger.isDebugEnabled())
								logger.debug(portName + ":Serial Received:" + line);
							// its not empty!
							if (line.length() > 0) {
								// map it if we havent already
								if (!mapped && uid.matcher(line).matches()) {
									// add to map
									logger.debug(portName + ":Serial Received:" + line);
									String type = StringUtils.substringBetween(line.toString(),
											ConfigConstants.UID + ":", ",");
									if (type != null) {
										logger.debug(portName + ":  device name:" + type);
										deviceType = type.trim();
										mapped = true;
									}
								}
								if (enableSerial) {
									ClientMessage txMsg = session.createMessage(true);
									txMsg.getBodyBuffer().writeString(line);
							
									producer.send(new SimpleString(Config.INCOMING_RAW), txMsg);
									if (logger.isDebugEnabled())
										logger.debug("json = " + line);

								} else {
									if (logger.isDebugEnabled())
										logger.debug("enableSerial false:" + line);
								}
							}
							complete = false;
							line = null;
							lineBuf = new StringBuffer();
						}
					}
				}
			} catch (Exception e) {
				running = false;
				stopReader();
				logger.error(portName, e);
			}

		}

		protected void stopReader() {
			try {
			serialPort.removeEventListener();
			} catch (Exception e1) {
				logger.error(portName, e1);
			}
			try {
				in.close();
			} catch (IOException e1) {
				logger.error(portName, e1);
			}

		}

	}

	/** */

	/**
	 * Set the camel producer, which fire the messages into camel
	 * 
	 * @param clientProducer
	 * @throws ActiveMQException
	 */
	public void setSession(ClientSession session) throws ActiveMQException {
		this.session = session;
		this.producer = session.createProducer(Config.INCOMING_RAW);

	}

	/**
	 * True if the serial port read/write threads are running
	 * 
	 * @return
	 */
	public boolean isRunning() {
		// no good on windoze
		if (!SystemUtils.IS_OS_WINDOWS && !portFile.exists()) {

			try {
				serialPort.close();
				serialReader.stopReader();
			} catch (Exception e) {
				logger.error("Problem disconnecting port " + portName + ", " + e.getMessage());
				logger.debug(e.getMessage(), e);
			}
			running = false;
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
			serialPort.close();
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
	public void process(Exchange exchange) throws Exception {
		// send to device
		String message = exchange.getIn().getBody(String.class);
		logger.debug(portName + ":msg received for device:" + message);
		if (StringUtils.isNotBlank(message)) {
			// check its valid for this device
			if (running && deviceType == null || message.contains(ConfigConstants.UID + ":" + deviceType)) {
				if (logger.isDebugEnabled())
					logger.debug(portName + ":wrote out to device:" + message);
				// queue them and write in background
				if (!queue.offer(message)) {
					if (logger.isDebugEnabled())
						logger.debug("Output queue id full for " + portName);
				}
			}
		}
	}

}
