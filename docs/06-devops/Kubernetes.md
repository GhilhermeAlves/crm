# Kubernetes — Orquestração

## Índice

- [Objetivo](#objetivo)
- [Descrição](#descrição)
- [Arquitetura](#arquitetura)
- [Manifests](#manifests)
- [Responsabilidades](#responsabilidades)
- [Dependências](#dependências)
- [Regras](#regras)
- [Futuras Melhorias](#futuras-melhorias)
- [Histórico de Revisões](#histórico-de-revisões)

---

## Objetivo

Documentar a configuração de Kubernetes para orquestração em produção.

## Descrição

Kubernetes é usado para orquestração em produção, oferecendo escalabilidade automática, alta disponibilidade e rolling updates.

## Arquitetura

```
┌─────────────────────────────────────────────┐
│                Ingress Controller            │
├─────────────────────────────────────────────┤
│  ┌─────────┐ ┌─────────┐ ┌──────────────┐  │
│  │ Backend  │ │Frontend │ │ RabbitMQ     │  │
│  │ (3 pods) │ │ (2 pods)│ │ (1 pod)      │  │
│  └─────────┘ └─────────┘ └──────────────┘  │
├─────────────────────────────────────────────┤
│  ┌─────────┐ ┌─────────┐ ┌──────────────┐  │
│  │PostgreSQL│ │  Redis   │ │   MinIO      │  │
│  │(Stateful)│ │(Stateful)│ │ (Stateful)  │  │
│  └─────────┘ └─────────┘ └──────────────┘  │
└─────────────────────────────────────────────┘
```

## Manifests

### Deployment (Backend)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: crm-backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: crm-backend
  template:
    spec:
      containers:
        - name: backend
          image: ghcr.io/becommerce/crm-backend:1.0.0
          ports:
            - containerPort: 8080
          env:
            - name: DATABASE_URL
              valueFrom:
                secretKeyRef:
                  name: db-credentials
                  key: url
          resources:
            requests:
              memory: "512Mi"
              cpu: "500m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 30
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
```

## Responsabilidades

- Orquestrar containers em produção
- Escalar horizontalmente
- Garantir alta disponibilidade
- Gerenciar secrets e configs

## Dependências

- [Docker.md](./Docker.md) — Containerização
- [CI.md](./CI.md) — Deploy automation
- [CD.md](./CD.md) — Deploy contínuo

## Regras

- Mínimo 2 réplicas para serviços críticos
- Pod Disruption Budget para manter disponibilidade
- Secrets gerenciados via External Secrets Operator
- Network Policies para isolar serviços
- Resource limits em todos os pods

## Futuras Melhorias

- Helm charts para gerenciamento
- GitOps com ArgoCD
- Service Mesh (Istio)
- Auto-scaling baseado em métricas custom
- Multi-cluster para DR

## Histórico de Revisões

| Versão | Data | Autor | Descrição |
|---|---|---|---|
| 1.0.0 | 2026-07-15 | Architect | Criação inicial |
