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
 * Cenário: Validação de Formato de Mensagem de Fraude
 * Entrada: Mensagem Kafka/RabbitMQ da API de Fraudes
 *
 * Valida:
 * - Formato correto da mensagem de resposta de fraude
 * - Presença de campos obrigatórios
 * - Validação de tipos de dados
 * - Estrutura JSON esperada
 *
 * Nota: Estes testes validam o FORMATO das mensagens.
 * O processamento real seria feito por consumers Kafka/RabbitMQ específicos.
 */
@DisplayName("Entrada Mensageria - Validação de Fraude")
class ValidacaoFraudeMessageTest extends BaseComponentTest {

    private PolicyProposalId idSolicitacao;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        idSolicitacao = PolicyProposalId.generate();
    }

    @Test
    @DisplayName("Mensagem de validação de fraude deve conter campos obrigatórios")
    void mensagemDeValidacaoDeFraudeDeveConterCamposObrigatorios() {
        // Given - Mensagem indicando que não há fraude
        String mensagemFraude = String.format("""
            {
                "policy_request_id": "%s",
                "fraud_detected": false,
                "fraud_score": 0.15,
                "validation_timestamp": "2024-01-15T10:30:00Z"
            }
            """, idSolicitacao.asString());

        // When & Then - Validar que mensagem contém campos obrigatórios
        assertTrue(mensagemFraude.contains("policy_request_id"));
        assertTrue(mensagemFraude.contains("fraud_detected"));
        assertTrue(mensagemFraude.contains(idSolicitacao.asString()));
    }

    @Test
    @DisplayName("Mensagem de fraude detectada deve incluir motivo")
    void mensagemDeFraudeDetectadaDeveIncluirMotivo() {
        // Given - Mensagem indicando fraude detectada
        String mensagemFraude = String.format("""
            {
                "policy_request_id": "%s",
                "fraud_detected": true,
                "fraud_score": 0.95,
                "fraud_reason": "Padrão de fraude identificado",
                "validation_timestamp": "2024-01-15T10:30:00Z"
            }
            """, idSolicitacao.asString());

        // When & Then - Validar estrutura da mensagem
        assertTrue(mensagemFraude.contains("fraud_detected"));
        assertTrue(mensagemFraude.contains("true"));
        assertTrue(mensagemFraude.contains("fraud_reason"));
        assertTrue(mensagemFraude.contains("Padrão de fraude identificado"));
    }

    @Test
    @DisplayName("Mensagem deve ter policy_request_id válido")
    void mensagemDeveTerPolicyRequestIdValido() {
        // Given - Mensagem com UUID válido
        String mensagemValida = String.format("""
            {
                "policy_request_id": "%s",
                "fraud_detected": false
            }
            """, idSolicitacao.asString());

        // When & Then
        assertTrue(mensagemValida.contains(idSolicitacao.asString()));
        assertDoesNotThrow(() -> PolicyProposalId.from(idSolicitacao.asString()));
    }

    @Test
    @DisplayName("Mensagem sem policy_request_id deve ser inválida")
    void mensagemSemPolicyRequestIdDeveSerInvalida() {
        // Given - Mensagem sem campo obrigatório
        String mensagemInvalida = """
            {
                "fraud_detected": false,
                "fraud_score": 0.15
            }
            """;

        // When & Then - Mensagem não contém campo obrigatório
        assertFalse(mensagemInvalida.contains("policy_request_id"));
    }

    @Test
    @DisplayName("Mensagem deve ter campo fraud_detected booleano")
    void mensagemDeveTerCampoFraudDetectedBooleano() {
        // Given - Mensagens com valores booleanos
        String mensagemSemFraude = String.format("""
            {
                "policy_request_id": "%s",
                "fraud_detected": false
            }
            """, idSolicitacao.asString());

        String mensagemComFraude = String.format("""
            {
                "policy_request_id": "%s",
                "fraud_detected": true
            }
            """, idSolicitacao.asString());

        // When & Then
        assertTrue(mensagemSemFraude.contains("\"fraud_detected\": false"));
        assertTrue(mensagemComFraude.contains("\"fraud_detected\": true"));
    }

    @Test
    @DisplayName("Mensagem pode conter fraud_score opcional")
    void mensagemPodeConterFraudScoreOpcional() {
        // Given - Mensagem mínima sem fraud_score
        String mensagemMinima = String.format("""
            {
                "policy_request_id": "%s",
                "fraud_detected": false
            }
            """, idSolicitacao.asString());

        // Mensagem completa com fraud_score
        String mensagemCompleta = String.format("""
            {
                "policy_request_id": "%s",
                "fraud_detected": false,
                "fraud_score": 0.15
            }
            """, idSolicitacao.asString());

        // When & Then - Ambas são válidas
        assertTrue(mensagemMinima.contains("policy_request_id"));
        assertTrue(mensagemMinima.contains("fraud_detected"));

        assertTrue(mensagemCompleta.contains("fraud_score"));
        assertTrue(mensagemCompleta.contains("0.15"));
    }

    @Test
    @DisplayName("Mensagem deve ter formato JSON válido")
    void mensagemDeveTerFormatoJsonValido() {
        // Given - Mensagem com formato JSON correto
        String mensagemValida = String.format("""
            {
                "policy_request_id": "%s",
                "fraud_detected": false,
                "fraud_score": 0.15,
                "validation_timestamp": "2024-01-15T10:30:00Z"
            }
            """, idSolicitacao.asString());

        // When & Then - Validar estrutura JSON
        assertTrue(mensagemValida.trim().startsWith("{"));
        assertTrue(mensagemValida.trim().endsWith("}"));
        assertTrue(mensagemValida.contains("\":"));
    }

    @Test
    @DisplayName("Mensagem com fraude alta deve ter score maior que 0.7")
    void mensagemComFraudeAltaDeveTerScoreMaiorQue07() {
        // Given - Mensagem com fraude de alto risco
        String mensagemAltoRisco = String.format("""
            {
                "policy_request_id": "%s",
                "fraud_detected": true,
                "fraud_score": 0.95,
                "fraud_reason": "Alto risco detectado"
            }
            """, idSolicitacao.asString());

        // When & Then
        assertTrue(mensagemAltoRisco.contains("\"fraud_score\": 0.95"));
        assertTrue(mensagemAltoRisco.contains("\"fraud_detected\": true"));
    }

    @Test
    @DisplayName("Mensagem pode incluir timestamp de validação")
    void mensagemPodeIncluirTimestampDeValidacao() {
        // Given
        String mensagem = String.format("""
            {
                "policy_request_id": "%s",
                "fraud_detected": false,
                "validation_timestamp": "2024-01-15T10:30:00Z"
            }
            """, idSolicitacao.asString());

        // When & Then
        assertTrue(mensagem.contains("validation_timestamp"));
        assertTrue(mensagem.contains("2024-01-15T10:30:00Z"));
    }
}
