@echo off
rem deploy-frontend.bat - 将 frontend/dist 的构建产物复制到后端 static 目录（Windows cmd）
rem 使用方法：在仓库根目录运行： scripts\deploy-frontend.bat

nset DIST_DIR=frontend\dist
nset TARGET_DIR=src\main\resources\static
rmecho Copying files from %DIST_DIR% to %TARGET_DIR%...
if not exist "%DIST_DIR%" (
  echo ERROR: %DIST_DIR% 不存在，请先在 frontend 目录运行 npm run build
  exit /b 1
)
if not exist "%TARGET_DIR%" (
  mkdir "%TARGET_DIR%"
)
xcopy "%DIST_DIR%\*" "%TARGET_DIR%\" /E /I /Y
echo Done. You can now rebuild/restart the backend to include the static files.
pause

