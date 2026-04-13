# scheduler-service 模块说明文档

## 1. 文档目的

这份文档延续 `user-service.md`、`solver-service.md`、`task-service.md` 的写法，不只说明“调度服务是做什么的”，还要把 `scheduler-service` 拆到“模块根目录 -> 分层目录 -> 具体代码文件”的粒度，便于下面几类工作直接定位：

- 写毕业论文中的模块设计说明时，能快速对应到真实代码目录；
- 联调节点注册、心跳、调度、节点管理时，知道应该看哪个类；
- 分析 `compute_node`、`node_solver_capability`、`schedule_record` 三张表时，知道各自落在哪个仓储与 Mapper；
- 区分“设计文档中的目标能力”和“当前代码已经落地的能力”；
- 后续继续改造调度策略、节点鉴权、失败补偿、多实例调度时，知道从哪里接着做。

分析范围以 `scheduler-service/src/main/java` 与 `scheduler-service/src/main/resources` 为主，`target/` 这类编译产物不作为设计分析对象。

---

## 2. 模块定位

`scheduler-service` 是整个平台中的“节点管理中心 + 调度编排中心”。

它主要回答四类问题：

1. 当前有哪些计算节点接入平台，它们是否在线、是否启用、负载怎样；
2. 每个节点支持哪些求解器能力，这些能力当前是否允许参与调度；
3. 待调度任务应该被分配到哪个节点；
4. 每次调度成功或失败时，系统如何留下可审计的调度记录。

从整个平台分工看：

- `user-service` 负责“谁在使用系统”；
- `solver-service` 负责“平台支持哪些求解器、模板和文件规则”；
- `task-service` 负责“任务如何创建、校验、提交、流转、回传”；
- `scheduler-service` 负责“任务应该派给哪个节点执行”；
- `node-agent` 负责“节点如何真正执行任务并回传状态”。

所以，`scheduler-service` 不是一个简单 CRUD 服务，而是连接 `task-service` 和 `node-agent` 的控制面服务。

---

## 3. 模块根目录与源码入口

### 3.1 模块根目录

模块根目录是：

```text
scheduler-service/
```

当前最重要的几个入口位置是：

- `scheduler-service/pom.xml`
- `scheduler-service/src/main/java/com/example/cae/scheduler/`
- `scheduler-service/src/main/resources/application.yml`

### 3.2 不需要作为设计分析重点的目录

下面这些目录不属于源代码设计主体：

```text
scheduler-service/target/
```

`target/` 是 Maven 编译产物，不应写进模块设计说明的“源码结构”部分。

---

## 4. 分层与文件夹映射

| 分层 | 对应文件夹 | 作用 |
| --- | --- | --- |
| 启动入口层 | `scheduler-service/src/main/java/com/example/cae/scheduler/` | Spring Boot 启动入口，开启定时调度 |
| 配置层 | `scheduler-service/src/main/java/com/example/cae/scheduler/config/` | Spring Bean、远程地址、调度基础配置 |
| 接口层 | `scheduler-service/src/main/java/com/example/cae/scheduler/interfaces/` | 对外接口、内部接口、请求对象、响应对象 |
| 应用层 | `scheduler-service/src/main/java/com/example/cae/scheduler/application/` | 调度编排、节点管理编排、Facade、Manager、定时任务 |
| 领域层 | `scheduler-service/src/main/java/com/example/cae/scheduler/domain/` | 节点模型、能力模型、调度记录模型、规则与策略抽象 |
| 基础设施层 | `scheduler-service/src/main/java/com/example/cae/scheduler/infrastructure/` | 数据持久化、远程调用、运行时支持组件 |
| 支撑层 | `scheduler-service/src/main/java/com/example/cae/scheduler/support/` | 日志写入等预留的通用支撑组件 |
| 资源配置层 | `scheduler-service/src/main/resources/` | 端口、数据库、远程服务地址等运行配置 |

如果只想快速定位代码，可以这样记：

- 看管理端和节点端接口：`interfaces/controller`
- 看服务间内部接口：`interfaces/internal`
- 看调度主流程：`application/scheduler`、`application/service`
- 看节点注册和心跳：`application/service/NodeAppService`
- 看选节点策略：`domain/strategy`
- 看数据库表落地：`infrastructure/persistence`
- 看对 `task-service` 和 `node-agent` 的调用：`infrastructure/client`

---

## 5. 模块级结构总览

当前源码结构如下：

```text
scheduler-service/
├── src/main/java/com/example/cae/scheduler/
│   ├── SchedulerApplication.java
│   ├── config/
│   │   ├── FeignClientConfig.java
│   │   ├── SchedulerRemoteServiceProperties.java
│   │   ├── SchedulerServiceConfig.java
│   │   └── SchedulingConfig.java
│   ├── interfaces/
│   │   ├── controller/
│   │   │   ├── NodeAgentController.java
│   │   │   ├── NodeController.java
│   │   │   └── ScheduleController.java
│   │   ├── internal/
│   │   │   ├── InternalSchedulerController.java
│   │   │   ├── NodeHeartbeatController.java
│   │   │   └── NodeRegisterController.java
│   │   ├── request/
│   │   └── response/
│   ├── application/
│   │   ├── assembler/
│   │   ├── facade/
│   │   ├── manager/
│   │   ├── scheduler/
│   │   └── service/
│   ├── domain/
│   │   ├── enums/
│   │   ├── model/
│   │   ├── repository/
│   │   ├── service/
│   │   └── strategy/
│   ├── infrastructure/
│   │   ├── client/
│   │   │   └── impl/
│   │   ├── persistence/
│   │   │   ├── entity/
│   │   │   ├── mapper/
│   │   │   └── repository/
│   │   └── support/
│   └── support/
│       └── ScheduleLogWriter.java
└── src/main/resources/application.yml
```

