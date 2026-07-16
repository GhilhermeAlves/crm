# Documentation Policy — Padrões de Documentação

## Objetivo

Definir padrões e convenções para toda a documentação do projeto.

## Escopo

Todos os arquivos Markdown em `docs/` e `docs-ai/`.

## Como utilizar

Consulte antes de criar ou modificar qualquer documento.

## Padrões

### Estrutura de um documento

Todo documento oficial em `docs/` deve conter:

```markdown
# Título

## Objetivo
[O que este documento descreve]

## Escopo
[O que está coberto]

## Como utilizar
[Como ler e aplicar]

## Conteúdo
[Documentação principal]

## Referências
[Links para outros documentos]

## Histórico de Revisão
| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | YYYY-MM-DD | Autor | Criação inicial |
```

### Estrutura de arquivos em `docs-ai/`

Arquivos em `docs-ai/` são **apenas navegação**. Devem conter:

- `Objetivo` — O que o arquivo faz
- `Escopo` — O que cobre
- `Como utilizar` — Instruções de uso
- `Referências` — Links para docs oficiais
- `Histórico de Revisão` — Versionamento

**NUNCA** conter detalhes de implementação.

### Convenções de nomenclatura

| Elemento | Formato | Exemplo |
|----------|---------|---------|
| Arquivo | PascalCase.md | `Auth.md`, `Leads.md` |
| Seção | ## Title Case | `## Como utilizar` |
| Tabela | Pipe-separated | `| Coluna | Coluna |` |
| Link interno | Relative path | `[Auth](../docs/01-backend/Auth.md)` |
| Link externo | Full URL | `[GitHub](https://...)` |

### Estrutura de pastas `docs/`

```
docs/
├── 00-core/          # Visão geral, padrões, decisões
├── 01-backend/       # Documentação backend por módulo
├── 02-frontend/      # Documentação frontend por módulo
├── 03-database/      # Banco de dados
├── 04-integrations/  # Integrações externas
├── 05-business-rules/ # Regras de negócio
├── 06-devops/        # DevOps e infraestrutura
└── 07-roadmap/       # Roadmap e planejamento
```

### Convenções de conteúdo

- **Idioma:** Português do Brasil para seções, inglês para código
- **Verbos:** Usar imperativo em "Como utilizar" ("Consulte", "Leia", "Acesse")
- **Links:** Sempre usar caminhos relativos para outros docs do projeto
- **Tabelas:** Usar para listagens e mapeamentos
- **Código:** Usar blocos fenced com linguagem especificada
- **Listas:** Usar `-` para listas não ordenadas, `1.` para ordenadas

### Versionamento

- `1.0` — Criação inicial
- `1.1+` — Atualizações incrementais
- `2.0` — Reestruturação significativa
- Data no formato ISO: `YYYY-MM-DD`

### Revisão de documentos

1. Criar ou modificar documento
2. Seguir estrutura padrão
3. Adicionar entrada no Histórico de Revisão
4. Verificar links internos
5. Testar renderização Markdown

## Referências

- Estrutura de pastas: `docs/00-core/FolderStructure.md`
- Convenções de código: `docs/00-core/CodingStandards.md`
- Política de mudança: [CHANGE_POLICY.md](CHANGE_POLICY.md)

## Histórico de Revisão

| Versão | Data | Autor | Alteração |
|--------|------|-------|-----------|
| 1.0 | 2026-07-15 | AI Agent | Criação inicial |
