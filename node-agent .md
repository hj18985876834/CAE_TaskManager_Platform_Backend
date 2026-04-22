# node-agent 模块说明文档

## 1. 文档目的

这份文档继续沿用 `user-service.md`、`solver-service.md`、`task-service.md`、`scheduler-service.md` 的说明方式，不只是回答“`node-agent` 是做什么的”，还要把它拆到“模块根目录 -> 分层目录 -> 具体代码文件 -> 文件职责”的粒度，方便下面几类工作：

- 写毕业论文或设计说明时，能把节点代理模块和真实代码一一对应；
- 排查“节点为什么没有注册上来”“心跳为什么失败”“任务为什么没有真正执行”“日志为什么没有回传”这类问题时，快速定位到具体类；
- 理清 `scheduler-service -> node-agent -> task-service` 这一条跨服务执行链；
- 区分“设计文档中的目标能力”和“当前已经真实落地的实现能力”；
- 为后续补容器执行、结果收集增强、共享存储、执行隔离和鉴权增强打基础。

分析范围以 `node-agent/src/main/java`、`node-agent/src/main/resources` 以及模块根目录下的部署文件为主，`target/` 不作为设计分析对象。

---

## 2. 模块定位

`node-agent` 是平台中的“节点执行代理服务”，部署在具体的计算节点上，负责把调度器下发的任务真正落到本机执行环境中。

如果说：

- `solver-service` 解决“平台支持哪些求解器、模板和文件规则”；
- `task-service` 解决“任务如何创建、校验、提交、流转和回传”；
- `scheduler-service` 解决“任务应该派发给哪个节点”；

那么 `node-agent` 解决的就是：

1. 节点如何向调度中心注册自己；
2. 节点如何持续上报心跳和运行负载；
3. 节点如何接收调度器下发的任务；
4. 节点如何准备工作目录与输入文件；
5. 节点如何选择合适的执行器并启动本地进程；
6. 节点如何将运行状态、日志、结果摘要、结果文件回传给 `task-service`；
7. 节点如何处理中途取消、超时和异常退出。

因此，`node-agent` 不是用户侧业务服务，也不是调度决策服务，而是平台的“执行端代理层”。

它在系统中的位置可以概括为：

> `scheduler-service` 负责选节点，`node-agent` 负责在这个节点上把任务真正执行起来。

---

## 3. 模块根目录与源码入口

### 3.1 模块根目录

模块根目录是：

```text
node-agent/
```

当前最重要的入口位置包括：

- `node-agent/pom.xml`
- `node-agent/src/main/java/com/example/cae/nodeagent/`
- `node-agent/src/main/resources/application.yml`
- `node-agent/Dockerfile`
- `node-agent/deploy.sh`

### 3.2 不需要作为设计分析重点的目录

下面这些目录不属于源码设计主体：

```text
node-agent/target/
```

`target/` 是打包后的编译产物，不应写入模块源码结构设计部分。

---

## 4. 分层与文件夹映射

| 分层       | 对应文件夹                                                   | 作用                                                 |
| ---------- | ------------------------------------------------------------ | ---------------------------------------------------- |
| 启动入口层 | `node-agent/src/main/java/com/example/cae/nodeagent/`        | Spring Boot 启动入口，开启定时任务                   |
| 配置层     | `node-agent/src/main/java/com/example/cae/nodeagent/config/` | 节点配置、线程池、HTTP 客户端配置                    |
| 接口层     | `node-agent/src/main/java/com/example/cae/nodeagent/interfaces/` | 接收调度器下发任务、接收取消请求、定义请求响应对象   |
| 应用层     | `node-agent/src/main/java/com/example/cae/nodeagent/application/` | 任务接收、异步执行编排、回传编排、心跳与注册编排     |
| 领域层     | `node-agent/src/main/java/com/example/cae/nodeagent/domain/` | 执行上下文、执行结果、节点信息、执行器抽象与选择规则 |
| 基础设施层 | `node-agent/src/main/java/com/example/cae/nodeagent/infrastructure/` | 跨服务调用、进程管理、文件准备、路径映射、命令构建   |
| 支撑层     | `node-agent/src/main/java/com/example/cae/nodeagent/support/` | 节点临时目录清理等附属支撑功能                       |
| 资源配置层 | `node-agent/src/main/resources/`                             | 节点基础配置、服务地址、并发与求解器配置             |

如果只想快速找代码，可以这样记：

- 看调度器下发入口：`interfaces/controller`
- 看任务接收和异步启动：`application/manager/TaskDispatchManager`
- 看真正执行流程：`application/manager/TaskExecuteManager`
- 看日志/结果回传：`application/manager/TaskReportManager`
- 看执行器实现：`domain/executor`
- 看进程运行细节：`infrastructure/process`
- 看输入输出目录和路径映射：`infrastructure/storage`
- 看与 `scheduler-service`、`task-service` 的远程调用：`infrastructure/client`

---

## 5. 模块级结构总览

当前源码结构如下：

