# 华为云 Windows Server 部署指南

## 📋 前置准备

### 1. 华为云 Windows Server 配置要求
- **操作系统**: Windows Server 2019 或更高版本
- **CPU**: 2核或以上
- **内存**: 4GB 或以上
- **磁盘**: 40GB 或以上
- **Java**: OpenJDK 21 或 Oracle JDK 21
- **安全组**: 开放以下端口
  - 8080 (后端 API 端口)
  - 3000 (前端静态服务端口，可选)
  - 22 或 3389 (SSH/RDP)

### 2. Windows Server 初始化配置

#### 2.1 安装 OpenSSH Server（重要！）

**通过 PowerShell 安装：**
```powershell
# 以管理员身份运行 PowerShell

# 安装 OpenSSH Server
Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0

# 启动 SSH 服务
Start-Service sshd

# 设置 SSH 服务开机自启
Set-Service -Name sshd -StartupType 'Automatic'

# 确认防火墙规则（应该已自动创建）
if (!(Get-NetFirewallRule -Name "OpenSSH-Server-In-TCP" -ErrorAction SilentlyContinue | Select-Object Name, Enabled)) {
    Write-Output "防火墙规则 'OpenSSH-Server-In-TCP' 不存在，正在创建..."
    New-NetFirewallRule -Name 'OpenSSH-Server-In-TCP' -DisplayName 'OpenSSH Server (sshd)' -Enabled True -Direction Inbound -Protocol TCP -Action Allow -LocalPort 22
} else {
    Write-Output "防火墙规则 'OpenSSH-Server-In-TCP' 已存在."
}

# 将 PowerShell 设置为默认 Shell（可选，推荐）
New-ItemProperty -Path "HKLM:\SOFTWARE\OpenSSH" -Name DefaultShell -Value "C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe" -PropertyType String -Force
```

#### 2.2 安装 Java 21

