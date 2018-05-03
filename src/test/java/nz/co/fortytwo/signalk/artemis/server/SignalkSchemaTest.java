package nz.co.fortytwo.signalk.artemis.server;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

public class SignalkSchemaTest {
	private static Logger logger = LogManager.getLogger(SignalkSchemaTest.class);



	@Test
	public void shouldValidateFullJson() throws URISyntaxException, ProcessingException {
		try {
			JsonNode fstabSchema = JsonLoader
					.fromURL(new URL("https://signalk.org/specification/1.0.0/schemas/signalk.json"));

			JsonSchemaFactory factory = JsonSchemaFactory.byDefault();

			JsonSchema schema = factory.getJsonSchema(fstabSchema);
			File dir = new File("./src/test/resources/samples/full");
			for (File f : dir.listFiles()) {
				if (f.isDirectory())
					continue;
				logger.debug("Validating sample:" + f.getName());
				JsonNode good = JsonLoader.fromFile(f);

				ProcessingReport report = schema.validateUnchecked(good,true);
				logger.debug("	Is valid? {} : {}",f.getName(), report.isSuccess());
				report.forEach((a)->{
					if(a.getLogLevel().equals(LogLevel.ERROR))
					 logger.debug(a);
				});
				
			}
		} catch (IOException e) {
			logger.error(e, e);
		}
	}
	
	@Test
	public void shouldValidateDeltaJson() throws URISyntaxException, ProcessingException {
		try {
			JsonNode fstabSchema = JsonLoader
					.fromURL(new URL("https://signalk.org/specification/1.0.0/schemas/delta.json"));

			JsonSchemaFactory factory = JsonSchemaFactory.byDefault();

			JsonSchema schema = factory.getJsonSchema(fstabSchema);
			File dir = new File("./src/test/resources/samples/delta");
			for (File f : dir.listFiles()) {
				if (f.isDirectory())
					continue;
				logger.debug("Testing sample:" + f.getName());
				JsonNode good = JsonLoader.fromFile(f);

				ProcessingReport report = schema.validateUnchecked(good,true);
				logger.debug("Validating {} : {}",f.getName(), report.isSuccess());
				report.forEach((a)->{
					if(a.getLogLevel().equals(LogLevel.ERROR))
					 logger.debug(a);
				});
				
			}
		} catch (IOException e) {
			logger.error(e, e);
		}
	}

}
