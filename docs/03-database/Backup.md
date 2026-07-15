# Backup — Backup e Recuperação

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Estratégia](#estratégia)
- [Tipos de Backup](#tipos-de-backup)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar a estratégia de backup e recuperação do banco de dados.

## Descrição

Backups são essenciais para recuperação de desastres. O sistema usa pg_dump para backups lógicos e WAL archiving para point-in-time recovery.

## Estratégia

| Tipo | Frequência | Retenção | Storage |
|---|---|---|---|
| Full Backup | Diário (02:00) | 30 dias | S3 |
| Incremental | A cada hora | 7 dias | S3 |
| WAL Archive | Contínuo | 7 dias | S3 |
| Snapshot | Semanal | 90 dias | S3 |

## Tipos de Backup

### Full Backup

```bash
pg_dump -Fc -Z9 -f backup_full_$(date +%Y%m%d).dump crm
```

### Restore

```bash
pg_restore -d crm backup_full_20260715.dump
```

### Point-in-Time Recovery

```bash
# Restaurar até específico timestamp
recovery_target_time = '2026-07-15 10:30:00'
```

## Responsabilidades

- Executar backups automaticamente
- Testar restore periodicamente
- Manter backups seguros e encriptados
- Documentar procedimento de recovery

## Dependências

- [06-devops/Backup.md](../06-devops/Backup.md) — Infraestrutura
- [Overview.md](./Overview.md) — Database

## Regras

- Backups diários são obrigatórios
- Restore testado mensalmente
- Backups são encriptados (AES-256)
- Retenção mínima: 30 dias
- RPO: 1 hora (máximo 1h de dados perdidos)
- RTO: 4 horas (máximo 4h para restaurar)

## Futuras Melhorias

- Backup automático testado (restore + verify)
- Cross-region backup
- Backup de Redis
- Backup de RabbitMQ
- DR plan automatizado

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
