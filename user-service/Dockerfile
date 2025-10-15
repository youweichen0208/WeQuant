FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# 复制Maven配置和源代码
COPY pom.xml .
COPY src ./src

# 下载依赖并构建
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests && \
    mv target/*.jar app.jar

# 运行时镜像
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 添加非root用户
RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser

# 复制构建产物
COPY --from=builder /app/app.jar .

# 修改所有权
RUN chown -R appuser:appuser /app

# 切换到非root用户
USER appuser

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# 暴露端口
EXPOSE 8081 20881

# 启动应用
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:+UnlockExperimentalVMOptions", \
    "-XX:+UseCGroupMemoryLimitForHeap", \
    "-jar", \
    "app.jar"]
