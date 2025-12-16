package io.github.athirson010.adapters.out.persistence.mongo;

import io.github.athirson010.adapters.out.persistence.mongo.document.*;
import io.github.athirson010.adapters.out.persistence.mongo.mapper.PolicyProposalEntityMapper;
import io.github.athirson010.adapters.out.persistence.mongo.repository.PolicyProposalMongoRepository;
import io.github.athirson010.domain.enums.Category;
import io.github.athirson010.domain.enums.PaymentMethod;
import io.github.athirson010.domain.enums.PolicyStatus;
import io.github.athirson010.domain.enums.SalesChannel;
import io.github.athirson010.domain.model.Money;
import io.github.athirson010.domain.model.PolicyProposal;
import io.github.athirson010.domain.model.PolicyProposalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderMongoAdapter - Testes Unitários")
class OrderMongoAdapterTest {

    @Mock
    private PolicyProposalMongoRepository mongoRepository;

    @Mock
    private PolicyProposalEntityMapper mapper;

    @InjectMocks
    private OrderMongoAdapter orderMongoAdapter;

    private PolicyProposal policyProposal;
    private PolicyProposalEntity policyProposalEntity;
    private String policyId;

    @BeforeEach
    void setUp() {
        policyId = UUID.randomUUID().toString();

        // Domain model
        policyProposal = PolicyProposal.builder()
                .id(PolicyProposalId.from(policyId))
                .customerId(UUID.randomUUID())
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

        // Entity
        policyProposalEntity = PolicyProposalEntity.builder()
                .id(policyId)
                .customerId(policyProposal.getCustomerId().toString())
                .productId("PROD-AUTO-2024")
                .category("AUTO")
                .salesChannel("MOBILE")
                .paymentMethod("CREDIT_CARD")
                .totalMonthlyPremiumAmount(MoneyEntity.builder()
                        .amount(BigDecimal.valueOf(350.00))
                        .currency("BRL")
                        .build())
                .insuredAmount(MoneyEntity.builder()
                        .amount(BigDecimal.valueOf(200000.00))
                        .currency("BRL")
                        .build())
                .coverages(List.of(CoverageEntity.builder()
                        .coverageName("COLISAO")
                        .coverageAmount(MoneyEntity.builder()
                                .amount(BigDecimal.valueOf(200000.00))
                                .currency("BRL")
                                .build())
                        .build()))
                .assistances(List.of(AssistanceEntity.builder()
                        .assistanceName("GUINCHO_24H")
                        .build()))
                .status("RECEIVED")
                .createdAt(Instant.now())
                .statusHistory(List.of())
                .build();
    }

    @Test
    @DisplayName("Deve salvar proposta de apólice com sucesso")
    void deveSalvarPropostaDeApoliceComSucesso() {
        // Given
        when(mapper.toEntity(policyProposal)).thenReturn(policyProposalEntity);
        when(mongoRepository.save(policyProposalEntity)).thenReturn(policyProposalEntity);
        when(mapper.toDomain(policyProposalEntity)).thenReturn(policyProposal);

        // When
        PolicyProposal result = orderMongoAdapter.save(policyProposal);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(policyProposal.getId());
        assertThat(result.getCustomerId()).isEqualTo(policyProposal.getCustomerId());
        assertThat(result.getProductId()).isEqualTo(policyProposal.getProductId());
        assertThat(result.getStatus()).isEqualTo(policyProposal.getStatus());

        verify(mapper).toEntity(policyProposal);
        verify(mongoRepository).save(policyProposalEntity);
        verify(mapper).toDomain(policyProposalEntity);
    }

