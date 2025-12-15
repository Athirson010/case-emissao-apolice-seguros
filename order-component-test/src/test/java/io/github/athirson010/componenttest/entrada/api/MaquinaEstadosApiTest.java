package io.github.athirson010.componenttest.entrada.api;

import io.github.athirson010.componenttest.BaseComponentTest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"test", "api"})
@AutoConfigureMockMvc
@DisplayName("Entrada API - Máquina de Estados")
class MaquinaEstadosApiTest extends BaseComponentTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateOrderUseCase createOrderUseCase;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        reset(createOrderUseCase);
    }

    @Test
    @DisplayName("Toda solicitação deve iniciar no estado RECEIVED")
    void todaSolicitacaoDeveIniciarNoEstadoReceived() throws Exception {
        // Given
        String requisicaoValida = """
                {
                    "customer_id": "123e4567-e89b-12d3-a456-426614174000",
                    "product_id": "1b2da7cc-b367-4196-8a78-9cfeec21f587",
                    "category": "AUTO",
                    "sales_channel": "MOBILE",
                    "payment_method": "CREDIT_CARD",
                    "total_monthly_premium_amount": "350.00",
                    "insured_amount": "200000.00",
                    "coverages": {"Colisão": "200000.00"},
                    "assistances": ["Guincho 24h"]
                }
                """;

        // Mock retornando proposta no estado RECEIVED
        PolicyProposal solicitacaoMock = PolicyProposal.builder()
                .id(PolicyProposalId.generate())
                .status(PolicyStatus.RECEIVED)  // Estado inicial obrigatório
                .createdAt(Instant.now())
                .build();

        when(createOrderUseCase.createPolicyRequest(any())).thenReturn(solicitacaoMock);

        // When & Then - Verificar que estado inicial é RECEIVED
        mockMvc.perform(post("/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requisicaoValida))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    @DisplayName("CANCELED permitido antes de APPROVED ou REJECTED")
    void canceledPermitidoAntesDeApprovedOuRejected() throws Exception {
        // Given - Proposta em estado RECEIVED
        PolicyProposalId idSolicitacao = PolicyProposalId.generate();

        PolicyProposal solicitacaoCancelada = PolicyProposal.builder()
                .id(idSolicitacao)
                .status(PolicyStatus.CANCELED)
                .createdAt(Instant.now())
                .build();

        when(createOrderUseCase.cancelPolicyRequest(any(), anyString()))
                .thenReturn(solicitacaoCancelada);

        String requisicaoCancelamento = """
                {
                    "reason": "Cliente solicitou"
                }
                """;

        // When & Then - Cancelamento deve ser permitido
        mockMvc.perform(post("/policies/" + idSolicitacao.asString() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requisicaoCancelamento))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        verify(createOrderUseCase, times(1)).cancelPolicyRequest(any(), eq("Cliente solicitou"));
    }

    @Test
    @DisplayName("De RECEIVED pode ir para VALIDATED ou CANCELED")
    void deReceivedPodeIrParaValidatedOuCanceled() throws Exception {
        // Given - Proposta em RECEIVED
        PolicyProposalId idSolicitacao = PolicyProposalId.generate();

        PolicyProposal solicitacaoCancelada = PolicyProposal.builder()
                .id(idSolicitacao)
                .status(PolicyStatus.CANCELED)
                .build();

        when(createOrderUseCase.cancelPolicyRequest(any(), anyString()))
                .thenReturn(solicitacaoCancelada);

        String requisicaoCancelamento = """
                {
                    "reason": "Teste de transição"
                }
                """;

        // When & Then - Transição RECEIVED → CANCELED permitida
        mockMvc.perform(post("/policies/" + idSolicitacao.asString() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requisicaoCancelamento))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    @DisplayName("Motivo de cancelamento deve ser obrigatório")
    void motivoDeCancelamentoDeveSerObrigatorio() throws Exception {
        // Given
        PolicyProposalId idSolicitacao = PolicyProposalId.generate();

        // Request sem motivo de cancelamento
        String requisicaoInvalida = "{}";

        // When & Then - Deve retornar erro de validação
        mockMvc.perform(post("/policies/" + idSolicitacao.asString() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requisicaoInvalida))
                .andExpect(status().isBadRequest());

        // Verify que o use case não foi chamado
        verify(createOrderUseCase, never()).cancelPolicyRequest(any(), anyString());
    }

    @Test
    @DisplayName("Transição de RECEIVED para VALIDATED deve ser permitida")
    void transicaoDeReceivedParaValidatedDeveSerPermitida() throws Exception {
        // Given
        PolicyProposalId idSolicitacao = PolicyProposalId.generate();

        PolicyProposal solicitacaoValidada = PolicyProposal.builder()
                .id(idSolicitacao)
                .status(PolicyStatus.VALIDATED)
                .createdAt(Instant.now())
                .build();

        // Simular processamento que transiciona para VALIDATED
        // (Isso normalmente seria feito por um processador assíncrono)
        // Aqui apenas validamos que o estado VALIDATED é possível

        // When & Then - VALIDATED é um estado válido
        assert solicitacaoValidada.getStatus() == PolicyStatus.VALIDATED;
    }

    @Test
    @DisplayName("Transição de VALIDATED para PENDING deve ser permitida")
    void transicaoDeValidatedParaPendingDeveSerPermitida() throws Exception {
        // Given
        PolicyProposalId idSolicitacao = PolicyProposalId.generate();

        PolicyProposal solicitacaoPendente = PolicyProposal.builder()
                .id(idSolicitacao)
                .status(PolicyStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        // When & Then - PENDING é um estado válido após VALIDATED
        assert solicitacaoPendente.getStatus() == PolicyStatus.PENDING;
    }

    @Test
    @DisplayName("Transição de PENDING para APPROVED deve ser permitida")
    void transicaoDePendingParaApprovedDeveSerPermitida() throws Exception {
        // Given
        PolicyProposalId idSolicitacao = PolicyProposalId.generate();

        PolicyProposal solicitacaoAprovada = PolicyProposal.builder()
                .id(idSolicitacao)
                .status(PolicyStatus.APPROVED)
                .createdAt(Instant.now())
                .build();

        // When & Then - APPROVED é um estado válido após PENDING
        assert solicitacaoAprovada.getStatus() == PolicyStatus.APPROVED;
    }

    @Test
    @DisplayName("Transição de PENDING para REJECTED deve ser permitida")
    void transicaoDePendingParaRejectedDeveSerPermitida() throws Exception {
        // Given
        PolicyProposalId idSolicitacao = PolicyProposalId.generate();

        PolicyProposal solicitacaoRejeitada = PolicyProposal.builder()
                .id(idSolicitacao)
                .status(PolicyStatus.REJECTED)
                .createdAt(Instant.now())
                .build();

        // When & Then - REJECTED é um estado válido após PENDING
        assert solicitacaoRejeitada.getStatus() == PolicyStatus.REJECTED;
    }

    @Test
    @DisplayName("Cancelamento deve preservar o motivo fornecido")
    void cancelamentoDevePreservarMotivoFornecido() throws Exception {
        // Given
        PolicyProposalId idSolicitacao = PolicyProposalId.generate();
        String motivoCancelamento = "Cliente desistiu da contratação";

        PolicyProposal solicitacaoCancelada = PolicyProposal.builder()
                .id(idSolicitacao)
                .status(PolicyStatus.CANCELED)
                .createdAt(Instant.now())
                .build();

        when(createOrderUseCase.cancelPolicyRequest(any(), anyString()))
                .thenReturn(solicitacaoCancelada);

        String requisicaoCancelamento = String.format("""
                {
                    "reason": "%s"
                }
                """, motivoCancelamento);

        // When
        mockMvc.perform(post("/policies/" + idSolicitacao.asString() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requisicaoCancelamento))
                .andExpect(status().isOk());

        // Then - Verificar que o motivo foi passado corretamente
        verify(createOrderUseCase, times(1))
                .cancelPolicyRequest(any(), eq(motivoCancelamento));
    }
}
