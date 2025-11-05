# 统一 Dockerfile - 支持两种构建模式
# 使用方式:
#   服务器构建: docker build --build-arg BUILD_MODE=server .
#   本地打包:   docker build --build-arg BUILD_MODE=local .

ARG BUILD_MODE=server

# ===== 服务器构建模式 =====
FROM maven:3.9-eclipse-temurin-25 AS build-server

WORKDIR /app

COPY pom.xml ./
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests && \
    cp target/im-*.jar app.jar

# ===== 本地打包模式 =====
FROM eclipse-temurin:25-jre-alpine AS build-local

WORKDIR /app

# 从本地复制预编译的 jar
COPY app.jar app.jar

# ===== 选择构建阶段 =====
FROM build-${BUILD_MODE} AS build-stage

# ===== 运行阶段 =====
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# 创建非 root 用户
RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser && \
    mkdir -p /var/log/im && \
    chown -R appuser:appuser /app /var/log/im

# 从构建阶段复制 jar
COPY --from=build-stage /app/app.jar app.jar

# 切换到非 root 用户
USER appuser

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/ || exit 1

# 启动应用
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}", \
    "-jar", \
    "app.jar"]
