@rem MindMate Gradle Launcher
@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.

set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve Java home if not set
if "%JAVA_HOME%"=="" (
    if exist "C:\Users\LOQ\.antigravity\extensions\redhat.java-1.54.0-win32-x64\jre\21.0.10-win32-x86_64" (
        set "JAVA_HOME=C:\Users\LOQ\.antigravity\extensions\redhat.java-1.54.0-win32-x64\jre\21.0.10-win32-x86_64"
    )
)

if "%ANDROID_HOME%"=="" (
    if exist "C:\Users\LOQ\AppData\Local\Android\Sdk" (
        set "ANDROID_HOME=C:\Users\LOQ\AppData\Local\Android\Sdk"
    )
)

@rem Find gradle executable
set GRADLE_BIN=C:\Users\LOQ\.gradle\wrapper\dists\gradle-8.10.2-bin\a04bxjujx95o3nb99gddekhwo\gradle-8.10.2\bin\gradle.bat

if exist "%GRADLE_BIN%" (
    call "%GRADLE_BIN%" %*
) else (
    gradle %*
)

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not "%GRADLE_EXIT_CONSOLE%"=="" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal
