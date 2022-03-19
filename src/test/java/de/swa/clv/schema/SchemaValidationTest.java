package de.swa.clv.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.Assert.assertTrue;

// Kudos to https://www.javatpoint.com/json-validator-java
public class SchemaValidationTest {

    @Test
    public void validateExampleJson() throws IOException {

        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        ObjectMapper objectMapper = new ObjectMapper();

        Path schemaPath = Paths.get("src/main/resources/cross-language-validation-schema.json");
        Path jsonPath = Paths.get("src/test/resources/schema-example.json");
        try (
                InputStream schemaStream = Files.newInputStream(schemaPath);
                InputStream jsonStream = Files.newInputStream(jsonPath)
        ) {
            JsonSchema schema = schemaFactory.getSchema(schemaStream);
            JsonNode json = objectMapper.readTree(jsonStream);

            Set<ValidationMessage> validationMsgs = schema.validate(json);

            if (!validationMsgs.isEmpty()) {
                validationMsgs.forEach(vm -> System.out.println(vm.getMessage()));
            }
            assertTrue(validationMsgs.isEmpty());
        }
    }
}