```text
node-agent/
├── Dockerfile
├── deploy.sh
├── pom.xml
├── src/main/java/com/example/cae/nodeagent/
│   ├── NodeAgentApplication.java
│   ├── application/
│   │   ├── assembler/
│   │   │   └── ExecutionContextAssembler.java
│   │   ├── manager/
│   │   │   ├── TaskDispatchManager.java
│   │   │   ├── TaskExecuteManager.java
│   │   │   ├── TaskReportManager.java
│   │   │   └── TaskRuntimeRegistry.java
│   │   ├── scheduler/
│   │   │   └── HeartbeatJob.java
│   │   └── service/
│   │       ├── HeartbeatAppService.java
│   │       ├── NodeRegisterAppService.java
│   │       └── TaskReportAppService.java
│   ├── config/
│   │   ├── ExecutorConfig.java
│   │   ├── FeignClientConfig.java
│   │   ├── NodeAgentBeanConfig.java
│   │   └── NodeAgentConfig.java
│   ├── domain/
│   │   ├── enums/
│   │   │   └── ExecutionStageEnum.java
│   │   ├── executor/
│   │   │   ├── AbstractSolverExecutor.java
│   │   │   ├── CalculixExecutor.java
│   │   │   ├── MockExecutor.java
│   │   │   ├── OpenFoamExecutor.java
│   │   │   └── SolverExecutor.java
│   │   ├── model/
│   │   │   ├── ExecutionContext.java
│   │   │   ├── ExecutionResult.java
│   │   │   ├── InputFileMeta.java
│   │   │   └── NodeInfo.java
│   │   └── service/
│   │       ├── ExecutionContextBuildService.java
│   │       └── ExecutorSelectDomainService.java
│   ├── infrastructure/
│   │   ├── client/
│   │   │   ├── SchedulerNodeClient.java
│   │   │   ├── TaskReportClient.java
│   │   │   └── impl/
│   │   │       ├── SchedulerNodeClientImpl.java
│   │   │       └── TaskReportClientImpl.java
│   │   ├── process/
│   │   │   ├── ProcessCanceledException.java
│   │   │   ├── ProcessExitHandler.java
│   │   │   ├── ProcessLogReader.java
│   │   │   ├── ProcessRunner.java
│   │   │   └── ProcessTimeoutException.java
│   │   ├── storage/
│   │   │   ├── InputFilePrepareService.java
│   │   │   ├── PathMappingSupport.java
│   │   │   ├── ResultFileCollector.java
│   │   │   └── WorkDirManager.java
│   │   └── support/
│   │       ├── CommandBuilder.java
│   │       ├── LogPushBuffer.java
│   │       └── NodeInfoCollector.java
│   ├── interfaces/
│   │   ├── controller/
│   │   │   └── DispatchController.java
│   │   ├── request/
│   │   │   ├── CancelTaskRequest.java
│   │   │   └── DispatchTaskRequest.java
│   │   └── response/
│   │       ├── CancelTaskResponse.java
│   │       └── DispatchTaskResponse.java
│   └── support/
│       └── AgentTempFileCleaner.java
└── src/main/resources/application.yml
```

和 `scheduler-service` 不同，`node-agent` 的复杂度不在持久化，而在：

- 进程执行；
- 工作目录准备；
- 跨系统路径映射；
- 日志与结果回传；
- 取消和超时处理；
- 多执行器分流。

---

## 6. 根目录文件说明

### 6.1 `node-agent/pom.xml`

模块的 Maven 描述文件，作用包括：

- 声明当前模块是 `node-agent`；
- 继承父工程 `cae-taskmanager-backend`；
- 引入 `spring-boot-starter-web`；
- 引入共享模块 `common-lib`；
- 配置 `spring-boot-maven-plugin` 打成可运行 Jar。

当前这个模块没有引入数据库依赖，也没有 MyBatis、JPA，说明 `node-agent` 是一个“无持久化、纯运行时”的服务。

### 6.2 `node-agent/Dockerfile`

节点代理的容器镜像构建文件。

它的关键点包括：

- 直接使用 `opencfd/openfoam-default:2312` 作为基础镜像；
- 容器内再安装 `openjdk-17-jre` 与 `hwloc`；
- 设置时区为 `Asia/Shanghai`；
- 创建 `/cae-data/workspaces` 和 `/cae-data/logs` 工作目录；
- 将打好的 `node-agent.jar` 复制进镜像；
- 清空基础镜像自带的 `ENTRYPOINT`；
- 通过 `source /usr/lib/openfoam/openfoam2312/etc/bashrc` 先加载 OpenFOAM 环境，再启动 Java。

这个 `Dockerfile` 说明当前 `node-agent` 的部署思路是：

- 节点代理本身可以容器化；
- 但任务执行仍主要是“容器内本机进程执行”，并不等于“每个任务独立容器执行”。

### 6.3 `node-agent/deploy.sh`

Ubuntu/Linux 环境下的快速部署脚本。

它的作用主要有：

- 检查并准备 `node-agent` 的 Jar 包；
- 构建 `cae-node-agent:latest` 镜像；
- 接收 Windows 宿主机 IP 和共享目录映射参数；
- 使用 `docker run` 启动一个或多个节点代理实例；
- 配置 `SCHEDULER_SERVICE_BASE_URL`、`TASK_SERVICE_BASE_URL`、`CAE_NODE_ADVERTISED_HOST` 等环境变量；
- 挂载共享工作目录 `/cae-data/workspaces`。

这个脚本对演示“Ubuntu 虚拟机上的 docker 计算节点”很有帮助，属于部署说明的重要组成部分。

### 6.4 `node-agent/src/main/resources/application.yml`

节点代理的运行配置文件，定义了：

- 服务端口：默认 `8085`
- 服务名：`node-agent`
- 节点 ID、节点编码、节点名称
- 日志读取字符集：`process-log-charset`
- 节点对外端口
- 最大并发
- `scheduler-service` 基础地址
- `task-service` 基础地址
- 广播给调度器的主机地址 `advertised-host`
- 工作目录根路径 `work-root`
- 节点支持的求解器 ID 列表 `solver-ids`
- 各求解器版本映射 `solver-versions`

这说明当前节点代理支持“通过静态配置声明本机支持哪些求解器及版本”，再在启动注册时上报给调度中心。

---

## 7. 启动入口层

### 7.1 对应文件夹

启动入口层对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/
```

### 7.2 文件说明

#### `NodeAgentApplication.java`

这是节点代理的 Spring Boot 启动类，负责：

- 启动整个 `node-agent`；
- 通过 `@EnableScheduling` 开启定时任务。

如果没有它：

- 启动后不会执行 `NodeRegisterAppService` 的注册逻辑；
- `HeartbeatJob` 和 `AgentTempFileCleaner` 这类定时任务也不会按计划运行。

---

## 8. 配置层

### 8.1 对应文件夹

配置层对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/config/
```

### 8.2 文件说明

#### `NodeAgentConfig.java`

这是节点代理最核心的配置对象，绑定前缀为 `cae.node`。

