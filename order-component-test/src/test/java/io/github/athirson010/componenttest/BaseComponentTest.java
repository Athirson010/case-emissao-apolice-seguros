package io.github.athirson010.componenttest;

import com.mongodb.client.MongoClient;
import com.rabbitmq.client.ConnectionFactory;
import io.github.athirson010.adapters.out.persistence.mongo.repository.PolicyProposalMongoRepository;
import io.github.athirson010.application.OrderApplication;
import io.github.athirson010.core.port.out.FraudQueuePort;
import io.github.athirson010.core.port.out.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseComponentTest {

    // ==================== MongoDB Mocks ====================
    @MockBean
    protected MongoClient mongoClient;

    @MockBean
    protected MongoTemplate mongoTemplate;

    @MockBean
    protected PolicyProposalMongoRepository policyProposalMongoRepository;

    // ==================== Kafka Mocks ====================
    @MockBean
    protected KafkaTemplate<String, String> kafkaTemplate;

    // ==================== RabbitMQ Mocks ====================
    @MockBean
    protected ConnectionFactory rabbitConnectionFactory;

    @MockBean(name = "rabbitTemplate")
    protected RabbitTemplate rabbitTemplate;

    // ==================== Portas de Saída (Dependências Externas) ====================
    @MockBean
    protected OrderRepository orderRepository;

    @MockBean
    protected FraudQueuePort fraudQueuePort;

    @BeforeEach
    public void setUp() {
        resetAllMocks();
    }

    protected void resetAllMocks() {
        Mockito.reset(
                mongoClient,
                mongoTemplate,
                policyProposalMongoRepository,
                kafkaTemplate,
                rabbitConnectionFactory,
                rabbitTemplate,
                orderRepository,
                fraudQueuePort
        );
    }
}
