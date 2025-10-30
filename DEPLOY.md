# IM 即时通讯系统 - 部署文档

本文档详细说明如何将 IM 项目部署到生产服务器。

## 📋 目录

- [服务器环境要求](#服务器环境要求)
- [快速部署（推荐）](#快速部署推荐)
- [手动部署步骤](#手动部署步骤)
- [配置说明](#配置说明)
- [常见问题](#常见问题)

---

## 🖥️ 服务器环境要求

### 必需环境

- **操作系统**: Linux (Ubuntu 20.04+ / CentOS 7+ 推荐)
- **Java**: JDK 21+
- **MySQL**: 8.0+
- **Redis**: 6.0+
- **Elasticsearch**: 7.x (可选，用于消息搜索)
- **Nginx**: 1.18+
- **Maven**: 3.6+ (本地构建用)
- **Node.js**: 20+ (本地构建用)

### 服务器配置建议

- **CPU**: 2 核心以上
- **内存**: 4GB 以上
- **硬盘**: 20GB 以上
- **网络**: 公网 IP 或域名

---

## 🚀 快速部署（推荐）

### 1. 准备工作

在本地机器上克隆项目：

```bash
git clone <your-repo-url>
cd im
```

### 2. 配置部署参数

编辑 `deploy.sh` 文件，修改以下变量：

```bash
SERVER_IP="your-server-ip"              # 改为你的服务器 IP
SERVER_USER="root"                      # 改为你的 SSH 用户名
DEPLOY_PATH="/opt/im"                   # 部署目录（可选）
```

编辑 `backend/src/main/resources/application-prod.properties`：

```properties
# MySQL 配置
spring.datasource.url=jdbc:mysql://localhost:3306/im?useUnicode=true&characterEncoding=utf8mb4
spring.datasource.username=root
spring.datasource.password=your_mysql_password

# Redis 配置
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=your_redis_password  # 如果有密码

# Elasticsearch 配置
spring.elasticsearch.uris=http://localhost:9200

# AI 配置
ai.apiKey=your_api_key_here
```

编辑 `nginx.conf`，修改域名：

```nginx
server_name your-domain.com;  # 改为你的域名或 IP
```

### 3. 初始化数据库

在服务器上执行：

```bash
mysql -u root -p < init.sql
```

或者手动创建：

```bash
# 登录 MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE im CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入表结构
USE im;
source /path/to/init.sql
```

### 4. 一键部署

在本地执行部署脚本：

```bash
chmod +x deploy.sh
./deploy.sh
```

脚本会自动完成：
- ✅ 构建后端 jar 包
- ✅ 构建前端静态文件
- ✅ 上传到服务器
- ✅ 配置 Nginx 和 systemd 服务
- ✅ 重启后端服务

### 5. 访问系统

打开浏览器访问：

```
http://your-server-ip
```

---

## 🔧 手动部署步骤

如果自动部署脚本不适用，可以按以下步骤手动部署。

### 步骤 1: 构建后端

在本地项目目录执行：

```bash
cd backend
mvn clean package -DskipTests
```

构建产物：`backend/target/im-0.0.1-SNAPSHOT.jar`

### 步骤 2: 构建前端

```bash
cd frontend

# 配置生产环境变量
cat > .env.production << EOF
VITE_API_BASE_URL=http://your-server-ip
VITE_WS_BASE_URL=ws://your-server-ip
EOF

# 安装依赖
npm install

# 构建
npm run build
```

构建产物：`frontend/dist/`

### 步骤 3: 上传到服务器

```bash
# 上传后端
scp backend/target/im-0.0.1-SNAPSHOT.jar root@your-server:/opt/im/backend/
scp backend/src/main/resources/application-prod.properties root@your-server:/opt/im/backend/

# 上传前端
scp -r frontend/dist/* root@your-server:/var/www/im/frontend/

# 上传配置文件
scp nginx.conf root@your-server:/opt/im/
scp im-backend.service root@your-server:/opt/im/
scp init.sql root@your-server:/opt/im/
```

### 步骤 4: 服务器配置

SSH 登录到服务器：

```bash
ssh root@your-server-ip
```

#### 4.1 创建目录

```bash
mkdir -p /opt/im/backend
mkdir -p /var/www/im/frontend
mkdir -p /var/log/im
```

#### 4.2 配置 Nginx

```bash
# 复制配置文件
cp /opt/im/nginx.conf /etc/nginx/conf.d/im.conf

# 测试配置
nginx -t

# 重载 Nginx
systemctl reload nginx
```

#### 4.3 配置 systemd 服务

```bash
# 复制服务文件
cp /opt/im/im-backend.service /etc/systemd/system/

# 重新加载 systemd
systemctl daemon-reload

# 启动服务
systemctl start im-backend

# 设置开机自启
systemctl enable im-backend

# 查看服务状态
systemctl status im-backend
```

### 步骤 5: 验证部署

#### 检查后端服务

```bash
# 查看服务状态
systemctl status im-backend

# 查看日志
journalctl -u im-backend -f

# 测试 API
curl http://localhost:8080/auth/info
```

#### 检查前端

```bash
# 访问前端
curl http://localhost

# 检查 Nginx
nginx -t
systemctl status nginx
```

#### 检查 WebSocket

```bash
# 查看端口
netstat -tulnp | grep 8080
```

---

## ⚙️ 配置说明

### 后端配置

**文件位置**: `/opt/im/backend/application-prod.properties`

```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/im
spring.datasource.username=root
spring.datasource.password=your_password

# Redis 配置
spring.redis.host=localhost
spring.redis.port=6379

# Elasticsearch 配置
spring.elasticsearch.uris=http://localhost:9200

# 日志配置
logging.file.name=/var/log/im/application.log
```

### 前端配置

**文件位置**: `frontend/.env.production` (构建前配置)

```bash
# 后端 API 地址
VITE_API_BASE_URL=http://your-server-ip

# WebSocket 地址
VITE_WS_BASE_URL=ws://your-server-ip
```

**如果使用 Nginx 反向代理**，前端访问后端通过同一域名：

```bash
# 前端和后端在同一域名下，API 使用相对路径
VITE_API_BASE_URL=http://your-domain.com
VITE_WS_BASE_URL=ws://your-domain.com
```

### Nginx 配置

**文件位置**: `/etc/nginx/conf.d/im.conf`

关键配置：

```nginx
# 前端静态文件目录
root /var/www/im/frontend;

# 后端 API 代理
location /auth/ {
    proxy_pass http://localhost:8080/auth/;
    proxy_cookie_path / /;
}

# WebSocket 代理
location /ws {
    proxy_pass http://localhost:8080/ws;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
```

---

## 🔍 常用命令

### 后端服务管理

```bash
# 启动服务
sudo systemctl start im-backend

# 停止服务
sudo systemctl stop im-backend

# 重启服务
sudo systemctl restart im-backend

# 查看状态
sudo systemctl status im-backend

# 查看日志（实时）
sudo journalctl -u im-backend -f

# 查看最近 100 行日志
sudo journalctl -u im-backend -n 100

# 开机自启
sudo systemctl enable im-backend

# 禁用开机自启
sudo systemctl disable im-backend
```

### Nginx 管理

```bash
# 测试配置
sudo nginx -t

# 重载配置
sudo systemctl reload nginx

# 重启 Nginx
sudo systemctl restart nginx

# 查看 Nginx 日志
tail -f /var/log/nginx/im-access.log
tail -f /var/log/nginx/im-error.log
```

### 数据库管理

```bash
# 备份数据库
mysqldump -u root -p im > im_backup_$(date +%Y%m%d).sql

# 恢复数据库
mysql -u root -p im < im_backup.sql

# 查看数据库大小
mysql -u root -p -e "SELECT table_schema AS 'Database', ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)' FROM information_schema.tables WHERE table_schema = 'im';"
```

### 日志查看

```bash
# 后端应用日志
tail -f /var/log/im/application.log

# 系统日志
sudo journalctl -u im-backend -f

# Nginx 访问日志
tail -f /var/log/nginx/im-access.log

# Nginx 错误日志
tail -f /var/log/nginx/im-error.log
```

---

## ❓ 常见问题

### 1. 后端服务无法启动

**问题**: `systemctl status im-backend` 显示失败

**排查步骤**:

```bash
# 查看详细日志
sudo journalctl -u im-backend -n 100

# 常见原因：
# - 端口 8080 被占用
# - MySQL/Redis/Elasticsearch 连接失败
# - application-prod.properties 配置错误
# - Java 版本不匹配

# 检查端口占用
sudo netstat -tulnp | grep 8080

# 手动启动测试
cd /opt/im/backend
java -Dspring.profiles.active=prod -jar im-0.0.1-SNAPSHOT.jar
```

### 2. 前端无法访问后端 API

**问题**: 前端显示网络错误或 CORS 错误

**解决方案**:

1. 检查后端是否启动：`curl http://localhost:8080/auth/info`
2. 检查 Nginx 配置是否正确：`nginx -t`
3. 检查防火墙：`sudo firewall-cmd --list-all` 或 `sudo ufw status`
4. 检查前端环境变量是否正确配置

### 3. WebSocket 连接失败

**问题**: 无法建立 WebSocket 连接

**排查步骤**:

```bash
# 检查后端 WebSocket 端点
curl -i -N -H "Connection: Upgrade" -H "Upgrade: websocket" http://localhost:8080/ws

# 检查 Nginx WebSocket 配置
# 确保有以下配置：
# proxy_http_version 1.1;
# proxy_set_header Upgrade $http_upgrade;
# proxy_set_header Connection "upgrade";
```

### 4. 数据库连接失败

**问题**: 后端日志显示 MySQL 连接错误

**解决方案**:

```bash
# 检查 MySQL 是否运行
sudo systemctl status mysql

# 测试 MySQL 连接
mysql -u root -p -e "SHOW DATABASES;"

# 检查数据库是否存在
mysql -u root -p -e "USE im; SHOW TABLES;"

# 检查用户权限
mysql -u root -p -e "SHOW GRANTS FOR 'root'@'localhost';"
```

### 5. Redis 连接失败

**问题**: 后端日志显示 Redis 连接错误

**解决方案**:

```bash
# 检查 Redis 是否运行
sudo systemctl status redis

# 测试 Redis 连接
redis-cli ping
# 应该返回 PONG

# 如果有密码
redis-cli -a your_password ping
```

### 6. Elasticsearch 连接失败（可选）

**问题**: 消息搜索功能不可用

**解决方案**:

```bash
# 检查 Elasticsearch 是否运行
sudo systemctl status elasticsearch

# 测试 Elasticsearch 连接
curl http://localhost:9200

# 如果不需要搜索功能，可以在后端代码中禁用 Elasticsearch
```

### 7. 端口被占用

**问题**: 启动服务时提示端口已被占用

**解决方案**:

```bash
# 查看占用端口的进程
sudo lsof -i :8080

# 或者
sudo netstat -tulnp | grep 8080

# 杀死进程
sudo kill -9 <PID>

# 或者修改后端端口（application-prod.properties）
server.port=8081
```

### 8. 权限问题

**问题**: 无法创建日志文件或访问文件

**解决方案**:

```bash
# 创建日志目录并设置权限
sudo mkdir -p /var/log/im
sudo chown -R $(whoami):$(whoami) /var/log/im

# 设置前端文件权限
sudo chown -R nginx:nginx /var/www/im/frontend

# 设置后端文件权限
sudo chown -R $(whoami):$(whoami) /opt/im/backend
```

---

## 🔐 安全建议

### 1. 防火墙配置

```bash
# Ubuntu (UFW)
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 22/tcp    # SSH
sudo ufw enable

# CentOS (Firewalld)
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --permanent --add-service=ssh
sudo firewall-cmd --reload
```

### 2. 数据库安全

```bash
# 修改 MySQL root 密码
ALTER USER 'root'@'localhost' IDENTIFIED BY 'strong_password';

# 禁止远程 root 登录
# 编辑 /etc/mysql/mysql.conf.d/mysqld.cnf
bind-address = 127.0.0.1

# 设置 Redis 密码
# 编辑 /etc/redis/redis.conf
requirepass your_redis_password
```

### 3. HTTPS 配置（推荐）

使用 Let's Encrypt 免费证书：

```bash
# 安装 Certbot
sudo apt install certbot python3-certbot-nginx  # Ubuntu
sudo yum install certbot python3-certbot-nginx  # CentOS

# 获取证书
sudo certbot --nginx -d your-domain.com

# 自动续期
sudo certbot renew --dry-run
```

修改 `nginx.conf` 启用 HTTPS 配置部分（取消注释）。

---

## 📊 监控和维护

### 日志轮转

创建 `/etc/logrotate.d/im`：

```bash
/var/log/im/*.log {
    daily
    rotate 30
    compress
    delaycompress
    notifempty
    create 0640 root root
    sharedscripts
    postrotate
        systemctl reload im-backend > /dev/null 2>&1 || true
    endscript
}
```

### 性能监控

```bash
# 查看 Java 进程
jps -v

# 查看内存使用
jstat -gc <pid>

# 查看线程
jstack <pid>

# 系统资源
htop
```

---

## 🆘 获取帮助

如果遇到问题，请按以下顺序排查：

1. 查看后端日志：`journalctl -u im-backend -n 100`
2. 查看 Nginx 日志：`tail -f /var/log/nginx/im-error.log`
3. 测试网络连接：`curl` 各个服务
4. 查看服务状态：`systemctl status`

---

## 📝 更新日志

记录每次部署的变更：

```bash
# 创建部署记录
echo "$(date): Deployed version 1.0.0" >> /opt/im/deploy.log
```

---

祝你部署顺利！🎉

