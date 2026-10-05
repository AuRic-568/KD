@echo off
cd /d "%~dp0"
where javaw >nul 2>nul
if errorlevel 1 (
  echo Java 17+ is required.
  pause
  exit /b 1
)
start "" javaw -jar TaskTracker.jar
