package io.github.athirson010.componenttest.entrada.api;

import io.github.athirson010.componenttest.BaseComponentTest;
import io.github.athirson010.domain.enums.PolicyStatus;
import io.github.athirson010.domain.model.PolicyProposal;
import io.github.athirson010.domain.model.PolicyProposalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles({"test", "api"})
@DisplayName("Entrada API - Cancelamento de Solicitação")
class CancelamentoSolicitacaoApiTest extends BaseComponentTest {

    @Autowired
    private MockMvc mockMvc;

    private PolicyProposalId idSolicitacao;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        // Preparar dados
        idSolicitacao = PolicyProposalId.from("123e4567-e89b-12d3-a456-426614174000");

        // Configurar comportamento padrão dos mocks das portas de saída
        // O repository deve salvar e retornar a proposta
        when(orderRepository.save(any(PolicyProposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // O fraud queue port não faz nada (void)
        doNothing().when(fraudQueuePort).sendToFraudQueue(any(PolicyProposal.class));
    }

    @Test
    @DisplayName("Deve cancelar solicitação com sucesso")
    void deveCancelarSolicitacaoComSucesso() throws Exception {
        // Given - Proposta existe e está em status RECEIVED
        PolicyProposal propostaExistente = PolicyProposal.builder()
                .id(idSolicitacao)
                .status(PolicyStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();

        when(orderRepository.findById(any()))
                .thenReturn(Optional.of(propostaExistente));

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

        // Verify - Repository foi consultado e salvo
        verify(orderRepository, times(1)).findById(any());
        verify(orderRepository, times(1)).save(any(PolicyProposal.class));

        // Verify - Mensagem foi enviada para fila
        verify(fraudQueuePort, times(1)).sendToFraudQueue(any(PolicyProposal.class));
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

        // Verify - Repository NÃO deve ser chamado devido à validação
        verify(orderRepository, never()).findById(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar 404 ao cancelar solicitação inexistente")
    void deveRetornar404AoCancelarSolicitacaoInexistente() throws Exception {
        // Given - Solicitação não existe no repository
        String idInexistente = "00000000-0000-0000-0000-000000000000";

        when(orderRepository.findById(any()))
                .thenReturn(Optional.empty());

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

        // Verify - Repository foi consultado mas não salvou
        verify(orderRepository, times(1)).findById(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar cancelamento de solicitação APPROVED")
    void deveRejeitarCancelamentoDeSolicitacaoApproved() throws Exception {
        // Given - Solicitação aprovada não pode ser cancelada
        PolicyProposal propostaAprovada = PolicyProposal.builder()
                .id(idSolicitacao)
                .status(PolicyStatus.APPROVED)
                .createdAt(Instant.now())
                .build();

        when(orderRepository.findById(any()))
                .thenReturn(Optional.of(propostaAprovada));

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

        // Verify - Repository foi consultado mas não salvou
        verify(orderRepository, times(1)).findById(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar cancelamento de solicitação REJECTED")
    void deveRejeitarCancelamentoDeSolicitacaoRejected() throws Exception {
        // Given - Solicitação rejeitada não pode ser cancelada
        PolicyProposal propostaRejeitada = PolicyProposal.builder()
                .id(idSolicitacao)
                .status(PolicyStatus.REJECTED)
                .createdAt(Instant.now())
                .build();

        when(orderRepository.findById(any()))
                .thenReturn(Optional.of(propostaRejeitada));

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

        // Verify - Repository foi consultado mas não salvou
        verify(orderRepository, times(1)).findById(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve aceitar cancelamento de solicitação RECEIVED")
    void deveAceitarCancelamentoDeSolicitacaoReceived() throws Exception {
        // Given - RECEIVED pode ser cancelado
        PolicyProposal propostaReceived = PolicyProposal.builder()
                .id(idSolicitacao)
                .status(PolicyStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();

        when(orderRepository.findById(any()))
                .thenReturn(Optional.of(propostaReceived));

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

        verify(orderRepository, times(1)).findById(any());
        verify(orderRepository, times(1)).save(any(PolicyProposal.class));
        verify(fraudQueuePort, times(1)).sendToFraudQueue(any(PolicyProposal.class));
    }

    @Test
    @DisplayName("Deve aceitar cancelamento de solicitação VALIDATED")
    void deveAceitarCancelamentoDeSolicitacaoValidated() throws Exception {
        // Given - VALIDATED pode ser cancelado
        PolicyProposal propostaValidated = PolicyProposal.builder()
                .id(idSolicitacao)
                .status(PolicyStatus.VALIDATED)
                .createdAt(Instant.now())
                .build();

        when(orderRepository.findById(any()))
                .thenReturn(Optional.of(propostaValidated));

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

        verify(orderRepository, times(1)).findById(any());
        verify(orderRepository, times(1)).save(any(PolicyProposal.class));
        verify(fraudQueuePort, times(1)).sendToFraudQueue(any(PolicyProposal.class));
    }

    @Test
    @DisplayName("Deve aceitar cancelamento de solicitação PENDING")
    void deveAceitarCancelamentoDeSolicitacaoPending() throws Exception {
        // Given - PENDING pode ser cancelado
        PolicyProposal propostaPending = PolicyProposal.builder()
                .id(idSolicitacao)
                .status(PolicyStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        when(orderRepository.findById(any()))
                .thenReturn(Optional.of(propostaPending));

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

        verify(orderRepository, times(1)).findById(any());
        verify(orderRepository, times(1)).save(any(PolicyProposal.class));
        verify(fraudQueuePort, times(1)).sendToFraudQueue(any(PolicyProposal.class));
    }

    @Test
    @DisplayName("Deve preservar motivo do cancelamento")
    void devePreservarMotivoDoCancelamento() throws Exception {
        // Given
        String motivoCancelamento = "Cliente desistiu da compra";

        PolicyProposal propostaReceived = PolicyProposal.builder()
                .id(idSolicitacao)
                .status(PolicyStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();

        when(orderRepository.findById(any()))
                .thenReturn(Optional.of(propostaReceived));

        String requisicao = String.format("""
                {
                    "reason": "%s"
                }
                """, motivoCancelamento);

        // When
        mockMvc.perform(post("/policies/" + idSolicitacao.asString() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requisicao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        // Then - Verificar que o repository foi chamado
        verify(orderRepository, times(1)).findById(any());
        verify(orderRepository, times(1)).save(any(PolicyProposal.class));
        verify(fraudQueuePort, times(1)).sendToFraudQueue(any(PolicyProposal.class));
    }
}
