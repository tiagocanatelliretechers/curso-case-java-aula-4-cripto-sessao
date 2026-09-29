@echo off
REM ===================================================================
REM  Curso CASE Java - instalar pre-requisitos (um clique no Windows).
REM  Da duplo-clique neste arquivo. Ele:
REM    1) pede elevacao de administrador (UAC),
REM    2) libera a execucao do script so nesta sessao,
REM    3) roda scripts\setup.ps1 (Git, Java 17, Maven, Docker, Eclipse).
REM  Voce pode passar flags: setup.bat -SkipDocker  /  -JavaVersion 21
REM ===================================================================

REM --- se nao estiver como admin, reabre elevado (mantendo os argumentos) ---
REM  Sem argumentos NAO se passa -ArgumentList (string vazia da erro).
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

REM --- ja elevado: vai para a pasta do .bat e roda o PowerShell ---
pushd "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup.ps1" %*
set "RC=%errorlevel%"
popd

echo.
echo ============================================================
echo  Setup finalizado (codigo %RC%).
echo  FECHE esta janela e abra um NOVO PowerShell antes de usar.
echo ============================================================
pause
