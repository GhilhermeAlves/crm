# CD — Deploy Contínuo

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Pipeline](#pipeline)
- [Ambientes](#ambientes)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o pipeline de Deploy Contínuo (CD).

## Descrição

CD garante que código validado chegue em produção de forma segura e automatizada, com rolling updates e rollback automático.

## Pipeline

### Deploy Flow

```
Merge to main
    │
2. CI Pipeline (build + test)
    │
3. Build Docker Images
    │
4. Push to Registry
    │
5. Deploy to Staging
    │
6. Smoke Tests (automated)
    │
7. Deploy to Production (manual approval)
    │
8. Canary Release (10% → 50% → 100%)
    │
9. Monitor for 15 minutes
    │
10. Rollback if errors detected
```

## Ambientes

| Ambiente | Uso | Trigger | Auto-deploy |
|---|---|---|---|
| Development | Desenvolvimento | Push to feature/* | Sim |
| Staging | QA/UAT | Merge to main | Sim |
| Production | Produção | Manual approval | Não |

### Staging

- Espelho de produção (menor escala)
- Dados de teste (seed)
- Testes automatizados rodando
- Deploy automático

### Production

- Deploy manual (aprovação)
- Canary release (10% → 100%)
- Monitoramento intensivo por 15 min
- Rollback automático se error rate > 5%

## Responsabilidades

- Deploy seguro e automatizado
- Rolling updates sem downtime
- Rollback rápido em caso de erro
- Monitoramento pós-deploy

## Dependências

- [CI.md](./CI.md) — Integração contínua
- [Kubernetes.md](./Kubernetes.md) — Orquestração
- [Monitoring.md](./Monitoring.md) — Monitoramento
- [03-database/Migrations.md](../03-database/Migrations.md) — Migrations

## Regras

- Deploy em horário comercial (9h-18h)
- Rollback disponível em < 5 minutos
- Database migrations são backward-compatible
- Feature flags para releases graduais
- Deploy log é mantido por 90 dias

## Futuras Melhorias

- GitOps com ArgoCD
- Blue-green deployment
- A/B testing de features
- Auto-rollback baseado em métricas
- Deploy via Slack command

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
