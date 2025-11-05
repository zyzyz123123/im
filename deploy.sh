#!/bin/bash

# IM 项目统一部署脚本
# 支持两种部署方式：服务器构建 和 本地打包

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;36m'
NC='\033[0m'

# 显示帮助
show_help() {
    cat << EOF
========================================
  IM 项目部署脚本
========================================

用法: ./deploy.sh [命令]

命令:
  server-build    服务器上构建并部署（需要能 git pull）
  local-build     本地构建打包（生成 deploy.tar.gz）
  install         安装部署（使用已打包的文件）
  update          更新部署（拉取代码并重启）
  start           启动所有服务
  stop            停止所有服务
  restart         重启所有服务
  logs            查看所有日志
  status          查看服务状态
  help            显示此帮助信息

示例:
  # 服务器上一键部署
  ./deploy.sh server-build

  # 本地打包
  ./deploy.sh local-build

  # 查看日志
  ./deploy.sh logs

EOF
}

# 检查环境
check_environment() {
    echo "检查环境..."
    
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}❌ Docker 未安装${NC}"
        exit 1
    fi
    
    if ! docker compose version &> /dev/null; then
        echo -e "${RED}❌ Docker Compose 未安装或版本过低${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ Docker 环境正常${NC}"
}

# 检查 .env 文件
check_env_file() {
    if [ ! -f ".env" ]; then
        echo -e "${YELLOW}⚠️  未找到 .env 文件${NC}"
        
        if [ -f "env.example.txt" ]; then
            echo "正在创建 .env 文件..."
            cp env.example.txt .env
            echo -e "${GREEN}✅ 已创建 .env 文件${NC}"
            echo -e "${YELLOW}⚠️  请编辑 .env 文件修改密码等配置${NC}"
            echo ""
            read -p "是否现在编辑 .env 文件？(y/n) " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                ${EDITOR:-vi} .env
            fi
        else
            echo -e "${RED}❌ 未找到 env.example.txt${NC}"
            exit 1
        fi
    fi
}

# 服务器上构建部署
server_build() {
    echo "========================================="
    echo "  服务器上构建部署"
    echo "========================================="
    echo ""
    
    check_environment
    check_env_file
    
    echo "构建 Docker 镜像..."
    docker compose build
    
    echo ""
    echo "启动服务..."
    docker compose up -d
    
    echo ""
    echo "等待服务启动..."
    sleep 10
    
    echo ""
    docker compose ps
    
    echo ""
    echo -e "${GREEN}✅ 部署完成${NC}"
    echo ""
    echo "访问地址: http://localhost"
    echo "查看日志: docker compose logs -f backend"
}

# 本地构建打包
local_build() {
    echo "========================================="
    echo "  本地构建打包"
    echo "========================================="
    echo ""
    
    # 检查构建工具
    if ! command -v mvn &> /dev/null; then
        echo -e "${RED}❌ Maven 未安装${NC}"
        exit 1
    fi
    
    if ! command -v npm &> /dev/null; then
        echo -e "${RED}❌ npm 未安装${NC}"
        exit 1
    fi
    
    echo "清理旧的构建产物..."
    rm -rf deploy
    mkdir -p deploy
    
    # 构建后端
    echo ""
    echo "构建后端..."
    cd backend
    mvn clean package -DskipTests
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ 后端构建成功${NC}"
        mkdir -p ../deploy/backend
        cp target/im-*.jar ../deploy/backend/app.jar
    else
        echo -e "${RED}❌ 后端构建失败${NC}"
        exit 1
    fi
    
    cd ..
    
    # 构建前端
    echo ""
    echo "构建前端..."
    cd frontend
    npm install
    npm run build
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ 前端构建成功${NC}"
        mkdir -p ../deploy/frontend
        cp -r dist ../deploy/frontend/
    else
        echo -e "${RED}❌ 前端构建失败${NC}"
        exit 1
    fi
    
    cd ..
    
    # 复制必要文件
    echo ""
    echo "复制配置文件..."
    cp deploy.sh deploy/                    # 复制部署脚本本身
    cp docker-compose.yml deploy/
    cp -r docker deploy/
    cp backend/Dockerfile deploy/backend/
    cp frontend/Dockerfile deploy/frontend/
    cp backend/src/main/resources/application-prod.properties deploy/backend/ 2>/dev/null || true
    [ -f .env ] && cp .env deploy/
    [ -f env.example.txt ] && cp env.example.txt deploy/
    
    echo ""
    echo "检查打包文件..."
    echo "后端 JAR: $(ls -lh deploy/backend/app.jar 2>/dev/null | awk '{print $5}' || echo '未找到')"
    echo "前端文件: $(ls deploy/frontend/dist/ 2>/dev/null | wc -l || echo 0) 个文件"
    
    # 打包
    echo ""
    echo "打包..."
    tar czf deploy.tar.gz deploy/
    
    echo ""
    echo "========================================="
    echo "  构建完成"
    echo "========================================="
    echo ""
    echo "部署包: deploy.tar.gz"
    ls -lh deploy.tar.gz | awk '{print "大小: " $5}'
    echo ""
    echo "上传到服务器:"
    echo -e "${BLUE}scp deploy.tar.gz user@server:/tmp/${NC}"
    echo ""
    echo "在服务器上部署:"
    echo -e "${BLUE}cd /opt && tar xzf /tmp/deploy.tar.gz && cd deploy && ./deploy.sh install${NC}"
}

