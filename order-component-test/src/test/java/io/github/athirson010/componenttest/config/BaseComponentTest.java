package io.github.athirson010.componenttest.config;

import com.mongodb.client.MongoClient;
import com.rabbitmq.client.ConnectionFactory;
import io.github.athirson010.adapters.out.persistence.mongo.repository.PolicyProposalMongoRepository;
import io.github.athirson010.application.OrderApplication;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    classes = OrderApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration"
    }
)
@ActiveProfiles({"test", "api"})
public abstract class BaseComponentTest {
    @MockBean
    protected MongoClient mongoClient;

    @MockBean
    protected MongoTemplate mongoTemplate;

    @MockBean
    protected PolicyProposalMongoRepository policyProposalMongoRepository;

    @MockBean
    protected KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    protected ConnectionFactory rabbitConnectionFactory;

    @MockBean(name = "rabbitTemplate")
    protected RabbitTemplate rabbitTemplate;

    @BeforeEach
    public void setUp() {
        Mockito.reset(
            mongoClient,
            mongoTemplate,
            policyProposalMongoRepository,
            kafkaTemplate,
            rabbitConnectionFactory,
            rabbitTemplate
        );
    }

    protected void resetAllMocks() {
        Mockito.reset(
            mongoClient,
            mongoTemplate,
            policyProposalMongoRepository,
            kafkaTemplate,
            rabbitConnectionFactory,
            rabbitTemplate
        );
    }
}
