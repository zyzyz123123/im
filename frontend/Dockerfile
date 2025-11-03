# 多阶段构建 - 构建阶段
FROM node:20-slim AS build

WORKDIR /app

# 复制 package.json 和 package-lock.json
COPY frontend/package*.json ./

# 安装依赖
RUN npm ci

# 复制源代码
COPY frontend/ .

# 构建应用
ARG VITE_API_BASE_URL=http://localhost
ARG VITE_WS_BASE_URL=ws://localhost
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_WS_BASE_URL=$VITE_WS_BASE_URL

RUN npm run build

# 生产阶段 - 使用 Nginx
FROM nginx:alpine

# 复制构建好的前端文件
COPY --from=build /app/dist /usr/share/nginx/html

# 复制 nginx 配置文件
COPY docker/nginx/default.conf /etc/nginx/conf.d/default.conf

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]

