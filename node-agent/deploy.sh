#!/bin/bash
# ==========================================================
# CAE 任务分发平台 - 计算节点（Linux 环境）快速部署脚本
# 适用场景: Ubuntu 虚拟机、实体服务器
# ==========================================================

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}=== 1. 检查 node-agent jar 包 ===${NC}"
# 为了防呆互通，支持把 node-agent.jar 直接放同级，也支持放 target
if [ ! -d "target" ]; then
    mkdir -p target
fi

# 检查当前目录下有没有抛过来的 jar
if ls node-agent*.jar 1> /dev/null 2>&1; then
    cp node-agent*.jar target/node-agent-1.0.0.jar
fi

# 校验 target 下是否包含编译好的 jar
if ! ls target/*.jar 1> /dev/null 2>&1; then
    echo -e "${RED}[错误] 未找到编译好的 jar 包!${NC}"
    echo "请先在 Windows 的 Backend 项目根目录下执行: "
    echo "  mvn clean package -DskipTests"
    echo "然后将 node-agent/target/node-agent-1.0.0.jar 拖进 Ubuntu 这个脚本旁边！"
    exit 1
fi

echo -e "${GREEN}=== 2. 开始构建包含 Java + OpenFOAM 的计算节点镜像 ===${NC}"
docker build -t cae-node-agent:latest .

echo -e "${GREEN}=== 3. 询问配置信息 ===${NC}"
# 默认启动1个节点
NODE_COUNT=${1:-1}

# 你的 Windows 本机 IP (如果没传第2个参数，则提示用户输入)
WINDOWS_IP=${2:-""}

if [ -z "$WINDOWS_IP" ]; then
    echo -e "${RED}[必填] 请输入你 Windows 电脑的局域网 IPv4 地址 (例如 192.168.1.100): ${NC}"
    read WINDOWS_IP
fi

# 确保输入不为空
if [ -z "$WINDOWS_IP" ]; then
    echo "Windows IP 不能为空，部署终止！"
    exit 1
fi

# 自动获取 Ubuntu 的本机 IP，发给 Windows 的调度中心，让它能找回来！
UBUNTU_IP=$(hostname -I | awk '{print $1}')

# === 新增：Windows 共享文件夹盘符映射 ===
# 为了实现零拷贝读取文件，我们需要知道你的 Windows 平台上的文件是存在哪的
WINDOWS_TASK_ROOT=${3:-"D:\\Project\\CAE_TaskManager_Platform\\Backend\\data\\tasks"}

echo "-> 调度中心地址将被配置为: http://$WINDOWS_IP:8084"
echo "-> 计算节点的对外观测 IP (Ubuntu_IP): $UBUNTU_IP"
echo "-> 跨系统路径映射: $WINDOWS_TASK_ROOT <==> /cae-data/workspaces"

# 挂载的硬盘路径 (Ubuntu 本地存储任务文件的路径，用于接收传输)
WORK_DIR="/opt/cae/data/workspaces"
mkdir -p $WORK_DIR
chmod -R 777 $WORK_DIR

# 开始基于镜像拉起多个实例 (单机多节点)
for i in $(seq 1 $NODE_COUNT); do
    NODE_NAME="cae-node-$i"
    echo "-> 正在启动计算节点: $NODE_NAME"
    
    # 强制清理重名的旧节点
    docker stop $NODE_NAME >/dev/null 2>&1
    docker rm $NODE_NAME >/dev/null 2>&1
    
    # 核心：运行容器
    # 注入 SCHEDULER_SERVICE_BASE_URL，让 node-agent 知道去哪找 Windows 上的调度中心！
    docker run -d \
        --name $NODE_NAME \
        --network=host \
        -e SPRING_PROFILES_ACTIVE=prod \
        -e SERVER_PORT=808$i \
        -e CAE_NODE_NODE_PORT=808$i \
        -e CAE_NODE_NODE_NAME="ubuntu-node-$i" \
        -e CAE_NODE_NODE_CODE="UBUNTU_$i" \
        -e CAE_NODE_ADVERTISED_HOST="$UBUNTU_IP:808$i" \
        -e SCHEDULER_SERVICE_BASE_URL="http://$WINDOWS_IP:8084" \
        -e CAE_NODE_TASK_BASE_URL="http://$WINDOWS_IP:8083" \
        -e CAE_NODE_PATH_MAPPING_WINDOWS="$WINDOWS_TASK_ROOT" \
        -e CAE_NODE_PATH_MAPPING_LINUX="/cae-data/workspaces" \
        -e CAE_NODE_WORK_ROOT="/cae-data/workspaces" \
        -v $WORK_DIR:/cae-data/workspaces \
        cae-node-agent:latest
done

echo -e "${GREEN}=== 部署完成！ ===${NC}"
echo "--------------------------------------------------------"
echo "✅ 容器和 OpenFOAM 已融合，随时待命！"
echo "🛠️ 日常管理命令："
echo "查看运行着几个节点  : docker ps"
echo "查看节点1的实时日志 : docker logs -f cae-node-1"
echo "登入节点1内部环境   : docker exec -it cae-node-1 /bin/bash"
echo "--------------------------------------------------------"