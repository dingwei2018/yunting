# 使用 OpenJDK 21 作为基础镜像
FROM eclipse-temurin:21-jre-jammy

# 设置工作目录
WORKDIR /app

# 创建日志目录
RUN mkdir -p /app/logs

# 复制 jar 文件到容器
COPY target/backend-1.0.0.jar app.jar

# 设置 JVM 参数和时区
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 暴露端口
EXPOSE 8080

# 安装 curl 用于健康检查
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

