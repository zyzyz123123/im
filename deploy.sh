#!/bin/bash

# IM 即时通讯系统 - 自动化部署脚本
# 使用方法: bash deploy.sh

set -e  # 遇到错误立即退出

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 配置变量（请根据实际情况修改）
SERVER_IP=120.48.139.174          # 服务器 IP
SERVER_USER="root"                       # SSH 用户名
DEPLOY_PATH="/opt/im"                    # 部署目录
NGINX_CONFIG_PATH="/etc/nginx/conf.d"   # Nginx 配置目录
FRONTEND_PATH="/var/www/im"              # 前端静态文件目录

echo_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

echo_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

echo_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查必要的工具
check_tools() {
    echo_info "检查必要的工具..."
    
    if ! command -v mvn &> /dev/null; then
        echo_error "Maven 未安装，请先安装 Maven"
        exit 1
    fi
    
    if ! command -v npm &> /dev/null; then
        echo_error "Node.js/npm 未安装，请先安装 Node.js"
        exit 1
    fi
    
    echo_info "工具检查完成"
}

# 构建后端
build_backend() {
    echo_info "开始构建后端..."
    cd backend
    
    # Maven 打包
    mvn clean package -DskipTests
    
    if [ $? -eq 0 ]; then
        echo_info "后端构建成功: backend/target/im-0.0.1-SNAPSHOT.jar"
    else
        echo_error "后端构建失败"
        exit 1
    fi
    
    cd ..
}

# 构建前端
build_frontend() {
    echo_info "开始构建前端..."
    cd frontend
    
    # 检查是否存在环境配置文件
    if [ ! -f ".env.production" ]; then
        echo_warn ".env.production 文件不存在，使用默认配置"
        echo "VITE_API_BASE_URL=http://${SERVER_IP}" > .env.production
        echo "VITE_WS_BASE_URL=ws://${SERVER_IP}" >> .env.production
    fi
    
    # 安装依赖
    npm install
    
    # 构建
    npm run build
    
    if [ $? -eq 0 ]; then
        echo_info "前端构建成功: frontend/dist/"
    else
        echo_error "前端构建失败"
        exit 1
    fi
    
    cd ..
}

# 部署到服务器
deploy_to_server() {
    echo_info "开始部署到服务器..."
    
    # 创建远程目录
    ssh ${SERVER_USER}@${SERVER_IP} "mkdir -p ${DEPLOY_PATH}/{backend,frontend} ${FRONTEND_PATH}/frontend"
    
    # 上传后端 jar 包
    echo_info "上传后端文件..."
    scp backend/target/im-0.0.1-SNAPSHOT.jar ${SERVER_USER}@${SERVER_IP}:${DEPLOY_PATH}/backend/
    scp backend/src/main/resources/application-prod.properties ${SERVER_USER}@${SERVER_IP}:${DEPLOY_PATH}/backend/
    
    # 上传前端静态文件
    echo_info "上传前端文件..."
    scp -r frontend/dist/* ${SERVER_USER}@${SERVER_IP}:${FRONTEND_PATH}/frontend/
    
    # 上传配置文件
    echo_info "上传配置文件..."
    scp nginx.conf ${SERVER_USER}@${SERVER_IP}:${DEPLOY_PATH}/
    scp im-backend.service ${SERVER_USER}@${SERVER_IP}:${DEPLOY_PATH}/
    
    echo_info "文件上传完成"
}

# 远程配置服务器
configure_server() {
    echo_info "配置服务器..."
    
    ssh ${SERVER_USER}@${SERVER_IP} << 'ENDSSH'
        # 配置 Nginx
        if [ -f /opt/im/nginx.conf ]; then
            sudo cp /opt/im/nginx.conf /etc/nginx/conf.d/im.conf
            sudo nginx -t && sudo systemctl reload nginx
            echo "Nginx 配置已更新"
        fi
        
        # 配置 systemd 服务
        if [ -f /opt/im/im-backend.service ]; then
            sudo cp /opt/im/im-backend.service /etc/systemd/system/
            sudo systemctl daemon-reload
            echo "systemd 服务配置已更新"
        fi
        
        # 创建日志目录
        sudo mkdir -p /var/log/im
        sudo chown -R $(whoami):$(whoami) /var/log/im
        
        echo "服务器配置完成"
ENDSSH
    
    echo_info "服务器配置完成"
}

# 重启服务
restart_services() {
    echo_info "重启后端服务..."
    
    ssh ${SERVER_USER}@${SERVER_IP} << 'ENDSSH'
        # 重启后端服务
        sudo systemctl restart im-backend
        
        # 等待服务启动
        sleep 5
        
        # 检查服务状态
        if sudo systemctl is-active --quiet im-backend; then
            echo "后端服务启动成功"
            sudo systemctl status im-backend --no-pager
        else
            echo "后端服务启动失败，请检查日志"
            sudo journalctl -u im-backend -n 50 --no-pager
            exit 1
        fi
ENDSSH
    
    if [ $? -eq 0 ]; then
        echo_info "服务重启成功"
    else
        echo_error "服务重启失败"
        exit 1
    fi
}

# 主流程
main() {
    echo_info "========================================"
    echo_info "IM 即时通讯系统 - 自动化部署"
    echo_info "========================================"
    echo ""
    
    # # 检查工具
    # check_tools
    
    # 构建
    echo_info "步骤 1/5: 构建后端"
    build_backend
    
    echo_info "步骤 2/5: 构建前端"
    build_frontend
    
    # 部署
    echo_info "步骤 3/5: 部署到服务器"
    deploy_to_server
    
    # echo_info "步骤 4/5: 配置服务器"
    # configure_server
    
    echo_info "步骤 5/5: 重启服务"
    restart_services
    
    echo ""
    echo_info "========================================"
    echo_info "部署完成！"
    echo_info "========================================"
    echo_info "访问地址: http://${SERVER_IP}"
    echo_info ""
    echo_info "常用命令:"
    echo_info "  查看后端日志: ssh ${SERVER_USER}@${SERVER_IP} 'sudo journalctl -u im-backend -f'"
    echo_info "  重启后端服务: ssh ${SERVER_USER}@${SERVER_IP} 'sudo systemctl restart im-backend'"
    echo_info "  查看服务状态: ssh ${SERVER_USER}@${SERVER_IP} 'sudo systemctl status im-backend'"
    echo_info "========================================"
}

# 运行主流程
main

