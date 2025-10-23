# 💬 IM 即时通讯系统

一个基于 Spring Boot + Vue 3 的现代化即时通讯系统。

## ✨ 主要功能

- 💬 实时聊天（WebSocket）
- 🔐 用户登录/注册（JWT + BCrypt）
- 🟢 在线状态显示
- 📱 最近联系人列表
- ✅ 消息已读/未读
- 💾 消息持久化

## 🛠️ 技术栈

**后端：** Spring Boot、WebSocket、MyBatis、MySQL、JWT

**前端：** Vue 3、Element Plus、Pinia、Vite

## 🚀 快速开始

### 1. 数据库配置

创建数据库并执行建表语句：

```sql
CREATE DATABASE im CHARACTER SET utf8mb4;

-- 用户表
CREATE TABLE t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    email VARCHAR(100),
    status TINYINT DEFAULT 1,
    created_at DATETIME,
    updated_at DATETIME
);

-- 消息表
CREATE TABLE t_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(50) UNIQUE NOT NULL,
    from_user_id VARCHAR(50) NOT NULL,
    to_user_id VARCHAR(50) NOT NULL,
    content TEXT,
    message_type TINYINT DEFAULT 1,
    status TINYINT DEFAULT 0,
    created_at DATETIME
);
```

### 2. 后端配置

修改 `backend/src/main/resources/application.properties`：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/im
spring.datasource.username=root
spring.datasource.password=your_password
```

### 3. 启动项目

**后端：**
```bash
cd backend
mvn spring-boot:run
```

**前端：**
```bash
cd frontend
npm install
npm run dev
```

访问：http://localhost:5173

## 📸 功能展示

- 用户注册/登录
- 实时发送/接收消息
- 查看在线用户
- 查看聊天历史
- 消息已读提示
- 离线消息（用户上线后接收）

## 🎯 技术亮点

- ✅ 前后端分离
- ✅ WebSocket 实时通信
- ✅ JWT 无状态认证
- ✅ 断线自动重连
- ✅ 消息持久化
- ✅ 现代化 UI 设计

## 📝 License

MIT
