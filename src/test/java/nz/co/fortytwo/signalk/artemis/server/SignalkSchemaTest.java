package nz.co.fortytwo.signalk.artemis.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import io.apptik.json.JsonElement;
import io.apptik.json.JsonObject;
import io.apptik.json.JsonReader;
import io.apptik.json.exception.JsonException;
import io.apptik.json.generator.JsonGenerator;
import io.apptik.json.generator.JsonGeneratorConfig;
import io.apptik.json.schema.Schema;
import io.apptik.json.schema.SchemaV4;

@Ignore
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
	
	@Test
	public void shouldCreateSignalkJson() throws JsonException, IOException, URISyntaxException{
		
		JsonReader reader = new JsonReader(new InputStreamReader(
				new URL("https://signalk.org/specification/1.0.0/schemas/groups/navigation.json").openStream()));
		reader.setLenient(true);
		
		 Schema schema = new SchemaV4();
		 schema.setOrigSrc(new URI("https://signalk.org/specification/1.0.0/schemas/groups/"));
		 
		 schema.wrap((JsonObject) JsonElement.readFrom(reader));
		 JsonGeneratorConfig cfg= new JsonGeneratorConfig();
		 cfg.globalArrayItemsMax=5;
		 cfg.globalArrayItemsMin=2;
		
		 //cfg.globalObjectPropertiesMin=1;
		 //cfg.globalUriPaths.add("file:///home/robert/gitrep/specification/schemas/definitions.json");
		 logger.debug(new JsonGenerator(schema, cfg).generate());
	}

}