这个结构和调度服务的职责是匹配的。相对 `user-service` 这类偏标准管理服务，`scheduler-service` 明显多出：

- `application/scheduler/`：定时调度和离线巡检；
- `domain/strategy/`：调度策略抽象；
- `infrastructure/client/`：与 `task-service`、`node-agent` 的双向调用；
- `interfaces/internal/`：给内部服务调用的协作接口。

---

## 6. 根目录文件说明

### 6.1 `scheduler-service/pom.xml`

模块的 Maven 描述文件，作用包括：

- 声明当前模块是 `scheduler-service`；
- 继承父工程 `cae-taskmanager-backend`；
- 引入 `spring-boot-starter-web`；
- 引入 `mybatis-plus-spring-boot3-starter`；
- 引入 `mysql-connector-j`；
- 引入共享模块 `common-lib`；
- 配置 `spring-boot-maven-plugin` 打包为可运行 Jar。

当前没有引入消息队列、服务发现、OpenFeign、分布式锁等依赖，这也直接反映了当前实现仍是“单实例轮询 + 同步 HTTP 调用”的基础版调度器。

### 6.2 `scheduler-service/src/main/resources/application.yml`

运行配置文件，主要定义：

- 服务端口：默认 `8084`
- 服务名：`scheduler-service`
- 数据库连接：默认连接 `scheduler_db`
- 远程任务服务地址：`cae.remote.task-base-url`
- 节点代理访问协议：`cae.remote.node-agent-scheme`

也就是说，调度器当前通过配置项直接拼接 `task-service` 和 `node-agent` 地址，而不是通过注册中心发现。

---

## 7. 启动入口层

### 7.1 对应文件夹

启动入口层对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/
```

### 7.2 文件说明

#### `SchedulerApplication.java`

这是调度服务的 Spring Boot 启动类，作用有两点：

- 启动整个 `scheduler-service`；
- 通过 `@EnableScheduling` 开启 Spring 定时任务能力。

没有这个类，`TaskScheduleJob` 和 `NodeOfflineCheckJob` 都不会自动执行，所以它是调度主循环真正的启动入口。

---

## 8. 配置层

### 8.1 对应文件夹

配置层对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/config/
```

### 8.2 文件说明

#### `SchedulerServiceConfig.java`

作用：注册 `RestTemplate` Bean。

当前调度服务所有跨服务调用都走这个 `RestTemplate`，包括：

- 调 `task-service` 获取待调度任务；
- 调 `task-service` 回写任务状态；
- 调 `node-agent` 下发任务；
- 调 `node-agent` 请求取消任务。

这里还统一设置了：

- 连接超时 `3000ms`
- 读取超时 `10000ms`

#### `SchedulerRemoteServiceProperties.java`

作用：绑定远程服务配置项，前缀是 `cae.remote`。

当前包含两个核心配置：

- `taskBaseUrl`：`task-service` 基础地址
- `nodeAgentScheme`：访问节点代理时默认使用的协议，如 `http`

`NodeAgentClientStub` 和 `TaskClientStub` 都依赖它。

#### `SchedulingConfig.java`

作用：调度相关配置占位类。

当前类是空实现，说明架构上预留了“集中放置调度器配置”的位置，但首版尚未放入线程池、策略切换、分布式锁等配置。

#### `FeignClientConfig.java`

作用：Feign 配置占位类。

当前也是空实现。结合 `pom.xml` 可以看出，当前工程实际上并没有使用 OpenFeign，而是保留了将来切换到声明式远程调用的扩展点。

---

## 9. 接口层

### 9.1 对应文件夹

接口层对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/interfaces/
```

它又分为四部分：

- `controller/`：对外接口
- `internal/`：内部服务接口
- `request/`：请求对象
- `response/`：响应对象

### 9.2 对外控制器目录

对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/interfaces/controller/
```

#### `NodeController.java`

管理员节点管理接口，对外暴露：

- `GET /api/nodes`：分页查询节点
- `GET /api/nodes/{nodeId}`：查看节点详情
- `POST /api/nodes/{nodeId}/status`：启用/禁用节点
- `POST /api/nodes/{nodeId}/solvers/{solverId}/status`：启用/禁用节点-求解器能力
- `GET /api/nodes/{nodeId}/solvers`：查看节点支持的求解器能力

这是“节点管理页面”最核心的控制器。

#### `ScheduleController.java`

调度记录查询接口，对外暴露：

- `GET /api/schedules`：分页查询调度记录
- `GET /api/tasks/{taskId}/schedules`：查看某个任务的全部调度记录

这是“调度监控页面”的主要数据入口。

#### `NodeAgentController.java`

面向 `node-agent` 的公开接入接口，对外暴露：

- `POST /api/node-agent/register`：节点注册
- `POST /api/node-agent/heartbeat`：节点心跳

它的两个关键职责是：

- 节点首次接入时返回平台分配的 `nodeId` 与 `nodeToken`
- 后续心跳时要求节点携带 `X-Node-Token` 请求头进行鉴权

也就是说，当前节点接入已经不是“完全匿名上报”，而是“注册换 token，心跳带 token”。

### 9.3 内部控制器目录

