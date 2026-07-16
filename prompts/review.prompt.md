# Review Prompt - Code Review

## Quando utilizar

- Revisando código antes de commit
- Revisando PR
- Verificando qualidade do código

## Objetivo

Revisar código buscando qualidade, segurança e aderência aos padrões do projeto.

## Entrada esperada

- Código a revisar (arquivo ou diff)
- Contexto da mudança (feature, bugfix, refactor)

## Resultado esperado

- Lista de achados com severidade
- Sugestões de melhoria
- Verificação de boas práticas

## Arquivos normalmente envolvidos

- Qualquer arquivo modificado

## Boas práticas

- **SOLID principles**: Verificar Single Responsibility, Open/Closed, etc.
- **Segredos no código**: Nunca aceitar chaves, senhas ou secrets hardcoded.
- **Naming conventions**: Verificar se nomes seguem o padrão do projeto.
- **Test coverage**: Verificar se há testes para o código modificado.
- **N+1 queries**: Verificar consultas que podem causar N+1 no banco.
- **Tratamento de erros**: Verificar se erros são tratados adequadamente.
- **Validação**: Verificar se inputs são validados.
- **Performance**: Verificar se há impacto negativo na performance.

## Níveis de severidade

- **CRITICAL**: Deve ser corrigido antes do merge
- **WARNING**: Deve ser avaliado e provavelmente corrigido
- **INFO**: Sugestão de melhoria opcional
- **NITPICK**: Preferência de estilo

## Exemplo de uso

```
Revisar o código do módulo Categories que foi implementado hoje:
- Verificar arquitetura Clean Architecture
- Verificar validações
- Verificar testes
- Verificar segurança
```
