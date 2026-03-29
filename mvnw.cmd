@REM Maven Wrapper (3.2+): main class ile calisir; -jar desteklenmez.
@setlocal
@set "MAVEN_PROJECTBASEDIR=%~dp0"
@if "%MAVEN_PROJECTBASEDIR:~-1%"=="\" set "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"
@set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
@set WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar

@if exist "%WRAPPER_JAR%" goto runMaven

@echo Indiriliyor: maven-wrapper.jar ...
@powershell -NoProfile -Command "Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%' -UseBasicParsing"
@if %ERRORLEVEL% neq 0 goto error

:runMaven
@java %MAVEN_OPTS% ^
  -classpath "%WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  org.apache.maven.wrapper.MavenWrapperMain %*
@if %ERRORLEVEL% neq 0 goto error
@goto end

:error
@echo ERROR: Maven derlemesi basarisiz.
@exit /b 1

:end
@endlocal
