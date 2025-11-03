# IM 即时通讯系统 - Docker 部署指南

## 📋 目录

- [系统要求](#系统要求)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [部署步骤](#部署步骤)
- [常用命令](#常用命令)
- [故障排除](#故障排除)
- [架构说明](#架构说明)

## 🔧 系统要求

- Docker 20.10+
- Docker Compose 2.0+
- 至少 4GB 可用内存
- 至少 10GB 可用磁盘空间

### 安装 Docker

**Linux:**
```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
```

**macOS:**
下载并安装 [Docker Desktop for Mac](https://www.docker.com/products/docker-desktop)

**Windows:**
下载并安装 [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop)

## 🚀 快速开始

### 1. 克隆项目（如果还没有）

```bash
git clone <your-repo-url>
cd im
```

### 2. 配置环境变量

```bash
# 复制环境变量示例文件
cp .env.example .env

# 编辑 .env 文件，修改配置
vim .env
```

**重要配置项：**
- `AI_APIKEY`: 通义千问 API Key（必须配置）
- `MYSQL_ROOT_PASSWORD`: MySQL root 密码
- `MYSQL_PASSWORD`: 应用数据库密码

### 3. 启动服务

```bash
# 方式一：使用部署脚本（推荐）
bash docker-deploy.sh start

# 方式二：直接使用 docker compose
docker compose up -d
```

### 4. 访问应用

- **前端界面**: http://localhost
- **后端 API**: http://localhost:8080
- **Elasticsearch**: http://localhost:9200

## ⚙️ 配置说明

### 环境变量配置

编辑 `.env` 文件来自定义配置：

```bash
# MySQL 配置
MYSQL_ROOT_PASSWORD=root123456      # MySQL root 密码
MYSQL_DATABASE=im                   # 数据库名
MYSQL_USER=imuser                   # 应用数据库用户
MYSQL_PASSWORD=impassword           # 应用数据库密码
MYSQL_PORT=3306                     # MySQL 端口

# Redis 配置
REDIS_PASSWORD=                     # Redis 密码（留空表示无密码）
REDIS_PORT=6379                     # Redis 端口

# Elasticsearch 配置
ES_PORT=9200                        # Elasticsearch 端口

# 后端配置
BACKEND_PORT=8080                   # 后端服务端口

# 前端配置
FRONTEND_PORT=80                    # 前端服务端口
VITE_API_BASE_URL=http://localhost  # API 基础 URL
VITE_WS_BASE_URL=ws://localhost     # WebSocket URL

# AI 配置
AI_APIKEY=your-api-key-here         # 通义千问 API Key
```

### 生产环境配置

对于生产环境部署，需要修改以下配置：

1. **修改密码**：所有默认密码都应该修改
2. **配置域名**：修改 `VITE_API_BASE_URL` 和 `VITE_WS_BASE_URL`
3. **端口映射**：根据需要调整端口
4. **资源限制**：在 `docker-compose.yml` 中添加资源限制

## 📦 部署步骤

### 开发环境部署

```bash
# 1. 启动服务
bash docker-deploy.sh start

# 2. 查看日志
bash docker-deploy.sh logs

# 3. 查看特定服务日志
bash docker-deploy.sh logs backend
```

### 生产环境部署

```bash
# 1. 配置环境变量
cp .env.example .env
vim .env  # 修改为生产环境配置

# 2. 构建镜像
bash docker-deploy.sh build

# 3. 启动服务
bash docker-deploy.sh start

# 4. 检查服务状态
docker compose ps

# 5. 查看日志
bash docker-deploy.sh logs
```

### 服务器部署

如果要部署到远程服务器：

```bash
# 1. 上传项目到服务器
scp -r im/ user@server:/path/to/deploy/

# 2. SSH 登录到服务器
ssh user@server

# 3. 进入项目目录
cd /path/to/deploy/im

# 4. 配置环境变量
vim .env

# 5. 启动服务
bash docker-deploy.sh start

# 6. 配置防火墙（如果需要）
sudo ufw allow 80/tcp
sudo ufw allow 8080/tcp
```

## 🔨 常用命令

### 部署脚本命令

```bash
# 启动服务
bash docker-deploy.sh start

# 停止服务
bash docker-deploy.sh stop

# 重启服务
bash docker-deploy.sh restart

# 重新构建镜像
bash docker-deploy.sh build

# 查看所有服务日志
bash docker-deploy.sh logs

# 查看特定服务日志
bash docker-deploy.sh logs backend
bash docker-deploy.sh logs frontend
bash docker-deploy.sh logs mysql

# 清理所有数据（危险操作）
bash docker-deploy.sh clean

# 显示帮助
bash docker-deploy.sh help
```

### Docker Compose 命令

```bash
# 启动服务
docker compose up -d

# 停止服务
docker compose down

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f

# 查看特定服务日志
docker compose logs -f backend

# 重启特定服务
docker compose restart backend

# 进入容器
docker compose exec backend sh
docker compose exec mysql mysql -uroot -p

# 查看资源使用情况
docker stats

# 删除所有容器和数据卷
docker compose down -v
```

### 数据库操作

```bash
# 进入 MySQL
docker compose exec mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD}

# 导出数据库
docker compose exec mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} im > backup.sql

# 导入数据库
docker compose exec -T mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} im < backup.sql

# 进入 Redis
docker compose exec redis redis-cli

# 查看 Elasticsearch 状态
curl http://localhost:9200/_cluster/health?pretty
```

## 🐛 故障排除

### 服务无法启动

```bash
# 1. 查看服务状态
docker compose ps

# 2. 查看日志
docker compose logs

# 3. 检查端口占用
lsof -i :80
lsof -i :8080
lsof -i :3306

# 4. 重启服务
bash docker-deploy.sh restart
```

### 后端连接数据库失败

```bash
# 1. 检查 MySQL 是否启动
docker compose ps mysql

# 2. 查看 MySQL 日志
docker compose logs mysql

# 3. 测试数据库连接
docker compose exec mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD}

# 4. 检查网络连接
docker compose exec backend ping mysql
```

### Elasticsearch 内存不足

如果 Elasticsearch 启动失败，可能是内存不足：

```bash
# 修改 docker-compose.yml 中的内存配置
# 将 ES_JAVA_OPTS 修改为更小的值
ES_JAVA_OPTS=-Xms256m -Xmx256m
```

### 前端无法访问后端

```bash
# 1. 检查后端是否启动
docker compose ps backend

# 2. 检查网络连接
docker compose exec frontend ping backend

# 3. 查看 nginx 日志
docker compose logs frontend

# 4. 测试后端 API
curl http://localhost:8080/actuator/health
```

### 清理并重新开始

```bash
# 停止所有服务并删除数据
bash docker-deploy.sh clean

# 重新构建并启动
bash docker-deploy.sh build
bash docker-deploy.sh start
```

## 🏗️ 架构说明

### 服务架构

```
┌─────────────┐
│   用户请求   │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Nginx     │ (前端 + 反向代理)
│   Port: 80  │
└──────┬──────┘
       │
       ├──────────────────┐
       │                  │
       ▼                  ▼
┌─────────────┐    ┌─────────────┐
│   静态文件   │    │   后端 API  │
│   (Vue.js)  │    │ (Spring Boot)│
└─────────────┘    └──────┬──────┘
                          │
                ┌─────────┼─────────┐
                │         │         │
                ▼         ▼         ▼
         ┌──────────┬─────────┬─────────┐
         │  MySQL   │  Redis  │   ES    │
         │ Port:3306│ Port:6379│Port:9200│
         └──────────┴─────────┴─────────┘
```

### 数据卷

- `im-mysql-data`: MySQL 数据持久化
- `im-redis-data`: Redis 数据持久化
- `im-es-data`: Elasticsearch 数据持久化
- `im-backend-logs`: 后端日志

### 网络

所有服务在同一个 Docker 网络 `im-network` 中，可以通过服务名互相访问。

## 📝 注意事项

1. **首次启动**：首次启动可能需要较长时间，因为需要下载镜像和初始化数据库
2. **数据持久化**：所有数据都保存在 Docker 数据卷中，删除容器不会丢失数据
3. **备份数据**：定期备份 MySQL 数据和 Elasticsearch 索引
4. **安全配置**：生产环境请修改所有默认密码
5. **资源监控**：使用 `docker stats` 监控资源使用情况
6. **日志管理**：定期清理日志文件，避免占用过多磁盘空间

## 🔄 更新部署

当代码更新后，重新部署：

```bash
# 1. 拉取最新代码
git pull

# 2. 重新构建镜像
bash docker-deploy.sh build

# 3. 重启服务
bash docker-deploy.sh restart
```

## 📞 获取帮助

如有问题，请查看：
- 项目日志：`bash docker-deploy.sh logs`
- Docker 文档：https://docs.docker.com/
- Spring Boot 文档：https://spring.io/projects/spring-boot
- Vue.js 文档：https://vuejs.org/

---

**祝你部署顺利！🎉**


