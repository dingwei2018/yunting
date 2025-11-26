# 云听后端服务

基于 Spring Boot 和 JDK 21 的后端服务项目。

## 技术栈

- **JDK**: 21
- **构建工具**: Maven
- **框架**: Spring Boot 3.3.0
- **数据库**: MySQL 8.0.36
- **对象存储**: Huawei OBS SDK（esdk-obs-java-bundle 3.23.9）
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
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── exception/
│   │   │   ├── service/
│   │   │   └── util/
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── logback-spring.xml
│   │       └── db/
│   │           ├── schema.sql       # 建表脚本
│   │           └── init-data.sql    # 基础数据
│   └── test/
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
├── .gitignore
└── pom.xml
```

## API 接口

- `GET /api/health` - 健康检查
- `POST /api/tasks/createTask?content=...` - 创建任务并自动拆句
- `GET /api/tasks/getTaskDetail?taskid={task_id}` - 获取任务详情
- `GET /api/tasks/listTasks?page=1&page_size=20&status=2` - 分页查询任务列表
- `GET /api/tasks/breaking-sentences?taskid={task_id}` - 获取断句列表
- `GET /api/breaking-sentences/info?breaking_sentence_id={id}` - 获取断句详情
- `DELETE /api/breaking-sentences?breaking_sentence_id={id}` - 删除断句
- `POST /api/breaking-sentences/synthesize?breaking_sentence_id={id}` - 合成单个断句
- `POST /api/breaking-sentences/resynthesize?breaking_sentence_id={id}` - 重新合成断句
- `POST /api/tasks/synthesize?taskid={task_id}` - 批量合成任务内断句
- `GET /api/synthesis/tasks?taskid={task_id}` - 查询任务合成状态

## 注意事项

- 确保 Docker 环境支持 JDK 21
- JDK 21 是 LTS 版本，镜像稳定可用
- MySQL 默认账号
  - 用户名：`yunting`
  - 密码：`yunting`
  - 数据库：`yunting`
  - Root 密码：`secret`
- 可以通过环境变量 `SPRING_DATASOURCE_*` 覆盖默认的数据库配置
- `src/main/resources/db/schema.sql` 提供全量建表脚本，`init-data.sql` 提供基础音色数据
- Huawei OBS 相关配置通过 `huaweicloud.obs.*` 或环境变量（如 `HUAWEICLOUD_OBS_ENDPOINT`、`HUAWEICLOUD_OBS_BUCKET`）注入，未配置时不会初始化 `ObsClient`

