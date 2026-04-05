# node-agent 模块分析文档

## 1. 模块定位

`node-agent` 是整个平台中最靠近计算节点运行环境的执行代理服务，部署在具体的计算节点上，负责把调度器下发的“可执行任务”真正落到本机执行环境中。

如果说：

- `solver-service` 解决“平台支持哪些求解器、模板和文件规则”
- `task-service` 解决“任务如何创建、校验、提交、跟踪和汇总”
- `scheduler-service` 解决“任务应该分配给哪个节点”

那么 `node-agent` 解决的就是：

- 节点如何向调度中心注册自己
- 节点如何持续上报心跳和负载
- 节点如何接收调度下发的任务
- 节点如何准备本地工作目录和输入文件
- 节点如何选择对应执行器并启动本地进程
- 节点如何把运行日志、结果摘要、结果文件和最终状态回传给 `task-service`
- 节点如何处理取消、超时和异常退出

因此，`node-agent` 不是用户侧业务服务，也不是调度决策服务，而是平台中的“节点执行侧代理”。

## 2. 模块在系统中的作用

从整个系统协作关系看，`node-agent` 主要承担以下作用：

1. 作为计算节点的身份载体，向 `scheduler-service` 注册节点信息、求解器能力和运行状态。
2. 作为任务执行入口，接收 `scheduler-service` 下发的 `/internal/dispatch-task` 请求。
3. 作为本地执行编排器，把任务从远程请求对象转换为本地执行上下文。
4. 作为进程管理器，启动求解器命令、读取标准输出和错误输出、处理超时与取消。
5. 作为结果回传器，向 `task-service` 上报状态、日志、结果摘要、结果文件和最终结束状态。
6. 作为共享存储桥接层，处理 Windows 与 Linux 之间的路径映射，适配跨机器共享目录场景。

也就是说，`node-agent` 处在 `scheduler-service` 与真实求解器运行环境之间，是调度闭环落到执行闭环的最后一环。

## 3. 当前实现架构

### 3.1 技术栈

- Spring Boot
- Spring MVC 风格 REST 接口
- `@Scheduled` 定时任务
- `RestTemplate` 方式调用 `scheduler-service` 与 `task-service`
- `ProcessBuilder` 启动本地外部进程
- 本地文件系统目录管理
- `common-lib` 中的统一返回体、异常、常量与枚举

### 3.2 当前目录结构

当前代码结构如下：

```text
node-agent/
└─ src/main/java/com/example/cae/nodeagent/
   ├─ NodeAgentApplication.java
   ├─ config/
   │  ├─ NodeAgentConfig.java
   │  ├─ NodeAgentBeanConfig.java
   │  ├─ ExecutorConfig.java
   │  └─ FeignClientConfig.java
   ├─ interfaces/
   │  ├─ controller/
   │  │  └─ DispatchController.java
   │  ├─ request/
   │  │  ├─ DispatchTaskRequest.java
   │  │  └─ CancelTaskRequest.java
   │  └─ response/
   │     ├─ DispatchTaskResponse.java
   │     └─ CancelTaskResponse.java
   ├─ application/
   │  ├─ assembler/
   │  │  └─ ExecutionContextAssembler.java
   │  ├─ manager/
   │  │  ├─ TaskDispatchManager.java
   │  │  ├─ TaskExecuteManager.java
   │  │  ├─ TaskReportManager.java
   │  │  └─ TaskRuntimeRegistry.java
   │  ├─ scheduler/
   │  │  └─ HeartbeatJob.java
   │  └─ service/
   │     ├─ NodeRegisterAppService.java
   │     ├─ HeartbeatAppService.java
   │     └─ TaskReportAppService.java
   ├─ domain/
   │  ├─ enums/
   │  │  └─ ExecutionStageEnum.java
   │  ├─ executor/
   │  │  ├─ SolverExecutor.java
   │  │  ├─ AbstractSolverExecutor.java
   │  │  ├─ MockExecutor.java
   │  │  ├─ OpenFoamExecutor.java
   │  │  └─ CalculixExecutor.java
   │  ├─ model/
   │  │  ├─ ExecutionContext.java
   │  │  ├─ ExecutionResult.java
   │  │  ├─ InputFileMeta.java
   │  │  └─ NodeInfo.java
   │  └─ service/
   │     ├─ ExecutionContextBuildService.java
   │     └─ ExecutorSelectDomainService.java
   ├─ infrastructure/
   │  ├─ client/
   │  │  ├─ SchedulerNodeClient.java
   │  │  ├─ TaskReportClient.java
   │  │  └─ impl/
   │  │     ├─ SchedulerNodeClientImpl.java
   │  │     └─ TaskReportClientImpl.java
   │  ├─ process/
   │  │  ├─ ProcessRunner.java
   │  │  ├─ ProcessLogReader.java
   │  │  ├─ ProcessExitHandler.java
   │  │  ├─ ProcessCanceledException.java
   │  │  └─ ProcessTimeoutException.java
   │  ├─ storage/
   │  │  ├─ WorkDirManager.java
   │  │  ├─ InputFilePrepareService.java
   │  │  ├─ ResultFileCollector.java
   │  │  └─ PathMappingSupport.java
   │  └─ support/
   │     ├─ NodeInfoCollector.java
   │     ├─ CommandBuilder.java
   │     └─ LogPushBuffer.java
   └─ support/
      └─ AgentTempFileCleaner.java
```

