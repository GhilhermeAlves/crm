# Deployment Prompt

## Quando utilizar

- Configurando deploy da aplicação
- Criando/modificando Dockerfiles
- Configurando CI/CD
- Configurando ambientes

## Objetivo

Configurar deployment seguro, escalável e automatizado.

## Entrada esperada

- Detalhes do ambiente (dev, staging, prod)
- Serviços necessários (banco, cache, filas)
- Configurações de infraestrutura

## Resultado esperado

- Dockerfiles otimizados
- Configurações CI/CD
- Scripts de deploy
- Configurações de ambientes

## Arquivos normalmente envolvidos

```
docker/
  ├── docker-compose.yml
  ├── docker-compose.dev.yml
  ├── docker-compose.prod.yml
  └── Dockerfile

.github/workflows/
  ├── ci.yml
  ├── cd.yml
  └── release.yml

.env.example
.env.development
.env.production
```

## Boas práticas

- **Multi-stage builds**: Usar múltiplos estágios no Dockerfile para reduzir tamanho da imagem.
- **Health checks**: Implementar endpoints de health check.
- **Resource limits**: Definir limites de CPU e memória.
- **Secrets management**: Nunca hardcodar secrets, usar variáveis de ambiente ou vault.
- **Rollback strategy**: Ter plano de rollback para cada deploy.
- **Logs estruturados**: Usar logs em JSON para facilitar monitoramento.
- **Cache de build**: Usar cache de dependências para builds rápidos.

## Fluxo de deploy

1. Build da aplicação
2. Rodar testes
3. Build da imagem Docker
4. Push para registry
5. Deploy no ambiente
6. Verificar health check
7. Monitorar logs

## Exemplo de uso

```
Configurar Docker Compose para desenvolvimento:
- Backend (Spring Boot)
- Frontend (Next.js)
- PostgreSQL 16
- Redis
- Flyway para migrations
```
