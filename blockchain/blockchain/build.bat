@echo off
echo ========================================
echo BLOCKCHAIN PROJECT - QUICK BUILD SCRIPT
echo ========================================
echo.

echo Step 1: Checking for OpenSSL installation...
if exist "C:\Program Files\OpenSSL-Win64\include\openssl\sha.h" (
    echo [OK] OpenSSL found at C:\Program Files\OpenSSL-Win64\
) else (
    echo [WARNING] OpenSSL not found at default location.
    echo Please install OpenSSL from: https://slproweb.com/products/Win32OpenSSL.html
    echo.
)

echo.
echo Step 2: Checking for httplib.h...
if exist "httplib.h" (
    echo [OK] httplib.h found
) else (
    echo [WARNING] httplib.h not found (this is okay for basic testing)
    echo Download from: https://raw.githubusercontent.com/yhirose/cpp-httplib/master/httplib.h
    echo.
)

echo.
echo Step 3: Building project...
echo Please build the project using Visual Studio (F5 or Ctrl+Shift+B)
echo.
echo Configuration:
echo - Make sure to build for x64 platform
echo - Use Debug or Release configuration
echo.
echo If you get compilation errors:
echo 1. Open project properties (Right-click project -^> Properties)
echo 2. Verify OpenSSL paths under C/C++ -^> General -^> Additional Include Directories
echo 3. Verify OpenSSL paths under Linker -^> General -^> Additional Library Directories
echo.
pause

