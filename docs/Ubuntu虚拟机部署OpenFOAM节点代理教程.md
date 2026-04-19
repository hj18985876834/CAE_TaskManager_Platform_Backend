# Ubuntu虚拟机部署 OpenFOAM 节点代理教程

## 1. 适用场景

本文档用于指导你按照当前项目的实际设计，在 Ubuntu 虚拟机中部署 `node-agent` 容器，并把它作为 OpenFOAM 计算节点接入现有平台，完成任务的全流程演示。

本文档默认采用如下拓扑：

- Windows 主机运行中心端服务
  - `gateway-service`
  - `user-service`
  - `solver-service`
  - `task-service`
  - `scheduler-service`
- Ubuntu 虚拟机运行 OpenFOAM 节点代理容器
  - `node-agent`
- OpenFOAM 求解实际在 Ubuntu 容器内部执行

## 2. 先明确当前项目的真实设计

在开始部署前，必须先理解当前项目的几个关键设计点。

### 2.1 节点代理不是纯网络执行器

当前实现下，`node-agent` 在收到调度任务后，会根据 `task-service` 下发的 `storagePath` 去读取输入文件，然后拷贝到本地工作目录，再解压 ZIP 包并执行命令。

这意味着：

- Ubuntu 节点必须能够访问到 `task-service` 存储任务文件的目录
- 不能只让节点和调度器网络互通，而文件目录完全不共享

换句话说，当前项目依赖：

- `task-service` 的任务目录
- Ubuntu 节点看到的 Linux 目录

这两者之间建立“同一份文件”的映射关系。

### 2.2 调度器派发任务时是回调节点注册的 host

节点注册时会上报自己的 `host`，调度器后续派发任务时，会直接访问：

- `http://<node.host>/internal/dispatch-task`

因此：

- `NODE_ADVERTISED_HOST` 必须填成 Windows 主机实际能够访问到的 Ubuntu IP:端口
- 不能填容器内部地址
- 不能填只在 Ubuntu 本机可见但 Windows 不可达的地址

### 2.3 OpenFOAM 已经通过节点镜像内置

项目里已经有专门的 OpenFOAM 节点镜像 Dockerfile：

- [node-agent/Dockerfile](/d:/Project/CAE_TaskManager_Platform/Backend/node-agent/Dockerfile)

它的设计是：

- 基于 `opencfd/openfoam-default:2312`
- 再安装 Java 17
- 启动时自动 `source` OpenFOAM 环境
- 然后运行 `node-agent.jar`

所以：

- Ubuntu 宿主机本身不必再单独安装 OpenFOAM
- 只要节点镜像能成功构建，容器内就已经具备 `simpleFoam`

### 2.4 OpenFOAM 当前默认绑定的是 solverId=1

当前 `node-agent` 默认配置中：

- `solver-ids: [1]`
- `solver-versions["1"] = "v10"`

对应初始化 SQL 里的 OpenFOAM 定义。

也就是说，在你没有额外修改配置时，这个节点会向平台注册自己支持 `solverId=1` 的 OpenFOAM。

## 3. 推荐的网络与目录方案

为了保证演示最稳定，推荐按下面方式准备环境。

### 3.1 虚拟机网络模式

推荐使用：

- `桥接网络`

这样可以让：

- Windows 主机直接访问 Ubuntu 虚拟机 IP
- Ubuntu 虚拟机直接访问 Windows 主机 IP

不推荐一开始就用复杂 NAT 转发方案，因为：

- 调度器需要主动回调 Ubuntu 节点
- 节点也需要主动访问 Windows 的 `task-service` 和 `scheduler-service`

### 3.2 任务目录共享方案

推荐优先使用以下任一方案：

1. Windows 目录通过 SMB 共享给 Ubuntu 挂载
2. 虚拟机共享文件夹功能挂载 Windows 目录

最关键的目标是：

- Windows 的任务目录，例如 `D:\Project\CAE_TaskManager_Platform\Backend\data\tasks`
- 在 Ubuntu 中被挂载成一个固定 Linux 路径，例如 `/opt/cae/data/workspaces`

然后容器再把这个 Linux 路径挂载进容器内部：

