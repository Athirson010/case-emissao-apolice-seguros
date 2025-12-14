package io.github.athirson010.componenttest;

import io.github.athirson010.application.OrderApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"test","api","order-consumer"})
@ComponentScan(basePackages = "io.github.athirson010")
@EnableMongoRepositories(basePackages = "io.github.athirson010.adapters.out.persistence.mongo")
public class ComponentTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
