/*
 * 
 * Copyright (C) 2012-2014 R T Huitema. All Rights Reserved.
 * Web: www.42.co.nz
 * Email: robert@42.co.nz
 * Author: R T Huitema
 * 
 * This file is part of the signalk-server-java project
 * 
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
 * WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nz.co.fortytwo.signalk.artemis.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.activemq.artemis.core.client.impl.ClientMessageImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;
import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

@Sharable
public class ArtemisUdpNettyHandler extends SimpleChannelInboundHandler<DatagramPacket> {

	private static Logger logger = LogManager.getLogger(ArtemisUdpNettyHandler.class);
	
	private static ClientSession rxSession;
	private static ClientProducer producer;
	
	private BiMap<String, InetSocketAddress> socketList = HashBiMap.create();
	//private BiMap<String, ClientSession> sessionList = HashBiMap.create();
	private Map<String, ChannelHandlerContext> channelList = new HashMap<>();
	//private BiMap<String, ClientProducer> producerList = HashBiMap.create();
	private BiMap<String, ClientConsumer> consumerList = HashBiMap.create();

	private String outputType;
	//private ClientConsumer consumer;

	static {
		try {
			rxSession = Util.getVmSession(Config.getConfigProperty(Config.ADMIN_USER),
					Config.getConfigProperty(Config.ADMIN_PWD));
			producer = rxSession.createProducer();
			rxSession.start();
		} catch (Exception e) {
			logger.error(e, e);
		}
	}
	
	public ArtemisUdpNettyHandler(String outputType) throws Exception {
		this.outputType = outputType;
	}

	private synchronized void send(ClientMessage msg) throws ActiveMQException{
		producer.send(Config.INCOMING_RAW, msg);
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// Send greeting for a new connection.
		NioDatagramChannel udpChannel = (NioDatagramChannel) ctx.channel();
		if (logger.isDebugEnabled())
			logger.debug("channelActive:" + udpChannel.localAddress());
		// TODO: associate the ip with a user?
		// TODO: get user login

		if (logger.isDebugEnabled())
			logger.debug("channelActive, ready:" + ctx);

	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
		String request = packet.content().toString(CharsetUtil.UTF_8);
		if (logger.isDebugEnabled())
			logger.debug("Sender " + packet.sender() + " sent request:" + request);
		String session = packet.sender().getAddress().getHostAddress()+":"+packet.sender().getPort();//ctx.channel().id().asLongText();
		NioDatagramChannel udpChannel = (NioDatagramChannel) ctx.channel();
		String localAddress = udpChannel.localAddress().getAddress().getHostAddress();
		InetAddress remoteAddress = packet.sender().getAddress(); 
		
		if (!socketList.inverse().containsKey(packet.sender())) {
			
			socketList.put(session, packet.sender());
			if (logger.isDebugEnabled())
				logger.debug("Added Sender " + packet.sender() + ", session:" + session);
			ctx.channel()
					.writeAndFlush(new DatagramPacket(
							Unpooled.copiedBuffer(Util.getWelcomeMsg().toString() + "\r\n", CharsetUtil.UTF_8),
							packet.sender()));
			// setup consumer
			if(!consumerList.containsKey(session)){
				
				rxSession.createTemporaryQueue("outgoing.reply." + session, RoutingType.ANYCAST, session);
				
				ClientConsumer consumer = rxSession.createConsumer(session, false);
				consumer.setMessageHandler(new MessageHandler() {
	
					@Override
					public void onMessage(ClientMessage message) {
						try {
							process(message);
						} catch (Exception e) {
							logger.error(e.getMessage(), e);
						}
	
					}
				});
				consumerList.put(session,consumer);
			}
			if(!channelList.containsKey(session)){
				channelList.put(session,ctx);
			}
		}

		Map<String, Object> headers = getHeaders(session, remoteAddress, localAddress);
		ClientMessage ex = new ClientMessageImpl((byte) 0, false, 0, System.currentTimeMillis(), (byte) 4, 1024);
		ex.getBodyBuffer().writeString(request);
		ex.putStringProperty(Config.AMQ_REPLY_Q, session);
		
		for (String hdr : headers.keySet()) {
			ex.putStringProperty(hdr, headers.get(hdr).toString());
		}
		send(ex);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}

	@Override
	public boolean acceptInboundMessage(Object msg) throws Exception {
		if (msg instanceof Json || msg instanceof String)
			return true;
		return super.acceptInboundMessage(msg);
	}

	private Map<String, Object> getHeaders(String wsSession, InetAddress remoteAddress, String localIp) throws Exception {
		
		Map<String, Object> headers = new HashMap<>();
		headers.put(Config.AMQ_SESSION_ID, wsSession);
		headers.put(Config.AMQ_CORR_ID, wsSession);
		headers.put(Config.MSG_SRC_IP, remoteAddress.getHostAddress());
		headers.put(Config.MSG_SRC_BUS, "udp." + remoteAddress.getHostAddress().replace('.', '_'));
		//TODO: fix UDP ip network source
		if (logger.isDebugEnabled())
			logger.debug("IP: local:" + localIp + ", remote:" + remoteAddress.getHostAddress());
		if (remoteAddress.isLoopbackAddress()|| remoteAddress.isAnyLocalAddress()) {
				//Util.sameNetwork(localIp, remoteAddress)
			headers.put(Config.MSG_SRC_TYPE, Config.INTERNAL_IP);
		} else {
			headers.put(Config.MSG_SRC_TYPE, Config.EXTERNAL_IP);
		}
		headers.put(ConfigConstants.OUTPUT_TYPE, outputType);
		return headers;
	}

	@Override
	protected void finalize() throws Throwable{
		channelList.clear();
		socketList.clear();
		for(Entry<String, ClientConsumer> entry:consumerList.entrySet()){
			try {
				entry.getValue().close();
			} catch (ActiveMQException e) {
				logger.error(e,e);
			}
		}
		consumerList.clear();
		if (producer != null) {
			try {
				producer.close();
			} catch (ActiveMQException e) {
				logger.warn(e, e);
			}
		}

		if (rxSession != null) {
			try {
				rxSession.close();
			} catch (ActiveMQException e) {
				logger.warn(e, e);
			}
		}
	}

	public void process(ClientMessage message) throws Exception {

		String msg = Util.readBodyBufferToString(message);
		if (logger.isDebugEnabled())
			logger.debug("UDP sending msg : " + msg);
		if (msg != null) {
			// get the session
			String session = message.getStringProperty(Config.AMQ_SUB_DESTINATION);
			if (logger.isDebugEnabled())
				logger.debug("UDP session id:" + session);
			if (Config.SK_SEND_TO_ALL.equals(session)) {
				// udp
				
					for (InetSocketAddress client : socketList.values()) {
						if (logger.isDebugEnabled())
							logger.debug("Sending udp: " + msg);
						// udpCtx.pipeline().writeAndFlush(msg+"\r\n");
						((NioDatagramChannel) channelList.get(session).channel()).writeAndFlush(
								new DatagramPacket(Unpooled.copiedBuffer(msg + "\r\n", CharsetUtil.UTF_8), client));
						if (logger.isDebugEnabled())
							logger.debug("Sent udp to " + client);
					}
				
				
			} else {

				// udp
		
					final InetSocketAddress client = socketList.get(session);
					if (logger.isDebugEnabled())
						logger.debug("Sending udp: " + msg);
					// udpCtx.pipeline().writeAndFlush(msg+"\r\n");
					((NioDatagramChannel) channelList.get(session).channel()).writeAndFlush(
							new DatagramPacket(Unpooled.copiedBuffer(msg + "\r\n", CharsetUtil.UTF_8), client));
					if (logger.isDebugEnabled())
						logger.debug("Sent udp for session: " + session);
					// TODO: how do we tell when a UDP client is gone
				
				
			}
		}

	}
}
