# Sistema de Emissão de Apólices de Seguros

![Itaú App](docs/itau-app.jpeg)

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-green.svg)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-latest-green.svg)](https://www.mongodb.com/)
[![Kafka](https://img.shields.io/badge/Kafka-3.1+-black.svg)](https://kafka.apache.org/)
[![AWS](https://img.shields.io/badge/AWS-SQS-orange.svg)](https://aws.amazon.com/sqs/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 📋 Sobre o Projeto

Sistema robusto e escalável para emissão, gerenciamento e análise de apólices de seguros, desenvolvido com foco em *
*Arquitetura Hexagonal (Ports and Adapters)**, **Event-Driven Architecture** e boas práticas de desenvolvimento.

O sistema utiliza:

- **MongoDB** para persistência
- **AWS SQS** para processamento assíncrono de análise de fraude
- **Apache Kafka** para publicação de eventos de apólices aprovadas
- **Spring Profiles** para separação de contextos e escalabilidade independente

## 🏗️ Arquitetura

![Diagrama de Solução](docs/diagrama.png)

O projeto foi desenvolvido utilizando **Arquitetura Hexagonal (Ports and Adapters)** com **Event-Driven Architecture**,
garantindo:

- **Separação de responsabilidades** entre camadas de domínio, aplicação e infraestrutura
- **Independência de frameworks** e tecnologias externas
- **Processamento assíncrono** com filas e eventos
- **Escalabilidade independente** de cada contexto via Spring Profiles
- **Facilidade de testes** e manutenção
- **Flexibilidade** para mudanças tecnológicas

### Arquitetura de Profiles - Separação de Contextos

A aplicação utiliza **Spring Profiles** para separar contextos e permitir escalabilidade independente:

| Profile            | Descrição                         | Porta | Componentes Ativos                    |
|--------------------|-----------------------------------|-------|---------------------------------------|
| **api**            | REST API para criação de apólices | 8080  | Controllers, SQS Producer, MongoDB    |
| **fraud-consumer** | Consumer para análise de fraude   | 8081  | SQS Consumer, Kafka Producer, MongoDB |

**Benefícios:**

- ✅ **1 único build** - Um JAR para ambos os contextos
- ✅ **Escalabilidade Independente** - Escale API e Consumer separadamente
- ✅ **Isolamento de Falhas** - Se o consumer falhar, a API continua funcionando
- ✅ **Otimização de Recursos** - Cada contexto usa apenas o necessário

📖 **Documentação completa**: [PROFILES.md](PROFILES.md)

### Estrutura Modular

O projeto está organizado em módulos Maven independentes seguindo os princípios da arquitetura hexagonal:

```
case-emissao-apolice-seguros/
│
├── order-domain/                          # Camada de Domínio (Núcleo da aplicação)
│   └── src/main/java/
│       └── io/github/athirson010/domain/
│           ├── enums/                     # Enumerações de domínio
│           │   ├── Category.java          # Categorias de seguro (AUTO, VIDA, RESIDENCIAL...)
│           │   ├── PolicyStatus.java      # Estados da apólice (RECEIVED, VALIDATED, APPROVED...)
│           │   ├── RiskClassification.java # Classificação de risco (REGULAR, HIGH_RISK...)
│           │   ├── PaymentMethod.java     # Métodos de pagamento (CREDIT_CARD, PIX, BOLETO)
│           │   ├── SalesChannel.java      # Canais de venda
│           │   └── OccurrenceType.java    # Tipos de ocorrência de fraude
│           ├── exception/                 # Exceções de domínio
│           │   ├── DomainException.java
│           │   └── InvalidTransitionException.java
│           ├── model/                     # Entidades e Value Objects
│           │   ├── PolicyProposal.java    # Agregado raiz - Proposta de apólice
│           │   ├── PolicyProposalId.java  # Value Object - ID da proposta
│           │   ├── Money.java             # Value Object - Valor monetário
│           │   ├── HistoryEntry.java      # Value Object - Entrada de histórico
│           │   ├── FraudAnalysisResult.java # Resultado da análise de fraude
│           │   └── FraudOccurrence.java   # Ocorrência de fraude
│           ├── rules/                     # Regras de negócio
│           │   └── (Validações de limites por categoria e risco)
│           └── service/                   # Serviços de domínio
│
├── order-core/                            # Camada de Aplicação
│   └── src/main/java/
│       └── io/github/athirson010/core/
│           ├── port/                      # Portas (interfaces)
│           │   ├── in/                    # Portas de entrada
│           │   │   └── CreateOrderUseCase.java  # Caso de uso de criação
│           │   └── out/                   # Portas de saída
│           │       ├── OrderRepository.java     # Repositório de apólices
│           │       ├── FraudQueuePort.java      # Porta para fila de fraude
│           │       ├── FraudCheckPort.java      # Porta para API de fraude
│           │       ├── OrderEventPort.java      # Porta para eventos (Kafka)
│           │       └── NotificationPort.java    # Porta para notificações
│           └── service/                   # Serviços de aplicação
│               ├── OrderApplicationService.java # Orquestração de casos de uso
│               └── PolicyValidationService.java # Validação de regras de negócio
│
├── order-adapters-in/                     # Adaptadores de Entrada
│   └── src/main/java/
│       └── io/github/athirson010/adapters/in/
│           ├── web/                       # Adaptador REST API
│           │   ├── PolicyRequestController.java  # Controller REST (@Profile("api"))
│           │   ├── dto/                   # DTOs de request/response
│           │   │   ├── CreatePolicyRequest.java
│           │   │   ├── CreatePolicyResponse.java
│           │   │   ├── CancelPolicyRequest.java
│           │   │   └── CancelPolicyResponse.java
│           │   ├── mapper/                # Mapeadores
│           │   │   └── PolicyRequestMapper.java
│           │   ├── exception/             # Tratamento de exceções
│           │   │   ├── GlobalExceptionHandler.java
│           │   │   └── ErrorResponse.java
│           │   └── validation/            # Validações customizadas
│           │       ├── annotation/        # Anotações de validação
│           │       │   └── ValidCoverages.java
│           │       └── validator/         # Validadores
│           └── messaging/                 # Adaptador de Mensageria
│               └── rabbitmq/              # Consumer SQS
│                   └── FraudQueueConsumer.java  # (@Profile("fraud-consumer"))
│
├── order-adapters-out/                    # Adaptadores de Saída
│   └── src/main/java/
│       └── io/github/athirson010/adapters/out/
│           ├── persistence/               # Adaptador de Persistência
│           │   └── mongo/                 # MongoDB
│           │       ├── document/          # Documentos MongoDB
│           │       │   ├── PolicyProposalDocument.java
│           │       │   ├── AddressEntity.java
│           │       │   ├── AssistanceEntity.java
│           │       │   ├── AutoDataEntity.java
│           │       │   └── (outros documentos...)
│           │       ├── enums/             # Enums para persistência
│           │       ├── mapper/            # Mapeadores de persistência
│           │       │   └── PolicyProposalDocumentMapper.java
│           │       └── repository/        # Repositórios MongoDB
│           │           └── PolicyProposalMongoRepository.java
│           ├── messaging/                 # Adaptadores de Mensageria
│           │   ├── rabbitmq/              # SQS
│           │   │   └── FraudQueueAdapter.java  # Producer SQS (@Profile("api"))
│           │   ├── kafka/                 # Kafka
│           │   │   └── OrderKafkaProducer.java # Producer Kafka (@Profile("fraud-consumer"))
│           │   └── sns/                   # SNS
│           │       └── OrderSnsAdapter.java
│           └── fraud/                     # Adaptador API de Fraude
│               ├── FraudApiAdapter.java   # Cliente da API de fraude (mock)
│               ├── dto/                   # DTOs da API de fraude
│               │   ├── FraudAnalysisResponseDto.java
│               │   └── FraudOccurrenceDto.java
│               └── mapper/                # Mapeadores
│                   └── FraudAnalysisMapper.java
│
├── order-application/                     # Módulo de Inicialização
│   └── src/main/
│       ├── java/
│       │   └── io/github/athirson010/application/
│       │       ├── OrderApplication.java  # Classe principal Spring Boot
│       │       └── config/                # Configurações
│       │           ├── JacksonConfig.java
│       │           ├── KafkaConfig.java   # (@Profile("fraud-consumer"))
│       │           ├── RabbitMQConfig.java # (SQS Config)
│       │           └── OpenApiConfig.java # Swagger/OpenAPI
│       ├── resources/
│       │   └── application.yml            # Properties unificado (ambos profiles)
│       └── test/
│           └── java/
│               └── io/github/athirson010/arch/
│                   └── ArchitectureTest.java  # Testes de arquitetura (ArchUnit)
│
├── docs/                                  # Documentação
│   ├── diagrama.png                       # Diagrama de solução
│   └── itau-app.jpeg                      # Logo/Imagem
│
├── docker-compose.yaml                    # Infraestrutura (MongoDB, Kafka, LocalStack)
├── pom.xml                                # POM pai do projeto multi-módulo
└── README.md                              # Este arquivo
```

## 🔄 Fluxo de Processamento Completo

```
┌─────────────────────────────────┐
│    Profile: API (porta 8080)    │
└────────────┬────────────────────┘
             │
             │ 1. POST /policies
             ↓
      ┌─────────────┐
      │  Controller │
      └──────┬──────┘
             │
             ├─→ 2. Persiste MongoDB (status: RECEIVED)
             │
             └─→ 3. Envia SQS (order-service-fraud-consumer)
                    │
                    │
                    ↓
┌────────────────────────────────────────┐
│ Profile: fraud-consumer (porta 8081)   │
└────────────┬───────────────────────────┘
             │
             │ 4. Consumer SQS recebe
             ↓
      ┌──────────────┐
      │ Fraud Queue  │
      │   Consumer   │
      └──────┬───────┘
             │
             ├─→ 5. API Fraudes (Mock)
             │       ↓ RiskClassification
             │
             ├─→ 6. PolicyValidationService
             │       ↓ Valida limites por categoria
             │
             ├─→ 7. Atualiza MongoDB
             │        ├─ VALIDATED → APPROVED (se válido)
             │        └─ VALIDATED → REJECTED (se inválido)
             │
             └─→ 8. Se APPROVED:
                     Publica Kafka (order-topic)
```

## 🎯 Funcionalidades

### Gestão de Apólices

- ✅ Criar nova proposta de apólice
- ✅ Consultar apólice por ID
- ✅ Cancelar apólice
- ✅ Máquina de estados com transições validadas
- ✅ Histórico completo de alterações de status

### Análise de Fraude Assíncrona

- ✅ Análise automática via API de fraude (mock)
- ✅ Classificação de risco do cliente (REGULAR, HIGH_RISK, PREFERENTIAL, NO_INFORMATION)
- ✅ Validação de limites de capital segurado por categoria e classificação
- ✅ Processamento assíncrono via SQS

### Publicação de Eventos

- ✅ Eventos de apólices aprovadas publicados no Kafka
- ✅ Integração com sistemas downstream
- ✅ Garantia de entrega com confirmação (acks=all)

### Fluxo de Estados

```
RECEIVED → VALIDATED → APPROVED → (Kafka Event)
    ↓           ↓
CANCELED   REJECTED
```

**Transições válidas:**

- `RECEIVED` → `VALIDATED` ou `CANCELED`
- `VALIDATED` → `APPROVED` ou `REJECTED`

### Categorias de Seguro Suportadas

- 🚗 **AUTO** - Seguro Automotivo
- ❤️ **VIDA** - Seguro de Vida
- 🏠 **RESIDENCIAL** - Seguro Residencial
- 🏢 **EMPRESARIAL** - Seguro Empresarial
- 📦 **OUTROS** - Outros tipos de seguro

### Classificações de Risco

- 👤 **REGULAR** - Cliente regular
- ⚠️ **HIGH_RISK** - Cliente de alto risco
- ⭐ **PREFERENTIAL** - Cliente preferencial
- ❓ **NO_INFORMATION** - Sem informações do cliente

### Regras de Validação por Classificação

#### Cliente REGULAR

| Categoria         | Limite de Capital Segurado |
|-------------------|----------------------------|
| VIDA, RESIDENCIAL | ≤ R$ 500.000,00            |
| AUTO              | ≤ R$ 350.000,00            |
| EMPRESARIAL       | ≤ R$ 255.000,00            |
| OUTROS            | ≤ R$ 100.000,00            |

#### Cliente HIGH_RISK

| Categoria         | Limite de Capital Segurado |
|-------------------|----------------------------|
| AUTO              | ≤ R$ 250.000,00            |
| RESIDENCIAL       | ≤ R$ 150.000,00            |
| VIDA, EMPRESARIAL | ≤ R$ 125.000,00            |
| OUTROS            | ≤ R$ 50.000,00             |

#### Cliente PREFERENTIAL

| Categoria         | Limite de Capital Segurado |
|-------------------|----------------------------|
| VIDA              | < R$ 800.000,00            |
| AUTO, RESIDENCIAL | < R$ 450.000,00            |
| EMPRESARIAL       | ≤ R$ 375.000,00            |
| OUTROS            | ≤ R$ 300.000,00            |

#### Cliente NO_INFORMATION

| Categoria         | Limite de Capital Segurado |
|-------------------|----------------------------|
| VIDA, RESIDENCIAL | ≤ R$ 200.000,00            |
| AUTO              | ≤ R$ 75.000,00             |
| EMPRESARIAL       | ≤ R$ 55.000,00             |
| OUTROS            | ≤ R$ 30.000,00             |

### Métodos de Pagamento

- 💳 **CREDIT_CARD** - Cartão de Crédito
- 💰 **PIX** - PIX
- 📄 **BOLETO** - Boleto Bancário

## 🚀 Tecnologias Utilizadas

### Core

- **Java 17**
- **Spring Boot 3.2.1**
- **Spring Web** (REST API)
- **Spring Data MongoDB**
- **Spring Kafka 3.1.1** - Produtor de eventos
- **Spring Cloud AWS 3.1.0** - Integração com SQS
- **Lombok** - Redução de boilerplate

### Banco de Dados

- **MongoDB 7.0** - Banco de dados NoSQL para persistência

### Mensageria e Eventos

- **AWS SQS** - Fila para processamento assíncrono de fraude
- **Apache Kafka** - Publicação de eventos de apólices aprovadas
- **LocalStack 3.0** - Emulação de serviços AWS em ambiente local

### Qualidade de Código

- **JUnit 5** - Testes unitários
- **ArchUnit** - Testes de arquitetura
- **Maven** - Gerenciamento de dependências e build

### Monitoramento

- **Spring Actuator** - Endpoints de health e métricas
- **Kafka UI** - Interface gráfica para monitoramento do Kafka

## 📦 Pré-requisitos

- Java 17 ou superior
- Maven 3.8+
- Docker e Docker Compose
- Git

## 🔧 Instalação e Execução

### 1. Clone o repositório

```bash
git clone https://github.com/seu-usuario/emissao-apolice-seguros.git
cd emissao-apolice-seguros
```

### 2. Inicie a infraestrutura (MongoDB, LocalStack, Kafka)

```bash
docker-compose up -d
```

Aguarde até que todos os serviços estejam saudáveis:

```bash
docker ps
```

Verifique que estão rodando:

- MongoDB (porta 27017)
- LocalStack SQS (porta 4566)
- Kafka (porta 9092)
- Zookeeper (porta 2181)
- Kafka UI (porta 8090)

### 3. Compile o projeto

```bash
mvn clean install -DskipTests
```

### 4. Execute os profiles

#### Opção A: Executar ambos os profiles simultaneamente

**Terminal 1 - Profile API:**

```bash
# Windows
start-api.bat

# Linux/Mac
./start-api.sh
```

**Terminal 2 - Profile Fraud Consumer:**

```bash
# Windows
start-fraud-consumer.bat

# Linux/Mac
./start-fraud-consumer.sh
```

#### Opção B: Executar manualmente com Maven

**Profile API:**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=api
```

**Profile Fraud Consumer:**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=fraud-consumer
```

#### Opção C: Executar com JAR

```bash
# Compilar
mvn clean package -DskipTests

# Profile API
java -jar order-application/target/order-application-0.0.1-SNAPSHOT.jar --spring.profiles.active=api

# Profile Fraud Consumer
java -jar order-application/target/order-application-0.0.1-SNAPSHOT.jar --spring.profiles.active=fraud-consumer
```

### 5. Verifique os serviços

**Profile API:**

```bash
curl http://localhost:8080/actuator/health
```

**Profile Fraud Consumer:**

```bash
curl http://localhost:8081/actuator/health
```

**Kafka UI:**

```
http://localhost:8090
```

## 🔌 Endpoints da API

### Profile API (porta 8080)

| Método | Endpoint           | Descrição                      |
|--------|--------------------|--------------------------------|
| POST   | `/policies`        | Criar nova proposta de apólice |
| GET    | `/policies/{id}`   | Buscar apólice por ID          |
| DELETE | `/policies/{id}`   | Cancelar apólice               |
| GET    | `/actuator/health` | Health check                   |

### Exemplo de Request - Criar Apólice

**POST** `http://localhost:8080/policies`

```json
{
  "customer_id": "123e4567-e89b-12d3-a456-426614174000",
  "product_id": "PROD-AUTO-2024",
  "category": "AUTO",
  "sales_channel": "MOBILE_APP",
  "payment_method": "CREDIT_CARD",
  "total_monthly_premium_amount": 350.00,
  "insured_amount": 50000.00,
  "coverages": {
    "COLISAO": 50000.00,
    "ROUBO_FURTO": 45000.00,
    "INCENDIO": 50000.00
  },
  "assistances": [
    "GUINCHO_24H",
    "CHAVEIRO",
    "TROCA_DE_PNEUS"
  ]
}
```

**Response:**

```json
{
  "policy_request_id": "8a5c3e1b-9f2d-4a7e-b3c8-1d4e5f6a7b8c",
  "status": "RECEIVED",
  "created_at": "2024-12-13T10:30:00Z"
}
```

### Exemplo de Request - Cancelar Apólice

**DELETE** `http://localhost:8080/policies/{id}`

**Response:**

```json
{
  "policy_request_id": "8a5c3e1b-9f2d-4a7e-b3c8-1d4e5f6a7b8c",
  "status": "CANCELED",
  "finished_at": "2024-12-13T11:00:00Z"
}
```

### Exemplo de Request - Consultar Apólice

**GET** `http://localhost:8080/policies/{id}`

**Response:**

```json
{
  "policy_request_id": "8a5c3e1b-9f2d-4a7e-b3c8-1d4e5f6a7b8c",
  "status": "APPROVED",
  "created_at": "2024-12-13T10:30:00Z",
  "finished_at": "2024-12-13T10:32:15Z"
}
```

## 🧪 Executando Testes

```bash
# Executar todos os testes
mvn test

# Executar testes de um módulo específico
cd order-application
mvn test

# Executar testes de arquitetura com ArchUnit
cd order-application
mvn test -Dtest=ArchitectureTest
```

### Testes de Arquitetura

O projeto utiliza **ArchUnit** para garantir que as regras de arquitetura hexagonal sejam respeitadas:

- Validação de dependências entre módulos
- Verificação de isolamento do domínio
- Garantia de que adaptadores dependem apenas de portas

## 📊 Padrões de Design Implementados

- **Hexagonal Architecture (Ports and Adapters)** - Separação completa entre domínio e infraestrutura
- **Event-Driven Architecture** - Processamento assíncrono com SQS e Kafka
- **CQRS Simplificado** - Separação de comandos (API) e processamento (Consumer)
- **Repository Pattern** - Abstração de persistência (MongoDB)
- **Factory Method Pattern** - Criação de entidades de domínio através de métodos estáticos
- **Builder Pattern** - Construção de objetos complexos (via Lombok @Builder)
- **DTO Pattern** - Transferência de dados entre camadas
- **Value Objects** - Objetos imutáveis de domínio (Money, PolicyRequestId)
- **State Machine Pattern** - Controle de transições de estado da apólice
- **Mapper Pattern** - Conversão entre DTOs e entidades de domínio
- **Conditional Bean Registration** - Beans condicionais via @Profile

## 🎯 Escalabilidade

### Cenário 1: Alta demanda na API

```bash
# Escale apenas o profile API
docker-compose up --scale api=5
```

### Cenário 2: Backlog na fila de fraude

```bash
# Escale apenas o consumer
docker-compose up --scale fraud-consumer=3
```

### Cenário 3: Escala completa

```bash
# Escale ambos independentemente
docker-compose up --scale api=3 --scale fraud-consumer=5
```

## 📈 Monitoramento

### Endpoints do Spring Actuator

**Profile API (porta 8080):**

- `/actuator/health` - Status da aplicação e dependências (MongoDB, SQS)
- `/actuator/info` - Informações da aplicação
- `/actuator/metrics` - Métricas da aplicação

**Profile Fraud Consumer (porta 8081):**

- `/actuator/health` - Status da aplicação e dependências (MongoDB, SQS, Kafka)
- `/actuator/metrics` - Métricas da aplicação

### Kafka UI

Acesse `http://localhost:8090` para visualizar:

- Tópicos Kafka
- Mensagens publicadas
- Consumer groups
- Partições e offsets

### Monitoramento de SQS

```bash
# Listar filas
aws --endpoint-url=http://localhost:4566 sqs list-queues

# Ver atributos da fila
aws --endpoint-url=http://localhost:4566 sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/order-service-fraud-consumer \
  --attribute-names All
```

📖 **Guia completo de monitoramento**: [MONITORING.md](MONITORING.md)

## 📝 Regras de Negócio

### Transições de Estado

- ✅ Apólices são criadas no estado **RECEIVED**
- ✅ Apenas transições válidas são permitidas
- ✅ Estados finais (**APPROVED**, **REJECTED**, **CANCELED**) não podem ser alterados
- ✅ Cancelamento só é permitido antes de atingir estado final

### Validações

- ✅ **Análise de Fraude** - Integração com API de análise de fraude (mock)
- ✅ **Classificação de Risco** - 4 categorias (REGULAR, HIGH_RISK, PREFERENTIAL, NO_INFORMATION)
- ✅ **Validação de Limites** - 16 regras diferentes (4 classificações × 4 categorias principais)
- ✅ **Validação de Categoria** - Verificação de categoria de seguro
- ✅ **Validação de Capital Segurado** - Limites por categoria e classificação

### Processamento Assíncrono

- ✅ API recebe requisição e persiste com status **RECEIVED**
- ✅ Mensagem enviada para SQS para processamento
- ✅ Consumer processa análise de fraude de forma assíncrona
- ✅ Status atualizado para **APPROVED** ou **REJECTED**
- ✅ Apólices aprovadas publicadas no Kafka para downstream

### Histórico

- ✅ Todas as alterações de estado são registradas
- ✅ Cada entrada do histórico contém: status, timestamp e motivo (quando aplicável)
- ✅ Histórico imutável e auditável

## 📚 Documentação Adicional

- [PROFILES.md](PROFILES.md) - Arquitetura detalhada de profiles
- [MONITORING.md](MONITORING.md) - Guia de monitoramento de Kafka e SQS

## 🤝 Contribuindo

1. Faça um fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

## 👥 Autores

- **Athirson de Oliveira** - *Desenvolvimento Inicial*

## 📞 Contato

- Email: athirson.candido@bandtec.com.br
- LinkedIn: [Athirson-Oliveira](https://br.linkedin.com/in/athirson-oliveira)

---

⭐ Se este projeto foi útil para você, considere dar uma estrela!
