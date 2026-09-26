#!/usr/bin/env bash
# =============================================================================
# Deploy manual do CRM na VPS (produção) — roda no SEU PC (Git Bash).
#
#   scripts/deploy.sh                       # deploy de backend, auth-service e frontend
#   scripts/deploy.sh backend               # só os serviços informados
#   scripts/deploy.sh --rollback [servicos] # volta para a imagem anterior
#
# Passos: (1) confere branch/commit/CI verde → (2) backup do banco na VPS →
# (3) envia o commit (git archive) para /opt/crm/releases/<sha> → (4) build das
# imagens na VPS com o commit gravado → (5) sobe os containers → (6) confere o
# health; se falhar, mostra os logs e o comando de rollback.
#
# Variáveis: VPS_HOST (default crm-vps), DEPLOY_BRANCH, ALLOW_RED_CI=1 (pula a
# checagem de CI — só em emergência).
# =============================================================================
set -euo pipefail

VPS_HOST=${VPS_HOST:-crm-vps}
DEPLOY_BRANCH=${DEPLOY_BRANCH:-crm-improvements-deploy-phase}
REGISTRY=ghcr.io/ghilhermealves/crm
REMOTE_ROOT=/opt/crm
ALL_SERVICES="backend auth-service frontend"

log()  { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }
fail() { printf '\n\033[1;31mERRO: %s\033[0m\n' "$*" >&2; exit 1; }

MODE=deploy
if [ "${1:-}" = "--rollback" ]; then MODE=rollback; shift; fi
SERVICES=${*:-$ALL_SERVICES}
for s in $SERVICES; do
  case " $ALL_SERVICES " in *" $s "*) ;; *) fail "serviço desconhecido: $s (use: $ALL_SERVICES)";; esac
done

# ---------------------------------------------------------------------------
# Script remoto de health: espera cada container ficar healthy (ou running,
# se não tiver healthcheck). Sai 1 e mostra logs se algum falhar.
# ---------------------------------------------------------------------------
remote_wait_health() {
  ssh -o BatchMode=yes "$VPS_HOST" "bash -s" -- $SERVICES <<'EOS'
set -u
deadline=$(( $(date +%s) + 300 ))
for svc in "$@"; do
  c="crm-$svc"
  while :; do
    st=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$c" 2>/dev/null || echo missing)
    case "$st" in
      healthy|running) echo "  ✅ $c: $st"; break ;;
      unhealthy|exited|dead|missing)
        echo "  ❌ $c: $st"; docker logs --tail 80 "$c" 2>&1; exit 1 ;;
    esac
    [ "$(date +%s)" -lt "$deadline" ] || { echo "  ❌ $c: timeout ($st)"; docker logs --tail 80 "$c" 2>&1; exit 1; }
    sleep 5
  done
done
EOS
}

# ---------------------------------------------------------------------------
# Rollback: :previous → :latest e recria os containers.
# ---------------------------------------------------------------------------
if [ "$MODE" = rollback ]; then
  log "Rollback de: $SERVICES"
  ssh -o BatchMode=yes "$VPS_HOST" "bash -s" -- "$REGISTRY" "$REMOTE_ROOT" $SERVICES <<'EOS'
set -euo pipefail
REGISTRY=$1; ROOT=$2; shift 2
COMPOSE="docker compose -f $ROOT/docker/docker-compose.yml"
for svc in "$@"; do
  docker image inspect "$REGISTRY/$svc:previous" >/dev/null 2>&1 \
    || { echo "sem imagem :previous para $svc"; exit 1; }
  docker tag "$REGISTRY/$svc:previous" "$REGISTRY/$svc:latest"
done
$COMPOSE up -d --no-deps --pull never "$@"
EOS
  remote_wait_health || fail "rollback subiu mas o health falhou — veja os logs acima"
  log "Rollback concluído. Obs.: migrations de banco NÃO são desfeitas (restaure o backup se preciso)."
  exit 0
fi

# ---------------------------------------------------------------------------
# 1) Conferências locais
# ---------------------------------------------------------------------------
log "1/6 Conferindo branch, commit e CI"
cd "$(git rev-parse --show-toplevel)"
[ "$(git branch --show-current)" = "$DEPLOY_BRANCH" ] \
  || fail "faça o deploy a partir de '$DEPLOY_BRANCH' (git switch $DEPLOY_BRANCH)"
