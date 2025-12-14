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
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de Componente - Entrada via API REST
 *
 * Cenário: Consulta de Solicitação de Apólice por ID
 * Entrada: HTTP GET /policies/{id}
 *
 * Valida:
 * - Consulta por ID existente
 * - Consulta por ID inexistente (404)
 * - Formato da resposta
 * - Dados retornados
 */
@AutoConfigureMockMvc
@DisplayName("Entrada API - Consulta de Solicitação por ID")
class ConsultaSolicitacaoApiTest extends BaseComponentTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateOrderUseCase createOrderUseCase;

    private PolicyProposalId idExistente;
    private PolicyProposal solicitacaoExistente;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        reset(createOrderUseCase);

        // Preparar dados de teste
        idExistente = PolicyProposalId.from("123e4567-e89b-12d3-a456-426614174000");
        solicitacaoExistente = PolicyProposal.builder()
                .id(idExistente)
                .status(PolicyStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Deve retornar solicitação existente por ID")
    void deveRetornarSolicitacaoExistentePorId() throws Exception {
        // Given - Solicitação existe
        when(createOrderUseCase.findPolicyRequestById(any()))
                .thenReturn(Optional.of(solicitacaoExistente));

        // When & Then
        mockMvc.perform(get("/policies/" + idExistente.asString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policy_request_id").value(idExistente.asString()))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.created_at").exists());

        // Verify
        verify(createOrderUseCase, times(1)).findPolicyRequestById(any());
    }

    @Test
    @DisplayName("Deve retornar 404 para solicitação inexistente")
    void deveRetornar404ParaSolicitacaoInexistente() throws Exception {
        // Given - Solicitação não existe
        String idInexistente = "00000000-0000-0000-0000-000000000000";
        when(createOrderUseCase.findPolicyRequestById(any()))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/policies/" + idInexistente))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(createOrderUseCase, times(1)).findPolicyRequestById(any());
    }

    @Test
    @DisplayName("Deve retornar solicitação com status VALIDATED")
    void deveRetornarSolicitacaoComStatusValidated() throws Exception {
        // Given - Solicitação validada
        PolicyProposal solicitacaoValidada = PolicyProposal.builder()
                .id(idExistente)
                .status(PolicyStatus.VALIDATED)
                .createdAt(Instant.now())
                .build();

        when(createOrderUseCase.findPolicyRequestById(any()))
                .thenReturn(Optional.of(solicitacaoValidada));

        // When & Then
        mockMvc.perform(get("/policies/" + idExistente.asString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATED"));
    }

    @Test
    @DisplayName("Deve retornar solicitação com status PENDING")
    void deveRetornarSolicitacaoComStatusPending() throws Exception {
        // Given - Solicitação pendente
        PolicyProposal solicitacaoPendente = PolicyProposal.builder()
                .id(idExistente)
                .status(PolicyStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        when(createOrderUseCase.findPolicyRequestById(any()))
                .thenReturn(Optional.of(solicitacaoPendente));

        // When & Then
        mockMvc.perform(get("/policies/" + idExistente.asString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("Deve retornar solicitação com status APPROVED")
    void deveRetornarSolicitacaoComStatusApproved() throws Exception {
        // Given - Solicitação aprovada
        PolicyProposal solicitacaoAprovada = PolicyProposal.builder()
                .id(idExistente)
                .status(PolicyStatus.APPROVED)
                .createdAt(Instant.now())
                .build();

        when(createOrderUseCase.findPolicyRequestById(any()))
                .thenReturn(Optional.of(solicitacaoAprovada));

        // When & Then
        mockMvc.perform(get("/policies/" + idExistente.asString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("Deve retornar solicitação com status REJECTED")
    void deveRetornarSolicitacaoComStatusRejected() throws Exception {
        // Given - Solicitação rejeitada
        PolicyProposal solicitacaoRejeitada = PolicyProposal.builder()
                .id(idExistente)
                .status(PolicyStatus.REJECTED)
                .createdAt(Instant.now())
                .build();

        when(createOrderUseCase.findPolicyRequestById(any()))
                .thenReturn(Optional.of(solicitacaoRejeitada));

        // When & Then
        mockMvc.perform(get("/policies/" + idExistente.asString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("Deve retornar solicitação com status CANCELED")
    void deveRetornarSolicitacaoComStatusCanceled() throws Exception {
        // Given - Solicitação cancelada
        PolicyProposal solicitacaoCancelada = PolicyProposal.builder()
                .id(idExistente)
                .status(PolicyStatus.CANCELED)
                .createdAt(Instant.now())
                .build();

        when(createOrderUseCase.findPolicyRequestById(any()))
                .thenReturn(Optional.of(solicitacaoCancelada));

        // When & Then
        mockMvc.perform(get("/policies/" + idExistente.asString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }
}
