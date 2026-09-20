@echo off
rem Lanzador de levantar.ps1 (evita el bloqueo de scripts de PowerShell).
rem Ejecutar desde una terminal (PowerShell o cmd) en la carpeta del proyecto.
rem Uso:  levantar.cmd [-Reset] [-NoBuild] [-Test] [-Down]
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0levantar.ps1" %*
exit /b %ERRORLEVEL%
