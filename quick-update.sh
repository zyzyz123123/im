#!/bin/bash
set -e

# 快速更新到服务器脚本
# 使用方法: ./quick-update.sh

# ========================================
# 配置区域 - 请修改为你的服务器信息
# ========================================
SERVER="root@120.48.139.174"  # 修改为你的服务器地址
REMOTE_PATH="/opt"          # 服务器项目路径

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;36m'
NC='\033[0m'

echo "========================================="
echo "  快速更新到服务器"
echo "========================================="
echo ""
echo "服务器: $SERVER"
echo "路径: $REMOTE_PATH"
echo ""

# 检查配置
if [ "$SERVER" = "root@your-server-ip" ]; then
    echo -e "${YELLOW}⚠️  请先修改脚本中的 SERVER 配置${NC}"
    echo "编辑: vi quick-update.sh"
    echo "修改: SERVER=\"root@你的服务器IP\""
    exit 1
fi

# 1. 本地构建
echo -e "${BLUE}📦 步骤 1/3: 本地构建...${NC}"
./deploy.sh local-build

if [ ! -f "deploy.tar.gz" ]; then
    echo -e "${RED}❌ 构建失败：未找到 deploy.tar.gz${NC}"
    exit 1
fi

echo -e "${GREEN}✅ 构建完成${NC}"
echo ""

# 2. 上传到服务器
echo -e "${BLUE}📤 步骤 2/3: 上传到服务器...${NC}"
scp deploy.tar.gz $SERVER:/tmp/

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 上传失败${NC}"
    exit 1
fi

echo -e "${GREEN}✅ 上传完成${NC}"
echo ""

# 3. 远程部署
echo -e "${BLUE}🚀 步骤 3/3: 服务器部署...${NC}"
ssh $SERVER << ENDSSH
set -e

cd $REMOTE_PATH

echo "备份当前版本..."
if [ -f "docker-compose.yml" ]; then
    tar czf ../im-backup-\$(date +%Y%m%d-%H%M%S).tar.gz . 2>/dev/null || true
else
    echo "首次部署，跳过备份"
fi

echo "解压新版本..."
tar xzf /tmp/deploy.tar.gz --strip-components=1

echo "确保脚本可执行..."
chmod +x deploy.sh || echo "警告：deploy.sh 不存在，请检查"

echo "检查文件..."
ls -la deploy.sh docker-compose.yml || echo "警告：关键文件缺失"

echo "更新部署..."
./deploy.sh update

echo ""
echo "查看服务状态..."
docker compose ps

echo ""
echo "检查后端启动..."
docker compose logs --tail=20 backend | grep -i "started\|error" || true

ENDSSH

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================="
    echo -e "  ${GREEN}✅ 部署成功！${NC}"
    echo "========================================="
    echo ""
    echo "常用命令:"
    echo "  查看日志: ssh $SERVER 'cd $REMOTE_PATH && docker compose logs -f backend'"
    echo "  查看状态: ssh $SERVER 'cd $REMOTE_PATH && docker compose ps'"
    echo "  重启服务: ssh $SERVER 'cd $REMOTE_PATH && docker compose restart backend'"
    echo ""
else
    echo -e "${RED}❌ 部署失败${NC}"
    echo "查看详细日志: ssh $SERVER 'cd $REMOTE_PATH && docker compose logs'"
    exit 1
fi
