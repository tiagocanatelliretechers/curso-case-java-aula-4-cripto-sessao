#!/usr/bin/env bash
# =====================================================================
# Curso CASE Java — script único de ambiente (Linux/macOS).
# Sobe TODA a infra do laboratório com um comando.
#
#   ./scripts/start.sh                 sobe app + banco (Docker) -> http://localhost:8080
#   ./scripts/start.sh --lab 3         faz checkout de aula-3-baseline e sobe
#   ./scripts/start.sh --lab 3 --hard  usa a solução (aula-3-hardened) em vez do baseline
#   ./scripts/start.sh --sonar         sobe também o SonarQube (Aula 5) em :9000
#   ./scripts/start.sh --dev           sobe só o banco; roda a app via ./mvnw (editar/debug)
#   ./scripts/start.sh --no-docker     SEM Docker: roda a app em H2 (memória)
#   ./scripts/start.sh --check         só verifica os pré-requisitos
#   ./scripts/start.sh --stop          derruba tudo
#   ./scripts/start.sh --help
# =====================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
APP="$ROOT/portal-pedidos"
COMPOSE="$ROOT/infra/docker-compose.yml"

LAB=""; SONAR=0; DEV=0; NODOCKER=0; CHECK=0; STOP=0; HARD=0
while [ $# -gt 0 ]; do
  case "$1" in
    --lab) LAB="${2:-}"; shift 2;;
    --sonar) SONAR=1; shift;;
    --dev) DEV=1; shift;;
    --no-docker|--nodocker|--h2) NODOCKER=1; shift;;
    --check) CHECK=1; shift;;
    --stop) STOP=1; shift;;
    --hard|--hardened) HARD=1; shift;;
    -h|--help) grep '^#' "$0" | sed 's/^# \{0,1\}//'; exit 0;;
    *) echo "Opção desconhecida: $1  (use --help)"; exit 1;;
  esac
done

c(){ printf '\033[%sm%s\033[0m\n' "$1" "$2"; }
ok(){ c "0;32" "  [ok] $1"; }
warn(){ c "1;33" "  [!] $1"; }
err(){ c "0;31" "  [x] $1"; }

DCOMPOSE=""
detect_compose(){ if docker compose version >/dev/null 2>&1; then DCOMPOSE="docker compose"; elif command -v docker-compose >/dev/null 2>&1; then DCOMPOSE="docker-compose"; fi; }

check_prereqs(){
  echo "Verificando pré-requisitos..."; local fail=0
  if command -v java >/dev/null 2>&1; then ok "Java: $(java -version 2>&1 | head -1)";
  elif [ "$NODOCKER" = "1" ] || [ "$DEV" = "1" ]; then err "Java 17+ não encontrado (necessário neste modo)"; fail=1;
  else warn "Java 17+ não encontrado (ok: o modo Docker não precisa)"; fi
  if command -v git >/dev/null 2>&1; then ok "Git: $(git --version)"; else err "Git não encontrado"; fail=1; fi
  if [ "$NODOCKER" = "1" ]; then
    ok "Modo sem Docker (H2) — Docker não é necessário."
  else
    if command -v docker >/dev/null 2>&1; then
      if docker info >/dev/null 2>&1; then ok "Docker: $(docker --version)"; else err "Docker instalado, mas o daemon não está rodando. Sem Docker? use --no-docker"; fail=1; fi
    else err "Docker não encontrado. Para rodar sem Docker (H2): ./scripts/start.sh --no-docker"; fail=1; fi
    detect_compose
    if [ -n "$DCOMPOSE" ]; then ok "Compose: $DCOMPOSE"; else err "Docker Compose não encontrado"; fail=1; fi
  fi
  return $fail
}

print_access(){
  echo; c "1;36" "================ AMBIENTE PRONTO ================"
  echo "  App .............. http://localhost:8080"
  [ "$SONAR" = "1" ] && echo "  SonarQube ........ http://localhost:9000  (admin/admin)"
  echo "  Banco (Postgres) . localhost:5433  (portal/portal)"
  echo; echo "  Contas de teste:"
  echo "    admin@portal.com / admin123"
  echo "    joao@acme.com    / senha123"
  echo "    maria@globex.com / senha123"
  echo; echo "  Parar tudo:  ./scripts/start.sh --stop"
  c "1;36" "================================================"
}

