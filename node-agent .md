# 九、node-agent 项目结构设计

`node-agent` 虽然是独立项目，但同样建议用清晰分层，不要只写成一堆 controller + service。

## 1. 推荐结构

```text
node-agent/
└── src/main/java/com/yourorg/nodeagent/
    ├── NodeAgentApplication.java
    ├── interfaces/
    │   ├── controller/
    │   ├── request/
    │   └── response/
    ├── application/
    │   ├── service/
    │   ├── manager/
    │   ├── assembler/
    │   └── scheduler/
    ├── domain/
    │   ├── model/
    │   ├── service/
    │   ├── executor/
    │   └── enums/
    ├── infrastructure/
    │   ├── client/
    │   ├── process/
    │   ├── storage/
    │   └── support/
    ├── config/
    └── support/
```

## 2. 目录职责

### interfaces/controller

- `DispatchController`

### application/manager

- `TaskDispatchManager`
- `TaskExecuteManager`
- `TaskReportManager`

### application/scheduler

如果要周期上报心跳，可以放：

- `HeartbeatJob`

### domain/executor

这是 node-agent 最核心的目录：

- `SolverExecutor`
- `AbstractSolverExecutor`
- `MockExecutor`
- `OpenFoamExecutor`
- `CalculixExecutor`

### domain/service

- `ExecutorSelectDomainService`
- `ExecutionContextBuildService`

### infrastructure/client

- `TaskReportClient`
- `SchedulerNodeClient`

### infrastructure/process

专门负责进程调用：

- `ProcessRunner`
- `ProcessLogReader`

### infrastructure/storage

- `WorkDirManager`
- `InputFilePrepareService`
- `ResultFileCollector`

### infrastructure/support

- `NodeInfoCollector`
- `CommandBuilder`

## 3. node-agent 的关键建议

建议执行链路明确拆开：

1. 接收任务
2. 准备目录
3. 准备输入文件
4. 选择执行器
5. 执行命令
6. 实时读取日志
7. 收集结果
8. 上报完成/失败

不要把这 8 步堆在一个 `execute()` 方法里。

---------

# 九、node-agent 完整包树

你的后端设计里已经明确 node-agent 需要：

- 接收 `/internal/dispatch-task`
- 执行器接口
- `OpenFoamExecutor / CalculixExecutor / MockExecutor`
- 实时读取日志
- 上报状态/日志/结果
  所以这里最适合强化 `executor + process + report` 结构。

## 1. 完整结构

```text
node-agent/
└── src/main/java/com/example/cae/nodeagent/
    ├── NodeAgentApplication.java
    ├── interfaces/
    │   ├── controller/
    │   │   └── DispatchController.java
    │   ├── request/
    │   │   └── DispatchTaskRequest.java
    │   └── response/
    │       └── DispatchTaskResponse.java
    ├── application/
    │   ├── service/
    │   │   ├── NodeRegisterAppService.java
    │   │   ├── HeartbeatAppService.java
    │   │   └── TaskReportAppService.java
    │   ├── manager/
    │   │   ├── TaskDispatchManager.java
    │   │   ├── TaskExecuteManager.java
    │   │   └── TaskReportManager.java
    │   ├── assembler/
    │   │   └── ExecutionContextAssembler.java
    │   └── scheduler/
    │       └── HeartbeatJob.java
    ├── domain/
    │   ├── model/
    │   │   ├── ExecutionContext.java
    │   │   ├── ExecutionResult.java
    │   │   └── NodeInfo.java
    │   ├── service/
    │   │   ├── ExecutorSelectDomainService.java
    │   │   └── ExecutionContextBuildService.java
    │   ├── executor/
    │   │   ├── SolverExecutor.java
    │   │   ├── AbstractSolverExecutor.java
    │   │   ├── MockExecutor.java
    │   │   ├── OpenFoamExecutor.java
    │   │   └── CalculixExecutor.java
    │   └── enums/
    │       └── ExecutionStageEnum.java
    ├── infrastructure/
    │   ├── client/
    │   │   ├── TaskReportClient.java
    │   │   └── SchedulerNodeClient.java
    │   ├── process/
    │   │   ├── ProcessRunner.java
    │   │   ├── ProcessLogReader.java
    │   │   └── ProcessExitHandler.java
    │   ├── storage/
    │   │   ├── WorkDirManager.java
    │   │   ├── InputFilePrepareService.java
    │   │   └── ResultFileCollector.java
    │   └── support/
    │       ├── CommandBuilder.java
    │       ├── NodeInfoCollector.java
    │       └── LogPushBuffer.java
    ├── config/
    │   ├── NodeAgentConfig.java
    │   ├── ExecutorConfig.java
    │   └── FeignClientConfig.java
    └── support/
        └── AgentTempFileCleaner.java
```

## 2. 最关键的几个类

- `DispatchController`
- `TaskDispatchManager`
- `TaskExecuteManager`
- `SolverExecutor`
- `AbstractSolverExecutor`
- `MockExecutor`
- `ProcessRunner`
- `TaskReportClient`

## 3. 为什么这样分

因为 node-agent 其实有三件事：

