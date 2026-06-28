@echo off
setlocal EnableExtensions DisableDelayedExpansion

rem Gradle wrapper launcher for Windows.
rem This version intentionally avoids parenthesized IF blocks because paths like
rem "R:\Mi unidad\...\(name)\..." break cmd.exe block parsing.

set "APP_HOME=%~dp0"
cd /d "%APP_HOME%"
if errorlevel 1 exit /b 1

set "JAVA_EXE=java.exe"
if not "%JAVA_HOME%"=="" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

if exist "%JAVA_EXE%" goto java_ok
if not "%JAVA_HOME%"=="" goto java_home_bad
where java.exe >nul 2>nul
if errorlevel 1 goto no_java
set "JAVA_EXE=java.exe"
goto java_ok

:java_home_bad
echo ERROR: JAVA_HOME is set but Java was not found here:
echo %JAVA_EXE%
echo Install Java 21 or fix JAVA_HOME.
exit /b 1

:no_java
echo ERROR: Java was not found.
echo Install Java 21 or set JAVA_HOME to your Java 21 installation.
exit /b 1

:java_ok
set "WRAPPER_JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar"
if not exist "%WRAPPER_JAR%" goto missing_wrapper

set "WRAPPER_TMP=%TEMP%\beanblaster-gradle-wrapper.jar"
copy /Y "%WRAPPER_JAR%" "%WRAPPER_TMP%" >nul
if errorlevel 1 goto copy_failed

"%JAVA_EXE%" -Xmx256m -Xms64m -jar "%WRAPPER_TMP%" %*
exit /b %ERRORLEVEL%

:missing_wrapper
echo ERROR: Missing Gradle wrapper jar.
echo Expected:
echo %WRAPPER_JAR%
exit /b 1

:copy_failed
echo ERROR: Could not copy Gradle wrapper jar to:
echo %WRAPPER_TMP%
exit /b 1