git diff --quiet HEAD || fail "há alterações não commitadas"
git fetch -q origin "$DEPLOY_BRANCH"
SHA=$(git rev-parse HEAD)
[ "$SHA" = "$(git rev-parse "origin/$DEPLOY_BRANCH")" ] \
  || fail "seu commit local difere do origin — faça git pull/push antes"
SHORT=${SHA:0:7}
echo "  commit: $SHORT  $(git log -1 --format=%s)"

if [ "${ALLOW_RED_CI:-0}" = 1 ]; then
  echo "  ⚠️  ALLOW_RED_CI=1 — pulando checagem de CI"
else
  command -v gh >/dev/null || fail "gh (GitHub CLI) não encontrado"
  CI=$(gh run list --workflow "CI Pipeline" --commit "$SHA" --limit 1 --json status,conclusion \
        -q '.[0] | "\(.status)/\(.conclusion)"' 2>/dev/null || true)
  case "$CI" in
    completed/success) echo "  ✅ CI verde" ;;
    "")                fail "nenhum CI encontrado para $SHORT" ;;
    *)                 fail "CI de $SHORT não está verde ($CI)" ;;
  esac
fi

# ---------------------------------------------------------------------------
# 2) Backup do banco
# ---------------------------------------------------------------------------
log "2/6 Backup do banco na VPS"
ssh -o BatchMode=yes "$VPS_HOST" "$REMOTE_ROOT/scripts/backup-db.sh"

# ---------------------------------------------------------------------------
# 3) Envia o código do commit
# ---------------------------------------------------------------------------
REL=$REMOTE_ROOT/releases/$SHORT
log "3/6 Enviando $SHORT para $REL"
git archive --format=tar "$SHA" | ssh -o BatchMode=yes "$VPS_HOST" "rm -rf '$REL' && mkdir -p '$REL' && tar -x -C '$REL'"

# ---------------------------------------------------------------------------
# 4) Build na VPS (guarda a imagem atual como :previous para rollback)
# ---------------------------------------------------------------------------
log "4/6 Build das imagens: $SERVICES"
ssh -o BatchMode=yes "$VPS_HOST" "bash -s" -- "$REGISTRY" "$REL" "$SHA" $SERVICES <<'EOS'
set -euo pipefail
REGISTRY=$1; REL=$2; SHA=$3; shift 3
for svc in "$@"; do
  case $svc in frontend) ctx=frontend-refine ;; *) ctx=$svc ;; esac
  # Imagem em uso agora vira :previous (rollback).
  cur=$(docker inspect -f '{{.Image}}' "crm-$svc" 2>/dev/null || true)
  [ -n "$cur" ] && docker tag "$cur" "$REGISTRY/$svc:previous"
  echo "--- build $svc ($ctx)"
  docker build -q \
    --label "org.opencontainers.image.revision=$SHA" \
    -t "$REGISTRY/$svc:latest" -t "$REGISTRY/$svc:${SHA:0:7}" \
    "$REL/$ctx"
done
EOS

# ---------------------------------------------------------------------------
# 5) Sobe os containers com as imagens novas
# ---------------------------------------------------------------------------
log "5/6 Subindo containers"
ssh -o BatchMode=yes "$VPS_HOST" "bash -s" -- "$REL" "$REMOTE_ROOT" $SERVICES <<'EOS'
set -euo pipefail
REL=$1; ROOT=$2; shift 2
COMPOSE="docker compose -f $ROOT/docker/docker-compose.yml"
# Compose versionado do commit (o antigo fica como .bak para comparação).
cp "$ROOT/docker/docker-compose.yml" "$ROOT/docker/docker-compose.yml.bak"
cp "$REL/docker/docker-compose.yml" "$ROOT/docker/docker-compose.yml"
$COMPOSE up -d --no-deps --pull never "$@"
EOS

# ---------------------------------------------------------------------------
# 6) Health
# ---------------------------------------------------------------------------
log "6/6 Conferindo health (até 5 min)"
if ! remote_wait_health; then
  fail "deploy de $SHORT com health falhando. Para voltar: scripts/deploy.sh --rollback $SERVICES"
fi

# Mantém só as 5 releases mais recentes.
ssh -o BatchMode=yes "$VPS_HOST" "cd $REMOTE_ROOT/releases && ls -1t | tail -n +6 | xargs -r rm -rf"

log "✅ Deploy de $SHORT concluído ($SERVICES)"
