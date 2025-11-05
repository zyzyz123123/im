# 统一 Dockerfile - 支持两种构建模式
# 使用方式:
#   服务器构建: docker build --build-arg BUILD_MODE=server .
#   本地打包:   docker build --build-arg BUILD_MODE=local .

ARG BUILD_MODE=server

# ===== 服务器构建模式 =====
FROM node:20-slim AS build-server

WORKDIR /app

ARG VITE_API_BASE_URL=
ARG VITE_WS_BASE_URL=
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_WS_BASE_URL=$VITE_WS_BASE_URL

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ .
RUN npm run build

# ===== 本地打包模式 =====
FROM node:20-slim AS build-local

WORKDIR /app

# 从本地复制预编译的 dist
COPY frontend/dist ./dist

# ===== 选择构建阶段 =====
FROM build-${BUILD_MODE} AS build-stage

# ===== 生产阶段 =====
FROM nginx:alpine

# 从构建阶段复制 dist
COPY --from=build-stage /app/dist /usr/share/nginx/html

# 复制 nginx 配置文件
COPY docker/nginx/default.conf /etc/nginx/conf.d/default.conf

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]
