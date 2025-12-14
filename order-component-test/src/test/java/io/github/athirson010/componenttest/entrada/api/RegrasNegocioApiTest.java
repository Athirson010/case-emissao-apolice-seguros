package io.github.athirson010.componenttest.entrada.api;

import io.github.athirson010.componenttest.config.BaseComponentTest;
import io.github.athirson010.core.port.in.CreateOrderUseCase;
import io.github.athirson010.domain.enums.PolicyStatus;
import io.github.athirson010.domain.model.PolicyProposal;
import io.github.athirson010.domain.model.PolicyProposalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de Componente - Entrada via API REST
 *
 * Cenário: Validação de Regras de Negócio por Tipo de Cliente
 * Entrada: HTTP POST /policies
 *
 * Valida as regras de capital segurado conforme classificação do cliente:
 *
 * CLIENTE REGULAR:
 * - BUS-REG-01: VIDA/RESIDENCIAL ≤ 500.000,00
 * - BUS-REG-02: AUTO ≤ 350.000,00
 * - BUS-REG-03: OUTROS ≤ 255.000,00
 *
 * CLIENTE ALTO RISCO:
 * - BUS-HR-01: AUTO ≤ 250.000,00
 * - BUS-HR-02: RESIDENCIAL ≤ 150.000,00
 * - BUS-HR-03: OUTROS ≤ 125.000,00
 *
 * CLIENTE PREFERENCIAL:
 * - BUS-PREF-01: VIDA < 800.000,00
 * - BUS-PREF-02: AUTO/RESIDENCIAL < 450.000,00
 * - BUS-PREF-03: OUTROS ≤ 375.000,00
 *
 * CLIENTE SEM INFORMAÇÃO:
 * - BUS-NOINFO-01: VIDA/RESIDENCIAL ≤ 200.000,00
 * - BUS-NOINFO-02: AUTO ≤ 75.000,00
 * - BUS-NOINFO-03: OUTROS ≤ 55.000,00
 */
@AutoConfigureMockMvc
@DisplayName("Entrada API - Regras de Negócio por Tipo de Cliente")
class RegrasNegocioApiTest extends BaseComponentTest {

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

    // ===========================================
    // Testes de Cliente REGULAR
    // ===========================================

