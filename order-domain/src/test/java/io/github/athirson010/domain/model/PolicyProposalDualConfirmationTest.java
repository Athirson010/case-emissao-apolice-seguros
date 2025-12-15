package io.github.athirson010.domain.model;

import io.github.athirson010.domain.enums.Category;
import io.github.athirson010.domain.enums.PaymentMethod;
import io.github.athirson010.domain.enums.PolicyStatus;
import io.github.athirson010.domain.enums.SalesChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes para a nova lógica de Dual Confirmation:
 * - Policy só é APPROVED se AMBAS respostas (pagamento + subscrição) forem aprovadas
 * - Policy só é REJECTED se AMBAS respostas chegarem E pelo menos uma for rejeitada
 * - Policy permanece PENDING se apenas UMA resposta foi recebida
 */
@DisplayName("PolicyProposal - Dual Confirmation Logic Tests")
class PolicyProposalDualConfirmationTest {

    private PolicyProposal policyProposal;

    @BeforeEach
    void setUp() {
        policyProposal = PolicyProposal.create(
                UUID.randomUUID(),
                "PROD-AUTO-2024",
                Category.AUTO,
                SalesChannel.MOBILE,
                PaymentMethod.CREDIT_CARD,
                Money.brl(new BigDecimal("350.00")),
                Money.brl(new BigDecimal("200000.00")),
                Map.of("COLISAO", Money.brl(new BigDecimal("200000.00"))),
                List.of("GUINCHO_24H"),
                Instant.now()
        );

        // Preparar policy para estado PENDING
        policyProposal.validate(Instant.now());
        policyProposal.markAsPending(Instant.now());
    }

    // ==================================================
    // Testes: Recebendo APENAS resposta de PAGAMENTO
    // ==================================================

