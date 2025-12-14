package io.github.athirson010.domain.model;

import io.github.athirson010.domain.enums.Category;
import io.github.athirson010.domain.enums.PaymentMethod;
import io.github.athirson010.domain.enums.PolicyStatus;
import io.github.athirson010.domain.enums.SalesChannel;
import io.github.athirson010.domain.exception.InvalidTransitionException;
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

@DisplayName("PolicyProposal - Testes Unitários")
class PolicyProposalTest {

    private PolicyProposal policyProposal;
    private PolicyProposalId policyId;

    @BeforeEach
    void setUp() {
        policyId = PolicyProposalId.generate();

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
                java.time.Instant.now()
        );
    }

    @Test
    @DisplayName("Deve criar proposta com status RECEIVED")
    void shouldCreatePolicyWithReceivedStatus() {
        // Then
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.RECEIVED);
        assertThat(policyProposal.getId()).isNotNull();
        assertThat(policyProposal.getCreatedAt()).isNotNull();
        assertThat(policyProposal.getHistory()).hasSize(1);
    }

    @Test
    @DisplayName("Deve validar proposta com sucesso")
    void shouldValidateProposalSuccessfully() {
        // When
        Instant now = Instant.now();
        policyProposal.validate(now);

        // Then
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.VALIDATED);
        assertThat(policyProposal.getHistory()).hasSize(2);
    }

    @Test
    @DisplayName("Deve aprovar proposta após validação")
    void shouldApproveProposalAfterValidation() {
        // Given
        policyProposal.validate(Instant.now());

        // When
        Instant now = Instant.now();
        policyProposal.approve(now);

        // Then
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.APPROVED);
        assertThat(policyProposal.getFinishedAt()).isNotNull();
        assertThat(policyProposal.getHistory()).hasSize(3);
    }

    @Test
    @DisplayName("Deve rejeitar proposta após validação")
    void shouldRejectProposalAfterValidation() {
        // Given
        policyProposal.validate(Instant.now());

        // When
        Instant now = Instant.now();
        policyProposal.reject("Capital segurado excede o limite", now);

        // Then
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(policyProposal.getFinishedAt()).isNotNull();
        assertThat(policyProposal.getHistory()).hasSize(3);
    }

    @Test
    @DisplayName("Deve cancelar proposta com sucesso")
    void shouldCancelProposalSuccessfully() {
        // When
        Instant now = Instant.now();
        policyProposal.cancel("Cliente solicitou cancelamento", now);

        // Then
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.CANCELED);
        assertThat(policyProposal.getFinishedAt()).isNotNull();
        assertThat(policyProposal.getHistory()).hasSize(2);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar validar proposta cancelada")
    void shouldThrowExceptionWhenValidatingCancelledProposal() {
        // Given
        policyProposal.cancel("Cancelado", Instant.now());

        // When/Then
        assertThatThrownBy(() -> policyProposal.validate(Instant.now()))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("Invalid transition");
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar aprovar sem validar")
    void shouldThrowExceptionWhenApprovingWithoutValidation() {
        // When/Then
        assertThatThrownBy(() -> policyProposal.approve(Instant.now()))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("Invalid transition");
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar rejeitar sem validar")
    void shouldThrowExceptionWhenRejectingWithoutValidation() {
        // When/Then
        assertThatThrownBy(() -> policyProposal.reject("Motivo", Instant.now()))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("Invalid transition");
    }

    @Test
    @DisplayName("Deve permitir cancelamento de proposta aprovada")
    void shouldAllowCancellingApprovedProposal() {
        // Given
        policyProposal.validate(Instant.now());
        policyProposal.approve(Instant.now());

        // When
        policyProposal.cancel("Motivo", Instant.now());

        // Then
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.CANCELED);
        assertThat(policyProposal.getHistory()).hasSize(4);
    }

    @Test
    @DisplayName("Deve registrar histórico de transições")
    void shouldRecordTransitionHistory() {
        // When
        policyProposal.validate(Instant.now());
        policyProposal.approve(Instant.now());

        // Then
        assertThat(policyProposal.getHistory()).hasSize(3);
        assertThat(policyProposal.getHistory().get(0).status()).isEqualTo(PolicyStatus.RECEIVED);
        assertThat(policyProposal.getHistory().get(1).status()).isEqualTo(PolicyStatus.VALIDATED);
        assertThat(policyProposal.getHistory().get(2).status()).isEqualTo(PolicyStatus.APPROVED);
    }

    @Test
    @DisplayName("Deve manter dados imutáveis após criação")
    void shouldKeepDataImmutableAfterCreation() {
        // Then
        assertThat(policyProposal.getCategory()).isEqualTo(Category.AUTO);
        assertThat(policyProposal.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(policyProposal.getTotalMonthlyPremiumAmount().amount())
                .isEqualByComparingTo(new BigDecimal("350.00"));
        assertThat(policyProposal.getInsuredAmount().amount())
                .isEqualByComparingTo(new BigDecimal("200000.00"));
    }

    @Test
    @DisplayName("Deve permitir cancelamento de proposta validada")
    void shouldAllowCancellingValidatedProposal() {
        // Given
        policyProposal.validate(Instant.now());

        // When
        policyProposal.cancel("Cliente desistiu", Instant.now());

        // Then
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.CANCELED);
        assertThat(policyProposal.getHistory()).hasSize(3);
    }

    @Test
    @DisplayName("Deve incluir motivo no histórico ao rejeitar")
    void shouldIncludeReasonInHistoryWhenRejecting() {
        // Given
        policyProposal.validate(Instant.now());

        // When
        String reason = "Capital segurado excede limite para categoria AUTO";
        policyProposal.reject(reason, Instant.now());

        // Then
        HistoryEntry lastEntry = policyProposal.getHistory().get(policyProposal.getHistory().size() - 1);
        assertThat(lastEntry.reason()).contains(reason);
    }

    @Test
    @DisplayName("Deve incluir motivo no histórico ao cancelar")
    void shouldIncludeReasonInHistoryWhenCancelling() {
        // When
        String reason = "Cliente solicitou cancelamento";
        policyProposal.cancel(reason, Instant.now());

        // Then
        HistoryEntry lastEntry = policyProposal.getHistory().get(policyProposal.getHistory().size() - 1);
        assertThat(lastEntry.reason()).contains(reason);
    }
}