- 作为节点进行注册和心跳
- 作为执行器运行任务
- 作为上报器把日志结果回传

分开之后最稳。



-------

# 八、node-agent 初始化代码骨架清单

你的后端设计里已经明确：这里是独立项目，要接收 `/internal/dispatch-task`，并通过执行器体系调用真实求解器或 MockExecutor。

## 1. Controller

### `interfaces/controller/DispatchController.java`

职责：接收调度下发

建议方法：

* `dispatch(DispatchTaskRequest request)`

---

## 2. Application

### `application/service/NodeRegisterAppService.java`

职责：节点注册

建议方法：

* `registerSelf()`

---

### `application/service/HeartbeatAppService.java`

职责：周期心跳

建议方法：

* `sendHeartbeat()`

---

### `application/service/TaskReportAppService.java`

职责：统一上报 task-service

建议方法：

* `reportStatus(Long taskId, String status)`
* `reportLog(Long taskId, Integer seqNo, String content)`
* `reportResultSummary(Long taskId, ExecutionResult result)`
* `reportResultFile(Long taskId, File resultFile)`
* `markFinished(Long taskId)`
* `markFailed(Long taskId, String failType, String failMessage)`

---

### `application/manager/TaskDispatchManager.java`

职责：接任务并启动执行

建议方法：

* `acceptTask(DispatchTaskRequest request)`
* `buildExecutionContext(DispatchTaskRequest request)`
* `startExecuteAsync(ExecutionContext context)`

---

### `application/manager/TaskExecuteManager.java`

职责：真正执行流程

建议方法：

* `execute(ExecutionContext context)`
* `prepareWorkDir(ExecutionContext context)`
* `prepareInputFiles(ExecutionContext context)`
* `selectExecutor(ExecutionContext context)`
* `runSolver(ExecutionContext context)`
* `collectResults(ExecutionContext context)`

---

### `application/manager/TaskReportManager.java`

职责：执行过程中的上报协调

建议方法：

* `reportRunning(ExecutionContext context)`
* `pushLog(Long taskId, String line)`
* `reportSuccess(ExecutionContext context, ExecutionResult result)`
* `reportFail(ExecutionContext context, Exception ex)`

---

### `application/scheduler/HeartbeatJob.java`

职责：周期心跳

建议方法：

* `sendHeartbeat()`

---

## 3. Domain

### `domain/model/ExecutionContext.java`

字段建议：

* `taskId`
* `solverId`
* `profileId`
* `taskType`
* `workDir`
* `inputDir`
* `resultDir`
* `paramsJson`

建议方法：

* `buildCommand()`

---

### `domain/model/ExecutionResult.java`

字段建议：

* `success`
* `durationSeconds`
* `summaryText`
* `metrics`
* `resultFiles`

---

### `domain/service/ExecutorSelectDomainService.java`

职责：选择执行器

建议方法：

* `selectExecutor(Long solverId, String taskType)`

---

### `domain/service/ExecutionContextBuildService.java`

职责：构建上下文

建议方法：

* `build(DispatchTaskRequest request)`

---

### `domain/executor/SolverExecutor.java`

职责：统一执行器接口

建议方法：

* `supports(ExecutionContext context)`
* `execute(ExecutionContext context)`

---

### `domain/executor/AbstractSolverExecutor.java`

职责：公共执行流程抽象

建议方法：

* `prepare(ExecutionContext context)`
* `doExecute(ExecutionContext context)`
* `parseResult(ExecutionContext context)`

---

### `domain/executor/MockExecutor.java`

职责：先跑通闭环

建议方法：

* `supports(...)`
* `execute(...)`

---

### `domain/executor/OpenFoamExecutor.java`

职责：后续真实接入

---

### `domain/executor/CalculixExecutor.java`

职责：后续真实接入

---

## 4. Infrastructure

### `infrastructure/client/TaskReportClient.java`

职责：调 task-service 回传

建议方法：

* `reportStatus(...)`
* `reportLog(...)`
* `reportResultSummary(...)`
* `reportResultFile(...)`
* `markFinished(...)`
* `markFailed(...)`

---

### `infrastructure/client/SchedulerNodeClient.java`

职责：调 scheduler-service 注册和心跳

建议方法：

* `register(NodeDTO node)`
* `heartbeat(NodeDTO node)`

---

### `infrastructure/process/ProcessRunner.java`

职责：运行命令

建议方法：

* `run(List<String> command, File workDir, Consumer<String> stdoutConsumer, Consumer<String> stderrConsumer)`

---

### `infrastructure/process/ProcessLogReader.java`

职责：实时读日志

建议方法：

* `read(InputStream inputStream, Consumer<String> lineConsumer)`

---

### `infrastructure/process/ProcessExitHandler.java`

职责：处理退出码

建议方法：

* `handleExitCode(int exitCode)`

---

### `infrastructure/storage/WorkDirManager.java`

职责：工作目录管理

建议方法：

* `prepareTaskDirs(Long taskId)`
* `cleanupTaskDirs(Long taskId)`

---

### `infrastructure/storage/InputFilePrepareService.java`

