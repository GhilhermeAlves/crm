# Backup — Backup e Recuperação (DevOps)

## Índice

- [Objetivo](#objetivo)
- [Estratégia](#estratégia)
- [Procedimentos](#procedimentos)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar procedimentos de backup e recuperação de desastres.

## Estratégia

| Componente | Método | Frequência | Retenção |
|---|---|---|---|
| PostgreSQL | pg_dump + WAL | Diário + hourly | 30d + 7d |
| Redis | RDB + AOF | A cada hora | 7 dias |
| RabbitMQ | Export | Diário | 7 dias |
| MinIO/S3 | Versioning | Contínuo | 90 dias |
| Application Config | Git | Contínuo | Indefinido |

## Procedimentos

### Restore de Database

```bash
# 1. Parar aplicação
kubectl scale deployment crm-backend --replicas=0

# 2. Restaurar database
pg_restore -d crm backup_20260715.dump

# 3. Verificar integridade
psql -d crm -c "SELECT count(*) FROM users;"

# 4. Reiniciar aplicação
kubectl scale deployment crm-backend --replicas=3
```

### DR Recovery

```bash
# 1. Criar novo cluster K8s
# 2. Aplicar manifests
kubectl apply -f k8s/

# 3. Restaurar database
# 4. Atualizar DNS
# 5. Verificar serviços
```

## Responsabilidades

- Executar backups automaticamente
- Testar restore mensalmente
- Documentar procedimentos de recovery
- Manter backup offsite

## Dependências

- [03-database/Backup.md](../03-database/Backup.md) — Database backup
- [Kubernetes.md](./Kubernetes.md) — Infraestrutura

## Regras

- Backup diário é obrigatório
- Restore testado mensalmente
- RPO: 1 hora
- RTO: 4 horas
- Backup encriptado (AES-256)
- Backup em região diferente (cross-region)

## Futuras Melhorias

- DR automatizado (failover)
- Backup de Kubernetes configs
- Chaos testing para validação
- Point-in-time recovery automatizado

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
