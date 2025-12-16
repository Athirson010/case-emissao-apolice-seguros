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
 * Testes para a nova lógica de Rejeição Imediata com Histórico Completo:
 * - Policy é REJECTED imediatamente se qualquer resposta (pagamento OU subscrição) for rejeitada
 * - Policy só é APPROVED se AMBAS respostas forem aprovadas
 * - Mesmo após REJECTED, a segunda resposta é registrada no histórico
 * - Histórico sempre contém resultado de AMBAS as respostas
 */
@DisplayName("PolicyProposal - Immediate Rejection with Complete History Tests")
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
    @DisplayName("Deve REJEITAR IMEDIATAMENTE ao receber resposta de pagamento REJEITADO")
    void deveRejeitarImediatamenteAoReceberPaymentRejected() {
        // When: Recebe resposta de pagamento rejeitado
        Instant now = Instant.now();
        policyProposal.processPaymentResponse(false, "Fundos insuficientes", now);

        // Then: Deve estar REJECTED imediatamente (não aguarda subscrição)
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(policyProposal.isPaymentConfirmed()).isFalse();
        assertThat(policyProposal.isPaymentResponseReceived()).isTrue();
        assertThat(policyProposal.getPaymentRejectionReason()).isEqualTo("Fundos insuficientes");
        assertThat(policyProposal.isSubscriptionResponseReceived()).isFalse();
        assertThat(policyProposal.getFinishedAt()).isNotNull();

        // Verifica histórico
        HistoryEntry lastEntry = policyProposal.getHistory().get(policyProposal.getHistory().size() - 1);
        assertThat(lastEntry.status()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(lastEntry.reason()).contains("Pagamento rejeitado: Fundos insuficientes");
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
    @DisplayName("Deve REJEITAR IMEDIATAMENTE ao receber resposta de subscrição REJEITADA")
    void deveRejeitarImediatamenteAoReceberSubscriptionRejected() {
        // When: Recebe resposta de subscrição rejeitada
        Instant now = Instant.now();
        policyProposal.processSubscriptionResponse(false, "Alto risco", now);

        // Then: Deve estar REJECTED imediatamente (não aguarda pagamento)
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(policyProposal.isSubscriptionConfirmed()).isFalse();
        assertThat(policyProposal.isSubscriptionResponseReceived()).isTrue();
        assertThat(policyProposal.getSubscriptionRejectionReason()).isEqualTo("Alto risco");
        assertThat(policyProposal.isPaymentResponseReceived()).isFalse();
        assertThat(policyProposal.getFinishedAt()).isNotNull();

        // Verifica histórico
        HistoryEntry lastEntry = policyProposal.getHistory().get(policyProposal.getHistory().size() - 1);
        assertThat(lastEntry.status()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(lastEntry.reason()).contains("Subscrição rejeitada: Alto risco");
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
    @DisplayName("Deve REJEITAR na primeira e registrar segunda no histórico quando AMBAS forem REJEITADAS")
    void deveRejeitarNaPrimeiraERegistrarSegundaQuandoAmbasRejeitadas() {
        // Given: Recebe pagamento rejeitado (REJEITA IMEDIATAMENTE)
        Instant time1 = Instant.now();
        policyProposal.processPaymentResponse(false, "Fundos insuficientes", time1);
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.REJECTED);

        // When: Recebe subscrição rejeitada (já está REJECTED, apenas adiciona ao histórico)
        Instant time2 = Instant.now();
        policyProposal.processSubscriptionResponse(false, "Alto risco", time2);

        // Then: Deve permanecer REJECTED
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(policyProposal.getFinishedAt()).isNotNull();
        assertThat(policyProposal.isPaymentResponseReceived()).isTrue();
        assertThat(policyProposal.isSubscriptionResponseReceived()).isTrue();

        // Verifica que o histórico tem 2 entradas de REJECTED
        // Histórico: RECEIVED, VALIDATED, PENDING, REJECTED (payment), REJECTED (subscription)
        assertThat(policyProposal.getHistory()).hasSizeGreaterThanOrEqualTo(5);

        // Penúltima entrada: rejeição por pagamento
        HistoryEntry paymentRejection = policyProposal.getHistory().get(policyProposal.getHistory().size() - 2);
        assertThat(paymentRejection.status()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(paymentRejection.reason()).contains("Pagamento rejeitado: Fundos insuficientes");

        // Última entrada: subscrição rejeitada após já estar rejected
        HistoryEntry subscriptionRejection = policyProposal.getHistory().get(policyProposal.getHistory().size() - 1);
        assertThat(subscriptionRejection.status()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(subscriptionRejection.reason()).contains("Subscrição rejeitada: Alto risco");
    }

    @Test
    @DisplayName("Deve REJEITAR quando pagamento APROVADO mas subscrição REJEITADA")
    void deveRejeitarQuandoPaymentApprovedMasSubscriptionRejected() {
        // Given: Recebe pagamento aprovado (permanece PENDING)
        policyProposal.processPaymentResponse(true, null, Instant.now());
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.PENDING);

        // When: Recebe subscrição rejeitada (REJEITA IMEDIATAMENTE)
        policyProposal.processSubscriptionResponse(false, "Alto risco", Instant.now());

        // Then: Deve estar REJECTED
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(policyProposal.isPaymentConfirmed()).isTrue();
        assertThat(policyProposal.isSubscriptionConfirmed()).isFalse();

        // Verifica que o motivo menciona apenas a subscrição (pagamento foi OK)
        HistoryEntry lastEntry = policyProposal.getHistory().get(policyProposal.getHistory().size() - 1);
        assertThat(lastEntry.status()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(lastEntry.reason()).contains("Subscrição rejeitada: Alto risco");
    }

    @Test
    @DisplayName("Deve REJEITAR quando pagamento REJEITADO mas subscrição APROVADA")
    void deveRejeitarQuandoPaymentRejectedMasSubscriptionApproved() {
        // Given: Recebe pagamento rejeitado (REJEITA IMEDIATAMENTE)
        policyProposal.processPaymentResponse(false, "Cartão inválido", Instant.now());
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.REJECTED);

        // When: Recebe subscrição aprovada (já está REJECTED, adiciona ao histórico)
        policyProposal.processSubscriptionResponse(true, null, Instant.now());

        // Then: Deve permanecer REJECTED
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(policyProposal.isPaymentConfirmed()).isFalse();
        assertThat(policyProposal.isSubscriptionConfirmed()).isTrue();

        // Verifica histórico: deve ter entrada de pagamento rejeitado
        HistoryEntry paymentRejection = policyProposal.getHistory().get(policyProposal.getHistory().size() - 2);
        assertThat(paymentRejection.reason()).contains("Pagamento rejeitado: Cartão inválido");

        // E deve ter entrada de subscrição aprovada após rejeição
        HistoryEntry subscriptionApproval = policyProposal.getHistory().get(policyProposal.getHistory().size() - 1);
        assertThat(subscriptionApproval.status()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(subscriptionApproval.reason()).contains("Subscrição aprovada (após rejeição por pagamento)");
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
        // When: Fluxo com rejeição (payment rejeitado, subscription aprovada)
        policyProposal.processPaymentResponse(false, "Erro no pagamento", Instant.now());
        policyProposal.processSubscriptionResponse(true, null, Instant.now());

        // Then: Histórico deve ter RECEIVED → VALIDATED → PENDING → REJECTED (payment) → REJECTED (subscription approved)
        assertThat(policyProposal.getHistory()).hasSize(5);
        assertThat(policyProposal.getHistory().get(0).status()).isEqualTo(PolicyStatus.RECEIVED);
        assertThat(policyProposal.getHistory().get(1).status()).isEqualTo(PolicyStatus.VALIDATED);
        assertThat(policyProposal.getHistory().get(2).status()).isEqualTo(PolicyStatus.PENDING);
        assertThat(policyProposal.getHistory().get(3).status()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(policyProposal.getHistory().get(3).reason()).contains("Pagamento rejeitado");
        assertThat(policyProposal.getHistory().get(4).status()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(policyProposal.getHistory().get(4).reason()).contains("Subscrição aprovada (após rejeição");
    }

    // ==================================================
    // Testes: Histórico completo com subscription rejeitada primeiro
    // ==================================================

    @Test
    @DisplayName("Deve registrar histórico completo quando subscription rejeitada primeiro e payment aprovado depois")
    void deveRegistrarHistoricoCompletoSubscriptionRejeitadaFirst() {
        // When: Subscription rejeitada primeiro (REJEITA IMEDIATAMENTE)
        policyProposal.processSubscriptionResponse(false, "Cliente de alto risco", Instant.now());
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.REJECTED);

        // Then: Payment aprovado depois (adiciona ao histórico)
        policyProposal.processPaymentResponse(true, null, Instant.now());

        // Verifica histórico completo
        assertThat(policyProposal.getHistory()).hasSize(5);

        // Penúltima entrada: subscrição rejeitada
        HistoryEntry subscriptionRejection = policyProposal.getHistory().get(3);
        assertThat(subscriptionRejection.status()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(subscriptionRejection.reason()).contains("Subscrição rejeitada: Cliente de alto risco");

        // Última entrada: pagamento aprovado após rejeição
        HistoryEntry paymentApproval = policyProposal.getHistory().get(4);
        assertThat(paymentApproval.status()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(paymentApproval.reason()).contains("Pagamento aprovado (após rejeição por subscrição)");
    }

    @Test
    @DisplayName("Deve registrar histórico completo quando subscription rejeitada primeiro e payment rejeitado depois")
    void deveRegistrarHistoricoCompletoAmbosRejeitadosSubscriptionFirst() {
        // When: Subscription rejeitada primeiro
        policyProposal.processSubscriptionResponse(false, "Alto risco", Instant.now());
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.REJECTED);

        // Then: Payment rejeitado depois
        policyProposal.processPaymentResponse(false, "Cartão bloqueado", Instant.now());
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.REJECTED);

        // Histórico tem 2 entradas de REJECTED
        assertThat(policyProposal.getHistory()).hasSize(5);

        HistoryEntry firstRejection = policyProposal.getHistory().get(3);
        assertThat(firstRejection.reason()).contains("Subscrição rejeitada: Alto risco");

        HistoryEntry secondRejection = policyProposal.getHistory().get(4);
        assertThat(secondRejection.reason()).contains("Pagamento rejeitado: Cartão bloqueado");
    }
}