- `/cae-data/workspaces`

## 4. 部署前准备

### 4.1 Windows 侧准备

确保 Windows 主机已经正常启动以下服务：

- `task-service`
- `scheduler-service`

建议同时启动整个后端，至少包括：

- `gateway-service`
- `user-service`
- `solver-service`
- `task-service`
- `scheduler-service`

并确认以下端口可访问：

- `8083` 对应 `task-service`
- `8084` 对应 `scheduler-service`

还要确认 Windows 防火墙没有拦住这两个端口。

### 4.2 Ubuntu 侧准备

在 Ubuntu 虚拟机中安装 Docker 和共享目录工具：

```bash
sudo apt-get update
sudo apt-get install -y docker.io cifs-utils
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker $USER
```

执行完后建议重新登录一次 Ubuntu 终端。

然后确认 Docker 可用：

```bash
docker --version
docker ps
```

### 4.3 获取两个关键 IP

你需要记住两个地址：

- `WINDOWS_IP`
  - Windows 主机在局域网中的 IPv4 地址
- `UBUNTU_IP`
  - Ubuntu 虚拟机在局域网中的 IPv4 地址

Ubuntu 查看 IP：

```bash
hostname -I
```

Windows 查看 IP：

```powershell
ipconfig
```

## 5. 第一步：在 Ubuntu 挂载 Windows 任务目录

这是最关键的一步。

假设 Windows 任务目录是：

```text
D:\Project\CAE_TaskManager_Platform\Backend\data\tasks
```

### 5.1 Windows 共享任务目录

在 Windows 中把该目录共享出来，例如共享名叫：

```text
cae_tasks
```

假设你的 Windows 用户名为：

```text
your_windows_user
```

### 5.2 Ubuntu 挂载共享目录

在 Ubuntu 中执行：

```bash
sudo mkdir -p /opt/cae/data/workspaces
sudo mount -t cifs //WINDOWS_IP/cae_tasks /opt/cae/data/workspaces \
  -o username=your_windows_user,password=your_windows_password,uid=$(id -u),gid=$(id -g),file_mode=0777,dir_mode=0777,iocharset=utf8
```

把其中的：

- `WINDOWS_IP`
- `your_windows_user`
- `your_windows_password`

换成你自己的实际值。

挂载成功后执行：

```bash
ls /opt/cae/data/workspaces
```

如果你能看到 Windows 任务目录下的内容，就说明共享成功。

### 5.3 如果你使用 VMware/VirtualBox 共享文件夹

如果你已经通过虚拟机共享文件夹把 Windows 目录挂进 Ubuntu，也可以不用 SMB。

但你必须保证最终满足下面关系：

- Ubuntu 中有一个实际可访问的 Linux 路径
- 该路径内容就是 Windows 的任务目录内容

例如：

```text
/opt/cae/data/workspaces
```

如果不是这个路径，也没关系，只要后面启动容器时统一替换即可。

## 6. 第二步：准备 node-agent 镜像构建文件

### 6.1 在 Windows 先构建 node-agent Jar

在项目根目录执行：

```powershell
mvn -pl node-agent -am package -DskipTests
```

构建完成后，生成的 Jar 一般在：

- [node-agent/target](/d:/Project/CAE_TaskManager_Platform/Backend/node-agent/target)

### 6.2 把以下文件复制到 Ubuntu

建议在 Ubuntu 中创建一个目录：

```bash
mkdir -p ~/cae-node-agent/target
```

然后把下面文件从 Windows 复制到 Ubuntu：

- [node-agent/Dockerfile](/d:/Project/CAE_TaskManager_Platform/Backend/node-agent/Dockerfile)
- [node-agent/deploy.sh](/d:/Project/CAE_TaskManager_Platform/Backend/node-agent/deploy.sh)
- `node-agent/target` 下生成的 Jar

最终 Ubuntu 目录建议长这样：

```text
~/cae-node-agent/
  Dockerfile
  deploy.sh
  target/
    node-agent-xxx.jar
```

### 6.3 在 Ubuntu 构建节点镜像

进入目录后执行：

```bash
cd ~/cae-node-agent
docker build -t cae-node-agent:latest .
```

