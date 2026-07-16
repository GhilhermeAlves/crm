# Testing Prompt

## Quando utilizar

- Criando testes unitários
- Criando testes de integração
- Criando testes E2E
- Aumentando cobertura de testes

## Objetivo

Criar testes que garantam qualidade e prevenham regressões.

## Entrada esperada

- Código a testar
- Tipo de teste (unitário, integração, E2E)
- Casos de teste desejados

## Resultado esperado

- Arquivos de teste
- Casos de teste cobrindo happy path e edge cases
- Cobertura adequada

## Arquivos normalmente envolvidos

```
backend/src/test/java/com/becommerce/crm/{module}/
  └── {Service}Test.java

frontend/src/**/*.test.ts
```

## Boas práticas

- **Cobertura mínima**: 80% unitários, 60% integração.
- **Mocks para deps externas**: Mockar banco de dados, APIs externas.
- **Edge cases**: Testar limites, nulls, vazios, inválidos.
- **Nomes descritivos**: `should_return_empty_list_when_no_categories_exist`.
- **AAA pattern**: Arrange, Act, Assert.
- **Isolamento**: Cada teste deve ser independente.
- **Dados de teste**: Usar fixtures ou builders para dados de teste.

## Tipos de teste

### Unitário
- Testa uma unidade isolada (classe, método)
- Rápido, sem dependências externas
- Mocka dependências

### Integração
- Testa integração entre componentes
- Usa banco de dados real (ou testcontainers)
- Verifica fluxo completo

### E2E
- Testa o sistema completo
- Simula uso real do usuário
- Mais lento, menos testes

## Exemplo de uso

```
Criar testes unitários para CategoryServiceImpl:
- should_create_category_when_valid_data
- should_throw_exception_when_name_is_blank
- should_throw_exception_when_name_already_exists
- should_return_category_when_valid_id
- should_throw_exception_when_category_not_found
- should_soft_delete_category_when_exists
```