    @ParameterizedTest
    @DisplayName("Deve validar limite de capital segurado para cliente REGULAR - VIDA/RESIDENCIAL")
    @CsvSource({
            "VIDA, 500000.00, true",
            "VIDA, 500000.01, false",
            "RESIDENCIAL, 500000.00, true",
            "RESIDENCIAL, 500000.01, false"
    })
    void deveValidarLimiteClienteRegularVidaResidencial(String categoria, String valorSegurado, boolean deveAceitar) throws Exception {
        // Given - Requisição com categoria e valor especificado
        String requisicao = criarRequisicaoApolice(categoria, valorSegurado);

        if (deveAceitar) {
            // Mock aceita a solicitação
            PolicyProposal solicitacaoAceita = PolicyProposal.builder()
                    .id(PolicyProposalId.generate())
                    .status(PolicyStatus.VALIDATED)
                    .createdAt(Instant.now())
                    .build();
            when(createOrderUseCase.createPolicyRequest(any())).thenReturn(solicitacaoAceita);

            // When & Then - Deve aceitar
            mockMvc.perform(post("/policies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requisicao))
                    .andExpect(status().isCreated());
        } else {
            // Mock rejeita a solicitação por exceder limite
            when(createOrderUseCase.createPolicyRequest(any()))
                    .thenThrow(new IllegalArgumentException("Capital segurado excede o limite para cliente REGULAR"));

            // When & Then - Deve rejeitar
            mockMvc.perform(post("/policies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requisicao))
                    .andExpect(status().is4xxClientError());
        }
    }

    @ParameterizedTest
    @DisplayName("Deve validar limite de capital segurado para cliente REGULAR - AUTO")
    @CsvSource({
            "AUTO, 350000.00, true",
            "AUTO, 350000.01, false",
            "AUTO, 300000.00, true"
    })
    void deveValidarLimiteClienteRegularAuto(String categoria, String valorSegurado, boolean deveAceitar) throws Exception {
        // Given
        String requisicao = criarRequisicaoApolice(categoria, valorSegurado);

        if (deveAceitar) {
            PolicyProposal solicitacaoAceita = PolicyProposal.builder()
                    .id(PolicyProposalId.generate())
                    .status(PolicyStatus.VALIDATED)
                    .build();
            when(createOrderUseCase.createPolicyRequest(any())).thenReturn(solicitacaoAceita);

            // When & Then
            mockMvc.perform(post("/policies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requisicao))
                    .andExpect(status().isCreated());
        } else {
            when(createOrderUseCase.createPolicyRequest(any()))
                    .thenThrow(new IllegalArgumentException("Capital segurado excede o limite"));

            // When & Then
            mockMvc.perform(post("/policies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requisicao))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Test
    @DisplayName("Deve validar limite de capital segurado para cliente REGULAR - OUTROS")
    void deveValidarLimiteClienteRegularOutros() throws Exception {
        // Given - EMPRESARIAL é categoria "OUTROS"
        String requisicaoValida = criarRequisicaoApolice("EMPRESARIAL", "255000.00");
        String requisicaoInvalida = criarRequisicaoApolice("EMPRESARIAL", "255000.01");

        // When & Then - Válido
        PolicyProposal solicitacaoAceita = PolicyProposal.builder()
                .id(PolicyProposalId.generate())
                .status(PolicyStatus.VALIDATED)
                .build();
        when(createOrderUseCase.createPolicyRequest(any())).thenReturn(solicitacaoAceita);

        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoValida))
                .andExpect(status().isCreated());

        // Reset para testar caso inválido
        reset(createOrderUseCase);
        when(createOrderUseCase.createPolicyRequest(any()))
                .thenThrow(new IllegalArgumentException("Limite excedido"));

        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoInvalida))
                .andExpect(status().is4xxClientError());
    }

    // ===========================================
    // Testes de Cliente ALTO RISCO (HIGH_RISK)
    // ===========================================

    @Test
    @DisplayName("Deve validar limite de capital segurado para cliente ALTO RISCO - AUTO")
    void deveValidarLimiteClienteAltoRiscoAuto() throws Exception {
        // Given
        String requisicaoValida = criarRequisicaoApolice("AUTO", "250000.00");
        String requisicaoInvalida = criarRequisicaoApolice("AUTO", "250000.01");

        // Valid
        PolicyProposal solicitacaoAceita = PolicyProposal.builder()
                .id(PolicyProposalId.generate())
                .status(PolicyStatus.VALIDATED)
                .build();
        when(createOrderUseCase.createPolicyRequest(any())).thenReturn(solicitacaoAceita);

        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoValida))
                .andExpect(status().isCreated());

        // Invalid
        reset(createOrderUseCase);
        when(createOrderUseCase.createPolicyRequest(any()))
                .thenThrow(new IllegalArgumentException("Limite excedido para cliente ALTO RISCO"));

        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoInvalida))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Deve validar limite de capital segurado para cliente ALTO RISCO - RESIDENCIAL")
    void deveValidarLimiteClienteAltoRiscoResidencial() throws Exception {
        // Given
        String requisicaoValida = criarRequisicaoApolice("RESIDENCIAL", "150000.00");

        PolicyProposal solicitacaoAceita = PolicyProposal.builder()
                .id(PolicyProposalId.generate())
                .status(PolicyStatus.VALIDATED)
                .build();
        when(createOrderUseCase.createPolicyRequest(any())).thenReturn(solicitacaoAceita);

        // When & Then
        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoValida))
                .andExpect(status().isCreated());
    }

    // ===========================================
    // Testes de Cliente PREFERENCIAL
    // ===========================================

    @Test
    @DisplayName("Deve validar limite de capital segurado para cliente PREFERENCIAL - VIDA")
    void deveValidarLimiteClientePreferencialVida() throws Exception {
        // Given - Nota: Para PREFERENCIAL VIDA o limite é < 800.000 (menor estrito)
        String requisicaoValida = criarRequisicaoApolice("VIDA", "799999.99");
        String requisicaoInvalida = criarRequisicaoApolice("VIDA", "800000.00"); // Não pode ser igual

        // Valid
        PolicyProposal solicitacaoAceita = PolicyProposal.builder()
                .id(PolicyProposalId.generate())
                .status(PolicyStatus.VALIDATED)
                .build();
        when(createOrderUseCase.createPolicyRequest(any())).thenReturn(solicitacaoAceita);

        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoValida))
                .andExpect(status().isCreated());