构建完成后查看：

```bash
docker images | grep cae-node-agent
```

## 7. 第三步：手工启动 OpenFOAM 节点容器

推荐优先使用手工 `docker run`，因为更容易理解和排查问题。

### 7.1 启动前确认参数

假设：

- Windows 主机 IP：`192.168.1.100`
- Ubuntu 虚拟机 IP：`192.168.1.50`
- Windows 任务目录：`D:\Project\CAE_TaskManager_Platform\Backend\data\tasks`
- Ubuntu 挂载目录：`/opt/cae/data/workspaces`
- 节点容器端口：`8085`

### 7.2 启动命令

```bash
docker run -d \
  --name cae-node-1 \
  --network=host \
  -e SERVER_PORT=8085 \
  -e CAE_NODE_NODE_PORT=8085 \
  -e CAE_NODE_NODE_NAME="ubuntu-node-1" \
  -e CAE_NODE_NODE_CODE="UBUNTU_1" \
  -e CAE_NODE_ADVERTISED_HOST="192.168.1.50:8085" \
  -e SCHEDULER_SERVICE_BASE_URL="http://192.168.1.100:8084" \
  -e CAE_NODE_TASK_BASE_URL="http://192.168.1.100:8083" \
  -e CAE_NODE_PATH_MAPPING_WINDOWS="D:\\Project\\CAE_TaskManager_Platform\\Backend\\data\\tasks" \
  -e CAE_NODE_PATH_MAPPING_LINUX="/cae-data/workspaces" \
  -e CAE_NODE_WORK_ROOT="/cae-data/workspaces" \
  -e CAE_NODE_PROCESS_LOG_CHARSET="UTF-8" \
  -e CAE_NODE_MAX_CONCURRENCY="2" \
  -v /opt/cae/data/workspaces:/cae-data/workspaces \
  cae-node-agent:latest
```

### 7.3 这几个参数是什么意思

- `CAE_NODE_ADVERTISED_HOST`
  - 节点注册到调度器时上报的地址
  - 必须是 Windows 调度器能访问到的 Ubuntu 地址

- `SCHEDULER_SERVICE_BASE_URL`
  - 节点注册和心跳发送到哪里

- `CAE_NODE_TASK_BASE_URL`
  - 节点把运行状态、日志、结果回传给哪里

- `CAE_NODE_PATH_MAPPING_WINDOWS`
  - `task-service` 记录在数据库中的 Windows 路径前缀

- `CAE_NODE_PATH_MAPPING_LINUX`
  - 节点容器内部看到的 Linux 路径前缀

- `CAE_NODE_WORK_ROOT`
  - 节点本地工作目录根路径

- `CAE_NODE_PROCESS_LOG_CHARSET=UTF-8`
  - Ubuntu + OpenFOAM 环境推荐显式设为 `UTF-8`

### 7.4 为什么要配置路径映射

因为 `task-service` 下发给节点的 `storagePath` 可能长这样：

```text
D:\Project\CAE_TaskManager_Platform\Backend\data\tasks\123\input\case.zip
```

而 Ubuntu 容器内部真正能访问到的是：

```text
/cae-data/workspaces/123/input/case.zip
```

所以节点需要把：

```text
D:\Project\CAE_TaskManager_Platform\Backend\data\tasks
```

映射为：

```text
/cae-data/workspaces
```

## 8. 第四步：用 deploy.sh 快速启动

如果你不想手工写 `docker run`，也可以用仓库里的部署脚本：

- [node-agent/deploy.sh](/d:/Project/CAE_TaskManager_Platform/Backend/node-agent/deploy.sh)

但要注意：

- 脚本默认不会帮你挂载 Windows 共享目录
- 你必须先自己把 Windows 任务目录挂载到 Ubuntu
- 并且建议挂载到脚本默认使用的路径：
  - `/opt/cae/data/workspaces`

### 8.1 使用方法

```bash
chmod +x deploy.sh
./deploy.sh 1 192.168.1.100 "D:\\Project\\CAE_TaskManager_Platform\\Backend\\data\\tasks"
```

三个参数分别是：

1. 启动节点数量
2. Windows 主机 IP
3. Windows 任务目录前缀

