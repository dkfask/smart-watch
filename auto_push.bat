@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo [%date% %time%] 开始检查代码变更...

:: 检查是否有未提交的变更
git status --porcelain > temp_status.txt
findstr /r "." temp_status.txt >nul 2>&1
if %errorlevel% neq 0 (
    echo [%date% %time%] 没有检测到代码变更，退出。
    del temp_status.txt 2>nul
    exit /b 0
)

echo [%date% %time%] 检测到代码变更，开始提交并推送...

:: 添加所有变更
git add -A

:: 提交（使用日期作为消息）
for /f "tokens=1-4 delims=/ " %%a in ('date /t') do set date=%%a%%b%%c
for /f "tokens=1-2 delims=: " %%a in ('time /t') do set time=%%a%%b
git commit -m "auto: %date% %time% 自动提交"

:: 推送到远程
git push origin main
if %errorlevel% equ 0 (
    echo [%date% %time%] 推送成功！
) else (
    echo [%date% %time%] 推送失败，请检查网络或权限。
)

del temp_status.txt 2>nul
