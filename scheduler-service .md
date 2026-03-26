# 八、scheduler-service 项目结构设计

调度服务要突出“节点管理”和“调度编排”两件事。

## 1. 推荐结构

```text
scheduler-service/
└── src/main/java/com/yourorg/scheduler/
    ├── SchedulerApplication.java
    ├── interfaces/
    │   ├── controller/
    │   ├── internal/
    │   ├── request/
    │   └── response/
    ├── application/
    │   ├── service/
    │   ├── facade/
    │   ├── manager/
    │   ├── scheduler/
    │   └── assembler/
    ├── domain/
    │   ├── model/
    │   ├── repository/
    │   ├── service/
    │   ├── enums/
    │   └── strategy/
    ├── infrastructure/
    │   ├── persistence/
    │   │   ├── entity/
    │   │   ├── mapper/
    │   │   └── repository/
    │   ├── client/
    │   └── support/
    ├── config/
    └── support/
```

## 2. 重点目录说明

### interfaces/controller

- `NodeController`
- `ScheduleController`

### interfaces/internal

- `NodeRegisterController`
- `NodeHeartbeatController`

### application/scheduler

这个目录建议专门保留给定时调度任务：

- `TaskScheduleJob`
- `NodeOfflineCheckJob`

### application/manager

- `TaskScheduleManager`
- `NodeManageManager`

### domain/strategy

建议专门放调度策略：

- `ScheduleStrategy`
- `FcfsLeastLoadStrategy`

这样后面如果要扩展“优先级调度”“标签调度”会很方便。

### domain/model

- `ComputeNode`
- `NodeSolverCapability`
- `ScheduleRecord`

### infrastructure/client

- `TaskClient`
- `NodeAgentClient`

### infrastructure/support

- `NodeLoadCalculator`
- `NodeHeartbeatChecker`

## 3. 这个服务的关键规范

调度服务一定要做到：

- 调度逻辑不写在 Controller
- 定时任务不直接操作 Mapper
- 调度策略单独抽象
- 节点离线检查单独成任务

这样结构会非常稳。

----------

# 八、scheduler-service 完整包树

你的后端设计里已经明确这个服务需要：

- 节点管理
- 注册
- 心跳
- 定时调度器
- FCFS + 最小负载策略
- 节点离线判断
  这些职责非常适合再抽一层 `strategy` 和 `scheduler`。

## 1. 完整结构

```text
scheduler-service/
└── src/main/java/com/example/cae/scheduler/
    ├── SchedulerApplication.java
    ├── interfaces/
    │   ├── controller/
    │   │   ├── NodeController.java
    │   │   └── ScheduleController.java
    │   ├── internal/
    │   │   ├── NodeRegisterController.java
    │   │   └── NodeHeartbeatController.java
    │   ├── request/
    │   │   ├── NodeRegisterRequest.java
    │   │   ├── NodeHeartbeatRequest.java
    │   │   └── UpdateNodeStatusRequest.java
    │   └── response/
    │       ├── NodeListItemResponse.java
    │       ├── NodeDetailResponse.java
    │       ├── ScheduleRecordResponse.java
    │       └── AvailableNodeResponse.java
    ├── application/
    │   ├── service/
    │   │   ├── NodeAppService.java
    │   │   └── ScheduleAppService.java
    │   ├── facade/
    │   │   ├── NodeFacade.java
    │   │   └── ScheduleFacade.java
    │   ├── manager/
    │   │   ├── TaskScheduleManager.java
    │   │   └── NodeManageManager.java
    │   ├── scheduler/
    │   │   ├── TaskScheduleJob.java
    │   │   └── NodeOfflineCheckJob.java
    │   └── assembler/
    │       ├── NodeAssembler.java
    │       └── ScheduleAssembler.java
    ├── domain/
    │   ├── model/
    │   │   ├── ComputeNode.java
    │   │   ├── NodeSolverCapability.java
    │   │   └── ScheduleRecord.java
    │   ├── repository/
    │   │   ├── ComputeNodeRepository.java
    │   │   ├── NodeSolverCapabilityRepository.java
    │   │   └── ScheduleRecordRepository.java
    │   ├── service/
    │   │   ├── NodeDomainService.java
    │   │   └── ScheduleDomainService.java
    │   ├── strategy/
    │   │   ├── ScheduleStrategy.java
    │   │   └── FcfsLeastLoadStrategy.java
    │   └── enums/
    │       └── ScheduleStatusEnum.java
    ├── infrastructure/
    │   ├── persistence/
    │   │   ├── entity/
    │   │   │   ├── ComputeNodePO.java
    │   │   │   ├── NodeSolverCapabilityPO.java
    │   │   │   └── ScheduleRecordPO.java
    │   │   ├── mapper/
    │   │   │   ├── ComputeNodeMapper.java
    │   │   │   ├── NodeSolverCapabilityMapper.java
    │   │   │   └── ScheduleRecordMapper.java
    │   │   └── repository/
    │   │       ├── ComputeNodeRepositoryImpl.java
    │   │       ├── NodeSolverCapabilityRepositoryImpl.java
    │   │       └── ScheduleRecordRepositoryImpl.java
    │   ├── client/
    │   │   ├── TaskClient.java
    │   │   └── NodeAgentClient.java
    │   └── support/
    │       ├── NodeLoadCalculator.java
    │       ├── NodeHeartbeatChecker.java
    │       └── AvailableNodeSelector.java
    ├── config/
    │   ├── SchedulerServiceConfig.java
    │   ├── SchedulingConfig.java
    │   └── FeignClientConfig.java
    └── support/
        └── ScheduleLogWriter.java
```

