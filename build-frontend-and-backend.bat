@echo off
REM 构建 frontend 并把构建产物复制到后端 resources/static
REM 使用方法（在仓库根 cmd 中运行）：
REM   build-frontend-and-backend.bat

IF NOT EXIST frontend (
  echo frontend 目录不存在，跳过
  goto :eof
)

cd frontend
if exist node_modules (
  echo node_modules 已存在，跳过 npm install
) else (
  echo 运行 npm install ...
  npm install || (
    echo npm install 失败，请检查 Node.js / npm 是否已安装。
    goto :eof
  )
)

echo 运行 npm run build ...
npm run build || (
  echo 构建 frontend 失败
  goto :eof
)

cd ..
REM 复制构建产物到后端静态资源目录（覆盖）
if not exist src\main\resources\static mkdir src\main\resources\static
xcopy /E /I /Y frontend\dist\* src\main\resources\static\

echo 已构建并复制前端产物到 src\main\resources\static\

echo 接下来你可以运行 Gradle 打包：
echo   gradlew.bat clean assemble -x test

echo 或直接运行后端（若已经打包）：
echo   java -jar build\libs\demo2.jar

