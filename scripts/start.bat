@echo off
REM ===================================================================
REM  Curso CASE Java - subir o ambiente (um clique no Windows).
REM  Da duplo-clique para subir app + banco (Docker) em :8080.
REM  Tambem aceita flags: start.bat -Lab 1  /  -Sonar  /  -Dev  /  -Stop
REM  (NAO precisa de administrador.)
REM ===================================================================
pushd "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1" %*
set "RC=%errorlevel%"
popd

echo.
echo (encerrado com codigo %RC%)
pause
