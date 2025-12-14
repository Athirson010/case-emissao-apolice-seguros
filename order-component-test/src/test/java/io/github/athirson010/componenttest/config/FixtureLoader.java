package io.github.athirson010.componenttest.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FixtureLoader {

    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

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

    @SneakyThrows
    public static <T> T loadFixture(String fileName, Class<T> valueType) {
        String json = loadFixtureAsString(fileName);
        return objectMapper.readValue(json, valueType);
    }

    @SneakyThrows
    public static String toJson(Object object) {
        return objectMapper.writeValueAsString(object);
    }

    @SneakyThrows
    public static String toPrettyJson(Object object) {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }

    @SneakyThrows
    public static <T> T fromJson(String json, Class<T> valueType) {
        return objectMapper.readValue(json, valueType);
    }

}
