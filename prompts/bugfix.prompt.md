# Bugfix Prompt

## Quando utilizar

- Corrigindo um bug reportado
- Corrigindo comportamento inesperado
- Corrigindo erro em produção

## Objetivo

Corrigir o bug com impacto mínimo, mantendo o comportamento existente.

## Entrada esperada

- Descrição do bug
- Logs de erro (se disponíveis)
- Passos para reproduzir
- Comportamento esperado vs atual

## Resultado esperado

- Correção do bug
- Teste de regressão
- Documentação (se necessário)

## Arquivos normalmente envolvidos

- Arquivos do módulo afetado
- Arquivos de teste correspondentes

## Boas práticas

- **Reproduzir primeiro**: Sempre reproduzir o bug antes de corrigir.
- **Causa raiz**: Identificar a causa raiz, não apenas o sintoma.
- **Mudanças mínimas**: Corrigir com o menor impacto possível.
- **Teste de regressão**: Adicionar teste que previne recorrência.
- **Documentar**: Atualizar documentação se o bug revelar lacuna.
- **Não quebrar outras coisas**: Verificar se a correção não afeta outras funcionalidades.

## Fluxo de correção

1. Reproduzir o bug
2. Identificar causa raiz
3. Criar teste que falha (reproduz o bug)
4. Corrigir o código
5. Verificar que o teste passa
6. Verificar que outros testes continuam passando
7. Documentar se necessário

## Exemplo de uso

```
Bug: Ao criar uma Category com nome duplicado, o sistema retorna 500 ao invés de 400.

Passos para reproduzir:
1. POST /api/v1/categories com name "Teste"
2. POST /api/v1/categories com name "Teste" novamente

Comportamento esperado: 400 Bad Request
Comportamento atual: 500 Internal Server Error

Corrigir e adicionar teste de regressão.
```
