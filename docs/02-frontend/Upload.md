# Upload — Upload de Arquivos

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Componentes](#componentes)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar o componente de upload de arquivos.

## Descrição

Componente de upload que suporta drag-and-drop, seleção de arquivo, preview e upload direto para S3 via URL pré-assinada.

## Componentes

| Componente | Descrição |
|---|---|
| FileUpload | Container principal com drag-and-drop |
| UploadPreview | Preview do arquivo selecionado |
| UploadProgress | Barra de progresso |
| UploadList | Lista de arquivos enviados |

## Responsabilidades

- Upload com drag-and-drop
- Validação de tipo e tamanho
- Preview de imagens
- Progress bar durante upload
- Retry em caso de falha

## Dependências

- [01-backend/FileStorage.md](../01-backend/FileStorage.md) — API de upload

## Regras

- Tamanho máximo: 25MB
- Tipos aceitos: imagens, documentos, áudio, vídeo
- Upload direto para S3 (não via backend)
- Progress bar exibida durante upload
- Erros exibidos inline

## Futuras Melhorias

- Upload múltiplo
- Pausar/retomar upload
- Crop de imagens antes do upload
- Virus scan no cliente
- Upload offline (fila de upload)

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