这个结构和 `node-agent` 的职责是匹配的：它既有控制入口，也有执行编排，还把进程、存储、远程上报这些基础设施能力拆了出来，而不是把所有逻辑堆进一个 `service` 类。

## 4. 各部分功能与职责

### 4.1 启动与配置层

#### `NodeAgentApplication`

职责：

- 启动 `node-agent`
- 开启定时任务支持
- 扫描整套 `com.example.cae` 包下组件

#### `NodeAgentConfig`

职责：

- 统一承载 `cae.node.*` 配置项
- 保存节点自身身份信息，如 `nodeId`、`nodeCode`、`nodeName`
- 保存运行配置，如 `maxConcurrency`、`workRoot`、`processLogCharset`
- 保存远程服务地址，如 `schedulerBaseUrl`、`taskBaseUrl`
- 保存注册成功后由调度器返回的 `nodeToken`
- 保存跨系统共享目录映射信息
- 保存节点支持的 `solverIds` 和 `solverVersions`

这是 `node-agent` 的核心配置入口，也是节点运行态信息的内存载体之一。

#### `NodeAgentBeanConfig`

职责：

- 提供任务执行线程池 `taskExecutor`
- 线程池大小直接跟随 `maxConcurrency`
- 提供超时受控的 `RestTemplate`

这里体现了 `node-agent` 的并发模型比较直接：并发上限由节点配置控制，线程池与运行中任务数之间保持一致性。

#### `ExecutorConfig` 与 `FeignClientConfig`

当前这两个类是空配置类，主要作用是保留结构位置，便于后续扩展：

- `ExecutorConfig` 适合未来挂接求解器执行器 Bean、解析器 Bean 或执行环境配置
- `FeignClientConfig` 当前未启用 Feign，更像是从其他模块结构中保留下来的预留点

### 4.2 接口层

#### `DispatchController`

这是 `node-agent` 唯一的对外控制入口，但这里的“对外”是指对调度器开放的内部入口，而不是给前端或网关使用的业务接口。

当前暴露两个接口：

- `POST /internal/dispatch-task`
- `POST /internal/cancel-task`

其中：

- `dispatch-task` 负责接收调度器下发任务，请求体为 `DispatchTaskRequest`
- `cancel-task` 负责接收取消请求，请求体为 `CancelTaskRequest`

接口层本身保持很薄，主要职责是：

- 接收请求
- 调用 `TaskDispatchManager`
- 返回统一的 `accepted + message` 响应

它不直接处理目录准备、进程控制或上报逻辑。

#### `DispatchTaskRequest`

这个请求对象承接的是调度器已经组装好的“可执行任务”，关键字段包括：

- `taskId`、`taskNo`
- `solverId`、`solverCode`
- `profileId`、`taskType`
- `commandTemplate`
- `parserName`
- `timeoutSeconds`
- `inputFiles`
- `params`

可以看出，`node-agent` 接收到的已经不是原始业务任务，而是带有执行模板信息的任务对象。

#### `CancelTaskRequest`

字段很轻量：

- `taskId`
- `reason`

取消请求并不重新建模为复杂流程对象，而是让运行时注册表直接处理。

### 4.3 应用层

#### `NodeRegisterAppService`

职责：

- 在应用启动后自动注册节点
- 调用 `NodeInfoCollector` 采集节点信息
- 调用 `SchedulerNodeClient.register(...)` 向调度器注册

这里通过 `@PostConstruct` 在启动后自动执行 `registerOnStartup()`，说明当前节点注册是节点自注册模式，而不是调度器主动发现模式。

