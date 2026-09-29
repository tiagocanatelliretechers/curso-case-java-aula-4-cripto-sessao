#!/usr/bin/env bash
# =====================================================================
# Curso CASE Java — setup de pré-requisitos (macOS / Linux).
# Instala TUDO que o aluno precisa, de uma vez:
#   Git · Java 17 (Temurin) · Maven · Docker · Eclipse (Enterprise Java)
#
#   ./scripts/setup.sh                 instala tudo o que faltar
#   ./scripts/setup.sh --skip-docker   não instala o Docker
#   ./scripts/setup.sh --skip-eclipse  não instala o Eclipse
#   ./scripts/setup.sh --java 21       usa o JDK 21 em vez do 17
#
# macOS  -> usa Homebrew (instala o brew se faltar).
# Linux  -> usa apt (Debian/Ubuntu). Precisa de sudo.
# =====================================================================
set -euo pipefail

JAVAV="17"; SKIP_DOCKER=0; SKIP_ECLIPSE=0; SKIP_MAVEN=0
while [ $# -gt 0 ]; do
  case "$1" in
    --java) JAVAV="${2:-17}"; shift 2;;
    --skip-docker) SKIP_DOCKER=1; shift;;
    --skip-eclipse) SKIP_ECLIPSE=1; shift;;
    --skip-maven) SKIP_MAVEN=1; shift;;
    -h|--help) grep '^#' "$0" | sed 's/^# \{0,1\}//'; exit 0;;
    *) echo "Opção desconhecida: $1 (use --help)"; exit 1;;
  esac
done

ok(){ printf '\033[0;32m  [ok] %s\033[0m\n' "$1"; }
info(){ printf '\033[0;36m  [..] %s\033[0m\n' "$1"; }
warn(){ printf '\033[1;33m  [!] %s\033[0m\n' "$1"; }
have(){ command -v "$1" >/dev/null 2>&1; }

echo; printf '\033[1;36m%s\033[0m\n' "==================== SETUP DE PRÉ-REQUISITOS ===================="; echo

# ------------------------------------------------------------------ macOS
setup_macos(){
  if ! have brew; then
    info "Instalando o Homebrew..."
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    eval "$([ -x /opt/homebrew/bin/brew ] && /opt/homebrew/bin/brew shellenv || /usr/local/bin/brew shellenv)"
  fi
  ok "Homebrew: $(brew --version | head -1)"

  brew_formula(){ if brew list "$1" >/dev/null 2>&1; then ok "$2 já instalado."; else info "Instalando $2..."; brew install "$1"; ok "$2 instalado."; fi; }
  brew_cask(){ if brew list --cask "$1" >/dev/null 2>&1; then ok "$2 já instalado."; else info "Instalando $2..."; brew install --cask "$1"; ok "$2 instalado."; fi; }

  brew_formula git "Git"
  brew_cask "temurin@${JAVAV}" "Java (Temurin JDK ${JAVAV})"
  [ "$SKIP_MAVEN" = "0" ] && brew_formula maven "Apache Maven"
  if [ "$SKIP_DOCKER" = "0" ]; then brew_cask docker "Docker Desktop"; warn "Abra o Docker Desktop uma vez e aguarde 'Engine running'."; fi
  [ "$SKIP_ECLIPSE" = "0" ] && brew_cask eclipse-jee "Eclipse IDE (Enterprise Java)"
}

# ------------------------------------------------------------------ Linux (apt)
setup_linux(){
  if ! have apt-get; then
    warn "Distro sem apt. Instale manualmente: openjdk-${JAVAV}, maven, git, docker, eclipse."
    warn "Veja materiais/MANUAL-AMBIENTE-ALUNO.md"; exit 1
  fi
  info "Atualizando índice de pacotes (sudo)..."; sudo apt-get update -y >/dev/null

  apt_pkg(){ if dpkg -s "$1" >/dev/null 2>&1; then ok "$2 já instalado."; else info "Instalando $2..."; sudo apt-get install -y "$1" >/dev/null; ok "$2 instalado."; fi; }

  apt_pkg git "Git"
  apt_pkg "openjdk-${JAVAV}-jdk" "Java (OpenJDK ${JAVAV})"
  [ "$SKIP_MAVEN" = "0" ] && apt_pkg maven "Apache Maven"

  if [ "$SKIP_DOCKER" = "0" ]; then
    if have docker; then ok "Docker já instalado."; else
      info "Instalando Docker Engine (script oficial get.docker.com)..."
      curl -fsSL https://get.docker.com | sudo sh
      sudo usermod -aG docker "$USER" || true
      warn "Você foi adicionado ao grupo 'docker' — FAÇA LOGOUT/LOGIN (ou 'newgrp docker') para valer."
    fi
  fi

  if [ "$SKIP_ECLIPSE" = "0" ]; then
    if have eclipse; then ok "Eclipse já instalado."
    elif have snap; then info "Instalando Eclipse via snap..."; sudo snap install eclipse --classic; ok "Eclipse instalado."
    else warn "snap indisponível — baixe o Eclipse (Enterprise Java) em https://www.eclipse.org/downloads/packages/"; fi
  fi

  local jhome="/usr/lib/jvm/java-${JAVAV}-openjdk-amd64"
  [ -d "$jhome" ] && warn "Opcional: export JAVA_HOME=$jhome  (adicione ao ~/.bashrc)"
}

case "$(uname -s)" in
  Darwin) setup_macos;;
  Linux)  setup_linux;;
  *) warn "SO não suportado por este script. Veja materiais/MANUAL-AMBIENTE-ALUNO.md"; exit 1;;
esac

echo; printf '\033[1;36m%s\033[0m\n' "======================== TUDO PRONTO ============================"
echo "  Abra um NOVO terminal e verifique:"
echo "    java -version && mvn -version && docker --version && git --version"
echo
echo "  Depois, suba o laboratório:"
echo "    ./scripts/start.sh --lab 1"
printf '\033[1;36m%s\033[0m\n' "================================================================="
