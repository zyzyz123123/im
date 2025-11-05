# 📦 IM 项目 - 快速部署指南

> 一个统一的部署方案，支持服务器构建和本地打包两种方式

## 🚀 快速开始

### 方案一：服务器上直接部署（推荐）

```bash
# 1. 克隆项目到服务器
git clone <your-repo>
cd im

# 2. 配置环境变量
cp env.example.txt .env
vi .env  # 修改密码

# 3. 一键部署
./deploy.sh server-build
```

### 方案二：本地打包上传（服务器无法 Git）

```bash
# 在本地电脑
./deploy.sh local-build
scp deploy.tar.gz user@server:/tmp/

# 在服务器上
cd /opt && tar xzf /tmp/deploy.tar.gz
cd deploy
./deploy.sh install
```

## 📝 常用命令

```bash
./deploy.sh server-build   # 服务器构建部署
./deploy.sh local-build    # 本地打包
./deploy.sh update         # 更新部署
./deploy.sh start          # 启动服务
./deploy.sh stop           # 停止服务
./deploy.sh restart        # 重启服务
./deploy.sh logs           # 查看日志
./deploy.sh status         # 查看状态
./deploy.sh help           # 显示帮助
```

## 📚 详细文档

完整的部署文档请查看：**[DEPLOY.md](./DEPLOY.md)**

包含：
- ✅ 详细的部署步骤
- ✅ 配置说明
- ✅ 常见问题排查
- ✅ 维护命令
- ✅ 安全建议

## 🗂️ 项目结构

```
im/
├── deploy.sh              # ⭐ 统一部署脚本
├── docker-compose.yml     # Docker Compose 配置
├── .env                   # 环境变量（需创建）
├── DEPLOY.md              # 📖 完整部署文档
│
├── backend/               # 后端 Spring Boot
│   └── Dockerfile         # 支持两种构建模式
│
├── frontend/              # 前端 Vue
│   └── Dockerfile         # 支持两种构建模式
│
└── docker/                # Docker 配置
    ├── mysql/init.sql
    ├── nginx/default.conf
    └── elasticsearch/Dockerfile
```

## ⚡ 服务端口

| 服务 | 端口 | 访问 |
|------|------|------|
| 前端 | 80 | http://localhost |
| 后端 | 内部 | 通过 Nginx 代理 |
| MySQL | 3306 | localhost:3306 |
| Redis | 6379 | localhost:6379 |
| ES | 9200 | localhost:9200 |

## 🔧 日常维护

```bash
# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f backend

# 重启某个服务
docker compose restart backend

# 备份数据库
docker exec im-mysql mysqldump -uroot -p im > backup.sql
```

## 🆘 快速故障排查

```bash
# 查看所有容器状态
docker compose ps

# 查看后端日志
docker compose logs backend | grep ERROR

# 检查配置是否生效
docker exec im-backend env | grep SPRING

# 重新构建（配置修改后）
docker compose down
docker compose build --no-cache
docker compose up -d
```

## 📖 更多信息

- **完整文档**: [DEPLOY.md](./DEPLOY.md)
- **项目 README**: [README.md](./README.md)
- **环境变量示例**: [env.example.txt](./env.example.txt)

---

**遇到问题？** 查看 [DEPLOY.md](./DEPLOY.md) 中的"常见问题"章节

