# 七、task-service 项目结构设计

这是全系统最核心、也最容易写乱的服务，必须重点设计。

## 1. 推荐结构

```text
task-service/
└── src/main/java/com/yourorg/task/
    ├── TaskApplication.java
    ├── interfaces/
    │   ├── controller/
    │   ├── internal/
    │   ├── request/
    │   └── response/
    ├── application/
    │   ├── service/
    │   ├── facade/
    │   ├── manager/
    │   ├── assembler/
    │   └── executor/
    ├── domain/
    │   ├── model/
    │   ├── repository/
    │   ├── service/
    │   ├── enums/
    │   └── rule/
    ├── infrastructure/
    │   ├── persistence/
    │   │   ├── entity/
    │   │   ├── mapper/
    │   │   └── repository/
    │   ├── client/
    │   ├── storage/
    │   └── support/
    ├── config/
    └── support/
```

## 2. 为什么要比别的服务更细

因为它同时承担：

- 任务创建
- 文件上传
- 文件校验
- 状态流转
- 日志处理
- 结果处理
- 内部回调
- 多服务协同

所以一定要把“流程编排”和“领域规则”拆开。

## 3. 重点目录说明

### interfaces/controller

对外接口：

- `TaskController`
- `TaskQueryController`
- `TaskLogController`
- `TaskResultController`

### interfaces/internal

对内接口：

- `InternalTaskDispatchController`
- `InternalTaskReportController`

建议把内部接口和外部接口分目录，避免混淆。

### interfaces/request

- `CreateTaskRequest`
- `ValidateTaskRequest`
- `SubmitTaskRequest`
- `CancelTaskRequest`
- `TaskListQueryRequest`

### interfaces/response

- `TaskDetailResponse`
- `TaskListItemResponse`
- `TaskStatusHistoryResponse`
- `TaskLogResponse`
- `TaskResultSummaryResponse`

### application/manager

这里是 task-service 的核心，建议至少拆成：

- `TaskLifecycleManager`
- `TaskValidationManager`
- `TaskDispatchManager`
- `TaskResultManager`

分别负责：

- 生命周期流转
- 校验编排
- 调度相关编排
- 结果与日志收尾

### application/service

放更偏应用层的服务：

- `TaskCommandAppService`
- `TaskQueryAppService`
- `TaskLogAppService`
- `TaskResultAppService`

### application/executor

这里不是 node-agent 的执行器，而是 task-service 内部的“命令执行编排器”，例如：

- `TaskCreateExecutor`
- `TaskValidateExecutor`
- `TaskSubmitExecutor`

如果你想让流程更清晰，可以用这种方式；如果不想太复杂，也可以只保留 manager。

### domain/model

建议按业务实体拆：

- `Task`
- `TaskFile`
- `TaskStatusHistory`
- `TaskResultSummary`
- `TaskResultFile`
- `TaskLogChunk`

### domain/rule

这里很重要，建议专门放业务规则：

- `TaskStatusRule`
- `TaskValidationRule`
- `TaskCancelRule`

### domain/service

- `TaskDomainService`
- `TaskStatusDomainService`
- `TaskValidationDomainService`

### infrastructure/client

- `SolverClient`
- `SchedulerClient`

### infrastructure/storage

- `TaskFileStorageService`
- `LocalTaskFileStorageService`

### infrastructure/support

- `TaskNoGenerator`
- `TaskPathResolver`
- `LogChunkAppender`

## 4. 强烈建议的聚合划分

task-service 里建议按三个聚合理解：

### 聚合 1：Task 主聚合

包含：

- Task
- TaskStatusHistory

### 聚合 2：Task 文件聚合

包含：

- TaskFile

### 聚合 3：Task 结果聚合

包含：

- TaskResultSummary
- TaskResultFile
- TaskLogChunk

这样你写代码时会很清楚：

- 创建任务更新哪些对象
- 校验文件更新哪些对象
- 运行结束更新哪些对象

## 5. task-service 最推荐的包命名例子

```text
task-service
├── interfaces/controller/TaskController.java
├── interfaces/internal/InternalTaskReportController.java
├── application/manager/TaskLifecycleManager.java
├── application/service/TaskCommandAppService.java
├── domain/model/Task.java
├── domain/rule/TaskStatusRule.java
├── infrastructure/client/SolverClient.java
├── infrastructure/storage/LocalTaskFileStorageService.java
└── infrastructure/persistence/repository/TaskRepositoryImpl.java
```

这个结构很清晰，后面维护起来也不容易乱。

----------

# 七、task-service 完整包树

这是最核心的服务。你的后端设计已经明确：`task-service` 需要额外的 `manager` 层，并且承担创建、校验、提交、状态流转、日志、结果、内部回调等复杂职责。

## 1. 完整结构

