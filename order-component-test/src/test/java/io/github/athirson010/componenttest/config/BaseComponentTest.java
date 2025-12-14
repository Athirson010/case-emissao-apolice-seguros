package io.github.athirson010.componenttest.config;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for component tests using mocks.
 * Provides common configuration and mocked dependencies.
 *
 * All component tests should extend this class to inherit:
 * - Spring Boot test configuration
 * - Mocked external dependencies (MongoDB, Kafka, RabbitMQ)
 * - Common test setup
 *
 * This approach uses @MockBean to mock all external dependencies,
 * eliminating the need for Docker containers.
 */
@SpringBootTest(
    classes = BaseComponentTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public abstract class BaseComponentTest {

    /**
     * Test configuration that scans all application packages.
     * This configuration mimics the main application setup.
     */
    @Configuration
    @ComponentScan(basePackages = "io.github.athirson010")
    @EnableMongoRepositories(basePackages = "io.github.athirson010.adapters.out.persistence.mongo")
    static class TestConfig {
        // This inner configuration class provides test-specific Spring configuration
    }

    /**
     * Mocked MongoDB template - all database operations will be mocked
     */
    @MockBean
    protected MongoTemplate mongoTemplate;

    /**
     * Mocked Kafka template - all Kafka operations will be mocked
     */
    @MockBean
    protected KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Setup method executed before each test.
     * Resets all mocks to ensure test isolation.
     */
    @BeforeEach
    public void setUp() {
        // Reset all mocks before each test
        Mockito.reset(mongoTemplate, kafkaTemplate);
    }

    /**
     * Utility method to reset all mocks manually if needed
     */
    protected void resetAllMocks() {
        Mockito.reset(mongoTemplate, kafkaTemplate);
    }
}