例如上面的命令表示：

- 启动 1 个节点
- 节点回连 Windows 上的调度服务和任务服务
- 把 Windows 路径前缀映射为容器内的 `/cae-data/workspaces`

### 8.2 如果你要启动多个节点

例如启动两个节点：

```bash
./deploy.sh 2 192.168.1.100 "D:\\Project\\CAE_TaskManager_Platform\\Backend\\data\\tasks"
```

脚本会自动使用：

- `8081`
- `8082`

等不同端口。

但如果 Ubuntu 上这些端口已有别的服务占用，需要手动调整脚本。

## 9. 第五步：验证节点是否接入成功

### 9.1 查看容器状态

```bash
docker ps
```

### 9.2 查看节点容器日志

```bash
docker logs -f cae-node-1
```

你应该能看到类似：

- 节点启动成功
- 注册成功
- 心跳发送成功

### 9.3 在平台侧验证

你可以通过以下任一方式确认：

1. 前端节点管理页面查看
2. 调用调度服务节点列表接口
3. 直接查 `scheduler_db.compute_node`
4. 直接查 `scheduler_db.node_solver_capability`

如果节点注册成功，应该能看到：

- 节点状态 `ONLINE`
- 节点最大并发已写入
- 节点求解器能力里有 OpenFOAM 对应记录

## 10. 第六步：如何用它做 OpenFOAM 全流程演示

### 10.1 先确认平台侧配置

你需要确认：

- OpenFOAM 求解器已启用
- OpenFOAM 模板已启用
- 该 Ubuntu 节点整体已启用
- 节点上的 OpenFOAM 能力已启用

### 10.2 注意：上传包必须是“可直接求解”的 OpenFOAM Case

当前默认模板命令是：

```text
simpleFoam -case ${taskDir}
```

这意味着：

- 节点只会直接运行 `simpleFoam`
- 不会自动先运行 `blockMesh`
- 不会自动先运行 `decomposePar`

所以你用于演示的 ZIP 包必须尽量满足以下条件之一：

1. 已经是一个可直接运行的完整 OpenFOAM Case
2. 或者压缩包里已经包含 `constant/polyMesh`

如果你的案例只有：

- `blockMeshDict`

但还没有真正生成网格，那么直接运行 `simpleFoam` 会失败。

### 10.3 推荐的演示做法

最稳妥的方式是：

- 准备一个已经在本地验证成功的可运行 OpenFOAM case
- 将整个 case 目录压缩成 ZIP
- 上传到平台
- 选择 OpenFOAM 对应模板
- 提交任务

### 10.4 如果你希望先执行 blockMesh 再执行 simpleFoam

你可以把模板命令改成类似：

```text
bash -lc "blockMesh -case ${taskDir} && simpleFoam -case ${taskDir}"
```

如果你还希望把日志同时落到本地文件，推荐改成：

```text
bash -lc "set -o pipefail; simpleFoam -case ${taskDir} 2>&1 | tee ${logDir}/simpleFoam.log"
```

这样你就可以在 Ubuntu 容器中直接：

```bash
tail -f /cae-data/workspaces/<taskId>/log/simpleFoam.log
```

但要注意：

- 这需要你在平台的模板配置中修改 `commandTemplate`
- 如果你不改模板，默认只是 `simpleFoam -case ${taskDir}`

## 11. 第七步：任务运行时怎么查看

### 11.1 查看节点代理服务日志

```bash
docker logs -f cae-node-1
```

这个日志主要看：

- 节点是否注册成功
- 是否收到派发请求
- 是否报出了节点级异常

### 11.2 进入容器内部查看

```bash
docker exec -it cae-node-1 /bin/bash
```

进入后可以检查：

```bash
which simpleFoam
echo $WM_PROJECT_VERSION
ls /cae-data/workspaces
```

### 11.3 查看任务工作目录

假设任务 ID 是 `123`，则当前实现下工作目录一般是：

```text
/cae-data/workspaces/123
```

你可以查看：

```bash
ls -R /cae-data/workspaces/123
```

通常里面会有：

- `input`
- `output`
- `log`
- 解压后的案例文件

### 11.4 查看求解器输出

