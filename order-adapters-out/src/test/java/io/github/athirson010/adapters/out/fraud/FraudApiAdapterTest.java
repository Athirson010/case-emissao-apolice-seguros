package io.github.athirson010.adapters.out.fraud;

import io.github.athirson010.adapters.out.fraud.dto.FraudAnalysisResponseDto;
import io.github.athirson010.adapters.out.fraud.mapper.FraudAnalysisMapper;
import io.github.athirson010.domain.enums.Category;
import io.github.athirson010.domain.enums.PaymentMethod;
import io.github.athirson010.domain.enums.PolicyStatus;
import io.github.athirson010.domain.enums.RiskClassification;
import io.github.athirson010.domain.enums.SalesChannel;
import io.github.athirson010.domain.model.FraudAnalysisResult;
import io.github.athirson010.domain.model.Money;
import io.github.athirson010.domain.model.PolicyProposal;
import io.github.athirson010.domain.model.PolicyProposalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudApiAdapter - Testes Unitários")
class FraudApiAdapterTest {

    @Mock
    private FraudAnalysisMapper mapper;

    @InjectMocks
    private FraudApiAdapter fraudApiAdapter;

    private PolicyProposal policyProposal;
    private FraudAnalysisResult mockAnalysisResult;

    @BeforeEach
    void setUp() {
        mockAnalysisResult = FraudAnalysisResult.builder()
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .analyzedAt(Instant.now())
                .classification(RiskClassification.REGULAR)
                .occurrences(List.of())
                .build();
    }

