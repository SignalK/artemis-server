package nz.co.fortytwo.signalk.artemis.intercept;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.core.client.impl.ClientMessageImpl;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import nz.co.fortytwo.signalk.artemis.util.Config;

public class GarbageInterceptorTest {

	@Test
	public void shouldAvoidGarbage() {
		GarbageInterceptor garbage = new GarbageInterceptor();
		ClientMessage msg = new ClientMessageImpl((byte)0, false, 0, System.currentTimeMillis(), (byte) 4,64);
		msg.getBodyBuffer().writeString("Rubbish");
		assertNull(garbage.getContentType(msg));
	}
	

	@Test
	public void shouldProcessNMEA() {
		GarbageInterceptor garbage = new GarbageInterceptor();
		ClientMessage msg = new ClientMessageImpl((byte)0, false, 0, System.currentTimeMillis(), (byte) 4,64);
		msg.getBodyBuffer().writeString("$GPRMC,144629.20,A,5156.91111,N,00434.80385,E,0.295,,011113,,,A*78");
		assertEquals(Config._0183, garbage.getContentType(msg));
	}
	
	@Test
	public void shouldProcessDelta() throws IOException {
		GarbageInterceptor garbage = new GarbageInterceptor();
		String body = FileUtils.readFileToString(new File("./src/test/resources/samples/delta.json"));
		ClientMessage msg = new ClientMessageImpl((byte)0, false, 0, System.currentTimeMillis(), (byte) 4,64);
		msg.getBodyBuffer().writeString(body);
		assertEquals(Config.JSON_DELTA, garbage.getContentType(msg));
	}
	
	@Test
	public void shouldProcessFullVessels() throws IOException {
		GarbageInterceptor garbage = new GarbageInterceptor();
		String body = FileUtils.readFileToString(new File("./src/test/resources/samples/full.json"));
		ClientMessage msg = new ClientMessageImpl((byte)0, false, 0, System.currentTimeMillis(), (byte) 4,64);
		msg.getBodyBuffer().writeString(body);
		assertEquals(Config.JSON_FULL, garbage.getContentType(msg));
	}
	
	@Test
	public void shouldProcessFullConfig() throws IOException {
		GarbageInterceptor garbage = new GarbageInterceptor();
		String body = FileUtils.readFileToString(new File("./src/test/resources/samples/signalk-config.json"));
		ClientMessage msg = new ClientMessageImpl((byte)0, false, 0, System.currentTimeMillis(), (byte) 4,64);
		msg.getBodyBuffer().writeString(body);
		assertEquals(Config.JSON_FULL, garbage.getContentType(msg));
	}

	@Test
	public void shouldProcessResources() throws IOException {
		GarbageInterceptor garbage = new GarbageInterceptor();
		String body = FileUtils.readFileToString(new File("./src/test/resources/samples/full_resources.json"));
		ClientMessage msg = new ClientMessageImpl((byte)0, false, 0, System.currentTimeMillis(), (byte) 4,64);
		msg.getBodyBuffer().writeString(body);
		assertEquals(Config.JSON_FULL, garbage.getContentType(msg));
	}
	@Test
	public void shouldProcessFullSources() throws IOException {
		GarbageInterceptor garbage = new GarbageInterceptor();
		String body = FileUtils.readFileToString(new File("./src/test/resources/samples/full_sources.json"));
		ClientMessage msg = new ClientMessageImpl((byte)0, false, 0, System.currentTimeMillis(), (byte) 4,64);
		msg.getBodyBuffer().writeString(body);
		assertEquals(Config.JSON_FULL, garbage.getContentType(msg));
	}
}