## 2. 最关键的几个类

- `TaskScheduleJob`
- `TaskScheduleManager`
- `FcfsLeastLoadStrategy`
- `TaskClient`
- `NodeAgentClient`
- `NodeHeartbeatChecker`

----------

# 七、scheduler-service 初始化代码骨架清单

你的后端设计里已经明确：这里需要 **定时任务调度器**，使用 `@Scheduled(fixedDelay = 5000)` 是当前最简单可行的方案。

## 1. Controller

### `interfaces/controller/NodeController.java`

职责：管理员查看和管理节点

建议方法：

* `pageNodes(NodePageQueryRequest request)`
* `getNodeDetail(Long nodeId)`
* `updateNodeStatus(Long nodeId, UpdateNodeStatusRequest request)`
* `listNodeSolvers(Long nodeId)`

---

### `interfaces/controller/ScheduleController.java`

职责：调度记录查询

建议方法：

* `pageSchedules(SchedulePageQueryRequest request)`
* `listTaskSchedules(Long taskId)`

---

### `interfaces/internal/NodeRegisterController.java`

职责：node-agent 注册

建议方法：

* `register(NodeRegisterRequest request)`

---

### `interfaces/internal/NodeHeartbeatController.java`

职责：node-agent 心跳上报

建议方法：

* `heartbeat(NodeHeartbeatRequest request)`

---

## 2. Application

### `application/service/NodeAppService.java`

建议方法：

* `pageNodes`
* `getNodeDetail`
* `updateNodeStatus`
* `registerNode`
* `heartbeat`

---

### `application/service/ScheduleAppService.java`

建议方法：

* `pageSchedules`
* `listTaskSchedules`

---

### `application/manager/TaskScheduleManager.java`

职责：调度主流程

建议方法：

* `scheduleQueuedTasks()`
* `scheduleOneTask(TaskDTO task)`
* `chooseNode(TaskDTO task)`
* `dispatchToNode(TaskDTO task, NodeDTO node)`
* `recordSchedule(Long taskId, Long nodeId, String status, String message)`

---

### `application/manager/NodeManageManager.java`

职责：节点维护流程

建议方法：

* `register(NodeRegisterRequest request)`
* `heartbeat(NodeHeartbeatRequest request)`
* `markOfflineNodes()`

---

### `application/scheduler/TaskScheduleJob.java`

职责：定时调度入口

建议方法：

* `scheduleTasks()`

---

### `application/scheduler/NodeOfflineCheckJob.java`

职责：定时离线检查

建议方法：

* `checkOfflineNodes()`

---

## 3. Domain

### `domain/model/ComputeNode.java`

建议方法：

* `enable()`
* `disable()`
* `refreshHeartbeat(...)`
* `markOffline()`
* `canDispatch()`

---

### `domain/model/NodeSolverCapability.java`

建议方法：

* `supports(Long solverId)`

---

### `domain/model/ScheduleRecord.java`

---

### `domain/service/ScheduleDomainService.java`

职责：调度规则

建议方法：

* `filterAvailableNodes(List<ComputeNode> nodes, Long solverId)`
* `checkNodeCanDispatch(ComputeNode node)`

---

### `domain/strategy/ScheduleStrategy.java`

职责：调度策略接口

建议方法：

* `selectNode(TaskDTO task, List<NodeDTO> nodes)`

---

### `domain/strategy/FcfsLeastLoadStrategy.java`

职责：先到先服务 + 最小负载

建议方法：

* `selectNode(...)`

---

## 4. Infrastructure

### `infrastructure/client/TaskClient.java`

职责：调 task-service

建议方法：

* `listQueuedTasks()`
* `markScheduled(Long taskId, Long nodeId)`
* `markDispatched(Long taskId, Long nodeId)`

---

### `infrastructure/client/NodeAgentClient.java`