    private PolicyProposal createPolicyProposal(UUID customerId) {
        return PolicyProposal.builder()
                .id(PolicyProposalId.generate())
                .customerId(customerId)
                .productId("PROD-AUTO-2024")
                .category(Category.AUTO)
                .salesChannel(SalesChannel.MOBILE)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .totalMonthlyPremiumAmount(Money.brl(BigDecimal.valueOf(350.00)))
                .insuredAmount(Money.brl(BigDecimal.valueOf(200000.00)))
                .coverages(Map.of("COLISAO", Money.brl(BigDecimal.valueOf(200000.00))))
                .assistances(List.of("GUINCHO_24H"))
                .status(PolicyStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Deve analisar fraude com classificação REGULAR")
    void deveAnalisarFraudeComClassificacaoRegular() {
        // Given - Busca UUID que resulta em REGULAR (hash % 4 == 0)
        UUID customerId = findCustomerIdForClassification(RiskClassification.REGULAR);
        policyProposal = createPolicyProposal(customerId);

        when(mapper.toDomain(any(FraudAnalysisResponseDto.class)))
                .thenReturn(mockAnalysisResult);

        // When
        FraudAnalysisResult result = fraudApiAdapter.analyzeFraud(policyProposal);

        // Then
        assertThat(result).isNotNull();

        ArgumentCaptor<FraudAnalysisResponseDto> captor = ArgumentCaptor.forClass(FraudAnalysisResponseDto.class);
        verify(mapper).toDomain(captor.capture());

        FraudAnalysisResponseDto capturedDto = captor.getValue();
        assertThat(capturedDto.getCustomerId()).isEqualTo(customerId);
        assertThat(capturedDto.getClassification()).isEqualTo(RiskClassification.REGULAR);
        assertThat(capturedDto.getOccurrences()).isEmpty();
        assertThat(capturedDto.getAnalyzedAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve analisar fraude com classificação HIGH_RISK e gerar 2 ocorrências")
    void deveAnalisarFraudeComClassificacaoHighRiskEGerar2Ocorrencias() {
        // Given - Busca UUID que resulta em HIGH_RISK (hash % 4 == 1)
        UUID customerId = findCustomerIdForClassification(RiskClassification.HIGH_RISK);
        policyProposal = createPolicyProposal(customerId);

        when(mapper.toDomain(any(FraudAnalysisResponseDto.class)))
                .thenReturn(mockAnalysisResult);

        // When
        FraudAnalysisResult result = fraudApiAdapter.analyzeFraud(policyProposal);

        // Then
        assertThat(result).isNotNull();

        ArgumentCaptor<FraudAnalysisResponseDto> captor = ArgumentCaptor.forClass(FraudAnalysisResponseDto.class);
        verify(mapper).toDomain(captor.capture());

        FraudAnalysisResponseDto capturedDto = captor.getValue();
        assertThat(capturedDto.getCustomerId()).isEqualTo(customerId);
        assertThat(capturedDto.getClassification()).isEqualTo(RiskClassification.HIGH_RISK);
        assertThat(capturedDto.getOccurrences()).hasSize(2);

        // Verifica primeira ocorrência (FRAUD)
        assertThat(capturedDto.getOccurrences().get(0).getType().name()).isEqualTo("FRAUD");
        assertThat(capturedDto.getOccurrences().get(0).getDescription())
                .isEqualTo("Attempted fraudulent transaction detected");
        assertThat(capturedDto.getOccurrences().get(0).getProductId()).isEqualTo("PROD-AUTO-2024");

        // Verifica segunda ocorrência (SUSPICION)
        assertThat(capturedDto.getOccurrences().get(1).getType().name()).isEqualTo("SUSPICION");
        assertThat(capturedDto.getOccurrences().get(1).getDescription())
                .isEqualTo("Unusual activity flagged for review");
    }

    @Test
    @DisplayName("Deve analisar fraude com classificação PREFERENTIAL sem ocorrências")
    void deveAnalisarFraudeComClassificacaoPreferentialSemOcorrencias() {
        // Given - Busca UUID que resulta em PREFERENTIAL (hash % 4 == 2)
        UUID customerId = findCustomerIdForClassification(RiskClassification.PREFERENTIAL);
        policyProposal = createPolicyProposal(customerId);

        when(mapper.toDomain(any(FraudAnalysisResponseDto.class)))
                .thenReturn(mockAnalysisResult);

        // When
        FraudAnalysisResult result = fraudApiAdapter.analyzeFraud(policyProposal);

        // Then
        assertThat(result).isNotNull();

        ArgumentCaptor<FraudAnalysisResponseDto> captor = ArgumentCaptor.forClass(FraudAnalysisResponseDto.class);
        verify(mapper).toDomain(captor.capture());

        FraudAnalysisResponseDto capturedDto = captor.getValue();
        assertThat(capturedDto.getCustomerId()).isEqualTo(customerId);
        assertThat(capturedDto.getClassification()).isEqualTo(RiskClassification.PREFERENTIAL);
        assertThat(capturedDto.getOccurrences()).isEmpty();
    }

    @Test
    @DisplayName("Deve analisar fraude com classificação NO_INFORMATION e gerar 1 ocorrência de suspeita")
    void deveAnalisarFraudeComClassificacaoNoInformationEGerar1Ocorrencia() {
        // Given - Busca UUID que resulta em NO_INFORMATION (hash % 4 == 3)
        UUID customerId = findCustomerIdForClassification(RiskClassification.NO_INFORMATION);
        policyProposal = createPolicyProposal(customerId);

        when(mapper.toDomain(any(FraudAnalysisResponseDto.class)))
                .thenReturn(mockAnalysisResult);

        // When
        FraudAnalysisResult result = fraudApiAdapter.analyzeFraud(policyProposal);

        // Then
        assertThat(result).isNotNull();

        ArgumentCaptor<FraudAnalysisResponseDto> captor = ArgumentCaptor.forClass(FraudAnalysisResponseDto.class);
        verify(mapper).toDomain(captor.capture());

        FraudAnalysisResponseDto capturedDto = captor.getValue();
        assertThat(capturedDto.getCustomerId()).isEqualTo(customerId);
        assertThat(capturedDto.getClassification()).isEqualTo(RiskClassification.NO_INFORMATION);
        assertThat(capturedDto.getOccurrences()).hasSize(1);

        // Verifica ocorrência (SUSPICION)
        assertThat(capturedDto.getOccurrences().get(0).getType().name()).isEqualTo("SUSPICION");
        assertThat(capturedDto.getOccurrences().get(0).getDescription())
                .isEqualTo("Customer has limited history with the insurer");
        assertThat(capturedDto.getOccurrences().get(0).getProductId()).isEqualTo("PROD-AUTO-2024");
    }

    @Test
    @DisplayName("Deve incluir orderId correto na resposta da análise")
    void deveIncluirOrderIdCorretoNaRespostaDaAnalise() {
        // Given
        UUID customerId = UUID.randomUUID();
        policyProposal = createPolicyProposal(customerId);
        String expectedOrderId = policyProposal.getId().asString();

        when(mapper.toDomain(any(FraudAnalysisResponseDto.class)))
                .thenReturn(mockAnalysisResult);

        // When
        fraudApiAdapter.analyzeFraud(policyProposal);

        // Then
        ArgumentCaptor<FraudAnalysisResponseDto> captor = ArgumentCaptor.forClass(FraudAnalysisResponseDto.class);
        verify(mapper).toDomain(captor.capture());

        FraudAnalysisResponseDto capturedDto = captor.getValue();
        assertThat(capturedDto.getOrderId().toString()).isEqualTo(expectedOrderId);
    }

    @Test
    @DisplayName("Deve incluir timestamp de análise na resposta")
    void deveIncluirTimestampDeAnaliseNaResposta() {
        // Given
        UUID customerId = UUID.randomUUID();
        policyProposal = createPolicyProposal(customerId);
        Instant before = Instant.now();

        when(mapper.toDomain(any(FraudAnalysisResponseDto.class)))
                .thenReturn(mockAnalysisResult);

        // When
        fraudApiAdapter.analyzeFraud(policyProposal);

        Instant after = Instant.now();

        // Then
        ArgumentCaptor<FraudAnalysisResponseDto> captor = ArgumentCaptor.forClass(FraudAnalysisResponseDto.class);
        verify(mapper).toDomain(captor.capture());

        FraudAnalysisResponseDto capturedDto = captor.getValue();
        assertThat(capturedDto.getAnalyzedAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("Deve analisar proposta com diferentes produtos")
    void deveAnalisarPropostaComDiferentesProdutos() {
        // Given
        UUID customerId = findCustomerIdForClassification(RiskClassification.HIGH_RISK);
        policyProposal = PolicyProposal.builder()
                .id(PolicyProposalId.generate())
                .customerId(customerId)
                .productId("PROD-VIDA-2024")
                .category(Category.VIDA)
                .salesChannel(SalesChannel.WEB)
                .paymentMethod(PaymentMethod.PIX)
                .totalMonthlyPremiumAmount(Money.brl(BigDecimal.valueOf(500.00)))
                .insuredAmount(Money.brl(BigDecimal.valueOf(300000.00)))
                .coverages(Map.of("MORTE", Money.brl(BigDecimal.valueOf(300000.00))))
                .assistances(List.of("ASSISTENCIA_FUNERAL"))
                .status(PolicyStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();

        when(mapper.toDomain(any(FraudAnalysisResponseDto.class)))
                .thenReturn(mockAnalysisResult);

        // When
        fraudApiAdapter.analyzeFraud(policyProposal);

        // Then
        ArgumentCaptor<FraudAnalysisResponseDto> captor = ArgumentCaptor.forClass(FraudAnalysisResponseDto.class);
        verify(mapper).toDomain(captor.capture());

        FraudAnalysisResponseDto capturedDto = captor.getValue();
        assertThat(capturedDto.getOccurrences()).hasSize(2);
        assertThat(capturedDto.getOccurrences().get(0).getProductId()).isEqualTo("PROD-VIDA-2024");
        assertThat(capturedDto.getOccurrences().get(1).getProductId()).isEqualTo("PROD-VIDA-2024");
    }

    @Test
    @DisplayName("Deve gerar IDs únicos para cada ocorrência")
    void deveGerarIdsUnicosParaCadaOcorrencia() {
        // Given
        UUID customerId = findCustomerIdForClassification(RiskClassification.HIGH_RISK);
        policyProposal = createPolicyProposal(customerId);

        when(mapper.toDomain(any(FraudAnalysisResponseDto.class)))
                .thenReturn(mockAnalysisResult);

        // When
        fraudApiAdapter.analyzeFraud(policyProposal);

        // Then
        ArgumentCaptor<FraudAnalysisResponseDto> captor = ArgumentCaptor.forClass(FraudAnalysisResponseDto.class);
        verify(mapper).toDomain(captor.capture());

        FraudAnalysisResponseDto capturedDto = captor.getValue();
        assertThat(capturedDto.getOccurrences()).hasSize(2);

        String occurrenceId1 = capturedDto.getOccurrences().get(0).getId();
        String occurrenceId2 = capturedDto.getOccurrences().get(1).getId();

        assertThat(occurrenceId1).isNotNull();
        assertThat(occurrenceId2).isNotNull();
        assertThat(occurrenceId1).isNotEqualTo(occurrenceId2);
    }

    @Test
    @DisplayName("Deve chamar mapper com DTO correto")
    void deveChamarMapperComDtoCorreto() {
        // Given
        UUID customerId = UUID.randomUUID();
        policyProposal = createPolicyProposal(customerId);

        when(mapper.toDomain(any(FraudAnalysisResponseDto.class)))
                .thenReturn(mockAnalysisResult);

        // When
        FraudAnalysisResult result = fraudApiAdapter.analyzeFraud(policyProposal);

        // Then
        assertThat(result).isEqualTo(mockAnalysisResult);
        verify(mapper, times(1)).toDomain(any(FraudAnalysisResponseDto.class));
    }

    /**
     * Método auxiliar para encontrar um UUID que resulte na classificação desejada.
     * Usa a mesma lógica do FraudApiAdapter para garantir consistência.
     */
    private UUID findCustomerIdForClassification(RiskClassification targetClassification) {
        for (int i = 0; i < 1000; i++) {
            UUID candidate = UUID.randomUUID();
            int hash = Math.abs(candidate.hashCode() % 4);

            RiskClassification classification = switch (hash) {
                case 0 -> RiskClassification.REGULAR;
                case 1 -> RiskClassification.HIGH_RISK;
                case 2 -> RiskClassification.PREFERENTIAL;
                default -> RiskClassification.NO_INFORMATION;
            };

            if (classification == targetClassification) {
                return candidate;
            }
        }

        throw new IllegalStateException("Could not find UUID for classification: " + targetClassification);
    }
}
