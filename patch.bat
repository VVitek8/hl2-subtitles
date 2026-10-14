@echo off
cd /d "%~dp0"
node patch.js patch.txt .
pause