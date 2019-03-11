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
import java.util.Map;
import java.util.Map.Entry;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQIllegalStateException;
import org.apache.activemq.artemis.api.core.ActiveMQNonExistentQueueException;
import org.apache.activemq.artemis.api.core.ActiveMQQueueExistsException;
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
import nz.co.fortytwo.signalk.artemis.subscription.SubscriptionManagerFactory;
import nz.co.fortytwo.signalk.artemis.util.Config;
import nz.co.fortytwo.signalk.artemis.util.ConfigConstants;
import nz.co.fortytwo.signalk.artemis.util.Util;

@Sharable
public class ArtemisTcpNettyHandler extends SimpleChannelInboundHandler<String> {

	private static Logger logger = LogManager.getLogger(ArtemisTcpNettyHandler.class);

	private ClientSession rxSession;
	private ClientProducer producer;
	private BiMap<String, ChannelHandlerContext> contextList = HashBiMap.create();

	private BiMap<String, ClientConsumer> consumerList = HashBiMap.create();
	private String outputType;


	public ArtemisTcpNettyHandler(String outputType) throws Exception {

		this.outputType = outputType;
		try {
			rxSession = Util.getVmSession(Config.getConfigProperty(Config.ADMIN_USER),
					Config.getConfigProperty(Config.ADMIN_PWD));
			producer = rxSession.createProducer();
			rxSession.start();
		} catch (Exception e) {
			logger.error(e, e);
		}
		
	}

	private synchronized void send(ClientMessage msg) throws ActiveMQException{
		producer.send(Config.INCOMING_RAW, msg);
		if(logger.isDebugEnabled())logger.debug("producer.send: {}", msg);
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// Send greeting for a new connection.
		if (logger.isDebugEnabled())
			logger.debug("channelActive:" + ctx);
		ctx.write(Util.getWelcomeMsg().toString() + "\r\n");
		ctx.flush();

		String sessionId = ctx.channel().id().asLongText();

		contextList.put(sessionId, ctx);
		createTemporaryQueue("outgoing.reply." + sessionId, RoutingType.ANYCAST, sessionId);

		// setup consumer
		ClientConsumer consumer = createConsumer(sessionId, false);
		consumer.setMessageHandler(new MessageHandler() {

			@Override
			public void onMessage(ClientMessage message) {
				try {
					message.acknowledge();
					process(message);
					
				}catch (Exception e){
					logger.error(e,e);
				}

			}
		});
		consumerList.put(sessionId, consumer);
		if (logger.isDebugEnabled())
			logger.debug("channelActive, ready:" + ctx);
	}

	private ClientConsumer createConsumer(String sessionId, boolean browseOnly) throws ActiveMQException {
		synchronized (rxSession) {
			return rxSession.createConsumer(sessionId, browseOnly);
		}
	}

	private void createTemporaryQueue(String queue, RoutingType anycast, String session) throws ActiveMQException {
		try{
			synchronized (rxSession) {
				rxSession.createTemporaryQueue(queue, anycast, session);
			}
		}catch (ActiveMQQueueExistsException e) {
			logger.debug(e);
		} 
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		// unsubscribe all
		String sessionId = contextList.inverse().get(ctx);
		// txSession.deleteQueue("outgoing.reply." + session);
		consumerList.get(sessionId).close();
		deleteQueue(sessionId);
		super.channelInactive(ctx);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String request) throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("Request: {}",request);

		ClientMessage msg = null;
		synchronized (rxSession) {
			msg=rxSession.createMessage(false);
		}
		msg.getBodyBuffer().writeString(request);
		getHeaders(ctx, msg);
		String tempQ = contextList.inverse().get(ctx);

		msg.putStringProperty(Config.AMQ_REPLY_Q, tempQ);
		
		send(msg);
	}

	private ClientMessage getHeaders(ChannelHandlerContext ctx, ClientMessage ex) throws Exception {

		ex.putStringProperty(Config.AMQ_SESSION_ID, contextList.inverse().get(ctx));
		ex.putStringProperty(Config.AMQ_CORR_ID, contextList.inverse().get(ctx));
		 InetAddress remoteAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress();
		// remoteAddress=remoteAddress.replace("/","");
		ex.putStringProperty(Config.MSG_SRC_IP, remoteAddress.getHostAddress());
		ex.putStringProperty(Config.MSG_SRC_BUS, "tcp." + remoteAddress.getHostAddress().replace('.', '_'));
		ex.putStringProperty(ConfigConstants.OUTPUT_TYPE, outputType);
		String localAddress = ((InetSocketAddress) ctx.channel().localAddress()).getAddress().getHostAddress();
		// localAddress=localAddress.replace("/","");
		if (logger.isDebugEnabled())
			logger.debug("IP: local:{}, remote:{}", localAddress , remoteAddress);
		if (remoteAddress.isLoopbackAddress()|| remoteAddress.isAnyLocalAddress())  {
			ex.putStringProperty(Config.MSG_SRC_TYPE, Config.MSG_SRC_TYPE_INTERNAL_IP);
		} else {
			ex.putStringProperty(Config.MSG_SRC_TYPE, Config.MSG_SRC_TYPE_EXTERNAL_IP);
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
	protected void finalize() throws Throwable {
		for (Entry<String, ClientConsumer> entry : consumerList.entrySet()) {
			try {
				entry.getValue().close();
				try {
					deleteQueue(entry.getKey());
				} catch (ActiveMQNonExistentQueueException e) {
					logger.debug(e.getMessage());
				} catch (ActiveMQIllegalStateException e) {
					logger.debug(e.getMessage());
				}
			} catch (ActiveMQException e) {
				logger.error(e, e);
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

	private void deleteQueue(String key) throws ActiveMQException {
		synchronized (rxSession) {
			rxSession.deleteQueue(key);
		}
	}

	public void process(ClientMessage message) throws Exception {

		String msg = Util.readBodyBufferToString(message);
		if (logger.isDebugEnabled())
			logger.debug("TCP sending msg : {} ", msg);
		if (msg != null) {
			// get the session
			String session = message.getStringProperty(Config.AMQ_SUB_DESTINATION);
			if (logger.isDebugEnabled())
				logger.debug("TCP session: {}", session);
			
			// tcp
			ChannelHandlerContext ctx = contextList.get(session);
			if (logger.isDebugEnabled())
				logger.debug("TCP send to : {}", ctx);
			if(ctx == null || !ctx.channel().isWritable()){
				//cant send, kill it
				try {
					consumerList.get(session).close();
				} catch (ActiveMQException e) {
					logger.error(e.getMessage(), e);
				}
				try {
					SubscriptionManagerFactory.getInstance().removeByTempQ(session);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
				consumerList.remove(session);
				return;
			}
			
			if (ctx != null )
				ctx.pipeline().writeAndFlush(msg + "\r\n");
		
		}

	}

}