    @Test
    @DisplayName("Deve permanecer PENDING ao receber APENAS resposta de pagamento APROVADO")
    void devePermancerPendingAoReceberApenasPaymentApproved() {
        // When: Recebe apenas resposta de pagamento aprovado
        Instant now = Instant.now();
        policyProposal.processPaymentResponse(true, null, now);

        // Then: Deve permanecer PENDING (aguardando subscrição)
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.PENDING);
        assertThat(policyProposal.isPaymentConfirmed()).isTrue();
        assertThat(policyProposal.isPaymentResponseReceived()).isTrue();
        assertThat(policyProposal.isSubscriptionConfirmed()).isFalse();
        assertThat(policyProposal.isSubscriptionResponseReceived()).isFalse();
    }

    @Test
    @DisplayName("Deve permanecer PENDING ao receber APENAS resposta de pagamento REJEITADO")
    void devePermancerPendingAoReceberApenasPaymentRejected() {
        // When: Recebe apenas resposta de pagamento rejeitado
        Instant now = Instant.now();
        policyProposal.processPaymentResponse(false, "Fundos insuficientes", now);

        // Then: Deve permanecer PENDING (aguardando subscrição)
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.PENDING);
        assertThat(policyProposal.isPaymentConfirmed()).isFalse();
        assertThat(policyProposal.isPaymentResponseReceived()).isTrue();
        assertThat(policyProposal.getPaymentRejectionReason()).isEqualTo("Fundos insuficientes");
        assertThat(policyProposal.isSubscriptionResponseReceived()).isFalse();
    }

    // ==================================================
    // Testes: Recebendo APENAS resposta de SUBSCRIÇÃO
    // ==================================================

    @Test
    @DisplayName("Deve permanecer PENDING ao receber APENAS resposta de subscrição APROVADA")
    void devePermancerPendingAoReceberApenasSubscriptionApproved() {
        // When: Recebe apenas resposta de subscrição aprovada
        Instant now = Instant.now();
        policyProposal.processSubscriptionResponse(true, null, now);

        // Then: Deve permanecer PENDING (aguardando pagamento)
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.PENDING);
        assertThat(policyProposal.isSubscriptionConfirmed()).isTrue();
        assertThat(policyProposal.isSubscriptionResponseReceived()).isTrue();
        assertThat(policyProposal.isPaymentConfirmed()).isFalse();
        assertThat(policyProposal.isPaymentResponseReceived()).isFalse();
    }

    @Test
    @DisplayName("Deve permanecer PENDING ao receber APENAS resposta de subscrição REJEITADA")
    void devePermancerPendingAoReceberApenasSubscriptionRejected() {
        // When: Recebe apenas resposta de subscrição rejeitada
        Instant now = Instant.now();
        policyProposal.processSubscriptionResponse(false, "Alto risco", now);

        // Then: Deve permanecer PENDING (aguardando pagamento)
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.PENDING);
        assertThat(policyProposal.isSubscriptionConfirmed()).isFalse();
        assertThat(policyProposal.isSubscriptionResponseReceived()).isTrue();
        assertThat(policyProposal.getSubscriptionRejectionReason()).isEqualTo("Alto risco");
        assertThat(policyProposal.isPaymentResponseReceived()).isFalse();
    }

    // ==================================================
    // Testes: AMBAS respostas APROVADAS → APPROVED
    // ==================================================

    @Test
    @DisplayName("Deve APROVAR quando AMBAS respostas forem APROVADAS (pagamento primeiro)")
    void deveAprovarQuandoAmbasRespostasAprovadasPaymentFirst() {
        // Given: Recebe pagamento aprovado primeiro
        Instant time1 = Instant.now();
        policyProposal.processPaymentResponse(true, null, time1);
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.PENDING);

        // When: Recebe subscrição aprovada depois
        Instant time2 = Instant.now();
        policyProposal.processSubscriptionResponse(true, null, time2);

        // Then: Deve estar APPROVED
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.APPROVED);
        assertThat(policyProposal.isPaymentConfirmed()).isTrue();
        assertThat(policyProposal.isSubscriptionConfirmed()).isTrue();
        assertThat(policyProposal.getFinishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve APROVAR quando AMBAS respostas forem APROVADAS (subscrição primeiro)")
    void deveAprovarQuandoAmbasRespostasAprovadasSubscriptionFirst() {
        // Given: Recebe subscrição aprovada primeiro
        Instant time1 = Instant.now();
        policyProposal.processSubscriptionResponse(true, null, time1);
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.PENDING);

        // When: Recebe pagamento aprovado depois
        Instant time2 = Instant.now();
        policyProposal.processPaymentResponse(true, null, time2);

        // Then: Deve estar APPROVED
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.APPROVED);
        assertThat(policyProposal.isPaymentConfirmed()).isTrue();
        assertThat(policyProposal.isSubscriptionConfirmed()).isTrue();
        assertThat(policyProposal.getFinishedAt()).isNotNull();
    }

    // ==================================================
    // Testes: PELO MENOS UMA rejeitada → REJECTED
    // ==================================================

    @Test
    @DisplayName("Deve REJEITAR quando AMBAS respostas forem REJEITADAS")
    void deveRejeitarQuandoAmbasRespostasRejeitadas() {
        // Given: Recebe pagamento rejeitado
        policyProposal.processPaymentResponse(false, "Fundos insuficientes", Instant.now());

        // When: Recebe subscrição rejeitada
        policyProposal.processSubscriptionResponse(false, "Alto risco", Instant.now());

        // Then: Deve estar REJECTED
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(policyProposal.getFinishedAt()).isNotNull();

        // Verifica que o motivo inclui AMBAS rejeições
        HistoryEntry lastEntry = policyProposal.getHistory().get(policyProposal.getHistory().size() - 1);
        assertThat(lastEntry.reason())
                .contains("Pagamento rejeitado: Fundos insuficientes")
                .contains("Subscrição rejeitada: Alto risco");
    }

    @Test
    @DisplayName("Deve REJEITAR quando pagamento APROVADO mas subscrição REJEITADA")
    void deveRejeitarQuandoPaymentApprovedMasSubscriptionRejected() {
        // Given: Recebe pagamento aprovado
        policyProposal.processPaymentResponse(true, null, Instant.now());
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.PENDING);

        // When: Recebe subscrição rejeitada
        policyProposal.processSubscriptionResponse(false, "Alto risco", Instant.now());

        // Then: Deve estar REJECTED
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.REJECTED);

        // Verifica que o motivo menciona apenas a subscrição (pagamento foi OK)
        HistoryEntry lastEntry = policyProposal.getHistory().get(policyProposal.getHistory().size() - 1);
        assertThat(lastEntry.reason()).contains("Subscrição rejeitada: Alto risco");
    }

    @Test
    @DisplayName("Deve REJEITAR quando pagamento REJEITADO mas subscrição APROVADA")
    void deveRejeitarQuandoPaymentRejectedMasSubscriptionApproved() {
        // Given: Recebe pagamento rejeitado
        policyProposal.processPaymentResponse(false, "Cartão inválido", Instant.now());
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.PENDING);

        // When: Recebe subscrição aprovada
        policyProposal.processSubscriptionResponse(true, null, Instant.now());

        // Then: Deve estar REJECTED
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.REJECTED);

        // Verifica que o motivo menciona apenas o pagamento (subscrição foi OK)
        HistoryEntry lastEntry = policyProposal.getHistory().get(policyProposal.getHistory().size() - 1);
        assertThat(lastEntry.reason()).contains("Pagamento rejeitado: Cartão inválido");
    }

    // ==================================================
    // Testes: Validações e Edge Cases
    // ==================================================

    @Test
    @DisplayName("Deve lançar exceção ao tentar processar pagamento duas vezes")
    void deveLancarExcecaoAoProcessarPaymentDuasVezes() {
        // Given: Já processou pagamento
        policyProposal.processPaymentResponse(true, null, Instant.now());

        // When/Then: Tenta processar novamente
        assertThatThrownBy(() -> policyProposal.processPaymentResponse(true, null, Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Payment response already received");
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar processar subscrição duas vezes")
    void deveLancarExcecaoAoProcessarSubscriptionDuasVezes() {
        // Given: Já processou subscrição
        policyProposal.processSubscriptionResponse(true, null, Instant.now());

        // When/Then: Tenta processar novamente
        assertThatThrownBy(() -> policyProposal.processSubscriptionResponse(true, null, Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Subscription response already received");
    }

    @Test
    @DisplayName("Deve registrar histórico correto em fluxo completo de aprovação")
    void deveRegistrarHistoricoCorretoEmFluxoDeAprovacao() {
        // When: Fluxo completo
        policyProposal.processPaymentResponse(true, null, Instant.now());
        policyProposal.processSubscriptionResponse(true, null, Instant.now());

        // Then: Histórico deve ter RECEIVED → VALIDATED → PENDING → APPROVED
        assertThat(policyProposal.getHistory()).hasSize(4);
        assertThat(policyProposal.getHistory().get(0).status()).isEqualTo(PolicyStatus.RECEIVED);
        assertThat(policyProposal.getHistory().get(1).status()).isEqualTo(PolicyStatus.VALIDATED);
        assertThat(policyProposal.getHistory().get(2).status()).isEqualTo(PolicyStatus.PENDING);
        assertThat(policyProposal.getHistory().get(3).status()).isEqualTo(PolicyStatus.APPROVED);
    }

    @Test
    @DisplayName("Deve registrar histórico correto em fluxo completo de rejeição")
    void deveRegistrarHistoricoCorretoEmFluxoDeRejeicao() {
        // When: Fluxo com rejeição
        policyProposal.processPaymentResponse(false, "Erro", Instant.now());
        policyProposal.processSubscriptionResponse(true, null, Instant.now());

        // Then: Histórico deve ter RECEIVED → VALIDATED → PENDING → REJECTED
        assertThat(policyProposal.getHistory()).hasSize(4);
        assertThat(policyProposal.getHistory().get(3).status()).isEqualTo(PolicyStatus.REJECTED);
    }
}
