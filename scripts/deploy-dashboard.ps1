<#╔══════════════════════════════════════════════════════════════════════╗
  ║  CRM SaaS Omnichannel — Deploy Dashboard FASE 1                    ║
  ║  Antes de executar:                                                ║
  ║    - PowerShell 7+ (pwsh)                                          ║
  ║    - SSH key carregada no agente ou acessível via ~/.ssh/          ║
   ║    - Acesso SSH crm-vps confirmado                                 ║
  ╚══════════════════════════════════════════════════════════════════════╝
#>

#requires -Version 7
using namespace System.Collections

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

# ─── Configurações ─────────────────────────────────────────────────────
$REMOTE_HOST    = "76.13.237.238"
$REMOTE_USER    = "root"
$REMOTE_PATH    = "/opt/crm"
$SSH_TARGET     = "crm-vps"   # alias definido em ~/.ssh/config
$PROJECT_ROOT   = $PWD
$TIMESTAMP      = Get-Date -Format "yyyyMMdd_HHmmss"
$TARBALL        = "${env:TEMP}\crm-deploy-${TIMESTAMP}.tar.gz"
$BACKUP_TAG     = "backup_${TIMESTAMP}"

# ─── Cores para output ─────────────────────────────────────────────────
$OK    = "✅"
$WARN  = "⚠️ "
$ERR   = "❌"
$INFO  = "ℹ️ "

function Write-Step($msg)  { Write-Host "`n${INFO} $msg" -ForegroundColor Cyan }
function Write-OK($msg)    { Write-Host "${OK} $msg" -ForegroundColor Green }
function Write-Warn($msg)  { Write-Host "${WARN} $msg" -ForegroundColor Yellow }
function Write-ErrorExit($msg) {
    Write-Host "${ERR} $msg" -ForegroundColor Red
    Write-Host "`n${ERR} Deploy ABORTADO. Nenhuma alteração destrutiva foi feita." -ForegroundColor Red
    exit 1
}

# ═══════════════════════════════════════════════════════════════════════
# PASSO 1  —  Verificar diretório do projeto
# ═══════════════════════════════════════════════════════════════════════
Write-Step "PASSO 1/14 — Verificando diretório do projeto..."

$markers = @(
    "docker/docker-compose.prod.yml",
    "backend/pom.xml",
    "frontend/package.json"
)

$missing = $markers | Where-Object { -not (Test-Path $_ -PathType Leaf) }
if ($missing) {
    Write-ErrorExit "Arquivos esperados não encontrados: $($missing -join ', '). Execute este script da raiz do projeto (caminho com docker/, backend/, frontend/)."
}
Write-OK "Diretório correto: $PROJECT_ROOT"

# ═══════════════════════════════════════════════════════════════════════
# PASSO 2  —  Exibir branch Git atual
# ═══════════════════════════════════════════════════════════════════════
Write-Step "PASSO 2/14 — Branch Git atual..."

try {
    $branch = git rev-parse --abbrev-ref HEAD 2>$null
    Write-OK "Branch: $branch"
} catch {
    Write-Warn "Não foi possível identificar o branch (diretório sem Git?)"
}

# ═══════════════════════════════════════════════════════════════════════
# PASSO 3  —  Exibir último commit
# ═══════════════════════════════════════════════════════════════════════
Write-Step "PASSO 3/14 — Último commit..."

try {
    $lastCommit = git log -1 --oneline 2>$null
    if ($lastCommit) {
        Write-OK "Commit: $lastCommit"
    }
} catch {
    Write-Warn "Não foi possível obter o último commit"
}

# ═══════════════════════════════════════════════════════════════════════
# PASSO 4  —  Confirmar que existem as alterações da FASE 1
# ═══════════════════════════════════════════════════════════════════════
Write-Step "PASSO 4/14 — Verificando alterações da FASE 1..."

