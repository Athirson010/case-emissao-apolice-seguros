package io.github.athirson010.componenttest.entrada.mensageria;

import io.github.athirson010.adapters.in.messaging.rabbitmq.PaymentConfirmationConsumer;
import io.github.athirson010.componenttest.BaseComponentTest;
import io.github.athirson010.domain.enums.PolicyStatus;
import io.github.athirson010.domain.model.PolicyProposal;
import io.github.athirson010.domain.model.PolicyProposalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles({"test", "order-consumer", "order-response-payment-consumer"})
@DisplayName("Entrada Mensageria - Confirmação de Pagamento")
class ConfirmacaoPagamentoMessageTest extends BaseComponentTest {

    @Autowired
    private PaymentConfirmationConsumer paymentConfirmationConsumer;

    private PolicyProposalId idSolicitacao;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        idSolicitacao = PolicyProposalId.generate();

        // Configurar comportamento padrão do repository
        when(orderRepository.save(any(PolicyProposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Deve processar pagamento aprovado e atualizar status para APPROVED")
    void deveProcessarPagamentoAprovadoEAtualizarStatus() {
        // Given - Proposta em estado PENDING
        PolicyProposal propostaPending = PolicyProposal.builder()
                .id(idSolicitacao)
                .status(PolicyStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        when(orderRepository.findById(any()))
                .thenReturn(Optional.of(propostaPending));

        String mensagemPagamento = String.format("""
                {
                    "policy_request_id": "%s",
                    "payment_status": "APPROVED",
                    "transaction_id": "TXN-123456789",
                    "amount": "350.00",
                    "payment_method": "CREDIT_CARD",
                    "payment_timestamp": "2024-01-15T10:45:00Z"
                }
                """, idSolicitacao.asString());

        // When - Consumer processa mensagem
        paymentConfirmationConsumer.consumePaymentConfirmation(mensagemPagamento);

        // Then - Verifica que repository foi consultado e atualizado
        verify(orderRepository, times(1)).findById(idSolicitacao);
        verify(orderRepository, times(1)).save(any(PolicyProposal.class));
    }

    @Test
    @DisplayName("Deve processar pagamento rejeitado e atualizar status para REJECTED")
    void deveProcessarPagamentoRejeitadoEAtualizarStatus() {
        // Given - Proposta em estado PENDING
        PolicyProposal propostaPending = PolicyProposal.builder()
                .id(idSolicitacao)
                .status(PolicyStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        when(orderRepository.findById(any()))
                .thenReturn(Optional.of(propostaPending));

        String mensagemPagamento = String.format("""
                {
                    "policy_request_id": "%s",
                    "payment_status": "REJECTED",
                    "rejection_reason": "Cartão sem limite",
                    "payment_timestamp": "2024-01-15T10:45:00Z"
                }
                """, idSolicitacao.asString());

        // When - Consumer processa mensagem
        paymentConfirmationConsumer.consumePaymentConfirmation(mensagemPagamento);

        // Then - Verifica que repository foi consultado e atualizado
        verify(orderRepository, times(1)).findById(idSolicitacao);
        verify(orderRepository, times(1)).save(any(PolicyProposal.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar processar pagamento para proposta que não está em PENDING ou REJECTED")
    void deveLancarExcecaoAoProcessarPagamentoParaPropostaNaoPending() {
        // Given - Proposta em estado RECEIVED (não PENDING)
        PolicyProposal propostaReceived = PolicyProposal.builder()
                .id(idSolicitacao)
                .status(PolicyStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();

        when(orderRepository.findById(any()))
                .thenReturn(Optional.of(propostaReceived));

        String mensagemPagamento = String.format("""
                {
                    "policy_request_id": "%s",
                    "payment_status": "APPROVED",
                    "transaction_id": "TXN-123456789"
                }
                """, idSolicitacao.asString());

        // When & Then - Consumer deve lançar exceção
        assertThrows(Exception.class, () -> {
            paymentConfirmationConsumer.consumePaymentConfirmation(mensagemPagamento);
        });

        // Repository foi consultado mas não salvou devido à exceção
        verify(orderRepository, times(1)).findById(idSolicitacao);
        verify(orderRepository, never()).save(any(PolicyProposal.class));
    }

    @Test
    @DisplayName("Deve lançar exceção para proposta inexistente")
    void deveLancarExcecaoParaPropostaInexistente() {
        // Given - Proposta não existe
        when(orderRepository.findById(any()))
                .thenReturn(Optional.empty());

        String mensagemPagamento = String.format("""
                {
                    "policy_request_id": "%s",
                    "payment_status": "APPROVED",
                    "transaction_id": "TXN-123456789"
                }
                """, idSolicitacao.asString());

        // When & Then - Deve lançar exceção
        assertThrows(RuntimeException.class, () -> {
            paymentConfirmationConsumer.consumePaymentConfirmation(mensagemPagamento);
        });

        // Verify - Repository foi consultado mas não salvou
        verify(orderRepository, times(1)).findById(idSolicitacao);
        verify(orderRepository, never()).save(any(PolicyProposal.class));
    }

    @Test
    @DisplayName("Deve aplicar rejeição imediata quando pagamento é rejeitado")
    void deveAplicarRejeicaoImediataQuandoPagamentoRejeitado() {
        // Given - Proposta em estado PENDING
        PolicyProposal propostaPending = PolicyProposal.builder()
                .id(idSolicitacao)
                .status(PolicyStatus.PENDING)
                .createdAt(Instant.now())
                .paymentResponseReceived(false)
                .subscriptionResponseReceived(false)
                .build();

        when(orderRepository.findById(any()))
                .thenReturn(Optional.of(propostaPending));

        String mensagemPagamentoRejeitado = String.format("""
                {
                    "policy_request_id": "%s",
                    "payment_status": "REJECTED",
                    "rejection_reason": "Cartão sem saldo",
                    "payment_timestamp": "2024-01-15T10:45:00Z"
                }
                """, idSolicitacao.asString());

        // When - Consumer processa rejeição de pagamento
        paymentConfirmationConsumer.consumePaymentConfirmation(mensagemPagamentoRejeitado);

        // Then - Status deve mudar para REJECTED imediatamente
        verify(orderRepository, times(1)).findById(idSolicitacao);
        verify(orderRepository, times(1)).save(argThat(policy ->
                policy.getStatus() == PolicyStatus.REJECTED
        ));
    }
}