职责：准备输入文件

建议方法：

* `prepare(ExecutionContext context)`

---

### `infrastructure/storage/ResultFileCollector.java`

职责：收集结果文件

建议方法：

* `collect(ExecutionContext context)`

---

### `infrastructure/support/CommandBuilder.java`

职责：拼接执行命令

建议方法：

* `buildCommand(ExecutionContext context)`

---

### `infrastructure/support/NodeInfoCollector.java`

职责：采集 CPU、内存、运行数

建议方法：

* `collectNodeInfo()`

---

### `infrastructure/support/LogPushBuffer.java`

职责：日志缓存批量推送

建议方法：

* `append(String line)`
* `flush(Long taskId)`

--------

可以。下面我继续给你 **`node-agent` 的完整初始化代码骨架**，保持和前面 `task-service`、`scheduler-service` 一样的粒度，并且严格对齐你现在已经统一后的后端设计口径：

- `node-agent` 是**独立项目**
- 对外核心入口是 `POST /internal/dispatch-task`
- 核心能力是 **执行器体系**
- 本地执行流程是：**准备输入 → 构建命令 → 执行进程 → 读取日志 → 解析结果 → 回传 task-service**
- 回传接口统一使用：
  `status-report / log-report / result-summary-report / result-file-report / mark-finished / mark-failed`

------

# 一、node-agent 的定位

`node-agent` 不是中心业务微服务，而是部署在计算节点上的**边缘执行程序**。它的职责很明确：

1. 启动时向 `scheduler-service` 注册
2. 周期性发送心跳
3. 接收调度服务下发的任务
4. 根据任务选择合适的执行器
5. 调用真实求解器或 MockExecutor 执行
6. 实时读取 stdout/stderr 并上报日志
7. 解析结果摘要与结果文件
8. 回传任务最终状态、日志和结果

所以它不是简单的“一个 Controller + 一个 Service”，而应该拆成：

- **Controller**：接任务
- **Application/Manager**：执行流程编排
- **Domain/Executor**：执行器体系
- **Infrastructure/Process**：进程调用与日志读取
- **Infrastructure/Client**：向中心服务回传

------

# 二、node-agent 最终推荐包树

```text
node-agent/
└── src/main/java/com/example/cae/nodeagent/
    ├── NodeAgentApplication.java
    ├── interfaces/
    │   ├── controller/
    │   │   └── DispatchController.java
    │   ├── request/
    │   │   ├── DispatchTaskRequest.java
    │   │   └── CancelTaskRequest.java
    │   └── response/
    │       ├── DispatchTaskResponse.java
    │       └── CancelTaskResponse.java
    ├── application/
    │   ├── service/
    │   │   ├── NodeRegisterAppService.java
    │   │   ├── HeartbeatAppService.java
    │   │   └── TaskReportAppService.java
    │   ├── manager/
    │   │   ├── TaskDispatchManager.java
    │   │   ├── TaskExecuteManager.java
    │   │   └── TaskReportManager.java
    │   ├── assembler/
    │   │   └── ExecutionContextAssembler.java
    │   └── scheduler/
    │       └── HeartbeatJob.java
    ├── domain/
    │   ├── model/
    │   │   ├── ExecutionContext.java
    │   │   ├── ExecutionResult.java
    │   │   ├── InputFileMeta.java
    │   │   └── NodeInfo.java
    │   ├── service/
    │   │   ├── ExecutorSelectDomainService.java
    │   │   └── ExecutionContextBuildService.java
    │   ├── executor/
    │   │   ├── SolverExecutor.java
    │   │   ├── AbstractSolverExecutor.java
    │   │   ├── MockExecutor.java
    │   │   ├── OpenFoamExecutor.java
    │   │   └── CalculixExecutor.java
    │   └── enums/
    │       └── ExecutionStageEnum.java
    ├── infrastructure/
    │   ├── client/
    │   │   ├── SchedulerNodeClient.java
    │   │   └── TaskReportClient.java
    │   ├── process/
    │   │   ├── ProcessRunner.java
    │   │   ├── ProcessLogReader.java
    │   │   └── ProcessExitHandler.java
    │   ├── storage/
    │   │   ├── WorkDirManager.java
    │   │   ├── InputFilePrepareService.java
    │   │   └── ResultFileCollector.java
    │   └── support/
    │       ├── CommandBuilder.java
    │       ├── NodeInfoCollector.java
    │       └── LogPushBuffer.java
    ├── config/
    │   ├── NodeAgentConfig.java
    │   ├── ExecutorConfig.java
    │   ├── FeignClientConfig.java
    │   └── ThreadPoolConfig.java
    └── support/
        └── AgentTempFileCleaner.java
```

这个结构和你前面已经定下来的“执行器为核心、支持多种求解器、日志实时读取、回传中心服务”的设计是完全对齐的。

------

# 三、node-agent 的核心调用链

你先记住这条主链：

## 1. 接收任务

```
DispatchController -> TaskDispatchManager
```

## 2. 启动执行

```
TaskDispatchManager -> TaskExecuteManager
```

## 3. 执行器选择