```text
task-service/
└── src/main/java/com/example/cae/task/
    ├── TaskApplication.java
    ├── interfaces/
    │   ├── controller/
    │   │   ├── TaskController.java
    │   │   ├── TaskQueryController.java
    │   │   ├── TaskLogController.java
    │   │   └── TaskResultController.java
    │   ├── internal/
    │   │   ├── InternalTaskDispatchController.java
    │   │   └── InternalTaskReportController.java
    │   ├── request/
    │   │   ├── CreateTaskRequest.java
    │   │   ├── ValidateTaskRequest.java
    │   │   ├── SubmitTaskRequest.java
    │   │   ├── CancelTaskRequest.java
    │   │   ├── TaskListQueryRequest.java
    │   │   ├── StatusReportRequest.java
    │   │   ├── LogReportRequest.java
    │   │   ├── ResultSummaryReportRequest.java
    │   │   └── ResultFileReportRequest.java
    │   └── response/
    │       ├── TaskCreateResponse.java
    │       ├── TaskListItemResponse.java
    │       ├── TaskDetailResponse.java
    │       ├── TaskStatusHistoryResponse.java
    │       ├── TaskFileResponse.java
    │       ├── TaskLogResponse.java
    │       ├── TaskResultSummaryResponse.java
    │       └── TaskResultFileResponse.java
    ├── application/
    │   ├── service/
    │   │   ├── TaskCommandAppService.java
    │   │   ├── TaskQueryAppService.java
    │   │   ├── TaskLogAppService.java
    │   │   └── TaskResultAppService.java
    │   ├── facade/
    │   │   ├── TaskCommandFacade.java
    │   │   └── TaskQueryFacade.java
    │   ├── manager/
    │   │   ├── TaskLifecycleManager.java
    │   │   ├── TaskValidationManager.java
    │   │   ├── TaskDispatchManager.java
    │   │   └── TaskResultManager.java
    │   └── assembler/
    │       ├── TaskAssembler.java
    │       ├── TaskLogAssembler.java
    │       └── TaskResultAssembler.java
    ├── domain/
    │   ├── model/
    │   │   ├── Task.java
    │   │   ├── TaskFile.java
    │   │   ├── TaskStatusHistory.java
    │   │   ├── TaskLogChunk.java
    │   │   ├── TaskResultSummary.java
    │   │   └── TaskResultFile.java
    │   ├── repository/
    │   │   ├── TaskRepository.java
    │   │   ├── TaskFileRepository.java
    │   │   ├── TaskStatusHistoryRepository.java
    │   │   ├── TaskLogRepository.java
    │   │   ├── TaskResultSummaryRepository.java
    │   │   └── TaskResultFileRepository.java
    │   ├── service/
    │   │   ├── TaskDomainService.java
    │   │   ├── TaskStatusDomainService.java
    │   │   ├── TaskValidationDomainService.java
    │   │   └── TaskResultDomainService.java
    │   ├── rule/
    │   │   ├── TaskStatusRule.java
    │   │   ├── TaskValidationRule.java
    │   │   └── TaskCancelRule.java
    │   └── enums/
    │       ├── TaskQueryScopeEnum.java
    │       └── TaskSortFieldEnum.java
    ├── infrastructure/
    │   ├── persistence/
    │   │   ├── entity/
    │   │   │   ├── TaskPO.java
    │   │   │   ├── TaskFilePO.java
    │   │   │   ├── TaskStatusHistoryPO.java
    │   │   │   ├── TaskLogChunkPO.java
    │   │   │   ├── TaskResultSummaryPO.java
    │   │   │   └── TaskResultFilePO.java
    │   │   ├── mapper/
    │   │   │   ├── TaskMapper.java
    │   │   │   ├── TaskFileMapper.java
    │   │   │   ├── TaskStatusHistoryMapper.java
    │   │   │   ├── TaskLogChunkMapper.java
    │   │   │   ├── TaskResultSummaryMapper.java
    │   │   │   └── TaskResultFileMapper.java
    │   │   └── repository/
    │   │       ├── TaskRepositoryImpl.java
    │   │       ├── TaskFileRepositoryImpl.java
    │   │       ├── TaskStatusHistoryRepositoryImpl.java
    │   │       ├── TaskLogRepositoryImpl.java
    │   │       ├── TaskResultSummaryRepositoryImpl.java
    │   │       └── TaskResultFileRepositoryImpl.java
    │   ├── client/
    │   │   ├── SolverClient.java
    │   │   └── SchedulerClient.java
    │   ├── storage/
    │   │   ├── TaskFileStorageService.java
    │   │   └── LocalTaskFileStorageService.java
    │   └── support/
    │       ├── TaskNoGenerator.java
    │       ├── TaskPathResolver.java
    │       ├── LogChunkAppender.java
    │       └── TaskQueryBuilder.java
    ├── config/
    │   ├── TaskServiceConfig.java
    │   ├── FeignClientConfig.java
    │   └── MultipartConfig.java
    └── support/
        └── TaskPermissionChecker.java
```

## 2. 这几个类最关键

- `TaskController`
- `InternalTaskReportController`
- `TaskLifecycleManager`
- `TaskValidationManager`
- `TaskStatusDomainService`
- `TaskStatusRule`
- `SolverClient`
- `LocalTaskFileStorageService`

## 3. 结构上的关键解释

### 为什么要 `interfaces/internal`

因为这个服务既有外部接口，也有内部接口：

- 外部接口给前端
- 内部接口给 scheduler-service 和 node-agent

分开后最清楚。

### 为什么要 `application/manager`

这是你原后端设计里已经强调的重点。任务逻辑过重，不能只靠一个 `TaskServiceImpl`。

### 为什么要 `domain/rule`

因为状态流转规则、取消规则、校验规则不应散落在 controller/service 里。



--------

# 六、task-service 初始化代码骨架清单

这是最核心的一块。你当前后端设计里最明确的一点，就是这里必须保留 **manager 层**，至少承担 `createTask / validateTask / submitTask / updateStatus / handleResult` 这些核心流程。

## 1. Controller

### `interfaces/controller/TaskController.java`

职责：

* 创建任务
* 上传文件
* 校验任务
* 提交任务
* 取消任务

建议方法：

* `createTask(CreateTaskRequest request)`
* `uploadTaskFiles(Long taskId, MultipartFile[] files)`
* `validateTask(Long taskId)`
* `submitTask(Long taskId)`
* `cancelTask(Long taskId)`

---

### `interfaces/controller/TaskQueryController.java`

职责：

* 任务列表
* 任务详情
* 状态历史
* 文件列表

建议方法：

* `pageMyTasks(TaskListQueryRequest request)`
* `pageAdminTasks(TaskListQueryRequest request)`
* `getTaskDetail(Long taskId)`
* `getTaskStatusHistory(Long taskId)`
* `getTaskFiles(Long taskId)`

---

### `interfaces/controller/TaskLogController.java`

职责：日志查询/下载

建议方法：

* `getLogs(Long taskId, Integer fromSeq, Integer pageSize)`
* `downloadLogs(Long taskId)`

---

### `interfaces/controller/TaskResultController.java`

职责：结果摘要与文件下载

建议方法：

* `getResultSummary(Long taskId)`
* `getResultFiles(Long taskId)`
* `downloadResultFile(Long fileId)`

---

### `interfaces/internal/InternalTaskDispatchController.java`

职责：提供给 scheduler-service 的内部接口

建议方法：

* `listQueuedTasks()`
* `markScheduled(Long taskId, Long nodeId)`
* `markDispatched(Long taskId, Long nodeId)`

---

### `interfaces/internal/InternalTaskReportController.java`

职责：提供给 node-agent 的内部回传接口

建议方法：

* `reportStatus(Long taskId, StatusReportRequest request)`
* `reportLog(Long taskId, LogReportRequest request)`
* `reportResultSummary(Long taskId, ResultSummaryReportRequest request)`
* `reportResultFile(Long taskId, ResultFileReportRequest request)`
* `markFinished(Long taskId)`
* `markFailed(Long taskId)`

---

## 2. Application

### `application/service/TaskCommandAppService.java`

职责：对外任务命令流程

建议方法：

* `createTask(CreateTaskRequest request, Long userId)`
* `uploadTaskFiles(Long taskId, MultipartFile[] files, Long userId)`
* `validateTask(Long taskId, Long userId)`
* `submitTask(Long taskId, Long userId)`
* `cancelTask(Long taskId, Long userId)`

---

### `application/service/TaskQueryAppService.java`

职责：查询流程

建议方法：

