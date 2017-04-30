package nz.co.fortytwo.signalk.artemis.intercept;

import org.apache.activemq.artemis.core.server.ServerSession;

public class SentenceEventSource {

	private String now;
	private String sourceRef;
	private ServerSession session;

	public SentenceEventSource(String device, String now, ServerSession session) {
		this.now=now;
		this.sourceRef=device; 
		this.session=session;
	}

	public String getNow() {
		return now;
	}

	public String getSourceRef() {
		return sourceRef;
	}

	public ServerSession getSession() {
		return session;
	}

}
