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

@ActiveProfiles({"test", "order-consumer"})
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
    @DisplayName("Deve ignorar pagamento para proposta que não está em PENDING")
    void deveIgnorarPagamentoParaPropostaNaoPending() {
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

        // When - Consumer processa mensagem
        paymentConfirmationConsumer.consumePaymentConfirmation(mensagemPagamento);

        // Then - Repository foi consultado mas NÃO salvou (proposta não estava em PENDING)
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
    @DisplayName("Deve processar pagamentos com diferentes métodos de pagamento")
    void deveProcessarPagamentosComDiferentesMetodos() {
        // Given - Proposta em estado PENDING
        PolicyProposal propostaPending = PolicyProposal.builder()
                .id(idSolicitacao)
                .status(PolicyStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        when(orderRepository.findById(any()))
                .thenReturn(Optional.of(propostaPending));

        String[] metodos = {"CREDIT_CARD", "DEBIT_CARD", "PIX", "BOLETO"};

        for (String metodo : metodos) {
            String mensagem = String.format("""
                    {
                        "policy_request_id": "%s",
                        "payment_status": "APPROVED",
                        "payment_method": "%s",
                        "transaction_id": "TXN-123"
                    }
                    """, idSolicitacao.asString(), metodo);

            // When - Consumer processa mensagem
            paymentConfirmationConsumer.consumePaymentConfirmation(mensagem);

            // Then - Verifica que foi processado
            verify(orderRepository, atLeastOnce()).findById(idSolicitacao);
        }
    }
}
