package nz.co.fortytwo.signalk.artemis.service;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;

import nz.co.fortytwo.signalk.artemis.server.SubscriptionManager;

import javax.servlet.http.HttpSession;

import static nz.co.fortytwo.signalk.util.SignalKConstants.SIGNALK_WS;

import java.awt.*;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class SignalkStreamService {

	private static Logger logger = LogManager.getLogger(SignalkStreamService.class);

	public static final int PLAYFIELD_WIDTH = 640;
	public static final int PLAYFIELD_HEIGHT = 480;
	public static final int GRID_SIZE = 10;

	protected static final AtomicInteger snakeIds = new AtomicInteger(0);
	protected static final Random random = new Random();

	protected SignalkStreamBroadcaster signalkStreamBroadcaster;

	public SignalkStreamService() {
	}

	public void onOpen(AtmosphereResource resource) {
		logger.debug("onOpen:"+resource);
		try {
			signalkStreamBroadcaster().onOpen(resource);
		
			signalkStreamBroadcaster().broadcast(String.format("{'type': 'join','data':[%s]}", resource.toString()));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void onClose(AtmosphereResource resource) {
		logger.debug("onClose:"+resource);
		try {
			signalkStreamBroadcaster().onClose(resource);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		signalkStreamBroadcaster().broadcast(
				String.format("{'type': 'leave', 'id': %d}", ((Integer) resource.session().getAttribute("id"))));
	}


	/**
	 * we receive a message from client
	 * @param resource
	 * @param message
	 */
	protected void onMessage(AtmosphereResource resource, String message) {
		logger.debug("onMessage:"+message);
		//send to incoming.input
		try {
			signalkStreamBroadcaster().onMessage(message);
		} catch (ActiveMQException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	SignalkStreamBroadcaster signalkStreamBroadcaster() {
		if (signalkStreamBroadcaster == null) {
			signalkStreamBroadcaster = new SignalkStreamBroadcaster(factory().lookup(SIGNALK_WS, true));
		}
		return signalkStreamBroadcaster;
	}

	abstract BroadcasterFactory factory();
}
