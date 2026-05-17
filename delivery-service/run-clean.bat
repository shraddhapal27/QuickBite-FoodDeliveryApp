@echo off
echo ==============================================
echo REBUILDING AND RESTARTING DELIVERY SERVICE
echo ==============================================

cd /d "%~dp0"
call .\mvnw clean compile spring-boot:run

pause