职责：调 node-agent

建议方法：

* `dispatchTask(String host, Integer port, TaskDTO task)`

---

### `infrastructure/support/NodeLoadCalculator.java`

建议方法：

* `calcLoad(NodeDTO node)`

---

### `infrastructure/support/NodeHeartbeatChecker.java`

建议方法：

* `isOffline(ComputeNode node, LocalDateTime now)`

---

### `infrastructure/support/AvailableNodeSelector.java`

建议方法：

* `filterBySolverCapability(List<NodeDTO> nodes, Long solverId)`

--------

这次我会重点把这几个东西设计清楚：

- 节点管理怎么分层
- 节点注册/心跳怎么落地
- 调度主流程怎么编排
- 调度策略怎么抽象
- 定时任务怎么组织
- 和 `task-service`、`node-agent` 怎么对接

------

# 一、scheduler-service 的定位

`scheduler-service` 负责四类核心能力：

1. 计算节点管理
2. 节点注册与心跳
3. 排队任务调度
4. 调度记录与监控

它的本质不是普通 CRUD 服务，而是一个**带调度逻辑的协调服务**。
所以这里不能只写：

- `NodeController`
- `ScheduleController`
- `ScheduleServiceImpl`

就结束了。

最合理的结构是：

- **Controller**：对外管理接口、对内注册心跳接口
- **Application/Manager**：调度流程编排
- **Domain/Strategy**：节点筛选、调度策略
- **Infrastructure/Client**：调 task-service、调 node-agent
- **Scheduler**：定时任务入口

------

# 二、scheduler-service 最终推荐包树

```text
scheduler-service/
└── src/main/java/com/example/cae/scheduler/
    ├── SchedulerApplication.java
    ├── interfaces/
    │   ├── controller/
    │   │   ├── NodeController.java
    │   │   └── ScheduleController.java
    │   ├── internal/
    │   │   ├── NodeRegisterController.java
    │   │   └── NodeHeartbeatController.java
    │   ├── request/
    │   └── response/
    ├── application/
    │   ├── service/
    │   ├── facade/
    │   ├── manager/
    │   ├── scheduler/
    │   └── assembler/
    ├── domain/
    │   ├── model/
    │   ├── repository/
    │   ├── service/
    │   ├── strategy/
    │   └── enums/
    ├── infrastructure/
    │   ├── persistence/
    │   │   ├── entity/
    │   │   ├── mapper/
    │   │   └── repository/
    │   ├── client/
    │   └── support/
    ├── config/
    └── support/
```

------

# 三、核心领域对象设计

------

## 1. ComputeNode.java

这是节点聚合根。

### 建议字段

