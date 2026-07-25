# File Lifecycle

## Objetivo

Documentar o ciclo de vida completo dos arquivos no CRM SaaS Omnichannel, desde o upload até a exclusão, incluindo organização de armazenamento, validação de tipos e tamanhos, geração de thumbnails, verificação de vírus, soft delete, limpeza de órfãos e políticas de retenção.

## Escopo

- Upload flow (presigned URL, multipart, streaming)
- Organização de armazenamento por tenant e contexto
- Tipos de arquivo permitidos e limites de tamanho
- Geração de thumbnails para imagens e PDFs
- Verificação de vírus (ClamAV)
- Soft delete e recuperação
- Limpeza de arquivos órfãos
- Políticas de retenção por tipo de arquivo
- Integração com object storage (S3/MinIO)
- Metadados de arquivo no PostgreSQL

## Responsabilidades

| Responsável | Responsabilidade |
|---|---|
| FileUploadService | Orquestração do upload: validação, presigning, persistência de metadados |
| FileStorageService | Abstração de storage: S3/MinIO; organização de paths por tenant |
| ThumbnailService | Geração de thumbnails para imagens (JPEG/PNG/WebP) e primeira página de PDFs |
| VirusScanService | Verificação de vírus via ClamAV; bloqueio de arquivos infectados |
| FileMetadataRepository | Persistência de metadados (nome, tipo, tamanho, path, tenant) no PostgreSQL |
| CleanupScheduler | Job agendado para limpeza de arquivos órfãos e expirados |
| RetentionPolicyService | Aplicação de políticas de retenção por tipo de arquivo e tenant |

## Fluxos

### Fluxo de Upload

```mermaid
sequenceDiagram
    participant Client as Frontend (Next.js 14)
    participant API as FileUploadService
    participant S3 as Object Storage (S3/MinIO)
    participant VS as VirusScanService
    participant TS as ThumbnailService
    participant DB as PostgreSQL

    Client->>API: Request upload (filename, type, size)
    API->>API: Valida tipo MIME e tamanho
    API->>S3: Gera presigned URL (PUT)
    S3-->>API: Presigned URL
    API-->>Client: Presigned URL
    Client->>S3: Upload direto via presigned URL
    S3-->>Client: 200 OK

    par Pós-upload
        API->>VS: Escaneia arquivo (async)
        VS-->>API: CLEAN / INFECTED
        alt Infectado
            API->>S3: Remove arquivo
            API->>DB: Marca como QUARANTINED
            API-->>Client: Erro: arquivo infectado
        end
    and
        API->>TS: Gera thumbnail (async)
        TS-->>API: Thumbnail URL
        TS->>S3: Salva thumbnail
    end

    API->>DB: Salva metadados (status: AVAILABLE)
    API-->>Client: File metadata (id, url, thumbnail)
```

### Organização de Armazenamento

```mermaid
flowchart TD
    Root[bucket-name/] --> T1[tenant-abc-123/]
    Root --> T2[tenant-def-456/]
    Root --> T3[tenant-ghi-789/]

    T1 --> C1[contacts/]
    T1 --> C2[deals/]
    T1 --> C3[tickets/]
    T1 --> C4[profiles/]
    T1 --> C5[templates/]

    C1 --> F1[contact-uuid/filename.pdf]
    C2 --> F2[deal-uuid/screenshot.png]
    C3 --> F3[ticket-uuid/attachment.docx]

    Root --> T[Templates de email]
    T --> T1[tmpl/welcome.png]

    Root --> TH[Thumbnails/]
    TH --> TH1[tenant-abc-123/]
```

### Verificação de Vírus

```mermaid
flowchart TD
    A[Arquivo recebido] --> B[Envia para ClamAV]
    B --> C{Resultado}
    C -->|CLEAN| D[Status: AVAILABLE]
    C -->|INFECTED| E[Status: QUARANTINED]
    C -->|ERROR| F[Status: PENDING_SCAN]
    E --> G[Remove do storage]
    E --> H[Alerta admin]
    E --> I[Log de segurança]
    F --> J[Retry scan após 5min]
    J --> B
```