当前包含的主要配置有：

- 节点身份信息：`nodeId`、`nodeCode`、`nodeName`
- 日志字符集：`processLogCharset`
- 节点对外端口：`nodePort`
- 最大并发：`maxConcurrency`
- 调度服务地址：`schedulerBaseUrl`
- 任务服务地址：`taskBaseUrl`
- 对外通告主机地址：`advertisedHost`
- 节点 token：`nodeToken`
- 工作目录根路径：`workRoot`
- Windows/Linux 路径映射前缀：`pathMappingWindows`、`pathMappingLinux`
- 支持的求解器列表：`solverIds`
- 求解器版本映射：`solverVersions`

它是整个 `node-agent` 的配置中心，`NodeInfoCollector`、`SchedulerNodeClientImpl`、`TaskReportClientImpl`、`WorkDirManager`、`PathMappingSupport` 都依赖它。

#### `NodeAgentBeanConfig.java`

负责注册两个重要 Bean：

1. `taskExecutor`
2. `RestTemplate`

其中 `taskExecutor` 的线程池策略是：

- 核心线程数 = 最大线程数 = `maxConcurrency`
- 队列容量 `200`
- 线程名前缀 `node-agent-task-`
- 拒绝策略 `CallerRunsPolicy`

这说明当前节点代理的并发控制思路是：

- 用配置的 `maxConcurrency` 控制真正并行执行任务数；
- 用线程池承接异步执行；
- 额外配合 `TaskDispatchManager` 和 `TaskRuntimeRegistry` 做运行数判断。

#### `ExecutorConfig.java`

执行器配置占位类。

当前是空实现，说明架构上预留了“集中配置执行器相关 Bean”的位置，但首版还没有填入更复杂的执行器装配逻辑。

#### `FeignClientConfig.java`

Feign 配置占位类。

当前同样是空实现。结合 `pom.xml` 可以看出，当前跨服务调用仍是 `RestTemplate`，还没有切到 OpenFeign。

---

## 9. 接口层

### 9.1 对应文件夹

接口层对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/interfaces/
```

它分为：

- `controller/`：接收调度器下发请求
- `request/`：请求对象
- `response/`：响应对象

### 9.2 控制器目录

对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/interfaces/controller/
```

#### `DispatchController.java`

这是节点代理暴露给调度服务的内部接口控制器，包含两个入口：

- `POST /internal/dispatch-task`
- `POST /internal/cancel-task`

其中：

- `dispatch()` 负责接收任务并调用 `TaskDispatchManager.acceptTask()`
- `cancel()` 负责接收取消请求并调用 `TaskDispatchManager.cancelTask()`

需要特别说明的是：

- 当前这两个接口没有额外 token 鉴权；
- 也就是说，当前 `scheduler-service -> node-agent` 的调用建立在受信任内网或受控部署环境之上。

### 9.3 请求对象目录

对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/interfaces/request/
```

#### `DispatchTaskRequest.java`

调度器下发给节点的任务请求对象，字段包括：

- `taskId`
- `taskNo`
- `solverId`
- `solverCode`
- `profileId`
- `taskType`
- `commandTemplate`
- `parserName`
- `timeoutSeconds`
- `inputFiles`
- `params`

它本质上是“执行上下文原始输入对象”，后续会被转换为 `ExecutionContext`。

#### `CancelTaskRequest.java`

取消任务请求对象，字段包括：

- `taskId`
- `reason`

### 9.4 响应对象目录

对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/interfaces/response/
```

#### `DispatchTaskResponse.java`

下发任务响应对象，字段包括：

- `accepted`
- `message`

调度器据此判断节点是否接受了当前任务。

#### `CancelTaskResponse.java`

取消任务响应对象，字段包括：

- `accepted`
- `message`

如果任务已经不在运行，则会返回 `accepted = false`。

---

## 10. 应用层

### 10.1 对应文件夹

应用层对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/application/
```

这是 `node-agent` 的核心编排层，负责把：

- 调度器下发请求；
- 运行时状态管理；
- 本地执行；
- 结果回传；
- 注册和心跳；

串成完整闭环。

### 10.2 组装器目录

对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/application/assembler/
```

#### `ExecutionContextAssembler.java`

作用：把 `DispatchTaskRequest` 转换成 `ExecutionContext`。

当前它本身逻辑很薄，真正构建逻辑委托给 `ExecutionContextBuildService`，但它提供了清晰的“接口层对象 -> 领域执行上下文”过渡层。

### 10.3 Manager 目录

