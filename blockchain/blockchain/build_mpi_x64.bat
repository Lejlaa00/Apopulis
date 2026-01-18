@echo off
REM This script sets up x64 environment and then calls build_mpi.bat

echo ========================================
echo Setting up x64 Native Tools environment...
echo ========================================
echo.

REM Try to find and call vcvarsall.bat for x64
if exist "%VCINSTALLDIR%\Auxiliary\Build\vcvarsall.bat" (
    call "%VCINSTALLDIR%\Auxiliary\Build\vcvarsall.bat" x64
) else if exist "%ProgramFiles%\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvarsall.bat" (
    call "%ProgramFiles%\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvarsall.bat" x64
) else if exist "%ProgramFiles(x86)%\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvarsall.bat" (
    call "%ProgramFiles(x86)%\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvarsall.bat" x64
) else (
    echo [ERROR] Could not find vcvarsall.bat
    echo.
    echo Please use "x64 Native Tools Command Prompt for VS 2022" instead.
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo Now building with x64 compiler...
echo ========================================
echo.

REM Now call the actual build script
call build_mpi.bat
