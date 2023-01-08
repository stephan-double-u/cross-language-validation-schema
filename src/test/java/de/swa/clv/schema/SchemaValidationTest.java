package de.swa.clv.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

// Kudos to https://www.javatpoint.com/json-validator-java
class SchemaValidationTest {

        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        ObjectMapper objectMapper = new ObjectMapper();

        Path schemaPath = Paths.get("src/main/resources/cross-language-validation-schema.json");
        InputStream schemaStream = Files.newInputStream(schemaPath);
        JsonSchema schema = schemaFactory.getSchema(schemaStream);

    SchemaValidationTest() throws IOException {
    }

    private static Stream<Arguments> testCaseProvider() {
        return Stream.of(
                arguments("minimal.json", List.of()),
                arguments("ruleKeysEmptyObjects.json", List.of()),
                arguments("ruleKeysNoObjects.json", List.of(
                        "$.contentRules: array wurde gefunden aber object erwartet",
                        "$.immutableRules: string wurde gefunden aber object erwartet",
                        "$.updateRules: integer wurde gefunden aber object erwartet",
                        "$.mandatoryRules: null wurde gefunden aber object erwartet")),
                arguments("missingSchemeVersionKey.json", List.of(
                        "$.schemaVersion ist ein Pflichtfeld aber fehlt")),
                arguments("unknownKey.json", List.of(
                        "$.foo ist nicht im Schema definiert und das Schema verbietet additionalProperties")),
                arguments("schema-example.json", List.of())
        );
    }

    @ParameterizedTest
    @MethodSource("testCaseProvider")
    void validateExampleJson(String jsonTestResource, List<String> expectedErrors) throws IOException {
        Path jsonPath = Paths.get("src/test/resources/" + jsonTestResource);

        try (InputStream jsonStream = Files.newInputStream(jsonPath)) {
            JsonNode json = objectMapper.readTree(jsonStream);
            Set<ValidationMessage> validationMsgs = schema.validate(json);

            if (expectedErrors.size() != validationMsgs.size()) {
                validationMsgs.forEach(vm -> System.out.println(vm.getMessage()));
            }

            assertEquals(expectedErrors.size(), validationMsgs.size());
            validationMsgs.stream()
                    .map(ValidationMessage::getMessage)
                    .forEach(m -> assertTrue(expectedErrors.contains(m)));
        }
    }
}
