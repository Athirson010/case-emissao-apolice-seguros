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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de Componente - Entrada via API REST
 *
 * Cenário: Validação de Requisições da API
 * Entrada: HTTP POST/GET /policies
 *
 * Valida as regras:
 * - API-REQ-01: Existe endpoint HTTP para criação de solicitação
 * - API-REQ-02: Request contém campos obrigatórios
 * - API-REQ-03: Sistema gera ID único e data/hora
 * - API-REQ-04: Persistência correta dos campos
 * - API-REQ-05: Existe endpoint para consulta por ID
 * - API-REQ-06: Existe endpoint para consulta por customer_id
 */
@AutoConfigureMockMvc
@DisplayName("Entrada API - Validação de Requisições")
class RegrasValidacaoApiTest extends BaseComponentTest {

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
    @DisplayName("Deve existir endpoint HTTP POST /policies para criação de solicitação")
    void deveExistirEndpointParaCriacaoDeSolicitacao() throws Exception {
        // Given
        String requisicaoValida = FixtureLoader.loadFixtureAsString("valid-policy-request.json");

        PolicyProposal solicitacaoMock = PolicyProposal.builder()
                .id(PolicyProposalId.generate())
                .status(PolicyStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();

        when(createOrderUseCase.createPolicyRequest(any())).thenReturn(solicitacaoMock);

        // When & Then - Validar que endpoint POST /policies existe e responde
        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoValida))
                .andDo(print())
                .andExpect(status().isCreated());

        verify(createOrderUseCase, times(1)).createPolicyRequest(any());
    }

    @Test
    @DisplayName("Deve validar que requisição contém todos os campos obrigatórios")
    void deveValidarCamposObrigatorios() throws Exception {
        // Given - Request válido com TODOS os campos obrigatórios
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
                    "Colisão": "200000.00"
                },
                "assistances": ["Guincho 24h"]
            }
            """;

        PolicyProposal solicitacaoMock = PolicyProposal.builder()
                .id(PolicyProposalId.generate())
                .status(PolicyStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();

        when(createOrderUseCase.createPolicyRequest(any())).thenReturn(solicitacaoMock);

        // When & Then - Request com todos os campos deve ser aceito
        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoValida))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Deve rejeitar requisição sem customer_id")
    void deveRejeitarRequisicaoSemCustomerId() throws Exception {
        // Given - Request sem customer_id
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

        // When & Then
        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoInvalida))
                .andExpect(status().isBadRequest());

        verify(createOrderUseCase, never()).createPolicyRequest(any());
    }

    @Test
    @DisplayName("Deve rejeitar requisição sem coverages")
    void deveRejeitarRequisicaoSemCoverages() throws Exception {
        // Given - Request sem coverages
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
    @DisplayName("Sistema deve gerar ID único e data/hora na criação")
    void deveGerarIdUnicoEDataHoraNaCriacao() throws Exception {
        // Given
        String requisicaoValida = FixtureLoader.loadFixtureAsString("valid-policy-request.json");

        PolicyProposalId idGerado = PolicyProposalId.generate();
        Instant criadoEm = Instant.now();

        PolicyProposal solicitacaoMock = PolicyProposal.builder()
                .id(idGerado)
                .status(PolicyStatus.RECEIVED)
                .createdAt(criadoEm)
                .build();

        when(createOrderUseCase.createPolicyRequest(any())).thenReturn(solicitacaoMock);

        // When & Then - Resposta deve conter ID e timestamp gerados
        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoValida))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.policy_request_id").value(idGerado.asString()))
                .andExpect(jsonPath("$.created_at").exists())
                .andExpect(jsonPath("$.created_at").isNotEmpty());
    }

    @Test
    @DisplayName("Deve existir endpoint GET /policies/{id} para consulta por ID")
    void deveExistirEndpointParaConsultaPorId() throws Exception {
        // Given
        PolicyProposalId idSolicitacao = PolicyProposalId.from("123e4567-e89b-12d3-a456-426614174000");
        PolicyProposal solicitacaoMock = PolicyProposal.builder()
                .id(idSolicitacao)
                .status(PolicyStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();

        when(createOrderUseCase.findPolicyRequestById(any())).thenReturn(Optional.of(solicitacaoMock));

        // When & Then - Endpoint GET /policies/{id} deve existir e funcionar
        mockMvc.perform(get("/policies/" + idSolicitacao.asString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policy_request_id").value(idSolicitacao.asString()))
                .andExpect(jsonPath("$.status").exists());

        verify(createOrderUseCase, times(1)).findPolicyRequestById(any());
    }

    @Test
    @DisplayName("Consulta por ID inexistente deve retornar 404")
    void consultaPorIdInexistenteDeveRetornar404() throws Exception {
        // Given
        String idInexistente = "00000000-0000-0000-0000-000000000000";
        when(createOrderUseCase.findPolicyRequestById(any())).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/policies/" + idInexistente))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve validar que customer_id é UUID válido")
    void deveValidarQueCustomerIdEhUuidValido() throws Exception {
        // Given - customer_id inválido (não é UUID)
        String requisicaoInvalida = """
            {
                "customer_id": "nao-eh-um-uuid-valido",
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
    }

    @Test
    @DisplayName("Deve validar que valores monetários são positivos")
    void deveValidarQueValoresMonetariosSaoPositivos() throws Exception {
        // Given - Valor segurado negativo
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
    @DisplayName("Deve validar que coverages não pode estar vazio")
    void deveValidarQueCoveragesNaoPodeEstarVazio() throws Exception {
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
    @DisplayName("Deve validar que assistances não pode estar vazio")
    void deveValidarQueAssistancesNaoPodeEstarVazio() throws Exception {
        // Given - Assistances vazio
        String requisicaoInvalida = """
            {
                "customer_id": "123e4567-e89b-12d3-a456-426614174000",
                "product_id": "1b2da7cc-b367-4196-8a78-9cfeec21f587",
                "category": "AUTO",
                "sales_channel": "MOBILE",
                "payment_method": "CREDIT_CARD",
                "total_monthly_premium_amount": "350.00",
                "insured_amount": "200000.00",
                "coverages": {"Colisão": "200000.00"},
                "assistances": []
            }
            """;

        // When & Then
        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoInvalida))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve validar categoria de apólice")
    void deveValidarCategoriaDeApolice() throws Exception {
        // Given - Categoria inválida
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
}
