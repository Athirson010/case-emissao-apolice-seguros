# Templates com Builders Semânticos

Este diretório contém builders semânticos para facilitar a criação de dados de teste nos testes de componentes.

## Objetivo

Substituir o uso de fixtures estáticas (`TestDataFixtures`) por builders fluentes que:
- São mais expressivos e legíveis
- Facilitam a criação de cenários complexos
- Permitem customização fácil de valores
- Documentam o domínio através do código

## Builders Disponíveis

### 1. PolicyRequestTemplateBuilder

Builder para criar requisições de apólice.

**Métodos Factory (Cenários Pré-definidos):**
```java
// Apólices REGULAR (dentro dos limites)
PolicyRequestTemplateBuilder.autoRegular()
PolicyRequestTemplateBuilder.vidaRegular()
PolicyRequestTemplateBuilder.residencialRegular()
PolicyRequestTemplateBuilder.empresarialRegular()

// Apólices HIGH_RISK
PolicyRequestTemplateBuilder.autoHighRisk()

// Apólices que EXCEDEM limites
PolicyRequestTemplateBuilder.autoExceedsRegularLimit()
PolicyRequestTemplateBuilder.vidaExceedsRegularLimit()

// Apólices PREFERENTIAL
PolicyRequestTemplateBuilder.vidaPreferential()

// Apólices NO_INFORMATION
PolicyRequestTemplateBuilder.autoNoInformation()
```

**Exemplo de Uso:**
```java
// Criar apólice de AUTO regular
String json = PolicyRequestTemplateBuilder.autoRegular()
    .withCustomerId("custom-uuid")
    .withInsuredAmount(new BigDecimal("250000.00"))
    .buildAsJson();

// Criar apólice de VIDA que excede limite
Map<String, Object> request = PolicyRequestTemplateBuilder.vidaExceedsRegularLimit()
    .buildAsMap();
```

### 2. PaymentConfirmationEventBuilder

Builder para criar eventos de confirmação de pagamento.

**Métodos Factory:**
```java
PaymentConfirmationEventBuilder.approved(policyId)
PaymentConfirmationEventBuilder.rejected(policyId, reason)
PaymentConfirmationEventBuilder.rejectedInsufficientFunds(policyId)
PaymentConfirmationEventBuilder.rejectedInvalidCard(policyId)
```

**Exemplo de Uso:**
```java
// Evento de pagamento aprovado
String json = PaymentConfirmationEventBuilder.approved(policyId)
    .withAmount("500.00")
    .buildAsJson();

// Evento de pagamento rejeitado
String json = PaymentConfirmationEventBuilder.rejectedInsufficientFunds(policyId)
    .buildAsJson();
```

### 3. SubscriptionConfirmationEventBuilder

Builder para criar eventos de confirmação de subscrição.

**Métodos Factory:**
```java
SubscriptionConfirmationEventBuilder.approved(policyId)
SubscriptionConfirmationEventBuilder.rejected(policyId, reason)
SubscriptionConfirmationEventBuilder.rejectedHighRisk(policyId)
SubscriptionConfirmationEventBuilder.rejectedIncompleteDocumentation(policyId)
```

**Exemplo de Uso:**
```java
// Evento de subscrição aprovada
String json = SubscriptionConfirmationEventBuilder.approved(policyId)
    .buildAsJson();

// Evento de subscrição rejeitada
String json = SubscriptionConfirmationEventBuilder.rejectedHighRisk(policyId)
    .buildAsJson();
```

### 4. PolicyFlowScenarioBuilder

Builder para criar cenários completos de fluxo (policy + payment + subscription).

**Métodos Factory:**
```java
PolicyFlowScenarioBuilder.successfulFlow(policyId)
PolicyFlowScenarioBuilder.failedByPayment(policyId, reason)
PolicyFlowScenarioBuilder.failedBySubscription(policyId, reason)
PolicyFlowScenarioBuilder.autoRegularFlow()
PolicyFlowScenarioBuilder.vidaRegularFlow()
```

**Exemplo de Uso:**
```java
// Cenário de sucesso completo
PolicyFlowScenarioBuilder scenario = PolicyFlowScenarioBuilder.successfulFlow(policyId);

String policyJson = scenario.getPolicyBuilder().buildAsJson();
String paymentJson = scenario.getPaymentBuilder().buildAsJson();
String subscriptionJson = scenario.getSubscriptionBuilder().buildAsJson();

// Cenário customizado
PolicyFlowScenarioBuilder scenario = PolicyFlowScenarioBuilder
    .customFlow(PolicyRequestTemplateBuilder.vidaPreferential())
    .withPaymentApproved(policyId)
    .withSubscriptionApproved(policyId);
```

## Vantagens sobre TestDataFixtures

### Antes (TestDataFixtures):
```java
// Dados estáticos, difíceis de customizar
String json = TestDataFixtures.createSamplePolicyRequestJson();
// Não fica claro qual o cenário sendo testado
```

### Agora (Templates):
```java
// Expressivo e semântico
String json = PolicyRequestTemplateBuilder.autoRegular()
    .withInsuredAmount(new BigDecimal("300000.00"))
    .buildAsJson();
// Fica claro: estamos testando uma apólice AUTO regular com 300k segurado
```

## Cobertura de Regras de Negócio

Os builders cobrem todos os cenários do `validation-rules.json`:

- ✅ Todas as categorias (AUTO, VIDA, RESIDENCIAL, EMPRESARIAL, OUTROS)
- ✅ Todas as classificações de risco (REGULAR, HIGH_RISK, PREFERENTIAL, NO_INFORMATION)
- ✅ Limites de capital segurado (16 combinações)
- ✅ Estados de pagamento (APPROVED, REJECTED)
- ✅ Estados de subscrição (APPROVED, REJECTED)
- ✅ Fluxos completos (sucesso, falha por pagamento, falha por subscrição)

## Boas Práticas

1. **Use métodos factory quando possível**: `PolicyRequestTemplateBuilder.autoRegular()` é mais claro que `new PolicyRequestTemplateBuilder().withCategory("AUTO")...`

2. **Nomeie cenários de forma descritiva**: Prefira `successfulFlow()` a `defaultFlow()`

3. **Customize apenas o necessário**: Use valores padrão e customize apenas o que é relevante para o teste

4. **Documente novos builders**: Se adicionar novos builders, documente-os aqui
