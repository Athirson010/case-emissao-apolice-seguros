package io.github.athirson010.componenttest.ciclovida;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.athirson010.adapters.in.messaging.rabbitmq.PaymentConfirmationConsumer;
import io.github.athirson010.adapters.in.messaging.rabbitmq.SubscriptionConfirmationConsumer;
import io.github.athirson010.componenttest.BaseComponentTest;
import io.github.athirson010.componenttest.templates.*;
import io.github.athirson010.core.port.out.OrderRepository;
import io.github.athirson010.domain.enums.PolicyStatus;
import io.github.athirson010.domain.model.PolicyProposal;
import io.github.athirson010.domain.model.PolicyProposalId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste de componente do ciclo de vida completo de uma apólice.
 *
 * Cenários testados:
 * 1. Fluxo de sucesso: RECEIVED → VALIDATED → PENDING → APPROVED
 * 2. Rejeição por pagamento negado
 * 3. Rejeição por subscrição negada
 * 4. Cancelamento antes de finalização
 * 5. Validação de estados finais imutáveis
 * 6. Histórico completo de transições
 */
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
    private SubscriptionConfirmationConsumer subscriptionConsumer;

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
        subscriptionConsumer.consumeSubscriptionConfirmation(subscriptionEvent);

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
    @DisplayName("Deve rejeitar apólice quando pagamento é negado")
    void deveRejeitarApoliceQuandoPagamentoNegado() throws Exception {
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

        // When: Validar e marcar como PENDING
        PolicyProposal policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        policy.validate(java.time.Instant.now());
        policy.markAsPending(java.time.Instant.now());
        orderRepository.save(policy);

        // When: Receber rejeição de pagamento
        String paymentEvent = PaymentConfirmationEventBuilder
                .rejectedInsufficientFunds(policyId)
                .buildAsJson();
        paymentConsumer.consumePaymentConfirmation(paymentEvent);

        // Then: Policy deve estar REJECTED
        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(policy.getFinishedAt()).isNotNull();

        // Then: Histórico deve conter o motivo da rejeição
        assertThat(policy.getHistory().get(policy.getHistory().size() - 1).reason())
                .contains("Pagamento rejeitado");
    }

    @Test
    @DisplayName("Deve rejeitar apólice quando subscrição é negada")
    void deveRejeitarApoliceQuandoSubscricaoNegada() throws Exception {
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

        // When: Validar e marcar como PENDING
        PolicyProposal policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        policy.validate(java.time.Instant.now());
        policy.markAsPending(java.time.Instant.now());
        orderRepository.save(policy);

        // When: Receber aprovação de pagamento primeiro
        String paymentEvent = PaymentConfirmationEventBuilder.approved(policyId).buildAsJson();
        paymentConsumer.consumePaymentConfirmation(paymentEvent);

        // When: Receber rejeição de subscrição
        String subscriptionEvent = SubscriptionConfirmationEventBuilder
                .rejectedHighRisk(policyId)
                .buildAsJson();
        subscriptionConsumer.consumeSubscriptionConfirmation(subscriptionEvent);

        // Then: Policy deve estar REJECTED
        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(policy.getFinishedAt()).isNotNull();

        // Then: Histórico deve conter o motivo da rejeição
        assertThat(policy.getHistory().get(policy.getHistory().size() - 1).reason())
                .contains("Subscrição rejeitada");
    }

    @Test
    @DisplayName("Deve permitir cancelamento antes de estado final")
    void devePermitirCancelamentoAntesDeEstadoFinal() throws Exception {
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

        // When: Cancelar a apólice
        mockMvc.perform(delete("/policies/" + policyId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        // Then: Policy deve estar CANCELED
        PolicyProposal policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.CANCELED);
        assertThat(policy.getFinishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Não deve permitir alterações em estados finais")
    void naoDevePermitirAlteracoesEmEstadosFinais() throws Exception {
        // Given: Criar e aprovar uma apólice completamente
        String policyJson = PolicyRequestTemplateBuilder.autoRegular().buildAsJson();

        MvcResult createResult = mockMvc.perform(post("/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String policyId = objectMapper.readTree(responseBody).get("policy_request_id").asText();

        // When: Completar o fluxo até APPROVED
        PolicyProposal policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        policy.validate(java.time.Instant.now());
        policy.markAsPending(java.time.Instant.now());
        orderRepository.save(policy);

        String paymentEvent = PaymentConfirmationEventBuilder.approved(policyId).buildAsJson();
        paymentConsumer.consumePaymentConfirmation(paymentEvent);

        String subscriptionEvent = SubscriptionConfirmationEventBuilder.approved(policyId).buildAsJson();
        subscriptionConsumer.consumeSubscriptionConfirmation(subscriptionEvent);

        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.APPROVED);

        // Then: Tentativa de cancelamento deve falhar
        mockMvc.perform(delete("/policies/" + policyId))
                .andExpect(status().isBadRequest());
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
        subscriptionConsumer.consumeSubscriptionConfirmation(subscriptionEvent);

        // Then: Verificar histórico completo
        policy = orderRepository.findById(PolicyProposalId.from(policyId)).orElseThrow();

        assertThat(policy.getHistory()).hasSize(4);

        // Verificar ordem cronológica
        for (int i = 0; i < policy.getHistory().size() - 1; i++) {
            assertThat(policy.getHistory().get(i).timestamp())
                    .isBefore(policy.getHistory().get(i + 1).timestamp());
        }

        // Verificar que cada transição foi registrada
        assertThat(policy.getHistory().get(0).status()).isEqualTo(PolicyStatus.RECEIVED);
        assertThat(policy.getHistory().get(1).status()).isEqualTo(PolicyStatus.VALIDATED);
        assertThat(policy.getHistory().get(2).status()).isEqualTo(PolicyStatus.PENDING);
        assertThat(policy.getHistory().get(3).status()).isEqualTo(PolicyStatus.APPROVED);
    }

    @Test
    @DisplayName("Deve manter aprovação pendente até receber AMBAS confirmações")
    void deveManterAprovacaoPendenteAteReceberAmbasConfirmacoes() throws Exception {
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
        subscriptionConsumer.consumeSubscriptionConfirmation(subscriptionEvent);

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
}