#### `HeartbeatAppService`

职责：

- 定期上报心跳
- 心跳失败时尝试重新注册
- 重新注册成功后再次补发一次心跳

这体现出当前设计有一个轻量级自愈逻辑：如果调度器重启、节点令牌丢失或节点记录失效，节点侧会尝试恢复而不是直接静默失败。

#### `TaskReportAppService`

职责：

- 封装状态回传、日志回传、结果摘要回传、结果文件回传
- 封装任务完成与失败回传
- 封装节点 `runningCount` 更新

这个类本身像一个“上报网关”，把 `TaskReportClient` 和 `SchedulerNodeClient` 聚合起来，对上层暴露统一的回传方法。

#### `ExecutionContextAssembler`

职责：

- 把 `DispatchTaskRequest` 转为 `ExecutionContext`

它本身很薄，但保留了“接口对象”和“内部执行上下文”之间的转换边界，便于以后增加默认值处理、字段补齐、参数展开等逻辑。

#### `TaskDispatchManager`

这是任务接入编排的核心类，职责包括：

- 校验 `taskId` 是否为空
- 校验节点是否超出最大并发
- 校验任务是否已经在本节点运行
- 在 `TaskRuntimeRegistry` 中注册任务
- 构建执行上下文
- 初始化日志序号
- 把真正执行逻辑投递给线程池异步执行
- 处理取消请求

它的作用是把“收到任务”转换为“异步执行已接管”。

这里有两个非常关键的保护：

- 若 `runningCount >= maxConcurrency`，直接拒绝任务
- 若相同 `taskId` 已注册运行，直接拒绝重复下发

#### `TaskExecuteManager`

这是本模块最核心的执行编排器，职责包括：

- 绑定当前工作线程到运行时注册表
- 检查任务是否已被取消
- 准备工作目录
- 准备输入文件
- 上报任务进入 `RUNNING`
- 选择执行器
- 调用执行器实际执行
- 根据执行结果决定走成功或失败上报
- 处理取消、超时和普通异常
- 最终统一收尾

它把整个执行链路拆成了比较清晰的阶段，而不是把目录、文件、进程、日志、结果全部揉在一个方法里。

#### `TaskReportManager`

这是执行结果汇总与上报编排器，职责包括：

- 为每个任务初始化日志序号计数器
- 上报运行中状态
- 推送日志行并维护自增 `seqNo`
- 上报结果摘要
- 逐个上报结果文件
- 上报完成或失败
- 上报取消状态
- 在任务结束后清理日志序号、移除运行态并更新节点运行数

它把“执行过程产生的各种输出”统一收敛成对外上报动作，是执行链路和远程回传链路之间的桥梁。

#### `TaskRuntimeRegistry`

这是 `node-agent` 并发与取消控制的关键组件，职责包括：

- 维护当前运行任务表
- 记录任务是否被取消
- 记录取消原因
- 记录任务对应工作线程
- 记录任务对应外部进程
- 在取消时同时中断线程并强制销毁进程

这个实现虽然简单，但非常实用。它把“任务运行态”从业务对象中抽离出来，形成节点本地的运行时内存注册表。

#### `HeartbeatJob`

职责：

- 按 `cae.node.heartbeat-interval-ms` 周期触发心跳上报

### 4.4 领域层

#### `ExecutionContext`

这是执行阶段最核心的领域模型，承载：

- 任务标识信息
- 求解器和模板信息
- 命令模板与解析器名
- 超时设置
- 输入文件列表和运行参数
- 本地工作目录、输入目录、输出目录、日志目录

它还提供了几个语义方法：

- `isTimeoutEnabled()`
- `hasInputFiles()`
- `hasParser()`

说明它不是纯数据包，而是具备一定执行语义判断能力。

#### `ExecutionResult`

职责：

- 表达执行成功或失败
- 承载耗时、摘要、指标、结果文件列表

它提供了：

- `success(...)`
- `fail(...)`

两个静态工厂方法，使执行器返回结果时更统一。

#### `InputFileMeta`

职责：

- 表达单个输入文件元数据
- 提供 `fileKey / originName / storagePath`

当前 `node-agent` 实际使用最关键的是 `originName` 与 `storagePath`。

#### `NodeInfo`

职责：

- 表达节点注册与心跳上报时的节点快照
- 承载 `host`、并发数、CPU、内存、运行数、支持的求解器列表

#### `ExecutorSelectDomainService`

职责：

