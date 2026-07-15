# Logs — Sistema de Logs

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Fluxo](#fluxo)
- [Níveis de Log](#níveis-de-log)
- [Formato](#formato)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o sistema de logs, incluindo níveis, formato, armazenamento e retenção.

## Descrição

Logs estruturados são fundamentais para debugging, monitoramento e auditoria. O sistema usa formato JSON estruturado com correlação de requests via trace ID.

## Responsabilidades

- Registrar logs em todas as camadas
- Formatar logs em JSON estruturado
- Correlacionar requests via trace ID
- Armazenar logs com retenção definida
- Facilitar busca e análise de logs

## Níveis de Log

| Nível | Quando Usar | Exemplo |
|---|---|---|
| ERROR | Erros que precisam de atenção | Falha de conexão com DB |
| WARN | Situações anormais mas recuperáveis | Rate limit atingido |
| INFO | Eventos normais do sistema | Request processado |
| DEBUG | Informações detalhadas para debug | Query SQL executada |
| TRACE | Informações muito detalhadas | Variáveis de entrada |

## Formato

```json
{
  "timestamp": "2026-07-15T10:30:00.000Z",
  "level": "INFO",
  "logger": "com.becommerce.crm.lead.service.LeadService",
  "traceId": "abc-123-def-456",
  "spanId": "789",
  "companyId": "uuid-da-empresa",
  "userId": "uuid-do-usuario",
  "message": "Lead created successfully",
  "context": {
    "leadId": "uuid-do-lead",
    "origin": "WHATSAPP"
  },
  "duration": 45,
  "httpMethod": "POST",
  "httpUrl": "/api/v1/leads",
  "httpStatusCode": 201,
  "remoteAddress": "192.168.1.1"
}
```

## Fluxo

```
1. Evento ocorre (request, erro, business event)
        │
2. Logger captura dados estruturados
        │
3. Log é serializado em JSON
        │
4. Log é escrito (async buffer)
        │
5. Log shipper envia para centralização
        │
6. Log é indexado e disponível para busca
```

## Dependências

- [Audit.md](./Audit.md) — Auditoria de ações
- [06-devops/Logs.md](../06-devops/Logs.md) — Infraestrutura de logs

## Regras

- Logs nunca devem conter senhas ou tokens
- Dados sensíveis devem ser mascarados
- Trace ID deve ser propagado em todas as camadas
- Logs de erro devem incluir stack trace
- Logs de request devem incluir duração
- Retenção: 30 dias online, 1 ano em archive

## Futuras Melhorias

- Distributed tracing com OpenTelemetry
- Alertas baseados em logs (error rate spike)
- IA para detecção de anomalias
- Log aggregation em tempo real
- Integración com ELK Stack ou Loki

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
