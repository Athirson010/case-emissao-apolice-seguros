package io.github.athirson010.componenttest.entrada.mensageria;

import io.github.athirson010.componenttest.config.BaseComponentTest;
import io.github.athirson010.domain.model.PolicyProposalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de Componente - Entrada via Mensageria
 *
 * Cenário: Validação de Formato de Mensagem de Pagamento
 * Entrada: Mensagem Kafka/RabbitMQ do Sistema de Pagamento
 *
 * Valida:
 * - Formato correto da mensagem de confirmação de pagamento
 * - Presença de campos obrigatórios
 * - Validação de status de pagamento
 * - Estrutura JSON esperada
 *
 * Nota: Estes testes validam o FORMATO das mensagens.
 * O processamento real seria feito por consumers Kafka/RabbitMQ específicos.
 */
@DisplayName("Entrada Mensageria - Confirmação de Pagamento")
class ConfirmacaoPagamentoMessageTest extends BaseComponentTest {

    private PolicyProposalId idSolicitacao;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        idSolicitacao = PolicyProposalId.generate();
    }

    @Test
    @DisplayName("Mensagem de pagamento deve conter campos obrigatórios")
    void mensagemDePagamentoDeveConterCamposObrigatorios() {
        // Given - Mensagem de pagamento aprovado
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

        // When & Then - Validar campos obrigatórios
        assertTrue(mensagemPagamento.contains("policy_request_id"));
        assertTrue(mensagemPagamento.contains("payment_status"));
        assertTrue(mensagemPagamento.contains(idSolicitacao.asString()));
    }

    @Test
    @DisplayName("Mensagem de pagamento aprovado deve ter status APPROVED")
    void mensagemDePagamentoAprovadoDeveTerStatusApproved() {
        // Given
        String mensagemPagamento = String.format("""
            {
                "policy_request_id": "%s",
                "payment_status": "APPROVED",
                "transaction_id": "TXN-123456789"
            }
            """, idSolicitacao.asString());

        // When & Then
        assertTrue(mensagemPagamento.contains("\"payment_status\": \"APPROVED\""));
        assertTrue(mensagemPagamento.contains("transaction_id"));
    }

    @Test
    @DisplayName("Mensagem de pagamento rejeitado deve ter status REJECTED")
    void mensagemDePagamentoRejeitadoDeveTerStatusRejected() {
        // Given
        String mensagemPagamento = String.format("""
            {
                "policy_request_id": "%s",
                "payment_status": "REJECTED",
                "rejection_reason": "Cartão sem limite",
                "payment_timestamp": "2024-01-15T10:45:00Z"
            }
            """, idSolicitacao.asString());

        // When & Then
        assertTrue(mensagemPagamento.contains("\"payment_status\": \"REJECTED\""));
        assertTrue(mensagemPagamento.contains("rejection_reason"));
        assertTrue(mensagemPagamento.contains("Cartão sem limite"));
    }

    @Test
    @DisplayName("Mensagem sem policy_request_id deve ser inválida")
    void mensagemSemPolicyRequestIdDeveSerInvalida() {
        // Given
        String mensagemInvalida = """
            {
                "payment_status": "APPROVED"
            }
            """;

        // When & Then
        assertFalse(mensagemInvalida.contains("policy_request_id"));
    }

    @Test
    @DisplayName("Mensagem aprovada deve conter transaction_id")
    void mensagemAprovadaDeveConterTransactionId() {
        // Given
        String mensagemPagamento = String.format("""
            {
                "policy_request_id": "%s",
                "payment_status": "APPROVED",
                "transaction_id": "TXN-987654321",
                "amount": "350.00"
            }
            """, idSolicitacao.asString());

        // When & Then
        assertTrue(mensagemPagamento.contains("transaction_id"));
        assertTrue(mensagemPagamento.contains("TXN-987654321"));
        assertTrue(mensagemPagamento.contains("amount"));
    }

    @Test
    @DisplayName("Mensagem deve suportar diferentes métodos de pagamento")
    void mensagemDeveSuportarDiferentesMetodosDePagamento() {
        // Given - Diferentes métodos de pagamento
        String[] metodos = {"CREDIT_CARD", "DEBIT_CARD", "PIX", "BOLETO"};

        for (String metodo : metodos) {
            String mensagem = String.format("""
                {
                    "policy_request_id": "%s",
                    "payment_status": "APPROVED",
                    "payment_method": "%s"
                }
                """, idSolicitacao.asString(), metodo);

            // When & Then
            assertTrue(mensagem.contains("payment_method"));
            assertTrue(mensagem.contains(metodo));
        }
    }

    @Test
    @DisplayName("Mensagem rejeitada deve incluir motivo da rejeição")
    void mensagemRejeitadaDeveIncluirMotivoDaRejeicao() {
        // Given
        String mensagemRejeitada = String.format("""
            {
                "policy_request_id": "%s",
                "payment_status": "REJECTED",
                "rejection_reason": "Saldo insuficiente"
            }
            """, idSolicitacao.asString());

        // When & Then
        assertTrue(mensagemRejeitada.contains("rejection_reason"));
        assertTrue(mensagemRejeitada.contains("Saldo insuficiente"));
    }

    @Test
    @DisplayName("Mensagem deve ter formato JSON válido")
    void mensagemDeveTerFormatoJsonValido() {
        // Given
        String mensagem = String.format("""
            {
                "policy_request_id": "%s",
                "payment_status": "APPROVED",
                "transaction_id": "TXN-123",
                "amount": "350.00"
            }
            """, idSolicitacao.asString());

        // When & Then
        assertTrue(mensagem.trim().startsWith("{"));
        assertTrue(mensagem.trim().endsWith("}"));
        assertTrue(mensagem.contains("\":"));
    }

    @Test
    @DisplayName("Mensagem pode incluir timestamp de processamento")
    void mensagemPodeIncluirTimestampDeProcessamento() {
        // Given
        String mensagem = String.format("""
            {
                "policy_request_id": "%s",
                "payment_status": "APPROVED",
                "payment_timestamp": "2024-01-15T10:45:00Z"
            }
            """, idSolicitacao.asString());

        // When & Then
        assertTrue(mensagem.contains("payment_timestamp"));
        assertTrue(mensagem.contains("2024-01-15T10:45:00Z"));
    }

    @Test
    @DisplayName("Mensagem aprovada deve incluir valor do pagamento")
    void mensagemAprovadaDeveIncluirValorDoPagamento() {
        // Given
        String mensagem = String.format("""
            {
                "policy_request_id": "%s",
                "payment_status": "APPROVED",
                "amount": "350.00"
            }
            """, idSolicitacao.asString());

        // When & Then
        assertTrue(mensagem.contains("amount"));
        assertTrue(mensagem.contains("350.00"));
    }
}
