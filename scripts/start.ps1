<#
=====================================================================
 Curso CASE Java - script unico de ambiente (Windows / PowerShell).
 Sobe TODA a infra do laboratorio com um comando.

   .\scripts\start.ps1                  sobe app + banco (Docker) -> http://localhost:8080
   .\scripts\start.ps1 -Lab 3           checkout de aula-3-baseline e sobe
   .\scripts\start.ps1 -Lab 3 -Hard     usa a solucao (aula-3-hardened)
   .\scripts\start.ps1 -Sonar           sobe tambem o SonarQube (Aula 5) em :9000
   .\scripts\start.ps1 -Dev             sobe so o banco; roda a app via .\mvnw.cmd
   .\scripts\start.ps1 -NoDocker        SEM Docker: roda a app em H2 (memoria)
   .\scripts\start.ps1 -Check           so verifica os pre-requisitos
   .\scripts\start.ps1 -Stop            derruba tudo

 Se aparecer erro de execucao de scripts, rode uma vez:
   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
=====================================================================
#>
param(
  [string]$Lab = "",
  [switch]$Sonar,
  [switch]$Dev,
  [switch]$NoDocker,   # roda a app em H2 (memoria), sem Docker/containers
  [switch]$Check,
  [switch]$Stop,
  [switch]$Hard
)
$ErrorActionPreference = "Stop"
# PowerShell 7+: stderr e exit codes de comandos nativos (ex.: `java -version`,
# `docker info`) NAO devem abortar o script. Sem isto, o "Stop" acima mata tudo.
$PSNativeCommandUseErrorActionPreference = $false

$ROOT    = Split-Path -Parent $PSScriptRoot
$APP     = Join-Path $ROOT "portal-pedidos"
$COMPOSE = Join-Path $ROOT "infra\docker-compose.yml"

function Ok($m){ Write-Host "  [ok] $m" -ForegroundColor Green }
function Warn($m){ Write-Host "  [!] $m" -ForegroundColor Yellow }
function Err($m){ Write-Host "  [x] $m" -ForegroundColor Red }

function Get-Compose {
  try { docker compose version *> $null; return "compose" } catch {}
  if (Get-Command docker-compose -ErrorAction SilentlyContinue) { return "legacy" }
  return $null
}
function Compose { param([Parameter(ValueFromRemainingArguments=$true)]$a)
  if ($script:DC -eq "compose") { & docker compose @a } else { & docker-compose @a }
}

function Check-Prereqs {
  Write-Host "Verificando pre-requisitos..."
  $fail = $false
  $usaMvn = ($NoDocker -or $Dev)   # esses modos rodam a app via mvnw -> exigem Java
  if (Get-Command java -ErrorAction SilentlyContinue) { Ok ("Java: " + ((java -version 2>&1)[0])) }
  elseif ($usaMvn) { Err "Java 17+ nao encontrado (necessario neste modo)"; $fail = $true }
  else { Warn "Java 17+ nao encontrado (ok: o modo Docker nao precisa)" }
  if (Get-Command git -ErrorAction SilentlyContinue) { Ok ("Git: " + (git --version)) } else { Err "Git nao encontrado"; $fail = $true }
  if ($NoDocker) {
    Ok "Modo sem Docker (H2) - Docker nao e necessario."
  } else {
    if (Get-Command docker -ErrorAction SilentlyContinue) {
      try { docker info *> $null; Ok ("Docker: " + (docker --version)) } catch { Err "Docker instalado, mas o daemon nao esta rodando (abra o Docker Desktop). Sem Docker? use -NoDocker"; $fail = $true }
    } else { Err "Docker nao encontrado. Para rodar sem Docker (em H2): .\scripts\start.ps1 -NoDocker"; $fail = $true }
    $script:DC = Get-Compose
    if ($script:DC) { Ok "Compose disponivel" } else { Err "Docker Compose nao encontrado"; $fail = $true }
  }
  return (-not $fail)
}

function Print-Access {
  Write-Host ""
  Write-Host "================ AMBIENTE PRONTO ================" -ForegroundColor Cyan
  Write-Host "  App .............. http://localhost:8080"
  if ($Sonar) { Write-Host "  SonarQube ........ http://localhost:9000  (admin/admin)" }
  Write-Host "  Banco (Postgres) . localhost:5433  (portal/portal)"
  Write-Host ""
  Write-Host "  Contas de teste:"
  Write-Host "    admin@portal.com / admin123"
  Write-Host "    joao@acme.com    / senha123"
  Write-Host "    maria@globex.com / senha123"
  Write-Host ""
  Write-Host "  Parar tudo:  .\scripts\start.ps1 -Stop"
  Write-Host "================================================" -ForegroundColor Cyan
}

