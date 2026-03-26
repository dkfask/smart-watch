# 设置项目路径
$projectPath = "E:\smart\master"
# 切换到项目目录
Set-Location -Path $projectPath

# 配置Git凭证
git config --global credential.helper store

# 设置输出编码为UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# 拉取最新代码
Write-Host "Pulling latest code..."
git pull origin main

# 检查是否有更改
$changes = git status --porcelain
if ($changes) {
    Write-Host "Found code changes, starting commit..."
    
    # 添加所有更改
    git add .
    
    # 提交代码，使用当前时间作为提交信息
    git commit -m "Auto commit - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    
    # 推送到远程仓库
    git push origin main
    
    Write-Host "Commit completed!"
} else {
    Write-Host "No code changes found, skipping commit."
}