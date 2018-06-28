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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;


/**
 * A manager to monitor the USB tty ports. It dynamically adds/removes
 * ports as the USB devices are added/removed
 * 
 * @author robert
 * 
 */
public class SerialPortManager implements Runnable {

	private static Logger logger = LogManager.getLogger(SerialPortManager.class);

	private List<SerialPortReader> serialPortList = new CopyOnWriteArrayList<SerialPortReader>();

	private boolean running = true;

	//private ClientSession session;

	@SuppressWarnings("static-access")
	public void run() {
		// not running, start now.
		try {
			
			while (running) {
				// remove any stopped readers
				List<SerialPortReader> tmpPortList = new ArrayList<SerialPortReader>();
				for (SerialPortReader reader : serialPortList) {
					if (!reader.isRunning()) {
						if(logger.isDebugEnabled())logger.debug("Comm port " + reader.getPortName() + " finished and marked for removal");
						tmpPortList.add(reader);
						reader.setRunning(false);
					}
					if(logger.isDebugEnabled())logger.debug("Comm port " + reader.getPortName() + " currently running");
				}
				serialPortList.removeAll(tmpPortList);
				//json array
				//String portStr ="[\"/dev/ttyUSB0\",\"/dev/ttyUSB1\",\"/dev/ttyUSB2\"]";
				Json ports = Config.getConfigJsonArray(ConfigConstants.SERIAL_PORTS);
				if(ports==null)ports = Json.array();
				if(!ports.isArray()){
					logger.error("Ports configuration is not a json list, check config: {}",ports);
					ports = Json.array();
				}
				for (Json port:ports.asJsonList()) {
					boolean portOk = false;
					String portStr = port.asString();
					try {
						//this doesnt work  on windozy
						if(!SystemUtils.IS_OS_WINDOWS){
							File portFile = new File(portStr);
							if (!portFile.exists()){
								if(logger.isDebugEnabled())logger.debug("Comm port "+portStr+" doesnt exist");
								continue;
							}
						}
						for (SerialPortReader reader : serialPortList) {
							if (StringUtils.equals(portStr, reader.getPortName())) {
								// its already up and running
								portOk = true;
							}
						}
						// if its running, ignore
						if (portOk){
							if(logger.isDebugEnabled())logger.debug("Comm port " + portStr + " found already connected");
							continue;
						}
	
						
						SerialPortReader serial = new SerialPortReader();
						//serial.setSession(session);
						//default 38400, then freeboard.cfg default, then freeboard.cfg per port
						int baudRate = Config.getConfigPropertyInt(ConfigConstants.SERIAL_PORT_BAUD);
						//get port name
						String portName = portStr;
						if(portStr.lastIndexOf("/")>0){
							portName=portStr.substring(portStr.lastIndexOf("/")+1);
						}
						if(Config.getConfigPropertyInt(ConfigConstants.SERIAL_PORT_BAUD+"."+portName)!=null){
							baudRate = Config.getConfigPropertyInt(ConfigConstants.SERIAL_PORT_BAUD+"."+portName);
							if(logger.isDebugEnabled())logger.debug("Comm port "+portStr+" override baud = "+baudRate);
						}
						
						if(logger.isDebugEnabled())logger.debug("Comm port " + portStr + " found and connecting at "+baudRate+"...");
						serial.connect(portStr, baudRate);
						if(logger.isDebugEnabled())logger.info("Comm port " + portStr + " found and connected");
						serialPortList.add(serial);
					} catch (NullPointerException np) {
						logger.error("Comm port " + portStr + " was null, probably not found, or nothing connected");
//					} catch (NoSuchPortException nsp) {
//						logger.error("Comm port " + portStr + " not found, or nothing connected");
//					}catch(PortInUseException p){
//						logger.error("Comm port " + portStr + " Port in use exception");
					} catch (Exception e) {
						logger.error("Port " + portStr + " failed", e);
					}
				}
				// delay for 30 secs, we dont want to burn up CPU for nothing
				try {
					Thread.currentThread().sleep(10 * 1000);
				} catch (InterruptedException ie) {
				}
			}
			//finished, so clean up
		} catch (Exception e1) {
			logger.error(e1.getMessage(),e1);
		}finally{
			serialPortList.clear();
		}
			
	}

	/**
	 * When the serial port is used to read from the arduino this must be called to shut
	 * down the readers, which are in their own threads.
	 */
	public void stopSerial() {

		for (SerialPortReader serial : serialPortList) {
			if (serial != null) {
				serial.setRunning(false);
			}
		}
		running = false;
	}

	public void process(String message) throws Exception {
		for (SerialPortReader serial : serialPortList) {
			if (serial != null) {
				serial.process(message);
			}
		}
		
	}

	@Override
	protected void finalize() throws Throwable {
		 stopSerial();
		super.finalize();
	}

}