- 遍历当前所有 `SolverExecutor`
- 基于 `supports(context)` 选择可用执行器
- 若没有匹配执行器则直接报错

这是一种很典型的策略模式实现，适合未来继续横向扩展执行器。

#### `ExecutionContextBuildService`

职责：

- 从下发请求中提取并构建标准 `ExecutionContext`

它让“上下文构建”具备独立职责，便于后续添加默认值、路径渲染、参数展开等增强逻辑。

#### `SolverExecutor`、`AbstractSolverExecutor`

这是执行器扩展体系的根接口和抽象基类：

- `supports(context)` 决定当前执行器是否适用
- `execute(context)` 是统一执行入口
- `AbstractSolverExecutor` 提供了模板方法模式，先 `prepare` 再 `doExecute`

#### `MockExecutor`

职责：

- 处理 `solverCode=MOCK` 或 `solverId` 为空/为 `0` 的任务
- 模拟产生日志
- 构造模拟执行成功结果

它非常适合作为演示环境、联调环境和无真实求解器环境下的替代执行器。

#### `OpenFoamExecutor` 与 `CalculixExecutor`

职责：

- 根据 `solverCode` 判断是否支持
- 构造命令
- 调用 `ProcessRunner` 启动本地进程
- 实时推送标准输出与错误输出日志
- 非 0 退出码时直接报错
- 执行完成后收集输出目录下结果文件
- 返回标准化 `ExecutionResult`

这两个执行器的结构基本一致，体现了当前执行器模型的可复用性。

#### `ExecutionStageEnum`

当前定义了：

- `RECEIVED`
- `PREPARING`
- `RUNNING`
- `COLLECTING`
- `FINISHED`
- `FAILED`

但从现有代码看，这个枚举还没有真正接入主流程，更像是后续丰富执行阶段可观测性的预留设计。

### 4.5 基础设施层

#### `SchedulerNodeClient` / `SchedulerNodeClientImpl`

职责：

- 向 `scheduler-service` 发起注册
- 向 `scheduler-service` 发起心跳
- 向 `scheduler-service` 更新运行计数

其中注册接口调用：

- `POST /api/node-agent/register`

心跳接口调用：

- `POST /api/node-agent/heartbeat`

运行计数更新调用：

- `POST /internal/nodes/{nodeId}/running-count`

注册成功后，调度器返回的 `nodeId` 与 `nodeToken` 会被写回 `NodeAgentConfig`，这使节点从“配置节点”变成“注册后的活跃节点”。

#### `TaskReportClient` / `TaskReportClientImpl`

职责：

- 向 `task-service` 上报状态
- 上报日志
- 上报结果摘要
- 上报结果文件
- 标记任务完成
- 标记任务失败

对应调用的内部接口包括：

- `/internal/tasks/{taskId}/status-report`
- `/internal/tasks/{taskId}/log-report`
- `/internal/tasks/{taskId}/result-summary-report`
- `/internal/tasks/{taskId}/result-file-report`
- `/internal/tasks/{taskId}/mark-finished`
- `/internal/tasks/{taskId}/mark-failed`

它还会在请求头中写入 `X-Node-Token`，与 `task-service` 的 `NodeAgentAuthService` 配合，完成节点回传校验。

#### `ProcessRunner`

职责：

- 启动本地外部进程
- 将进程对象绑定到运行时注册表
- 分别读取标准输出和错误输出
- 处理超时等待
- 处理取消中断
- 检查退出码
- 在结束后清理进程引用

这是 `node-agent` 最贴近操作系统进程控制的类。

#### `ProcessLogReader`

职责：

- 按配置字符集读取求解器输出流
- 逐行回调消费者

这里的 `processLogCharset` 很重要，因为不同求解器、不同平台环境下日志编码可能并不一致。当前默认配置是 `GBK`，比较贴近 Windows 本地求解器输出场景。

#### `ProcessExitHandler`

职责：

- 非 0 退出码时报错

它虽然很简单，但保留了“退出码解释”这个扩展点。

#### `WorkDirManager`

职责：

- 按任务创建本地工作目录
- 生成 `input / output / log` 三类子目录
- 将目录路径写回 `ExecutionContext`

当前目录结构是：

```text
{workRoot}/{taskId}/
├─ input/
├─ output/
└─ log/
```

这种设计足够清晰，便于后续定位任务现场文件。

#### `InputFilePrepareService`

职责：

- 遍历任务输入文件列表
- 根据共享路径映射把来源路径转换成本机可访问路径
- 把输入文件复制到本地 `input` 目录