```java
public class ComputeNode {
    private Long id;
    private String nodeCode;
    private String nodeName;
    private String host;
    private Integer port;
    private String status;
    private Integer enabled;
    private Integer maxConcurrency;
    private Integer runningCount;
    private BigDecimal cpuUsage;
    private BigDecimal memoryUsage;
    private LocalDateTime lastHeartbeatTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 建议方法

```java
public void enable()
public void disable()
public void markOnline()
public void markOffline()
public void refreshHeartbeat(BigDecimal cpuUsage, BigDecimal memoryUsage, Integer runningCount, LocalDateTime heartbeatTime)
public boolean isOnline()
public boolean canDispatch()
public boolean isOverloaded()
```

### 说明

这里的方法只处理领域行为，不操作数据库。

------

## 2. NodeSolverCapability.java

### 建议字段

```java
public class NodeSolverCapability {
    private Long id;
    private Long nodeId;
    private Long solverId;
    private Integer enabled;
    private LocalDateTime createdAt;
}
```

### 建议方法

```java
public boolean supports(Long solverId)
public boolean isEnabled()
```

------

## 3. ScheduleRecord.java

### 建议字段

```java
public class ScheduleRecord {
    private Long id;
    private Long taskId;
    private Long nodeId;
    private String strategyName;
    private String scheduleStatus;
    private String scheduleMessage;
    private LocalDateTime createdAt;
}
```

------

# 四、request / response 设计

------

## 1. interfaces/request

### NodeRegisterRequest.java

```java
public class NodeRegisterRequest {
    private String nodeCode;
    private String nodeName;
    private String host;
    private Integer port;
    private Integer maxConcurrency;
    private List<Long> solverIds;
}
```

------

### NodeHeartbeatRequest.java

```java
public class NodeHeartbeatRequest {
    private String nodeCode;
    private BigDecimal cpuUsage;
    private BigDecimal memoryUsage;
    private Integer runningCount;
}
```

------

### UpdateNodeStatusRequest.java

```java
public class UpdateNodeStatusRequest {
    private Integer enabled;
}
```

------

### NodePageQueryRequest.java

```java
public class NodePageQueryRequest {
    private Integer pageNum;
    private Integer pageSize;
    private String nodeCode;
    private String nodeName;
    private String status;
    private Integer enabled;
}
```

------

### SchedulePageQueryRequest.java

```java
public class SchedulePageQueryRequest {
    private Integer pageNum;
    private Integer pageSize;
    private Long taskId;
    private Long nodeId;
    private String scheduleStatus;
    private String strategyName;
}
```

------

## 2. interfaces/response

### NodeListItemResponse.java

```java
public class NodeListItemResponse {
    private Long nodeId;
    private String nodeCode;
    private String nodeName;
    private String host;
    private Integer port;
    private String status;
    private Integer enabled;
    private Integer maxConcurrency;
    private Integer runningCount;
    private BigDecimal cpuUsage;
    private BigDecimal memoryUsage;
    private LocalDateTime lastHeartbeatTime;
}
```

------

### NodeDetailResponse.java

```java
public class NodeDetailResponse {
    private Long nodeId;
    private String nodeCode;
    private String nodeName;
    private String host;
    private Integer port;
    private String status;
    private Integer enabled;
    private Integer maxConcurrency;
    private Integer runningCount;
    private BigDecimal cpuUsage;
    private BigDecimal memoryUsage;
    private LocalDateTime lastHeartbeatTime;
    private List<Long> solverIds;
}
```

------

### ScheduleRecordResponse.java

```java
public class ScheduleRecordResponse {
    private Long id;
    private Long taskId;
    private Long nodeId;
    private String strategyName;
    private String scheduleStatus;
    private String scheduleMessage;
    private LocalDateTime createdAt;
}
```

------

### AvailableNodeResponse.java

```java
public class AvailableNodeResponse {
    private Long nodeId;
    private String nodeCode;
    private String nodeName;
    private Integer runningCount;
    private Integer maxConcurrency;
    private BigDecimal cpuUsage;
    private BigDecimal memoryUsage;
}
```

------

# 五、Controller 层完整骨架

------

## 1. NodeController.java

职责：管理员节点管理接口。

```java
@RestController
@RequestMapping("/api/nodes")
@RequiredArgsConstructor
public class NodeController {

    private final NodeAppService nodeAppService;

    @GetMapping
    public Result<PageResult<NodeListItemResponse>> pageNodes(NodePageQueryRequest request) {
        return Result.success(nodeAppService.pageNodes(request));
    }

    @GetMapping("/{nodeId}")
    public Result<NodeDetailResponse> getNodeDetail(@PathVariable Long nodeId) {
        return Result.success(nodeAppService.getNodeDetail(nodeId));
    }

    @PutMapping("/{nodeId}/status")
    public Result<Void> updateNodeStatus(@PathVariable Long nodeId,
                                         @RequestBody @Valid UpdateNodeStatusRequest request) {
        nodeAppService.updateNodeStatus(nodeId, request);
        return Result.success();
    }

    @GetMapping("/{nodeId}/solvers")
    public Result<List<Long>> listNodeSolvers(@PathVariable Long nodeId) {
        return Result.success(nodeAppService.listNodeSolvers(nodeId));
    }
}
```

------

## 2. ScheduleController.java

职责：调度记录查询。

```java
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleAppService scheduleAppService;

    @GetMapping("/schedules")
    public Result<PageResult<ScheduleRecordResponse>> pageSchedules(SchedulePageQueryRequest request) {
        return Result.success(scheduleAppService.pageSchedules(request));
    }

    @GetMapping("/tasks/{taskId}/schedules")
    public Result<List<ScheduleRecordResponse>> listTaskSchedules(@PathVariable Long taskId) {
        return Result.success(scheduleAppService.listTaskSchedules(taskId));
    }
}
```

------

## 3. NodeRegisterController.java

职责：供 node-agent 注册使用。

```java
@RestController
@RequestMapping("/internal/nodes")
@RequiredArgsConstructor
public class NodeRegisterController {

    private final NodeManageManager nodeManageManager;

    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid NodeRegisterRequest request) {
        nodeManageManager.register(request);
        return Result.success();
    }
}
```

------

## 4. NodeHeartbeatController.java

职责：供 node-agent 心跳上报。

```java
@RestController
@RequestMapping("/internal/nodes")
@RequiredArgsConstructor
public class NodeHeartbeatController {

    private final NodeManageManager nodeManageManager;

    @PostMapping("/heartbeat")
    public Result<Void> heartbeat(@RequestBody @Valid NodeHeartbeatRequest request) {
        nodeManageManager.heartbeat(request);
        return Result.success();
    }
}
```

------

# 六、Application 层完整骨架

------

## 1. NodeAppService.java

职责：节点查询和管理应用层。

```java
@Service
@RequiredArgsConstructor
public class NodeAppService {