# 安装部署（使用打包的文件）
install() {
    echo "========================================="
    echo "  安装部署（本地打包模式）"
    echo "========================================="
    echo ""
    
    check_environment
    check_env_file
    
    # 设置构建参数，使用本地文件
    export BUILD_MODE=local
    
    echo "使用本地打包模式构建 Docker 镜像..."
    docker compose build
    
    echo ""
    echo "启动服务..."
    docker compose up -d
    
    echo ""
    echo "等待服务启动..."
    sleep 10
    
    echo ""
    docker compose ps
    
    echo ""
    echo -e "${GREEN}✅ 部署完成${NC}"
}

# 更新部署
update() {
    echo "========================================="
    echo "  更新部署"
    echo "========================================="
    echo ""
    
    # 判断构建模式
    if [ -f "backend/app.jar" ] && [ -d "frontend/dist" ]; then
        echo "检测到预编译文件，使用本地打包模式..."
        export BUILD_MODE=local
    elif command -v git &> /dev/null && [ -d ".git" ]; then
        echo "Git 环境，使用服务器构建模式..."
        echo "拉取最新代码..."
        git pull
        export BUILD_MODE=server
    elif [ -d "backend/src" ]; then
        echo "源码目录存在，使用服务器构建模式..."
        export BUILD_MODE=server
    else
        echo -e "${YELLOW}⚠️  无法确定构建模式，默认使用服务器构建${NC}"
        export BUILD_MODE=server
    fi
    
    echo ""
    echo "重新构建（BUILD_MODE=$BUILD_MODE）..."
    docker compose build
    
    echo ""
    echo "重启服务..."
    docker compose up -d
    
    echo ""
    echo -e "${GREEN}✅ 更新完成${NC}"
}

# 启动服务
start() {
    echo "启动所有服务..."
    docker compose up -d
    echo -e "${GREEN}✅ 服务已启动${NC}"
}

# 停止服务
stop() {
    echo "停止所有服务..."
    docker compose down
    echo -e "${GREEN}✅ 服务已停止${NC}"
}

# 重启服务
restart() {
    echo "重启所有服务..."
    docker compose restart
    echo -e "${GREEN}✅ 服务已重启${NC}"
}

# 查看日志
logs() {
    docker compose logs -f
}

# 查看状态
status() {
    echo "========================================="
    echo "  服务状态"
    echo "========================================="
    docker compose ps
    echo ""
    echo "========================================="
    echo "  资源使用"
    echo "========================================="
    docker stats --no-stream
}

# 主程序
main() {
    case "${1:-help}" in
        server-build)
            server_build
            ;;
        local-build)
            local_build
            ;;
        install)
            install
            ;;
        update)
            update
            ;;
        start)
            start
            ;;
        stop)
            stop
            ;;
        restart)
            restart
            ;;
        logs)
            logs
            ;;
        status)
            status
            ;;
        help|*)
            show_help
            ;;
    esac
}

main "$@"

