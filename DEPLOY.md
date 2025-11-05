# IM 项目部署指南

> **一站式部署文档** - 支持服务器构建和本地打包两种方式

## 📋 目录

- [快速开始](#快速开始)
- [方案选择](#方案选择)
- [配置说明](#配置说明)
- [更新部署](#更新部署)
- [常见问题](#常见问题)
- [维护命令](#维护命令)

---

## 🚀 快速开始

### 前置要求

- Docker 20.10+
- Docker Compose 2.0+
- 服务器至少 2核4GB 内存

### 首次部署

```bash
# 1. 克隆或上传项目到服务器
git clone <your-repo> 或 scp 上传

# 2. 进入项目目录
cd im

# 3. 配置环境变量
cp env.example.txt .env
vi .env  # 修改密码和 API Key

# 4. 一键部署
./deploy.sh
```

---

## 🎯 方案选择

### 方案 A：服务器上构建（适合有 Git 访问权限）

**适用场景：**
- ✅ 服务器可以访问 GitHub
- ✅ 服务器网络良好，可以下载 Maven/npm 依赖
- ✅ 服务器配置 ≥ 2核4GB

**操作步骤：**
```bash
# 在服务器上
git clone <repo>
cd im
./deploy.sh server-build
```

**优点：** 简单快捷，一条命令完成  
**缺点：** 需要下载依赖，首次较慢（5-10分钟）

---

### 方案 B：本地打包上传（服务器无法 Git Pull）

**适用场景：**
- ✅ 服务器无法访问 GitHub
- ✅ 服务器网络不稳定
- ✅ 服务器配置较低

**操作步骤：**
```bash
# 在本地电脑
cd im
./deploy.sh local-build

# 上传到服务器
scp deploy.tar.gz user@server:/tmp/

# 在服务器上
cd /opt && tar xzf /tmp/deploy.tar.gz
cd im
./deploy.sh install
```

**优点：** 不占用服务器资源，部署快（1-2分钟）  
**缺点：** 需要传输大文件（约60MB）

---

## 📁 配置说明

### 环境变量 (.env)

```bash
# MySQL 配置
MYSQL_ROOT_PASSWORD=YourStrongPassword123!
MYSQL_DATABASE=im
MYSQL_USER=imuser
MYSQL_PASSWORD=ImPassword456!

# Redis 配置
REDIS_PASSWORD=RedisPassword789!

# AI 配置
AI_APIKEY=sk-your-api-key
```

### 端口映射

| 服务 | 容器端口 | 主机端口 | 访问地址 |
|------|---------|---------|----------|
| Frontend | 80 | 80 | http://localhost |
| Backend | 8080 | 内部访问 | 通过 Nginx 代理 |
| MySQL | 3306 | 127.0.0.1:3306 | localhost:3306 |
| Redis | 6379 | 127.0.0.1:6379 | localhost:6379 |
| Elasticsearch | 9200 | 127.0.0.1:9200 | localhost:9200 |

---

## 🔄 更新部署

### 日常更新（有 Git 访问）

```bash
# 在服务器上
cd im
git pull
./deploy.sh update
```

### 本地打包更新（无 Git 访问）

```bash
# 在本地
cd im
./deploy.sh local-build

# 上传并部署
scp deploy.tar.gz user@server:/tmp/
ssh user@server "cd /opt/im && tar xzf /tmp/deploy.tar.gz && ./deploy.sh update"
```

### 只更新前端

```bash
# 服务器构建方式
docker compose build frontend
docker compose up -d frontend

# 本地打包方式（先本地 npm run build）
rsync -avz frontend/dist/ user@server:/opt/im/frontend/dist/
ssh user@server "cd /opt/im && docker compose build frontend && docker compose up -d frontend"
```

### 只更新后端

```bash
# 服务器构建方式
docker compose build backend
docker compose up -d backend

# 本地打包方式（先本地 mvn package）
scp backend/target/im-*.jar user@server:/opt/im/backend/app.jar
ssh user@server "cd /opt/im && docker compose build backend && docker compose up -d backend"
```

---

## 🐛 常见问题

### 1. Redis 连接 localhost 错误

**症状：** `Unable to connect to localhost:6379`

**解决：** 检查 `application-prod.properties` 配置

```properties
# 两个配置都要有
spring.redis.host=redis
spring.data.redis.host=redis
```

### 2. MySQL 权限错误

**症状：** `Access denied for user 'imuser'`

**解决：**
```bash
docker exec -it im-mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD}
CREATE USER IF NOT EXISTS 'imuser'@'%' IDENTIFIED BY 'ImPassword456!';
GRANT ALL PRIVILEGES ON im.* TO 'imuser'@'%';
FLUSH PRIVILEGES;
EXIT;
docker compose restart backend
```

### 3. 端口被占用

**症状：** `bind: address already in use`

**解决：**
```bash
# 查找占用进程
lsof -i :80
kill -9 <PID>

# 或修改端口
vi .env
# 修改 FRONTEND_PORT=8080
```

### 4. 配置修改不生效

**解决：** 完全重建

```bash
docker compose down
docker compose build --no-cache
docker compose up -d
```

---

## 🛠️ 维护命令

### 服务管理

```bash
# 启动所有服务
docker compose up -d

# 停止所有服务
docker compose down

# 重启特定服务
docker compose restart backend
docker compose restart frontend

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f backend
docker compose logs -f frontend
```

### 数据库操作

```bash
# 进入 MySQL
docker exec -it im-mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD}

# 备份数据库
docker exec im-mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} im > backup_$(date +%Y%m%d).sql

# 恢复数据库
docker exec -i im-mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} im < backup.sql
```

### Redis 操作

```bash
# 进入 Redis CLI
docker exec -it im-redis redis-cli -a ${REDIS_PASSWORD}

# 清空缓存
docker exec -it im-redis redis-cli -a ${REDIS_PASSWORD} FLUSHALL
```

### 调试命令

```bash
# 进入容器
docker exec -it im-backend sh
docker exec -it im-frontend sh

# 查看环境变量
docker exec -it im-backend env | grep SPRING

# 查看容器资源使用
docker stats

# 测试容器间连接
docker exec -it im-backend ping mysql
docker exec -it im-backend ping redis
```

---

## 🔒 安全建议

### 生产环境检查清单

- [ ] 修改所有默认密码
- [ ] 配置防火墙（只开放 80/443）
- [ ] 配置 HTTPS 证书
- [ ] 定期备份数据库
- [ ] 定期更新 Docker 镜像

### 防火墙配置

```bash
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw deny 3306/tcp
sudo ufw deny 6379/tcp
sudo ufw deny 9200/tcp
sudo ufw enable
```

### 自动备份

```bash
# 添加到 crontab
crontab -e

# 每天凌晨 2 点备份
0 2 * * * docker exec im-mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD} im > /backup/im_$(date +\%Y\%m\%d).sql
```

---

## 📊 性能监控

### 健康检查

```bash
# 检查所有容器状态
docker compose ps

# 检查后端健康
curl http://localhost:8080/actuator/health

# 检查 Elasticsearch
curl http://localhost:9200/_cluster/health

# 检查 Redis
docker exec -it im-redis redis-cli -a ${REDIS_PASSWORD} ping
```

### 日志分析

```bash
# 查看错误日志
docker compose logs backend | grep ERROR

# 查看最近 100 行日志
docker compose logs --tail=100 backend

# 实时查看日志
docker compose logs -f backend
```

---

## 🆘 紧急回滚

```bash
# 1. 停止当前版本
docker compose down

# 2. 恢复数据库备份（如果需要）
docker exec -i im-mysql mysql -uroot -p${MYSQL_ROOT_PASSWORD} im < backup_yesterday.sql

# 3. 使用旧镜像启动
docker tag im-backend:backup im-backend:latest
docker compose up -d
```

---

## 📚 文件结构

```
im/
├── deploy.sh                    # ⭐ 统一部署脚本
├── docker-compose.yml           # ⭐ Docker Compose 配置
├── .env                         # ⭐ 环境变量（需手动创建）
├── env.example.txt              # 环境变量示例
│
├── backend/
│   ├── Dockerfile               # 后端 Dockerfile（支持两种模式）
│   └── src/main/resources/
│       └── application-prod.properties  # 生产环境配置
│
├── frontend/
│   └── Dockerfile               # 前端 Dockerfile（支持两种模式）
│
└── docker/
    ├── nginx/default.conf       # Nginx 配置
    ├── mysql/init.sql           # 数据库初始化
    └── elasticsearch/Dockerfile # ES + IK 分词器
```

---

## 🌍 国内部署优化

### Docker 镜像加速

```bash
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io"
  ]
}
EOF

sudo systemctl restart docker
```

### Maven 加速

在 `pom.xml` 中已配置阿里云镜像源。

### npm 加速

```bash
npm config set registry https://registry.npmmirror.com
```

---

**最后更新:** 2025-11-04  
**版本:** 3.0 Unified

如有问题请查看日志: `docker compose logs -f`