这一步是把“任务文件在平台上的存储位置”转换为“当前节点本地执行目录中的输入材料”。

#### `ResultFileCollector`

职责：

- 收集 `output` 目录下的直接文件

当前实现比较简单，只收集输出目录第一层文件，不递归处理子目录。

#### `PathMappingSupport`

职责：

- 处理 Windows 路径到 Linux 路径的映射
- 处理 Linux 路径回写为 Windows 路径

这对于当前项目非常关键，因为平台很可能是：

- `task-service` 在 Windows 上落库和保存路径
- `node-agent` 在 Linux 节点上执行
- 节点和平台通过共享目录读写同一批文件

没有这层映射，就无法把数据库中的路径安全地转换为节点本机可读取路径。

#### `NodeInfoCollector`

职责：

- 采集节点标识与主机地址
- 采集最大并发数
- 采集 CPU 使用率
- 采集内存使用率
- 采集当前运行任务数
- 采集节点支持的求解器列表

它让节点注册与心跳不必依赖外部传参，而是直接根据本机状态实时生成快照。

#### `CommandBuilder`

职责：

- 根据操作系统类型包装命令模板
- Windows 下使用 `cmd /c`
- Linux 下使用 `/bin/sh -c`

这意味着当前 `node-agent` 执行的不是结构化命令数组模板，而是字符串命令模板。

#### `LogPushBuffer`

当前这个类提供了简单的日志缓存与批量取出能力，但从现有执行链路看并未实际接入主流程，属于预留组件。

### 4.6 通用支持层

#### `AgentTempFileCleaner`

职责：

- 定期扫描 `workRoot`
- 删除超过 24 小时未更新的任务目录

这说明当前节点并不是永久保留所有任务现场，而是有一个非常轻量的清理策略，避免本地磁盘持续膨胀。

## 5. 核心业务流程

### 5.1 节点启动注册流程

`node-agent` 启动后的典型流程如下：

```text
NodeAgentApplication 启动
  -> NodeRegisterAppService.registerOnStartup()
  -> NodeInfoCollector.collectNodeInfo()
  -> SchedulerNodeClientImpl.register()
  -> 调用 scheduler-service /api/node-agent/register
  -> scheduler-service 返回 nodeId + nodeToken
  -> 写回 NodeAgentConfig
```

这个流程的关键意义在于：

- 节点不是写死在调度器里的
- 节点上线后可以自注册
- 节点令牌由调度器统一下发

### 5.2 节点心跳流程

```text
HeartbeatJob 定时触发
  -> HeartbeatAppService.sendHeartbeat()
  -> NodeInfoCollector.collectNodeInfo()
  -> SchedulerNodeClientImpl.heartbeat()
  -> 调用 scheduler-service /api/node-agent/heartbeat
  -> 若失败则尝试重新注册
  -> 重新注册成功后再次发送心跳
```

心跳上报的内容主要包括：

- `nodeId`
- `nodeCode`
- `cpuUsage`
- `memoryUsage`
- `runningCount`

这保证调度器拿到的是动态节点状态，而不是静态配置。

### 5.3 任务下发与执行流程

这是 `node-agent` 最关键的主链路：

```text
scheduler-service 下发 /internal/dispatch-task
  -> DispatchController.dispatch()
  -> TaskDispatchManager.acceptTask()
  -> 校验 taskId / 并发上限 / 是否重复运行
  -> TaskRuntimeRegistry.register(taskId)
  -> ExecutionContextAssembler.fromDispatchRequest()
  -> TaskReportManager.onTaskAccepted(taskId)
  -> 投递到 taskExecutor 异步执行
  -> TaskExecuteManager.execute(context)
  -> 绑定 workerThread
  -> 检查是否已取消
  -> WorkDirManager.prepareTaskDirs()
  -> InputFilePrepareService.prepare()
  -> TaskReportManager.reportRunning()
  -> ExecutorSelectDomainService.selectExecutor()
  -> SolverExecutor.execute()
  -> CommandBuilder.buildCommand()
  -> ProcessRunner.run()
  -> ProcessLogReader 按行读取日志并回传
  -> ResultFileCollector.collect()
  -> TaskReportManager.reportSuccess()/reportFail()
  -> TaskReportManager.completeTask()
```

这个流程说明：

- `node-agent` 不是“收到任务就同步执行然后阻塞返回”
- 而是“快速接受请求，然后异步接管任务执行”

因此它更像一个本节点上的轻量执行守护进程。

### 5.4 任务取消流程