    private final ComputeNodeRepository computeNodeRepository;
    private final NodeSolverCapabilityRepository nodeSolverCapabilityRepository;
    private final NodeAssembler nodeAssembler;

    public PageResult<NodeListItemResponse> pageNodes(NodePageQueryRequest request) {
        PageResult<ComputeNode> page = computeNodeRepository.page(request);
        List<NodeListItemResponse> records = page.getRecords()
                .stream()
                .map(nodeAssembler::toListItemResponse)
                .toList();
        return PageResult.of(page.getTotal(), page.getPageNum(), page.getPageSize(), records);
    }

    public NodeDetailResponse getNodeDetail(Long nodeId) {
        ComputeNode node = computeNodeRepository.findById(nodeId);
        List<NodeSolverCapability> capabilities = nodeSolverCapabilityRepository.listByNodeId(nodeId);
        return nodeAssembler.toDetailResponse(node, capabilities);
    }

    public void updateNodeStatus(Long nodeId, UpdateNodeStatusRequest request) {
        ComputeNode node = computeNodeRepository.findById(nodeId);
        if (request.getEnabled() != null && request.getEnabled() == 1) {
            node.enable();
        } else {
            node.disable();
        }
        computeNodeRepository.update(node);
    }

    public List<Long> listNodeSolvers(Long nodeId) {
        return nodeSolverCapabilityRepository.listByNodeId(nodeId)
                .stream()
                .filter(NodeSolverCapability::isEnabled)
                .map(NodeSolverCapability::getSolverId)
                .toList();
    }
}
```

------

## 2. ScheduleAppService.java

职责：调度记录查询。

```java
@Service
@RequiredArgsConstructor
public class ScheduleAppService {

    private final ScheduleRecordRepository scheduleRecordRepository;
    private final ScheduleAssembler scheduleAssembler;

    public PageResult<ScheduleRecordResponse> pageSchedules(SchedulePageQueryRequest request) {
        PageResult<ScheduleRecord> page = scheduleRecordRepository.page(request);
        List<ScheduleRecordResponse> records = page.getRecords()
                .stream()
                .map(scheduleAssembler::toResponse)
                .toList();
        return PageResult.of(page.getTotal(), page.getPageNum(), page.getPageSize(), records);
    }

    public List<ScheduleRecordResponse> listTaskSchedules(Long taskId) {
        return scheduleRecordRepository.listByTaskId(taskId)
                .stream()
                .map(scheduleAssembler::toResponse)
                .toList();
    }
}
```

------

# 七、Manager 层完整骨架

这层最关键。

------

## 1. NodeManageManager.java

职责：节点注册、心跳、离线检查。

```java
@Service
@RequiredArgsConstructor
public class NodeManageManager {

    private final ComputeNodeRepository computeNodeRepository;
    private final NodeSolverCapabilityRepository nodeSolverCapabilityRepository;
    private final NodeDomainService nodeDomainService;

    @Transactional
    public void register(NodeRegisterRequest request) {
        ComputeNode node = computeNodeRepository.findByNodeCode(request.getNodeCode());

        if (node == null) {
            node = new ComputeNode();
            node.setNodeCode(request.getNodeCode());
            node.setNodeName(request.getNodeName());
            node.setHost(request.getHost());
            node.setPort(request.getPort());
            node.setMaxConcurrency(request.getMaxConcurrency());
            node.markOnline();
            computeNodeRepository.save(node);
        } else {
            node.setNodeName(request.getNodeName());
            node.setHost(request.getHost());
            node.setPort(request.getPort());
            node.setMaxConcurrency(request.getMaxConcurrency());
            node.markOnline();
            computeNodeRepository.update(node);
        }

        nodeSolverCapabilityRepository.replaceNodeSolvers(node.getId(), request.getSolverIds());
    }

    @Transactional
    public void heartbeat(NodeHeartbeatRequest request) {
        ComputeNode node = computeNodeRepository.findByNodeCode(request.getNodeCode());
        if (node == null) {
            throw new BizException("节点未注册");
        }

        node.refreshHeartbeat(
                request.getCpuUsage(),
                request.getMemoryUsage(),
                request.getRunningCount(),
                LocalDateTime.now()
        );
        node.markOnline();
        computeNodeRepository.update(node);
    }

    @Transactional
    public void markOfflineNodes() {
        List<ComputeNode> nodes = computeNodeRepository.listAllEnabled();
        for (ComputeNode node : nodes) {
            if (nodeDomainService.isOffline(node, LocalDateTime.now())) {
                node.markOffline();
                computeNodeRepository.update(node);
            }
        }
    }
}
```

------

## 2. TaskScheduleManager.java

职责：调度主流程。

```java
@Service
@RequiredArgsConstructor
public class TaskScheduleManager {

