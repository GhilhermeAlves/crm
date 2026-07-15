# FileStorage — Armazenamento de Arquivos

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Responsabilidades](#responsabilidades)
- [Fluxo](#fluxo)
- [Endpoints](#endpoints)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o módulo de armazenamento de arquivos, incluindo upload, download e organização.

## Descrição

Arquivos são armazenados em S3-compatible storage (MinIO para desenvolvimento, AWS S3 para produção). O sistema suporta upload de imagens, documentos, áudio e vídeo para mensagens, profiles e documentos.

## Responsabilidades

- Upload de arquivos com validação
- Download seguro com URLs temporárias
- Organização por tenant e contexto
- Thumbnails automáticos para imagens
- Limpeza de arquivos órfãos

## Organização

```
storage/
├── tenant/
│   └── {companyId}/
│       ├── contacts/
│       │   └── {contactId}/
│       │       └── avatar/
│       ├── messages/
│       │   └── {conversationId}/
│       │       └── {messageId}/
│       ├── campaigns/
│       │   └── {campaignId}/
│       │       └── media/
│       └── documents/
│           └── {type}/
│               └── {filename}
```

## Fluxo

### Upload

```
1. Usuário seleciona arquivo
        │
2. Frontend valida tipo e tamanho
        │
3. Backend gera URL pré-assinada
        │
4. Frontend faz upload direto para S3
        │
5. Backend confirma e registra metadata
        │
6. Thumbnail é gerada (se imagem)
```

### Download

```
1. Usuário solicita arquivo
        │
2. Backend verifica permissão
        │
3. Backend gera URL temporária (15 min)
        │
4. URL é retornada ao cliente
        │
5. Cliente baixa arquivo
```

## Endpoints

| Método | Endpoint | Descrição | Permissão |
|---|---|---|---|
| POST | `/api/v1/files/upload` | Upload de arquivo | `file:write` |
| GET | `/api/v1/files/{id}/download` | URL de download | `file:read` |
| DELETE | `/api/v1/files/{id}` | Deletar arquivo | `file:delete` |
| GET | `/api/v1/files` | Listar arquivos | `file:read` |

## Dependências

- [Companies.md](./Companies.md) — Isolamento por tenant
- [Messages.md](./Messages.md) — Anexos de mensagens
- [03-database/Overview.md](../03-database/Overview.md) — Metadata dos arquivos

## Regras

- Tamanho máximo: 25MB por arquivo
- Tipos aceitos: imagens (jpg, png, gif, webp), documentos (pdf, doc, xls), áudio (mp3, ogg, wav), vídeo (mp4, webm)
- Arquivos são isolados por tenant
- URLs temporárias expiram em 15 minutos
- Arquivos deletados são soft deleted
- Limpeza de arquivos órfãos: semanal

## Futuras Melhorias

- CDN para distribuição global
- Processamento de mídia (compressão, resize)
- OCR para documentos
- Virus scan automático
- Deduplicação de arquivos
- Streaming de áudio/vídeo

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
