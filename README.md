<div align="center">

# Sistema de Emissão de Apólices de Seguros

![Itaú App](docs/itau-app.jpeg)

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-green.svg)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-7.0-green.svg)](https://www.mongodb.com/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.13-orange.svg)](https://www.rabbitmq.com/)
[![Kafka](https://img.shields.io/badge/Kafka-7.5.0-black.svg)](https://kafka.apache.org/)
[![Grafana](https://img.shields.io/badge/Grafana-10.2.3-orange.svg)](https://grafana.com/)
[![Loki](https://img.shields.io/badge/Loki-2.9.3-yellow.svg)](https://grafana.com/oss/loki/)
[![Tempo](https://img.shields.io/badge/Tempo-2.3.1-purple.svg)](https://grafana.com/oss/tempo/)
[![Prometheus](https://img.shields.io/badge/Prometheus-2.48.1-red.svg)](https://prometheus.io/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

</div>

## 📋 Sobre o Projeto

Sistema robusto e escalável para emissão, gerenciamento e processamento de apólices de seguros, desenvolvido com foco em **Clean Architecture**, **Event-Driven Architecture** e boas práticas de desenvolvimento (SOLID, Clean Code).

### Tecnologias Utilizadas

- **MongoDB** para persistência de dados
- **RabbitMQ** para mensageria assíncrona (pagamentos e subscrições)
- **Spring Boot 3.2+** como framework base
- **Java 17+** com recursos modernos
- **Clean Architecture** (Ports & Adapters / Hexagonal Architecture)

---

## 🎯 Desafio Técnico - Resumo Executivo

### Objetivos Alcançados

✅ **Máquina de Estados Completa**: Implementação rigorosa de transições válidas e estados finais imutáveis
✅ **16 Regras de Validação**: 100% das regras do `validation-rules.json` implementadas e testadas
✅ **Consumers RabbitMQ**: Processamento de eventos de pagamento e subscrição
✅ **Testes de Componentes**: Cobertura completa do ciclo de vida das apólices
✅ **Templates com Builders**: Substituição de fixtures por builders semânticos
✅ **Documentação Completa**: Arquitetura, decisões técnicas e premissas documentadas

### Decisões de Escopo

✅ **Docker Compose**: Infraestrutura completa com MongoDB 7.0, RabbitMQ 3.13, Kafka 7.5.0 e Kafka UI
❌ **Apache Avro**: Optou-se por JSON para agilizar desenvolvimento e facilitar testes
❌ **Observabilidade Avançada**: Métricas e traces não foram implementados (fora do escopo)

---

## 🏗️ Arquitetura

### Decisões Arquiteturais

#### Clean Architecture (Ports & Adapters)

O projeto foi estruturado seguindo os princípios da **Clean Architecture**:

```
┌─────────────────────────────────────────────────┐
│           CAMADA DE DOMÍNIO (Core)              │
│  - Regras de negócio puras                      │
│  - Máquina de estados (PolicyProposal)          │
│  - Value Objects (Money, PolicyProposalId)      │
│  - Sem dependências externas                    │
└─────────────────────────────────────────────────┘
                      ↑
┌─────────────────────────────────────────────────┐
│        CAMADA DE APLICAÇÃO (Use Cases)          │
│  - Orquestração de casos de uso                 │
│  - PolicyValidationService (16 regras)          │
│  - Ports (interfaces para I/O)                  │
└─────────────────────────────────────────────────┘
                      ↑
┌─────────────────────────────────────────────────┐
│    CAMADA DE INFRAESTRUTURA (Adapters)          │
│  - Controllers REST (Adapters IN)               │
│  - Consumers RabbitMQ (Adapters IN)             │
│  - MongoDB Repository (Adapters OUT)            │
│  - Configurações Spring                         │
└─────────────────────────────────────────────────┘
```

**Benefícios**:
- ✅ **Testabilidade**: Domínio testável sem dependências externas
- ✅ **Flexibilidade**: Troca de tecnologias sem impacto no core
- ✅ **Manutenibilidade**: Separação clara de responsabilidades
- ✅ **Independência**: Domínio não conhece frameworks ou bibliotecas

---

## 🔄 Máquina de Estados

### Diagrama de Transições

```
RECEIVED → VALIDATED → PENDING → APPROVED ✓
    ↓           ↓          ↓
CANCELED    REJECTED   REJECTED
```

### Transições Válidas

| Estado Atual | Transições Permitidas | Restrições |
|--------------|----------------------|------------|
| **RECEIVED** | VALIDATED, CANCELED | Estado inicial |
| **VALIDATED** | PENDING, REJECTED | Após validação de fraude |
| **PENDING** | APPROVED, REJECTED | Aguarda pagamento E subscrição |
| **APPROVED** | - | Estado final (imutável) |
| **REJECTED** | - | Estado final (imutável) |
| **CANCELED** | - | Estado final (imutável) |

### Regras de Aprovação/Rejeição (Rejeição Imediata com Histórico Completo)

A apólice utiliza o conceito de **Rejeição Imediata com Histórico Completo**: se **QUALQUER** resposta (pagamento OU subscrição) for rejeitada, o status muda para **REJECTED imediatamente**. Mesmo após rejeitado, quando a segunda resposta chegar, ela é **registrada no histórico**.

#### Comportamento por Cenário:

| Evento 1 | Evento 2 | Status após E1 | Status após E2 | Histórico |
|----------|----------|----------------|----------------|-----------|
| ✅ Pagamento APPROVED | ✅ Subscrição APPROVED | **PENDING** | **APPROVED** ✓ | Ambas aprovadas |
| ✅ Pagamento APPROVED | ❌ Subscrição REJECTED | **PENDING** | **REJECTED** ✗ | Subscrição rejeitou |
| ❌ Pagamento REJECTED | ✅ Subscrição APPROVED | **REJECTED** ✗ | **REJECTED** | Pagamento rejeitou + Subscrição aprovada (após rejeição) |
| ❌ Pagamento REJECTED | ❌ Subscrição REJECTED | **REJECTED** ✗ | **REJECTED** | Ambas rejeitadas - 2 entradas no histórico |
| ✅ Subscrição APPROVED | ✅ Pagamento APPROVED | **PENDING** | **APPROVED** ✓ | Ambas aprovadas |
| ✅ Subscrição APPROVED | ❌ Pagamento REJECTED | **PENDING** | **REJECTED** ✗ | Pagamento rejeitou |
| ❌ Subscrição REJECTED | ✅ Pagamento APPROVED | **REJECTED** ✗ | **REJECTED** | Subscrição rejeitou + Pagamento aprovado (após rejeição) |
| ❌ Subscrição REJECTED | ❌ Pagamento REJECTED | **REJECTED** ✗ | **REJECTED** | Ambas rejeitadas - 2 entradas no histórico |

#### Regras:

✅ **APPROVED**: Somente quando **AMBAS** respostas forem **APPROVED**
❌ **REJECTED (Imediato)**: Quando **QUALQUER** resposta for **REJECTED** (não aguarda a segunda)
📋 **Histórico Completo**: **SEMPRE** registra resultado de AMBAS respostas, mesmo após rejeição

#### Exemplos de Histórico:

**Cenário 1: Pagamento rejeitado, depois subscrição aprovada**
```
1. RECEIVED
2. VALIDATED
3. PENDING
4. REJECTED - "Pagamento rejeitado: Fundos insuficientes"
5. REJECTED - "Subscrição aprovada (após rejeição por pagamento)"
```

**Cenário 2: Ambas rejeitadas**
```
1. RECEIVED
2. VALIDATED
3. PENDING
4. REJECTED - "Pagamento rejeitado: Cartão inválido"
5. REJECTED - "Subscrição rejeitada: Alto risco"
```

**Cenário 3: Ambas aprovadas**
```
1. RECEIVED
2. VALIDATED
3. PENDING
4. APPROVED
```

Qualquer tentativa de transição inválida resulta em `InvalidTransitionException`.

**Implementação**:
- `order-domain/.../PolicyProposal.java:121-226` (processPaymentResponse, processSubscriptionResponse)
- `order-adapters-in/.../PaymentConfirmationConsumer.java:46-83`
- `order-adapters-in/.../SubscriptionConfirmationConsumer.java:46-83`

**Testes**: `order-domain/.../PolicyProposalDualConfirmationTest.java` (17 testes cobrindo todos os cenários)

---

## 📐 Regras de Negócio (validation-rules.json)

### Limites de Capital Segurado por Classificação de Risco

O sistema implementa **16 regras de validação** (4 classificações × 4 categorias):

#### Cliente REGULAR

| Categoria | Limite | Operador |
|-----------|--------|----------|
| VIDA, RESIDENCIAL | R$ 500.000 | ≤ |
| AUTO | R$ 350.000 | ≤ |
| EMPRESARIAL | R$ 255.000 | ≤ |
| OUTROS | R$ 100.000 | ≤ |

#### Cliente HIGH_RISK

| Categoria | Limite | Operador |
|-----------|--------|----------|
| AUTO | R$ 250.000 | ≤ |
| RESIDENCIAL | R$ 150.000 | ≤ |
| VIDA, EMPRESARIAL | R$ 125.000 | ≤ |
| OUTROS | R$ 50.000 | ≤ |

#### Cliente PREFERENTIAL

| Categoria | Limite | Operador |
|-----------|--------|----------|
| VIDA | R$ 800.000 | < (estritamente menor) |
| AUTO, RESIDENCIAL | R$ 450.000 | < (estritamente menor) |
| EMPRESARIAL | R$ 375.000 | ≤ |
| OUTROS | R$ 300.000 | ≤ |

#### Cliente NO_INFORMATION

| Categoria | Limite | Operador |
|-----------|--------|----------|
| VIDA, RESIDENCIAL | R$ 200.000 | ≤ |
| AUTO | R$ 75.000 | ≤ |
| EMPRESARIAL | R$ 55.000 | ≤ |
| OUTROS | R$ 30.000 | ≤ |

**Implementação**: `order-core/src/main/java/io/github/athirson010/core/service/PolicyValidationService.java`

**Testes**: `order-component-test/src/test/java/io/github/athirson010/componenttest/validacao/ValidationRulesCompleteComponentTest.java`

---

## 🐰 Mensageria com RabbitMQ

### Por que RabbitMQ?

**Decisão**: Utilizamos **RabbitMQ** ao invés de Apache Kafka ou AWS SQS pelos seguintes motivos:

1. **Interface Gráfica**: Management UI facilita debug e visualização de filas
2. **Simplicidade**: Configuração e testes locais mais simples
3. **Flexibilidade**: Suporta múltiplos padrões de mensageria (pub/sub, routing, topic)
4. **Ampla Adoção**: Tecnologia consolidada e bem documentada

### Arquitetura de Mensageria

```
┌─────────────────────────────────────────────────────┐
│                   RabbitMQ Broker                   │
├─────────────────────────────────────────────────────┤
│                                                     │
│  Exchange: order.integration.exchange (Topic)      │
│       │                                             │
│       ├─→ Queue: order-service-consumer            │
│       │   (Routing Key: order.process)             │
│       │                                             │
│       ├─→ Queue: order.payment.confirmation.queue  │
│       │   (Routing Key: payment.confirmation)      │
│       │                                             │
│       └─→ Queue: order.subscription.confirmation.queue
│           (Routing Key: subscription.confirmation) │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### Consumers Implementados

O sistema utiliza **profiles do Spring** para permitir escalabilidade independente de cada consumer:

| Profile | Consumer | Fila | Responsabilidade |
|---------|----------|------|------------------|
| `order-consumer` | OrderConsumer | `order-service-consumer` | Processa criação de apólices |
| `order-response-payment-consumer` | PaymentConfirmationConsumer | `order.payment.confirmation.queue` | Processa respostas de pagamento |
| `order-response-insurance-consumer` | InsuranceSubscriptionConfirmationConsumer | `order.subscription.confirmation.queue` | Processa respostas de subscrição |

**Benefícios desta arquitetura**:
- ✅ **Escalabilidade Independente**: Cada consumer pode escalar horizontalmente conforme demanda
- ✅ **Isolamento de Falhas**: Problema em um consumer não afeta os outros
- ✅ **Deploy Independente**: Cada profile pode ser atualizado sem afetar os demais
- ✅ **Otimização de Recursos**: Dimensionar recursos específicos para cada carga de trabalho

#### 1. PaymentConfirmationConsumer

**Profile**: `order-response-payment-consumer`

**Função**: Processa eventos de confirmação/rejeição de pagamento

**Queue**: `order.payment.confirmation.queue`

**Eventos Aceitos**:
```json
{
  "policy_request_id": "uuid",
  "payment_status": "APPROVED" | "REJECTED",
  "transaction_id": "string",
  "amount": "decimal",
  "payment_method": "CREDIT_CARD" | "PIX" | "BOLETO",
  "payment_timestamp": "ISO-8601",
  "rejection_reason": "string (opcional)"
}
```

**Comportamento (Rejeição Imediata)**:
- `APPROVED`: Marca `paymentConfirmed = true`, mantém PENDING até subscription chegar (ou APROVA se subscription já veio aprovada)
- `REJECTED`: Muda status para REJECTED **imediatamente** (não aguarda subscription)
- **Histórico**: Se já estiver REJECTED (por subscription), adiciona entrada no histórico registrando resultado do pagamento

**Implementação**: `order-adapters-in/src/main/java/io/github/athirson010/adapters/in/messaging/rabbitmq/PaymentConfirmationConsumer.java`

#### 2. InsuranceSubscriptionConfirmationConsumer

**Profile**: `order-response-insurance-consumer`

**Função**: Processa eventos de confirmação/rejeição de subscrição de seguro

**Queue**: `order.subscription.confirmation.queue`

**Eventos Aceitos**:
```json
{
  "policy_request_id": "uuid",
  "subscription_status": "APPROVED" | "REJECTED",
  "subscription_id": "string",
  "authorization_timestamp": "ISO-8601",
  "rejection_reason": "string (opcional)"
}
```

**Comportamento (Rejeição Imediata)**:
- `APPROVED`: Marca `subscriptionConfirmed = true`, mantém PENDING até payment chegar (ou APROVA se payment já veio aprovado)
- `REJECTED`: Muda status para REJECTED **imediatamente** (não aguarda payment)
- **Histórico**: Se já estiver REJECTED (por payment), adiciona entrada no histórico registrando resultado da subscrição

**Implementação**: `order-adapters-in/src/main/java/io/github/athirson010/adapters/in/messaging/rabbitmq/InsuranceSubscriptionConfirmationConsumer.java`

### Exemplos de Uso

#### Publicar evento de pagamento aprovado (RabbitMQ CLI)

```bash
# Publicar mensagem de pagamento aprovado
rabbitmqadmin publish \
  exchange=order.integration.exchange \
  routing_key=payment.confirmation \
  payload='{"policy_request_id":"8a5c3e1b-9f2d-4a7e-b3c8-1d4e5f6a7b8c","payment_status":"APPROVED","transaction_id":"TXN-12345","amount":"350.00","payment_method":"CREDIT_CARD","payment_timestamp":"2025-12-15T10:30:00Z"}'
```

#### Publicar evento de subscrição aprovada

```bash
rabbitmqadmin publish \
  exchange=order.integration.exchange \
  routing_key=subscription.confirmation \
  payload='{"policy_request_id":"8a5c3e1b-9f2d-4a7e-b3c8-1d4e5f6a7b8c","subscription_status":"APPROVED","subscription_id":"SUB-67890","authorization_timestamp":"2025-12-15T10:31:00Z"}'
```

### Configuração RabbitMQ

**Arquivo**: `order-application/src/main/resources/application.properties`

```properties
# RabbitMQ Connection
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=admin
spring.rabbitmq.password=admin

# Exchanges e Queues
rabbitmq.exchanges.order-integration=order.integration.exchange
rabbitmq.queues.payment-confirmation=order.payment.confirmation.queue
rabbitmq.queues.subscription-confirmation=order.subscription.confirmation.queue
```

---

## 🎨 Design Patterns Implementados

### 1. State Pattern (Máquina de Estados)

**Onde**: `PolicyProposal.java`

**Por quê**: Controlar transições válidas de estados da apólice, garantindo que regras de negócio sejam respeitadas.

**Exemplo**:
```java
public void validate(Instant now) {
    validateTransition(PolicyStatus.VALIDATED);  // State pattern aqui
    this.status = PolicyStatus.VALIDATED;
    addHistoryEntry(PolicyStatus.VALIDATED, now, null);
}
```

### 2. Strategy Pattern (Validações)

**Onde**: `PolicyValidationService.java`

**Por quê**: Diferentes estratégias de validação para cada classificação de risco.

**Exemplo**:
```java
boolean isValid = switch (classification) {
    case REGULAR -> validateRegularCustomer(insuredAmount, category);
    case HIGH_RISK -> validateHighRiskCustomer(insuredAmount, category);
    case PREFERENTIAL -> validatePreferentialCustomer(insuredAmount, category);
    case NO_INFORMATION -> validateNoInformationCustomer(insuredAmount, category);
};
```

### 3. Builder Pattern (Testes)

**Onde**: `order-component-test/src/test/java/io/github/athirson010/componenttest/templates/`

**Por quê**: Criação fluente e semântica de dados de teste, substituindo fixtures estáticas.

**Exemplo**:
```java
String json = PolicyRequestTemplateBuilder.autoRegular()
    .withCustomerId("custom-uuid")
    .withInsuredAmount(new BigDecimal("250000.00"))
    .buildAsJson();
```

### 4. Factory Method (Criação de Entidades)

**Onde**: `PolicyProposal.create()`

**Por quê**: Garantir que entidades sejam criadas em estado válido.

**Exemplo**:
```java
public static PolicyProposal create(UUID customerId, String productId, ...) {
    PolicyProposal policy = PolicyProposal.builder()
        .id(PolicyProposalId.generate())
        .status(PolicyStatus.RECEIVED)
        .build();

    policy.addHistoryEntry(PolicyStatus.RECEIVED, now, null);
    return policy;
}
```

### 5. Repository Pattern

**Onde**: `OrderRepository` interface + `PolicyProposalMongoRepository` implementação

**Por quê**: Abstrair persistência, permitindo troca de banco de dados sem impactar domínio.

### 6. Value Objects

**Onde**: `Money`, `PolicyProposalId`, `HistoryEntry`

**Por quê**: Encapsular conceitos de negócio com validação e imutabilidade.

**Exemplo**:
```java
@Getter
@ToString
@EqualsAndHashCode
public class Money {
    private final BigDecimal amount;
    private final String currency;

    public static Money brl(BigDecimal amount) {
        return new Money(amount, "BRL");
    }
}
```

---

## 🧪 Estratégia de Testes

### Pirâmide de Testes Implementada

```
        /\
       /  \  E2E (Não implementados - fora escopo)
      /────\
     /      \ Testes de Componentes (✅ Implementados)
    /────────\
   /          \ Testes Unitários (✅ Implementados)
  /────────────\
```

### Testes Unitários

**Onde**: `order-domain/src/test/java/`

**Cobertura**:
- ✅ Todas as transições de estado válidas
- ✅ Todas as transições inválidas (exceções)
- ✅ Estados finais imutáveis
- ✅ Histórico de transições
- ✅ Value Objects

**Exemplo**: `PolicyProposalTest.java` - 25 testes cobrindo toda a máquina de estados

### Testes de Componentes

**Onde**: `order-component-test/src/test/java/`

**Cobertura**:
- ✅ Ciclo de vida completo (RECEIVED → APPROVED)
- ✅ Fluxos de rejeição (pagamento e subscrição)
- ✅ Cancelamento
- ✅ 100% das 16 regras de validação
- ✅ Edge cases (valores no limite, decimais, operadores < vs ≤)

**Testes Principais**:
1. `PolicyLifecycleComponentTest.java` - Ciclo de vida end-to-end
2. `ValidationRulesCompleteComponentTest.java` - 16 regras × 3 casos cada = 48 testes parametrizados

### Templates com Builders

**Substituição de TestDataFixtures**: Criamos builders semânticos ao invés de fixtures estáticas.

**Vantagens**:
- ✅ Mais expressivo: `PolicyRequestTemplateBuilder.autoRegular()`
- ✅ Customizável: `.withInsuredAmount(...)`
- ✅ Documenta o domínio: métodos como `autoExceedsRegularLimit()`
- ✅ Reutilizável: `PolicyFlowScenarioBuilder.successfulFlow()`

**Localização**: `order-component-test/src/test/java/io/github/athirson010/componenttest/templates/`

**Builders Criados**:
- `PolicyRequestTemplateBuilder` - Criação de solicitações de apólice
- `PaymentConfirmationEventBuilder` - Eventos de pagamento
- `SubscriptionConfirmationEventBuilder` - Eventos de subscrição
- `PolicyFlowScenarioBuilder` - Cenários completos

---

## 🚀 Como Executar

### Pré-requisitos

- Java 17+
- Maven 3.8+
- MongoDB 7.0
- RabbitMQ 3.13
- Git
- Docker e Docker Compose (para infraestrutura)

### 1. Iniciar Infraestrutura com Docker Compose

O projeto inclui um `docker-compose.yaml` completo com toda a infraestrutura necessária:

```bash
docker-compose up -d
```

Isso iniciará:
- **MongoDB 7.0** (porta 27017)
- **RabbitMQ 3.13** com Management UI (portas 5672 e 15672)
- **Kafka 7.5.0** (porta 9092)
- **Zookeeper** (porta 2181)
- **Kafka UI** (porta 8090)

Aguarde até que todos os serviços estejam saudáveis:

```bash
docker-compose ps
```

**Interfaces Web Disponíveis**:
- RabbitMQ Management: http://localhost:15672 (admin/admin)
- Kafka UI: http://localhost:8090

#### Alternativa: Docker comandos individuais

Se preferir iniciar apenas MongoDB e RabbitMQ separadamente:

**MongoDB**:
```bash
docker run -d \
  --name mongodb \
  -p 27017:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=admin \
  -e MONGO_INITDB_ROOT_PASSWORD=admin123 \
  mongo:7.0
```

**RabbitMQ**:
```bash
docker run -d \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=admin \
  rabbitmq:3.13-management
```

### 2. Compilar o Projeto

```bash
mvn clean install
```

### 3. Executar a Aplicação

O sistema possui **3 profiles** que permitem executar cada consumer de forma independente para **escalabilidade horizontal**:

#### Opção A: Executar todos os consumers juntos (Desenvolvimento)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=order-consumer,order-response-payment-consumer,order-response-insurance-consumer
```

ou

```bash
java -jar order-application/target/order-application-*.jar \
  --spring.profiles.active=order-consumer,order-response-payment-consumer,order-response-insurance-consumer
```

**Porta**: 8080

#### Opção B: Executar consumers separadamente (Produção - Escalabilidade)

Esta é a **arquitetura recomendada para produção**, permitindo escalar cada consumer independentemente:

**Terminal 1 - Consumer Principal de Orders**:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=order-consumer
# Porta: 8080
```

**Terminal 2 - Consumer de Respostas de Pagamento**:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=order-response-payment-consumer
# Porta: 8081 (ou configure outra)
```

**Terminal 3 - Consumer de Respostas de Subscrição de Seguro**:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=order-response-insurance-consumer
# Porta: 8082 (ou configure outra)
```

**Benefícios da separação**:
- **Escalabilidade**: Se respostas de pagamento aumentarem, escale apenas `order-response-payment-consumer`
- **Resiliência**: Falha em um consumer não derruba os outros
- **Deploy Independente**: Atualizar lógica de payment sem afetar insurance
- **Métricas Isoladas**: Monitorar performance de cada consumer separadamente

**Exemplo de escalabilidade horizontal**:
```bash
# 1 instância do consumer principal
java -jar order-application.jar --spring.profiles.active=order-consumer --server.port=8080

# 3 instâncias do consumer de pagamento (alto volume)
java -jar order-application.jar --spring.profiles.active=order-response-payment-consumer --server.port=8081
java -jar order-application.jar --spring.profiles.active=order-response-payment-consumer --server.port=8082
java -jar order-application.jar --spring.profiles.active=order-response-payment-consumer --server.port=8083

# 2 instâncias do consumer de subscrição
java -jar order-application.jar --spring.profiles.active=order-response-insurance-consumer --server.port=8084
java -jar order-application.jar --spring.profiles.active=order-response-insurance-consumer --server.port=8085
```

### 4. Executar Testes

```bash
# Todos os testes
mvn test

# Apenas testes de componentes
cd order-component-test && mvn test

# Apenas testes unitários
cd order-domain && mvn test
```

---

## 📡 Endpoints da API

### POST /policies

Cria uma nova solicitação de apólice.

**Request**:
```json
{
  "customer_id": "123e4567-e89b-12d3-a456-426614174000",
  "product_id": "PROD-AUTO-2024",
  "category": "AUTO",
  "sales_channel": "MOBILE",
  "payment_method": "CREDIT_CARD",
  "total_monthly_premium_amount": 350.00,
  "insured_amount": 200000.00,
  "coverages": {
    "COLISAO": 200000.00
  },
  "assistances": ["GUINCHO_24H"]
}
```

**Response** (201 Created):
```json
{
  "policy_request_id": "8a5c3e1b-9f2d-4a7e-b3c8-1d4e5f6a7b8c",
  "status": "RECEIVED",
  "created_at": "2025-12-15T10:30:00Z"
}
```

### GET /policies/{id}

Consulta o status de uma apólice.

**Response** (200 OK):
```json
{
  "policy_request_id": "8a5c3e1b-9f2d-4a7e-b3c8-1d4e5f6a7b8c",
  "status": "PENDING",
  "created_at": "2025-12-15T10:30:00Z",
  "finished_at": null
}
```

### DELETE /policies/{id}

Cancela uma apólice (somente antes de estados finais).

**Response** (200 OK):
```json
{
  "policy_request_id": "8a5c3e1b-9f2d-4a7e-b3c8-1d4e5f6a7b8c",
  "status": "CANCELED",
  "finished_at": "2025-12-15T11:00:00Z"
}
```

---

## 🏛️ Princípios SOLID Aplicados

### Single Responsibility Principle (SRP)

- `PolicyProposal`: Responsável APENAS por gerenciar estado e transições
- `PolicyValidationService`: Responsável APENAS por validar regras de limites
- `PaymentConfirmationConsumer`: Responsável APENAS por processar eventos de pagamento

### Open/Closed Principle (OCP)

- Novos estados podem ser adicionados sem modificar lógica existente
- Novas regras de validação podem ser adicionadas sem alterar outras

### Liskov Substitution Principle (LSP)

- Implementações de `OrderRepository` são substituíveis (MongoDB, em memória, etc.)

### Interface Segregation Principle (ISP)

- Ports específicas ao invés de uma interface genérica (`OrderRepository`, `OrderEventPort`, etc.)

### Dependency Inversion Principle (DIP)

- Domínio depende de interfaces (ports), não de implementações concretas
- Inversão de controle via Spring

---

## 📋 Premissas e Decisões

### Premissas de Negócio

1. **Rejeição Imediata com Histórico Completo**:
   - Policy muda para **REJECTED imediatamente** se **QUALQUER** resposta (pagamento OU subscrição) for rejeitada
   - Só aprova se **AMBAS** respostas forem **APPROVED**
   - **Histórico completo**: Mesmo após rejeitado, a segunda resposta é registrada no histórico

2. **Garantia de Histórico**:
   - O histórico **SEMPRE** contém o resultado de **AMBAS** as respostas (pagamento E subscrição)
   - Se primeira resposta rejeitar, status muda para REJECTED
   - Se segunda resposta chegar após rejeição, adiciona entrada no histórico com resultado (aprovado ou rejeitado)

3. **Eventos fora de ordem**:
   - Se uma confirmação chega antes da policy estar PENDING, ela é ignorada
   - Não é permitido processar a mesma resposta (pagamento ou subscrição) duas vezes

4. **Estados finais**: APPROVED, REJECTED e CANCELED são imutáveis

5. **Cancelamento**: Permitido apenas antes de estados finais

6. **Exemplo de histórico com rejeição**:
   - Pagamento rejeitado → entrada no histórico: "Pagamento rejeitado: <motivo>"
   - Subscrição aprovada depois → nova entrada: "Subscrição aprovada (após rejeição por pagamento)"
   - Status final: **REJECTED**

### Decisões Técnicas

#### Por que JSON ao invés de Apache Avro?

**Decisão**: Utilizar JSON para mensageria ao invés de Apache Avro.

**Motivos**:
1. **Tempo de desenvolvimento**: Avro requer setup de schema registry, geração de código, etc.
2. **Facilidade de debug**: JSON é legível e facilmente inspecionável no RabbitMQ Management UI
3. **Simplicidade**: Para o escopo do desafio, JSON é suficiente
4. **Trade-off consciente**: Sabemos que Avro seria melhor para produção (performance, schema evolution)

**Impacto**: Mensagens JSON são maiores e sem garantia de schema, mas facilitam desenvolvimento e testes.

#### Por que RabbitMQ ao invés de Kafka ou SQS?

**Decisão**: Utilizar RabbitMQ.

**Motivos**:
1. **Interface gráfica**: Management UI facilita visualização e debug
2. **Setup local**: Mais simples que Kafka (sem Zookeeper, Schema Registry, etc.)
3. **Adequação ao problema**: Volumes não justificam complexidade do Kafka
4. **Familiaridade**: RabbitMQ é amplamente conhecido e bem documentado

#### Profiles Separados para Escalabilidade

**Decisão**: Separar consumers em profiles Spring independentes.

**Profiles criados**:
1. `order-consumer`: Consumer principal de processamento de pedidos
2. `order-response-payment-consumer`: Consumer dedicado para respostas de pagamento
3. `order-response-insurance-consumer`: Consumer dedicado para respostas de subscrição de seguro

**Motivos**:
1. **Escalabilidade Horizontal**: Cada consumer pode ter N instâncias independentes
2. **Isolamento de Falhas**: Problema em payment não afeta insurance e vice-versa
3. **Otimização de Recursos**: Escalar apenas o consumer com maior carga
4. **Deploy Independente**: Atualizar lógica de payment sem restart de insurance
5. **Métricas Granulares**: Monitorar performance de cada consumer separadamente

**Exemplo de produção**:
```bash
# Baixa carga de orders: 1 instância
1x order-consumer (porta 8080)

# Alta carga de pagamentos: 5 instâncias
5x order-response-payment-consumer (portas 8081-8085)

# Média carga de subscrições: 2 instâncias
2x order-response-insurance-consumer (portas 8086-8087)
```

**Implementação**:
- `PaymentConfirmationConsumer.java`: `@Profile("order-response-payment-consumer")`
- `InsuranceSubscriptionConfirmationConsumer.java`: `@Profile("order-response-insurance-consumer")`
- `application.properties`: Documentação de todos os profiles disponíveis

#### Docker Compose

**Status**: ✅ Implementado

**Infraestrutura completa** no arquivo `docker-compose.yaml`:
- MongoDB 7.0
- RabbitMQ 3.13 com Management UI
- Kafka 7.5.0 com Zookeeper
- Kafka UI para monitoramento
- Network isolada para todos os serviços
- Health checks configurados
- Volumes persistentes para MongoDB

**Uso**:
```bash
docker-compose up -d
```

**Nota**: A aplicação Java não está no docker-compose (executada via Maven/JAR), permitindo maior agilidade no desenvolvimento e debug.

#### Observabilidade (Grafana Stack)

**Status**: ✅ **IMPLEMENTADO E CONFIGURADO**

A aplicação está totalmente integrada com o Grafana Stack (LGTM):

**Stack de Observabilidade** no arquivo `docker-compose.observability.yaml`:
- **Grafana 10.2.3** (porta 3000) - Dashboards unificados
- **Loki 2.9.3** (porta 3100) - Agregação de logs
- **Tempo 2.3.1** (porta 3200) - Distributed tracing
- **Prometheus 2.48.1** (porta 9090) - Métricas
- **Promtail 2.9.3** (porta 9080) - Coleta de logs

**Integração da Aplicação**:

✅ **Métricas (Prometheus)**:
- Endpoint: `http://localhost:8080/actuator/prometheus`
- Dependência: `micrometer-registry-prometheus`
- Coleta automática: Prometheus scrape a cada 15s

✅ **Traces (Tempo)**:
- OpenTelemetry OTLP exportando para `http://localhost:4318/v1/traces`
- Dependências: `micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`
- Sampling: 100% (development)
- Trace ID e Span ID incluídos nos logs

✅ **Logs (Loki)**:
- Logs estruturados em JSON enviados para `http://localhost:3100/loki/api/v1/push`
- Dependência: `loki-logback-appender`
- Labels: `app=order-service`, `host=<hostname>`, `level=<log-level>`
- Correlação: Trace ID incluído em cada log

**Como Usar**:

1. **Iniciar Stack de Observabilidade**:
```bash
docker-compose -f docker-compose.observability.yaml up -d
```

2. **Iniciar Aplicação**:
```bash
mvn spring-boot:run
```

3. **Acessar Grafana**: http://localhost:3000
   - Usuário: `admin`
   - Senha: `admin`

4. **Verificar Integração**:
```bash
# Métricas
curl http://localhost:8080/actuator/prometheus

# Traces (após fazer algumas requisições)
# Grafana → Explore → Tempo → Search

# Logs
# Grafana → Explore → Loki → Query: {app="order-service"}
```

**Queries Úteis**:

**Loki (Logs)**:
```logql
# Todos os logs da aplicação
{app="order-service"}

# Apenas erros
{app="order-service"} |= "ERROR"

# Logs de uma policy específica
{app="order-service"} |= "policyId=123"
```

**Prometheus (Métricas)**:
```promql
# Taxa de requisições HTTP
rate(http_server_requests_seconds_count[5m])

# Latência P95
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Uso de memória JVM
jvm_memory_used_bytes{area="heap"}
```

**Documentação Completa**: Ver `observability/README.md`

---

## 📊 Estrutura de Módulos Maven

```
case-emissao-apolice-seguros/
│
├── pom.xml (parent)
│
├── order-domain/              # Domínio puro (sem Spring)
│   └── pom.xml
│
├── order-core/                # Use Cases e Ports
│   └── pom.xml
│
├── order-adapters-in/         # Controllers, Consumers
│   └── pom.xml
│
├── order-adapters-out/        # MongoDB, Mensageria
│   └── pom.xml
│
├── order-application/         # Startup e Config
│   └── pom.xml
│
└── order-component-test/      # Testes end-to-end
    └── pom.xml
```

---

## 📚 Referências e Documentação Adicional

- [Clean Architecture - Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Domain-Driven Design - Eric Evans](https://www.domainlanguage.com/ddd/)
- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)
- [Spring AMQP](https://spring.io/projects/spring-amqp)

---

## 🤝 Contribuindo

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

---

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

---

## 👥 Autor

**Athirson de Oliveira** - *Desenvolvimento Inicial*

- Email: athirson.candido@bandtec.com.br
- LinkedIn: [Athirson-Oliveira](https://br.linkedin.com/in/athirson-oliveira)

---

## ✅ Checklist de Validação

Este projeto atende aos seguintes requisitos do desafio técnico:

- [x] Todas as transições de estado respeitam validation-rules.json
- [x] Estados finais são imutáveis
- [x] **Rejeição Imediata**: Policy muda para REJECTED imediatamente se QUALQUER resposta for rejeitada
- [x] Policy só é APPROVED quando AMBAS respostas (pagamento E subscrição) são positivas
- [x] **Histórico Completo**: SEMPRE registra resultado de AMBAS respostas, mesmo após rejeição
- [x] Segunda resposta (após rejeição) é registrada no histórico mantendo status REJECTED
- [x] Transições inválidas são rejeitadas com InvalidTransitionException
- [x] Não permite processar a mesma resposta (pagamento/subscrição) duas vezes
- [x] Templates substituem completamente TestDataFixtures
- [x] README reflete fielmente o código e arquitetura
- [x] 16 regras de validação implementadas e testadas (100% cobertura)
- [x] Consumers de pagamento e seguro funcionais com nova lógica
- [x] **Profiles separados**: 3 profiles para escalabilidade independente
- [x] **PaymentConfirmationConsumer**: Profile `order-response-payment-consumer`
- [x] **InsuranceSubscriptionConfirmationConsumer**: Profile `order-response-insurance-consumer`
- [x] Histórico completo de transições registrado com motivos combinados
- [x] Clean Architecture implementada
- [x] Princípios SOLID aplicados
- [x] Design Patterns documentados e justificados
- [x] Testes de componentes cobrindo ciclo de vida completo
- [x] 19 testes unitários para Rejeição Imediata com Histórico Completo
- [x] Testes cobrem todos os cenários: ambas aprovadas, ambas rejeitadas, uma aprovada + outra rejeitada
- [x] Mensageria documentada com exemplos de uso
- [x] Premissas e limitações claramente documentadas
- [x] **Observabilidade Completa**: Logs, Traces e Métricas integrados com Grafana Stack
- [x] Métricas expostas via `/actuator/prometheus` e coletadas pelo Prometheus
- [x] Traces enviados para Tempo via OpenTelemetry OTLP
- [x] Logs estruturados em JSON enviados para Loki com Trace ID
- [x] Grafana configurado com datasources automáticos (Prometheus, Loki, Tempo)

---

⭐ Se este projeto foi útil para você, considere dar uma estrela!
