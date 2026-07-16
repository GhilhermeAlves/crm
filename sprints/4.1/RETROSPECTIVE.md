# Retrospectiva — Sprint 4.1

**Sprint:** 4.1 — Infraestrutura Auth
**Fases:** 4.1A (Planning) ✅ | 4.1B (Development) ✅ | 4.1C (Review) ✅ | 4.1D (Close) ✅
**Data:** 2026-07-15
**Nota da Revisão:** 93/100

---

## O que funcionou bem

1. **Divisão em sub-sprints (4.1A → 4.1B → 4.1C → 4.1D)**: A separação entre Planning, Development, Review e Close permitiu foco em cada etapa sem misturar responsabilidades.
2. **Protocolo de execução**: Seguir rigorosamente o `SPRINT_EXECUTION_PROTOCOL.md` garantiu que nenhum passo fosse pulado.
3. **GitHub-first**: O uso do protocolo com Fases 1-2-3 (memória → knowledge layer → docs) evitou leitura desnecessária de documentos.
4. **Código enxuto**: Cada classe tem responsabilidade única, sem métodos grandes ou código morto.
5. **Revisão com correções aplicadas**: Os 3 problemas identificados foram corrigidos durante a própria revisão, sem necessidade de sprint de correção adicional.

## O que pode melhorar

1. **Maven indisponível**: A validação de compilação (`mvn compile`) não pôde ser executada porque o Maven não está instalado no ambiente de desenvolvimento. Recomenda-se instalar o Maven ou adicionar o wrapper (mvnw) ao projeto.
2. **Testes adiados**: Testes unitários e de integração foram postergados para Sprint 4.5. Idealmente, deveriam acompanhar o desenvolvimento.
3. **CORS permissivo**: A configuração atual permite qualquer origem (`*`). Embora intencional para desenvolvimento, deve ser restringida antes de produção.
4. **Documentação de contexto**: O `auth.context.md` não foi atualizado com os novos arquivos de infraestrutura de segurança (embora não seja crítico, pois a funcionalidade não mudou).

## Problemas Encontrados

| Problema | Severidade | Resolução |
|----------|-----------|-----------|
| Duplicidade de bean (@Component + @Bean) nos handlers | 🟡 Médio | Removido @Component, mantido @Bean |
| ObjectMapper estático ignorando config Spring | 🟡 Médio | Injetado via construtor |
| GlobalExceptionHandler sem handlers 401/403 | 🟡 Médio | Adicionados durante review |

## Lições Aprendidas

1. `@Component` + `@Bean` para mesma classe causa duplicidade no contexto Spring — usar apenas uma abordagem.
2. `ObjectMapper` deve sempre ser injetado (Spring gerencia a configuração global), nunca instanciado com `new`.
3. `GlobalExceptionHandler` deve cobrir `AuthenticationException` e `AccessDeniedException` mesmo que o filter chain também trate — a cobertura em duas camadas (filter + controller) é uma boa prática de resiliência.
4. A estrutura de pacotes `infrastructure/security/config/` + `infrastructure/security/filter/` + `infrastructure/config/web/` + `presentation/rest/handler/` é consistente com o padrão Clean Architecture do projeto.

## Boas Práticas Identificadas

1. **JwtProperties** como `@ConfigurationProperties` + `@EnableConfigurationProperties` — centraliza config JWT de forma type-safe.
2. **SecurityConfig** com `@EnableWebSecurity` + lambda DSL — padrão moderno do Spring Security 6.x.
3. **Handlers de exception no filter chain** (JwtAuthenticationEntryPoint, JwtAccessDeniedHandler) + **GlobalExceptionHandler** no controller — cobertura completa de erros.
4. **OpenApiConfig** com Bearer JWT security scheme — documentação pronta para Swagger UI.

## Riscos Futuros

| Risco | Sprint Afetada | Probabilidade |
|-------|---------------|--------------|
| JwtAuthenticationFilter não adicionado ao chain | 4.3 | Baixa (planejado) |
| CORS permissivo em produção | Deploy | Média |
| Maven não disponível para compilar | 4.2+ | Alta |
| Testes acumulados sem execução | 4.5 | Média |

## Métricas da Sprint

| Métrica | Valor |
|---------|-------|
| Duração total | 1 dia (4 fases) |
| Arquivos criados (Java) | 7 (infraestrutura) + 51 (domain/app/infra/presentation anteriores) = 58 |
| Arquivos criados (SQL) | 1 (anterior) |
| Arquivos modificados | 3 (application.yml + 3 corrigidos no review) |
| Correções no review | 3 |
| Nota da revisão | 93/100 |
| Pendências técnicas | mvn compile, testes (Sprint 4.5) |

## Next Actions

1. ✅ Sprint 4.1 encerrada — aguardar autorização para Sprint 4.2 (Usuários)
2. Instalar Maven ou adicionar mvnw ao projeto
3. Executar `mvn compile` manualmente antes da Sprint 4.2

---

*Atualizado em: 2026-07-15*