Set-Location $ROOT

if ($Stop) {
  $script:DC = Get-Compose
  Write-Host "Derrubando a infra..."
  Compose -f $COMPOSE --profile sonar down
  Ok "Infra encerrada."; exit 0
}

if (-not (Check-Prereqs)) { Write-Host ""; Err "Pre-requisitos faltando. Veja materiais\MANUAL-AMBIENTE-ALUNO.md"; exit 1 }
if ($Check) { Write-Host ""; Ok "Ambiente ok."; exit 0 }

if ($Lab -ne "") {
  $tag = "aula-$Lab-baseline"; if ($Hard) { $tag = "aula-$Lab-hardened" }
  Write-Host "Selecionando o codebase do lab: $tag"
  git rev-parse -q --verify "refs/tags/$tag" *> $null
  if ($LASTEXITCODE -ne 0) { Err "Tag $tag nao existe:"; git tag; exit 1 }
  git diff --quiet; $d1 = $LASTEXITCODE; git diff --cached --quiet; $d2 = $LASTEXITCODE
  if ($d1 -ne 0 -or $d2 -ne 0) { Warn "Alteracoes nao commitadas - salvando em stash automatico..."; git stash push -u -m "auto antes de $tag" | Out-Null }
  git checkout -q $tag; Ok "Codebase em $tag"
}

if ($NoDocker) {
  Write-Host "Modo SEM DOCKER: app em H2 (memoria), sem banco/containers..."
  Set-Location $APP
  # defaults inofensivos (o baseline nem usa; versoes hardened leem do ambiente)
  if (-not $env:PORTAL_JWT_SECRET) { $env:PORTAL_JWT_SECRET = "AyjpJmIAw5EyzdygA5Ydd2vm8pp5Sqed7wDAk6MFKbY=" }
  if (-not $env:PORTAL_CRYPTO_KEY) { $env:PORTAL_CRYPTO_KEY = "cMlQRds5K9r42D8FYoxCQhM/vdyxStTh1Ew+OsG9Gjs=" }
  Write-Host ""
  Write-Host "================ AMBIENTE (H2, sem Docker) ================" -ForegroundColor Cyan
  Write-Host "  App .............. http://localhost:8080"
  Write-Host "  Console do H2 .... http://localhost:8080/h2-console"
  Write-Host "  Contas: admin@portal.com/admin123 . joao@acme.com/senha123"
  Write-Host "  (a app roda em primeiro plano; Ctrl+C encerra)"
  Write-Host "==========================================================" -ForegroundColor Cyan
  Write-Host ""
  & .\mvnw.cmd spring-boot:run
} elseif ($Dev) {
  Write-Host "Modo DEV: so o banco no Docker; app via .\mvnw.cmd..."
  Compose -f $COMPOSE up -d db
  if ($Sonar) { Compose -f $COMPOSE --profile sonar up -d sonarqube }
  Print-Access
  Write-Host "  (a app roda em primeiro plano; Ctrl+C encerra so a app)"; Write-Host ""
  Set-Location $APP
  if (-not $env:PORTAL_JWT_SECRET) { $env:PORTAL_JWT_SECRET = "AyjpJmIAw5EyzdygA5Ydd2vm8pp5Sqed7wDAk6MFKbY=" }
  if (-not $env:PORTAL_CRYPTO_KEY) { $env:PORTAL_CRYPTO_KEY = "cMlQRds5K9r42D8FYoxCQhM/vdyxStTh1Ew+OsG9Gjs=" }
  # app roda no HOST: o banco esta publicado em localhost:5433 (nao no host de rede "db")
  $env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5433/portal"
  $env:SPRING_DATASOURCE_USERNAME = "portal"
  $env:SPRING_DATASOURCE_PASSWORD = "portal"
  $env:SPRING_PROFILES_ACTIVE = "postgres"
  & .\mvnw.cmd spring-boot:run
} else {
  Write-Host "Subindo app + banco via Docker (pode demorar no 1o build)..."
  if ($Sonar) { Compose -f $COMPOSE --profile sonar up -d --build } else { Compose -f $COMPOSE up -d --build }
  Write-Host "Aguardando a app responder em :8080..."
  for ($i=0; $i -lt 60; $i++) {
    try { Invoke-WebRequest -UseBasicParsing http://localhost:8080/login -TimeoutSec 3 *> $null; Ok "App no ar."; break } catch { Start-Sleep 3 }
  }
  Print-Access
}
