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

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import mjson.Json;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

@Sharable
public class ArtemisNettyHandler extends SimpleChannelInboundHandler<String> {

	private static Logger logger = LogManager.getLogger(ArtemisNettyHandler.class);
	private BiMap<String, ChannelHandlerContext> contextList = HashBiMap.create();
	private BiMap<String, ClientSession> sessionList = HashBiMap.create();
	private BiMap<String, ClientProducer> producerList = HashBiMap.create();
	private BiMap<String, ClientConsumer> consumerList = HashBiMap.create();
	private String outputType;


	public ArtemisNettyHandler(String outputType) throws Exception {

		this.outputType = outputType;

	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// Send greeting for a new connection.
		if (logger.isDebugEnabled())
			logger.debug("channelActive:" + ctx);
		ctx.write(Util.getWelcomeMsg().toString() + "\r\n");
		ctx.flush();

		String session = ctx.channel().id().asLongText();
		
		contextList.put(session, ctx);
		ClientSession rxSession = Util.getVmSession(Config.getConfigProperty(Config.ADMIN_USER),
				Config.getConfigProperty(Config.ADMIN_PWD));
		rxSession.start();
		sessionList.put(session, rxSession);
		rxSession.createTemporaryQueue("outgoing.reply." + session, RoutingType.ANYCAST, session);

		ClientProducer producer = rxSession.createProducer();
		producerList.put(session, producer);

		// setup consumer
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
		if (logger.isDebugEnabled())
			logger.debug("channelActive, ready:" + ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		// unsubscribe all
		String session = contextList.inverse().get(ctx);
		// txSession.deleteQueue("outgoing.reply." + session);
		consumerList.get(session).close();
		producerList.get(session).close();
		sessionList.get(session).close();		
		super.channelInactive(ctx);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String request) throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("Request:" + request);
		String session = contextList.inverse().get(ctx);
		ClientProducer producer = producerList.get(session);
		ClientMessage msg = sessionList.get(session).createMessage(false);
		msg.getBodyBuffer().writeString(request);
		getHeaders(ctx, msg);
		String tempQ = contextList.inverse().get(ctx);

		msg.putStringProperty(Config.AMQ_REPLY_Q, tempQ);
		producer.send(Config.INCOMING_RAW, msg);
	}

	private ClientMessage getHeaders(ChannelHandlerContext ctx, ClientMessage ex) throws Exception {

		ex.putStringProperty(Config.AMQ_SESSION_ID, contextList.inverse().get(ctx));
		String remoteAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
		// remoteAddress=remoteAddress.replace("/","");
		ex.putStringProperty(Config.MSG_SRC_IP, remoteAddress);
		ex.putStringProperty(Config.MSG_SRC_BUS, "tcp." + remoteAddress.replace('.', '_'));
		ex.putStringProperty(ConfigConstants.OUTPUT_TYPE, outputType);
		String localAddress = ((InetSocketAddress) ctx.channel().localAddress()).getAddress().getHostAddress();
		// localAddress=localAddress.replace("/","");
		if (logger.isDebugEnabled())
			logger.debug("IP: local:" + localAddress + ", remote:" + remoteAddress);
		if (Util.sameNetwork(localAddress, remoteAddress)) {
			ex.putStringProperty(Config.MSG_SRC_TYPE, Config.INTERNAL_IP);
		} else {
			ex.putStringProperty(Config.MSG_SRC_TYPE, Config.EXTERNAL_IP);
		}
		return ex;
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

	public ChannelHandlerContext getChannel(String sessionId) {
		return contextList.get(sessionId);
	}

	public Map<String, ChannelHandlerContext> getContextList() {
		return contextList;
	}

	@Override
	public boolean acceptInboundMessage(Object msg) throws Exception {
		if (msg instanceof Json || msg instanceof String)
			return true;
		return super.acceptInboundMessage(msg);
	}

	@Override
	protected void finalize() throws Throwable{
		for(Entry<String, ClientConsumer> entry:consumerList.entrySet()){
			try {
				entry.getValue().close();
			} catch (ActiveMQException e) {
				logger.error(e,e);
			}
		}
		consumerList.clear();
		for(Entry<String, ClientProducer> entry:producerList.entrySet()){
			try {
				entry.getValue().close();
			} catch (ActiveMQException e) {
				logger.error(e,e);
			}
		}
		producerList.clear();
		for(Entry<String, ClientSession> entry:sessionList.entrySet()){
			try {
				entry.getValue().close();
			} catch (ActiveMQException e) {
				logger.error(e,e);
			}
		}
		sessionList.clear();
	}
	public void process(ClientMessage message) throws Exception {

		String msg = Util.readBodyBufferToString(message);
		if (logger.isDebugEnabled())
			logger.debug("TCP sending msg : " + msg);
		if (msg != null) {
			// get the session
			String session = message.getStringProperty(Config.AMQ_SUB_DESTINATION);
			if (logger.isDebugEnabled())
				logger.debug("TCP session:" + session);
			if (Config.SK_SEND_TO_ALL.equals(session)) {
				// tcp
				for (String key : contextList.keySet()) {
					ChannelHandlerContext ctx = getChannel(key);
					if (ctx != null && ctx.channel().isWritable())
						ctx.pipeline().writeAndFlush(msg + "\r\n");
				}
			} else {
				// tcp
				ChannelHandlerContext ctx = contextList.get(session);
				if (logger.isDebugEnabled())
					logger.debug("TCP send to :" + ctx);
				if (ctx != null && ctx.channel().isWritable())
					ctx.pipeline().writeAndFlush(msg + "\r\n");
			}
		}

	}

}