对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/application/manager/
```

#### `TaskDispatchManager.java`

任务接收管理器，是 `DispatchController` 背后的第一层业务入口。

它负责：

- 校验调度请求是否包含 `taskId`
- 校验节点当前是否达到最大并发
- 防止同一个任务重复运行
- 构造执行上下文
- 标记任务已接受
- 将实际执行提交到线程池异步运行
- 处理取消请求

关键方法如下。

##### `acceptTask(DispatchTaskRequest request)`

流程是：

1. 校验请求和 `taskId`
2. 检查 `TaskRuntimeRegistry.runningCount()` 是否已达到 `maxConcurrency`
3. 调 `taskRuntimeRegistry.register(taskId)` 防止重复任务
4. 将请求转成 `ExecutionContext`
5. 调 `taskReportManager.onTaskAccepted(taskId)` 初始化日志序号
6. 把执行任务提交给线程池，异步调用 `taskExecuteManager.execute(context)`

如果线程池提交失败，会把已注册的运行态回滚。

##### `cancelTask(CancelTaskRequest request)`

流程是：

1. 校验 `taskId`
2. 调 `taskRuntimeRegistry.cancel(taskId, reason)`
3. 若任务存在，就尝试中断线程和销毁进程

#### `TaskExecuteManager.java`

这是节点代理中最核心的执行编排器，负责把一条任务真正执行到底。

主要职责包括：

- 绑定当前执行线程到运行时注册表
- 检查是否已收到取消请求
- 准备工作目录
- 准备输入文件
- 上报任务进入 `RUNNING`
- 选择执行器
- 调用执行器执行
- 根据执行结果走成功、失败或取消回传
- 最终做收尾清理

关键方法如下。

##### `execute(ExecutionContext context)`

主执行流程：

1. 绑定 worker thread
2. 检查是否已取消
3. `prepareWorkDir(context)`
4. `prepareInputFiles(context)`
5. `taskReportManager.reportRunning(context)`
6. `selectExecutor(context)`
7. `executor.execute(context)`
8. 根据 `ExecutionResult.success` 分流：
   - 成功：`reportSuccess`
   - 失败：`reportFail`
9. 如果异常是取消或 `ProcessCanceledException`，走 `reportCanceled`
10. `finally` 中总会 `completeTask(taskId)`

##### `prepareWorkDir(ExecutionContext context)`

委托 `WorkDirManager` 创建任务本地工作目录。

##### `prepareInputFiles(ExecutionContext context)`

委托 `InputFilePrepareService` 把任务输入文件复制或解压到本地目录。

##### `selectExecutor(ExecutionContext context)`

委托 `ExecutorSelectDomainService` 根据 `solverCode` 选择合适的执行器。

#### `TaskReportManager.java`

负责运行中的回传编排，是“执行”和“回传”之间的桥梁。

它的主要职责包括：

- 初始化日志序号
- 上报 `RUNNING`
- 逐行推送日志
- 回传结果摘要
- 回传结果文件
- 上报完成
- 上报失败
- 上报取消
- 完成后清理运行态并更新调度器运行数

关键方法如下。

##### `onTaskAccepted(Long taskId)`

为任务初始化日志序号计数器。

##### `reportRunning(ExecutionContext context)`

向 `task-service` 上报状态 `RUNNING`。

##### `pushLog(Long taskId, Integer seqNo, String line)`

逐行上报日志。  
如果外部没有指定 `seqNo`，它会自动递增生成。

##### `reportSuccess(ExecutionContext context, ExecutionResult result, long startMillis)`

成功回传链路：

- 补齐执行时长
- 上报结果摘要
- 遍历回传结果文件
- 最后标记任务完成

##### `reportFail(ExecutionContext context, Exception ex)`

失败回传链路：

- 若异常是 `ProcessTimeoutException`，写 `TIMEOUT`
- 否则写 `RUNTIME_ERROR`

##### `reportCanceled(ExecutionContext context, String reason)`

取消回传链路：

- 当前只调用 `reportStatus(taskId, "CANCELED", message)`

也就是说，当前取消是通过“状态上报”为主，而不是单独走 `markFailed` 或 `markFinished`。

##### `completeTask(Long taskId)`

统一收尾动作：

- 清理日志序号缓存
- 从运行时注册表移除任务
- 调调度服务把 `runningCount - 1`

这里有一个很重要的实现细节：

- `node-agent` 在任务接收时没有显式上报 `runningCount + 1`
- 因为调度器在选中节点时已经预占了并发数
- 节点侧真正负责的是完成后尽快回传 `-1`，以及在心跳中持续上报实时 `runningCount`

#### `TaskRuntimeRegistry.java`

这是节点代理唯一的“运行态注册表”，使用内存中的 `ConcurrentHashMap` 保存当前活动任务。

它负责：

- 注册任务是否进入运行态
- 统计运行中的任务数
- 记录每个任务的执行线程
- 记录每个任务的底层 `Process`
- 标记取消请求与取消原因
- 发出线程中断和进程销毁信号

这说明当前 `node-agent` 的运行态完全在内存里，服务重启后不会保留。

### 10.4 定时任务目录

对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/application/scheduler/
```

#### `HeartbeatJob.java`

心跳定时任务，默认每 `10000ms` 执行一次。

它负责调用 `HeartbeatAppService.sendHeartbeat()`，持续把节点负载上报给调度器。

### 10.5 应用服务目录

对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/application/service/
```

#### `NodeRegisterAppService.java`

节点注册应用服务。

它有两个职责：

- 在启动后通过 `@PostConstruct` 自动注册自己
- 提供 `registerSelf()` 供其他地方显式重试注册

它调用链是：

- `NodeInfoCollector.collectNodeInfo()`
- `SchedulerNodeClient.register(nodeInfo)`

当前实现中，如果启动注册失败，只会记录日志，不会阻止 `node-agent` 启动。

#### `HeartbeatAppService.java`

节点心跳应用服务。

它的逻辑是：

1. 采集当前节点信息
2. 调调度器发送心跳
3. 如果失败，则尝试“重新注册一次”
4. 再重新发一次心跳

这意味着当前 `node-agent` 有一个比较实用的自恢复逻辑：

- 心跳失败时会主动尝试重新注册，而不是一直静默失败。

#### `TaskReportAppService.java`

任务回传应用服务，是 `TaskReportManager` 对外部系统的薄封装。

它负责把回传请求分别分派给：

- `TaskReportClient`：回传到 `task-service`
- `SchedulerNodeClient`：回调调度服务调整运行数

主要能力包括：

- `reportStatus`
- `reportLog`
- `reportResultSummary`
- `reportResultFile`
- `markFinished`
- `markFailed`
- `updateRunningCount`

---

## 11. 领域层

### 11.1 对应文件夹

领域层对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/domain/
```

### 11.2 枚举目录

对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/domain/enums/
```

#### `ExecutionStageEnum.java`

定义了执行阶段枚举：

- `RECEIVED`
- `PREPARING`
- `RUNNING`
- `COLLECTING`
- `FINISHED`
- `FAILED`

当前它更多是一个预留的语义化枚举，并没有完整贯穿到回传链路中。

### 11.3 领域模型目录

对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/domain/model/
```

#### `ExecutionContext.java`

任务执行上下文模型，是 `node-agent` 的核心领域对象。

它包含两类信息：

1. 调度器下发的业务信息
2. 节点本地执行时计算出的目录信息

主要字段包括：