    @Test
    @DisplayName("Deve buscar proposta de apólice por ID com sucesso")
    void deveBuscarPropostaDeApolicePorIdComSucesso() {
        // Given
        PolicyProposalId id = PolicyProposalId.from(policyId);
        when(mongoRepository.findById(policyId)).thenReturn(Optional.of(policyProposalEntity));
        when(mapper.toDomain(policyProposalEntity)).thenReturn(policyProposal);

        // When
        Optional<PolicyProposal> result = orderMongoAdapter.findById(id);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
        assertThat(result.get().getCustomerId()).isEqualTo(policyProposal.getCustomerId());
        assertThat(result.get().getProductId()).isEqualTo(policyProposal.getProductId());

        verify(mongoRepository).findById(policyId);
        verify(mapper).toDomain(policyProposalEntity);
    }

    @Test
    @DisplayName("Deve retornar Optional vazio quando proposta não for encontrada")
    void deveRetornarOptionalVazioQuandoPropostaNaoForEncontrada() {
        // Given
        PolicyProposalId id = PolicyProposalId.from(policyId);
        when(mongoRepository.findById(policyId)).thenReturn(Optional.empty());

        // When
        Optional<PolicyProposal> result = orderMongoAdapter.findById(id);

        // Then
        assertThat(result).isEmpty();

        verify(mongoRepository).findById(policyId);
        verify(mapper, never()).toDomain(any());
    }

    @Test
    @DisplayName("Deve salvar proposta de apólice no estado VALIDATED")
    void deveSalvarPropostaDeApoliceNoEstadoValidated() {
        // Given
        policyProposal = PolicyProposal.builder()
                .id(PolicyProposalId.from(policyId))
                .customerId(UUID.randomUUID())
                .productId("PROD-VIDA-2024")
                .category(Category.VIDA)
                .salesChannel(SalesChannel.WEB)
                .paymentMethod(PaymentMethod.PIX)
                .totalMonthlyPremiumAmount(Money.brl(BigDecimal.valueOf(500.00)))
                .insuredAmount(Money.brl(BigDecimal.valueOf(300000.00)))
                .coverages(Map.of("MORTE", Money.brl(BigDecimal.valueOf(300000.00))))
                .assistances(List.of("ASSISTENCIA_FUNERAL"))
                .status(PolicyStatus.VALIDATED)
                .createdAt(Instant.now())
                .build();

        policyProposalEntity = PolicyProposalEntity.builder()
                .id(policyId)
                .customerId(policyProposal.getCustomerId().toString())
                .productId("PROD-VIDA-2024")
                .category("VIDA")
                .status("VALIDATED")
                .build();

        when(mapper.toEntity(policyProposal)).thenReturn(policyProposalEntity);
        when(mongoRepository.save(policyProposalEntity)).thenReturn(policyProposalEntity);
        when(mapper.toDomain(policyProposalEntity)).thenReturn(policyProposal);

        // When
        PolicyProposal result = orderMongoAdapter.save(policyProposal);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PolicyStatus.VALIDATED);