**下载并安装：**
1. 访问 [Adoptium](https://adoptium.net/) 或 [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)
2. 下载 Windows x64 版本的 JDK 21
3. 运行安装程序，选择"添加到 PATH"

**验证安装：**
```powershell
java -version
```

#### 2.3 创建部署目录

```powershell
# 创建项目目录
New-Item -Path "C:\se2025\backend" -ItemType Directory -Force
New-Item -Path "C:\se2025\frontend" -ItemType Directory -Force
New-Item -Path "C:\temp" -ItemType Directory -Force
```

#### 2.4 配置 Windows 防火墙

```powershell
# 开放 8080 端口（后端 API）
New-NetFirewallRule -DisplayName "Spring Boot Backend" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow

# 如果需要单独运行前端服务，开放 3000 端口
New-NetFirewallRule -DisplayName "Frontend Static Server" -Direction Inbound -LocalPort 3000 -Protocol TCP -Action Allow
```

## 🔐 GitHub Secrets 配置

在 GitHub 仓库的 `Settings` → `Secrets and variables` → `Actions` 中添加：

| Secret 名称 | 说明 | 示例 |
|------------|------|------|
| `HUAWEI_SERVER_IP` | 华为云服务器公网IP | `123.456.789.012` |
| `SSH_USERNAME` | SSH 登录用户名 | `Administrator` 或其他用户 |
| `SSH_PRIVATE_KEY` | SSH 私钥 | 完整的私钥内容 |

### 如何生成和配置 SSH 密钥（Windows）

**在本地 Windows 机器或 Linux 机器上生成密钥对：**
```powershell
# Windows PowerShell
ssh-keygen -t rsa -b 4096 -C "your_email@example.com" -f $env:USERPROFILE\.ssh\huawei_deploy
```

**在服务器上配置公钥：**
```powershell
# 在华为云 Windows Server 上执行
# 创建 .ssh 目录
$sshPath = "$env:USERPROFILE\.ssh"
New-Item -Path $sshPath -ItemType Directory -Force

# 将公钥内容添加到 authorized_keys
# 方法1: 手动复制公钥内容到这个文件
notepad "$sshPath\authorized_keys"

# 方法2: 从本地机器直接上传
# 在本地执行:
# scp $env:USERPROFILE\.ssh\huawei_deploy.pub Administrator@服务器IP:C:\Users\Administrator\.ssh\authorized_keys
```

**设置正确的权限（重要！）：**
```powershell
# 在服务器上执行
icacls "$env:USERPROFILE\.ssh\authorized_keys" /inheritance:r
icacls "$env:USERPROFILE\.ssh\authorized_keys" /grant:r "$env:USERNAME:(R)"
```

**获取私钥内容添加到 GitHub Secrets：**
```powershell
# 在本地机器执行
Get-Content $env:USERPROFILE\.ssh\huawei_deploy
# 复制完整输出到 GitHub Secrets 的 SSH_PRIVATE_KEY
```

## 🚀 部署流程

### 自动部署
当代码推送到 `main` 分支时，GitHub Actions 会自动：
1. 构建和测试前端（生成 dist 目录）
2. 构建和测试后端（生成 JAR 包）
3. 将构建产物部署到华为云 Windows Server
4. 停止旧版本应用
5. 启动新版本应用

### 部署后的目录结构
```
C:\se2025\
├── backend\
│   ├── project-0.0.1-SNAPSHOT.jar  # Spring Boot JAR 包
│   ├── app.log                      # 应用标准输出日志
│   └── app-error.log                # 应用错误日志
└── frontend\
    ├── index.html                   # 前端入口文件
    └── assets\                      # 前端资源文件
```

## 🌐 前端静态文件服务

### 方案一：使用 Spring Boot 提供静态文件服务（推荐）

**1. 修改部署脚本，将前端文件集成到后端：**

在 `.github/workflows/deploy.yml` 中的 PowerShell 脚本添加：
```powershell
# 将前端文件复制到后端的 static 目录
New-Item -Path 'C:\se2025\backend\static' -ItemType Directory -Force
Copy-Item -Path 'C:\se2025\frontend\*' -Destination 'C:\se2025\backend\static\' -Recurse -Force
```

**2. 在后端 `application.properties` 中添加：**
```properties
# 静态资源配置
spring.web.resources.static-locations=file:///C:/se2025/backend/static/
spring.mvc.static-path-pattern=/**
server.port=8080
```

访问 `http://服务器IP:8080` 即可访问应用。

### 方案二：使用 Node.js serve（需要安装 Node.js）

**安装 Node.js：**
1. 下载 [Node.js Windows 安装包](https://nodejs.org/)
2. 运行安装程序

**安装 serve：**
```powershell
npm install -g serve
```

**创建启动脚本 `C:\se2025\start-frontend.ps1`：**
```powershell
Set-Location C:\se2025\frontend
serve -s . -l 3000
```

**使用 NSSM 将前端服务注册为 Windows 服务：**
```powershell
# 下载 NSSM: https://nssm.cc/download
# 解压后将 nssm.exe 放到 C:\Windows\System32

# 创建服务
nssm install SE2025Frontend "C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe" "-ExecutionPolicy Bypass -File C:\se2025\start-frontend.ps1"

# 启动服务
nssm start SE2025Frontend

# 设置服务描述
nssm set SE2025Frontend Description "SE2025 前端静态服务"
```

访问 `http://服务器IP:3000` 即可访问前端。

## ⚙️ 后端 Windows 服务配置（推荐）

使用 NSSM 将 Spring Boot 应用注册为 Windows 服务，实现开机自启和自动重启。

**1. 下载并安装 NSSM：**
- 下载地址: https://nssm.cc/download
- 解压后将 `nssm.exe` 复制到 `C:\Windows\System32`

**2. 创建启动脚本 `C:\se2025\backend\start-backend.ps1`：**
```powershell
Set-Location C:\se2025\backend
java -jar project-0.0.1-SNAPSHOT.jar
```

**3. 注册 Windows 服务：**
```powershell
# 创建服务
nssm install SE2025Backend java "-jar C:\se2025\backend\project-0.0.1-SNAPSHOT.jar"

# 设置工作目录
nssm set SE2025Backend AppDirectory "C:\se2025\backend"

# 设置日志输出
nssm set SE2025Backend AppStdout "C:\se2025\backend\app.log"
nssm set SE2025Backend AppStderr "C:\se2025\backend\app-error.log"

# 设置服务描述
nssm set SE2025Backend Description "SE2025 后端服务"

# 设置服务在失败时自动重启
nssm set SE2025Backend AppExit Default Restart

# 启动服务
nssm start SE2025Backend
```

**4. 修改 GitHub Actions 部署脚本使用服务：**

将 `.github/workflows/deploy.yml` 中的启动部分改为：
```powershell
# 重启后端服务
Stop-Service -Name SE2025Backend -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 3
Start-Service -Name SE2025Backend

# 等待服务启动
Start-Sleep -Seconds 10

# 检查服务状态
$service = Get-Service -Name SE2025Backend
if ($service.Status -eq 'Running') {
    Write-Host '后端服务启动成功'
} else {
    Write-Host '后端服务启动失败'
    Get-Content 'C:\se2025\backend\app-error.log' -Tail 50
    exit 1
}
```

## 📊 应用管理

### 查看应用状态

**使用 PowerShell：**
```powershell
# 查看后端进程
Get-Process java | Where-Object { $_.CommandLine -like '*project-0.0.1-SNAPSHOT.jar*' }

# 查看服务状态（如果使用了 NSSM）
Get-Service SE2025Backend
Get-Service SE2025Frontend

# 查看应用日志
Get-Content C:\se2025\backend\app.log -Tail 50 -Wait

# 查看错误日志
Get-Content C:\se2025\backend\app-error.log -Tail 50
```

### 手动启动/停止应用

**如果使用 Windows 服务（推荐）：**
```powershell
# 启动服务
Start-Service SE2025Backend

# 停止服务
Stop-Service SE2025Backend

# 重启服务
Restart-Service SE2025Backend
```

**如果使用进程方式：**
```powershell
# 停止后端
Get-Process java | Where-Object { $_.CommandLine -like '*project-0.0.1-SNAPSHOT.jar*' } | Stop-Process -Force

# 启动后端
Set-Location C:\se2025\backend
Start-Process -FilePath java -ArgumentList '-jar', 'project-0.0.1-SNAPSHOT.jar' -RedirectStandardOutput 'app.log' -RedirectStandardError 'app-error.log' -NoNewWindow
```

## 🔧 生产环境配置

### 创建生产环境配置文件

在 `se2025BackEnd\src\main\resources\` 创建 `application-prod.properties`：

```properties
# 服务器配置
server.port=8080
server.address=0.0.0.0

# 数据库配置（使用 Windows 路径）
spring.datasource.url=jdbc:mysql://localhost:3306/se2025?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_password

# JPA 配置
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Redis 配置
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=

# Elasticsearch 配置
spring.elasticsearch.uris=http://localhost:9200

# Kafka 配置
spring.kafka.bootstrap-servers=localhost:9092

# 日志配置（Windows 路径）
logging.level.root=INFO
logging.level.com.SE2025BackEnd_16.project=DEBUG
logging.file.name=C:/se2025/backend/application.log

# 静态资源配置（如果集成前端）
spring.web.resources.static-locations=file:///C:/se2025/backend/static/
spring.mvc.static-path-pattern=/**

# 跨域配置
spring.web.cors.allowed-origins=http://localhost:3000,http://your-server-ip:3000
```

### 修改启动命令使用生产配置

**在服务配置中添加 profile 参数：**
```powershell
nssm set SE2025Backend AppParameters "-jar C:\se2025\backend\project-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod"
```

## 🔒 安全建议

1. **防火墙配置**: 
   - 仅开放必要端口（8080、3000、22/3389）
   - 配置入站规则限制访问来源

2. **SSH 安全**:
   - 使用密钥认证，禁用密码登录
   - 修改默认 SSH 端口（可选）

3. **定期更新**:
   - Windows Update
   - Java 版本
   - 依赖包更新

4. **数据库安全**:
   - 使用强密码
   - 限制远程访问
   - 定期备份

5. **访问控制**:
   - 使用 Windows 防火墙
   - 配置华为云安全组
   - 使用 VPN（如需要）

## 🐛 常见问题

### 1. SSH 连接失败
```powershell
# 检查 SSH 服务状态
Get-Service sshd

# 查看 SSH 日志
Get-EventLog -LogName Application -Source sshd -Newest 20

# 重启 SSH 服务
Restart-Service sshd
```

### 2. 端口被占用
```powershell
# 查看端口占用
netstat -ano | findstr :8080

# 根据 PID 查找进程
Get-Process -Id <PID>

# 杀死进程
Stop-Process -Id <PID> -Force
```

### 3. Java 进程无法停止
```powershell
# 强制结束所有 Java 进程
Get-Process java | Stop-Process -Force
```

### 4. 应用启动失败
```powershell
# 检查日志
Get-Content C:\se2025\backend\app-error.log -Tail 100

# 检查 Java 版本
java -version

# 检查环境变量
$env:JAVA_HOME
$env:PATH
```

### 5. 前端无法访问后端 API
- 检查跨域配置
- 检查防火墙设置
- 检查后端服务是否正常运行
- 检查前端 API 地址配置

## 📝 维护建议

### 1. 日志管理
```powershell
# 创建日志清理脚本 C:\se2025\cleanup-logs.ps1
$logPath = "C:\se2025\backend"
$daysToKeep = 7

Get-ChildItem -Path $logPath -Filter "*.log" | 
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-$daysToKeep) } | 
    Remove-Item -Force

# 使用任务计划程序定期执行
```

### 2. 自动备份
```powershell
# 创建备份脚本 C:\se2025\backup.ps1
$backupPath = "C:\Backups\se2025_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
New-Item -Path $backupPath -ItemType Directory -Force

# 备份应用文件
Copy-Item -Path "C:\se2025" -Destination $backupPath -Recurse -Force

# 压缩备份（需要 7-Zip 或使用 PowerShell 5.0+ 的 Compress-Archive）
Compress-Archive -Path $backupPath -DestinationPath "$backupPath.zip"
Remove-Item -Path $backupPath -Recurse -Force
```

### 3. 性能监控
- 使用 Windows 性能监视器监控 CPU、内存使用
- 定期检查应用日志
- 使用第三方监控工具（如 Prometheus + Grafana）

### 4. Windows 定时任务配置
```powershell
# 创建定时任务：每天凌晨 2 点清理日志
$action = New-ScheduledTaskAction -Execute 'PowerShell.exe' -Argument '-ExecutionPolicy Bypass -File C:\se2025\cleanup-logs.ps1'
$trigger = New-ScheduledTaskTrigger -Daily -At 2am
Register-ScheduledTask -Action $action -Trigger $trigger -TaskName "SE2025LogCleanup" -Description "清理 SE2025 应用日志"
```

## 🔄 回滚策略

```powershell
# 1. 停止当前服务
Stop-Service SE2025Backend -Force

# 2. 备份当前版本
Copy-Item "C:\se2025\backend\project-0.0.1-SNAPSHOT.jar" "C:\se2025\backend\project-0.0.1-SNAPSHOT.jar.current"

# 3. 恢复旧版本
Copy-Item "C:\se2025\backend\project-0.0.1-SNAPSHOT.jar.backup" "C:\se2025\backend\project-0.0.1-SNAPSHOT.jar" -Force

# 4. 重启服务
Start-Service SE2025Backend
```

## 📞 快速参考命令

```powershell
# === 服务管理 ===
Get-Service SE2025Backend                    # 查看服务状态
Start-Service SE2025Backend                  # 启动服务
Stop-Service SE2025Backend                   # 停止服务
Restart-Service SE2025Backend                # 重启服务

# === 进程管理 ===
Get-Process java                              # 查看 Java 进程
Get-Process java | Stop-Process -Force        # 停止所有 Java 进程

# === 日志查看 ===
Get-Content C:\se2025\backend\app.log -Tail 50 -Wait    # 实时查看日志

# === 端口检查 ===
netstat -ano | findstr :8080                  # 查看 8080 端口占用

# === 防火墙管理 ===
Get-NetFirewallRule | Where-Object {$_.LocalPort -eq 8080}  # 查看防火墙规则
```

## 📖 更多资源

- [OpenSSH for Windows 文档](https://docs.microsoft.com/en-us/windows-server/administration/openssh/openssh_overview)
- [NSSM 官方文档](https://nssm.cc/usage)
- [PowerShell 文档](https://docs.microsoft.com/en-us/powershell/)
- [Spring Boot on Windows](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html#deployment.installing)