- 任务标识：`taskId`、`taskNo`
- 求解器信息：`solverId`、`solverCode`
- 模板信息：`profileId`、`taskType`
- 执行信息：`commandTemplate`、`parserName`、`timeoutSeconds`
- 输入信息：`inputFiles`、`params`
- 本地目录：`workDir`、`taskDir`、`inputDir`、`outputDir`、`logDir`

它还封装了几个方便判断的方法：

- `isTimeoutEnabled()`
- `hasInputFiles()`
- `hasParser()`

#### `ExecutionResult.java`

执行结果模型，代表一次执行完成后的总结信息。

核心字段包括：

- 是否成功
- 持续时间
- 摘要文本
- 指标信息 `metrics`
- 结果文件列表

并提供了：

- `success(...)`
- `fail(...)`

两个静态工厂方法。

#### `InputFileMeta.java`

输入文件元信息模型，字段包括：

- `fileKey`
- `originName`
- `storagePath`

这些数据来自调度器透传的任务输入文件清单。

#### `NodeInfo.java`

节点信息模型，用于注册和心跳上报。

核心字段包括：

- 节点编码
- 节点名称
- 主机地址
- 最大并发
- CPU 使用率
- 内存使用率
- 当前运行任务数
- 支持的求解器 ID 列表

### 11.4 领域服务目录

对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/domain/service/
```

#### `ExecutionContextBuildService.java`

负责从 `DispatchTaskRequest` 构造 `ExecutionContext`。

当前它做的是字段级拷贝，目录类字段还需要在后续工作目录准备阶段再补齐。

#### `ExecutorSelectDomainService.java`

执行器选择领域服务。

它会遍历 Spring 容器中的所有 `SolverExecutor` 实现，找到第一个 `supports(context)` 返回 `true` 的执行器。

如果找不到，就抛出异常：

- `no available executor for solver`

这就是当前节点侧“按求解器选择执行器”的真正入口。

### 11.5 执行器目录

对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/domain/executor/
```

#### `SolverExecutor.java`

执行器统一接口，定义：

- `boolean supports(ExecutionContext context)`
- `ExecutionResult execute(ExecutionContext context)`

#### `AbstractSolverExecutor.java`

执行器抽象基类，提供模板方法：

- 先 `prepare(context)`
- 再 `doExecute(context)`

当前默认 `prepare()` 是空实现，给各执行器预留自定义准备逻辑。

#### `OpenFoamExecutor.java`

OpenFOAM 执行器。

它的行为是：

- `supports()` 依据 `solverCode == OPENFOAM`
- 通过 `CommandBuilder` 生成命令
- 调 `ProcessRunner.run(...)` 执行进程
- 将标准输出和标准错误都逐行推送给 `TaskReportManager.pushLog()`
- 用 `ResultFileCollector` 扫描结果目录
- 返回 `ExecutionResult.success(...)`

如果进程退出码不是 0，会抛出异常。

#### `CalculixExecutor.java`

CalculiX 执行器。

整体模式与 `OpenFoamExecutor` 一致，只是 `supports()` 判断的是 `solverCode == CALCULIX`。

#### `MockExecutor.java`

Mock 执行器。

它支持：

- `solverCode == MOCK`
- 或 `solverId == null`
- 或 `solverId == 0`

作用主要是用于联调或演示场景：

- 模拟输出 5 行日志
- 睡眠构造执行时间
- 返回一个简单成功结果

它使得即使没有真实求解器环境，平台主链也能跑通。

---

## 12. 基础设施层

### 12.1 对应文件夹

基础设施层对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/infrastructure/
```

### 12.2 远程调用目录

对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/infrastructure/client/
```

#### `SchedulerNodeClient.java`

调度服务客户端抽象，定义：

- 注册节点
- 发送心跳
- 调整节点运行任务数

#### `TaskReportClient.java`

任务服务回传客户端抽象，定义：

- 上报状态
- 上报日志
- 上报结果摘要
- 上报结果文件
- 标记完成
- 标记失败

#### `impl/SchedulerNodeClientImpl.java`

`SchedulerNodeClient` 的实现类，使用 `RestTemplate` 调用 `scheduler-service`。

它对应的远程接口包括：

- `POST /api/node-agent/register`
- `POST /api/node-agent/heartbeat`
- `POST /internal/tasks/{taskId}/dispatch-failed`

其中有几个非常关键的实现细节：

- 注册时会把 `solverIds` 转成 `solvers[{solverId, solverVersion}]`
- 注册成功后会把返回的 `nodeId` 和 `nodeToken` 回写到 `NodeAgentConfig`
- 心跳请求会通过请求头携带 `X_NODE_TOKEN`
- 执行前失败不会再直接回写 task-service，而是先回调 scheduler-service，由调度服务统一驱动任务状态回写与预占释放

#### `impl/TaskReportClientImpl.java`

`TaskReportClient` 的实现类，使用 `RestTemplate` 调用 `task-service`。

它对应的远程接口包括：

- `POST /internal/tasks/{taskId}/status-report`
- `POST /internal/tasks/{taskId}/log-report`
- `POST /internal/tasks/{taskId}/result-summary-report`
- `POST /internal/tasks/{taskId}/result-file-report`
- `POST /internal/tasks/{taskId}/mark-finished`
- `POST /internal/tasks/{taskId}/mark-failed`

关键实现细节包括：

- 每次回传都会带上 `nodeId`
- 每次回传都会携带 `X_NODE_TOKEN`
- 结果文件路径会通过 `PathMappingSupport.toWindowsPath()` 转回平台可识别的 Windows 路径
- 根据结果文件后缀推断 `fileType`

这说明当前节点回传链已经支持“节点 token 校验”和“跨 Windows/Linux 路径映射”。

### 12.3 进程执行目录

