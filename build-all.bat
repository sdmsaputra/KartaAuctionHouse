@echo off
REM PlayerAuctions Multi-Version Build Script for Windows

echo 🔨 Building PlayerAuctions - All Versions...
echo =============================================

REM Clean previous builds
echo 🧹 Cleaning previous builds...
call mvn clean

REM Build Modern (1.19-1.21) - v2.0.0
echo.
echo 📦 Building Modern (v2.0.0)...
call mvn package -Pmodern
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Modern build FAILED!
    goto :error
)
echo ✅ Modern build SUCCESS!

REM Build Legacy (1.16-1.18) - v1.9.9
echo.
echo 📦 Building Legacy (v1.9.9)...
call mvn package -Plegacy
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Legacy build FAILED!
    goto :error
)
echo ✅ Legacy build SUCCESS!

REM Build 1.20 - v2.0.0
echo.
echo 📦 Building 1.20 (v2.0.0)...
call mvn package -P1.20
if %ERRORLEVEL% NEQ 0 (
    echo ❌ 1.20 build FAILED!
    goto :error
)
echo ✅ 1.20 build SUCCESS!

REM Build 1.19 - v2.0.0
echo.
echo 📦 Building 1.19 (v2.0.0)...
call mvn package -P1.19
if %ERRORLEVEL% NEQ 0 (
    echo ❌ 1.19 build FAILED!
    goto :error
)
echo ✅ 1.19 build SUCCESS!

echo.
echo 🎉 All builds completed!
echo 📂 Check target/ directory for all jar files:
dir target\PlayerAuctions-*.jar

goto :end

:error
echo.
echo ❌ Build process FAILED!
pause
exit /b 1

:end
echo.
echo 📋 Build Summary:
dir target\PlayerAuctions-*.jar
echo.
echo 📝 Version Scheme:
echo • Modern versions (1.19-1.21): v2.0.0
echo • Legacy versions (1.16-1.18): v1.9.9
echo.
echo ✅ Build process completed successfully!
pause