```
TaskExecuteManager -> ExecutorSelectDomainService -> SolverExecutor
```

## 4. 日志与结果回传

```
TaskExecuteManager -> TaskReportManager -> TaskReportClient
```

## 5. 心跳注册

```
HeartbeatJob / NodeRegisterAppService -> SchedulerNodeClient
```

这样以后代码不会乱。

------

# 四、request / response 设计

------

## 1. DispatchTaskRequest.java

这个类必须和接口设计文档里的下发任务结构对齐，字段至少包括：

```java
public class DispatchTaskRequest {
    private Long taskId;
    private String taskNo;
    private Long solverId;
    private String solverCode;
    private Long profileId;
    private String taskType;
    private String commandTemplate;
    private String parserName;
    private Integer timeoutSeconds;
    private List<InputFileMeta> inputFiles;
    private Map<String, Object> params;
}
```

这和接口设计里 `dispatch-task` 的请求体是一致的。

------

## 2. CancelTaskRequest.java

增强项，先预留：

```java
public class CancelTaskRequest {
    private Long taskId;
    private String reason;
}
```

------

## 3. DispatchTaskResponse.java

```java
public class DispatchTaskResponse {
    private Boolean accepted;
    private String message;
}
```

------

## 4. CancelTaskResponse.java

```java
public class CancelTaskResponse {
    private Boolean accepted;
    private String message;
}
```

------

# 五、Domain Model 设计

------

## 1. ExecutionContext.java

这是 node-agent 的核心上下文对象，所有执行流程都围绕它展开。

### 建议字段

```java
public class ExecutionContext {
    private Long taskId;
    private String taskNo;
    private Long solverId;
    private String solverCode;
    private Long profileId;
    private String taskType;
    private String commandTemplate;
    private String parserName;
    private Integer timeoutSeconds;
    private List<InputFileMeta> inputFiles;
    private Map<String, Object> params;

    private String workDir;
    private String inputDir;
    private String outputDir;
    private String logDir;
}
```

### 建议方法

```java
public boolean isTimeoutEnabled()
public boolean hasInputFiles()
public boolean hasParser()
```

------

## 2. ExecutionResult.java

### 建议字段

```java
public class ExecutionResult {
    private Boolean success;
    private Integer durationSeconds;
    private String summaryText;
    private Map<String, Object> metrics;
    private List<File> resultFiles;
}
```

### 建议方法

```java
public static ExecutionResult success(...)
public static ExecutionResult fail(...)
```

------

## 3. InputFileMeta.java

```java
public class InputFileMeta {
    private String fileKey;
    private String originName;
    private String storagePath;
}
```

------

## 4. NodeInfo.java

```java
public class NodeInfo {
    private String nodeCode;
    private String nodeName;
    private String host;
    private Integer port;
    private Integer maxConcurrency;
    private BigDecimal cpuUsage;
    private BigDecimal memoryUsage;
    private Integer runningCount;
    private List<Long> solverIds;
}
```

------

# 六、Controller 层完整骨架

------

## 1. DispatchController.java

这是 node-agent 的唯一核心对外接口入口。

```java
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class DispatchController {

    private final TaskDispatchManager taskDispatchManager;

    @PostMapping("/dispatch-task")
    public Result<DispatchTaskResponse> dispatch(@RequestBody @Valid DispatchTaskRequest request) {
        taskDispatchManager.acceptTask(request);
        DispatchTaskResponse response = new DispatchTaskResponse();
        response.setAccepted(true);
        response.setMessage("task accepted");
        return Result.success(response);
    }

    @PostMapping("/cancel-task")
    public Result<CancelTaskResponse> cancel(@RequestBody @Valid CancelTaskRequest request) {
        taskDispatchManager.cancelTask(request);
        CancelTaskResponse response = new CancelTaskResponse();
        response.setAccepted(true);
        response.setMessage("cancel signal sent");
        return Result.success(response);
    }
}
```

这里统一使用 `/internal/dispatch-task`，和你已经修正后的设计一致，不再使用旧的 `/internal/dispatch`。

------

# 七、Application 层完整骨架

------

## 1. NodeRegisterAppService.java

职责：节点启动时向 `scheduler-service` 注册。

```java
@Service
@RequiredArgsConstructor
public class NodeRegisterAppService {

    private final SchedulerNodeClient schedulerNodeClient;
    private final NodeInfoCollector nodeInfoCollector;

    public void registerSelf() {
        NodeInfo nodeInfo = nodeInfoCollector.collectNodeInfo();
        schedulerNodeClient.register(nodeInfo);
    }
}
```

------

## 2. HeartbeatAppService.java

职责：周期性心跳。

```java
@Service
@RequiredArgsConstructor
public class HeartbeatAppService {

    private final SchedulerNodeClient schedulerNodeClient;
    private final NodeInfoCollector nodeInfoCollector;

    public void sendHeartbeat() {
        NodeInfo nodeInfo = nodeInfoCollector.collectNodeInfo();
        schedulerNodeClient.heartbeat(nodeInfo);
    }
}
```

------

## 3. TaskReportAppService.java