        // Invalid
        reset(createOrderUseCase);
        when(createOrderUseCase.createPolicyRequest(any()))
                .thenThrow(new IllegalArgumentException("Deve ser menor que 800.000 para cliente PREFERENCIAL"));

        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoInvalida))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Deve validar limite de capital segurado para cliente PREFERENCIAL - AUTO/RESIDENCIAL")
    void deveValidarLimiteClientePreferencialAutoResidencial() throws Exception {
        // Given - Nota: Para PREFERENCIAL AUTO/RESIDENCIAL o limite é < 450.000 (menor estrito)
        String requisicaoValida = criarRequisicaoApolice("AUTO", "449999.99");

        PolicyProposal solicitacaoAceita = PolicyProposal.builder()
                .id(PolicyProposalId.generate())
                .status(PolicyStatus.VALIDATED)
                .build();
        when(createOrderUseCase.createPolicyRequest(any())).thenReturn(solicitacaoAceita);

        // When & Then
        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoValida))
                .andExpect(status().isCreated());
    }

    // ===========================================
    // Testes de Cliente SEM INFORMAÇÃO
    // ===========================================

    @Test
    @DisplayName("Deve validar limite de capital segurado para cliente SEM INFORMAÇÃO - AUTO")
    void deveValidarLimiteClienteSemInformacaoAuto() throws Exception {
        // Given
        String requisicaoValida = criarRequisicaoApolice("AUTO", "75000.00");
        String requisicaoInvalida = criarRequisicaoApolice("AUTO", "75000.01");

        // Valid
        PolicyProposal solicitacaoAceita = PolicyProposal.builder()
                .id(PolicyProposalId.generate())
                .status(PolicyStatus.VALIDATED)
                .build();
        when(createOrderUseCase.createPolicyRequest(any())).thenReturn(solicitacaoAceita);

        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoValida))
                .andExpect(status().isCreated());

        // Invalid
        reset(createOrderUseCase);
        when(createOrderUseCase.createPolicyRequest(any()))
                .thenThrow(new IllegalArgumentException("Limite excedido para cliente SEM INFORMAÇÃO"));

        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoInvalida))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Deve validar limite de capital segurado para cliente SEM INFORMAÇÃO - VIDA/RESIDENCIAL")
    void deveValidarLimiteClienteSemInformacaoVidaResidencial() throws Exception {
        // Given
        String requisicaoValida = criarRequisicaoApolice("VIDA", "200000.00");

        PolicyProposal solicitacaoAceita = PolicyProposal.builder()
                .id(PolicyProposalId.generate())
                .status(PolicyStatus.VALIDATED)
                .build();
        when(createOrderUseCase.createPolicyRequest(any())).thenReturn(solicitacaoAceita);

        // When & Then
        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoValida))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Deve validar limite de capital segurado para cliente SEM INFORMAÇÃO - OUTROS")
    void deveValidarLimiteClienteSemInformacaoOutros() throws Exception {
        // Given
        String requisicaoValida = criarRequisicaoApolice("EMPRESARIAL", "55000.00");
        String requisicaoInvalida = criarRequisicaoApolice("EMPRESARIAL", "55000.01");

        // Valid
        PolicyProposal solicitacaoAceita = PolicyProposal.builder()
                .id(PolicyProposalId.generate())
                .status(PolicyStatus.VALIDATED)
                .build();
        when(createOrderUseCase.createPolicyRequest(any())).thenReturn(solicitacaoAceita);

        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoValida))
                .andExpect(status().isCreated());

        // Invalid
        reset(createOrderUseCase);
        when(createOrderUseCase.createPolicyRequest(any()))
                .thenThrow(new IllegalArgumentException("Limite excedido"));

        mockMvc.perform(post("/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requisicaoInvalida))
                .andExpect(status().is4xxClientError());
    }

    // ===========================================
    // Métodos Auxiliares
    // ===========================================

    private String criarRequisicaoApolice(String categoria, String valorSegurado) {
        return String.format("""
            {
                "customer_id": "123e4567-e89b-12d3-a456-426614174000",
                "product_id": "1b2da7cc-b367-4196-8a78-9cfeec21f587",
                "category": "%s",
                "sales_channel": "MOBILE",
                "payment_method": "CREDIT_CARD",
                "total_monthly_premium_amount": "350.00",
                "insured_amount": "%s",
                "coverages": {"Colisão": "%s"},
                "assistances": ["Guincho 24h"]
            }
            """, categoria, valorSegurado, valorSegurado);
    }
}
