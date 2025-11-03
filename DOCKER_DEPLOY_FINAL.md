# IM 项目 Docker 部署完整指南

> **一键部署脚本：** `./quick-deploy.sh`

## 📋 目录

- [快速开始](#快速开始)
- [配置文件说明](#配置文件说明)
- [核心配置详解](#核心配置详解)
- [常见问题排查](#常见问题排查)
- [常用命令](#常用命令)

---

## 🚀 快速开始

### 前置要求

- Docker 20.10+
- Docker Compose 2.0+
- 至少 4GB 内存

### 一键部署

```bash
# 1. 克隆项目
git clone <your-repo>
cd im

# 2. 创建环境变量文件
cp env.example.txt .env
# 编辑 .env，修改密码和服务器地址

# 3. 执行部署脚本
chmod +x quick-deploy.sh
./quick-deploy.sh

# 或手动部署
docker compose build
docker compose up -d
```

### 手动部署步骤

```bash
# 1. 创建 .env 文件
cat > .env << 'EOF'
MYSQL_ROOT_PASSWORD=YourStrongPassword123!
MYSQL_DATABASE=im
MYSQL_USER=imuser
MYSQL_PASSWORD=ImPassword456!
REDIS_PASSWORD=RedisPassword789!
VITE_API_BASE_URL=http://your-server-ip
VITE_WS_BASE_URL=ws://your-server-ip
AI_APIKEY=your-api-key
EOF

# 2. 构建镜像
docker compose build

# 3. 启动服务
docker compose up -d

# 4. 查看日志
docker compose logs -f
```

### 验证部署

```bash
# 检查服务状态
docker compose ps

# 测试各服务
curl http://localhost              # 前端
curl http://localhost:8080         # 后端
curl http://localhost:9200         # Elasticsearch

# 查看日志
docker compose logs -f backend
```

---

## 📁 配置文件说明

### 核心文件清单

```
im/
├── docker-compose.yml              # ⭐ Docker Compose 主配置
├── .env                            # ⭐ 环境变量（需手动创建）
├── env.example.txt                 # 环境变量示例
├── quick-deploy.sh                 # 一键部署脚本
│
├── backend/
│   ├── Dockerfile                  # ⭐ 后端镜像构建
│   ├── .dockerignore
│   └── src/main/resources/
│       ├── application.properties           # 本地开发配置
│       └── application-prod.properties      # ⭐ Docker 生产配置
│
├── frontend/
│   ├── Dockerfile                  # ⭐ 前端镜像构建
│   └── .dockerignore
│
└── docker/
    ├── nginx/default.conf          # Nginx 配置
    ├── mysql/init.sql              # ⭐ 数据库初始化
    └── elasticsearch/Dockerfile    # ES + IK 分词器
```

### 端口映射

| 服务 | 容器端口 | 主机端口 | 访问地址 |
|------|---------|---------|----------|
| Frontend | 80 | 80 | http://localhost |
| Backend | 8080 | 8080 | http://localhost:8080 |
| MySQL | 3306 | 3306 | localhost:3306 |
| Redis | 6379 | 6379 | localhost:6379 |
| Elasticsearch | 9200 | 9200 | http://localhost:9200 |

---

## 🔑 核心配置详解

### 1. 环境变量 (.env)

```bash
# MySQL 配置
MYSQL_ROOT_PASSWORD=YourStrongPassword123!
MYSQL_DATABASE=im
MYSQL_USER=imuser
MYSQL_PASSWORD=ImPassword456!

# Redis 配置
REDIS_PASSWORD=RedisPassword789!

# 前端配置（替换为实际服务器地址）
VITE_API_BASE_URL=http://120.48.139.174
VITE_WS_BASE_URL=ws://120.48.139.174

# AI 配置
AI_APIKEY=sk-your-api-key
```

### 2. application-prod.properties（⭐ 最重要）

```properties
spring.application.name=im

# MySQL 配置
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://mysql:3306/im}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:imuser}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:ImPassword456!}

# Redis 配置（普通 Redis 连接）
spring.redis.host=${SPRING_REDIS_HOST:redis}
spring.redis.port=${SPRING_REDIS_PORT:6379}
spring.redis.password=${SPRING_REDIS_PASSWORD:RedisPassword789!}

# ⚠️ 关键配置：Spring Session Redis 专用
# 这是解决 "Unable to connect to localhost:6379" 的关键！
spring.data.redis.host=${SPRING_REDIS_HOST:redis}
spring.data.redis.port=${SPRING_REDIS_PORT:6379}
spring.data.redis.password=${SPRING_REDIS_PASSWORD:RedisPassword789!}

# Elasticsearch 配置
spring.elasticsearch.uris=${SPRING_ELASTICSEARCH_URIS:http://elasticsearch:9200}
```

**重要说明：**
- `spring.redis.*` - 给普通 Redis 操作用（RedisTemplate）
- `spring.data.redis.*` - 给 Spring Session 用（HTTP Session 存储）
- **两者必须同时配置**，否则 Spring Session 会连接 localhost！

### 3. docker-compose.yml 关键配置

```yaml
services:
  backend:
    environment:
      SPRING_PROFILES_ACTIVE: prod      # 激活 prod profile
      SPRING_REDIS_HOST: redis          # 硬编码 Docker 服务名
      SPRING_REDIS_PASSWORD: ${REDIS_PASSWORD:-}
      # ... 其他配置
```

### 4. 本地开发 vs Docker 部署

| 配置项 | 本地开发 | Docker 部署 |
|-------|---------|-------------|
| 配置文件 | application.properties | application-prod.properties |
| Profile | 默认 | prod |
| MySQL | localhost:3306 | mysql:3306 |
| Redis | localhost:6379 | redis:6379 |
| ES | localhost:9200 | elasticsearch:9200 |

---

## 🐛 常见问题排查

### ⚠️ 问题 1：Redis 连接 localhost 错误（最常见！）

**症状：**
```
RedisConnectionException: Unable to connect to localhost/<unresolved>:6379
```

**原因：**
Spring Session 使用 `spring.data.redis.*` 配置，而不是 `spring.redis.*`

**解决：**

在 `application-prod.properties` 中**同时**配置：

```properties
# 普通 Redis
spring.redis.host=redis
spring.redis.password=xxx

# Spring Session Redis（关键！）
spring.data.redis.host=redis
spring.data.redis.password=xxx
```

**验证：**
```bash
# 查看 jar 包配置
docker exec -it im-backend sh -c "unzip -p /app/app.jar BOOT-INF/classes/application-prod.properties" | grep redis

# 应该看到两组配置
```

---

### ⚠️ 问题 2：MySQL 权限错误

**症状：**
```
Access denied for user 'imuser'@'172.18.0.x'
```

**解决：**
```bash
# 进入 MySQL
docker exec -it im-mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD}

# 创建用户并授权
CREATE USER IF NOT EXISTS 'imuser'@'%' IDENTIFIED BY 'ImPassword456!';
GRANT ALL PRIVILEGES ON im.* TO 'imuser'@'%';
FLUSH PRIVILEGES;
EXIT;

# 重启后端
docker compose restart backend
```

---

### ⚠️ 问题 3：镜像拉取失败

**症状：**
```
context deadline exceeded
pull access denied
```

**解决：配置 Docker 镜像加速**

```bash
# 编辑 /etc/docker/daemon.json
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io"
  ]
}
EOF

# 重启 Docker
sudo systemctl restart docker
```

**Elasticsearch 特殊处理：**

使用 7.17.16 版本（8.x 在某些镜像源不可用）：

```yaml
elasticsearch:
  build:
    context: ./docker/elasticsearch
    dockerfile: Dockerfile  # 使用自定义 Dockerfile
```

`docker/elasticsearch/Dockerfile`:
```dockerfile
FROM elasticsearch:7.17.16

RUN /usr/share/elasticsearch/bin/elasticsearch-plugin install --batch \
    https://get.infini.cloud/elasticsearch/analysis-ik/7.17.16
```

---

### ⚠️ 问题 4：前端构建失败

**症状：**
```
You are using Node.js 18.x. Vite requires 20.19+
Cannot find native binding
```

**解决：**

使用 Node 20 + Debian 基础镜像：

```dockerfile
FROM node:20-slim AS build  # 不是 node:18-alpine
```

---

### ⚠️ 问题 5：配置修改不生效

**症状：**
修改了配置，重启后还是用旧配置

**解决：完全重建**

```bash
# 停止所有服务
docker compose down

# 删除旧镜像
docker rmi $(docker images | grep 'im-' | awk '{print $3}')

# 清理缓存
docker system prune -f

# 重新构建（不使用缓存）
docker compose build --no-cache

# 启动
docker compose up -d
```

**验证配置：**
```bash
# 检查 jar 包内的配置
docker exec -it im-backend sh -c "unzip -p /app/app.jar BOOT-INF/classes/application-prod.properties" | head -30
```

---

### ⚠️ 问题 6：端口被占用

**症状：**
```
bind: address already in use
```

**解决：**
```bash
# 查找占用进程
lsof -i :8080
# 或
netstat -tlnp | grep 8080

# 杀死进程
kill -9 [PID]

# 或修改端口
# 编辑 .env 文件
BACKEND_PORT=8081
```

---

## 🛠️ 常用命令

### 服务管理

```bash
# 启动所有服务
docker compose up -d

# 停止所有服务
docker compose down

# 停止并删除数据卷（⚠️ 会删除数据）
docker compose down -v

# 重启特定服务
docker compose restart backend

# 查看服务状态
docker compose ps

# 查看服务日志
docker compose logs -f backend

# 查看所有日志
docker compose logs -f
```

### 重新构建

```bash
# 重新构建特定服务
docker compose build backend

# 重新构建所有服务（不使用缓存）
docker compose build --no-cache

# 完全重建（推荐）
docker compose down
docker compose build --no-cache
docker compose up -d
```

### 数据库操作

```bash
# 进入 MySQL
docker exec -it im-mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD}

# 备份数据库
docker exec im-mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} im > backup.sql

# 恢复数据库
docker exec -i im-mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} im < backup.sql

# 查看数据库表
docker exec -it im-mysql mysql -uimuser -p${MYSQL_PASSWORD} -e "SHOW TABLES FROM im"
```

### Redis 操作

```bash
# 进入 Redis CLI
docker exec -it im-redis redis-cli -a ${REDIS_PASSWORD}

# 测试连接
docker exec -it im-redis redis-cli -a ${REDIS_PASSWORD} ping

# 查看所有 keys
docker exec -it im-redis redis-cli -a ${REDIS_PASSWORD} KEYS '*'

# 清空 Redis
docker exec -it im-redis redis-cli -a ${REDIS_PASSWORD} FLUSHALL
```

### 调试命令

```bash
# 进入容器
docker exec -it im-backend sh

# 查看容器环境变量
docker exec -it im-backend env

# 查看容器内文件
docker exec -it im-backend ls -la /app

# 查看网络
docker network inspect im-network

# 查看数据卷
docker volume ls
docker volume inspect im-mysql-data

# 测试容器间连接
docker exec -it im-backend ping mysql
docker exec -it im-backend ping redis

# 查看资源使用
docker stats
```

---

## 📊 健康检查清单

部署完成后，逐项检查：

### 基础检查

```bash
# 1. 所有容器都在运行
docker compose ps
# 状态应该都是 Up (healthy)

# 2. 前端可访问
curl http://localhost
# 应该返回 HTML

# 3. 后端可访问
curl http://localhost:8080
# 应该返回 JSON 或错误页面（不是连接失败）

# 4. Redis 正常
docker exec -it im-redis redis-cli -a ${REDIS_PASSWORD} ping
# 应该返回 PONG

# 5. MySQL 正常
docker exec -it im-mysql mysql -uimuser -p${MYSQL_PASSWORD} -e "SELECT 1"
# 应该返回 1

# 6. Elasticsearch 正常
curl http://localhost:9200/_cluster/health
# 应该返回 JSON

# 7. 后端日志无 ERROR
docker compose logs backend | grep ERROR
# 应该没有 localhost 连接错误
```

### 功能测试

```bash
# 测试注册
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"nickname":"testuser","password":"test123","email":"test@example.com"}'

# 测试登录
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"nickname":"testuser","password":"test123"}'
```

---

## 🔒 安全建议

### 生产环境必做

1. **修改所有默认密码**
   - MySQL root 密码
   - MySQL 用户密码
   - Redis 密码

2. **限制端口访问**
   ```bash
   # 只开放 80 和 443
   sudo ufw allow 80/tcp
   sudo ufw allow 443/tcp
   sudo ufw deny 3306/tcp  # MySQL 不对外开放
   sudo ufw deny 6379/tcp  # Redis 不对外开放
   sudo ufw deny 9200/tcp  # ES 不对外开放
   ```

3. **配置 HTTPS**
   - 使用 Let's Encrypt 证书
   - 配置 Nginx SSL

4. **定期备份**
   ```bash
   # 自动备份脚本
   0 2 * * * docker exec im-mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} im > /backup/im_$(date +\%Y\%m\%d).sql
   ```

5. **更新镜像**
   ```bash
   # 定期更新基础镜像
   docker compose pull
   docker compose up -d
   ```

---

## 🌍 国内部署优化

### Docker 镜像加速

```bash
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://docker.mirrors.sjtug.sjtu.edu.cn"
  ]
}
EOF

sudo systemctl restart docker
```

### Maven 加速（pom.xml）

```xml
<repositories>
  <repository>
    <id>aliyun</id>
    <url>https://maven.aliyun.com/repository/public</url>
  </repository>
</repositories>
```

### npm 加速（package.json）

```bash
npm config set registry https://registry.npmmirror.com
```

---

## 📚 参考资料

- [Docker 官方文档](https://docs.docker.com/)
- [Docker Compose 文档](https://docs.docker.com/compose/)
- [Spring Boot Docker](https://spring.io/guides/topicals/spring-boot-docker)
- [项目 README](./README.md)

---

## 🆘 获取帮助

1. **查看日志**
   ```bash
   docker compose logs -f backend
   ```

2. **重启服务**
   ```bash
   docker compose restart backend
   ```

3. **完全重建**
   ```bash
   docker compose down -v
   docker compose build --no-cache
   docker compose up -d
   ```

4. **检查配置**
   ```bash
   docker exec -it im-backend env | grep REDIS
   ```

---

**最后更新：** 2025-11-03  
**版本：** 2.0 Final
