# 🚀 Deployment Workflow

## Regra de Ouro

**TODA mudança de frontend DEVE aparecer PRIMEIRO no local (`npm run dev`) ANTES de ir pra VPS.**

Modelo: **Frontend Local + Backend/Keycloak Production (VPS)**

---

## Estrutura de Branches

```
┌─ feature/frontend-refine (seu sandbox frontend)
│   ↓ testa localmente: npm run dev
│   ↓ git commit aqui PRIMEIRO
│   ↓ quando pronto → merge para deploy-phase
│
├─ feature/backend-microservices (seu sandbox backend)
│   ↓ testa localmente (se rodar backend local)
│   ↓ git commit aqui PRIMEIRO
│   ↓ quando pronto → merge para deploy-phase
│
└─ crm-improvements-deploy-phase (branch de staging - agregador)
    ↓ recebe merges de refine + microservices
    ↓ git push origin
    ↓ ssh crm-vps + deploy
    
VPS Staging/Production
    ↓ ambiente final de teste
```

**Fluxo de commits:** 
```
refine + microservices (com commits individuais)
    ↓
(quando testado e pronto)
    ↓
deploy-phase (agrupa ambas)
    ↓
VPS (deploy final)
```

---

## Passo a Passo: Frontend

### 1️⃣ Desenvolvimento em `feature/frontend-refine`
```bash
cd C:/Users/ghilh/Desktop/PROJETO/crm
git checkout feature/frontend-refine

# Abra outro terminal
cd frontend
npm run dev
# Acesse http://localhost:3000
# Faça mudanças no código
# Navegador recarrega automático (hot-reload)
```

### 2️⃣ Teste Local
- [ ] Feature funciona em `localhost:3000`?
- [ ] Conecta ao backend da VPS corretamente?
- [ ] Sem erros no console?
- [ ] Sem warnings no build?

### 3️⃣ Commit em `feature/frontend-refine` (PRIMEIRO)
```bash
# Terminal 1: parar npm run dev (Ctrl+C)
cd frontend

git add .
git commit -m "feat(frontend): descrição da mudança"

# Verificar commits
git log --oneline -3
```

### 4️⃣ Merge para `crm-improvements-deploy-phase` (DEPOIS)
```bash
# Voltar ao repo root
cd ..

# Puxar últimas mudanças de deploy-phase
git fetch origin crm-improvements-deploy-phase
git checkout crm-improvements-deploy-phase

# Merge de refine
git merge feature/frontend-refine -m "Merge: frontend updates de refine"

# Verificar merge
git log --oneline -5
```

### 5️⃣ Push para GitHub
```bash
git push origin crm-improvements-deploy-phase
```

### 6️⃣ Deploy na VPS
```bash
ssh crm-vps bash <<'DEPLOY'
cd /opt/crm/docker
docker compose pull crm-frontend
docker compose up -d crm-frontend
DEPLOY
```

---

## Passo a Passo: Backend

### 1️⃣ Desenvolvimento em `feature/backend-microservices`
```bash
cd C:/Users/ghilh/Desktop/PROJETO/crm
git checkout feature/backend-microservices

# Se vai rodar backend local (opcional)
cd backend
mvn clean install
mvn spring-boot:run  # Terminal separado

# Seu frontend local conecta via: NEXT_PUBLIC_API_URL=http://localhost:8082
```

### 2️⃣ Teste (com backend da VPS)
Opção A: **Usar backend da VPS** (mais comum)
```bash
# Frontend local já conecta ao backend da VPS por padrão
npm run dev  # Terminal separado, em ./frontend
```

Opção B: **Rodar backend local**
```bash
cd backend
mvn clean install
mvn spring-boot:run

# Em outro terminal, frontend conecta local:
cd frontend
NEXT_PUBLIC_API_URL=http://localhost:8082 npm run dev
```

### 3️⃣ Commit em `feature/backend-microservices` (PRIMEIRO)
```bash
# Depois que testou e está tudo OK

cd backend  # (se estava em backend/)
git add .
git commit -m "feat(backend): descrição da mudança"

# Verificar commits
git log --oneline -3
```

### 4️⃣ Merge para `crm-improvements-deploy-phase` (DEPOIS)
```bash
# Voltar ao repo root
cd ../..  # (ou cd ~/projeto/crm)

# Puxar últimas mudanças de deploy-phase
git fetch origin crm-improvements-deploy-phase
git checkout crm-improvements-deploy-phase

# Merge de microservices
git merge feature/backend-microservices -m "Merge: backend updates de microservices"

# Verificar merge
git log --oneline -5
```