职责：统一封装回传 `task-service` 的动作。

```java
@Service
@RequiredArgsConstructor
public class TaskReportAppService {

    private final TaskReportClient taskReportClient;

    public void reportRunning(Long taskId) {
        taskReportClient.reportStatus(taskId, "RUNNING", "节点已启动执行");
    }

    public void reportLog(Long taskId, Integer seqNo, String content) {
        taskReportClient.reportLog(taskId, seqNo, content);
    }

    public void reportResultSummary(Long taskId, ExecutionResult result) {
        taskReportClient.reportResultSummary(taskId, result);
    }

    public void reportResultFile(Long taskId, File resultFile) {
        taskReportClient.reportResultFile(taskId, resultFile);
    }

    public void markFinished(Long taskId) {
        taskReportClient.markFinished(taskId);
    }

    public void markFailed(Long taskId, String failType, String failMessage) {
        taskReportClient.markFailed(taskId, failType, failMessage);
    }
}
```

------

# 八、Manager 层完整骨架

这层是 node-agent 的核心。

------

## 1. TaskDispatchManager.java

职责：接收任务并启动异步执行。

```java
@Service
@RequiredArgsConstructor
public class TaskDispatchManager {

    private final ExecutionContextBuildService executionContextBuildService;
    private final TaskExecuteManager taskExecuteManager;
    private final Executor taskExecutor;

    public void acceptTask(DispatchTaskRequest request) {
        ExecutionContext context = executionContextBuildService.build(request);
        taskExecutor.execute(() -> taskExecuteManager.execute(context));
    }

    public void cancelTask(CancelTaskRequest request) {
        // 增强项，先预留
        // 可后续根据 taskId 查进程并发送中断信号
    }
}
```

------

## 2. TaskExecuteManager.java

职责：真正编排执行全流程。

```java
@Service
@RequiredArgsConstructor
public class TaskExecuteManager {

    private final WorkDirManager workDirManager;
    private final InputFilePrepareService inputFilePrepareService;
    private final ExecutorSelectDomainService executorSelectDomainService;
    private final TaskReportManager taskReportManager;

    public void execute(ExecutionContext context) {
        long start = System.currentTimeMillis();
        try {
            prepareWorkDir(context);
            prepareInputFiles(context);

            taskReportManager.reportRunning(context);

            SolverExecutor executor = selectExecutor(context);
            ExecutionResult result = executor.execute(context);

            taskReportManager.reportSuccess(context, result, start);
        } catch (Exception ex) {
            taskReportManager.reportFail(context, ex);
        }
    }

    public void prepareWorkDir(ExecutionContext context) {
        workDirManager.prepareTaskDirs(context);
    }

    public void prepareInputFiles(ExecutionContext context) {
        inputFilePrepareService.prepare(context);
    }

    public SolverExecutor selectExecutor(ExecutionContext context) {
        return executorSelectDomainService.selectExecutor(context);
    }
}
```

------

## 3. TaskReportManager.java

职责：将运行状态、日志、结果统一协调回传。

```java
@Service
@RequiredArgsConstructor
public class TaskReportManager {

    private final TaskReportAppService taskReportAppService;

    public void reportRunning(ExecutionContext context) {
        taskReportAppService.reportRunning(context.getTaskId());
    }

    public void pushLog(Long taskId, Integer seqNo, String line) {
        taskReportAppService.reportLog(taskId, seqNo, line);
    }

    public void reportSuccess(ExecutionContext context, ExecutionResult result, long startMillis) {
        taskReportAppService.reportResultSummary(context.getTaskId(), result);

        if (result.getResultFiles() != null) {
            for (File file : result.getResultFiles()) {
                taskReportAppService.reportResultFile(context.getTaskId(), file);
            }
        }

        taskReportAppService.markFinished(context.getTaskId());
    }

    public void reportFail(ExecutionContext context, Exception ex) {
        taskReportAppService.markFailed(
                context.getTaskId(),
                "RUNTIME_ERROR",
                ex.getMessage()
        );
    }
}
```

------

# 九、Heartbeat 定时任务骨架

## 1. HeartbeatJob.java

```java
@Component
@RequiredArgsConstructor
public class HeartbeatJob {

    private final HeartbeatAppService heartbeatAppService;

    @Scheduled(fixedDelay = 10000)
    public void sendHeartbeat() {
        heartbeatAppService.sendHeartbeat();
    }
}
```

建议启动时额外调用一次 `registerSelf()`，之后再定时发心跳。

------

# 十、Domain Service / Executor 层骨架

------

## 1. ExecutionContextBuildService.java

职责：由下发请求构建执行上下文。

```java
@Service
public class ExecutionContextBuildService {

    public ExecutionContext build(DispatchTaskRequest request) {
        ExecutionContext context = new ExecutionContext();
        context.setTaskId(request.getTaskId());
        context.setTaskNo(request.getTaskNo());
        context.setSolverId(request.getSolverId());
        context.setSolverCode(request.getSolverCode());
        context.setProfileId(request.getProfileId());
        context.setTaskType(request.getTaskType());
        context.setCommandTemplate(request.getCommandTemplate());
        context.setParserName(request.getParserName());
        context.setTimeoutSeconds(request.getTimeoutSeconds());
        context.setInputFiles(request.getInputFiles());
        context.setParams(request.getParams());
        return context;
    }
}
```