对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/infrastructure/process/
```

#### `ProcessRunner.java`

本地外部进程运行器，是节点代理的关键基础设施组件。

它的执行流程是：

1. 用 `ProcessBuilder` 启动外部命令
2. 把 `Process` 注册到 `TaskRuntimeRegistry`
3. 起两个线程分别读取标准输出和错误输出
4. 根据是否配置超时决定 `waitFor` 方式
5. 超时则强杀进程并抛 `ProcessTimeoutException`
6. 等待日志线程结束
7. 若任务已被取消，则抛 `ProcessCanceledException`
8. 检查退出码
9. 最终清理注册的进程句柄

#### `ProcessLogReader.java`

进程日志读取器。

它按 `NodeAgentConfig.processLogCharset` 指定的字符集逐行读取输出流，默认可配置为 `GBK` 或 `UTF-8`。

这对于 Windows 环境下的求解器输出尤其重要。

#### `ProcessExitHandler.java`

进程退出码检查器。

当前规则很简单：

- 退出码不为 0 就抛异常

#### `ProcessCanceledException.java`

表示任务被取消时的专用异常。

#### `ProcessTimeoutException.java`

表示进程执行超时的专用异常。

### 12.4 存储与工作目录目录

对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/infrastructure/storage/
```

#### `WorkDirManager.java`

任务工作目录管理器。

它会基于：

- `workRoot`
- `taskId`

为每个任务创建：

- `workDir`
- `taskDir`
- `inputDir`
- `outputDir`
- `logDir`

目录结构大致是：

```text
{workRoot}/{taskId}/
├── input/
├── output/
└── log/
```

其中 `cleanupTaskDirs()` 当前还是预留空实现。

#### `InputFilePrepareService.java`

输入文件准备服务，是节点代理最关键的文件处理组件之一。

它的主要职责包括：

- 根据 `storagePath` 找到原始输入文件
- 通过 `PathMappingSupport.toLinuxPath()` 将平台路径映射为节点本地路径
- 把输入文件复制到任务 `inputDir`
- 如果文件是 `.zip`，则自动解压到工作目录
- 通过 Zip Slip 防护确保压缩包不会越界写入
- 如果压缩包只有一个顶层目录，则把 `taskDir` 指向该目录

这使得当前节点代理可以直接处理“上传压缩包后执行”的场景。

#### `ResultFileCollector.java`

结果文件收集器。

当前实现非常简单：

- 扫描 `outputDir`
- 只返回该目录下的文件
- 不递归子目录

这说明当前结果收集属于基础版实现，没有复杂的规则过滤、归档和索引策略。

#### `PathMappingSupport.java`

跨系统路径映射工具。

它提供：

- `toLinuxPath(String windowsPath)`
- `toWindowsPath(String linuxPath)`

主要用于 Windows 平台的任务文件路径与 Linux 节点容器内路径之间的互相转换。

这是当前支持“Windows 管理平台 + Ubuntu docker 节点”场景的关键组件。

### 12.5 基础支撑目录