### 5️⃣ Push para GitHub
```bash
git push origin crm-improvements-deploy-phase
```

### 6️⃣ Deploy Backend na VPS
```bash
ssh crm-vps bash <<'DEPLOY'
cd /opt/crm/docker
docker compose pull crm-backend crm-auth-service
docker compose up -d crm-backend crm-auth-service
DEPLOY
```

---

## Quando Fazer Merge para `crm-improvements-deploy-phase`?

### ✅ SEMPRE fazer merge quando:
1. Testou localmente e está funcionando
2. Sem erros de console/build
3. Backend responde corretamente
4. Commit foi feito em `refine` ou `microservices`

### ❌ NÃO fazer merge quando:
1. Feature ainda está em desenvolvimento (commit só depois)
2. Há conflitos não resolvidos
3. Testes falharam
4. Mudanças não estão commitadas

### 📋 Checklist antes de Merge

**Frontend:**
- [ ] `npm run dev` roda sem erros
- [ ] Testei a feature em `localhost:3000`
- [ ] Conecta ao backend da VPS? Ou local?
- [ ] Sem warnings em console/build
- [ ] Commit feito em `feature/frontend-refine`

**Backend:**
- [ ] `mvn clean install` OK
- [ ] Testes passam (`mvn test`)
- [ ] Se rodar local: `mvn spring-boot:run` inicia sem erros
- [ ] Logs sem erros
- [ ] Commit feito em `feature/backend-microservices`

**Depois: Merge para deploy-phase**
```bash
git checkout crm-improvements-deploy-phase
git merge feature/frontend-refine    # OU
git merge feature/backend-microservices
git push origin crm-improvements-deploy-phase
```

---

## CI/CD Automático (Futuro)

```
main (CI/CD automático)
  ↓ Tests pass → docker push → staging
  ↓ Tag v* → docker push → production + release notes
```

Hoje: Manual. Fazer PR pra `main` quando quiser automatizar.

---

## Checklist pré-Deploy

- [ ] Código funciona localmente (`npm run dev`)
- [ ] Sem erros de console
- [ ] Backend conecta corretamente
- [ ] Git commit feito
- [ ] Merge em `crm-improvements-deploy-phase`
- [ ] `git push` feito
- [ ] VPS atualizado e containers reiniciados

---

## Ambientes

| Env | Frontend | Backend | Keycloak | Conecta com | Usado para |
|---|---|---|---|---|---|
| **Local Dev** | `localhost:3000` (dev) | VPS prod | VPS prod | NEXT_PUBLIC_API_URL=https://srv1348... | Você: develop + teste |
| **Local Dev (Alt)** | `localhost:3000` (dev) | `localhost:8082` (local) | VPS prod | NEXT_PUBLIC_API_URL=http://localhost:8082 | Você: teste backend local |
| **VPS Staging** | `srv1348261.hstgr.cloud` (nginx) | VPS prod | VPS prod | Origin: backend da VPS | Teste final + deploy |
| **Production** | (futuro) | (futuro) | (futuro) | — | Usuários reais |

---

## Visão Geral do Fluxo Completo

```
DESENVOLVIMENTO (sua máquina)
├─ Terminal 1: git checkout feature/frontend-refine
│  └─ cd frontend && npm run dev → http://localhost:3000
│
└─ Terminal 2: git checkout feature/backend-microservices
   └─ (opcional) cd backend && mvn spring-boot:run

         ↓ Testou tudo? Funcionou?

COMMIT (primeiras branches)
├─ git commit -m "feat(frontend): ..." (em refine)
└─ git commit -m "feat(backend): ..." (em microservices)

         ↓ Mergeia quando OK

STAGING (deploy-phase)
├─ git checkout crm-improvements-deploy-phase
├─ git merge feature/frontend-refine
├─ git merge feature/backend-microservices
└─ git push origin crm-improvements-deploy-phase

         ↓ Deploy manual na VPS

PRODUÇÃO (VPS)
├─ docker compose pull
└─ docker compose up -d
   └─ Mudanças agora visíveis em srv1348261.hstgr.cloud
```

---

**Última atualização:** 2026-09-24