    private final TaskClient taskClient;
    private final NodeAgentClient nodeAgentClient;
    private final ComputeNodeRepository computeNodeRepository;
    private final NodeSolverCapabilityRepository nodeSolverCapabilityRepository;
    private final ScheduleRecordRepository scheduleRecordRepository;
    private final ScheduleDomainService scheduleDomainService;
    private final ScheduleStrategy scheduleStrategy;

    @Transactional
    public void scheduleQueuedTasks() {
        List<TaskDTO> queuedTasks = taskClient.listQueuedTasks();
        if (queuedTasks == null || queuedTasks.isEmpty()) {
            return;
        }

        for (TaskDTO task : queuedTasks) {
            scheduleOneTask(task);
        }
    }

    @Transactional
    public void scheduleOneTask(TaskDTO task) {
        List<ComputeNode> allNodes = computeNodeRepository.listAllEnabled();
        List<ComputeNode> availableNodes = scheduleDomainService.filterAvailableNodes(
                allNodes,
                task.getSolverId(),
                nodeSolverCapabilityRepository.listAll()
        );

        if (availableNodes.isEmpty()) {
            recordSchedule(task.getTaskId(), null, "FAILED", "无可用节点");
            return;
        }

        ComputeNode selectedNode = scheduleStrategy.selectNode(task, availableNodes);

        if (selectedNode == null) {
            recordSchedule(task.getTaskId(), null, "FAILED", "调度策略未选出节点");
            return;
        }

        taskClient.markScheduled(task.getTaskId(), selectedNode.getId());

        try {
            nodeAgentClient.dispatchTask(selectedNode.getHost(), selectedNode.getPort(), task);
            taskClient.markDispatched(task.getTaskId(), selectedNode.getId());
            recordSchedule(task.getTaskId(), selectedNode.getId(), "SUCCESS", "调度成功");
        } catch (Exception ex) {
            recordSchedule(task.getTaskId(), selectedNode.getId(), "FAILED", "任务下发失败: " + ex.getMessage());
        }
    }

    public void recordSchedule(Long taskId, Long nodeId, String status, String message) {
        ScheduleRecord record = new ScheduleRecord();
        record.setTaskId(taskId);
        record.setNodeId(nodeId);
        record.setStrategyName("FCFS_LEAST_LOAD");
        record.setScheduleStatus(status);
        record.setScheduleMessage(message);
        scheduleRecordRepository.save(record);
    }
}
```

------

# 八、Scheduler 定时任务骨架

------

## 1. TaskScheduleJob.java

```java
@Component
@RequiredArgsConstructor
public class TaskScheduleJob {

    private final TaskScheduleManager taskScheduleManager;

    @Scheduled(fixedDelay = 5000)
    public void scheduleTasks() {
        taskScheduleManager.scheduleQueuedTasks();
    }
}
```

------

## 2. NodeOfflineCheckJob.java

```java
@Component
@RequiredArgsConstructor
public class NodeOfflineCheckJob {

    private final NodeManageManager nodeManageManager;

    @Scheduled(fixedDelay = 10000)
    public void checkOfflineNodes() {
        nodeManageManager.markOfflineNodes();
    }
}
```

------

# 九、Domain Service / Strategy 层骨架

------

## 1. NodeDomainService.java

职责：节点领域规则。

```java
@Service
public class NodeDomainService {

    public boolean isOffline(ComputeNode node, LocalDateTime now) {
        if (node.getLastHeartbeatTime() == null) {
            return true;
        }
        return Duration.between(node.getLastHeartbeatTime(), now).getSeconds() > 30;
    }

    public boolean canDispatch(ComputeNode node) {
        return node != null
                && node.isOnline()
                && node.getEnabled() != null
                && node.getEnabled() == 1
                && node.getRunningCount() != null
                && node.getMaxConcurrency() != null
                && node.getRunningCount() < node.getMaxConcurrency();
    }
}
```

------

## 2. ScheduleDomainService.java

职责：节点筛选逻辑。

```java
@Service
@RequiredArgsConstructor
public class ScheduleDomainService {

    private final NodeDomainService nodeDomainService;

