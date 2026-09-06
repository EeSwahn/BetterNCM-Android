@echo off
REM Launcher script for the PowerShell auto build and install tool.
echo Starting PowerShell script...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0auto_build_install.ps1"
pause