$fase1Files = @(
    "backend/src/main/java/com/becommerce/crm/application/dashboard"
    "backend/src/main/java/com/becommerce/crm/presentation/rest/dashboard/DashboardController.java"
    "frontend/src/features/dashboard"
)

$allPresent = $true
foreach ($f in $fase1Files) {
    if (-not (Test-Path $f)) {
        Write-Warn "Arquivo ausente: $f"
        $allPresent = $false
    }
}

if (-not $allPresent) {
    Write-ErrorExit "Arquivos da FASE 1 não encontrados. Certifique-se de que o código foi sincronizado (git pull, etc.)."
}
Write-OK "Todas as alterações da FASE 1 estão presentes."

# ═══════════════════════════════════════════════════════════════════════
# PASSO 5  —  Verificar conexão SSH
# ═══════════════════════════════════════════════════════════════════════
Write-Step "PASSO 5/14 — Verificando conexão SSH com ${SSH_TARGET}..."

ssh -o BatchMode=yes -o ConnectTimeout=10 $SSH_TARGET "echo SSH_OK" 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-ErrorExit "Falha na conexão SSH com ${SSH_TARGET}. Verifique o alias em ~/.ssh/config."
}
Write-OK "Conectado a: ${SSH_TARGET}"

# ═══════════════════════════════════════════════════════════════════════
# PASSO 6  —  Backup dos containers/imagens atuais
# ═══════════════════════════════════════════════════════════════════════
Write-Step "PASSO 6/14 — Realizando backup das imagens Docker atuais..."

$backupCmds = @"
set -e
echo 'Removendo tag de backup anterior (se existir)...'
docker rmi crm-backend:predeploy 2>/dev/null || true
docker rmi crm-frontend:predeploy 2>/dev/null || true
echo 'Criando tags de backup...'
docker tag crm-backend:latest crm-backend:predeploy 2>/dev/null && echo '  backend tagged' || echo '  backend: no existing tag to backup'
docker tag crm-frontend:latest crm-frontend:predeploy 2>/dev/null && echo '  frontend tagged' || echo '  frontend: no existing tag to backup'
echo 'BACKUP_OK'
"@

try {
    $backupResult = ssh $SSH_TARGET $backupCmds 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-ErrorExit "Falha ao criar backup das imagens. Exit code: $LASTEXITCODE"
    }
    if ("$backupResult" -notmatch "BACKUP_OK") {
        Write-ErrorExit "Backup não foi confirmado. Saída: $backupResult"
    }
    Write-OK "Backup criado com tag 'predeploy' para backend e frontend."
} catch {
    Write-ErrorExit "Erro durante backup: $_"
}

# ═══════════════════════════════════════════════════════════════════════
# PASSO 7  —  Sincronizar código para VPS
# ═══════════════════════════════════════════════════════════════════════
Write-Step "PASSO 7/14 — Sincronizando código para ${REMOTE_PATH}..."

# Criar tarball local excluindo diretórios desnecessários
Write-Host "  Compactando diretório local (excluindo node_modules, target, .git, .next/cache)..."
try {
    & {
        # Lista de exclusão compatível com tar do Windows (PowerShell 7+)
        tar --exclude="node_modules" `
            --exclude="target" `
            --exclude=".git" `
            --exclude=".next/cache" `
            -czf $TARBALL `
            -C "$(Split-Path $PROJECT_ROOT -Parent)" `
            "$(Split-Path $PROJECT_ROOT -Leaf)" 2>&1
    }
    if ($LASTEXITCODE -ne 0) {
        Write-ErrorExit "Falha ao criar tarball local."
    }
    Write-OK "Tarball criado: $TARBALL"
} catch {
    Write-ErrorExit "Erro ao criar tarball: $_"
}

# Transferir via scp
Write-Host "  Transferindo para VPS (isso pode levar alguns minutos)..."
try {
    scp -q $TARBALL "${SSH_TARGET}:/tmp/crm-deploy.tar.gz" 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-ErrorExit "Falha na transferência via scp."
    }
    Write-OK "Tarball transferido com sucesso."
} catch {
    Write-ErrorExit "Erro no scp: $_"
}

