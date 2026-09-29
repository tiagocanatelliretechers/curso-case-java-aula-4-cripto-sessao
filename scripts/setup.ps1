<#
=====================================================================
 Curso CASE Java - setup de pre-requisitos (Windows).
 Instala TUDO que o aluno precisa, de uma vez, usando o winget
 (gerenciador de pacotes que ja vem no Windows 10/11):

   - Git
   - Java 17 (Eclipse Temurin JDK)  -> com JAVA_HOME e PATH
   - Apache Maven
   - Docker Desktop
   - Eclipse IDE for Enterprise Java and Web Developers

 Uso (em um PowerShell):
   .\scripts\setup.ps1                 instala tudo o que faltar
   .\scripts\setup.ps1 -SkipDocker     nao instala o Docker Desktop
   .\scripts\setup.ps1 -SkipEclipse    nao instala o Eclipse
   .\scripts\setup.ps1 -JavaVersion 21 usa o JDK 21 em vez do 17

 Dica: rode em um PowerShell "como Administrador" para instalar sem
 varios prompts do UAC. Se aparecer erro de politica de execucao:
   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
=====================================================================
#>
param(
  [string]$JavaVersion = "17",
  [switch]$SkipDocker,
  [switch]$SkipEclipse,
  [switch]$SkipMaven
)
$ErrorActionPreference = "Stop"
# PowerShell 7+: stderr / exit codes de comandos nativos (winget, java) NAO devem
# abortar o script. Sem isto, o `winget list` de um pacote ausente mata o setup.
$PSNativeCommandUseErrorActionPreference = $false

function Ok($m){ Write-Host "  [ok] $m" -ForegroundColor Green }
function Info($m){ Write-Host "  [..] $m" -ForegroundColor Cyan }
function Warn($m){ Write-Host "  [!] $m" -ForegroundColor Yellow }
function Err($m){ Write-Host "  [x] $m" -ForegroundColor Red }

# --- winget presente? ---
if (-not (Get-Command winget -ErrorAction SilentlyContinue)) {
  Err "winget nao encontrado."
  Write-Host "  Instale o 'App Installer' pela Microsoft Store (ou atualize o Windows) e rode de novo:"
  Write-Host "  https://apps.microsoft.com/detail/9nblggh4nns1"
  exit 1
}

$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltinRole]::Administrator)
if (-not $isAdmin) { Warn "Rodando sem privilegios de admin - o Windows pode pedir confirmacao (UAC) a cada instalacao." }

# Instala um pacote se ainda nao estiver presente (idempotente).
function Install-Pkg {
  param([string]$Id, [string]$Nome, [string]$Override = "")
  winget list -e --id $Id --accept-source-agreements *> $null
  if ($LASTEXITCODE -eq 0) { Ok "$Nome ja instalado."; return }
  Info "Instalando $Nome ($Id)..."
  $args = @("install","-e","--id",$Id,"--silent","--accept-source-agreements","--accept-package-agreements")
  if ($Override -ne "") { $args += @("--override",$Override) }
  & winget @args
  if ($LASTEXITCODE -eq 0) { Ok "$Nome instalado." }
  else { Warn "$Nome retornou codigo $LASTEXITCODE (pode ja estar instalado ou exigir reinicio)." }
}

# Maven NAO existe no catalogo do winget -> baixa direto da Apache (fonte permanente).
# E opcional: o projeto usa .\mvnw.cmd e o Eclipse traz Maven embutido.
function Install-Maven {
  param([string]$Ver = "3.9.9")
  if (Get-Command mvn -ErrorAction SilentlyContinue) { Ok "Maven ja instalado."; return }
  $dest = "C:\tools\apache-maven-$Ver"
  if (-not (Test-Path "$dest\bin\mvn.cmd")) {
    $zip = Join-Path $env:TEMP "apache-maven-$Ver-bin.zip"
    $url = "https://archive.apache.org/dist/maven/maven-3/$Ver/binaries/apache-maven-$Ver-bin.zip"
    try {
      Info "Baixando Apache Maven $Ver (Apache archive)..."
      Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $zip
      New-Item -ItemType Directory -Force -Path "C:\tools" | Out-Null
      Expand-Archive -Path $zip -DestinationPath "C:\tools" -Force
      Remove-Item $zip -ErrorAction SilentlyContinue
      Ok "Maven extraido em $dest"
    } catch { Warn "Nao consegui baixar o Maven ($($_.Exception.Message)). Opcional - use .\mvnw.cmd."; return }
  } else { Ok "Maven ja em $dest" }
  [Environment]::SetEnvironmentVariable("MAVEN_HOME", $dest, "User")
  $up = [Environment]::GetEnvironmentVariable("Path","User")
  if ($up -notlike "*$dest\bin*") { [Environment]::SetEnvironmentVariable("Path", "$up;$dest\bin", "User") }
  Ok "Maven no PATH do usuario (reabra o terminal para valer)."
}

Write-Host ""
Write-Host "==================== SETUP DE PRE-REQUISITOS ====================" -ForegroundColor Cyan
Write-Host ""

# 1) Git
Install-Pkg -Id "Git.Git" -Nome "Git"

# 2) Java (Temurin JDK) - com JAVA_HOME + PATH via features do MSI
$javaId = "EclipseAdoptium.Temurin.$JavaVersion.JDK"
Install-Pkg -Id $javaId -Nome "Java (Temurin JDK $JavaVersion)" `
  -Override "/passive ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJavaHome"

# 3) Maven (opcional: nao existe no winget; baixa da Apache. O projeto usa .\mvnw.cmd)
if (-not $SkipMaven) { Install-Maven }

# 4) Docker Desktop (habilita o WSL2 antes - pre-requisito)
if (-not $SkipDocker) {
  Info "Habilitando WSL2 (pre-requisito do Docker Desktop)..."
  try { & wsl --install --no-distribution *> $null } catch {}
  try {
    dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart *> $null
    dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart *> $null
    Ok "Recursos de WSL2 habilitados (efetivam apos reiniciar)."
  } catch { Warn "Nao consegui habilitar o WSL2 automaticamente: $($_.Exception.Message)" }

  Install-Pkg -Id "Docker.DockerDesktop" -Nome "Docker Desktop"
  Warn "Docker Desktop: apos REINICIAR o Windows, abra o app uma vez e aguarde 'Engine running'."
  Warn "Em VM VMware o Docker so roda se a VIRTUALIZACAO ANINHADA estiver ligada; senao use start.ps1 -NoDocker."
}

# 5) Eclipse IDE for Enterprise Java and Web Developers
if (-not $SkipEclipse) { Install-Pkg -Id "EclipseFoundation.Eclipse.JEE" -Nome "Eclipse IDE (Enterprise Java)" }

Write-Host ""
Write-Host "======================== TUDO PRONTO ============================" -ForegroundColor Cyan
Write-Host "  FECHE e reabra o PowerShell (ou reinicie) para o PATH/JAVA_HOME valerem."
Write-Host ""
Write-Host "  Verifique com:"
Write-Host "    java -version"
Write-Host "    mvn -version"
Write-Host "    docker --version"
Write-Host "    git --version"
Write-Host ""
Write-Host "  Depois, para subir o laboratorio:"
Write-Host "    .\scripts\start.ps1 -Lab 1"
Write-Host "=================================================================" -ForegroundColor Cyan
