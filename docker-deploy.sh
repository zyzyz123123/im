#!/bin/bash

# IM 即时通讯系统 - Docker 自动化部署脚本
# 使用方法: bash docker-deploy.sh [选项]
# 选项:
#   start   - 启动所有服务
#   stop    - 停止所有服务
#   restart - 重启所有服务
#   build   - 重新构建镜像
#   logs    - 查看日志
#   clean   - 清理所有容器和数据卷（危险操作）

set -e  # 遇到错误立即退出

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

echo_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

echo_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

echo_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# 检查 Docker 和 Docker Compose
check_docker() {
    echo_info "检查 Docker 环境..."
    
    if ! command -v docker &> /dev/null; then
        echo_error "Docker 未安装，请先安装 Docker"
        echo_info "安装指南: https://docs.docker.com/get-docker/"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        echo_error "Docker Compose 未安装，请先安装 Docker Compose"
        echo_info "安装指南: https://docs.docker.com/compose/install/"
        exit 1
    fi
    
    echo_info "Docker 环境检查完成"
}

# 检查环境变量文件
check_env() {
    if [ ! -f .env ]; then
        echo_warn ".env 文件不存在，正在创建..."
        cp .env.example .env
        echo_info "已创建 .env 文件，请根据需要修改配置"
        echo_warn "特别是 AI_APIKEY 需要配置为你的实际 API Key"
    fi
}

# 创建必要的目录
create_dirs() {
    echo_info "创建必要的目录..."
    mkdir -p docker/nginx
    mkdir -p docker/mysql
    echo_info "目录创建完成"
}

# 启动服务
start_services() {
    echo_step "启动 IM 服务..."
    
    # 使用 docker compose 或 docker-compose
    if docker compose version &> /dev/null; then
        docker compose up -d
    else
        docker-compose up -d
    fi
    
    echo_info "服务启动中..."
    echo_info "等待服务健康检查..."
    sleep 10
    
    # 检查服务状态
    if docker compose version &> /dev/null; then
        docker compose ps
    else
        docker-compose ps
    fi
    
    echo ""
    echo_info "========================================"
    echo_info "服务启动完成！"
    echo_info "========================================"
    echo_info "前端访问地址: http://localhost"
    echo_info "后端 API 地址: http://localhost:8080"
    echo_info "MySQL 端口: 3306"
    echo_info "Redis 端口: 6379"
    echo_info "Elasticsearch 端口: 9200"
    echo_info "========================================"
    echo_info "常用命令:"
    echo_info "  查看日志: bash docker-deploy.sh logs"
    echo_info "  停止服务: bash docker-deploy.sh stop"
    echo_info "  重启服务: bash docker-deploy.sh restart"
    echo_info "========================================"
}

# 停止服务
stop_services() {
    echo_step "停止 IM 服务..."
    
    if docker compose version &> /dev/null; then
        docker compose down
    else
        docker-compose down
    fi
    
    echo_info "服务已停止"
}

# 重启服务
restart_services() {
    echo_step "重启 IM 服务..."
    stop_services
    sleep 2
    start_services
}

# 重新构建镜像
build_services() {
    echo_step "重新构建镜像..."
    
    if docker compose version &> /dev/null; then
        docker compose build --no-cache
    else
        docker-compose build --no-cache
    fi
    
    echo_info "镜像构建完成"
    echo_warn "请运行 'bash docker-deploy.sh restart' 重启服务以使用新镜像"
}

# 查看日志
view_logs() {
    echo_step "查看服务日志..."
    
    if [ -z "$2" ]; then
        # 显示所有服务的日志
        if docker compose version &> /dev/null; then
            docker compose logs -f
        else
            docker-compose logs -f
        fi
    else
        # 显示特定服务的日志
        if docker compose version &> /dev/null; then
            docker compose logs -f "$2"
        else
            docker-compose logs -f "$2"
        fi
    fi
}

# 清理所有数据
clean_all() {
    echo_warn "========================================"
    echo_warn "警告：此操作将删除所有容器和数据卷！"
    echo_warn "所有数据库数据、Redis 数据、日志等将被永久删除！"
    echo_warn "========================================"
    read -p "确定要继续吗？(输入 'yes' 确认): " confirm
    
    if [ "$confirm" != "yes" ]; then
        echo_info "操作已取消"
        exit 0
    fi
    
    echo_step "清理所有容器和数据..."
    
    if docker compose version &> /dev/null; then
        docker compose down -v
    else
        docker-compose down -v
    fi
    
    # 删除镜像
    echo_step "删除镜像..."
    docker rmi im-backend im-frontend 2>/dev/null || true
    
    echo_info "清理完成"
}

# 显示帮助信息
show_help() {
    echo "IM 即时通讯系统 - Docker 部署脚本"
    echo ""
    echo "使用方法: bash docker-deploy.sh [选项]"
    echo ""
    echo "选项:"
    echo "  start       启动所有服务（默认）"
    echo "  stop        停止所有服务"
    echo "  restart     重启所有服务"
    echo "  build       重新构建镜像"
    echo "  logs        查看日志（可指定服务名，如: logs backend）"
    echo "  clean       清理所有容器和数据卷（危险操作）"
    echo "  help        显示帮助信息"
    echo ""
    echo "示例:"
    echo "  bash docker-deploy.sh start"
    echo "  bash docker-deploy.sh logs backend"
    echo "  bash docker-deploy.sh build"
}

# 主流程
main() {
    # 检查 Docker 环境
    check_docker
    
    # 获取命令
    COMMAND=${1:-start}
    
    case "$COMMAND" in
        start)
            check_env
            create_dirs
            start_services
            ;;
        stop)
            stop_services
            ;;
        restart)
            restart_services
            ;;
        build)
            build_services
            ;;
        logs)
            view_logs "$@"
            ;;
        clean)
            clean_all
            ;;
        help)
            show_help
            ;;
        *)
            echo_error "未知命令: $COMMAND"
            show_help
            exit 1
            ;;
    esac
}

# 运行主流程
main "$@"