### Soft Delete e Limpeza

```mermaid
flowchart TD
    A[Usuário deleta arquivo] --> B[Status: SOFT_DELETED]
    B --> C[Registra deleted_at]
    C --> D{Período de carência}
    D -->|< 30 dias| E[Restaurável]
    D -->|> 30 dias| F[Status: DELETED]
    F --> G[Remove do storage]
    G --> H[Remove metadados]

    subgraph Limpeza de Órfãos
        I[Cleanup Job - diário] --> J[Busca arquivos sem referência]
        J --> K{Arquivo sem referência > 7 dias?}
        K -->|Sim| L[Marca como ORPHAN]
        L --> M[Agenda remoção em 24h]
        K -->|Não| N[Ignora]
    end
```

### Políticas de Retenção

```mermaid
flowchart LR
    subgraph Retenção por Tipo
        A[Attachments] -->|90 dias| R1[Remoção automática]
        B[Thumbnails] -->|30 dias| R2[Regeneração]
        C[Templates] -->|Sem expiração| R3[Mantido]
        D[Profile Images] -->|Sem expiração| R4[Mantido]
        E[Exports] -->|7 dias| R5[Remoção automática]
        F[Backups] -->|365 dias| R6[Remoção automática]
    end
```

## Dependências

| Dependência | Finalidade |
|---|---|
| PostgreSQL 16 | Metadados de arquivo (nome, tipo, tamanho, path, status, tenant) |
| Redis 7 | Cache de presigned URLs; controle de upload rate limiting |
| S3/MinIO | Object storage para arquivos e thumbnails |
| ClamAV | Verificação de vírus |
| Spring Boot 3 | Framework de orquestração |
| Java 25 | Linguagem de implementação |
| Apache Tika | Detecção de tipo MIME real (bypass de extensão) |
| Thumbnailator / imgscalr | Geração de thumbnails |
| Next.js 14 (frontend) | Interface de upload e preview de arquivos |

## Boas Práticas

- **Validação server-side**: sempre validar tipo MIME real via Apache Tika, não apenas extensão do arquivo
- **Presigned URLs**: usar presigned URLs para upload direto ao S3; não passar arquivos pelo backend
- **Tenant isolation**: cada tenant deve ter um prefixo separado no bucket; nunca compartilhar paths entre tenants
- **Soft delete**: arquivos nunca devem ser removidos imediatamente; sempre usar soft delete com período de carência
- **Idempotência**: upload com mesmo hash (SHA-256) deve reutilizar arquivo existente (deduplicação)
- **Thumbnails sob demanda**: gerar thumbnails no primeiro acesso, não no upload; cachear no Redis
- **Scan async**: verificação de vírus deve ser assíncrona; não bloquear o upload; marcar como PENDING_SCAN até conclusão
- **Cleanup job**: executar limpeza diariamente; remover arquivos ORPHAN após 24h; remover SOFT_DELETED após 30 dias
- **Rate limiting**: limitar upload por tenant (ex: 100MB/hora para planos free)
- **Logging**: registrar todas as operações de arquivo com tenant_id, user_id, action; não logar conteúdo
- **Backup de metadados**: metadados no PostgreSQL devem ser backupados; arquivos no S3 devem ter versionamento ativado
- **Max file size**: limitar por tipo (attachments: 25MB, profile images: 5MB, exports: 50MB)

## Referências

- AWS S3 Presigned URLs: https://docs.aws.amazon.com/AmazonS3/latest/userguide/PresignedUrl.html
- Apache Tika MIME Detection: https://tika.apache.org/
- ClamAV: https://www.clamav.net/
- LGPD - Art. 15 (eliminação de dados após finalidade)

## Histórico de Revisão

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0 | 2026-07-15 | Paulo Alves | Criação inicial do documento |