对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/interfaces/internal/
```

#### `NodeRegisterController.java`

旧式内部节点注册接口：

- `POST /internal/scheduler/nodes/register`

它接收的是 `NodeRegisterRequest`，只上报 `solverIds`，不带版本信息。当前主链路已经更多走 `NodeAgentController`，但这个接口仍保留，适合兼容更简单的内部调用方式。

#### `NodeHeartbeatController.java`

旧式内部节点心跳接口：

- `POST /internal/scheduler/nodes/heartbeat`

和 `NodeAgentController` 相比，它不要求节点 token，更像是内部受信任调用接口。

#### `InternalSchedulerController.java`

这是调度服务给其他内部服务使用的协作接口，包含：

- `GET /internal/nodes/available`：按求解器查询可用节点
- `POST /internal/schedules`：写入调度记录
- `POST /internal/nodes/{nodeId}/running-count`：调整节点运行任务数
- `POST /internal/nodes/{nodeId}/cancel-task`：向节点发起取消任务
- `GET /internal/nodes/{nodeId}/token/verify`：校验节点 token

其中最关键的是最后一个接口。`task-service` 在接收 `node-agent` 状态回传时，会调用这里确认“当前回传者是否真的是绑定该任务的节点”。

### 9.4 请求对象目录

对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/interfaces/request/
```

#### `NodePageQueryRequest.java`

节点分页查询条件对象，包含：

- 页码、页大小
- 节点名
- 在线状态
- 启用状态
- 求解器筛选条件

这个类有一个很重要的辅助方法 `getSolverIdAsLong()`，它会把前端传来的字符串 `solverId` 转成 `Long`，并兼容：

- `null`
- 空字符串
- `"undefined"`
- `"null"`
- 非法数字

这说明当前实现已经考虑到了前端查询参数经常出现的“字符串脏值”问题。

#### `SchedulePageQueryRequest.java`

调度记录分页查询条件对象，包含：

- 页码、页大小
- `taskId`
- `nodeId`
- `scheduleStatus`
- `strategyName`
- `startTime`
- `endTime`

用于调度审计页面的筛选。

#### `NodeRegisterRequest.java`

内部节点注册请求对象，字段包括：

- `nodeCode`
- `nodeName`
- `host`
- `maxConcurrency`
- `solverIds`

这是简化版节点注册模型，不带求解器版本。

#### `NodeAgentRegisterRequest.java`

节点代理公开注册请求对象，是当前更完整的注册模型，字段包括：

- `nodeCode`
- `nodeName`
- `host`
- `maxConcurrency`
- `solvers`

其中 `solvers` 是一个列表，每项是 `SolverItem`，包含：

- `solverId`
- `solverVersion`

这正对应当前系统关于“节点负责上报事实能力，平台保存并授权调度”的实际实现。

#### `NodeHeartbeatRequest.java`

节点心跳请求对象，字段包括：

- `nodeId`
- `cpuUsage`
- `memoryUsage`
- `runningCount`

它只负责上报运行时负载，不负责修改求解器能力。

#### `InternalScheduleRecordRequest.java`

内部调度记录写入请求对象，字段包括：

- `taskId`
- `nodeId`
- `strategyName`
- `scheduleStatus`
- `scheduleMessage`

适合服务间主动补记调度审计信息。

#### `NodeTaskCancelRequest.java`

内部取消节点任务请求对象，字段包括：

- `taskId`
- `reason`

用于把取消请求从调度服务继续传给具体节点代理。

#### `UpdateNodeStatusRequest.java`

节点启用状态修改请求对象，只包含：

- `enabled`

用于管理员控制 `compute_node.enabled`。

#### `UpdateNodeSolverStatusRequest.java`

节点-求解器能力启用状态修改请求对象，只包含：

- `enabled`

用于管理员控制 `node_solver_capability.enabled`。

#### `UpdateRunningCountRequest.java`

节点运行任务数调整请求对象，只包含：

- `delta`

供节点代理在任务开始/结束时调整占用数。

### 9.5 响应对象目录

对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/interfaces/response/
```

#### `AvailableNodeResponse.java`

返回给内部服务的“可用节点摘要”，字段包括：

- 节点 ID、编码、名称
- 主机地址
- 当前运行数
- 最大并发数

#### `NodeAgentRegisterResponse.java`

返回给节点代理的注册响应，字段包括：

- `nodeId`
- `nodeToken`

这是节点后续心跳和任务回传鉴权的基础。

#### `NodeDetailResponse.java`

节点详情响应对象，包含：

- 节点基础信息
- 在线状态和启用状态
- 并发、CPU、内存、最近心跳
- 节点支持的求解器能力列表

#### `NodeListItemResponse.java`

节点列表页响应对象，提供列表页展示所需的节点基础状态与负载信息。

#### `NodeSolverResponse.java`

节点某个求解器能力的摘要对象，字段包括：

- `solverId`
- `solverVersion`
- `enabled`

#### `ScheduleRecordResponse.java`

调度记录响应对象，字段包括：

- 调度记录 ID
- 任务 ID
- 节点 ID
- 调度策略名
- 调度状态
- 调度说明
- 创建时间

---

## 10. 应用层

### 10.1 对应文件夹

应用层对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/application/
```

这一层是调度服务真正的编排层，负责把“接口输入”“领域规则”“仓储操作”“远程调用”串成完整业务流程。

### 10.2 组装器目录

对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/application/assembler/
```

#### `NodeAssembler.java`

负责节点对象转换，主要做三类转换：

- `NodeRegisterRequest -> ComputeNode`
- `ComputeNode -> NodeDetailResponse`
- `ComputeNode <-> ComputeNodePO`

它让“接口对象、领域对象、持久化对象”之间保持解耦。

#### `ScheduleAssembler.java`

负责调度记录对象转换，主要做三类转换：

- `ScheduleRecord -> ScheduleRecordResponse`
- 构造新的 `ScheduleRecord`
- `ScheduleRecord <-> ScheduleRecordPO`

`confirmScheduleSuccess()` 和 `recordScheduleFailure()` 最终都会通过它构造调度记录对象。

### 10.3 Facade 目录

对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/application/facade/
```

