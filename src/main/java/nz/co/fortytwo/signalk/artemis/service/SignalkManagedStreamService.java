package nz.co.fortytwo.signalk.artemis.service;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atmosphere.config.service.Disconnect;
import org.atmosphere.config.service.ManagedService;
import org.atmosphere.config.service.Post;
import org.atmosphere.config.service.Ready;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceFactory;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.HeaderConfig;

import nz.co.fortytwo.signalk.artemis.server.SubscriptionManager;

//@ManagedService(path = "/signalk/v1/stream")
public class SignalkManagedStreamService  {

	private static Logger logger = LogManager.getLogger(SignalkManagedStreamService.class);

	private final ConcurrentLinkedQueue<String> uuids = new ConcurrentLinkedQueue<String>();

	@Inject
	private BroadcasterFactory factory;

	@Inject
	private AtmosphereResourceFactory resourceFactory;

	@Ready
	public void onReady(final AtmosphereResource r) {
		if(logger.isDebugEnabled())logger.debug("onReady:"+r);
		if (!uuids.contains(r.uuid())) {

			//super.onOpen(r);

			uuids.add(r.uuid());
		}
	}

	@Disconnect
	public void onDisconnect(AtmosphereResourceEvent event) {
		if(logger.isDebugEnabled())logger.debug("onDisconnect:"+event);
		AtmosphereRequest request = event.getResource().getRequest();
		String s = request.getHeader(HeaderConfig.X_ATMOSPHERE_TRANSPORT);
		if (s != null && s.equalsIgnoreCase(HeaderConfig.DISCONNECT_TRANSPORT_MESSAGE)) {
		//	SignalkManagedStreamService.super.onClose(event.getResource());
			uuids.remove(event.getResource().uuid());
		}
	}

	@Post
	public void onMessage(AtmosphereResource resource) {
		if(logger.isDebugEnabled())logger.debug("onMessage:"+resource);
		//try {
			// Here we need to find the suspended AtmosphereResource
			//super.onMessage(resourceFactory.find(resource.uuid()), resource.getRequest().getReader().readLine());
		//} catch (IOException e) {
		//	e.printStackTrace();
		//}
	}

//	@Override
//	BroadcasterFactory factory() {
//		return factory;
//	}
}
