@echo off
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\testar.ps1" %*
exit /b %errorlevel%
