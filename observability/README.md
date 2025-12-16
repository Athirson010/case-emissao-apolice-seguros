# 📊 Stack de Observabilidade - Grafana LGTM

Este diretório contém a configuração completa da stack de observabilidade usando **Grafana, Loki, Tempo e Prometheus** (
LGTM Stack).

---

## 🎯 **Componentes da Stack**

| Componente        | Função              | Porta | URL                   |
|-------------------|---------------------|-------|-----------------------|
| **Grafana**       | Dashboard unificado | 3000  | http://localhost:3000 |
| **Loki**          | Agregação de logs   | 3100  | http://localhost:3100 |
| **Promtail**      | Coleta de logs      | 9080  | -                     |
| **Tempo**         | Distributed tracing | 3200  | http://localhost:3200 |
| **Prometheus**    | Métricas            | 9090  | http://localhost:9090 |
| **Node Exporter** | Métricas do sistema | 9100  | http://localhost:9100 |

---

## 🚀 **Como Executar**

### 1. Iniciar a Stack Principal (MongoDB, RabbitMQ, etc)

```bash
docker-compose up -d
```

### 2. Iniciar a Stack de Observabilidade

```bash
docker-compose -f docker-compose.observability.yaml up -d
```

### 3. Verificar Status

```bash
docker-compose -f docker-compose.observability.yaml ps
```

### 4. Acessar Grafana

Abra: http://localhost:3000

**Credenciais**:

- **Usuário**: `admin`
- **Senha**: `admin`

---

## 📊 **Dashboards Disponíveis**

### 1. **Métricas da Aplicação (Prometheus)**

Após fazer login no Grafana:

1. Vá em **Explore** → Selecione **Prometheus**
2. Query de exemplo:

```promql
# Taxa de requisições HTTP
rate(http_server_requests_seconds_count[5m])

# Uso de memória JVM
jvm_memory_used_bytes

# Taxa de erros
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
```

### 2. **Logs da Aplicação (Loki)**

1. Vá em **Explore** → Selecione **Loki**
2. Query de exemplo:

```logql
# Todos os logs da aplicação
{job="order-service"}

# Logs de erro
{job="order-service"} |= "ERROR"

# Logs com filtro por nível
{job="order-service"} | json | level="ERROR"
```

### 3. **Traces (Tempo)**

1. Vá em **Explore** → Selecione **Tempo**
2. Pesquise por Trace ID ou use Service Graph

---

## 🔧 **Configuração da Aplicação Spring Boot**

### 1. Adicionar Dependências no `pom.xml`

```xml
<!-- Micrometer para métricas -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

        <!-- Distributed Tracing com OpenTelemetry -->
<dependency>
<groupId>io.micrometer</groupId>
<artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>

<dependency>
<groupId>io.opentelemetry</groupId>
<artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>

        <!-- Logback para Loki (opcional - melhora integração) -->
<dependency>
<groupId>com.github.loki4j</groupId>
<artifactId>loki-logback-appender</artifactId>
<version>1.5.1</version>
</dependency>
```

### 2. Configurar `application.properties`

```properties
# ============================================
# OBSERVABILITY - Metrics, Logs, Traces
# ============================================
# Expor endpoint Prometheus
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.metrics.export.prometheus.enabled=true
# Distributed Tracing com OpenTelemetry
management.tracing.sampling.probability=1.0
management.otlp.tracing.endpoint=http://localhost:4318/v1/traces
# Logs estruturados (JSON) para melhor integração com Loki
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
logging.file.name=./logs/order-service.log
logging.logback.rollingpolicy.max-file-size=10MB
logging.logback.rollingpolicy.max-history=10
```

### 3. Criar diretório de logs

```bash
mkdir logs
```

---

## 📈 **Dashboards Recomendados**

Importe dashboards prontos do Grafana:

1. **JVM Micrometer** - ID: `4701`
2. **Spring Boot** - ID: `12900`
3. **Loki Logs** - ID: `13639`
4. **RabbitMQ** - ID: `10991`

**Como importar**:

1. Grafana → Dashboards → Import
2. Cole o ID do dashboard
3. Selecione o datasource correto

---

## 🔍 **Queries Úteis**

### **Prometheus (Métricas)**

```promql
# Latência P95 das requisições
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Requisições por segundo
rate(http_server_requests_seconds_count[1m])

# Taxa de erro
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
/
sum(rate(http_server_requests_seconds_count[5m]))

# Uso de memória heap
jvm_memory_used_bytes{area="heap"}

# Threads ativas
jvm_threads_live_threads
```

### **Loki (Logs)**

```logql
# Logs de erro nos últimos 5 minutos
{job="order-service"} |= "ERROR"

# Logs de uma transaction específica
{job="order-service"} |= "policyId=8a5c3e1b"

# Contagem de logs por nível
sum by (level) (count_over_time({job="order-service"} [5m]))

# Logs com latência > 1s
{job="order-service"}
| json
| duration > 1000
```

### **Tempo (Traces)**

- Pesquise por **Trace ID** (extraído dos logs)
- Use **Service Graph** para visualizar dependências
- Filtre por **Duration** para encontrar requisições lentas

---

## 🛠️ **Troubleshooting**

### Problema: Grafana não encontra datasources

**Solução**: Verifique se os serviços estão rodando:

```bash
docker-compose -f docker-compose.observability.yaml ps
```

### Problema: Prometheus não coleta métricas da aplicação

**Solução**:

1. Verifique se `/actuator/prometheus` está acessível:

```bash
curl http://localhost:8080/actuator/prometheus
```

2. Verifique se a aplicação está acessível do container:

```bash
curl http://host.docker.internal:8080/actuator/prometheus
```

### Problema: Loki não recebe logs

**Solução**:

1. Verifique se o diretório `./logs` existe
2. Verifique se a aplicação está escrevendo logs em `./logs/order-service.log`
3. Restart do Promtail:

```bash
docker-compose -f docker-compose.observability.yaml restart promtail
```

---

## 📚 **Documentação Adicional**

- [Grafana Docs](https://grafana.com/docs/)
- [Loki Docs](https://grafana.com/docs/loki/latest/)
- [Tempo Docs](https://grafana.com/docs/tempo/latest/)
- [Prometheus Docs](https://prometheus.io/docs/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

---

## 🔄 **Comandos Úteis**

```bash
# Iniciar tudo
docker-compose up -d && docker-compose -f docker-compose.observability.yaml up -d

# Ver logs do Grafana
docker-compose -f docker-compose.observability.yaml logs -f grafana

# Restart de um serviço
docker-compose -f docker-compose.observability.yaml restart prometheus

# Parar observabilidade (mantém dados)
docker-compose -f docker-compose.observability.yaml stop

# Parar e remover tudo (PERDE DADOS)
docker-compose -f docker-compose.observability.yaml down -v

# Ver uso de recursos
docker stats
```

---

## ✅ **Checklist de Integração**

- [ ] `docker-compose.observability.yaml` executando
- [ ] Grafana acessível em http://localhost:3000
- [ ] Datasources configurados automaticamente
- [ ] Aplicação Spring Boot com dependências de observabilidade
- [ ] Endpoint `/actuator/prometheus` exposto
- [ ] Logs sendo escritos em `./logs/order-service.log`
- [ ] Prometheus coletando métricas da aplicação
- [ ] Loki recebendo logs via Promtail
- [ ] Dashboards importados e funcionando

---

**Stack de Observabilidade completa e pronta para uso!** 📊🔍✨