#### `NodeFacade.java`

节点管理对外门面，负责把控制器请求统一转交给 `NodeManageManager`。

它封装的主要能力包括：

- 注册
- 心跳
- 节点分页
- 节点详情
- 节点启停
- 节点求解器能力启停
- 获取节点 token

#### `ScheduleFacade.java`

调度记录查询门面，负责把控制器请求统一转给 `TaskScheduleManager`。

### 10.4 Manager 目录

对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/application/manager/
```

#### `NodeManageManager.java`

节点管理编排器，是 `NodeFacade` 和 `NodeAppService` 之间的一层轻量桥接。

当前它的逻辑比较薄，主要是统一节点管理相关入口，方便后续把更复杂的跨服务协作、日志记录、补偿动作都收口到这里。

#### `TaskScheduleManager.java`

调度编排器，是 `ScheduleFacade`、`TaskScheduleJob` 与 `ScheduleAppService` 之间的桥接层。

当前封装能力包括：

- 选节点调度
- 记录调度成功
- 记录调度失败
- 释放节点占位
- 向节点发取消请求
- 查询调度记录

### 10.5 定时任务目录

对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/application/scheduler/
```

#### `TaskScheduleJob.java`

这是调度器主循环，是真正“让任务被派出去”的类。

当前流程是：

1. 每隔 `scheduler.task-schedule-interval-ms` 拉取一次待调度任务，默认 5 秒；
2. 通过 `taskClient.listPendingTasks(20)` 一次拉最多 20 个 `QUEUED` 任务；
3. 对每个任务调用 `taskScheduleManager.schedule(task)` 选节点；
4. 调 `task-service` 将任务标记为 `SCHEDULED`；
5. 调 `node-agent` 下发任务；
6. 再调 `task-service` 把任务标记为 `DISPATCHED`；
7. 写入调度成功记录。

如果中间异常：

- 若已占用节点，会释放 `runningCount`；
- 若任务已经被标记为 `SCHEDULED`，会把任务写成 `FAILED`；
- 无论如何都会记录一条调度失败记录。

这说明当前实现已经有“占位释放”和“调度失败审计”，但还没有“自动重试调度”。

#### `NodeOfflineCheckJob.java`

节点离线巡检定时任务。

默认每隔 `scheduler.node-offline-check-interval-ms` 执行一次，默认 15 秒。它会调用 `NodeHeartbeatChecker`，把超过 30 秒未上报心跳的节点判定为离线。

### 10.6 应用服务目录

对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/application/service/
```

#### `NodeAppService.java`

这是节点管理主服务，也是本模块最重要的类之一。

它负责：

- 节点注册
- 节点代理注册
- 心跳处理
- 节点分页与详情
- 节点启停
- 节点求解器能力启停
- 可用节点筛选
- `runningCount` 调整
- 节点 token 获取与校验
- 节点能力同步

它里面最关键的方法如下。

##### `registerNode(NodeRegisterRequest request)`

处理内部简化版节点注册：

- 若节点已存在，更新名称、主机、并发数；
- 若没有 token，则补发 token；
- 标记节点上线并刷新心跳时间；
- 用 `replaceNodeCapabilitiesWithDetails()` 重建节点能力；
- 若节点不存在，则新建节点并默认启用。

##### `registerNodeFromAgent(NodeAgentRegisterRequest request)`

处理 `node-agent` 的公开注册，这是当前主链路。

它会先把代理上报对象转成 `NodeRegisterRequest` 完成基础节点注册，再用 `mergeCapabilities()` 按带版本的 `solvers` 列表重建能力表。

这个方法的实现细节非常重要：

- 节点实际上报 `solverId + solverVersion`；
- 平台据此重建 `node_solver_capability`；
- 如果该能力原本已有 `enabled` 状态，则会保留管理员原来的开关；
- 如果某能力本次不再上报，则会因为“先删后插”被移除，避免脏数据残留。

这和需求分析文档中的“两层责任”设计是一致的。

##### `heartbeat(NodeHeartbeatRequest request, String nodeToken)`

处理节点心跳：

- 校验心跳字段合法性；
- 按 `nodeId` 查节点；
- 若传入 token，则校验 token；
- 更新 CPU、内存、运行数、最近心跳时间；
- 将节点重新标记为 `ONLINE`。

心跳不会改求解器能力，只更新运行态信息。

##### `pageNodes(NodePageQueryRequest request)`

分页查询节点列表，调用 `computeNodeRepository.page()`，再转换成 `NodeListItemResponse`。

##### `getNodeDetail(Long nodeId)`

查询单个节点详情，并附带加载该节点的全部求解器能力。

##### `updateNodeStatus(Long nodeId, UpdateNodeStatusRequest request)`

修改 `compute_node.enabled`，控制节点整体是否允许参与调度。

##### `updateNodeSolverStatus(Long nodeId, Long solverId, UpdateNodeSolverStatusRequest request)`

修改 `node_solver_capability.enabled`，控制某节点某求解器能力是否允许参与调度。

##### `listAvailableNodes(Long solverId)`

根据求解器 ID 返回可参与调度的节点。当前筛选条件是：

- 节点在线
- 节点整体启用
- 节点未满载
- 节点存在对应求解器能力
- 该能力已启用

##### `updateRunningCount(Long nodeId, Integer delta)`

按增量调整节点当前运行任务数，供节点代理在任务开始/结束时上报。

##### `getNodeToken(Long nodeId)` / `validateNodeToken(Long nodeId, String nodeToken)`

提供节点 token 查询和校验能力，支撑任务回传鉴权链路。

#### `ScheduleAppService.java`

这是调度主服务，是本模块另一个核心类。

它负责：

- 按求解器筛选候选节点
- 选择目标节点
- 预占节点并发
- 写调度成功/失败记录
- 释放节点占位
- 向节点发送取消任务请求
- 分页查询调度记录

关键方法如下。

##### `scheduleTask(TaskDTO task)`

这是“选节点”的主方法，流程是：

1. 校验 `taskId`、`solverId` 是否存在；
2. 读取所有 `ONLINE` 节点；
3. 根据 `solverId` 读取对应的节点能力；
4. 通过 `ScheduleDomainService.filterAvailableNodes()` 筛出满足条件的节点；
5. 调 `ScheduleStrategy.selectNode()` 选择最终节点；
6. 先把节点的 `runningCount + 1` 作为并发占位；
7. 返回选中的 `nodeId`。

这里有两个很关键的实现细节：

- 调度服务本身只负责“选哪个节点”，不负责排序任务队列先后；任务顺序来自 `task-service` 的待调度接口；
- 节点在真正下发前就会先占用并发，这样可以降低短时间内重复选中同一节点的概率。

##### `confirmScheduleSuccess(Long taskId, Long nodeId, String scheduleMessage)`

写入一条成功调度记录，策略名目前固定写 `FCFS_LEAST_LOAD`。

##### `recordScheduleFailure(Long taskId, Long nodeId, String scheduleMessage)`

写入一条失败调度记录。

注意：这里允许 `nodeId` 为空，所以数据库中的 `schedule_record.node_id` 最好与代码保持一致，允许为空。

##### `releaseNodeReservation(Long nodeId)`

释放节点的并发占位，用于调度失败后的补偿。

##### `cancelTaskOnNode(Long nodeId, Long taskId, String reason)`

向指定节点代理发送取消任务请求。

##### `recordSchedule(InternalScheduleRecordRequest request)`

提供内部手工写调度记录的能力。

##### `listByTaskId(Long taskId)` / `pageRecords(SchedulePageQueryRequest request)`

调度记录查询能力。

---

## 11. 领域层

### 11.1 对应文件夹

领域层对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/domain/
```