* `pageMyTasks(TaskListQueryRequest request, Long userId)`
* `pageAdminTasks(TaskListQueryRequest request)`
* `getTaskDetail(Long taskId, Long userId, String roleCode)`
* `getTaskStatusHistory(Long taskId, Long userId, String roleCode)`
* `getTaskFiles(Long taskId, Long userId, String roleCode)`

---

### `application/service/TaskLogAppService.java`

建议方法：

* `getLogs(Long taskId, Integer fromSeq, Integer pageSize, Long userId, String roleCode)`
* `downloadLogs(Long taskId, Long userId, String roleCode)`

---

### `application/service/TaskResultAppService.java`

建议方法：

* `getResultSummary(Long taskId, Long userId, String roleCode)`
* `getResultFiles(Long taskId, Long userId, String roleCode)`
* `downloadResultFile(Long fileId, Long userId, String roleCode)`

---

## 3. Manager

### `application/manager/TaskLifecycleManager.java`

职责：生命周期总协调

建议方法：

* `createTask(CreateTaskRequest request, Long userId)`
* `submitTask(Long taskId, Long userId)`
* `cancelTask(Long taskId, Long userId)`
* `markScheduled(Long taskId, Long nodeId)`
* `markDispatched(Long taskId, Long nodeId)`
* `markRunning(Long taskId)`
* `markSuccess(Long taskId)`
* `markFailed(Long taskId, String failType, String failMessage)`

---

### `application/manager/TaskValidationManager.java`

职责：任务校验编排

建议方法：

* `validateTask(Long taskId, Long userId)`
* `loadProfileRules(Long profileId)`
* `validateForm(Task task)`
* `validateFiles(Task task, List<TaskFile> files, List<FileRuleDTO> rules)`

---

### `application/manager/TaskDispatchManager.java`

职责：调度协作

建议方法：

* `listQueuedTasks()`
* `markScheduled(Long taskId, Long nodeId)`
* `markDispatched(Long taskId, Long nodeId)`

---

### `application/manager/TaskResultManager.java`

职责：日志、结果、完成收尾

建议方法：

* `appendLog(Long taskId, Integer seqNo, String content)`
* `saveResultSummary(Long taskId, ResultSummaryReportRequest request)`
* `saveResultFile(Long taskId, ResultFileReportRequest request)`
* `finishTask(Long taskId)`
* `failTask(Long taskId, String failType, String failMessage)`

---

## 4. Domain

### `domain/model/Task.java`

建议方法：

* `submit()`
* `cancel()`
* `bindNode(Long nodeId)`
* `markValidated()`
* `markScheduled()`
* `markDispatched()`
* `markRunning()`
* `markSuccess()`
* `markFailed(String failType, String failMessage)`

---

### `domain/model/TaskFile.java`

建议方法：

* `isInputFile()`
* `isArchiveFile()`

---

### `domain/model/TaskStatusHistory.java`

---

### `domain/model/TaskLogChunk.java`

---

### `domain/model/TaskResultSummary.java`

---

### `domain/model/TaskResultFile.java`

---

### `domain/service/TaskStatusDomainService.java`

职责：集中处理状态流转

建议方法：

* `transfer(Task task, String targetStatus, String reason, String operatorType, Long operatorId)`
* `checkCanTransfer(String fromStatus, String toStatus)`
* `writeHistory(Long taskId, String fromStatus, String toStatus, String reason, String operatorType, Long operatorId)`

---

### `domain/service/TaskValidationDomainService.java`

职责：纯校验规则

建议方法：

* `checkTaskEditable(Task task)`
* `checkTaskCanSubmit(Task task)`
* `checkFilesMatchRules(List<TaskFile> files, List<FileRuleDTO> rules)`

---

### `domain/rule/TaskStatusRule.java`

职责：状态机规则集中定义

建议方法：

* `canTransfer(String fromStatus, String toStatus)`
* `isFinished(String status)`

---

### `domain/rule/TaskCancelRule.java`

职责：取消规则

建议方法：

* `canCancel(String status)`

---

## 5. Infrastructure

### `infrastructure/client/SolverClient.java`

职责：调 solver-service

建议方法：

* `getProfileDetail(Long profileId)`
* `getFileRules(Long profileId)`
* `getUploadSpec(Long profileId)`

---

### `infrastructure/client/SchedulerClient.java`

职责：如需主动通知 scheduler-service，可预留

建议方法：

* `notifyTaskSubmitted(Long taskId)`

---

### `infrastructure/storage/TaskFileStorageService.java`

职责：抽象文件存储

建议方法：

* `saveInputFile(Long taskId, MultipartFile file)`
* `openFile(String storagePath)`
* `deleteFile(String storagePath)`

---

### `infrastructure/storage/LocalTaskFileStorageService.java`

职责：本地文件系统实现

---

### `infrastructure/support/TaskNoGenerator.java`

建议方法：

* `generateTaskNo()`

---

### `infrastructure/support/TaskPathResolver.java`

建议方法：

* `resolveTaskRoot(Long taskId)`
* `resolveInputDir(Long taskId)`
* `resolveLogFile(Long taskId)`
* `resolveResultDir(Long taskId)`

---

### `infrastructure/support/LogChunkAppender.java`

建议方法：

* `append(Long taskId, Integer seqNo, String content)`

----------



# 一、task-service 的定位

`task-service` 负责的核心能力有 8 个：

1. 创建任务
2. 上传输入文件
3. 校验任务
4. 提交任务
5. 查询任务
6. 状态流转
7. 日志管理
8. 结果管理

同时它还要对接两个外部角色：

- 对前端暴露业务接口
- 对 `scheduler-service` 和 `node-agent` 暴露内部接口

所以它必须分层清晰，不能写成一个大 `TaskServiceImpl`。

------

# 二、task-service 最终推荐包树

```text
task-service/
└── src/main/java/com/example/cae/task/
    ├── TaskApplication.java
    ├── interfaces/
    │   ├── controller/
    │   │   ├── TaskController.java
    │   │   ├── TaskQueryController.java
    │   │   ├── TaskLogController.java
    │   │   └── TaskResultController.java
    │   ├── internal/
    │   │   ├── InternalTaskDispatchController.java
    │   │   └── InternalTaskReportController.java
    │   ├── request/
    │   └── response/
    ├── application/
    │   ├── service/
    │   ├── facade/
    │   ├── manager/
    │   └── assembler/
    ├── domain/
    │   ├── model/
    │   ├── repository/
    │   ├── service/
    │   ├── rule/
    │   └── enums/
    ├── infrastructure/
    │   ├── persistence/
    │   │   ├── entity/
    │   │   ├── mapper/
    │   │   └── repository/
    │   ├── client/
    │   ├── storage/
    │   └── support/
    ├── config/
    └── support/
```

