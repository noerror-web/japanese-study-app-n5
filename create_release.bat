@echo off
title Japanese Study App N5 - Build & Create GitHub Release
set PATH=C:\Program Files\Git\cmd;C:\Program Files\GitHub CLI;%PATH%
set JAVA_HOME=C:\Users\Administrator\.antigravity-ide\extensions\redhat.java-1.56.0-win32-x64\jre\21.0.12.1-win32-x86_64

echo =========================================================
echo  Japanese Study App N5 - Build & GitHub Release Creator
echo =========================================================
echo.
echo Step 1: Building latest Debug & Release APKs...
call gradlew.bat assembleDebug assembleRelease

echo.
echo Step 2: Publishing Release via Python Script...
py publish_v2_release.py

echo.
echo =========================================================
echo  SUCCESS! Build & Release process complete.
echo =========================================================
pause