### 11.2 枚举目录

对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/domain/enums/
```

#### `ScheduleStatusEnum.java`

定义调度状态枚举：

- `SUCCESS`
- `FAILED`

当前这个枚举已经存在，但应用层写记录时仍主要直接写字符串常量，如 `"SUCCESS"`、`"FAILED"`、`"UNKNOWN"`，说明枚举还没有在整个模块里统一用起来。

### 11.3 领域模型目录

对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/domain/model/
```

#### `ComputeNode.java`

计算节点领域模型，对应 `compute_node` 表。

核心字段包括：

- 节点编码、名称、主机
- 节点 token
- 在线状态
- 启用状态
- 最大并发
- 当前运行数
- CPU、内存使用率
- 最近心跳时间

它还封装了几个很重要的领域行为：

- `markOnline()`
- `markOffline()`
- `refreshHeartbeat(...)`
- `enable()`
- `disable()`
- `canDispatch()`

其中 `canDispatch()` 是当前判断“节点是否可继续接任务”的基础规则。

#### `NodeSolverCapability.java`

节点求解器能力领域模型，对应 `node_solver_capability` 表。

核心字段包括：

- `nodeId`
- `solverId`
- `solverVersion`
- `enabled`

它体现的是“这个节点具备某个求解器能力”，不是全局求解器定义本身。

#### `ScheduleRecord.java`

调度记录领域模型，对应 `schedule_record` 表。

它只记录调度审计信息，不保存任务完整详情。核心字段包括：

- `taskId`
- `nodeId`
- `strategyName`
- `scheduleStatus`
- `scheduleMessage`
- `createdAt`

### 11.4 仓储抽象目录

对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/domain/repository/
```

#### `ComputeNodeRepository.java`

计算节点仓储抽象，定义：

- 按 ID、节点编码、token 查询
- 保存与更新
- 分页查询
- 按状态查询节点列表

#### `NodeSolverCapabilityRepository.java`

节点能力仓储抽象，定义：

- 按节点查询能力
- 按求解器查询能力
- 按节点替换能力列表
- 按节点带详情替换能力列表
- 更新单条能力开关

#### `ScheduleRecordRepository.java`

调度记录仓储抽象，定义：

- 保存调度记录
- 分页查询调度记录
- 按任务查询调度记录

### 11.5 领域服务目录

对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/domain/service/
```

#### `NodeDomainService.java`

节点领域规则服务，主要负责：

- 校验注册请求是否合法
- 校验心跳请求是否合法
- 判断节点是否满足派发条件

它把节点层面的“业务校验规则”从应用层里抽出来了。

#### `ScheduleDomainService.java`

调度领域规则服务，主要负责：

- 根据求解器能力列表过滤出可参与某任务调度的节点

这里体现的规则是：

- 能力必须属于目标 `solverId`
- 能力必须启用
- 节点本身必须可派发

### 11.6 调度策略目录

