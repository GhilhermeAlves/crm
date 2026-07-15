# Storage — Armazenamento (S3)

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Configuração](#configuração)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar a integração com S3-compatible storage para arquivos.

## Descrição

Arquivos são armazenados em S3 (AWS ou MinIO para dev). Suporta upload, download, thumbnails e cleanup.

## Configuração

```properties
storage.provider=s3
storage.s3.bucket=crm-files
storage.s3.region=us-east-1
storage.s3.access-key=${AWS_ACCESS_KEY}
storage.s3.secret-key=${AWS_SECRET_KEY}
storage.s3.endpoint=http://localhost:9000  # MinIO para dev
```

## Dependências

- [01-backend/FileStorage.md](../01-backend/FileStorage.md) — Lógica de storage
- [02-frontend/Upload.md](../02-frontend/Upload.md) — Upload UI

## Regras

- URLs pré-assinadas para upload/download
- TTL de URLs: 15 minutos
- Tamanho máximo: 25MB
- Thumbnails automáticos para imagens
- Cleanup semanal de arquivos órfãos
- Isolamento por tenant

## Futuras Melhorias

- CDN para distribuição global
- Image processing (resize, compress)
- Virus scan
- Deduplicação
- Storage tiering (hot/warm/cold)

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
