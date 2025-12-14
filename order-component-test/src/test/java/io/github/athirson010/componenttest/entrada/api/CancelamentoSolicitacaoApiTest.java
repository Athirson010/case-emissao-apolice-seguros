package io.github.athirson010.componenttest.entrada.api;

import io.github.athirson010.componenttest.config.BaseComponentTest;
import io.github.athirson010.core.port.in.CreateOrderUseCase;
import io.github.athirson010.domain.enums.PolicyStatus;
import io.github.athirson010.domain.model.PolicyProposal;
import io.github.athirson010.domain.model.PolicyProposalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de Componente - Entrada via API REST
 *
 * Cenário: Cancelamento de Solicitação de Apólice
 * Entrada: HTTP POST /policies/{id}/cancel
 *
 * Valida:
 * - Cancelamento com sucesso
 * - Cancelamento com motivo obrigatório
 * - Cancelamento de solicitação inexistente
 * - Cancelamento de solicitação em estado final
 * - Transições de estado permitidas
 */
@AutoConfigureMockMvc
@DisplayName("Entrada API - Cancelamento de Solicitação")
class CancelamentoSolicitacaoApiTest extends BaseComponentTest {

    @Autowired

    private MockMvc mockMvc;

    @MockBean
    private CreateOrderUseCase createOrderUseCase;

    private PolicyProposalId idSolicitacao;
    private PolicyProposal solicitacaoCancelada;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        reset(createOrderUseCase);

        // Preparar dados
        idSolicitacao = PolicyProposalId.from("123e4567-e89b-12d3-a456-426614174000");
        solicitacaoCancelada = PolicyProposal.builder()
                .id(idSolicitacao)
                .status(PolicyStatus.CANCELED)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Deve cancelar solicitação com sucesso")
    void deveCancelarSolicitacaoComSucesso() throws Exception {
        // Given
        when(createOrderUseCase.cancelPolicyRequest(any(), anyString()))
                .thenReturn(solicitacaoCancelada);

        String requisicaoCancelamento = """
            {
                "reason": "Cliente solicitou cancelamento"
            }
            """;

        // When & Then
        mockMvc.perform(post("/policies/" + idSolicitacao.asString() + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoCancelamento))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policy_request_id").value(idSolicitacao.asString()))
                .andExpect(jsonPath("$.status").value("CANCELED"));

        verify(createOrderUseCase, times(1))
                .cancelPolicyRequest(any(), eq("Cliente solicitou cancelamento"));
    }

    @Test
    @DisplayName("Deve rejeitar cancelamento sem motivo")
    void deveRejeitarCancelamentoSemMotivo() throws Exception {
        // Given - Requisição sem reason
        String requisicaoInvalida = "{}";

        // When & Then
        mockMvc.perform(post("/policies/" + idSolicitacao.asString() + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoInvalida))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Verify - Use case NÃO deve ser chamado
        verify(createOrderUseCase, never()).cancelPolicyRequest(any(), anyString());
    }

    @Test
    @DisplayName("Deve retornar 404 ao cancelar solicitação inexistente")
    void deveRetornar404AoCancelarSolicitacaoInexistente() throws Exception {
        // Given - Solicitação não existe
        String idInexistente = "00000000-0000-0000-0000-000000000000";

        when(createOrderUseCase.cancelPolicyRequest(any(), anyString()))
                .thenThrow(new IllegalArgumentException("Policy not found"));

        String requisicao = """
            {
                "reason": "Teste de cancelamento"
            }
            """;

        // When & Then
        mockMvc.perform(post("/policies/" + idInexistente + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicao))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(createOrderUseCase, times(1))
                .cancelPolicyRequest(any(), eq("Teste de cancelamento"));
    }

    @Test
    @DisplayName("Deve rejeitar cancelamento de solicitação APPROVED")
    void deveRejeitarCancelamentoDeSolicitacaoApproved() throws Exception {
        // Given - Solicitação aprovada não pode ser cancelada
        when(createOrderUseCase.cancelPolicyRequest(any(), anyString()))
                .thenThrow(new IllegalStateException("Cannot cancel approved policy"));

        String requisicao = """
            {
                "reason": "Tentativa de cancelar aprovada"
            }
            """;

        // When & Then
        mockMvc.perform(post("/policies/" + idSolicitacao.asString() + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicao))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Deve rejeitar cancelamento de solicitação REJECTED")
    void deveRejeitarCancelamentoDeSolicitacaoRejected() throws Exception {
        // Given - Solicitação rejeitada não pode ser cancelada
        when(createOrderUseCase.cancelPolicyRequest(any(), anyString()))
                .thenThrow(new IllegalStateException("Cannot cancel rejected policy"));

        String requisicao = """
            {
                "reason": "Tentativa de cancelar rejeitada"
            }
            """;

        // When & Then
        mockMvc.perform(post("/policies/" + idSolicitacao.asString() + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicao))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Deve aceitar cancelamento de solicitação RECEIVED")
    void deveAceitarCancelamentoDeSolicitacaoReceived() throws Exception {
        // Given - RECEIVED pode ser cancelado
        when(createOrderUseCase.cancelPolicyRequest(any(), anyString()))
                .thenReturn(solicitacaoCancelada);

        String requisicao = """
            {
                "reason": "Cancelamento de solicitação recebida"
            }
            """;

        // When & Then
        mockMvc.perform(post("/policies/" + idSolicitacao.asString() + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    @DisplayName("Deve aceitar cancelamento de solicitação VALIDATED")
    void deveAceitarCancelamentoDeSolicitacaoValidated() throws Exception {
        // Given - VALIDATED pode ser cancelado
        when(createOrderUseCase.cancelPolicyRequest(any(), anyString()))
                .thenReturn(solicitacaoCancelada);

        String requisicao = """
            {
                "reason": "Cancelamento de solicitação validada"
            }
            """;

        // When & Then
        mockMvc.perform(post("/policies/" + idSolicitacao.asString() + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    @DisplayName("Deve aceitar cancelamento de solicitação PENDING")
    void deveAceitarCancelamentoDeSolicitacaoPending() throws Exception {
        // Given - PENDING pode ser cancelado
        when(createOrderUseCase.cancelPolicyRequest(any(), anyString()))
                .thenReturn(solicitacaoCancelada);

        String requisicao = """
            {
                "reason": "Cancelamento de solicitação pendente"
            }
            """;

        // When & Then
        mockMvc.perform(post("/policies/" + idSolicitacao.asString() + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    @DisplayName("Deve preservar motivo do cancelamento")
    void devePreservarMotivoDoCancelamento() throws Exception {
        // Given
        String motivoCancelamento = "Cliente desistiu da compra";

        when(createOrderUseCase.cancelPolicyRequest(any(), anyString()))
                .thenReturn(solicitacaoCancelada);

        String requisicao = String.format("""
            {
                "reason": "%s"
            }
            """, motivoCancelamento);

        // When
        mockMvc.perform(post("/policies/" + idSolicitacao.asString() + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicao))
                .andExpect(status().isOk());

        // Then - Verificar que o motivo foi passado corretamente
        verify(createOrderUseCase, times(1))
                .cancelPolicyRequest(any(), eq(motivoCancelamento));
    }
}