当前实现中，求解器标准输出主要会被节点代理转发给 `task-service` 的任务日志接口。

因此：

- 平台前端任务日志页面
- 或任务日志相关接口

才是最稳定的主查看入口。

如果你希望在 Ubuntu 侧直接 `tail -f` 看本地日志，建议按上面方式把模板命令改成带 `tee` 的版本。

## 12. 第八步：如何判断演示链路是否真正跑通

一个完整的 OpenFOAM 演示至少应满足：

1. 节点在平台中显示 `ONLINE`
2. 节点的 OpenFOAM 能力处于启用状态
3. 任务成功上传并校验通过
4. 任务提交后进入排队
5. 调度器把任务派发到 Ubuntu 节点
6. Ubuntu 节点开始执行 `simpleFoam`
7. 平台能看到任务日志持续增长
8. 最终任务状态进入 `SUCCESS` 或可解释的 `FAILED`

## 13. 当前实现下的几个重要限制

### 13.1 结果文件收集目前只扫描 `output` 目录

当前 `node-agent` 的结果文件收集逻辑只会扫描：

```text
${outputDir}
```

而 OpenFOAM 默认结果通常写在 case 目录本身，不一定写入 `output`。

这意味着：

- 任务可能执行成功
- 但平台“结果文件列表”未必能自动看到 OpenFOAM 生成的结果文件

如果你希望演示“结果文件上报”，建议：

1. 手工把关键输出复制到 `${outputDir}`
2. 或后续增强 `ResultFileCollector`

### 13.2 当前上传校验规则比真实求解要求更宽松

平台当前的文件校验规则更偏“平台原型演示”，并没有把 OpenFOAM 运行所需的全部文件都校验到位。

所以：

- 校验通过
- 不等于一定能成功求解

你用于演示的 ZIP 包最好提前在本地 OpenFOAM 环境中验证通过。

### 13.3 当前节点能力上报是配置式，不是自动扫描式

当前节点默认上报的是：

- `solverId=1`

如果你后续调整了数据库中的求解器定义，记得同步修改节点配置，否则可能出现：

- 节点注册成功
- 但调度时匹配不上求解器能力

## 14. 常用运维命令

### 14.1 查看容器

```bash
docker ps
docker ps -a
```

### 14.2 查看日志

```bash
docker logs -f cae-node-1
```

### 14.3 进入容器

```bash
docker exec -it cae-node-1 /bin/bash
```

### 14.4 重启节点

```bash
docker restart cae-node-1
```

### 14.5 停止并删除节点

```bash
docker stop cae-node-1
docker rm cae-node-1
```

### 14.6 查看工作目录

```bash
ls -R /opt/cae/data/workspaces
```

## 15. 最推荐的实际操作顺序

如果你的目标是尽快完成一次 OpenFOAM 全流程演示，推荐按下面顺序操作：

1. 在 Windows 先启动完整后端服务
2. 确保 Windows `task-service` 和 `scheduler-service` 可被 Ubuntu 访问
3. 把 Windows 任务目录共享给 Ubuntu，并在 Ubuntu 成功挂载
4. 在 Windows 构建 `node-agent.jar`
5. 把 `Dockerfile`、`deploy.sh`、Jar 复制到 Ubuntu
6. 在 Ubuntu 构建 `cae-node-agent:latest`
7. 用手工 `docker run` 启动第一个节点
8. 在平台中确认节点 `ONLINE`
9. 上传一个已经验证过的真实 OpenFOAM case ZIP
10. 提交任务并观察调度、日志和状态变化

## 16. 总结

按照当前项目设计，Ubuntu 虚拟机上的 OpenFOAM 节点代理容器部署，最核心不是 `docker run` 这一条命令，而是同时保证下面三件事成立：

1. Windows 中心服务和 Ubuntu 节点网络互通
2. Ubuntu 节点能够访问 Windows 的任务目录
3. 节点注册时上报的 `NODE_ADVERTISED_HOST` 是 Windows 调度器可达的真实地址

这三点一旦打通，节点代理容器的构建和启动本身并不复杂；而一旦其中任意一点没有打通，就会出现“节点看起来在线，但任务跑不起来”的问题。