------

## 2. ExecutorSelectDomainService.java

职责：选择具体执行器。

```java
@Service
@RequiredArgsConstructor
public class ExecutorSelectDomainService {

    private final List<SolverExecutor> executors;

    public SolverExecutor selectExecutor(ExecutionContext context) {
        return executors.stream()
                .filter(executor -> executor.supports(context))
                .findFirst()
                .orElseThrow(() -> new BizException("未找到可用执行器"));
    }
}
```

------

## 3. SolverExecutor.java

统一执行器接口。

```java
public interface SolverExecutor {

    boolean supports(ExecutionContext context);

    ExecutionResult execute(ExecutionContext context);
}
```

这和你文档里“执行器接口是核心”的定位完全一致。

------

## 4. AbstractSolverExecutor.java

职责：抽象公共执行流程。

```java
public abstract class AbstractSolverExecutor implements SolverExecutor {

    @Override
    public ExecutionResult execute(ExecutionContext context) {
        prepare(context);
        return doExecute(context);
    }

    protected void prepare(ExecutionContext context) {
        // 默认留空
    }

    protected abstract ExecutionResult doExecute(ExecutionContext context);
}
```

------

## 5. MockExecutor.java

最先实现，优先打通最小闭环。

```java
@Component
@RequiredArgsConstructor
public class MockExecutor extends AbstractSolverExecutor {

    private final TaskReportManager taskReportManager;

    @Override
    public boolean supports(ExecutionContext context) {
        return "MOCK".equalsIgnoreCase(context.getSolverCode()) || context.getSolverId() == 0;
    }

    @Override
    protected ExecutionResult doExecute(ExecutionContext context) {
        try {
            for (int i = 1; i <= 5; i++) {
                Thread.sleep(1000);
                taskReportManager.pushLog(context.getTaskId(), i, "mock running step " + i);
            }

            ExecutionResult result = new ExecutionResult();
            result.setSuccess(true);
            result.setDurationSeconds(5);
            result.setSummaryText("mock solver execute success");
            result.setMetrics(Map.of("iterations", 5));
            result.setResultFiles(Collections.emptyList());
            return result;
        } catch (Exception e) {
            throw new RuntimeException("mock execute failed", e);
        }
    }
}
```

你的后端设计里也明确写了“MockExecutor 先实现”，这点非常重要。

------

## 6. OpenFoamExecutor.java

先给骨架，后面再填真实执行逻辑。

```java
@Component
@RequiredArgsConstructor
public class OpenFoamExecutor extends AbstractSolverExecutor {

    private final CommandBuilder commandBuilder;
    private final ProcessRunner processRunner;
    private final ResultFileCollector resultFileCollector;
    private final TaskReportManager taskReportManager;

    @Override
    public boolean supports(ExecutionContext context) {
        return "OPENFOAM".equalsIgnoreCase(context.getSolverCode());
    }

    @Override
    protected ExecutionResult doExecute(ExecutionContext context) {
        List<String> command = commandBuilder.buildCommand(context);

        processRunner.run(
                command,
                new File(context.getWorkDir()),
                line -> taskReportManager.pushLog(context.getTaskId(), null, line),
                line -> taskReportManager.pushLog(context.getTaskId(), null, line)
        );

        ExecutionResult result = new ExecutionResult();
        result.setSuccess(true);
        result.setSummaryText("OpenFOAM execute success");
        result.setResultFiles(resultFileCollector.collect(context));
        return result;
    }
}
```

------

## 7. CalculixExecutor.java

```java
@Component
@RequiredArgsConstructor
public class CalculixExecutor extends AbstractSolverExecutor {

    @Override
    public boolean supports(ExecutionContext context) {
        return "CALCULIX".equalsIgnoreCase(context.getSolverCode());
    }

    @Override
    protected ExecutionResult doExecute(ExecutionContext context) {
        // 先留骨架，后续补真实命令执行
        ExecutionResult result = new ExecutionResult();
        result.setSuccess(true);
        result.setSummaryText("Calculix execute success");
        return result;
    }
}
```

------

# 十一、Infrastructure / Client 层骨架

------

## 1. SchedulerNodeClient.java

职责：调 `scheduler-service` 的注册和心跳接口。

```java
@Component
@RequiredArgsConstructor
public class SchedulerNodeClient {

    private final RestTemplate restTemplate;
    private final NodeAgentConfig nodeAgentConfig;

    public void register(NodeInfo nodeInfo) {
        String url = nodeAgentConfig.getSchedulerBaseUrl() + "/internal/nodes/register";
        restTemplate.postForObject(url, nodeInfo, Void.class);
    }

    public void heartbeat(NodeInfo nodeInfo) {
        String url = nodeAgentConfig.getSchedulerBaseUrl() + "/internal/nodes/heartbeat";
        restTemplate.postForObject(url, nodeInfo, Void.class);
    }
}
```

