package io.github.athirson010.componenttest.ciclovida;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.athirson010.adapters.in.messaging.rabbitmq.InsuranceSubscriptionConfirmationConsumer;
import io.github.athirson010.adapters.in.messaging.rabbitmq.PaymentConfirmationConsumer;
import io.github.athirson010.componenttest.BaseComponentTest;
import io.github.athirson010.componenttest.templates.PaymentConfirmationEventBuilder;
import io.github.athirson010.componenttest.templates.PolicyRequestTemplateBuilder;
import io.github.athirson010.componenttest.templates.SubscriptionConfirmationEventBuilder;
import io.github.athirson010.core.port.out.OrderRepository;
import io.github.athirson010.domain.enums.PolicyStatus;
import io.github.athirson010.domain.model.PolicyProposal;
import io.github.athirson010.domain.model.PolicyProposalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de componente do ciclo de vida completo de uma apólice.
 * <p>
 * Cenários testados:
 * 1. Fluxo de sucesso: RECEIVED → VALIDATED → PENDING → APPROVED
 * 2. Rejeição por pagamento negado
 * 3. Rejeição por subscrição negada
 * 4. Cancelamento antes de finalização
 * 5. Validação de estados finais imutáveis
 * 6. Histórico completo de transições
 */
@ActiveProfiles({"test", "api", "order-consumer", "order-response-payment-consumer", "order-response-insurance-consumer"})
@DisplayName("Ciclo de Vida Completo da Apólice - Testes de Componente")
public class PolicyLifecycleComponentTest extends BaseComponentTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentConfirmationConsumer paymentConsumer;

    @Autowired
    private InsuranceSubscriptionConfirmationConsumer subscriptionConsumer;

    // Armazenamento em memória para simular o repository nos testes
    private final java.util.Map<PolicyProposalId, PolicyProposal> policyStorage = new java.util.HashMap<>();

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        policyStorage.clear();

        // Configurar mock do repository para armazenar e recuperar policies
        when(orderRepository.save(any(PolicyProposal.class)))
                .thenAnswer(invocation -> {
                    PolicyProposal policy = invocation.getArgument(0);
                    policyStorage.put(policy.getId(), policy);
                    return policy;
                });

        when(orderRepository.findById(any(PolicyProposalId.class)))
                .thenAnswer(invocation -> {
                    PolicyProposalId id = invocation.getArgument(0);
                    return java.util.Optional.ofNullable(policyStorage.get(id));
                });
    }

    @Test
    @DisplayName("Deve completar fluxo de sucesso: RECEIVED → VALIDATED → PENDING → APPROVED")
    void deveCompletarFluxoDeSucessoCompleto() throws Exception {
        // Given: Criar uma solicitação de apólice AUTO regular
        String policyJson = PolicyRequestTemplateBuilder.autoRegular().buildAsJson();

        // When: Criar a apólice via API
        MvcResult createResult = mockMvc.perform(post("/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.policy_request_id").exists())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String policyId = objectMapper.readTree(responseBody).get("policy_request_id").asText();

        // Then: Verificar que foi criada com status RECEIVED
        PolicyProposal policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.RECEIVED);
        assertThat(policy.getHistory()).hasSize(1);

        // When: Simular validação e transição para PENDING
        policy.validate(java.time.Instant.now());
        policy.markAsPending(java.time.Instant.now());
        orderRepository.save(policy);

        // When: Receber confirmação de pagamento
        String paymentEvent = PaymentConfirmationEventBuilder.approved(policyId).buildAsJson();
        paymentConsumer.consumePaymentConfirmation(paymentEvent);

        // Then: Policy ainda deve estar PENDING (aguardando subscrição)
        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.PENDING);
        assertThat(policy.isPaymentConfirmed()).isTrue();
        assertThat(policy.isSubscriptionConfirmed()).isFalse();

        // When: Receber confirmação de subscrição
        String subscriptionEvent = SubscriptionConfirmationEventBuilder.approved(policyId).buildAsJson();
        subscriptionConsumer.consumeInsuranceSubscriptionConfirmation(subscriptionEvent);

        // Then: Policy deve estar APPROVED
        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.APPROVED);
        assertThat(policy.isPaymentConfirmed()).isTrue();
        assertThat(policy.isSubscriptionConfirmed()).isTrue();
        assertThat(policy.getFinishedAt()).isNotNull();

        // Then: Histórico deve conter todas as transições
        assertThat(policy.getHistory()).hasSize(4);
        assertThat(policy.getHistory().get(0).status()).isEqualTo(PolicyStatus.RECEIVED);
        assertThat(policy.getHistory().get(1).status()).isEqualTo(PolicyStatus.VALIDATED);
        assertThat(policy.getHistory().get(2).status()).isEqualTo(PolicyStatus.PENDING);
        assertThat(policy.getHistory().get(3).status()).isEqualTo(PolicyStatus.APPROVED);
    }

    @Test
    @DisplayName("Deve rejeitar apólice IMEDIATAMENTE quando pagamento é negado")
    void deveRejeitarApoliceImediatamenteQuandoPagamentoNegado() throws Exception {
        // Given: Criar uma solicitação de apólice
        String policyJson = PolicyRequestTemplateBuilder.autoRegular().buildAsJson();

        // When: Criar a apólice
        MvcResult createResult = mockMvc.perform(post("/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String policyId = objectMapper.readTree(responseBody).get("policy_request_id").asText();

        // Verificar que foi salva no banco
        PolicyProposal savedPolicy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(savedPolicy).isNotNull();
        assertThat(savedPolicy.getStatus()).isEqualTo(PolicyStatus.RECEIVED);

        // When: Validar e marcar como PENDING
        PolicyProposal policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        policy.validate(java.time.Instant.now());
        policy.markAsPending(java.time.Instant.now());
        orderRepository.save(policy);

        // When: Receber rejeição de pagamento (REJEIÇÃO IMEDIATA)
        String paymentEvent = PaymentConfirmationEventBuilder
                .rejectedInsufficientFunds(policyId)
                .buildAsJson();
        paymentConsumer.consumePaymentConfirmation(paymentEvent);

        // Then: Policy deve estar REJECTED imediatamente (não aguarda subscrição)
        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(policy.getFinishedAt()).isNotNull();
        assertThat(policy.isPaymentResponseReceived()).isTrue();
        assertThat(policy.isSubscriptionResponseReceived()).isFalse();

        // Then: Histórico deve conter o motivo da rejeição
        assertThat(policy.getHistory().get(policy.getHistory().size() - 1).reason())
                .contains("Pagamento rejeitado");

        // When: Agora subscrição chega (após já estar REJECTED)
        String subscriptionEvent = SubscriptionConfirmationEventBuilder.approved(policyId).buildAsJson();
        subscriptionConsumer.consumeInsuranceSubscriptionConfirmation(subscriptionEvent);

        // Then: Deve permanecer REJECTED e adicionar entrada no histórico
        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(policy.isSubscriptionResponseReceived()).isTrue();

        // Verificar que histórico tem ambas as respostas
        assertThat(policy.getHistory()).hasSizeGreaterThanOrEqualTo(5);
        assertThat(policy.getHistory().get(policy.getHistory().size() - 1).reason())
                .contains("Subscrição aprovada (após rejeição por pagamento)");
    }

    @Test
    @DisplayName("Deve rejeitar apólice IMEDIATAMENTE quando subscrição é negada")
    void deveRejeitarApoliceImediatamenteQuandoSubscricaoNegada() throws Exception {
        // Given: Criar uma solicitação de apólice
        String policyJson = PolicyRequestTemplateBuilder.autoRegular().buildAsJson();

        // When: Criar a apólice
        MvcResult createResult = mockMvc.perform(post("/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String policyId = objectMapper.readTree(responseBody).get("policy_request_id").asText();

        // Verificar que foi salva no banco
        PolicyProposal savedPolicy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(savedPolicy).isNotNull();

        // When: Validar e marcar como PENDING
        PolicyProposal policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        policy.validate(java.time.Instant.now());
        policy.markAsPending(java.time.Instant.now());
        orderRepository.save(policy);

        // When: Receber aprovação de pagamento primeiro
        String paymentEvent = PaymentConfirmationEventBuilder.approved(policyId).buildAsJson();
        paymentConsumer.consumePaymentConfirmation(paymentEvent);

        // Then: Ainda PENDING (aguardando subscrição)
        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.PENDING);

        // When: Receber rejeição de subscrição (REJEIÇÃO IMEDIATA)
        String subscriptionEvent = SubscriptionConfirmationEventBuilder
                .rejectedHighRisk(policyId)
                .buildAsJson();
        subscriptionConsumer.consumeInsuranceSubscriptionConfirmation(subscriptionEvent);

        // Then: Policy deve estar REJECTED imediatamente
        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(policy.getFinishedAt()).isNotNull();
        assertThat(policy.isPaymentResponseReceived()).isTrue();
        assertThat(policy.isSubscriptionResponseReceived()).isTrue();

        // Then: Histórico deve conter o motivo da rejeição
        assertThat(policy.getHistory().get(policy.getHistory().size() - 1).reason())
                .contains("Subscrição rejeitada");
    }

    @Test
    @DisplayName("Deve permitir cancelamento de apólice em estado RECEIVED")
    void devePermitirCancelamentoEmEstadoReceived() throws Exception {
        // Given: Criar uma apólice
        String policyJson = PolicyRequestTemplateBuilder.autoRegular().buildAsJson();

        MvcResult createResult = mockMvc.perform(post("/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String policyId = objectMapper.readTree(responseBody).get("policy_request_id").asText();

        // Verificar que está em RECEIVED
        PolicyProposal policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.RECEIVED);

        // When: Cancelar a apólice
        String cancelRequest = """
                {
                    "reason": "Cliente desistiu da contratação"
                }
                """;

        mockMvc.perform(post("/policies/" + policyId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policy_request_id").value(policyId))
                .andExpect(jsonPath("$.status").value("CANCELED"));

        // Then: Verificar que foi cancelada
        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.CANCELED);
        assertThat(policy.getFinishedAt()).isNotNull();
        assertThat(policy.getHistory().get(policy.getHistory().size() - 1).reason())
                .contains("Cliente desistiu da contratação");
    }

    @Test
    @DisplayName("Deve permitir cancelamento de apólice em estado VALIDATED")
    void devePermitirCancelamentoEmEstadoValidated() throws Exception {
        // Given: Criar e validar uma apólice
        String policyJson = PolicyRequestTemplateBuilder.autoRegular().buildAsJson();

        MvcResult createResult = mockMvc.perform(post("/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String policyId = objectMapper.readTree(responseBody).get("policy_request_id").asText();

        // Validar a apólice
        PolicyProposal policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        policy.validate(java.time.Instant.now());
        orderRepository.save(policy);

        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.VALIDATED);

        // When: Cancelar a apólice
        String cancelRequest = """
                {
                    "reason": "Informações incorretas fornecidas"
                }
                """;

        mockMvc.perform(post("/policies/" + policyId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        // Then: Verificar cancelamento
        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.CANCELED);
        assertThat(policy.getFinishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve permitir cancelamento de apólice em estado PENDING")
    void devePermitirCancelamentoEmEstadoPending() throws Exception {
        // Given: Criar apólice e marcar como PENDING
        String policyJson = PolicyRequestTemplateBuilder.autoRegular().buildAsJson();

        MvcResult createResult = mockMvc.perform(post("/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String policyId = objectMapper.readTree(responseBody).get("policy_request_id").asText();

        // Colocar em PENDING
        PolicyProposal policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        policy.validate(java.time.Instant.now());
        policy.markAsPending(java.time.Instant.now());
        orderRepository.save(policy);

        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.PENDING);

        // When: Cancelar a apólice
        String cancelRequest = """
                {
                    "reason": "Processo de aprovação muito demorado"
                }
                """;

        mockMvc.perform(post("/policies/" + policyId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        // Then: Verificar cancelamento
        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.CANCELED);
        assertThat(policy.getFinishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Não deve permitir cancelamento de apólice APROVADA")
    void naoDevePermitirCancelamentoDeApoliceAprovada() throws Exception {
        // Given: Criar e aprovar uma apólice
        String policyJson = PolicyRequestTemplateBuilder.autoRegular().buildAsJson();

        MvcResult createResult = mockMvc.perform(post("/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String policyId = objectMapper.readTree(responseBody).get("policy_request_id").asText();

        // Aprovar a apólice
        PolicyProposal policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        policy.validate(java.time.Instant.now());
        policy.markAsPending(java.time.Instant.now());
        orderRepository.save(policy);

        String paymentEvent = PaymentConfirmationEventBuilder.approved(policyId).buildAsJson();
        paymentConsumer.consumePaymentConfirmation(paymentEvent);

        String subscriptionEvent = SubscriptionConfirmationEventBuilder.approved(policyId).buildAsJson();
        subscriptionConsumer.consumeInsuranceSubscriptionConfirmation(subscriptionEvent);

        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.APPROVED);

        // When/Then: Tentar cancelar deve falhar
        String cancelRequest = """
                {
                    "reason": "Tentativa de cancelamento"
                }
                """;

        mockMvc.perform(post("/policies/" + policyId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelRequest))
                .andExpect(status().isBadRequest());

        // Verificar que continua APPROVED
        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.APPROVED);
    }

    @Test
    @DisplayName("Não deve permitir cancelamento de apólice REJEITADA")
    void naoDevePermitirCancelamentoDeApoliceRejeitada() throws Exception {
        // Given: Criar e rejeitar uma apólice
        String policyJson = PolicyRequestTemplateBuilder.autoRegular().buildAsJson();

        MvcResult createResult = mockMvc.perform(post("/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String policyId = objectMapper.readTree(responseBody).get("policy_request_id").asText();

        // Rejeitar a apólice
        PolicyProposal policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        policy.validate(java.time.Instant.now());
        policy.markAsPending(java.time.Instant.now());
        orderRepository.save(policy);

        String paymentEvent = PaymentConfirmationEventBuilder.rejectedInsufficientFunds(policyId).buildAsJson();
        paymentConsumer.consumePaymentConfirmation(paymentEvent);

        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.REJECTED);

        // When/Then: Tentar cancelar deve falhar
        String cancelRequest = """
                {
                    "reason": "Tentativa de cancelamento"
                }
                """;

        mockMvc.perform(post("/policies/" + policyId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelRequest))
                .andExpect(status().isBadRequest());

        // Verificar que continua REJECTED
        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.REJECTED);
    }

    @Test
    @DisplayName("Não deve permitir cancelar apólice já CANCELADA")
    void naoDevePermitirCancelarApoliceCancelada() throws Exception {
        // Given: Criar e cancelar uma apólice
        String policyJson = PolicyRequestTemplateBuilder.autoRegular().buildAsJson();

        MvcResult createResult = mockMvc.perform(post("/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String policyId = objectMapper.readTree(responseBody).get("policy_request_id").asText();

        // Cancelar pela primeira vez
        String cancelRequest1 = """
                {
                    "reason": "Primeiro cancelamento"
                }
                """;

        mockMvc.perform(post("/policies/" + policyId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelRequest1))
                .andExpect(status().isOk());

        PolicyProposal policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.CANCELED);

        // When/Then: Tentar cancelar novamente deve falhar
        String cancelRequest2 = """
                {
                    "reason": "Segundo cancelamento"
                }
                """;

        mockMvc.perform(post("/policies/" + policyId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelRequest2))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 404 ao tentar cancelar apólice inexistente")
    void deveRetornar404AoCancelarApoliceInexistente() throws Exception {
        // Given: ID de apólice que não existe
        String nonExistentId = "00000000-0000-0000-0000-000000000000";

        // When/Then: Tentar cancelar deve retornar 404
        String cancelRequest = """
                {
                    "reason": "Tentativa de cancelamento"
                }
                """;

        mockMvc.perform(post("/policies/" + nonExistentId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelRequest))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve registrar histórico completo de todas as transições")
    void deveRegistrarHistoricoCompletoDeTodasAsTransicoes() throws Exception {
        // Given: Criar uma apólice
        String policyJson = PolicyRequestTemplateBuilder.autoRegular().buildAsJson();

        MvcResult createResult = mockMvc.perform(post("/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String policyId = objectMapper.readTree(responseBody).get("policy_request_id").asText();

        // When: Executar todas as transições
        PolicyProposal policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();

        java.time.Instant time1 = java.time.Instant.now();
        policy.validate(time1);

        java.time.Instant time2 = java.time.Instant.now().plusMillis(100);
        policy.markAsPending(time2);
        orderRepository.save(policy);

        String paymentEvent = PaymentConfirmationEventBuilder.approved(policyId).buildAsJson();
        paymentConsumer.consumePaymentConfirmation(paymentEvent);

        String subscriptionEvent = SubscriptionConfirmationEventBuilder.approved(policyId).buildAsJson();
        subscriptionConsumer.consumeInsuranceSubscriptionConfirmation(subscriptionEvent);

        // Then: Verificar histórico completo
        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();

        assertThat(policy.getHistory()).hasSize(4);

        // Verificar que cada transição foi registrada
        assertThat(policy.getHistory().get(0).status()).isEqualTo(PolicyStatus.RECEIVED);
        assertThat(policy.getHistory().get(1).status()).isEqualTo(PolicyStatus.VALIDATED);
        assertThat(policy.getHistory().get(2).status()).isEqualTo(PolicyStatus.PENDING);
        assertThat(policy.getHistory().get(3).status()).isEqualTo(PolicyStatus.APPROVED);
    }

    @Test
    @DisplayName("Deve aprovar apenas quando AMBAS respostas forem positivas")
    void deveAprovarApenasQuandoAmbasRespostasForemPositivas() throws Exception {
        // Given: Criar uma apólice
        String policyJson = PolicyRequestTemplateBuilder.autoRegular().buildAsJson();

        MvcResult createResult = mockMvc.perform(post("/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String policyId = objectMapper.readTree(responseBody).get("policy_request_id").asText();

        // When: Validar e marcar como PENDING
        PolicyProposal policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        policy.validate(java.time.Instant.now());
        policy.markAsPending(java.time.Instant.now());
        orderRepository.save(policy);

        // When: Receber apenas confirmação de subscrição (sem pagamento)
        String subscriptionEvent = SubscriptionConfirmationEventBuilder.approved(policyId).buildAsJson();
        subscriptionConsumer.consumeInsuranceSubscriptionConfirmation(subscriptionEvent);

        // Then: Deve permanecer PENDING
        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.PENDING);
        assertThat(policy.isSubscriptionConfirmed()).isTrue();
        assertThat(policy.isPaymentConfirmed()).isFalse();

        // When: Agora receber confirmação de pagamento
        String paymentEvent = PaymentConfirmationEventBuilder.approved(policyId).buildAsJson();
        paymentConsumer.consumePaymentConfirmation(paymentEvent);

        // Then: Agora deve estar APPROVED
        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.APPROVED);
        assertThat(policy.isPaymentConfirmed()).isTrue();
        assertThat(policy.isSubscriptionConfirmed()).isTrue();
    }

    @Test
    @DisplayName("Deve registrar histórico completo quando AMBAS respostas forem rejeitadas")
    void deveRegistrarHistoricoCompletoQuandoAmbasRejeitadas() throws Exception {
        // Given: Criar uma apólice
        String policyJson = PolicyRequestTemplateBuilder.autoRegular().buildAsJson();

        MvcResult createResult = mockMvc.perform(post("/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String policyId = objectMapper.readTree(responseBody).get("policy_request_id").asText();

        // Verificar persistência no banco
        PolicyProposal savedPolicy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(savedPolicy).isNotNull();
        assertThat(savedPolicy.getStatus()).isEqualTo(PolicyStatus.RECEIVED);

        // When: Validar e marcar como PENDING
        PolicyProposal policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        policy.validate(java.time.Instant.now());
        policy.markAsPending(java.time.Instant.now());
        orderRepository.save(policy);

        // When: Pagamento rejeitado primeiro (REJEIÇÃO IMEDIATA)
        String paymentEvent = PaymentConfirmationEventBuilder
                .rejectedInsufficientFunds(policyId)
                .buildAsJson();
        paymentConsumer.consumePaymentConfirmation(paymentEvent);

        // Then: Deve estar REJECTED
        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.REJECTED);

        // When: Subscrição também rejeitada (adiciona ao histórico)
        String subscriptionEvent = SubscriptionConfirmationEventBuilder
                .rejectedHighRisk(policyId)
                .buildAsJson();
        subscriptionConsumer.consumeInsuranceSubscriptionConfirmation(subscriptionEvent);

        // Then: Deve permanecer REJECTED
        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(policy.isPaymentResponseReceived()).isTrue();
        assertThat(policy.isSubscriptionResponseReceived()).isTrue();

        // Then: Histórico deve ter AMBAS as rejeições
        // RECEIVED, VALIDATED, PENDING, REJECTED (payment), REJECTED (subscription)
        assertThat(policy.getHistory()).hasSizeGreaterThanOrEqualTo(5);

        // Verificar que tem o motivo do pagamento
        boolean hasPaymentRejection = policy.getHistory().stream()
                .anyMatch(entry -> entry.reason() != null && entry.reason().contains("Pagamento rejeitado"));
        assertThat(hasPaymentRejection).isTrue();

        // Verificar que tem o motivo da subscrição
        boolean hasSubscriptionRejection = policy.getHistory().stream()
                .anyMatch(entry -> entry.reason() != null && entry.reason().contains("Subscrição rejeitada"));
        assertThat(hasSubscriptionRejection).isTrue();
    }
}