    public List<ComputeNode> filterAvailableNodes(List<ComputeNode> nodes,
                                                  Long solverId,
                                                  List<NodeSolverCapability> capabilities) {

        Set<Long> supportedNodeIds = capabilities.stream()
                .filter(item -> item.getEnabled() != null && item.getEnabled() == 1)
                .filter(item -> Objects.equals(item.getSolverId(), solverId))
                .map(NodeSolverCapability::getNodeId)
                .collect(Collectors.toSet());

        return nodes.stream()
                .filter(nodeDomainService::canDispatch)
                .filter(node -> supportedNodeIds.contains(node.getId()))
                .toList();
    }
}
```

------

## 3. ScheduleStrategy.java

```java
public interface ScheduleStrategy {
    ComputeNode selectNode(TaskDTO task, List<ComputeNode> nodes);
}
```

------

## 4. FcfsLeastLoadStrategy.java

职责：先到先服务 + 最小负载。

```java
@Component
public class FcfsLeastLoadStrategy implements ScheduleStrategy {

    @Override
    public ComputeNode selectNode(TaskDTO task, List<ComputeNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }

        return nodes.stream()
                .min(Comparator
                        .comparing(ComputeNode::getRunningCount, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ComputeNode::getCpuUsage, Comparator.nullsLast(BigDecimal::compareTo))
                        .thenComparing(ComputeNode::getMemoryUsage, Comparator.nullsLast(BigDecimal::compareTo)))
                .orElse(null);
    }
}
```

------

# 十、Repository 接口骨架

------

## 1. ComputeNodeRepository.java

```java
public interface ComputeNodeRepository {
    ComputeNode findById(Long nodeId);
    ComputeNode findByNodeCode(String nodeCode);
    void save(ComputeNode node);
    void update(ComputeNode node);
    PageResult<ComputeNode> page(NodePageQueryRequest request);
    List<ComputeNode> listAllEnabled();
}
```

------

## 2. NodeSolverCapabilityRepository.java

```java
public interface NodeSolverCapabilityRepository {
    List<NodeSolverCapability> listByNodeId(Long nodeId);
    List<NodeSolverCapability> listAll();
    void replaceNodeSolvers(Long nodeId, List<Long> solverIds);
}
```

------

## 3. ScheduleRecordRepository.java

```java
public interface ScheduleRecordRepository {
    void save(ScheduleRecord record);
    PageResult<ScheduleRecord> page(SchedulePageQueryRequest request);
    List<ScheduleRecord> listByTaskId(Long taskId);
}
```

------

# 十一、Persistence 层骨架

------

## 1. entity

建议类：

- `ComputeNodePO`
- `NodeSolverCapabilityPO`
- `ScheduleRecordPO`

字段与表一一对应，不写业务方法。

------

## 2. mapper

### ComputeNodeMapper.java

```java
@Mapper
public interface ComputeNodeMapper extends BaseMapper<ComputeNodePO> {
}
```

### NodeSolverCapabilityMapper.java

### ScheduleRecordMapper.java

同理。

------

## 3. repository impl

### ComputeNodeRepositoryImpl.java

```java
@Repository
@RequiredArgsConstructor
public class ComputeNodeRepositoryImpl implements ComputeNodeRepository {

    private final ComputeNodeMapper computeNodeMapper;
    private final NodeAssembler nodeAssembler;

    @Override
    public ComputeNode findById(Long nodeId) { ... }

    @Override
    public ComputeNode findByNodeCode(String nodeCode) { ... }

    @Override
    public void save(ComputeNode node) { ... }

    @Override
    public void update(ComputeNode node) { ... }

    @Override
    public PageResult<ComputeNode> page(NodePageQueryRequest request) { ... }

    @Override
    public List<ComputeNode> listAllEnabled() { ... }
}
```

------

### NodeSolverCapabilityRepositoryImpl.java

```java
@Repository
@RequiredArgsConstructor
public class NodeSolverCapabilityRepositoryImpl implements NodeSolverCapabilityRepository {

    private final NodeSolverCapabilityMapper mapper;
    private final NodeAssembler nodeAssembler;

    @Override
    public List<NodeSolverCapability> listByNodeId(Long nodeId) { ... }

    @Override
    public List<NodeSolverCapability> listAll() { ... }

    @Override
    @Transactional
    public void replaceNodeSolvers(Long nodeId, List<Long> solverIds) { ... }
}
```

------

### ScheduleRecordRepositoryImpl.java

```java
@Repository
@RequiredArgsConstructor
public class ScheduleRecordRepositoryImpl implements ScheduleRecordRepository {

    private final ScheduleRecordMapper scheduleRecordMapper;
    private final ScheduleAssembler scheduleAssembler;

    @Override
    public void save(ScheduleRecord record) { ... }

    @Override
    public PageResult<ScheduleRecord> page(SchedulePageQueryRequest request) { ... }

    @Override
    public List<ScheduleRecord> listByTaskId(Long taskId) { ... }
}
```

------

# 十二、Client 层骨架

------

## 1. TaskClient.java

职责：调 task-service。

```java
@FeignClient(name = "task-service")
public interface TaskClient {