这里和你之前给 `scheduler-service` 设计的内部接口是一致的。

------

## 2. TaskReportClient.java

职责：调 `task-service` 回传执行状态。

```java
@Component
@RequiredArgsConstructor
public class TaskReportClient {

    private final RestTemplate restTemplate;
    private final NodeAgentConfig nodeAgentConfig;

    public void reportStatus(Long taskId, String status, String reason) {
        String url = nodeAgentConfig.getTaskServiceBaseUrl() + "/internal/tasks/" + taskId + "/status-report";
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        body.put("reason", reason);
        restTemplate.postForObject(url, body, Void.class);
    }

    public void reportLog(Long taskId, Integer seqNo, String content) {
        String url = nodeAgentConfig.getTaskServiceBaseUrl() + "/internal/tasks/" + taskId + "/log-report";
        Map<String, Object> body = new HashMap<>();
        body.put("seqNo", seqNo);
        body.put("content", content);
        restTemplate.postForObject(url, body, Void.class);
    }

    public void reportResultSummary(Long taskId, ExecutionResult result) {
        String url = nodeAgentConfig.getTaskServiceBaseUrl() + "/internal/tasks/" + taskId + "/result-summary-report";
        Map<String, Object> body = new HashMap<>();
        body.put("success", result.getSuccess());
        body.put("durationSeconds", result.getDurationSeconds());
        body.put("summaryText", result.getSummaryText());
        body.put("metrics", result.getMetrics());
        restTemplate.postForObject(url, body, Void.class);
    }

    public void reportResultFile(Long taskId, File file) {
        String url = nodeAgentConfig.getTaskServiceBaseUrl() + "/internal/tasks/" + taskId + "/result-file-report";
        Map<String, Object> body = new HashMap<>();
        body.put("fileType", "RESULT");
        body.put("fileName", file.getName());
        body.put("storagePath", file.getAbsolutePath());
        body.put("fileSize", file.length());
        restTemplate.postForObject(url, body, Void.class);
    }

    public void markFinished(Long taskId) {
        String url = nodeAgentConfig.getTaskServiceBaseUrl() + "/internal/tasks/" + taskId + "/mark-finished";
        restTemplate.postForObject(url, null, Void.class);
    }

    public void markFailed(Long taskId, String failType, String failMessage) {
        String url = nodeAgentConfig.getTaskServiceBaseUrl()
                + "/internal/tasks/" + taskId + "/mark-failed?failType=" + failType + "&failMessage=" + failMessage;
        restTemplate.postForObject(url, null, Void.class);
    }
}
```

这和接口设计里 node-agent 执行后回传 `status-report / log-report / result-summary-report / result-file-report / mark-finished / mark-failed` 的链路一致。

------

# 十二、Infrastructure / Process 层骨架

------

## 1. ProcessRunner.java

职责：真正执行命令。

```java
@Component
public class ProcessRunner {

    public int run(List<String> command,
                   File workDir,
                   Consumer<String> stdoutConsumer,
                   Consumer<String> stderrConsumer) {

        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(workDir);
            Process process = builder.start();

            new Thread(() -> read(process.getInputStream(), stdoutConsumer)).start();
            new Thread(() -> read(process.getErrorStream(), stderrConsumer)).start();

            return process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException("process run failed", e);
        }
    }

    private void read(InputStream inputStream, Consumer<String> consumer) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                consumer.accept(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
```

这正对应你文档里的本地执行流程：`ProcessBuilder` 执行命令并实时读取日志。

------

## 2. ProcessLogReader.java

如果你想把日志读取从 `ProcessRunner` 中拆出来，可以进一步独立：

```java
@Component
public class ProcessLogReader {

    public void read(InputStream inputStream, Consumer<String> lineConsumer) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineConsumer.accept(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("read process log failed", e);
        }
    }
}
```

------

## 3. ProcessExitHandler.java

```java
@Component
public class ProcessExitHandler {

    public void checkExitCode(int exitCode) {
        if (exitCode != 0) {
            throw new BizException("solver exit code != 0");
        }
    }
}
```

------

# 十三、Infrastructure / Storage 层骨架

------

## 1. WorkDirManager.java

```java
@Component
public class WorkDirManager {

    public void prepareTaskDirs(ExecutionContext context) {
        String root = "/data/tasks/" + context.getTaskId();
        context.setWorkDir(root);
        context.setInputDir(root + "/input");
        context.setOutputDir(root + "/output");
        context.setLogDir(root + "/log");

        new File(context.getInputDir()).mkdirs();
        new File(context.getOutputDir()).mkdirs();
        new File(context.getLogDir()).mkdirs();
    }

    public void cleanupTaskDirs(Long taskId) {
        // 可选增强项，先预留
    }
}
```

------

## 2. InputFilePrepareService.java

职责：把中心存储里的输入文件准备到本地工作目录。

```java
@Component
public class InputFilePrepareService {

    public void prepare(ExecutionContext context) {
        if (context.getInputFiles() == null) {
            return;
        }

        for (InputFileMeta file : context.getInputFiles()) {
            // 开发阶段可简单 copy
            // 后续可支持 unzip / dir flatten / rename
        }
    }
}
```

