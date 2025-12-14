package io.github.athirson010.componenttest.entrada.api;

import io.github.athirson010.componenttest.config.BaseComponentTest;
import io.github.athirson010.componenttest.config.FixtureLoader;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de Componente - Entrada via API REST
 *
 * Cenário: Criação de Solicitação de Apólice
 * Entrada: HTTP POST /policies
 *
 * Valida:
 * - Campos obrigatórios
 * - Validações de formato
 * - Geração de ID e timestamp
 * - Estado inicial RECEIVED
 * - Respostas HTTP corretas
 */
@AutoConfigureMockMvc
@DisplayName("Entrada API - Criação de Solicitação de Apólice")
class CriacaoSolicitacaoApiTest extends BaseComponentTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateOrderUseCase createOrderUseCase;

    private PolicyProposal solicitacaoMock;
    private PolicyProposalId idGerado;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        reset(createOrderUseCase);

        // Preparar mock padrão
        idGerado = PolicyProposalId.generate();
        solicitacaoMock = PolicyProposal.builder()
                .id(idGerado)
                .status(PolicyStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Deve criar solicitação com todos os campos obrigatórios")
    void deveCriarSolicitacaoComTodosCamposObrigatorios() throws Exception {
        // Given - Requisição válida com todos os campos
        String requisicaoValida = """
            {
                "customer_id": "123e4567-e89b-12d3-a456-426614174000",
                "product_id": "1b2da7cc-b367-4196-8a78-9cfeec21f587",
                "category": "AUTO",
                "sales_channel": "MOBILE",
                "payment_method": "CREDIT_CARD",
                "total_monthly_premium_amount": "350.00",
                "insured_amount": "200000.00",
                "coverages": {
                    "Colisão": "200000.00",
                    "Roubo": "150000.00"
                },
                "assistances": ["Guincho 24h", "Chaveiro"]
            }
            """;

        when(createOrderUseCase.createPolicyRequest(any())).thenReturn(solicitacaoMock);

        // When - Chamada à API
        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoValida))
                .andDo(print())

                // Then - Validações
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.policy_request_id").value(idGerado.asString()))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.created_at").exists());

        // Verify - Use case foi chamado uma vez
        verify(createOrderUseCase, times(1)).createPolicyRequest(any());
    }

    @Test
    @DisplayName("Deve rejeitar solicitação sem customer_id")
    void deveRejeitarSolicitacaoSemCustomerId() throws Exception {
        // Given - Requisição sem customer_id
        String requisicaoInvalida = """
            {
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

        // When & Then - Deve retornar 400 Bad Request
        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoInvalida))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Verify - Use case NÃO deve ser chamado
        verify(createOrderUseCase, never()).createPolicyRequest(any());
    }

    @Test
    @DisplayName("Deve rejeitar solicitação com customer_id inválido (não UUID)")
    void deveRejeitarSolicitacaoComCustomerIdInvalido() throws Exception {
        // Given - customer_id não é UUID
        String requisicaoInvalida = """
            {
                "customer_id": "nao-e-um-uuid",
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

        // When & Then
        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoInvalida))
                .andExpect(status().isBadRequest());

        verify(createOrderUseCase, never()).createPolicyRequest(any());
    }

    @Test
    @DisplayName("Deve rejeitar solicitação sem coverages")
    void deveRejeitarSolicitacaoSemCoverages() throws Exception {
        // Given - Sem coverages
        String requisicaoInvalida = """
            {
                "customer_id": "123e4567-e89b-12d3-a456-426614174000",
                "product_id": "1b2da7cc-b367-4196-8a78-9cfeec21f587",
                "category": "AUTO",
                "sales_channel": "MOBILE",
                "payment_method": "CREDIT_CARD",
                "total_monthly_premium_amount": "350.00",
                "insured_amount": "200000.00",
                "assistances": ["Guincho 24h"]
            }
            """;

        // When & Then
        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoInvalida))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve rejeitar solicitação com coverages vazio")
    void deveRejeitarSolicitacaoComCoveragesVazio() throws Exception {
        // Given - Coverages vazio
        String requisicaoInvalida = """
            {
                "customer_id": "123e4567-e89b-12d3-a456-426614174000",
                "product_id": "1b2da7cc-b367-4196-8a78-9cfeec21f587",
                "category": "AUTO",
                "sales_channel": "MOBILE",
                "payment_method": "CREDIT_CARD",
                "total_monthly_premium_amount": "350.00",
                "insured_amount": "200000.00",
                "coverages": {},
                "assistances": ["Guincho 24h"]
            }
            """;

        // When & Then
        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoInvalida))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve rejeitar solicitação sem assistances")
    void deveRejeitarSolicitacaoSemAssistances() throws Exception {
        // Given - Sem assistances
        String requisicaoInvalida = """
            {
                "customer_id": "123e4567-e89b-12d3-a456-426614174000",
                "product_id": "1b2da7cc-b367-4196-8a78-9cfeec21f587",
                "category": "AUTO",
                "sales_channel": "MOBILE",
                "payment_method": "CREDIT_CARD",
                "total_monthly_premium_amount": "350.00",
                "insured_amount": "200000.00",
                "coverages": {"Colisão": "200000.00"}
            }
            """;

        // When & Then
        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoInvalida))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve rejeitar solicitação com valor monetário negativo")
    void deveRejeitarSolicitacaoComValorNegativo() throws Exception {
        // Given - Valor negativo
        String requisicaoInvalida = """
            {
                "customer_id": "123e4567-e89b-12d3-a456-426614174000",
                "product_id": "1b2da7cc-b367-4196-8a78-9cfeec21f587",
                "category": "AUTO",
                "sales_channel": "MOBILE",
                "payment_method": "CREDIT_CARD",
                "total_monthly_premium_amount": "-100.00",
                "insured_amount": "200000.00",
                "coverages": {"Colisão": "200000.00"},
                "assistances": ["Guincho 24h"]
            }
            """;

        // When & Then
        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoInvalida))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve rejeitar solicitação com categoria inválida")
    void deveRejeitarSolicitacaoComCategoriaInvalida() throws Exception {
        // Given - Categoria não existe
        String requisicaoInvalida = """
            {
                "customer_id": "123e4567-e89b-12d3-a456-426614174000",
                "product_id": "1b2da7cc-b367-4196-8a78-9cfeec21f587",
                "category": "CATEGORIA_INVALIDA",
                "sales_channel": "MOBILE",
                "payment_method": "CREDIT_CARD",
                "total_monthly_premium_amount": "350.00",
                "insured_amount": "200000.00",
                "coverages": {"Colisão": "200000.00"},
                "assistances": ["Guincho 24h"]
            }
            """;

        // When & Then
        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoInvalida))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve gerar ID único para cada solicitação")
    void deveGerarIdUnicoParaCadaSolicitacao() throws Exception {
        // Given
        String requisicao = FixtureLoader.loadFixtureAsString("valid-policy-request.json");

        PolicyProposalId id1 = PolicyProposalId.generate();
        PolicyProposalId id2 = PolicyProposalId.generate();

        PolicyProposal solicitacao1 = PolicyProposal.builder()
                .id(id1)
                .status(PolicyStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();

        PolicyProposal solicitacao2 = PolicyProposal.builder()
                .id(id2)
                .status(PolicyStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();

        when(createOrderUseCase.createPolicyRequest(any()))
                .thenReturn(solicitacao1)
                .thenReturn(solicitacao2);

        // When - Primeira chamada
        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicao))
                .andExpect(jsonPath("$.policy_request_id").value(id1.asString()));

        // When - Segunda chamada
        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicao))
                .andExpect(jsonPath("$.policy_request_id").value(id2.asString()));

        // Then - IDs devem ser diferentes
        assert !id1.equals(id2) : "IDs devem ser únicos";
    }

    @Test
    @DisplayName("Deve retornar estado inicial RECEIVED ao criar solicitação")
    void deveRetornarEstadoInicialReceived() throws Exception {
        // Given
        String requisicao = FixtureLoader.loadFixtureAsString("valid-policy-request.json");
        when(createOrderUseCase.createPolicyRequest(any())).thenReturn(solicitacaoMock);

        // When & Then
        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicao))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }
}
