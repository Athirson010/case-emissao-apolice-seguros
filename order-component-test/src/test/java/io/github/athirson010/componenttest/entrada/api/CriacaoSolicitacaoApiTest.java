package io.github.athirson010.componenttest.entrada.api;

import io.github.athirson010.componenttest.BaseComponentTest;
import io.github.athirson010.domain.model.PolicyProposal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc
@ActiveProfiles({"test", "api"})
@DisplayName("Entrada API - Criação de Solicitação de Apólice")
class CriacaoSolicitacaoApiTest extends BaseComponentTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        // Configurar comportamento padrão dos mocks das portas de saída
        // O repository deve salvar e retornar a proposta
        when(orderRepository.save(any(PolicyProposal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // O fraud queue port não faz nada (void)
        doNothing().when(fraudQueuePort).sendToFraudQueue(any(PolicyProposal.class));
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

        // When - Chamada à API
        mockMvc.perform(post("/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requisicaoValida))
                .andDo(print())

                // Then - Validações
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.policy_request_id").exists())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.created_at").exists());

        // Verify - Repository foi chamado para salvar
        verify(orderRepository, times(1)).save(any(PolicyProposal.class));

        // Verify - Mensagem foi enviada para fila de fraude
        verify(fraudQueuePort, times(1)).sendToFraudQueue(any(PolicyProposal.class));
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

        // Verify - Repository NÃO deve ser chamado devido à validação
        verify(orderRepository, never()).save(any());
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

        verify(orderRepository, never()).save(any());
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
}