对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/domain/strategy/
```

#### `ScheduleStrategy.java`

调度策略接口，定义统一入口：

- `ComputeNode selectNode(TaskDTO task, List<ComputeNode> nodes)`

以后如果要扩展“优先级调度、标签调度、亲和性调度、失败重调度”，都应继续沿着这个抽象扩展。

#### `FcfsLeastLoadStrategy.java`

当前唯一落地的调度策略实现。

它的实际行为是：

- 在候选节点里，优先选 `runningCount` 最小的；
- 若相同，再比较 CPU 使用率；
- 若还相同，再比较内存使用率。

需要注意两点：

- 它并不负责队列的先来先服务，任务顺序来自 `task-service` 返回的 `QUEUED` 列表排序；
- “FCFS” 更多体现为“任务列表按优先级和提交时间排序后被依次处理”，而不是这个类内部自己做任务排序。

---

## 12. 基础设施层

### 12.1 对应文件夹

基础设施层对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/infrastructure/
```

### 12.2 远程调用目录

对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/infrastructure/client/
```

#### `TaskClient.java`

任务服务客户端抽象，定义：

- 获取待调度任务
- 标记任务为 `SCHEDULED`
- 标记任务为 `DISPATCHED`
- 标记任务调度失败
- 在节点离线时批量标记相关任务失败

#### `NodeAgentClient.java`

节点代理客户端抽象，定义：

- 下发调度任务
- 取消节点任务

#### `impl/TaskClientStub.java`

`TaskClient` 的当前实现，使用 `RestTemplate` 调用 `task-service`。

它对应的接口有：

- `GET /internal/tasks/queued`
- `POST /internal/tasks/{taskId}/mark-scheduled`
- `POST /internal/tasks/{taskId}/mark-dispatched`
- `POST /internal/tasks/{taskId}/dispatch-failed`
- `POST /internal/tasks/node-offline/fail`

当前说明调度服务和任务服务的联动是“同步 HTTP 回写”，不是异步消息。

#### `impl/NodeAgentClientStub.java`

`NodeAgentClient` 的当前实现，使用 `RestTemplate` 调用具体节点代理。

它的关键职责包括：

- 根据 `nodeId` 先查出节点主机地址；
- 自动拼出节点代理基础地址；
- 调 `POST /internal/dispatch-task` 下发任务；
- 调 `POST /internal/cancel-task` 取消任务；
- 校验节点代理返回的 `accepted` 标志；
- 若节点 host 为空或节点不存在，直接报错。

它还会在下发时组装完整调度载荷，包括：

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

### 12.3 持久化目录

对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/infrastructure/persistence/
```

#### 12.3.1 `entity/`

##### `ComputeNodePO.java`

`compute_node` 的持久化对象。

##### `NodeSolverCapabilityPO.java`

`node_solver_capability` 的持久化对象。

##### `ScheduleRecordPO.java`

`schedule_record` 的持久化对象。

#### 12.3.2 `mapper/`

##### `ComputeNodeMapper.java`

计算节点 MyBatis Mapper，负责：

- 按 ID/编码/token 查询节点
- 插入节点
- 更新节点
- 分页查询节点
- 按状态查询节点

其中分页 SQL 支持按 `solverIdAsLong` 过滤已启用能力节点。

##### `NodeSolverCapabilityMapper.java`

节点能力 MyBatis Mapper，负责：

- 按节点查能力
- 按求解器查能力
- 删除某节点全部能力
- 批量插入节点能力
- 批量插入带版本的节点能力
- 更新单条能力的版本和启用状态

当前“能力重建”采用的是“先删后插”策略。

##### `ScheduleRecordMapper.java`

调度记录 MyBatis Mapper，负责：

- 插入调度记录
- 分页查询调度记录
- 按任务查调度记录

#### 12.3.3 `repository/`

##### `ComputeNodeRepositoryImpl.java`

`ComputeNodeRepository` 的实现类，负责把：

- `ComputeNodeMapper`
- `NodeAssembler`

组合起来，完成领域对象和持久化对象之间的双向转换。

##### `NodeSolverCapabilityRepositoryImpl.java`

`NodeSolverCapabilityRepository` 的实现类，负责：

- 加载节点能力
- 加载求解器能力
- 替换节点全部能力
- 带版本替换节点全部能力
- 更新单条能力开关

这是当前“节点上报能力 -> 平台重建 `node_solver_capability`”的真正落地点。

##### `ScheduleRecordRepositoryImpl.java`

`ScheduleRecordRepository` 的实现类，负责调度记录的保存和查询。

### 12.4 运行支撑目录

