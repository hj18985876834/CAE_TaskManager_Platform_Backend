# 使用 OpenFOAM 官方镜像作为基础 (基于 Ubuntu)
# 这样容器内天然就包含了完整的 OpenFOAM 仿真环境
FROM opencfd/openfoam-default:2312

# 切换为 root 用户以安装 Java 等环境
USER root

# 更新 APT 并安装 JDK 17 和必要的网络测试工具
# 清理 apt 缓存以减小镜像体积
RUN apt-get update && \
    apt-get install -y openjdk-17-jre hwloc && \
    rm -rf /var/lib/apt/lists/*

# 设置时区为上海
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建工作目录和数据存储目录
WORKDIR /app
RUN mkdir -p /cae-data/workspaces /cae-data/logs && chmod -R 777 /cae-data

# 复制打包好的 node-agent jar 包到镜像内
# 注意：构建镜像前，需确保 jar 包已经在 target 目录下或与 Dockerfile 同级
COPY target/*.jar /app/node-agent.jar

# 关键核心：启动命令
# 必须清空 OpenFOAM 官方镜像自带的 ENTRYPOINT（它自带的 /openfoam/run 脚本会导致引号解析错误）
ENTRYPOINT []

# 必须先 source OpenFOAM 的环境变量，然后启动 java
CMD ["/bin/bash", "-c", "source /usr/lib/openfoam/openfoam2312/etc/bashrc && exec java -jar /app/node-agent.jar"]
