package nz.co.fortytwo.signalk.artemis.divert;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.apache.activemq.artemis.core.server.ServerMessage;
import org.apache.activemq.artemis.core.server.impl.ServerMessageImpl;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import nz.co.fortytwo.signalk.artemis.util.Config;

public class GarbageMsgTest {

	@Test
	public void shouldAvoidGarbage() {
		GarbageFilter garbage = new GarbageFilter();
		ServerMessage msg = new ServerMessageImpl(123456L,64);
		msg.getBodyBuffer().writeString("Rubbish");
		assertNull(garbage.getContentType(msg));
	}
	

	@Test
	public void shouldProcessNMEA() {
		GarbageFilter garbage = new GarbageFilter();
		ServerMessage msg = new ServerMessageImpl(123456L,64);
		msg.getBodyBuffer().writeString("$GPRMC,144629.20,A,5156.91111,N,00434.80385,E,0.295,,011113,,,A*78");
		assertEquals(Config._0183, garbage.getContentType(msg));
	}
	
	@Test
	public void shouldProcessDelta() throws IOException {
		GarbageFilter garbage = new GarbageFilter();
		String body = FileUtils.readFileToString(new File("./src/test/resources/samples/delta.json"));
		ServerMessage msg = new ServerMessageImpl(123456L,64);
		msg.getBodyBuffer().writeString(body);
		assertEquals(Config.JSON_DELTA, garbage.getContentType(msg));
	}
	
	@Test
	public void shouldProcessFullVessels() throws IOException {
		GarbageFilter garbage = new GarbageFilter();
		String body = FileUtils.readFileToString(new File("./src/test/resources/samples/full.json"));
		ServerMessage msg = new ServerMessageImpl(123456L,64);
		msg.getBodyBuffer().writeString(body);
		assertEquals(Config.JSON_FULL, garbage.getContentType(msg));
	}
	
	@Test
	public void shouldProcessFullConfig() throws IOException {
		GarbageFilter garbage = new GarbageFilter();
		String body = FileUtils.readFileToString(new File("./src/test/resources/samples/signalk-config.json"));
		ServerMessage msg = new ServerMessageImpl(123456L,64);
		msg.getBodyBuffer().writeString(body);
		assertEquals(Config.JSON_FULL, garbage.getContentType(msg));
	}

	@Test
	public void shouldProcessResources() throws IOException {
		GarbageFilter garbage = new GarbageFilter();
		String body = FileUtils.readFileToString(new File("./src/test/resources/samples/full_resources.json"));
		ServerMessage msg = new ServerMessageImpl(123456L,64);
		msg.getBodyBuffer().writeString(body);
		assertEquals(Config.JSON_FULL, garbage.getContentType(msg));
	}
	@Test
	public void shouldProcessFullSources() throws IOException {
		GarbageFilter garbage = new GarbageFilter();
		String body = FileUtils.readFileToString(new File("./src/test/resources/samples/full_sources.json"));
		ServerMessage msg = new ServerMessageImpl(123456L,64);
		msg.getBodyBuffer().writeString(body);
		assertEquals(Config.JSON_FULL, garbage.getContentType(msg));
	}
}