对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/infrastructure/support/
```

#### `AvailableNodeSelector.java`

一个简单的候选节点过滤组件，只保留 `canDispatch()` 的节点。

当前主调度链路里并没有直接使用它，属于保留的基础组件。

#### `NodeHeartbeatChecker.java`

节点离线巡检支撑组件，是 `NodeOfflineCheckJob` 的核心实现。

它的逻辑是：

- 取出所有 `ONLINE` 节点；
- 如果最近心跳时间为空或早于“当前时间 - 30 秒”，就认为节点离线；
- 调 `task-service` 把该节点上未完成任务批量标记失败；
- 再把节点本身改为 `OFFLINE`，并清零运行数和负载。

这说明当前离线补偿策略是“失败终止”，不是“自动重排回队列”。

#### `NodeLoadCalculator.java`

负载分值计算组件，当前按：

- `runningCount * 10 + cpu + memory`

计算一个综合分值。

但它目前没有直接接入 `FcfsLeastLoadStrategy`，属于预留扩展组件。

---

## 13. 支撑层

### 13.1 对应文件夹

支撑层对应文件夹：

```text
scheduler-service/src/main/java/com/example/cae/scheduler/support/
```

### 13.2 文件说明

#### `ScheduleLogWriter.java`

一个简单的调度日志包装器，提供：

- `info(String message)`
- `warn(String message)`

当前主链路里基本还没有深入使用它，更多是一个统一日志前缀的预留点。

---

## 14. 与数据库表的对应关系

### 14.1 `compute_node`

对应代码链路：

- 领域模型：`domain/model/ComputeNode.java`
- 持久化对象：`infrastructure/persistence/entity/ComputeNodePO.java`
- Mapper：`infrastructure/persistence/mapper/ComputeNodeMapper.java`
- 仓储实现：`infrastructure/persistence/repository/ComputeNodeRepositoryImpl.java`

它保存节点本身的信息：

- 节点编码
- 主机地址
- token
- 在线状态
- 节点整体启用状态
- 最大并发
- 当前运行数
- CPU、内存、最近心跳

### 14.2 `node_solver_capability`

对应代码链路：

- 领域模型：`domain/model/NodeSolverCapability.java`
- 持久化对象：`infrastructure/persistence/entity/NodeSolverCapabilityPO.java`
- Mapper：`infrastructure/persistence/mapper/NodeSolverCapabilityMapper.java`
- 仓储实现：`infrastructure/persistence/repository/NodeSolverCapabilityRepositoryImpl.java`

它保存节点的求解器能力信息：

- 节点 ID
- 求解器 ID
- 求解器版本
- 能力是否允许参与调度

当前这张表的生成来源是“节点注册上报能力”，不是管理员手工虚构绑定。

### 14.3 `schedule_record`

对应代码链路：

- 领域模型：`domain/model/ScheduleRecord.java`
- 持久化对象：`infrastructure/persistence/entity/ScheduleRecordPO.java`
- Mapper：`infrastructure/persistence/mapper/ScheduleRecordMapper.java`
- 仓储实现：`infrastructure/persistence/repository/ScheduleRecordRepositoryImpl.java`

它只保存调度审计信息，不保存任务全部内容。

---

## 15. 核心调用链

### 15.1 节点注册链路

当前主注册链路是：

1. `node-agent` 启动后调用 `NodeRegisterAppService.registerSelf()`
2. `NodeInfoCollector` 采集：
   - 节点编码
   - 节点名称
   - 主机地址
   - 最大并发
   - 已配置求解器 ID 列表
3. `SchedulerNodeClientImpl.register()` 组装 `NodeAgentRegisterRequest`
4. 调用 `scheduler-service` 的 `NodeAgentController.register()`
5. 转到 `NodeFacade -> NodeManageManager -> NodeAppService.registerNodeFromAgent()`
6. 若节点已存在则更新，若不存在则新建
7. 通过 `mergeCapabilities()` 把上报的求解器列表重建为 `node_solver_capability`
8. 返回 `nodeId + nodeToken`
9. `node-agent` 本地保存这两个值，供后续心跳和任务回传使用

这里最值得写进论文说明的是：

- 节点注册上报的是“事实能力”
- 平台落库的是 `compute_node + node_solver_capability`
- 管理员后续控制的是“允不允许调度”

### 15.2 节点心跳与离线判定链路

心跳链路：

1. `node-agent` 定时调用 `HeartbeatAppService.sendHeartbeat()`
2. `SchedulerNodeClientImpl.heartbeat()` 调 `/api/node-agent/heartbeat`
3. 携带 `X-Node-Token`
4. `NodeAgentController.heartbeat()` 收到请求
5. 转到 `NodeAppService.heartbeat(request, nodeToken)`
6. 校验 token
7. 刷新节点 CPU、内存、运行数和最近心跳时间
8. 节点状态标记为 `ONLINE`

离线巡检链路：

1. `NodeOfflineCheckJob.run()` 定时执行
2. 调 `NodeHeartbeatChecker.markOfflineNodes()`
3. 找出超过 30 秒没心跳的节点
4. 调 `task-service` 把该节点上的 `SCHEDULED / DISPATCHED / RUNNING` 任务批量标记失败
5. 节点改成 `OFFLINE`

### 15.3 任务调度链路

主调度链路：

1. `TaskScheduleJob.run()` 每 5 秒执行一次
2. `TaskClientStub.listPendingTasks(20)` 向 `task-service` 拉 `QUEUED` 任务
3. `TaskScheduleManager.schedule(task)`
4. `ScheduleAppService.scheduleTask(task)` 选节点
5. `ScheduleDomainService` 过滤可用节点
6. `FcfsLeastLoadStrategy` 选出最终节点
7. 先把节点 `runningCount + 1`
8. 调 `task-service` 把任务标记为 `SCHEDULED`
9. 调 `node-agent` 下发任务
10. 再调 `task-service` 把任务标记为 `DISPATCHED`
11. 写入成功调度记录

失败时：

- 若只是“没有可用节点”，任务仍会保留在 `QUEUED` 队列中，等待下一轮轮询；
- 若已经标成 `SCHEDULED` 但下发失败，则当前实现会把任务直接改为 `FAILED`；
- 失败调度会写 `schedule_record`；
- 若已占用节点并发，则会回滚 `runningCount`。

### 15.4 节点启停与能力启停链路

管理员操作链路：

1. 前端调用 `NodeController`
2. 进入 `NodeFacade -> NodeManageManager -> NodeAppService`
3. 若是节点启停，则修改 `compute_node.enabled`
4. 若是节点能力启停，则修改 `node_solver_capability.enabled`

因此，当前调度是否成立，至少要同时满足：

- 节点 `ONLINE`
- 节点 `enabled = 1`
- 节点存在目标求解器能力
- 能力 `enabled = 1`

### 15.5 节点 token 校验链路

当前系统中，任务执行状态、日志、结果回传时，`task-service` 会调用调度服务确认节点身份：

1. `task-service` 中的 `NodeAgentAuthService`
2. 调 `scheduler-service` 的 `GET /internal/nodes/{nodeId}/token/verify`
3. `InternalSchedulerController` 调用 `NodeAppService.validateNodeToken()`
4. 返回 token 是否有效

这使得“任务只允许被分配给它的节点回传状态”成为可能。

---

## 16. 设计文档与当前实现的对照

### 16.1 已经较好落地的部分

结合 `需求分析与系统设计.md`、`后端设计.md`、`数据库设计.md`，当前 `scheduler-service` 已经较好落地了下面这些设计目标：

- 节点注册、心跳、在线/离线动态维护；
- `compute_node`、`node_solver_capability`、`schedule_record` 三表分工明确；
- 节点能力由注册上报同步，不是管理员手工虚构；
- 管理员可控制节点整体启停与节点-求解器能力启停；
- 调度流程已经形成“拉队列 -> 选节点 -> 标记状态 -> 下发 -> 记审计”的完整闭环；
- 节点离线后会补偿失败该节点上的未完成任务；
- 调度记录可分页查询、可按任务查询；
- 调度策略已抽象成接口，不是全部硬编码在定时任务里。

### 16.2 当前是“基础调度器”而不是“生产级调度器”的部分

当前实现虽然闭环已经跑通，但仍然是明显的基础版：

- 只有单机轮询调度，没有分布式锁，也没有多实例防重；
- 没有消息队列，任务获取和状态回写都靠同步 HTTP；
- 没有抢占式调度、标签调度、配额调度、资源需求建模；
- 没有自动重试调度、失败重调度；
- 没有调度事件即时触发，仍主要依赖定时轮询；
- 没有复杂资源模型，当前节点负载主要只看运行数、CPU、内存；
- 没有把 `ScheduleStatusEnum` 真正贯穿全链路使用。

### 16.3 当前是“预留/占位”的部分

下面这些类目前更像架构预留位：

- `SchedulingConfig.java`
- `FeignClientConfig.java`
- `AvailableNodeSelector.java`
- `NodeLoadCalculator.java`
- `ScheduleLogWriter.java`
- `ComputeNodeRepository.findByNodeToken()` 这条仓储查询能力当前使用频率较低

这类代码适合在文档里表述为“为后续增强预留的扩展点”，不要表述成已经形成完整能力。

### 16.4 当前实现里几个必须写清楚的细节

#### 1. 节点能力同步是“事实上报 + 平台授权”

当前真实实现并不是管理员手工绑定节点支持哪些求解器，而是：

- `node-agent` 注册时上报 `solverId + solverVersion`
- `scheduler-service` 重建 `node_solver_capability`
- 保留原有 `enabled` 开关
- 未再上报的能力会被移除

这一点和需求分析中的设计是吻合的。

#### 2. 调度任务顺序主要由 `task-service` 决定

`TaskScheduleJob` 从 `task-service` 拉到的 `QUEUED` 列表，本身已经按任务优先级与提交时间排序。  
`scheduler-service` 负责的是“在当前这批待调度任务中，为某个任务选哪个节点”，而不是自己重排整个任务队列。

#### 3. 调度失败后的处理还比较保守

当前实现中：

- 无可用节点：任务继续留在 `QUEUED`
- 已标 `SCHEDULED` 但下发失败：任务直接记为 `FAILED`
- 节点离线：受影响任务直接记为 `FAILED`

也就是说，当前没有自动回队和自动重调度机制。

#### 4. `schedule_record.node_id` 最好允许为空

代码里存在“调度失败时 `nodeId = null` 也照样写调度记录”的逻辑，所以数据库表若仍强制 `NOT NULL`，就会和代码口径冲突。这个点在文档中需要明确写成“实现收口要求”。

---

## 17. 推荐的阅读顺序

### 17.1 先看接口

建议先看：

- `NodeController.java`
- `ScheduleController.java`
- `NodeAgentController.java`
- `InternalSchedulerController.java`

这样能先理解调度服务对外暴露了哪些能力。

### 17.2 再看主流程编排

接着看：

- `TaskScheduleJob.java`
- `ScheduleAppService.java`
- `NodeAppService.java`

这样能看清楚真正的业务主链。

### 17.3 再看领域规则和策略

然后看：

- `ComputeNode.java`
- `NodeSolverCapability.java`
- `ScheduleDomainService.java`
- `FcfsLeastLoadStrategy.java`

这样能知道调度判定规则到底是什么。

### 17.4 最后看数据库与远程调用

最后看：

- `TaskClientStub.java`
- `NodeAgentClientStub.java`
- `ComputeNodeMapper.java`
- `NodeSolverCapabilityMapper.java`
- `ScheduleRecordMapper.java`

这样就能把“业务抽象”和“实际落库/实际调用”对应起来。

---

## 18. 当前结论

`scheduler-service` 当前已经完成了“本科毕设/课程设计层面可用的基础调度中心”：

- 节点可动态注册、心跳、上下线；
- 节点能力可随注册自动同步；
- 管理员可控制节点和能力开关；
- 调度器可按在线状态、启用状态、节点能力、并发上限筛选节点；
- 任务可被实际派发到节点代理；
- 调度失败、节点离线都有基本补偿和审计记录。

但也要如实说明，它还不是完整的生产级分布式调度系统。当前更准确的定位是：

> 一个已经打通“任务入队 -> 中央调度 -> 节点执行 -> 状态回传”主链路的基础型 CAE 分布式调度原型。

如果下一步继续完善，最值得优先增强的方向是：

- 多实例调度防重；
- 自动重试与重调度；
- 更丰富的调度策略；
- 事件驱动而非纯轮询触发；
- 更严格的调度状态一致性控制。