对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/infrastructure/support/
```

#### `CommandBuilder.java`

命令构建器。

它会把 `commandTemplate` 中的变量替换为上下文真实值，例如：

- `${taskId}`
- `${taskNo}`
- `${solverId}`
- `${solverCode}`
- `${profileId}`
- `${taskType}`
- `${workDir}`
- `${taskDir}`
- `${inputDir}`
- `${outputDir}`
- `${logDir}`

同时还会把 `params` 中的键值也并入替换。

最终：

- Windows 上用 `cmd /c`
- Linux 上用 `/bin/sh -c`

执行命令。

需要注意的是，当前替换是“简单字符串替换”，没有做更强的参数转义和 shell 安全封装。

#### `LogPushBuffer.java`

日志缓冲组件，提供：

- `append(String line)`
- `drain()`

当前主执行链路里基本没有真正使用它，更像是为“批量推送日志”预留的支撑点。

#### `NodeInfoCollector.java`

节点信息采集器，是注册和心跳的基础组件。

它负责采集：

- 节点编码、名称
- 通告给平台的主机地址
- 最大并发
- CPU 使用率
- 内存使用率
- 当前运行任务数
- 支持的求解器列表

其中：

- `runningCount` 来自 `TaskRuntimeRegistry`
- CPU 和内存来自 `OperatingSystemMXBean`
- 如果无法读取系统负载，就回落为 `0`
- 若设置了 `advertisedHost`，优先使用该地址，否则自动拼接本机 IP + 端口

---

## 13. 支撑层

### 13.1 对应文件夹

支撑层对应文件夹：

```text
node-agent/src/main/java/com/example/cae/nodeagent/support/
```

### 13.2 文件说明

#### `AgentTempFileCleaner.java`

节点临时目录清理器。

它是一个定时任务，默认每小时执行一次，会：

- 扫描 `workRoot`
- 找出最后修改时间超过 24 小时的任务目录
- 递归删除这些旧目录

这是节点磁盘空间自清理的基础能力。

需要说明的是：

- 当前实现是直接递归删除；
- 适合作为开发和演示环境的基础清理策略；
- 生产环境如果要增强，应再补更严格的安全边界、保留策略和清理审计。

---

## 14. 与外部服务接口的对应关系

`node-agent` 没有自己的数据库表，核心关系主要体现在它与 `scheduler-service` 和 `task-service` 的接口协作上。

### 14.1 与 `scheduler-service` 的接口关系

节点代理主动调用调度服务的接口包括：

- `POST /api/node-agent/register`
- `POST /api/node-agent/heartbeat`
- `POST /internal/tasks/{taskId}/dispatch-failed`

对应代码链路：

- `application/service/NodeRegisterAppService.java`
- `application/service/HeartbeatAppService.java`
- `application/service/TaskReportAppService.java`
- `infrastructure/client/impl/SchedulerNodeClientImpl.java`

### 14.2 接收 `scheduler-service` 的派发接口

调度服务主动调用节点代理的接口包括：

- `POST /internal/dispatch-task`
- `POST /internal/cancel-task`

对应代码链路：

- `interfaces/controller/DispatchController.java`
- `application/manager/TaskDispatchManager.java`

### 14.3 与 `task-service` 的回传接口关系

节点代理主动调用任务服务的接口包括：

- `POST /internal/tasks/{taskId}/status-report`
- `POST /internal/tasks/{taskId}/log-report`
- `POST /internal/tasks/{taskId}/result-summary-report`
- `POST /internal/tasks/{taskId}/result-file-report`
- `POST /internal/tasks/{taskId}/mark-finished`
- `POST /internal/tasks/{taskId}/mark-failed`

对应代码链路：

- `application/manager/TaskReportManager.java`
- `application/service/TaskReportAppService.java`
- `infrastructure/client/impl/TaskReportClientImpl.java`

### 14.4 与共享文件目录的关系

节点代理对文件目录的使用关系包括：

- 读取平台输入文件：`InputFilePrepareService`
- 将输入文件复制或解压到本地任务目录：`WorkDirManager + InputFilePrepareService`
- 扫描本地输出目录收集结果：`ResultFileCollector`
- 回传结果文件路径时做 Linux/Windows 路径反向映射：`PathMappingSupport`

---

## 15. 核心调用链

### 15.1 启动注册链路

节点代理启动后的注册链路如下：

1. `NodeAgentApplication` 启动 Spring 容器
2. `NodeRegisterAppService.registerOnStartup()` 被 `@PostConstruct` 触发
3. `NodeInfoCollector.collectNodeInfo()` 收集：
   - 节点标识
   - 广播地址
   - 并发
   - 当前运行数
   - 支持求解器列表
4. `SchedulerNodeClientImpl.register()` 组装注册请求
5. 调 `scheduler-service` 的 `/api/node-agent/register`
6. 调度服务返回 `nodeId + nodeToken`
7. 节点代理把两者写回 `NodeAgentConfig`

需要强调的是：

- `node-agent` 上报的求解器能力来源于本地配置；
- 上报时会同时带 `solverVersion`；
- 调度中心据此重建节点能力表。

### 15.2 心跳链路

心跳链路如下：

1. `HeartbeatJob.sendHeartbeat()` 定时执行
2. `HeartbeatAppService.sendHeartbeat()`
3. `NodeInfoCollector.collectNodeInfo()` 重新采集 CPU、内存、运行数
4. `SchedulerNodeClientImpl.heartbeat(nodeInfo)`
5. 调 `scheduler-service` 的 `/api/node-agent/heartbeat`
6. 请求头带上 `X_NODE_TOKEN`

如果失败：

1. `HeartbeatAppService` 先尝试重新注册
2. 再重发一次心跳

这条链保证了节点代理和调度器之间能进行“自恢复式重新握手”。

### 15.3 任务接收与异步执行链路

调度器下发任务后的链路如下：

1. `scheduler-service` 调 `POST /internal/dispatch-task`
2. `DispatchController.dispatch()`
3. `TaskDispatchManager.acceptTask(request)`
4. 校验并发和重复任务
5. `ExecutionContextAssembler.fromDispatchRequest(request)`
6. `taskReportManager.onTaskAccepted(taskId)`
7. 提交给线程池异步执行
8. `TaskExecuteManager.execute(context)`

这说明：

- 接口层只负责接请求；
- 真正执行一定是异步的；
- 每个运行任务都先登记到 `TaskRuntimeRegistry`。

### 15.4 本地执行链路

任务真正执行时的链路如下：

1. `TaskExecuteManager.prepareWorkDir(context)`
2. `WorkDirManager.prepareTaskDirs(context)`
3. `TaskExecuteManager.prepareInputFiles(context)`
4. `InputFilePrepareService.prepare(context)`
5. `taskReportManager.reportRunning(context)`
6. `ExecutorSelectDomainService.selectExecutor(context)`
7. 对应执行器：
   - `OpenFoamExecutor`
   - `CalculixExecutor`
   - `MockExecutor`
8. `CommandBuilder.buildCommand(context)`
9. `ProcessRunner.run(...)`
10. 日志逐行通过 `TaskReportManager.pushLog(...)` 回传
11. 完成后 `ResultFileCollector.collect(context)`
12. `TaskReportManager.reportSuccess(...)` 或 `reportFail(...)`
13. `TaskReportManager.completeTask(taskId)`

### 15.5 输入文件准备链路

输入文件准备链路如下：

1. 从调度器传来的 `DispatchTaskRequest.inputFiles` 取出每个文件的 `storagePath`
2. `PathMappingSupport.toLinuxPath()` 把平台路径转换为节点本地路径
3. 复制到 `inputDir`
4. 如果是压缩包：
   - 自动解压
   - 做 Zip Slip 防护
   - 根据顶层目录情况调整 `taskDir`

这条链是当前“Windows 平台上传压缩包 -> Ubuntu 节点执行”的关键实现。

### 15.6 日志、结果与状态回传链路

回传链路如下：

1. 执行开始时：`reportRunning()`
2. 执行过程中：`pushLog()`
3. 执行成功后：
   - `reportResultSummary()`
   - `reportResultFile()` 循环回传
   - `markFinished()`
4. 执行失败后：`markFailed()`
5. 执行取消后：`reportStatus(..., "CANCELED", ...)`
6. 执行完成后：`updateRunningCount(-1)`

每次回传都会带：

- `nodeId`
- `X_NODE_TOKEN`

因此 `task-service` 可以校验回传身份是否合法。

### 15.7 取消任务链路

取消链路如下：

1. `scheduler-service` 调 `POST /internal/cancel-task`
2. `DispatchController.cancel()`
3. `TaskDispatchManager.cancelTask(request)`
4. `TaskRuntimeRegistry.cancel(taskId, reason)`
5. 若任务正在运行：
   - 标记 `cancelRequested = true`
   - 保存取消原因
   - 强制销毁底层进程
   - 中断执行线程
6. `TaskExecuteManager.execute()` 捕获取消态
7. `TaskReportManager.reportCanceled(context, reason)`

当前取消策略是“强制终止”，不是“优雅停止”。

---

## 16. 设计文档与当前实现的对照

### 16.1 已经较好落地的部分

结合 `需求分析与系统设计.md`、`后端设计.md`、`开发文档.md`，当前 `node-agent` 已经较好落地了这些设计目标：

- 节点启动自动注册；
- 周期性心跳上报；
- 节点运行负载采集；
- 接收调度器下发任务；
- 支持执行器抽象与多执行器分流；
- 工作目录准备；
- 输入文件复制与 zip 解压；
- 本机进程执行；
- 标准输出/错误输出日志回传；
- 执行结果摘要与结果文件回传；
- 中途取消与超时处理；
- 节点回传时使用 token 鉴权；
- Linux/Windows 路径映射；
- 节点临时目录定期清理。

### 16.2 当前是“最小执行闭环”而不是“完整执行平台”的部分

当前实现仍明显属于基础版执行代理：

- 只有本机进程执行，没有真正的 `CONTAINER` 执行模式；
- 没有和 `solver-service.execMode` 做完整联动；
- 没有对象存储或稳定共享存储方案，仍是本地路径复制；
- 结果收集只是简单扫描 `outputDir`；
- 没有更复杂的结果解析器机制；
- 没有执行重试、断点恢复、沙箱隔离；
- 没有统一的任务级资源限制和监控；
- 没有更强的日志缓冲、批量推送或背压控制；
- 没有持久化运行态，重启后活动任务上下文会丢失。

### 16.3 当前是“预留/占位”的部分

当前这些类更偏预留位：

- `ExecutorConfig.java`
- `FeignClientConfig.java`
- `ExecutionStageEnum.java`
- `LogPushBuffer.java`
- `WorkDirManager.cleanupTaskDirs()`

它们适合在文档里描述成“为后续增强预留的扩展点”，而不是已经形成完整能力。

### 16.4 当前实现里几个必须写清楚的细节

#### 1. 节点实际上报求解器能力来自本地配置

当前 `node-agent` 不是扫描系统软件自动发现求解器，而是：

- 从 `application.yml` 或环境变量读 `solver-ids`
- 再根据 `solver-versions` 组装版本号
- 注册时上报给调度器

因此它是“配置声明 + 注册上报”的实现，不是“自动探测安装环境”的实现。

#### 2. 调度器已经预占并发，节点侧主要负责回传实时运行数和结束后的 `-1`

当前并发数更新逻辑不是完全由节点侧单独决定，而是两边协同：

- `scheduler-service` 在选中节点时先 `runningCount + 1`
- `node-agent` 心跳持续上报真实 `runningCount`
- 任务完成时 `node-agent` 再主动发 `updateRunningCount(-1)`

这点在文档里最好明确，否则容易误以为节点接收任务时为什么没有显式 `+1`。

#### 3. 命令执行目前是 shell 字符串替换，不是结构化参数执行

`CommandBuilder` 当前只是把变量替换进 `commandTemplate`，再交给：

- Windows：`cmd /c`
- Linux：`/bin/sh -c`

执行。

这说明当前实现灵活，但也意味着：

- 参数转义能力较弱；
- 复杂命令更依赖模板书写正确；
- 安全控制和可审计性还可以进一步增强。

#### 4. 调度器下发到节点代理的接口当前没有额外鉴权

当前 `scheduler-service -> node-agent` 的 `/internal/dispatch-task`、`/internal/cancel-task` 调用，没有像节点回传到 `task-service` 那样携带 `X_NODE_TOKEN`。

所以当前安全模型更接近：

- 节点回传到平台：有 token 校验
- 平台下发到节点：默认受信任网络

这在文档里必须如实说明。

#### 5. 取消任务当前是强制 kill，不是优雅停止

`TaskRuntimeRegistry.cancel()` 会：

- 中断线程
- `process.destroyForcibly()`

这对基础版原型是可接受的，但在真实求解器场景下，后续可能需要按求解器支持情况提供更细的退出策略。

---

## 17. 推荐的阅读顺序

### 17.1 先看对外入口

建议先看：

- `DispatchController.java`
- `DispatchTaskRequest.java`
- `CancelTaskRequest.java`

这样可以先理解节点代理从调度器接收的到底是什么。

### 17.2 再看执行编排主链

接着看：

- `TaskDispatchManager.java`
- `TaskExecuteManager.java`
- `TaskReportManager.java`
- `TaskRuntimeRegistry.java`

这四个类基本就是整个节点代理的“主心骨”。

### 17.3 再看执行器和进程基础设施

然后看：

- `ExecutorSelectDomainService.java`
- `OpenFoamExecutor.java`
- `CalculixExecutor.java`
- `MockExecutor.java`
- `ProcessRunner.java`
- `CommandBuilder.java`

这样能看清楚任务是怎么从“上下文”变成“本机进程”的。

### 17.4 最后看注册、心跳、回传与文件路径

最后看：

- `NodeRegisterAppService.java`
- `HeartbeatAppService.java`
- `SchedulerNodeClientImpl.java`
- `TaskReportClientImpl.java`
- `InputFilePrepareService.java`
- `PathMappingSupport.java`

这样就能把“执行端”和“平台端”的联动关系串起来。

---

## 18. 当前结论

`node-agent` 当前已经完成了“节点代理最小执行闭环”的核心实现：

- 节点能注册、能心跳；
- 调度器能把任务下发到节点；
- 节点能准备工作目录和输入文件；
- 节点能选择执行器并启动本地进程；
- 日志、状态、结果摘要和结果文件都能回传；
- 取消、超时、异常退出都有基本处理。

因此，从毕设或课程设计原型的标准看，它已经足以支撑：

> “中心调度 + 多节点执行 + 执行过程回传”的基础分布式执行闭环。

但它还不是生产级执行平台代理。当前更准确的定位是：

> 一个面向 CAE 仿真场景、以本机进程执行为主的基础型节点执行代理。

如果下一步继续增强，最值得优先补的方向是：

- 容器执行模式；
- 更可靠的共享存储或对象存储；
- 更细粒度的结果收集与解析；
- 更强的调度器到节点代理鉴权；
- 更安全的命令构建与参数转义；
- 更稳健的执行隔离、重试与恢复机制。