    @GetMapping("/internal/tasks/queued")
    Result<List<TaskDTO>> listQueuedTasks();

    @PostMapping("/internal/tasks/{taskId}/mark-scheduled")
    Result<Void> markScheduled(@PathVariable("taskId") Long taskId,
                               @RequestParam("nodeId") Long nodeId);

    @PostMapping("/internal/tasks/{taskId}/mark-dispatched")
    Result<Void> markDispatched(@PathVariable("taskId") Long taskId,
                                @RequestParam("nodeId") Long nodeId);
}
```

------

## 2. NodeAgentClient.java

职责：调 node-agent。

```java
@Component
public class NodeAgentClient {

    private final RestTemplate restTemplate;

    public NodeAgentClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void dispatchTask(String host, Integer port, TaskDTO task) {
        String url = "http://" + host + ":" + port + "/internal/dispatch-task";
        restTemplate.postForObject(url, task, Void.class);
    }
}
```

后面也可以换成 OpenFeign 或 WebClient。

------

# 十三、Assembler 层骨架

------

## 1. NodeAssembler.java

```java
@Component
public class NodeAssembler {

    public NodeListItemResponse toListItemResponse(ComputeNode node) { ... }

    public NodeDetailResponse toDetailResponse(ComputeNode node, List<NodeSolverCapability> capabilities) { ... }

    public ComputeNode fromPO(ComputeNodePO po) { ... }

    public ComputeNodePO toPO(ComputeNode node) { ... }

    public NodeSolverCapability fromPO(NodeSolverCapabilityPO po) { ... }

    public NodeSolverCapabilityPO toPO(NodeSolverCapability capability) { ... }
}
```

------

## 2. ScheduleAssembler.java

```java
@Component
public class ScheduleAssembler {

    public ScheduleRecordResponse toResponse(ScheduleRecord record) { ... }

    public ScheduleRecord fromPO(ScheduleRecordPO po) { ... }

    public ScheduleRecordPO toPO(ScheduleRecord record) { ... }
}
```

------

# 十四、Config 层骨架

建议先建这几个：

### SchedulerApplication.java

```java
@SpringBootApplication
@EnableScheduling
@EnableFeignClients
public class SchedulerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }
}
```

------

### FeignClientConfig.java

可先空壳，后续加统一 header、超时配置。

------

### SchedulingConfig.java

如果你后期要统一线程池，可放这里。

------

# 十五、最小可运行实现顺序

你不要一次写全，按这个顺序最稳。

## 第一批

先建：

- `ComputeNode`
- `NodeController`
- `NodeAppService`
- `ComputeNodeRepository`
- `ComputeNodeRepositoryImpl`
- `ComputeNodeMapper`
- `NodeAssembler`

先跑通：

- 节点列表
- 节点详情
- 启停节点

------

## 第二批

再建：

- `NodeRegisterController`
- `NodeHeartbeatController`
- `NodeManageManager`
- `NodeDomainService`
- `NodeSolverCapabilityRepository`

跑通：

- 节点注册
- 节点心跳
- 节点在线/离线更新

------

## 第三批

再建：

- `TaskClient`
- `TaskScheduleManager`
- `TaskScheduleJob`
- `ScheduleStrategy`
- `FcfsLeastLoadStrategy`

跑通：

- 拉取 queued 任务
- 节点筛选
- 标记 scheduled

------

## 第四批

再建：

- `NodeAgentClient`
- `ScheduleRecordRepository`
- `ScheduleController`
- `ScheduleAppService`

跑通：

- 下发任务
- 标记 dispatched
- 写调度记录
- 查看调度记录

------

# 十六、你现在最该先建的 12 个类

如果你只想先把主骨架搭出来，建议先建这 12 个：

1. `ComputeNode`
2. `NodeController`
3. `NodeRegisterController`
4. `NodeHeartbeatController`
5. `NodeAppService`
6. `NodeManageManager`
7. `TaskScheduleManager`
8. `TaskScheduleJob`
9. `NodeDomainService`
10. `ScheduleStrategy`
11. `FcfsLeastLoadStrategy`
12. `TaskClient`

这 12 个一出来，`scheduler-service` 的主干就有了。

------

# 十七、最终建议

`scheduler-service` 最怕的两个坑是：

- 把调度逻辑写进定时任务类
- 把节点逻辑和调度逻辑混成一个 Service

你现在这版骨架已经把它拆开了：

- `NodeManageManager` 专管节点
- `TaskScheduleManager` 专管调度
- `TaskScheduleJob` 只是入口
- `ScheduleStrategy` 专门抽调度算法
- `TaskClient / NodeAgentClient` 专门做外部调用

这就是比较规范、后面也不容易乱的结构。

--------

