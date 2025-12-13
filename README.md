# Sistema de Emissão de Apólices de Seguros

![Itaú App](docs/itau-app.jpeg)

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-green.svg)](https://spring.io/projects/spring-boot)
[![MongoDB](https://img.shields.io/badge/MongoDB-latest-green.svg)](https://www.mongodb.com/)
[![AWS](https://img.shields.io/badge/AWS-SNS-orange.svg)](https://aws.amazon.com/sns/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 📋 Sobre o Projeto

Sistema robusto e escalável para emissão, gerenciamento e consulta de apólices de seguros, desenvolvido com foco em *
*Arquitetura Hexagonal (Ports and Adapters)** e boas práticas de desenvolvimento. O sistema utiliza MongoDB para
persistência, AWS SNS para mensageria e implementa validações de fraude e notificações assíncronas.

## 🏗️ Arquitetura

![Diagrama de Solução](docs/diagrama.png)

O projeto foi desenvolvido utilizando **Arquitetura Hexagonal (Ports and Adapters)**, garantindo:

- **Separação de responsabilidades** entre camadas de domínio, aplicação e infraestrutura
- **Independência de frameworks** e tecnologias externas
- **Facilidade de testes** e manutenção
- **Flexibilidade** para mudanças tecnológicas

### Estrutura Modular

O projeto está organizado em módulos Maven independentes seguindo os princípios da arquitetura hexagonal:

```
├── order-domain/           # Núcleo da aplicação
│   ├── Entidades de domínio (PolicyRequest)
│   ├── Value Objects (Money, PolicyRequestId, HistoryEntry)
│   ├── Enums (PolicyStatus, Category, PaymentMethod)
│   ├── Regras de negócio e validações reutilizáveis
│   └── Exceções de domínio
│
├── order-core/             # Camada de aplicação
│   ├── Casos de uso (CreateOrderUseCase)
│   ├── Portas de entrada (in) - interfaces para adaptadores de entrada
│   ├── Portas de saída (out) - interfaces para adaptadores de saída
│   └── Serviços de aplicação que orquestram o domínio
│
├── order-adapters-in/      # Adaptadores de entrada
│   ├── Controllers REST
│   ├── DTOs de request/response
│   └── Mappers (conversão entre DTOs e entidades de domínio)
│
├── order-adapters-out/     # Adaptadores de saída
│   ├── Implementação de persistência (MongoDB)
│   ├── Integração com AWS SNS (mensageria)
│   ├── Integração com APIs externas (fraude)
│   └── Mappers de persistência (conversão entre domínio e documentos)
│
└── order-application/      # Inicialização
    ├── Configuração Spring Boot
    ├── Application properties
    └── Testes de arquitetura (ArchUnit)
```

## 🎯 Funcionalidades

### Gestão de Solicitações de Apólices

- ✅ Criar nova solicitação de apólice
- ✅ Consultar solicitação por ID
- ✅ Cancelar solicitação de apólice
- ✅ Máquina de estados com transições validadas
- ✅ Histórico completo de alterações de status
- ✅ Validação de fraude integrada
- ✅ Notificações via AWS SNS

### Fluxo de Estados

O sistema implementa uma máquina de estados robusta:

```
RECEIVED → VALIDATED → PENDING → APPROVED
    ↓           ↓          ↓
REJECTED ← ─────┴──────────┘
    ↓
CANCELED (pode ser cancelado a qualquer momento antes dos estados finais)
```

### Categorias de Seguro Suportadas

- 🚗 **AUTO** - Seguro Automotivo
- ❤️ **VIDA** - Seguro de Vida
- 🏠 **RESIDENCIAL** - Seguro Residencial
- 📦 **OUTROS** - Outros tipos de seguro

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
- **Lombok** - Redução de boilerplate

### Banco de Dados

- **MongoDB** - Banco de dados NoSQL para persistência

### Mensageria e Integração

- **AWS SNS** - Notificações assíncronas
- **Spring Cloud AWS 3.1.0** - Integração com AWS
- **LocalStack** - Emulação de serviços AWS em ambiente local

### Qualidade de Código

- **JUnit 5** - Testes unitários
- **ArchUnit** - Testes de arquitetura
- **Maven** - Gerenciamento de dependências e build

### Monitoramento

- **Spring Actuator** - Endpoints de health e métricas

## 📦 Pré-requisitos

- Java 17 ou superior
- Maven 3.8+
- Docker e Docker Compose (para MongoDB e LocalStack)
- Git

## 🔧 Instalação e Execução

### 1. Clone o repositório

```bash
git clone https://github.com/seu-usuario/emissao-apolice-seguros.git
cd emissao-apolice-seguros
```

### 2. Configure e inicie MongoDB e LocalStack

```bash
# Crie um arquivo docker-compose.yml na raiz do projeto
docker-compose up -d
```

**Exemplo de docker-compose.yml:**

```yaml
version: '3.8'
services:
  mongodb:
    image: mongo:latest
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: admin123
      MONGO_INITDB_DATABASE: insurance_db
    volumes:
      - mongodb_data:/data/db

  localstack:
    image: localstack/localstack:latest
    ports:
      - "4566:4566"
    environment:
      - SERVICES=sns
      - AWS_DEFAULT_REGION=us-east-1
    volumes:
      - "./localstack-init:/etc/localstack/init/ready.d"

volumes:
  mongodb_data:
```

### 3. Compile o projeto

```bash
mvn clean install
```

### 4. Execute a aplicação

```bash
cd order-application
mvn spring-boot:run
```

Ou execute o JAR gerado:

```bash
java -jar order-application/target/order-application-0.0.1-SNAPSHOT.jar
```

### 5. Acesse os endpoints

- **API Base URL:** `http://localhost:8080`
- **Health Check:** `http://localhost:8080/actuator/health`
- **Métricas:** `http://localhost:8080/actuator/metrics`

## 🔌 Endpoints da API

### Solicitações de Apólice

| Método | Endpoint                | Descrição                         |
|--------|-------------------------|-----------------------------------|
| POST   | `/policies`             | Criar nova solicitação de apólice |
| GET    | `/policies/{id}`        | Buscar solicitação por ID         |
| POST   | `/policies/{id}/cancel` | Cancelar solicitação de apólice   |

### Exemplo de Request - Criar Solicitação de Apólice

**POST** `/policies`

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
  "created_at": "2024-12-12T10:30:00Z"
}
```

### Exemplo de Request - Cancelar Solicitação

**POST** `/policies/{id}/cancel`

```json
{
  "reason": "Cliente solicitou cancelamento antes da aprovação"
}
```

**Response:**

```json
{
  "policy_request_id": "8a5c3e1b-9f2d-4a7e-b3c8-1d4e5f6a7b8c",
  "status": "CANCELED",
  "finished_at": "2024-12-12T11:00:00Z"
}
```

### Exemplo de Request - Consultar Solicitação

**GET** `/policies/{id}`

**Response:**

```json
{
  "policy_request_id": "8a5c3e1b-9f2d-4a7e-b3c8-1d4e5f6a7b8c",
  "status": "PENDING",
  "created_at": "2024-12-12T10:30:00Z"
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
- **Repository Pattern** - Abstração de persistência (MongoDB)
- **Factory Method Pattern** - Criação de entidades de domínio através de métodos estáticos
- **Builder Pattern** - Construção de objetos complexos (via Lombok @Builder)
- **DTO Pattern** - Transferência de dados entre camadas
- **Value Objects** - Objetos imutáveis de domínio (Money, PolicyRequestId)
- **State Machine Pattern** - Controle de transições de estado da apólice
- **Mapper Pattern** - Conversão entre DTOs e entidades de domínio

## 📈 Monitoramento e Métricas

Endpoints do Spring Actuator disponíveis:

- `/actuator/health` - Status da aplicação e dependências (MongoDB, AWS)
- `/actuator/info` - Informações da aplicação
- `/actuator/metrics` - Métricas da aplicação

## 📝 Regras de Negócio

### Transições de Estado

- ✅ Solicitações são criadas no estado **RECEIVED**
- ✅ Apenas transições válidas são permitidas
- ✅ Estados finais (**APPROVED**, **REJECTED**, **CANCELED**) não podem ser alterados
- ✅ Cancelamento só é permitido antes de atingir estado final

### Validações

- ✅ **Validação de Fraude** - Integração com API externa de análise de fraude
- ✅ **Validação de Pagamento** - Verificação de método de pagamento
- ✅ **Validação de Subscrição** - Análise de risco baseada em categoria e valor segurado

### Histórico

- ✅ Todas as alterações de estado são registradas
- ✅ Cada entrada do histórico contém: status, timestamp e motivo (quando aplicável)
- ✅ Histórico imutável e auditável

### Notificações

- ✅ Notificações automáticas via AWS SNS para eventos importantes
- ✅ Eventos notificados: criação, aprovação, rejeição e cancelamento

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
