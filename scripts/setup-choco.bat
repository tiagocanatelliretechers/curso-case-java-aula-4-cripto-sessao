@echo off
REM ===================================================================
REM  Curso CASE Java - instalar pre-requisitos via CHOCOLATEY (1 clique).
REM  Da duplo-clique. Pede admin (UAC) e roda scripts\setup-choco.ps1
REM  (Git, Java 17, Maven, Eclipse, Docker). Aceita flags: -SkipDocker etc.
REM ===================================================================
net session >nul 2>&1
if %errorlevel% neq 0 (
  echo Solicitando permissao de administrador...
  if "%~1"=="" (
    powershell -NoProfile -Command "Start-Process -Verb RunAs -FilePath '%~f0'"
  ) else (
    powershell -NoProfile -Command "Start-Process -Verb RunAs -FilePath '%~f0' -ArgumentList '%*'"
  )
  exit /b
)
pushd "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup-choco.ps1" %*
set "RC=%errorlevel%"
popd
echo.
echo ============================================================
echo  Setup (Chocolatey) finalizado (codigo %RC%).
echo  FECHE esta janela e abra um NOVO PowerShell antes de usar.
echo ============================================================
pause
