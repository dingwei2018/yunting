# 云听后端服务

基于 Spring Boot 和 JDK 21 的后端服务项目。

## 技术栈

- **JDK**: 21
- **构建工具**: Maven
- **框架**: Spring Boot 3.3.0
- **数据库**: MySQL 8.0.36
- **容器化**: Docker

## 快速开始

### 本地开发

1. 确保已安装 JDK 21 和 Maven
2. 启动 MySQL（本地或使用容器，保持与 `docker-compose.yml` 中一致的账号）
3. 运行应用：
```bash
mvn spring-boot:run
```

4. 访问健康检查接口：
```
http://localhost:8080/api/health
```

### Docker 运行

#### 使用 Docker Compose（推荐）

```bash
docker-compose up -d
```

默认会同时启动：

- `mysql`：MySQL 8.0.36，端口映射 `3306:3306`
- `backend`：Spring Boot 服务，端口映射 `8080:8080`

#### 使用 Docker 命令

1. 构建镜像：
```bash
docker build -t yunting-backend:latest .
```

2. 运行容器：
```bash
docker run -d -p 8080:8080 --name yunting-backend yunting-backend:latest
```

## 项目结构

```
.
├── src/
│   ├── main/
│   │   ├── java/com/yunting/
│   │   │   ├── BackendApplication.java
│   │   │   └── controller/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
├── .gitignore
└── pom.xml
```

## API 接口

- `GET /api/health` - 健康检查接口

## 注意事项

- 确保 Docker 环境支持 JDK 21
- JDK 21 是 LTS 版本，镜像稳定可用
- MySQL 默认账号
  - 用户名：`yunting`
  - 密码：`yunting`
  - 数据库：`yunting`
  - Root 密码：`secret`
- 可以通过环境变量 `SPRING_DATASOURCE_*` 覆盖默认的数据库配置