------

## 3. ResultFileCollector.java

```java
@Component
public class ResultFileCollector {

    public List<File> collect(ExecutionContext context) {
        File outputDir = new File(context.getOutputDir());
        File[] files = outputDir.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(files);
    }
}
```

------

# 十四、Support 层骨架

------

## 1. CommandBuilder.java

```java
@Component
public class CommandBuilder {

    public List<String> buildCommand(ExecutionContext context) {
        if (context.getCommandTemplate() == null) {
            throw new BizException("commandTemplate 不能为空");
        }
        return Arrays.asList("bash", "-lc", context.getCommandTemplate());
    }
}
```

------

## 2. NodeInfoCollector.java

```java
@Component
public class NodeInfoCollector {

    public NodeInfo collectNodeInfo() {
        NodeInfo info = new NodeInfo();
        info.setNodeCode("node-01");
        info.setNodeName("计算节点01");
        info.setHost("127.0.0.1");
        info.setPort(9090);
        info.setMaxConcurrency(2);
        info.setCpuUsage(BigDecimal.ZERO);
        info.setMemoryUsage(BigDecimal.ZERO);
        info.setRunningCount(0);
        info.setSolverIds(List.of(0L, 1L, 2L));
        return info;
    }
}
```

------

## 3. LogPushBuffer.java

如果后面你想把日志改成“积累几行再上报一次”，可以加这个缓冲器：

```java
@Component
public class LogPushBuffer {

    private final List<String> buffer = new ArrayList<>();

    public void append(String line) {
        buffer.add(line);
    }

    public List<String> drain() {
        List<String> copy = new ArrayList<>(buffer);
        buffer.clear();
        return copy;
    }
}
```

毕设最小闭环阶段可以先不用。

------

# 十五、Config 层骨架

------

## 1. NodeAgentApplication.java

```java
@SpringBootApplication
@EnableScheduling
public class NodeAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(NodeAgentApplication.class, args);
    }
}
```

------

## 2. NodeAgentConfig.java

```java
@Data
@Configuration
@ConfigurationProperties(prefix = "node.agent")
public class NodeAgentConfig {
    private String schedulerBaseUrl;
    private String taskServiceBaseUrl;
}
```

------

## 3. ExecutorConfig.java

如果要自定义线程池：

```java
@Configuration
public class ThreadPoolConfig {

    @Bean
    public Executor taskExecutor() {
        return Executors.newFixedThreadPool(4);
    }
}
```

------

# 十六、最小可运行实现顺序

不要一次写完，按这个顺序最稳：

## 第一批：先打通最小执行闭环

先建这些类：

- `DispatchController`
- `DispatchTaskRequest`
- `TaskDispatchManager`
- `TaskExecuteManager`
- `ExecutionContext`
- `ExecutionContextBuildService`
- `SolverExecutor`
- `MockExecutor`
- `TaskReportManager`
- `TaskReportClient`

先做到：

- 能接收 `/internal/dispatch-task`
- 能调用 `MockExecutor`
- 能回传 success / failed

这一步最符合你现在整个系统“先跑通 Mock 闭环”的目标。

## 第二批：补节点注册与心跳

再建：

- `NodeRegisterAppService`
- `HeartbeatAppService`
- `HeartbeatJob`
- `SchedulerNodeClient`
- `NodeInfoCollector`

做到：

- 启动注册
- 周期心跳

## 第三批：补真实命令执行骨架

再建：

- `CommandBuilder`
- `ProcessRunner`
- `WorkDirManager`
- `InputFilePrepareService`
- `ResultFileCollector`
- `OpenFoamExecutor`
- `CalculixExecutor`

做到：

- 可以替换掉 mock，接真实 solver

------

# 十七、你现在最该先建的 12 个类

如果你想立刻开工，我建议 node-agent 优先建下面这 12 个：

1. `DispatchController`
2. `DispatchTaskRequest`
3. `DispatchTaskResponse`
4. `TaskDispatchManager`
5. `TaskExecuteManager`
6. `TaskReportManager`
7. `ExecutionContext`
8. `ExecutionContextBuildService`
9. `SolverExecutor`
10. `AbstractSolverExecutor`
11. `MockExecutor`
12. `TaskReportClient`

这 12 个类搭起来，node-agent 主骨架就出来了。

------

# 十八、最终建议

`node-agent` 最怕的两个坑是：

- 把所有执行逻辑写进 `DispatchController`
- 把所有求解器执行逻辑写进一个 `execute()` 方法

你现在这版骨架已经把它拆开了：

- `DispatchController` 只接任务
- `TaskDispatchManager` 只负责接收并异步启动
- `TaskExecuteManager` 负责总执行流程
- `ExecutorSelectDomainService` 负责选执行器
- `SolverExecutor` 体系负责不同求解器实现
- `TaskReportManager` 负责统一回传

这就是比较清晰、规范、后面也不容易乱的结构。
而且它和你前面已经定下来的 `task-service`、`scheduler-service` 结构是能完整拼起来的。

--------

