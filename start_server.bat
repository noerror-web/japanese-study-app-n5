@echo off
echo ===================================================
echo   Japanese Study App - Asset Server ^& USB Routing
echo ===================================================
echo.
echo 1. Setting up USB port forwarding (ADB reverse)...
adb reverse tcp:8000 tcp:8000
echo Routing configured: http://127.0.0.1:8000/ inside app -> local PC port 8000
echo.
echo 2. Starting Java HTTP Asset Server on port 8000...
"C:\Users\Administrator\.gradle\jdks\eclipse_adoptium-17-amd64-windows.2\bin\java.exe" "C:\Users\Administrator\Music\online_assets\SimpleHttpServer.java"
pause
