package io.github.athirson010.componenttest.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for loading test fixtures from JSON files.
 * Provides methods to load and parse JSON fixtures from the test resources.
 */
public class FixtureLoader {

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    /**
     * Loads a JSON fixture file as a String
     *
     * @param fileName the name of the fixture file (relative to fixtures/ directory)
     * @return the content of the file as a String
     */
    @SneakyThrows
    public static String loadFixtureAsString(String fileName) {
        String path = "fixtures/" + fileName;
        try (InputStream inputStream = FixtureLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Fixture file not found: " + path);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Loads a JSON fixture file and parses it to the specified type
     *
     * @param fileName the name of the fixture file (relative to fixtures/ directory)
     * @param valueType the class type to parse the JSON into
     * @param <T> the type of the object to return
     * @return the parsed object
     */
    @SneakyThrows
    public static <T> T loadFixture(String fileName, Class<T> valueType) {
        String json = loadFixtureAsString(fileName);
        return objectMapper.readValue(json, valueType);
    }

    /**
     * Converts an object to JSON string
     *
     * @param object the object to convert
     * @return the JSON representation
     */
    @SneakyThrows
    public static String toJson(Object object) {
        return objectMapper.writeValueAsString(object);
    }

    /**
     * Converts an object to pretty printed JSON string
     *
     * @param object the object to convert
     * @return the pretty printed JSON representation
     */
    @SneakyThrows
    public static String toPrettyJson(Object object) {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }

    /**
     * Parses a JSON string to the specified type
     *
     * @param json the JSON string
     * @param valueType the class type to parse into
     * @param <T> the type of the object to return
     * @return the parsed object
     */
    @SneakyThrows
    public static <T> T fromJson(String json, Class<T> valueType) {
        return objectMapper.readValue(json, valueType);
    }

    /**
     * Gets the ObjectMapper instance for custom operations
     *
     * @return the ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
