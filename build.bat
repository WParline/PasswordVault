@echo off
setlocal enabledelayedexpansion

set GRADLE_VERSION=8.10
set GRADLE_DIR=%USERPROFILE%\.gradle\wrapper\dists\gradle-%GRADLE_VERSION%-bin
set GRADLE_HOME=

REM Find Gradle installation
for /d %%d in ("%GRADLE_DIR%\*") do (
    if exist "%%d\bin\gradle.bat" set "GRADLE_HOME=%%d"
)

if defined GRADLE_HOME goto :RUN

echo Downloading Gradle %GRADLE_VERSION%...
echo This may take a few minutes...

REM Download Gradle distribution
set GRADLE_URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip
set TEMP_ZIP=%TEMP%\gradle-%GRADLE_VERSION%-bin.zip

REM Use PowerShell to download
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%GRADLE_URL%' -OutFile '%TEMP_ZIP%'}"

if not exist "%TEMP_ZIP%" (
    echo Download failed. Please check your internet connection.
    exit /b 1
)

REM Extract Gradle
set TARGET_DIR=%USERPROFILE%\.gradle\wrapper\dists
powershell -Command "& {Add-Type -AssemblyName System.IO.Compression.FileSystem; [System.IO.Compression.ZipFile]::ExtractToDirectory('%TEMP_ZIP%', '%TARGET_DIR%')}"

REM Find extracted directory
for /d %%d in ("%GRADLE_DIR%\*") do (
    if exist "%%d\bin\gradle.bat" set "GRADLE_HOME=%%d"
)

if not defined GRADLE_HOME (
    echo Extraction failed.
    exit /b 1
)

:RUN
"%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