tune_sysctl_for_sonar(){
  if [ "$(uname)" = "Linux" ]; then
    local cur; cur="$(cat /proc/sys/vm/max_map_count 2>/dev/null || echo 0)"
    [ "$cur" -lt 262144 ] && { warn "SonarQube precisa de vm.max_map_count>=262144 (atual: $cur)."; warn "Rode: sudo sysctl -w vm.max_map_count=262144"; }
  fi
}

cd "$ROOT"

if [ "$STOP" = "1" ]; then
  detect_compose; echo "Derrubando a infra..."; $DCOMPOSE -f "$COMPOSE" --profile sonar down; ok "Infra encerrada."; exit 0
fi

if ! check_prereqs; then echo; err "Pré-requisitos faltando. Veja materiais/MANUAL-AMBIENTE-ALUNO.md"; exit 1; fi
[ "$CHECK" = "1" ] && { echo; ok "Ambiente ok."; exit 0; }

if [ -n "$LAB" ]; then
  tag="aula-${LAB}-baseline"; [ "$HARD" = "1" ] && tag="aula-${LAB}-hardened"
  echo "Selecionando o codebase do lab: $tag"
  git rev-parse -q --verify "refs/tags/$tag" >/dev/null || { err "Tag $tag não existe:"; git tag | sed 's/^/    /'; exit 1; }
  if ! git diff --quiet || ! git diff --cached --quiet; then warn "Há alterações não commitadas — salvando em stash automático..."; git stash push -u -m "auto antes de $tag" || true; fi
  git checkout -q "$tag"; ok "Codebase em $tag"
fi

if [ "$NODOCKER" = "1" ]; then
  echo "Modo SEM DOCKER: app em H2 (memória), sem banco/containers..."
  cd "$APP"
  export PORTAL_JWT_SECRET="${PORTAL_JWT_SECRET:-AyjpJmIAw5EyzdygA5Ydd2vm8pp5Sqed7wDAk6MFKbY=}"
  export PORTAL_CRYPTO_KEY="${PORTAL_CRYPTO_KEY:-cMlQRds5K9r42D8FYoxCQhM/vdyxStTh1Ew+OsG9Gjs=}"
  echo; c "1;36" "================ AMBIENTE (H2, sem Docker) ================"
  echo "  App .............. http://localhost:8080"
  echo "  Console do H2 .... http://localhost:8080/h2-console"
  echo "  Contas: admin@portal.com/admin123 · joao@acme.com/senha123"
  echo "  (a app roda em primeiro plano; Ctrl+C encerra)"
  c "1;36" "=========================================================="; echo
  ./mvnw spring-boot:run
elif [ "$DEV" = "1" ]; then
  echo "Modo DEV: só o banco no Docker; app via ./mvnw..."
  $DCOMPOSE -f "$COMPOSE" up -d db
  [ "$SONAR" = "1" ] && { tune_sysctl_for_sonar; $DCOMPOSE -f "$COMPOSE" --profile sonar up -d sonarqube; }
  print_access; echo "  (a app roda em primeiro plano; Ctrl+C encerra só a app)"; echo
  cd "$APP"
  export PORTAL_JWT_SECRET="${PORTAL_JWT_SECRET:-AyjpJmIAw5EyzdygA5Ydd2vm8pp5Sqed7wDAk6MFKbY=}"
  export PORTAL_CRYPTO_KEY="${PORTAL_CRYPTO_KEY:-cMlQRds5K9r42D8FYoxCQhM/vdyxStTh1Ew+OsG9Gjs=}"
  # app roda no HOST: o banco está publicado em localhost:5433 (não no host de rede "db")
  export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5433/portal"
  export SPRING_DATASOURCE_USERNAME="portal"
  export SPRING_DATASOURCE_PASSWORD="portal"
  SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run
else
  echo "Subindo app + banco via Docker (pode demorar no 1º build)..."
  if [ "$SONAR" = "1" ]; then tune_sysctl_for_sonar; $DCOMPOSE -f "$COMPOSE" --profile sonar up -d --build; else $DCOMPOSE -f "$COMPOSE" up -d --build; fi
  echo "Aguardando a app responder em :8080..."
  for i in $(seq 1 60); do
    curl -fs -o /dev/null http://localhost:8080/login 2>/dev/null && { ok "App no ar."; break; }
    sleep 3
    [ "$i" = "60" ] && warn "Demorou — veja: $DCOMPOSE -f infra/docker-compose.yml logs -f app"
  done
  print_access
fi
