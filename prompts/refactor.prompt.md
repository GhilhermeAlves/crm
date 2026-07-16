# Refactor Prompt

## Quando utilizar

- Melhorando código existente sem mudar comportamento
- Simplificando lógica complexa
- Aplicando padrões de design

## Objetivo

Melhorar a qualidade do código sem alterar o comportamento externo.

## Entrada esperada

- Código a refatorar
- Motivação da refatoração
- Restrições (se houver)

## Resultado esperado

- Código refatorado
- Mesmos testes passando
- Comportamento idêntico

## Arquivos normalmente envolvidos

- Arquivos do módulo sendo refatorado

## Boas práticas

- **Testes antes/depois**: Garantir que todos os testes passam antes e depois.
- **Uma mudança por vez**: Refatorar em passos pequenos e verificáveis.
- **Seguir padrões existentes**: Usar os mesmos padrões já adotados no projeto.
- **Não mudar API pública**: Não alterar assinaturas de métodos públicos.
- **Commits atômicos**: Commitar cada passo da refatoração.
- **Verificar cobertura**: Garantir que testes cobrem o código refatorado.

## Tipos de refatoração comuns

- Extrair método
- Extrair classe
- Renomear variável/método
- Simplificar condicionais
- Remover código morto
- Aplicar padrão de design
- Melhorar nomes

## Exemplo de uso

```
Refatorar o método findAll do CategoryService que está muito longo:
- Extrair lógica de filtro para método privado
- Extrair lógica de paginação para método privado
- Manter comportamento idêntico
- Garantir que testes continuam passando
```
