@echo off
setlocal
cd /d "%~dp0"
if exist build\classes rmdir /s /q build\classes
if exist build\fat rmdir /s /q build\fat
mkdir build\classes
mkdir build\fat
javac --release 17 --add-modules jdk.httpserver -cp "dependencies\*" -d build\classes src\*.java || exit /b 1
xcopy /e /i /q build\classes build\fat >nul
xcopy /e /i /q web build\fat\web >nul
for %%f in (dependencies\*.jar) do (
  pushd build\fat
  jar xf "..\..\%%f"
  popd
)
if exist build\fat\META-INF\*.SF del /q build\fat\META-INF\*.SF
if exist build\fat\META-INF\*.RSA del /q build\fat\META-INF\*.RSA
if exist build\fat\META-INF\*.DSA del /q build\fat\META-INF\*.DSA
>build\manifest.mf echo Main-Class: Main
jar cfm TaskTracker.jar build\manifest.mf -C build\fat . || exit /b 1
echo Built TaskTracker.jar