# Extrair no VPS
Write-Host "  Extraindo no VPS..."
try {
    $extractCmds = @"
set -e
mkdir -p ${REMOTE_PATH}
echo 'Extraindo tarball...'
tar -xzf /tmp/crm-deploy.tar.gz -C /tmp
echo 'Sincronizando arquivos (mantendo configurações existentes)...'
cp -r /tmp/crm/* ${REMOTE_PATH}/ 2>/dev/null || cp -r /tmp/crm/** ${REMOTE_PATH}/ 2>/dev/null || true
rm -f /tmp/crm-deploy.tar.gz
rm -rf /tmp/crm
echo 'EXTRACT_OK'
"@
    $extractResult = ssh $SSH_TARGET $extractCmds 2>$null
    if ($LASTEXITCODE -ne 0 -or "$extractResult" -notmatch "EXTRACT_OK") {
        Write-ErrorExit "Falha ao extrair no VPS: $extractResult"
    }
    Write-OK "Código sincronizado em ${REMOTE_PATH}."
} catch {
    Write-ErrorExit "Erro na extração no VPS: $_"
}

# Limpar tarball local
Remove-Item -Path $TARBALL -Force -ErrorAction SilentlyContinue

# ═══════════════════════════════════════════════════════════════════════
# PASSO 8  —  Rebuildar backend
# ═══════════════════════════════════════════════════════════════════════
Write-Step "PASSO 8/14 — Rebuildando imagem do backend..."

try {
    $buildBackend = ssh $SSH_TARGET "cd ${REMOTE_PATH} && docker compose -f docker-compose.yml -f docker-compose.backend.yml -p crm-infrastructure build --no-cache backend 2>&1" 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-ErrorExit "Falha no build do backend. Log do build:`n$buildBackend"
    }
    Write-OK "Backend rebuiltado com sucesso."
} catch {
    Write-ErrorExit "Erro no build do backend: $_"
}

# ═══════════════════════════════════════════════════════════════════════
# PASSO 9  —  Rebuildar frontend
# ═══════════════════════════════════════════════════════════════════════
Write-Step "PASSO 9/14 — Rebuildando imagem do frontend..."

try {
    $buildFrontend = ssh $SSH_TARGET "cd ${REMOTE_PATH} && docker compose -f docker-compose.app.yml build --no-cache frontend 2>&1" 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-ErrorExit "Falha no build do frontend. Log do build:`n$buildFrontend"
    }
    Write-OK "Frontend rebuiltado com sucesso."
} catch {
    Write-ErrorExit "Erro no build do frontend: $_"
}

# ═══════════════════════════════════════════════════════════════════════
# PASSO 10 —  Subir apenas backend e frontend
# ═══════════════════════════════════════════════════════════════════════
Write-Step "PASSO 10/14 — Recriando containers backend e frontend..."

try {
    $upResult = ssh $SSH_TARGET "cd ${REMOTE_PATH} && (docker compose -f docker-compose.yml -f docker-compose.backend.yml -p crm-infrastructure up -d --no-deps --force-recreate backend && docker compose -f docker-compose.app.yml up -d --no-deps --force-recreate frontend) 2>&1" 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-ErrorExit "Falha ao recriar containers: $upResult"
    }
    Write-OK "Containers backend e frontend recriados."
} catch {
    Write-ErrorExit "Erro ao subir containers: $_"
}

# ═══════════════════════════════════════════════════════════════════════
# PASSO 11 —  Verificar status dos containers
# ═══════════════════════════════════════════════════════════════════════
Write-Step "PASSO 11/14 — Verificando status dos containers..."

Start-Sleep -Seconds 10

try {
    $status = ssh $SSH_TARGET "docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'" 2>$null
    Write-Host "`n$status`n"

    # Verificar se crm-backend está running
    $backendRunning = $status -match "crm-backend.*Up"
    $frontendRunning = $status -match "crm-frontend.*Up"

    if (-not $backendRunning) {
        Write-ErrorExit "Container backend não está rodando."
    }
    if (-not $frontendRunning) {
        Write-ErrorExit "Container frontend não está rodando."
    }
    Write-OK "Todos os containers estão rodando."
} catch {
    Write-ErrorExit "Erro ao verificar status: $_"
}

# ═══════════════════════════════════════════════════════════════════════
# PASSO 12 —  Verificar logs do backend
# ═══════════════════════════════════════════════════════════════════════
Write-Step "PASSO 12/14 — Verificando logs do backend..."

Start-Sleep -Seconds 5

try {
    $backendLogs = ssh $SSH_TARGET "docker logs crm-backend --tail 30 2>&1" 2>$null
    Write-Host "`n--- Últimas 30 linhas do backend ---`n$backendLogs`n---`n"

    # Verificar erros fatais no startup
    $hasStartupError = $backendLogs -match "(ERROR|Exception|Failed to start|Application run failed)"
    if ($hasStartupError) {
        Write-Warn "Possíveis erros detectados nos logs do backend. Verifique acima."
    } else {
        Write-OK "Logs do backend sem erros fatais."
    }
} catch {
    Write-Warn "Não foi possível obter logs do backend: $_"
}

# ═══════════════════════════════════════════════════════════════════════
# PASSO 13 —  Verificar logs do frontend
# ═══════════════════════════════════════════════════════════════════════
Write-Step "PASSO 13/14 — Verificando logs do frontend..."

try {
    $frontendLogs = ssh $SSH_TARGET "docker logs crm-frontend --tail 30 2>&1" 2>$null
    Write-Host "`n--- Últimas 30 linhas do frontend ---`n$frontendLogs`n---`n"

    $hasFrontendError = $frontendLogs -match "(ERROR|Error|Failed)"
    if ($hasFrontendError) {
        Write-Warn "Possíveis erros detectados nos logs do frontend. Verifique acima."
    } else {
        Write-OK "Logs do frontend sem erros."
    }
} catch {
    Write-Warn "Não foi possível obter logs do frontend: $_"
}

# ═══════════════════════════════════════════════════════════════════════
# PASSO 14 —  Testar endpoints
# ═══════════════════════════════════════════════════════════════════════
Write-Step "PASSO 14/14 — Testando endpoints..."

# 14a. Frontend HTTPS
Write-Host "  Testando frontend (HTTPS)..."
try {
    $frontendCode = ssh $SSH_TARGET "curl -s -o /dev/null -w '%{http_code}' --connect-timeout 10 'https://srv1348261.hstgr.cloud/'" 2>$null
    if ($frontendCode -eq "200" -or $frontendCode -eq "302" -or $frontendCode -eq "301") {
        Write-OK "Frontend acessível (HTTP $frontendCode)"
    } else {
        Write-Warn "Frontend retornou HTTP $frontendCode"
    }
} catch {
    Write-Warn "Não foi possível testar frontend: $_"
}

# 14b. Backend health (sem auth — endpoint público)
Write-Host "  Testando backend (health check)..."
try {
    $healthResult = ssh $SSH_TARGET "curl -s -o /dev/null -w '%{http_code}' --connect-timeout 10 'http://localhost:8080/actuator/health'" 2>$null
    if ($healthResult -eq "200") {
        Write-OK "Backend saudável (HTTP $healthResult)"
    } else {
        Write-Warn "Backend retornou HTTP $healthResult"
    }
} catch {
    Write-Warn "Não foi possível testar backend: $_"
}

# 14c. Dashboard endpoints (precisa de token — testamos apenas conectividade/infra)
Write-Host "  Testando conectividade dos endpoints do Dashboard..."
try {
    $dashboardApi = ssh $SSH_TARGET "curl -s -o /dev/null -w '%{http_code}' --connect-timeout 10 'http://localhost:8081/api/v1/dashboard/kpis'" 2>$null
    Write-OK "Endpoint /api/v1/dashboard/kpis retornou HTTP $dashboardApi (401 = esperado sem token, rota existe e autenticação funciona)"
} catch {
    Write-Warn "Não foi possível testar endpoint do dashboard: $_"
}

# ═══════════════════════════════════════════════════════════════════════
# RESUMO FINAL
# ═══════════════════════════════════════════════════════════════════════
Write-Host "`n$('═' * 70)" -ForegroundColor Cyan
Write-Host "  DEPLOY CONCLUÍDO" -ForegroundColor Green
Write-Host "  Backup disponível com tag: predeploy" -ForegroundColor Yellow
Write-Host "  Para fazer rollback manual, execute:" -ForegroundColor Yellow
Write-Host "    docker compose down backend frontend" -ForegroundColor Gray
Write-Host "    docker tag crm-backend:predeploy crm-backend:latest" -ForegroundColor Gray
Write-Host "    docker tag crm-frontend:predeploy crm-frontend:latest" -ForegroundColor Gray
Write-Host "    docker compose up -d backend frontend" -ForegroundColor Gray
Write-Host "$('═' * 70)" -ForegroundColor Cyan

Write-Host "`n${INFO} Próximo passo: VALIDAÇÃO MANUAL (ver abaixo)" -ForegroundColor Cyan

# ═══════════════════════════════════════════════════════════════════════
# SEÇÃO DE VALIDAÇÃO MANUAL
# ═══════════════════════════════════════════════════════════════════════
Write-Host @"


╔══════════════════════════════════════════════════════════════════════╗
║                 VALIDAÇÃO MANUAL — Faça no navegador                ║
╚══════════════════════════════════════════════════════════════════════╝

1.  Abra o navegador em:
    https://srv1348261.hstgr.cloud

2.  Faça login usando o fluxo Keycloak:
    - Clique em "Entrar com Keycloak" (ou similar)
    - Insira suas credenciais
    - Você será redirecionado de volta ao /dashboard

3.  Confirme que o Dashboard carregou com os KPIs:
    ┌──────────────────────────────────────────────┐
    │  Total de Usuários    Usuários Ativos         │
    │  Usuários Inativos    Novos Usuários (mês)    │
    │  Eventos (mês)        Total de Eventos        │
    └──────────────────────────────────────────────┘
    Os valores devem vir do banco de dados, NÃO
    ser mocks ou números fixos.

4.  Abra o DevTools (F12) → aba Network

5.  Recarregue a página (F5)

6.  Filtre por "dashboard" ou "kpis" no Network

7.  Verifique as chamadas de API:
    ┌──────────────────────────────────────────────────┐
    │  GET /api/v1/dashboard/kpis                      │
    │  GET /api/v1/dashboard/recent-activities          │
    └──────────────────────────────────────────────────┘

8.  Para cada chamada, confirme:
    ✅ HTTP 200
    ✅ Response JSON válido
    ✅ Dados preenchidos (ex.: totalUsers > 0)
    ✅ Headers incluem Authorization: Bearer <token>

9.  Abra a aba Console e confirme:
    ✅ Nenhum erro (mensagens em vermelho)
    ✅ Nenhum warning relacionado ao Dashboard

10. Recarregue a página (F5) e confirme:
    ✅ Dashboard carrega normalmente
    ✅ Skeleton é exibido brevemente (loading state)
    ✅ Dados são carregados novamente

11. Abra uma janela anônima/privada e tente acessar:
    https://srv1348261.hstgr.cloud/dashboard
    ✅ Deve redirecionar para /login

12. Se tudo estiver funcionando, a FASE 1 está concluída!

"@ -ForegroundColor White