```text
scheduler-service 下发 /internal/cancel-task
  -> DispatchController.cancel()
  -> TaskDispatchManager.cancelTask()
  -> TaskRuntimeRegistry.cancel(taskId, reason)
  -> 设置 cancelRequested
  -> 如有进程则 destroyForcibly()
  -> 如有线程则 interrupt()
  -> TaskExecuteManager 捕获取消态
  -> TaskReportManager.reportCanceled()
  -> TaskReportManager.completeTask()
```

这条链路表明当前取消实现不是业务层“标记取消后等待自然结束”，而是直接对本地运行态发出中断与进程销毁信号。

## 6. 核心设计

### 6.1 节点侧职责保持聚焦

当前 `node-agent` 的设计边界非常清晰，它只做三类事：

- 节点注册与心跳
- 本地任务执行
- 执行结果回传

它不负责：

- 任务创建与校验
- 求解器模板管理
- 调度策略选择
- 用户鉴权与前端接口

这种职责划分很适合微服务架构中的执行代理模块。

### 6.2 并发控制采用“线程池 + 运行时注册表”

当前并发模型的核心是：

- `NodeAgentBeanConfig` 提供固定大小线程池
- `TaskDispatchManager` 先检查 `runningCount`
- `TaskRuntimeRegistry` 负责记录活动任务

这种设计的优点是实现成本低、语义直观，而且可以自然支持：

- 并发上限控制
- 重复任务拦截
- 取消信号投递
- 运行中任务计数

### 6.3 执行器采用策略模式

当前执行器体系是：

- `SolverExecutor` 统一接口
- `AbstractSolverExecutor` 提供模板方法
- `MockExecutor / OpenFoamExecutor / CalculixExecutor` 负责具体实现
- `ExecutorSelectDomainService` 按 `supports(...)` 选择执行器

这样做的好处是：

- 扩展新求解器时不需要改动总流程
- 执行器之间职责隔离
- 可以支持“真实求解器”和“模拟求解器”并存

### 6.4 共享存储与跨系统路径映射解耦

`PathMappingSupport + InputFilePrepareService + TaskReportClientImpl` 共同解决了一个很现实的问题：

- 平台记录的文件路径可能是 Windows 风格
- 计算节点执行环境可能是 Linux
- 结果文件回传后又需要让平台按 Windows 路径进行展示或访问

这说明当前设计已经考虑到“平台与计算节点异构部署”的真实场景，而不只是单机自娱自乐的演示代码。

### 6.5 执行与上报解耦

`TaskExecuteManager` 不直接写 HTTP 调用，而是统一通过 `TaskReportManager -> TaskReportAppService -> TaskReportClient` 上报。

这带来两个好处：

- 执行编排逻辑更清晰
- 将来如果要把回传方式从同步 HTTP 改成消息队列、批量缓冲或异步重试，改动面会更小

### 6.6 节点状态采用“注册 + 心跳 + 轻量自恢复”

当前节点治理模型不是复杂的服务发现体系，而是：

- 启动时主动注册
- 运行中持续心跳
- 心跳失败时尝试重新注册

这非常适合本科毕设原型系统：实现难度适中，但又足以支撑动态节点管理的完整展示。

## 7. 架构难点与解决方案

### 7.1 难点一：调度任务如何真正落到节点本地执行

调度器只知道任务应该去哪台节点，但并不知道如何在节点上准备目录、复制输入、执行命令、收集输出。

当前解决方案是：

- 用 `DispatchTaskRequest` 传递执行所需关键元数据
- 在节点侧构建 `ExecutionContext`
- 用 `TaskExecuteManager` 统一编排执行链路

这样把“调度成功”与“执行完成”之间的空白补上了。

### 7.2 难点二：如何在节点上实现可取消、可超时的进程执行

真实求解器运行常常是长时任务，如果只调用 `Runtime.exec()` 而没有运行态跟踪，就很难实现取消和超时。

当前解决方案是：

- `TaskRuntimeRegistry` 保存 `workerThread` 与 `process`
- `ProcessRunner` 使用 `waitFor(timeout, TimeUnit.SECONDS)` 做超时控制
- 取消时同时 `interrupt` 线程并 `destroyForcibly` 进程

这个方案虽然不算复杂，但已经足以支持任务生命周期中的关键异常分支。

### 7.3 难点三：日志如何实时回传

如果任务执行完成后才一次性上传日志，不利于调试，也不利于观察任务是否卡死。

当前解决方案是：

