#!/bin/bash

# IM 项目 Docker 快速部署脚本
# 使用方法: ./quick-deploy.sh

set -e

echo "========================================="
echo "  IM 项目 Docker 快速部署"
echo "========================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 Docker 和 Docker Compose
echo "检查环境..."
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker 未安装，请先安装 Docker${NC}"
    exit 1
fi

if ! docker compose version &> /dev/null; then
    echo -e "${RED}❌ Docker Compose 未安装或版本过低${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Docker 和 Docker Compose 已安装${NC}"
echo ""

# 检查 .env 文件
if [ ! -f ".env" ]; then
    echo -e "${YELLOW}⚠️  未找到 .env 文件，正在创建...${NC}"
    
    read -p "请输入服务器 IP 或域名（用于前端访问后端）: " SERVER_IP
    
    cat > .env << EOF
# MySQL 配置
MYSQL_ROOT_PASSWORD=YourStrongPassword123!
MYSQL_DATABASE=im
MYSQL_USER=imuser
MYSQL_PASSWORD=ImPassword456!
MYSQL_PORT=3306

# Redis 配置
REDIS_PASSWORD=RedisPassword789!
REDIS_PORT=6379

# Elasticsearch 配置
ES_PORT=9200

# 后端配置
BACKEND_PORT=8080

# 前端配置
FRONTEND_PORT=80
VITE_API_BASE_URL=http://${SERVER_IP}
VITE_WS_BASE_URL=ws://${SERVER_IP}

# AI 配置
AI_APIKEY=your-api-key-here
EOF
    
    echo -e "${GREEN}✅ .env 文件已创建${NC}"
    echo -e "${YELLOW}⚠️  请编辑 .env 文件修改密码和 API Key${NC}"
    echo ""
    
    read -p "是否现在编辑 .env 文件？(y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        ${EDITOR:-vi} .env
    fi
else
    echo -e "${GREEN}✅ .env 文件已存在${NC}"
fi

echo ""

# 询问是否重新构建
read -p "是否需要完全重新构建（会删除所有数据）？(y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "停止并删除所有容器和数据..."
    docker compose down -v
    
    echo "清理 Docker 缓存..."
    docker system prune -f
    
    echo "删除旧镜像..."
    docker images | grep 'im-' | awk '{print $3}' | xargs -r docker rmi -f
    
    REBUILD="--no-cache"
else
    REBUILD=""
fi

echo ""
echo "开始构建镜像..."
docker compose build $REBUILD

echo ""
echo "启动所有服务..."
docker compose up -d

echo ""
echo "等待服务启动..."
sleep 10

echo ""
echo "========================================="
echo "  服务状态"
echo "========================================="
docker compose ps

echo ""
echo "========================================="
echo "  健康检查"
echo "========================================="

# 检查各服务
check_service() {
    local service=$1
    local port=$2
    local url=$3
    
    echo -n "检查 $service ... "
    if curl -s -f "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 正常${NC}"
        return 0
    else
        echo -e "${RED}❌ 异常${NC}"
        return 1
    fi
}

# 等待服务完全启动
sleep 5

check_service "MySQL" "3306" "tcp://localhost:3306" || true
check_service "Redis" "6379" "tcp://localhost:6379" || true
check_service "Elasticsearch" "9200" "http://localhost:9200" || true
check_service "Backend" "8080" "http://localhost:8080" || true
check_service "Frontend" "80" "http://localhost" || true

echo ""
echo "========================================="
echo "  查看日志（Ctrl+C 退出）"
echo "========================================="
echo ""

read -p "是否查看后端日志？(y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    docker compose logs -f backend
fi

echo ""
echo "========================================="
echo "  部署完成"
echo "========================================="
echo ""
echo "访问地址："
echo "  前端: http://localhost"
echo "  后端: http://localhost:8080"
echo ""
echo "常用命令："
echo "  查看所有日志:   docker compose logs -f"
echo "  查看后端日志:   docker compose logs -f backend"
echo "  重启服务:       docker compose restart"
echo "  停止服务:       docker compose down"
echo ""
echo "如遇到问题，请查看 DOCKER_DEPLOY_FINAL.md 和 CONFIG_FILES_SUMMARY.md"
echo ""

