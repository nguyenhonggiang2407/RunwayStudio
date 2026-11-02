@echo off
cd /d "%~dp0"
if not exist target\runway-store-2.0.0.jar (
  call mvnw.cmd clean verify
  if errorlevel 1 (
    pause
    exit /b 1
  )
)
java -jar target\runway-store-2.0.0.jar
pause
