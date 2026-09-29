<#
=====================================================================
 Curso CASE Java - setup de pre-requisitos via CHOCOLATEY (Windows).
 Alternativa ao setup.ps1 (winget). Melhor aqui porque o Chocolatey
 TEM Maven e Eclipse no catalogo (o winget nao tem Maven e o Eclipse
 falha por hash). Instala numa rodada:

   - Git            (git)
   - Java 17        (temurin17)  -> com JAVA_HOME e PATH
   - Apache Maven   (maven)
   - Eclipse IDE    (eclipse)
   - Docker Desktop (docker-desktop)   [use -SkipDocker para pular]

 Uso (PowerShell COMO ADMINISTRADOR):
   .\scripts\setup-choco.ps1
   .\scripts\setup-choco.ps1 -SkipDocker
   .\scripts\setup-choco.ps1 -JavaVersion 21

 Se aparecer erro de politica de execucao:
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
# stderr/exit code de comando nativo (choco) nao deve abortar o script (PS7+)
$PSNativeCommandUseErrorActionPreference = $false

function Ok($m){ Write-Host "  [ok] $m" -ForegroundColor Green }
function Info($m){ Write-Host "  [..] $m" -ForegroundColor Cyan }
function Warn($m){ Write-Host "  [!] $m" -ForegroundColor Yellow }
function Err($m){ Write-Host "  [x] $m" -ForegroundColor Red }

# --- precisa ser admin (choco instala em Program Files) ---
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltinRole]::Administrator)
if (-not $isAdmin) { Err "Rode este script em um PowerShell COMO ADMINISTRADOR."; exit 1 }

# --- instala o Chocolatey se faltar ---
if (-not (Get-Command choco -ErrorAction SilentlyContinue)) {
  Info "Instalando o Chocolatey..."
  Set-ExecutionPolicy Bypass -Scope Process -Force
  [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
  Invoke-Expression ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
  $env:Path = [Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [Environment]::GetEnvironmentVariable("Path","User")
}
if (-not (Get-Command choco -ErrorAction SilentlyContinue)) { Err "Chocolatey nao ficou disponivel. Feche e reabra o PowerShell (admin) e rode de novo."; exit 1 }
Ok ("Chocolatey: " + (choco --version))

function Choco-Install {
  param([string]$Pkg, [string]$Nome, [string]$Params = "")
  Info "Instalando $Nome ($Pkg)..."
  $a = @("install",$Pkg,"-y","--no-progress","--limit-output")
  if ($Params -ne "") { $a += @("--params",$Params) }
  & choco @a
  # 0 = ok ; 1641/3010 = ok mas exige reiniciar
  if ($LASTEXITCODE -eq 0 -or $LASTEXITCODE -eq 3010 -or $LASTEXITCODE -eq 1641) { Ok "$Nome pronto." }
  else { Warn "$Nome retornou codigo $LASTEXITCODE (verifique o log acima)." }
}

Write-Host ""
Write-Host "============== SETUP DE PRE-REQUISITOS (Chocolatey) ==============" -ForegroundColor Cyan
Write-Host ""

# 1) Git
Choco-Install -Pkg "git" -Nome "Git"

# 2) Java (Temurin) com JAVA_HOME + PATH
Choco-Install -Pkg "temurin$JavaVersion" -Nome "Java (Temurin JDK $JavaVersion)" `
  -Params "'/ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJavaHome /quiet'"

# 3) Maven
if (-not $SkipMaven) { Choco-Install -Pkg "maven" -Nome "Apache Maven" }

# 4) Eclipse
if (-not $SkipEclipse) { Choco-Install -Pkg "eclipse" -Nome "Eclipse IDE" }

# 5) Docker Desktop (habilita WSL2 antes)
if (-not $SkipDocker) {
  Info "Habilitando WSL2 (pre-requisito do Docker Desktop)..."
  try { & wsl --install --no-distribution *> $null } catch {}
  try {
    dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart *> $null
    dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart *> $null
    Ok "Recursos de WSL2 habilitados (efetivam apos reiniciar)."
  } catch { Warn "Nao consegui habilitar o WSL2: $($_.Exception.Message)" }
  Choco-Install -Pkg "docker-desktop" -Nome "Docker Desktop"
  Warn "Docker: em VM VMware so roda com VIRTUALIZACAO ANINHADA ligada; senao use start.ps1 -NoDocker."
}

Write-Host ""
Write-Host "======================== TUDO PRONTO ============================" -ForegroundColor Cyan
Write-Host "  FECHE e reabra o PowerShell (ou reinicie) para PATH/JAVA_HOME valerem."
Write-Host "  Verifique:  java -version ; mvn -version ; docker --version ; git --version"
Write-Host "  Eclipse: Menu Iniciar -> Eclipse   (instala em C:\Program Files\Eclipse Foundation)"
Write-Host "  Subir o lab:  .\scripts\start.ps1 -Lab 1    (ou -NoDocker)"
Write-Host "=================================================================" -ForegroundColor Cyan
