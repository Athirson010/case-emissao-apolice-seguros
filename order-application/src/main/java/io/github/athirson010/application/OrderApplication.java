package io.github.athirson010.application;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@OpenAPIDefinition(servers = {
        @Server(url = "/", description = "Default Server URL")
})
@SpringBootApplication(scanBasePackages = "io.github.athirson010")
@EnableMongoRepositories(basePackages = "io.github.athirson010.adapters.out.persistence.mongo")
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