- `ProcessLogReader` 分别读取 stdout / stderr
- 每读取一行就交给 `TaskReportManager.pushLog(...)`
- 由 `TaskReportClientImpl` 实时推送到 `task-service`

因此当前系统已经具备了“任务运行中持续看到节点输出”的基础能力。

### 7.4 难点四：节点文件路径如何跨系统兼容

在 Windows 平台管理任务、Linux 节点执行求解，是非常常见但也非常容易出问题的部署方式。

当前解决方案是：

- 在配置中定义 `pathMappingWindows` 与 `pathMappingLinux`
- 输入文件复制前做一次路径转换
- 结果文件回传前再做一次反向路径转换

这样可以避免数据库中记录的路径在节点上无法直接访问。

### 7.5 难点五：节点回传接口如何做最基本安全控制

如果 `task-service` 完全相信任意请求都来自合法节点，会存在伪造回传风险。

当前解决方案是：

- 节点注册时由 `scheduler-service` 生成 `nodeToken`
- `node-agent` 回传 `task-service` 时带上 `X-Node-Token`
- `task-service` 再调用 `scheduler-service` 验证 `nodeToken`

这说明当前系统已经具备了内部节点身份校验意识，而不是单纯依赖网关白名单。

## 8. 关键技术手段

### 8.1 启动自注册

通过 `@PostConstruct` 自动触发节点注册，减少人工干预。

### 8.2 定时心跳

通过 `@Scheduled` 周期上报节点负载，支持动态节点状态刷新。

### 8.3 本地进程管理

通过 `ProcessBuilder + waitFor(timeout)` 实现命令启动、阻塞等待、超时控制。

### 8.4 运行态注册表

通过 `ConcurrentHashMap` 保存任务运行时状态，实现：

- 运行任务计数
- 取消标记
- 工作线程跟踪
- 子进程跟踪

### 8.5 实时日志推送

通过双流读取标准输出与错误输出，实现执行过程日志实时上报。

### 8.6 节点负载采集

通过 `OperatingSystemMXBean` 采集 CPU 与内存使用率，形成调度所需的运行状态信息。

### 8.7 共享目录路径映射

通过 `PathMappingSupport` 统一处理 Windows 与 Linux 路径映射，解决异构部署问题。

### 8.8 结果文件类型推断

`TaskReportClientImpl` 会根据后缀简单判断结果文件类型：

- `log / txt` -> `LOG`
- `html / pdf / doc / docx` -> `REPORT`
- `png / jpg / jpeg / bmp` -> `IMAGE`
- 其他 -> `RESULT`

这有助于结果文件在任务侧被更好地分类展示。

## 9. 当前实现的优点

- 已形成“注册节点 -> 心跳上报 -> 接收任务 -> 执行命令 -> 回传状态与结果”的完整执行闭环。
- 已具备可扩展的执行器体系，支持 `Mock`、`OpenFOAM`、`CalculiX` 三类执行模式。
- 已考虑跨操作系统共享目录映射，不是只适用于单机同构环境。
- 已支持任务取消与超时处理，节点不再只是单向执行黑盒。
- 已支持实时日志回传，便于联调、排错和演示。
- 已具备节点内部身份校验链路，安全性优于纯白名单放行。
- 已提供临时任务目录清理能力，避免节点磁盘无限增长。

## 10. 当前实现的局限与边界

### 10.1 `dispatch-task` 与 `cancel-task` 入口当前未做节点侧认证

`DispatchController` 当前只暴露内部接口，但没有额外校验调用方身份。

这意味着当前设计默认：

- `node-agent` 端口只在受控内网暴露
- 调度器是唯一调用者

这更适合原型环境，而不是直接暴露到复杂生产网络环境。

### 10.2 命令模板当前还是字符串直传模式

`CommandBuilder` 当前只是把 `commandTemplate` 包装成：

- Windows: `cmd /c`
- Linux: `/bin/sh -c`

它还没有实现：

- 参数占位符渲染
- 命令白名单
- 执行沙箱
- 环境变量隔离

因此当前更像“可信内部命令模板执行”，而不是生产级安全执行框架。

### 10.3 结果处理能力目前较轻量

当前节点侧完成后主要做的是：

- 判断退出码
- 收集 `output` 目录下的文件
- 上报摘要与文件

但尚未实现：

- 解析器 `parserName` 的实际调用
- 结构化结果提取
- 指标自动解析
- 递归目录结果收集

也就是说，当前 `parserName` 字段还处于“数据已贯通、能力未落地”的状态。

