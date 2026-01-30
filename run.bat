@echo off
setlocal enabledelayedexpansion

:: Simple cross-platform runner for Windows
:: Usage: run.bat [--gui|--cli]
set MODE=--gui
if "%1"=="--cli" set MODE=
if "%1"=="-c" set MODE=

:: prefer kotlinc if available (SDKMAN/MSYS or Kotlin installer)
where kotlinc >nul 2>&1
if %ERRORLEVEL%==0 (
  echo Found kotlinc. Compiling Kotlin sources...
  if exist app.jar del /f /q app.jar
  :: collect .kt files (simple glob works in cmd)
  set KTFILES=
  for /r %%f in (src\*.kt) do set KTFILES=!KTFILES! "%%f"
  for /r %%f in (GUI\*.kt) do set KTFILES=!KTFILES! "%%f"
  kotlinc -d app.jar -include-runtime %KTFILES%
  echo Running (mode=%MODE%)
  set MAIN=com.corinthians.app.AppCorinthiansKt
  if "%MODE%"=="--gui" (
    java -cp app.jar %MAIN% --gui
  ) else (
    java -cp app.jar %MAIN%
  )
  exit /b %ERRORLEVEL%
)

:: if gradlew.bat present, use it
if exist gradlew.bat (
  echo Using Gradle wrapper gradlew.bat
  if "%MODE%"=="--gui" (
    gradlew.bat run --args="--gui"
  ) else (
    gradlew.bat run --args=""
  )
  exit /b %ERRORLEVEL%
)

:: fallback: if fat jar exists, run it directly with main class
if exist app.jar (
  echo Running existing app.jar
  set MAIN=com.corinthians.app.AppCorinthiansKt
  if "%MODE%"=="--gui" (
    java -cp app.jar %MAIN% --gui
  ) else (
    java -cp app.jar %MAIN%
  )
  exit /b %ERRORLEVEL%
)

:: nothing available; guide the user
necho No Kotlin compiler (kotlinc), Gradle wrapper, or app.jar found.
necho Install Kotlin (kotlinc) or add gradlew to the repo. Alternatively build on Linux and provide the fat jar.
exit /b 2