        verify(mapper).toEntity(policyProposal);
        verify(mongoRepository).save(policyProposalEntity);
        verify(mapper).toDomain(policyProposalEntity);
    }

    @Test
    @DisplayName("Deve salvar proposta de apólice no estado PENDING")
    void deveSalvarPropostaDeApoliceNoEstadoPending() {
        // Given
        policyProposal = PolicyProposal.builder()
                .id(PolicyProposalId.from(policyId))
                .customerId(UUID.randomUUID())
                .productId("PROD-RESIDENCIAL-2024")
                .category(Category.RESIDENCIAL)
                .salesChannel(SalesChannel.WHATSAPP)
                .paymentMethod(PaymentMethod.BOLETO)
                .totalMonthlyPremiumAmount(Money.brl(BigDecimal.valueOf(150.00)))
                .insuredAmount(Money.brl(BigDecimal.valueOf(100000.00)))
                .coverages(Map.of("INCENDIO", Money.brl(BigDecimal.valueOf(100000.00))))
                .assistances(List.of("ELETRICISTA", "ENCANADOR"))
                .status(PolicyStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        policyProposalEntity = PolicyProposalEntity.builder()
                .id(policyId)
                .customerId(policyProposal.getCustomerId().toString())
                .productId("PROD-RESIDENCIAL-2024")
                .category("RESIDENCIAL")
                .status("PENDING")
                .build();

        when(mapper.toEntity(policyProposal)).thenReturn(policyProposalEntity);
        when(mongoRepository.save(policyProposalEntity)).thenReturn(policyProposalEntity);
        when(mapper.toDomain(policyProposalEntity)).thenReturn(policyProposal);

        // When
        PolicyProposal result = orderMongoAdapter.save(policyProposal);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PolicyStatus.PENDING);

        verify(mapper).toEntity(policyProposal);
        verify(mongoRepository).save(policyProposalEntity);
    }

    @Test
    @DisplayName("Deve salvar proposta de apólice no estado APPROVED")
    void deveSalvarPropostaDeApoliceNoEstadoApproved() {
        // Given
        Instant now = Instant.now();
        policyProposal = PolicyProposal.builder()
                .id(PolicyProposalId.from(policyId))
                .customerId(UUID.randomUUID())
                .productId("PROD-EMPRESARIAL-2024")
                .category(Category.EMPRESARIAL)
                .salesChannel(SalesChannel.WEB)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .totalMonthlyPremiumAmount(Money.brl(BigDecimal.valueOf(1000.00)))
                .insuredAmount(Money.brl(BigDecimal.valueOf(500000.00)))
                .coverages(Map.of("RESPONSABILIDADE_CIVIL", Money.brl(BigDecimal.valueOf(500000.00))))
                .assistances(List.of("CONSULTORIA_JURIDICA"))
                .status(PolicyStatus.APPROVED)
                .createdAt(now)
                .finishedAt(now)
                .build();

        policyProposalEntity = PolicyProposalEntity.builder()
                .id(policyId)
                .customerId(policyProposal.getCustomerId().toString())
                .productId("PROD-EMPRESARIAL-2024")
                .category("EMPRESARIAL")
                .status("APPROVED")
                .finishedAt(now)
                .build();

        when(mapper.toEntity(policyProposal)).thenReturn(policyProposalEntity);
        when(mongoRepository.save(policyProposalEntity)).thenReturn(policyProposalEntity);
        when(mapper.toDomain(policyProposalEntity)).thenReturn(policyProposal);

        // When
        PolicyProposal result = orderMongoAdapter.save(policyProposal);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PolicyStatus.APPROVED);
        assertThat(result.getFinishedAt()).isNotNull();

        verify(mapper).toEntity(policyProposal);
        verify(mongoRepository).save(policyProposalEntity);
    }

    @Test
    @DisplayName("Deve salvar proposta de apólice no estado REJECTED")
    void deveSalvarPropostaDeApoliceNoEstadoRejected() {
        // Given
        Instant now = Instant.now();
        policyProposal = PolicyProposal.builder()
                .id(PolicyProposalId.from(policyId))
                .customerId(UUID.randomUUID())
                .productId("PROD-AUTO-2024")
                .category(Category.AUTO)
                .salesChannel(SalesChannel.MOBILE)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .totalMonthlyPremiumAmount(Money.brl(BigDecimal.valueOf(350.00)))
                .insuredAmount(Money.brl(BigDecimal.valueOf(200000.00)))
                .coverages(Map.of("COLISAO", Money.brl(BigDecimal.valueOf(200000.00))))
                .assistances(List.of("GUINCHO_24H"))
                .status(PolicyStatus.REJECTED)
                .createdAt(now)
                .finishedAt(now)
                .build();

        policyProposalEntity = PolicyProposalEntity.builder()
                .id(policyId)
                .customerId(policyProposal.getCustomerId().toString())
                .productId("PROD-AUTO-2024")
                .category("AUTO")
                .status("REJECTED")
                .finishedAt(now)
                .build();

        when(mapper.toEntity(policyProposal)).thenReturn(policyProposalEntity);
        when(mongoRepository.save(policyProposalEntity)).thenReturn(policyProposalEntity);
        when(mapper.toDomain(policyProposalEntity)).thenReturn(policyProposal);

        // When
        PolicyProposal result = orderMongoAdapter.save(policyProposal);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PolicyStatus.REJECTED);
        assertThat(result.getFinishedAt()).isNotNull();

        verify(mapper).toEntity(policyProposal);
        verify(mongoRepository).save(policyProposalEntity);
    }

    @Test
    @DisplayName("Deve salvar proposta de apólice com múltiplas coberturas")
    void deveSalvarPropostaDeApoliceComMultiplasCoberturas() {
        // Given
        policyProposal = PolicyProposal.builder()
                .id(PolicyProposalId.from(policyId))
                .customerId(UUID.randomUUID())
                .productId("PROD-AUTO-PREMIUM-2024")
                .category(Category.AUTO)
                .salesChannel(SalesChannel.MOBILE)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .totalMonthlyPremiumAmount(Money.brl(BigDecimal.valueOf(800.00)))
                .insuredAmount(Money.brl(BigDecimal.valueOf(250000.00)))
                .coverages(Map.of(
                        "COLISAO", Money.brl(BigDecimal.valueOf(250000.00)),
                        "ROUBO", Money.brl(BigDecimal.valueOf(200000.00)),
                        "INCENDIO", Money.brl(BigDecimal.valueOf(150000.00))
                ))
                .assistances(List.of("GUINCHO_24H", "CARRO_RESERVA", "CHAVEIRO"))
                .status(PolicyStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();

        when(mapper.toEntity(policyProposal)).thenReturn(policyProposalEntity);
        when(mongoRepository.save(policyProposalEntity)).thenReturn(policyProposalEntity);
        when(mapper.toDomain(policyProposalEntity)).thenReturn(policyProposal);

        // When
        PolicyProposal result = orderMongoAdapter.save(policyProposal);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCoverages()).hasSize(3);
        assertThat(result.getCoverages()).containsKeys("COLISAO", "ROUBO", "INCENDIO");
        assertThat(result.getAssistances()).hasSize(3);

        verify(mapper).toEntity(policyProposal);
        verify(mongoRepository).save(policyProposalEntity);
    }

    @Test
    @DisplayName("Deve buscar proposta por ID com todas as categorias")
    void deveBuscarPropostaPorIdComTodasAsCategorias() {
        // Given - Testa categoria OUTROS
        policyProposal = PolicyProposal.builder()
                .id(PolicyProposalId.from(policyId))
                .customerId(UUID.randomUUID())
                .productId("PROD-OUTROS-2024")
                .category(Category.OUTROS)
                .salesChannel(SalesChannel.OUTROS)
                .paymentMethod(PaymentMethod.DEBIT)
                .totalMonthlyPremiumAmount(Money.brl(BigDecimal.valueOf(100.00)))
                .insuredAmount(Money.brl(BigDecimal.valueOf(50000.00)))
                .coverages(Map.of("COBERTURA_BASICA", Money.brl(BigDecimal.valueOf(50000.00))))
                .assistances(List.of("ASSISTENCIA_GERAL"))
                .status(PolicyStatus.RECEIVED)
                .createdAt(Instant.now())
                .build();

        PolicyProposalId id = PolicyProposalId.from(policyId);
        when(mongoRepository.findById(policyId)).thenReturn(Optional.of(policyProposalEntity));
        when(mapper.toDomain(policyProposalEntity)).thenReturn(policyProposal);

        // When
        Optional<PolicyProposal> result = orderMongoAdapter.findById(id);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getCategory()).isEqualTo(Category.OUTROS);

        verify(mongoRepository).findById(policyId);
        verify(mapper).toDomain(policyProposalEntity);
    }
}