------

# 三、先给你总调用链

你先记住这一条主链：

## 1. 对外命令链

```
Controller -> AppService -> Manager -> DomainService/Repository -> Infrastructure
```

## 2. 对外查询链

```
Controller -> AppService -> Repository -> Assembler -> Response
```

## 3. 内部回传链

```
InternalController -> Manager -> DomainService -> Repository
```

------

# 四、核心领域对象设计

先定最核心的 `domain/model`。

------

## 1. Task.java

这是主聚合根。

### 建议字段

```java
public class Task {
    private Long id;
    private String taskNo;
    private String taskName;
    private Long userId;
    private Long solverId;
    private Long profileId;
    private String taskType;
    private String status;
    private Integer priority;
    private Long nodeId;
    private String paramsJson;
    private LocalDateTime submitTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String failType;
    private String failMessage;
    private Integer deletedFlag;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 建议方法

```java
public void markValidated()
public void submit()
public void bindNode(Long nodeId)
public void markScheduled()
public void markDispatched()
public void markRunning()
public void markSuccess()
public void markFailed(String failType, String failMessage)
public void cancel()
public boolean isOwner(Long userId)
public boolean isFinished()
```

### 说明

这里的方法只做**领域状态变化**，不直接写数据库。

------

## 2. TaskFile.java

### 建议字段

```java
public class TaskFile {
    private Long id;
    private Long taskId;
    private String fileRole;
    private String fileKey;
    private String originName;
    private String storagePath;
    private Long fileSize;
    private String fileSuffix;
    private String checksum;
    private LocalDateTime createdAt;
}
```

### 建议方法

```java
public boolean isInputFile()
public boolean isArchiveFile()
public boolean matchFileKey(String fileKey)
```

------

## 3. TaskStatusHistory.java

### 建议字段

```java
public class TaskStatusHistory {
    private Long id;
    private Long taskId;
    private String fromStatus;
    private String toStatus;
    private String changeReason;
    private String operatorType;
    private Long operatorId;
    private LocalDateTime createdAt;
}
```

------

## 4. TaskLogChunk.java

### 建议字段

```java
public class TaskLogChunk {
    private Long id;
    private Long taskId;
    private Integer seqNo;
    private String logContent;
    private LocalDateTime createdAt;
}
```

------

## 5. TaskResultSummary.java

### 建议字段

```java
public class TaskResultSummary {
    private Long id;
    private Long taskId;
    private Integer successFlag;
    private Integer durationSeconds;
    private String summaryText;
    private String metricsJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

------

## 6. TaskResultFile.java

### 建议字段

```java
public class TaskResultFile {
    private Long id;
    private Long taskId;
    private String fileType;
    private String fileName;
    private String storagePath;
    private Long fileSize;
    private LocalDateTime createdAt;
}
```

------

# 五、request / response 设计

------

## 1. interfaces/request

### CreateTaskRequest.java

```java
public class CreateTaskRequest {
    private String taskName;
    private Long solverId;
    private Long profileId;
    private String taskType;
    private Map<String, Object> paramsJson;
}
```

### SubmitTaskRequest.java

可为空壳，后面扩展：

```java
public class SubmitTaskRequest {
}
```

### ValidateTaskRequest.java

也可以先空壳：

```java
public class ValidateTaskRequest {
}
```

### CancelTaskRequest.java

```java
public class CancelTaskRequest {
    private String reason;
}
```

### TaskListQueryRequest.java

```java
public class TaskListQueryRequest {
    private Integer pageNum;
    private Integer pageSize;
    private String taskName;
    private String taskNo;
    private String status;
    private Long solverId;
    private Long profileId;
    private Long nodeId;
    private Long userId;
}
```

### StatusReportRequest.java

```java
public class StatusReportRequest {
    private String status;
    private String reason;
}
```

### LogReportRequest.java

```java
public class LogReportRequest {
    private Integer seqNo;
    private String content;
}
```

### ResultSummaryReportRequest.java

```java
public class ResultSummaryReportRequest {
    private Boolean success;
    private Integer durationSeconds;
    private String summaryText;
    private Map<String, Object> metrics;
}
```

### ResultFileReportRequest.java

```java
public class ResultFileReportRequest {
    private String fileType;
    private String fileName;
    private String storagePath;
    private Long fileSize;
}
```

------

## 2. interfaces/response

### TaskCreateResponse.java

```java
public class TaskCreateResponse {
    private Long taskId;
    private String status;
}
```

### TaskListItemResponse.java

```java
public class TaskListItemResponse {
    private Long taskId;
    private String taskNo;
    private String taskName;
    private Long solverId;
    private Long profileId;
    private String taskType;
    private String status;
    private Long nodeId;
    private LocalDateTime submitTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
```

### TaskDetailResponse.java

```java
public class TaskDetailResponse {
    private Long taskId;
    private String taskNo;
    private String taskName;
    private Long userId;
    private Long solverId;
    private Long profileId;
    private String taskType;
    private String status;
    private Long nodeId;
    private String paramsJson;
    private String failType;
    private String failMessage;
    private LocalDateTime submitTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
```

### TaskStatusHistoryResponse.java

### TaskFileResponse.java

### TaskLogResponse.java

### TaskResultSummaryResponse.java

### TaskResultFileResponse.java

这些都按字段直出即可，不复杂。

------

# 六、Controller 层完整骨架

------

## 1. TaskController.java

职责：任务命令操作。

```java
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskCommandAppService taskCommandAppService;

    @PostMapping
    public Result<TaskCreateResponse> createTask(@RequestBody @Valid CreateTaskRequest request) {
        Long userId = LoginUserHolder.getUserId();
        return Result.success(taskCommandAppService.createTask(request, userId));
    }

    @PostMapping("/{taskId}/files")
    public Result<Void> uploadTaskFiles(@PathVariable Long taskId,
                                        @RequestPart("files") MultipartFile[] files) {
        Long userId = LoginUserHolder.getUserId();
        taskCommandAppService.uploadTaskFiles(taskId, files, userId);
        return Result.success();
    }

    @PostMapping("/{taskId}/validate")
    public Result<Void> validateTask(@PathVariable Long taskId) {
        Long userId = LoginUserHolder.getUserId();
        taskCommandAppService.validateTask(taskId, userId);
        return Result.success();
    }

    @PostMapping("/{taskId}/submit")
    public Result<Void> submitTask(@PathVariable Long taskId) {
        Long userId = LoginUserHolder.getUserId();
        taskCommandAppService.submitTask(taskId, userId);
        return Result.success();
    }

    @PostMapping("/{taskId}/cancel")
    public Result<Void> cancelTask(@PathVariable Long taskId,
                                   @RequestBody(required = false) CancelTaskRequest request) {
        Long userId = LoginUserHolder.getUserId();
        taskCommandAppService.cancelTask(taskId, userId, request == null ? null : request.getReason());
        return Result.success();
    }
}
```

------

## 2. TaskQueryController.java

```java
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskQueryController {

    private final TaskQueryAppService taskQueryAppService;

    @GetMapping
    public Result<PageResult<TaskListItemResponse>> pageMyTasks(TaskListQueryRequest request) {
        Long userId = LoginUserHolder.getUserId();
        return Result.success(taskQueryAppService.pageMyTasks(request, userId));
    }

    @GetMapping("/{taskId}")
    public Result<TaskDetailResponse> getTaskDetail(@PathVariable Long taskId) {
        Long userId = LoginUserHolder.getUserId();
        String roleCode = LoginUserHolder.getRoleCode();
        return Result.success(taskQueryAppService.getTaskDetail(taskId, userId, roleCode));
    }

    @GetMapping("/{taskId}/status-history")
    public Result<List<TaskStatusHistoryResponse>> getTaskStatusHistory(@PathVariable Long taskId) {
        Long userId = LoginUserHolder.getUserId();
        String roleCode = LoginUserHolder.getRoleCode();
        return Result.success(taskQueryAppService.getTaskStatusHistory(taskId, userId, roleCode));
    }

    @GetMapping("/{taskId}/files")
    public Result<List<TaskFileResponse>> getTaskFiles(@PathVariable Long taskId) {
        Long userId = LoginUserHolder.getUserId();
        String roleCode = LoginUserHolder.getRoleCode();
        return Result.success(taskQueryAppService.getTaskFiles(taskId, userId, roleCode));
    }
}
```

------

## 3. TaskLogController.java

```java
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskLogController {

    private final TaskLogAppService taskLogAppService;

    @GetMapping("/{taskId}/logs")
    public Result<List<TaskLogResponse>> getLogs(@PathVariable Long taskId,
                                                 @RequestParam(defaultValue = "0") Integer fromSeq,
                                                 @RequestParam(defaultValue = "100") Integer pageSize) {
        Long userId = LoginUserHolder.getUserId();
        String roleCode = LoginUserHolder.getRoleCode();
        return Result.success(taskLogAppService.getLogs(taskId, fromSeq, pageSize, userId, roleCode));
    }
}
```

------

## 4. TaskResultController.java

```java
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskResultController {

    private final TaskResultAppService taskResultAppService;

    @GetMapping("/{taskId}/result-summary")
    public Result<TaskResultSummaryResponse> getResultSummary(@PathVariable Long taskId) {
        Long userId = LoginUserHolder.getUserId();
        String roleCode = LoginUserHolder.getRoleCode();
        return Result.success(taskResultAppService.getResultSummary(taskId, userId, roleCode));
    }

    @GetMapping("/{taskId}/result-files")
    public Result<List<TaskResultFileResponse>> getResultFiles(@PathVariable Long taskId) {
        Long userId = LoginUserHolder.getUserId();
        String roleCode = LoginUserHolder.getRoleCode();
        return Result.success(taskResultAppService.getResultFiles(taskId, userId, roleCode));
    }
}
```

------

## 5. InternalTaskDispatchController.java

给 `scheduler-service` 用。

```java
@RestController
@RequestMapping("/internal/tasks")
@RequiredArgsConstructor
public class InternalTaskDispatchController {

    private final TaskDispatchManager taskDispatchManager;

    @GetMapping("/queued")
    public Result<List<TaskDTO>> listQueuedTasks() {
        return Result.success(taskDispatchManager.listQueuedTasks());
    }

    @PostMapping("/{taskId}/mark-scheduled")
    public Result<Void> markScheduled(@PathVariable Long taskId,
                                      @RequestParam Long nodeId) {
        taskDispatchManager.markScheduled(taskId, nodeId);
        return Result.success();
    }

    @PostMapping("/{taskId}/mark-dispatched")
    public Result<Void> markDispatched(@PathVariable Long taskId,
                                       @RequestParam Long nodeId) {
        taskDispatchManager.markDispatched(taskId, nodeId);
        return Result.success();
    }
}
```

------

## 6. InternalTaskReportController.java

给 `node-agent` 用。

```java
@RestController
@RequestMapping("/internal/tasks")
@RequiredArgsConstructor
public class InternalTaskReportController {

    private final TaskResultManager taskResultManager;
    private final TaskLifecycleManager taskLifecycleManager;

    @PostMapping("/{taskId}/status-report")
    public Result<Void> reportStatus(@PathVariable Long taskId,
                                     @RequestBody StatusReportRequest request) {
        taskLifecycleManager.reportStatus(taskId, request);
        return Result.success();
    }

    @PostMapping("/{taskId}/log-report")
    public Result<Void> reportLog(@PathVariable Long taskId,
                                  @RequestBody LogReportRequest request) {
        taskResultManager.appendLog(taskId, request.getSeqNo(), request.getContent());
        return Result.success();
    }

    @PostMapping("/{taskId}/result-summary-report")
    public Result<Void> reportResultSummary(@PathVariable Long taskId,
                                            @RequestBody ResultSummaryReportRequest request) {
        taskResultManager.saveResultSummary(taskId, request);
        return Result.success();
    }

    @PostMapping("/{taskId}/result-file-report")
    public Result<Void> reportResultFile(@PathVariable Long taskId,
                                         @RequestBody ResultFileReportRequest request) {
        taskResultManager.saveResultFile(taskId, request);
        return Result.success();
    }

    @PostMapping("/{taskId}/mark-finished")
    public Result<Void> markFinished(@PathVariable Long taskId) {
        taskResultManager.finishTask(taskId);
        return Result.success();
    }

    @PostMapping("/{taskId}/mark-failed")
    public Result<Void> markFailed(@PathVariable Long taskId,
                                   @RequestParam String failType,
                                   @RequestParam String failMessage) {
        taskResultManager.failTask(taskId, failType, failMessage);
        return Result.success();
    }
}
```

------

# 七、Application 层完整骨架

------

## 1. TaskCommandAppService.java

```java
@Service
@RequiredArgsConstructor
public class TaskCommandAppService {

    private final TaskLifecycleManager taskLifecycleManager;
    private final TaskValidationManager taskValidationManager;

    public TaskCreateResponse createTask(CreateTaskRequest request, Long userId) {
        return taskLifecycleManager.createTask(request, userId);
    }

    public void uploadTaskFiles(Long taskId, MultipartFile[] files, Long userId) {
        taskLifecycleManager.uploadTaskFiles(taskId, files, userId);
    }

    public void validateTask(Long taskId, Long userId) {
        taskValidationManager.validateTask(taskId, userId);
    }

    public void submitTask(Long taskId, Long userId) {
        taskLifecycleManager.submitTask(taskId, userId);
    }

    public void cancelTask(Long taskId, Long userId, String reason) {
        taskLifecycleManager.cancelTask(taskId, userId, reason);
    }
}
```

------

## 2. TaskQueryAppService.java

```java
@Service
@RequiredArgsConstructor
public class TaskQueryAppService {

    private final TaskRepository taskRepository;
    private final TaskFileRepository taskFileRepository;
    private final TaskStatusHistoryRepository taskStatusHistoryRepository;
    private final TaskAssembler taskAssembler;
    private final TaskPermissionChecker taskPermissionChecker;

    public PageResult<TaskListItemResponse> pageMyTasks(TaskListQueryRequest request, Long userId) { ... }

    public TaskDetailResponse getTaskDetail(Long taskId, Long userId, String roleCode) { ... }

    public List<TaskStatusHistoryResponse> getTaskStatusHistory(Long taskId, Long userId, String roleCode) { ... }

    public List<TaskFileResponse> getTaskFiles(Long taskId, Long userId, String roleCode) { ... }
}
```

------

## 3. TaskLogAppService.java

```java
@Service
@RequiredArgsConstructor
public class TaskLogAppService {

    private final TaskLogRepository taskLogRepository;
    private final TaskRepository taskRepository;
    private final TaskPermissionChecker taskPermissionChecker;
    private final TaskLogAssembler taskLogAssembler;

    public List<TaskLogResponse> getLogs(Long taskId, Integer fromSeq, Integer pageSize, Long userId, String roleCode) { ... }
}
```

------

## 4. TaskResultAppService.java

```java
@Service
@RequiredArgsConstructor
public class TaskResultAppService {

    private final TaskRepository taskRepository;
    private final TaskResultSummaryRepository taskResultSummaryRepository;
    private final TaskResultFileRepository taskResultFileRepository;
    private final TaskPermissionChecker taskPermissionChecker;
    private final TaskResultAssembler taskResultAssembler;

    public TaskResultSummaryResponse getResultSummary(Long taskId, Long userId, String roleCode) { ... }

    public List<TaskResultFileResponse> getResultFiles(Long taskId, Long userId, String roleCode) { ... }
}
```

------

# 八、Manager 层完整骨架

这是最关键的部分。

------

## 1. TaskLifecycleManager.java

```java
@Service
@RequiredArgsConstructor
public class TaskLifecycleManager {

    private final TaskRepository taskRepository;
    private final TaskFileRepository taskFileRepository;
    private final TaskStatusDomainService taskStatusDomainService;
    private final TaskValidationDomainService taskValidationDomainService;
    private final TaskFileStorageService taskFileStorageService;
    private final TaskAssembler taskAssembler;
    private final TaskNoGenerator taskNoGenerator;

    public TaskCreateResponse createTask(CreateTaskRequest request, Long userId) {
        Task task = taskAssembler.toTask(request, userId);
        task.setTaskNo(taskNoGenerator.generateTaskNo());
        task.setStatus(TaskStatusEnum.CREATED.name());
        taskRepository.save(task);
        return taskAssembler.toCreateResponse(task);
    }

    public void uploadTaskFiles(Long taskId, MultipartFile[] files, Long userId) { ... }

    public void submitTask(Long taskId, Long userId) { ... }

    public void cancelTask(Long taskId, Long userId, String reason) { ... }

    public void markScheduled(Long taskId, Long nodeId) { ... }

    public void markDispatched(Long taskId, Long nodeId) { ... }

    public void reportStatus(Long taskId, StatusReportRequest request) { ... }
}
```

### 它的职责

- 任务创建
- 文件上传
- 提交
- 调度阶段状态变更
- 取消
- node-agent 状态回传

------

## 2. TaskValidationManager.java

```java
@Service
@RequiredArgsConstructor
public class TaskValidationManager {

    private final TaskRepository taskRepository;
    private final TaskFileRepository taskFileRepository;
    private final SolverClient solverClient;
    private final TaskStatusDomainService taskStatusDomainService;
    private final TaskValidationDomainService taskValidationDomainService;

    public void validateTask(Long taskId, Long userId) {
        Task task = loadAndCheckOwner(taskId, userId);
        List<TaskFile> files = taskFileRepository.listByTaskId(taskId);
        List<FileRuleDTO> rules = solverClient.getFileRules(task.getProfileId());

        taskValidationDomainService.checkTaskEditable(task);
        taskValidationDomainService.checkFilesMatchRules(files, rules);

        taskStatusDomainService.transfer(
                task,
                TaskStatusEnum.VALIDATED.name(),
                "任务校验通过",
                OperatorTypeEnum.USER.name(),
                userId
        );

        taskRepository.update(task);
    }

    private Task loadAndCheckOwner(Long taskId, Long userId) { ... }
}
```

### 它的职责

只做校验编排，不做其他。

------

## 3. TaskDispatchManager.java

```java
@Service
@RequiredArgsConstructor
public class TaskDispatchManager {

    private final TaskRepository taskRepository;
    private final TaskStatusDomainService taskStatusDomainService;

    public List<TaskDTO> listQueuedTasks() {
        List<Task> tasks = taskRepository.listByStatus(TaskStatusEnum.QUEUED.name());
        return tasks.stream().map(this::toTaskDTO).toList();
    }

    public void markScheduled(Long taskId, Long nodeId) {
        Task task = taskRepository.findById(taskId);
        task.bindNode(nodeId);
        taskStatusDomainService.transfer(
                task,
                TaskStatusEnum.SCHEDULED.name(),
                "调度器已选择节点",
                OperatorTypeEnum.SYSTEM.name(),
                null
        );
        taskRepository.update(task);
    }

    public void markDispatched(Long taskId, Long nodeId) {
        Task task = taskRepository.findById(taskId);
        task.bindNode(nodeId);
        taskStatusDomainService.transfer(
                task,
                TaskStatusEnum.DISPATCHED.name(),
                "任务已下发到节点",
                OperatorTypeEnum.SYSTEM.name(),
                null
        );
        taskRepository.update(task);
    }

    private TaskDTO toTaskDTO(Task task) { ... }
}
```

------

## 4. TaskResultManager.java

```java
@Service
@RequiredArgsConstructor
public class TaskResultManager {

    private final TaskRepository taskRepository;
    private final TaskLogRepository taskLogRepository;
    private final TaskResultSummaryRepository taskResultSummaryRepository;
    private final TaskResultFileRepository taskResultFileRepository;
    private final TaskStatusDomainService taskStatusDomainService;

    public void appendLog(Long taskId, Integer seqNo, String content) { ... }

    public void saveResultSummary(Long taskId, ResultSummaryReportRequest request) { ... }

    public void saveResultFile(Long taskId, ResultFileReportRequest request) { ... }

    public void finishTask(Long taskId) {
        Task task = taskRepository.findById(taskId);
        taskStatusDomainService.transfer(
                task,
                TaskStatusEnum.SUCCESS.name(),
                "任务执行完成",
                OperatorTypeEnum.NODE.name(),
                null
        );
        taskRepository.update(task);
    }

    public void failTask(Long taskId, String failType, String failMessage) {
        Task task = taskRepository.findById(taskId);
        task.markFailed(failType, failMessage);
        taskStatusDomainService.transfer(
                task,
                TaskStatusEnum.FAILED.name(),
                failMessage,
                OperatorTypeEnum.NODE.name(),
                null
        );
        taskRepository.update(task);
    }
}
```

------

# 九、Domain Service / Rule 层骨架

------

## 1. TaskStatusRule.java

```java
@Component
public class TaskStatusRule {

    private static final Map<String, Set<String>> TRANSFER_MAP = new HashMap<>();

    static {
        TRANSFER_MAP.put("CREATED", Set.of("VALIDATED", "CANCELED"));
        TRANSFER_MAP.put("VALIDATED", Set.of("QUEUED", "CANCELED"));
        TRANSFER_MAP.put("QUEUED", Set.of("SCHEDULED", "CANCELED"));
        TRANSFER_MAP.put("SCHEDULED", Set.of("DISPATCHED", "FAILED"));
        TRANSFER_MAP.put("DISPATCHED", Set.of("RUNNING", "FAILED"));
        TRANSFER_MAP.put("RUNNING", Set.of("SUCCESS", "FAILED", "TIMEOUT", "CANCELED"));
    }

    public boolean canTransfer(String fromStatus, String toStatus) {
        return TRANSFER_MAP.getOrDefault(fromStatus, Collections.emptySet()).contains(toStatus);
    }

    public boolean isFinished(String status) {
        return Set.of("SUCCESS", "FAILED", "CANCELED", "TIMEOUT").contains(status);
    }
}
```

------

## 2. TaskCancelRule.java

```java
@Component
public class TaskCancelRule {

    public boolean canCancel(String status) {
        return Set.of("CREATED", "VALIDATED", "QUEUED", "RUNNING").contains(status);
    }
}
```

------

## 3. TaskValidationDomainService.java

```java
@Service
@RequiredArgsConstructor
public class TaskValidationDomainService {

    public void checkTaskEditable(Task task) {
        if (!Set.of("CREATED", "VALIDATED").contains(task.getStatus())) {
            throw new BizException("当前状态不允许校验");
        }
    }

    public void checkTaskCanSubmit(Task task) {
        if (!TaskStatusEnum.VALIDATED.name().equals(task.getStatus())) {
            throw new BizException("任务未校验通过，不能提交");
        }
    }

    public void checkFilesMatchRules(List<TaskFile> files, List<FileRuleDTO> rules) {
        // 先实现：检查必填文件是否齐全
        // 后续再增强：文件名模式、文件类型、目录结构
    }
}
```

------

## 4. TaskStatusDomainService.java

这是最关键的领域服务。

```java
@Service
@RequiredArgsConstructor
public class TaskStatusDomainService {

    private final TaskStatusRule taskStatusRule;
    private final TaskStatusHistoryRepository taskStatusHistoryRepository;

    public void transfer(Task task,
                         String targetStatus,
                         String reason,
                         String operatorType,
                         Long operatorId) {

        String fromStatus = task.getStatus();

        if (!taskStatusRule.canTransfer(fromStatus, targetStatus)) {
            throw new BizException("非法状态流转: " + fromStatus + " -> " + targetStatus);
        }

        switch (targetStatus) {
            case "VALIDATED" -> task.markValidated();
            case "QUEUED" -> task.submit();
            case "SCHEDULED" -> task.markScheduled();
            case "DISPATCHED" -> task.markDispatched();
            case "RUNNING" -> task.markRunning();
            case "SUCCESS" -> task.markSuccess();
            case "CANCELED" -> task.cancel();
            case "FAILED" -> {}
            case "TIMEOUT" -> {}
            default -> throw new BizException("不支持的目标状态");
        }

        TaskStatusHistory history = new TaskStatusHistory();
        history.setTaskId(task.getId());
        history.setFromStatus(fromStatus);
        history.setToStatus(targetStatus);
        history.setChangeReason(reason);
        history.setOperatorType(operatorType);
        history.setOperatorId(operatorId);

        taskStatusHistoryRepository.save(history);
    }
}
```

------

# 十、Repository 接口骨架

------

## 1. TaskRepository.java

```java
public interface TaskRepository {
    Task findById(Long taskId);
    void save(Task task);
    void update(Task task);
    PageResult<Task> pageMyTasks(TaskListQueryRequest request, Long userId);
    PageResult<Task> pageAdminTasks(TaskListQueryRequest request);
    List<Task> listByStatus(String status);
}
```

------

## 2. TaskFileRepository.java

```java
public interface TaskFileRepository {
    void saveBatch(List<TaskFile> files);
    List<TaskFile> listByTaskId(Long taskId);
}
```

------

## 3. TaskStatusHistoryRepository.java

```java
public interface TaskStatusHistoryRepository {
    void save(TaskStatusHistory history);
    List<TaskStatusHistory> listByTaskId(Long taskId);
}
```

------

## 4. TaskLogRepository.java

```java
public interface TaskLogRepository {
    void save(TaskLogChunk chunk);
    List<TaskLogChunk> listByTaskIdAndSeq(Long taskId, Integer fromSeq, Integer pageSize);
}
```

------

## 5. TaskResultSummaryRepository.java

```java
public interface TaskResultSummaryRepository {
    void saveOrUpdate(TaskResultSummary summary);
    TaskResultSummary findByTaskId(Long taskId);
}
```

------

## 6. TaskResultFileRepository.java

```java
public interface TaskResultFileRepository {
    void save(TaskResultFile file);
    List<TaskResultFile> listByTaskId(Long taskId);
    TaskResultFile findById(Long fileId);
}
```

------

# 十一、Persistence 层骨架

------

## 1. entity

建议类名：

- `TaskPO`
- `TaskFilePO`
- `TaskStatusHistoryPO`
- `TaskLogChunkPO`
- `TaskResultSummaryPO`
- `TaskResultFilePO`

这些字段和表字段一一对应，不写业务方法。

------

## 2. mapper

建议都继承 `BaseMapper<T>`：

```java
@Mapper
public interface TaskMapper extends BaseMapper<TaskPO> {
}
```

其余同理。

------

## 3. repository impl

### TaskRepositoryImpl.java

```java
@Repository
@RequiredArgsConstructor
public class TaskRepositoryImpl implements TaskRepository {

    private final TaskMapper taskMapper;
    private final TaskAssembler taskAssembler;

    @Override
    public Task findById(Long taskId) { ... }

    @Override
    public void save(Task task) { ... }

    @Override
    public void update(Task task) { ... }

    @Override
    public PageResult<Task> pageMyTasks(TaskListQueryRequest request, Long userId) { ... }

    @Override
    public PageResult<Task> pageAdminTasks(TaskListQueryRequest request) { ... }

    @Override
    public List<Task> listByStatus(String status) { ... }
}
```

其他 `RepositoryImpl` 同样模式。

------

# 十二、Client / Storage / Support 层骨架

------

## 1. SolverClient.java

```java
@FeignClient(name = "solver-service")
public interface SolverClient {

    @GetMapping("/api/profiles/{profileId}")
    Result<SolverProfileDTO> getProfileDetail(@PathVariable("profileId") Long profileId);

    @GetMapping("/api/profiles/{profileId}/file-rules")
    Result<List<FileRuleDTO>> getFileRules(@PathVariable("profileId") Long profileId);

    @GetMapping("/api/profiles/{profileId}/upload-spec")
    Result<Object> getUploadSpec(@PathVariable("profileId") Long profileId);
}
```

------

## 2. TaskFileStorageService.java

```java
public interface TaskFileStorageService {
    TaskFile saveInputFile(Long taskId, MultipartFile file);
    InputStream openFile(String storagePath);
    void deleteFile(String storagePath);
}
```

------

## 3. LocalTaskFileStorageService.java

```java
@Service
@RequiredArgsConstructor
public class LocalTaskFileStorageService implements TaskFileStorageService {

    private final TaskPathResolver taskPathResolver;

    @Override
    public TaskFile saveInputFile(Long taskId, MultipartFile file) { ... }

    @Override
    public InputStream openFile(String storagePath) { ... }

    @Override
    public void deleteFile(String storagePath) { ... }
}
```

------

## 4. TaskNoGenerator.java

```java
@Component
public class TaskNoGenerator {
    public String generateTaskNo() {
        return "TASK" + System.currentTimeMillis();
    }
}
```

------

## 5. TaskPathResolver.java

```java
@Component
public class TaskPathResolver {

    public String resolveTaskRoot(Long taskId) { ... }

    public String resolveInputDir(Long taskId) { ... }

    public String resolveLogDir(Long taskId) { ... }

    public String resolveResultDir(Long taskId) { ... }
}
```

------

## 6. TaskPermissionChecker.java

```java
@Component
public class TaskPermissionChecker {

    public void checkCanAccess(Task task, Long userId, String roleCode) {
        if ("ADMIN".equals(roleCode)) {
            return;
        }
        if (!task.isOwner(userId)) {
            throw new ForbiddenException("无权限访问该任务");
        }
    }
}
```

------

# 十三、Assembler 层骨架

Assembler 非常重要，用来隔离对象转换。

------

## 1. TaskAssembler.java

```java
@Component
public class TaskAssembler {

    public Task toTask(CreateTaskRequest request, Long userId) { ... }

    public TaskCreateResponse toCreateResponse(Task task) { ... }

    public TaskDetailResponse toDetailResponse(Task task) { ... }

    public TaskListItemResponse toListItemResponse(Task task) { ... }

    public TaskPO toPO(Task task) { ... }

    public Task fromPO(TaskPO po) { ... }
}
```

------

## 2. TaskLogAssembler.java

```java
@Component
public class TaskLogAssembler {

    public TaskLogResponse toResponse(TaskLogChunk chunk) { ... }

    public TaskLogChunkPO toPO(TaskLogChunk chunk) { ... }

    public TaskLogChunk fromPO(TaskLogChunkPO po) { ... }
}
```

------

## 3. TaskResultAssembler.java

```java
@Component
public class TaskResultAssembler {

    public TaskResultSummaryResponse toSummaryResponse(TaskResultSummary summary) { ... }

    public TaskResultFileResponse toFileResponse(TaskResultFile file) { ... }
}
```

------

# 十四、最小可运行实现顺序

你别一次全写，按这个顺序最稳：

## 第一批

先把这些建出来：

- `Task`
- `TaskController`
- `TaskQueryController`
- `TaskCommandAppService`
- `TaskQueryAppService`
- `TaskLifecycleManager`
- `TaskRepository`
- `TaskRepositoryImpl`
- `TaskMapper`
- `TaskAssembler`

先跑通：

- 创建任务
- 查询任务列表
- 查询任务详情

------

## 第二批

再补：

- `TaskStatusRule`
- `TaskStatusDomainService`
- `TaskStatusHistoryRepository`
- `InternalTaskDispatchController`
- `TaskDispatchManager`

跑通：

- 提交任务
- 查询队列任务
- 标记调度/下发

------

## 第三批

再补：

- `TaskFile`
- `TaskFileRepository`
- `TaskFileStorageService`
- `TaskValidationManager`
- `TaskValidationDomainService`
- `SolverClient`

跑通：

- 上传文件
- 校验任务

------

## 第四批

再补：

- `TaskLogRepository`
- `TaskResultSummaryRepository`
- `TaskResultFileRepository`
- `TaskResultManager`
- `InternalTaskReportController`

跑通：

- 日志回传
- 结果摘要回传
- 文件回传
- 成功/失败回传

------

# 十五、你现在最该先建的 12 个类

如果你只想先开工，我建议先建这 12 个：

1. `Task`
2. `TaskController`
3. `TaskQueryController`
4. `TaskCommandAppService`
5. `TaskQueryAppService`
6. `TaskLifecycleManager`
7. `TaskRepository`
8. `TaskRepositoryImpl`
9. `TaskMapper`
10. `TaskAssembler`
11. `TaskStatusRule`
12. `TaskStatusDomainService`

这 12 个搭起来，`task-service` 主骨架就出来了。

------

# 十六、最终建议

`task-service` 最怕两件事：

- 把所有逻辑塞进 Controller
- 把所有逻辑塞进一个 `TaskServiceImpl`

你现在这版骨架已经避开了这两个坑：

- **Controller** 很薄
- **AppService** 负责入口流程
- **Manager** 负责复杂编排
- **DomainService/Rule** 负责状态和校验规则
- **Repository** 负责持久化
- **Client/Storage** 负责外部依赖

这就是最适合你这个项目的 `task-service` 结构。

------

