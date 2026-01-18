@echo off
echo ========================================
echo BLOCKCHAIN MPI - BUILD SCRIPT
echo ========================================
echo.

echo Checking for MS-MPI...
if exist "C:\Program Files\Microsoft MPI\Inc\mpi.h" (
    echo [OK] MS-MPI found
) else if exist "C:\Program Files (x86)\Microsoft SDKs\MPI\Include\mpi.h" (
    echo [OK] MS-MPI SDK found
) else (
    echo [ERROR] MS-MPI not found!
    echo Please install MS-MPI from:
    echo https://docs.microsoft.com/en-us/message-passing-interface/microsoft-mpi
    pause
    exit /b 1
)

echo Checking for OpenSSL...
if exist "C:\Program Files\OpenSSL-Win64\include\openssl\sha.h" (
    echo [OK] OpenSSL found
) else (
    echo [ERROR] OpenSSL not found at C:\Program Files\OpenSSL-Win64\
    pause
    exit /b 1
)

echo.
echo Building blockchain_mpi.exe...
echo.

REM Check if we're using x64 compiler
if "%VSCMD_ARG_TGT_ARCH%"=="" (
    echo [WARNING] Architecture not detected. Attempting to configure x64...
    if exist "%VCINSTALLDIR%\Auxiliary\Build\vcvarsall.bat" (
        call "%VCINSTALLDIR%\Auxiliary\Build\vcvarsall.bat" x64
    ) else if exist "%ProgramFiles%\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvarsall.bat" (
        call "%ProgramFiles%\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvarsall.bat" x64
    ) else (
        echo [ERROR] Cannot find vcvarsall.bat
        echo.
        echo SOLUTION: Please use "x64 Native Tools Command Prompt for VS 2022"
        echo 1. Press Win key
        echo 2. Search for: x64 Native Tools
        echo 3. Select "x64 Native Tools Command Prompt for VS 2022"
        echo 4. Navigate to this directory and run build_mpi.bat again
        echo.
        pause
        exit /b 1
    )
) else if not "%VSCMD_ARG_TGT_ARCH%"=="x64" (
    echo [ERROR] Current architecture is %VSCMD_ARG_TGT_ARCH%, but x64 is required.
    echo.
    echo SOLUTION: Please use "x64 Native Tools Command Prompt for VS 2022"
    echo 1. Close this window
    echo 2. Open "x64 Native Tools Command Prompt for VS 2022"
    echo 3. Navigate to this directory and run build_mpi.bat again
    echo.
    pause
    exit /b 1
) else (
    echo [OK] Using x64 compiler
)

REM Set MPI paths
set MPI_INC=C:\Program Files (x86)\Microsoft SDKs\MPI\Include
set MPI_LIB=C:\Program Files (x86)\Microsoft SDKs\MPI\Lib\x64

REM Check if alternative MPI location exists
if exist "C:\Program Files\Microsoft MPI\Inc" (
    set MPI_INC=C:\Program Files\Microsoft MPI\Inc
    set MPI_LIB=C:\Program Files\Microsoft MPI\Lib\x64
)

REM Set OpenSSL library path (VC subdirectory for Visual C++ libraries)
set OPENSSL_LIB_PATH=C:\Program Files\OpenSSL-Win64\lib\VC\x64\MD

REM Check if OpenSSL VC libraries exist
if not exist "%OPENSSL_LIB_PATH%\libcrypto.lib" (
    echo [ERROR] OpenSSL libraries not found at %OPENSSL_LIB_PATH%
    echo Please verify OpenSSL installation.
    pause
    exit /b 1
)

echo [INFO] Using OpenSSL libraries from: %OPENSSL_LIB_PATH%

REM Build for x64 platform with dynamic runtime (/MD)
cl /EHsc /O2 /std:c++17 /MD ^
   /I"C:\Program Files\OpenSSL-Win64\include" ^
   /I"%MPI_INC%" ^
   main_mpi.cpp Block.cpp Blockchain.cpp Miner.cpp ^
   /link /MACHINE:X64 ^
   /LIBPATH:"%OPENSSL_LIB_PATH%" ^
   /LIBPATH:"%MPI_LIB%" ^
   libcrypto.lib libssl.lib ws2_32.lib crypt32.lib msmpi.lib ^
   /OUT:blockchain_mpi.exe

if errorlevel 1 (
    goto :build_failed
) else (
    goto :build_success
)

:build_success
    echo.
    echo ========================================
    echo BUILD SUCCESSFUL!
    echo ========================================
    echo.
    echo Executable created: blockchain_mpi.exe
    echo.
    echo Usage:
    echo   Single node:
    echo     blockchain_mpi.exe
    echo.
    echo   Multiple nodes (4 processes):
    echo     mpiexec -n 4 blockchain_mpi.exe
    echo.
    echo   Multiple machines:
    echo     mpiexec -hosts 2 node1 4 node2 4 blockchain_mpi.exe
    echo.
    goto :end

:build_failed
    echo.
    echo ========================================
    echo BUILD FAILED!
    echo ========================================
    echo.
    echo Troubleshooting:
    echo 1. Run from Visual Studio Developer Command Prompt
    echo 2. Verify MS-MPI is installed
    echo 3. Verify OpenSSL is installed
    echo 4. Check paths in this script match your installations
    echo.

:end

pause