### 10.4 部分类仍是预留扩展点

当前尚未真正接入主链路的预留项包括：

- `ExecutionStageEnum`
- `LogPushBuffer`
- `ExecutorConfig`
- `FeignClientConfig`
- `WorkDirManager.cleanupTaskDirs(...)`

这说明模块已经考虑了后续扩展方向，但当前版本仍然是以原型可用为目标。

### 10.5 回传可靠性仍以同步 HTTP 为主

当前所有状态、日志、结果回传都依赖同步 `RestTemplate` 调用，没有：

- 失败重试队列
- 批量缓冲
- 断点续传
- 幂等去重

这在网络稳定的演示环境中通常足够，但在高延迟或弱网络环境下可靠性有限。

### 10.6 清理策略较简单

`AgentTempFileCleaner` 当前按“目录最后修改时间超过 24 小时”直接递归删除。

这套策略虽然实用，但还没有：

- 与任务最终状态联动
- 白名单保护
- 更细粒度保留策略
- 删除失败重试与审计记录

### 10.7 容器化执行环境目前存在“设计目标”和“默认编排”之间的差距

仓库中 `node-agent/Dockerfile` 明显是面向 `OpenFOAM + Java` 的专用节点镜像，而根目录 `compose.yaml` 中的 `node-agent` 服务实际使用的是通用 `Dockerfile.service`。

这意味着当前默认 `docker compose` 方案更偏向：

- 原型联调
- `MockExecutor` 场景
- 通用 Java 服务运行

而真正的求解器执行环境，仍然更依赖专用镜像或单独部署脚本。

## 11. 对本科毕设的价值

从本科毕业设计角度看，`node-agent` 的价值非常高，主要体现在：

1. 它把“任务管理平台”真正延伸到了“计算节点执行平台”，使系统不再只是数据库和页面层面的原型。
2. 它把调度器、任务中心和真实执行环境连接起来，形成完整的端到端闭环。
3. 它体现了分布式节点注册、心跳监测、任务下发、远程执行、日志回传、结果回收等多个关键技术点。
4. 它能够很好支撑“CAE 仿真任务并非只做表单管理，而是能在节点上实际执行”的核心展示目标。

因此，`node-agent` 是论文和答辩中非常值得重点讲解的模块之一。

## 12. 答辩时可采用的表述

可以将该模块概括为：

> `node-agent` 是部署在计算节点上的执行代理服务，负责完成节点自注册、周期性心跳上报、调度任务接收、本地工作目录准备、求解器命令执行、日志实时回传、结果文件收集以及任务完成状态回传。系统通过 `scheduler-service` 管理节点身份与运行状态，通过 `task-service` 汇聚任务执行过程数据，从而形成“调度中心下发、节点本地执行、任务中心统一沉淀”的完整执行闭环。

## 13. 后续可扩展方向

- 为 `/internal/dispatch-task` 和 `/internal/cancel-task` 增加调度器身份认证或签名校验。
- 真正落地 `parserName` 对应的结果解析器体系，自动提取结构化指标与摘要。
- 把命令模板从字符串执行升级为可渲染、可校验的结构化命令模型。
- 增加日志与结果回传失败后的本地缓冲、重试与幂等机制。
- 支持输出目录递归收集、结果压缩打包与分级上传。
- 完善任务目录清理策略，按任务状态、保留周期和磁盘水位做更精细治理。
- 增加更多求解器执行器，如 Abaqus、Fluent、Code_Aster 等。
- 把节点资源模型从 CPU/内存扩展到 GPU、许可证、磁盘容量等维度。
- 将 `node-agent/Dockerfile` 所代表的专用求解器运行环境与默认编排方式统一起来。

## 14. 当前结论

`node-agent` 当前已经完成了本项目原型平台中与“节点执行代理”相关的核心能力建设：

- 可完成节点启动注册与周期性心跳
- 可接收调度器下发任务并进行并发控制
- 可在本地准备任务目录与输入文件
- 可按求解器类型选择执行器并启动本地进程
- 可处理日志回传、结果回传、取消和超时
- 可通过 `nodeToken` 与上游服务协同完成内部回传认证

从实现深度看，它已经超出了“简单调用一个脚本”的演示层面，而是形成了一个具有节点身份管理、本地执行编排、进程控制和结果汇聚能力的轻量级执行代理原型。

从本科毕设要求看，这个模块已经能够很好支撑“微服务 + 动态计算节点 + 仿真任务执行闭环”这一核心技术目标的展示。
