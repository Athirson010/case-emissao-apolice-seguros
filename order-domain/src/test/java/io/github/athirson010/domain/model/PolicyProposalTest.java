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
    void deveCriarPropostaComStatusReceived() {
        // Then
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.RECEIVED);
        assertThat(policyProposal.getId()).isNotNull();
        assertThat(policyProposal.getCreatedAt()).isNotNull();
        assertThat(policyProposal.getHistory()).hasSize(1);
    }

    @Test
    @DisplayName("Deve validar proposta com sucesso")
    void deveValidarPropostaComSucesso() {
        // When
        Instant now = Instant.now();
        policyProposal.validate(now);

        // Then
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.VALIDATED);
        assertThat(policyProposal.getHistory()).hasSize(2);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar aprovar direto após validação sem passar por PENDING")
    void deveLancarExcecaoAoTentarAprovarDiretoAposValidacao() {
        // Given
        policyProposal.validate(Instant.now());

        // When/Then
        assertThatThrownBy(() -> policyProposal.approve(Instant.now()))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("Invalid transition");
    }

    @Test
    @DisplayName("Deve aprovar proposta após passar por PENDING e receber confirmações")
    void deveAprovarPropostaAposPendingEConfirmacoes() {
        // Given
        policyProposal.validate(Instant.now());
        policyProposal.markAsPending(Instant.now());

        // When
        Instant now = Instant.now();
        policyProposal.confirmPayment(now);
        policyProposal.confirmSubscription(now);

        // Then
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.APPROVED);
        assertThat(policyProposal.getFinishedAt()).isNotNull();
        assertThat(policyProposal.isPaymentConfirmed()).isTrue();
        assertThat(policyProposal.isSubscriptionConfirmed()).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar proposta após validação")
    void deveRejeitarPropostaAposValidacao() {
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
    void deveCancelarPropostaComSucesso() {
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
    void deveLancarExcecaoAoValidarPropostaCancelada() {
        // Given
        policyProposal.cancel("Cancelado", Instant.now());

        // When/Then
        assertThatThrownBy(() -> policyProposal.validate(Instant.now()))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("Invalid transition");
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar aprovar sem validar")
    void deveLancarExcecaoAoAprovarSemValidacao() {
        // When/Then
        assertThatThrownBy(() -> policyProposal.approve(Instant.now()))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("Invalid transition");
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar rejeitar sem validar")
    void deveLancarExcecaoAoRejeitarSemValidacao() {
        // When/Then
        assertThatThrownBy(() -> policyProposal.reject("Motivo", Instant.now()))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("Invalid transition");
    }

    @Test
    @DisplayName("Não deve permitir cancelamento de proposta aprovada")
    void naoDevePermitirCancelamentoDePropstaAprovada() {
        // Given
        policyProposal.validate(Instant.now());
        policyProposal.markAsPending(Instant.now());
        policyProposal.confirmPayment(Instant.now());
        policyProposal.confirmSubscription(Instant.now());

        // When/Then
        assertThatThrownBy(() -> policyProposal.cancel("Motivo", Instant.now()))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("Cannot cancel policy proposal in final state");
    }

    @Test
    @DisplayName("Deve registrar histórico de transições completo")
    void deveRegistrarHistoricoDeTransicoes() {
        // When
        policyProposal.validate(Instant.now());
        policyProposal.markAsPending(Instant.now());
        policyProposal.confirmPayment(Instant.now());
        policyProposal.confirmSubscription(Instant.now());

        // Then
        assertThat(policyProposal.getHistory()).hasSize(4);
        assertThat(policyProposal.getHistory().get(0).status()).isEqualTo(PolicyStatus.RECEIVED);
        assertThat(policyProposal.getHistory().get(1).status()).isEqualTo(PolicyStatus.VALIDATED);
        assertThat(policyProposal.getHistory().get(2).status()).isEqualTo(PolicyStatus.PENDING);
        assertThat(policyProposal.getHistory().get(3).status()).isEqualTo(PolicyStatus.APPROVED);
    }

    @Test
    @DisplayName("Deve manter dados imutáveis após criação")
    void deveManterlDadosImutaveisAposCriacao() {
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
    void devePermitirCancelamentoDePropstaValidada() {
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
    void deveIncluirMotivoNoHistoricoAoRejeitar() {
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
    void deveIncluirMotivoNoHistoricoAoCancelar() {
        // When
        String reason = "Cliente solicitou cancelamento";
        policyProposal.cancel(reason, Instant.now());

        // Then
        HistoryEntry lastEntry = policyProposal.getHistory().get(policyProposal.getHistory().size() - 1);
        assertThat(lastEntry.reason()).contains(reason);
    }

    @Test
    @DisplayName("Deve marcar proposta como PENDING após validação")
    void deveMarcarPropostaComoPendingAposValidacao() {
        // Given
        policyProposal.validate(Instant.now());

        // When
        policyProposal.markAsPending(Instant.now());

        // Then
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.PENDING);
        assertThat(policyProposal.isPaymentConfirmed()).isFalse();
        assertThat(policyProposal.isSubscriptionConfirmed()).isFalse();
    }

    @Test
    @DisplayName("Deve confirmar pagamento em estado PENDING")
    void deveConfirmarPagamentoEmEstadoPending() {
        // Given
        policyProposal.validate(Instant.now());
        policyProposal.markAsPending(Instant.now());

        // When
        policyProposal.confirmPayment(Instant.now());

        // Then
        assertThat(policyProposal.isPaymentConfirmed()).isTrue();
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.PENDING); // Ainda PENDING até subscrição
    }

    @Test
    @DisplayName("Deve confirmar subscrição em estado PENDING")
    void deveConfirmarSubscricaoEmEstadoPending() {
        // Given
        policyProposal.validate(Instant.now());
        policyProposal.markAsPending(Instant.now());

        // When
        policyProposal.confirmSubscription(Instant.now());

        // Then
        assertThat(policyProposal.isSubscriptionConfirmed()).isTrue();
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.PENDING); // Ainda PENDING até pagamento
    }

    @Test
    @DisplayName("Deve aprovar automaticamente quando ambas confirmações forem recebidas")
    void deveAprovarAutomaticamenteQuandoAmbasConfirmacoesForemRecebidas() {
        // Given
        policyProposal.validate(Instant.now());
        policyProposal.markAsPending(Instant.now());
        policyProposal.confirmPayment(Instant.now());

        // When
        policyProposal.confirmSubscription(Instant.now());

        // Then
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.APPROVED);
        assertThat(policyProposal.isPaymentConfirmed()).isTrue();
        assertThat(policyProposal.isSubscriptionConfirmed()).isTrue();
    }

    @Test
    @DisplayName("Deve lançar exceção ao confirmar pagamento fora do estado PENDING")
    void deveLancarExcecaoAoConfirmarPagamentoForaDoEstadoPending() {
        // When/Then
        assertThatThrownBy(() -> policyProposal.confirmPayment(Instant.now()))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("Cannot confirm payment");
    }

    @Test
    @DisplayName("Deve lançar exceção ao confirmar subscrição fora do estado PENDING")
    void deveLancarExcecaoAoConfirmarSubscricaoForaDoEstadoPending() {
        // When/Then
        assertThatThrownBy(() -> policyProposal.confirmSubscription(Instant.now()))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining("Cannot confirm subscription");
    }

    @Test
    @DisplayName("Deve permitir cancelamento de proposta PENDING")
    void devePermitirCancelamentoDePropstaPending() {
        // Given
        policyProposal.validate(Instant.now());
        policyProposal.markAsPending(Instant.now());

        // When
        policyProposal.cancel("Cliente desistiu", Instant.now());

        // Then
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.CANCELED);
    }

    @Test
    @DisplayName("Deve rejeitar proposta PENDING se necessário")
    void deveRejeitarPropostaPendingSeNecessario() {
        // Given
        policyProposal.validate(Instant.now());
        policyProposal.markAsPending(Instant.now());

        // When
        policyProposal.reject("Pagamento negado", Instant.now());

        // Then
        assertThat(policyProposal.getStatus()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(policyProposal.getFinishedAt()).isNotNull();
    }
}
