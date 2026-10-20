@echo off
setlocal

@REM Normalize the directory to avoid a trailing backslash escaping a quoted JVM argument.
for %%I in ("%~dp0.") do set "MAVEN_PROJECTBASEDIR=%%~fI"
pushd "%MAVEN_PROJECTBASEDIR%"
if errorlevel 1 exit /b 1
set "WRAPPER_JAR=.mvn\wrapper\maven-wrapper.jar"

if not exist "%WRAPPER_JAR%" (
    echo Missing Maven Wrapper JAR: "%WRAPPER_JAR%" 1>&2
    popd
    exit /b 1
)

set "JAVA_EXE=java"
if defined JAVA_HOME set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

"%JAVA_EXE%" %MAVEN_OPTS% -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=." org.apache.maven.wrapper.MavenWrapperMain %*
set "ROOMLY_MAVEN_EXIT_CODE=%ERRORLEVEL%"
popd
exit /b %ROOMLY_MAVEN_EXIT_CODE%
