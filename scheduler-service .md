# scheduler-service 模块分析文档

## 1. 模块定位

`scheduler-service` 是平台中的任务调度与计算节点管理服务，负责把 `task-service` 中已经进入队列的任务分配到合适的计算节点，并持续维护节点在线状态、负载信息与求解器能力信息。

如果说 `task-service` 解决“任务生命周期如何流转”，那么 `scheduler-service` 解决的就是：

- 当前有哪些节点可用
- 哪些节点支持某个求解器
- 某个排队任务应派发到哪个节点
- 节点掉线后如何补偿任务状态
- 如何向平台提供节点与调度记录查询能力

因此，`scheduler-service` 在整个系统中扮演的是“资源感知型调度中心”和“节点状态中心”的角色。

## 2. 模块在系统中的作用

从系统协作关系看，`scheduler-service` 主要解决以下问题：

1. 接收节点代理注册信息，建立节点主数据与求解器能力映射。
2. 接收节点心跳，动态刷新节点状态、负载、运行并发数。
3. 周期性扫描排队任务，执行节点筛选与调度分配。
4. 调用 `node-agent` 下发任务，同时回写 `task-service` 的调度状态。
5. 周期性检查长时间未心跳节点，并触发任务失败补偿。
6. 对外提供节点查询、可用节点查询和调度记录查询能力。
7. 对内提供节点 Token 校验、运行数调整和节点侧取消转发能力。

它连接了 `task-service` 和 `node-agent`，是任务真正进入执行阶段前的关键枢纽。

## 3. 当前实现架构

### 3.1 技术栈

- Spring Boot
- 分层架构：`interfaces / application / domain / infrastructure`
- MyBatis Repository 持久化
- `RestTemplate` 调用 `task-service` 和 `node-agent`
- Spring 定时任务驱动调度轮询与离线检测
- 策略模式封装调度算法

### 3.2 当前目录结构

当前代码结构如下：

```text
scheduler-service/
├─ src/main/java/com/example/cae/scheduler/
│  ├─ SchedulerApplication.java
│  ├─ config/
│  │  ├─ SchedulerServiceConfig.java
│  │  ├─ SchedulerRemoteServiceProperties.java
│  │  ├─ SchedulingConfig.java
│  │  └─ FeignClientConfig.java
│  ├─ interfaces/
│  │  ├─ controller/
│  │  │  ├─ NodeController.java
│  │  │  ├─ NodeAgentController.java
│  │  │  └─ ScheduleController.java
│  │  ├─ internal/
│  │  │  ├─ NodeRegisterController.java
│  │  │  ├─ NodeHeartbeatController.java
│  │  │  └─ InternalSchedulerController.java
│  │  ├─ request/
│  │  └─ response/
│  ├─ application/
│  │  ├─ assembler/
│  │  ├─ facade/
│  │  ├─ manager/
│  │  ├─ scheduler/
│  │  └─ service/
│  ├─ domain/
│  │  ├─ enums/
│  │  ├─ model/
│  │  ├─ repository/
│  │  ├─ service/
│  │  └─ strategy/
│  ├─ infrastructure/
│  │  ├─ client/
│  │  ├─ persistence/
│  │  └─ support/
│  └─ support/
└─ src/main/resources/application.yml
```

整体结构很清晰，围绕“节点管理”和“调度执行”两条主线展开。

## 4. 各部分功能与职责

### 4.1 接口层

#### `NodeController`

负责平台侧节点管理接口，包括：

- 节点分页查询
- 节点详情查询
- 启停节点
- 启停节点上的某个求解器能力
- 查询节点支持的求解器列表

这部分主要面向前端管理端使用。

#### `NodeAgentController`

负责面向节点代理的公开接口，包括：

- 节点注册 `/api/node-agent/register`
- 节点心跳 `/api/node-agent/heartbeat`

这是“节点动态状态”的起点。当前节点并不是靠人工静态维护，而是由节点代理主动注册并持续发送心跳来驱动状态刷新。

#### `ScheduleController`

负责调度记录查询，包括：

- 调度记录分页
- 按任务查询调度记录

这让调度过程具备可追踪性，而不是仅在日志中体现。

#### `InternalSchedulerController`

负责提供给其他服务的内部接口，包括：

- 查询某求解器的可用节点
- 记录调度记录
- 调整节点运行数
- 转发取消任务到节点
- 校验节点 Token

其中 `/internal/nodes/available` 还被 `task-service` 用于生成 `queueReason`，说明它不仅参与调度，也参与“排队原因解释”。

#### `NodeRegisterController` 与 `NodeHeartbeatController`

这两个控制器提供了更早期或更内部化的节点注册、心跳接口，体现出当前系统同时兼顾“节点代理直接接入”和“内部接口方式接入”的兼容思路。

### 4.2 应用层

#### `NodeAppService`

这是节点管理主服务，负责：

- 注册新节点
- 处理节点代理注册
- 处理心跳
- 更新节点启停状态
- 更新节点求解器能力启停状态
- 查询节点详情和能力列表
- 标记节点离线
- 查询在线节点和可用节点
- 维护节点 `runningCount`
- 生成和校验 `nodeToken`

它是“节点状态中心”的核心实现。

#### `ScheduleAppService`

这是调度主服务，负责：

- 执行调度算法选节点
- 预占节点运行数
- 记录调度成功与失败
- 调用 `node-agent` 下发任务
- 转发取消请求
- 查询调度记录

它是“调度中心”的核心实现。

### 4.3 Manager 与 Facade 层

#### `NodeManageManager` 与 `NodeFacade`

负责对节点相关应用流程做进一步封装，让控制器保持更薄。这一层虽然不复杂，但使结构更规整，方便论文和答辩时表达分层设计。

#### `TaskScheduleManager` 与 `ScheduleFacade`

负责调度流程和调度记录查询的统一入口，屏蔽控制器对 `ScheduleAppService` 的直接依赖。

### 4.4 定时任务层

#### `TaskScheduleJob`

这是整个模块最关键的执行入口之一。它按固定周期执行：

1. 调用 `task-service` 拉取排队任务
2. 根据调度策略选择节点
3. 先在本地预占节点 `runningCount`
4. 回写任务状态为 `SCHEDULED`
5. 调用 `node-agent` 下发任务
6. 成功后回写为 `DISPATCHED`
7. 记录调度成功记录
8. 如失败则释放节点预占并记录失败

这说明当前项目的调度实现是“调度器主动轮询 + 同步派发”的模式。

#### `NodeOfflineCheckJob`

负责按固定周期执行节点离线检查。它会调用 `NodeHeartbeatChecker` 判断在线节点是否长时间没有心跳，若超时则：

- 通知 `task-service` 将该节点影响的任务批量标记失败
- 再将节点标记为离线

这条链路是节点动态状态真正闭环的关键。

### 4.5 领域层

#### `ComputeNode`

`ComputeNode` 是节点聚合根，承载了节点核心运行状态，包括：

- `status`
- `enabled`
- `maxConcurrency`
- `runningCount`
- `cpuUsage`
- `memoryUsage`
- `lastHeartbeatTime`
- `nodeToken`

并提供关键领域行为：

- `markOnline()`
- `markOffline()`
- `refreshHeartbeat()`
- `enable()`
- `disable()`
- `canDispatch()`

其中 `canDispatch()` 非常关键，它要求节点同时满足：

- 在线
- 启用
- 当前运行数小于最大并发数

这说明当前系统中的节点状态确实是动态调度状态，而不是数据库中的静态标签。

#### `NodeSolverCapability`

负责描述节点支持哪些求解器以及对应版本、是否启用。  
这是调度时“节点能不能跑这个任务”的核心依据。

#### `ScheduleRecord`

负责记录每次调度行为，包括：

- 任务 ID
- 节点 ID
- 调度策略名
- 调度状态
- 调度消息

它构成了调度过程的审计轨迹。

#### `NodeDomainService`

负责节点领域规则，包括：

- 注册请求合法性校验
- 心跳请求合法性校验
- 节点是否可派发判断

#### `ScheduleDomainService`

负责可调度节点过滤逻辑，即：

- 先看节点是否在线、启用且未满载
- 再看节点是否支持该求解器且能力启用

它把“节点资源状态”和“节点能力状态”合并为统一调度前置判断。

#### `ScheduleStrategy` 与 `FcfsLeastLoadStrategy`

当前调度策略采用策略模式封装，实际实现为 `FcfsLeastLoadStrategy`。  
当前策略核心特征是：

- 以可派发节点集合为输入
- 优先选择 `runningCount` 更小的节点
- 若并列，再比较 CPU 使用率
- 再比较内存使用率

虽然类名叫 FCFS + Least Load，但从当前代码看，真正落实的是“在当前可调度任务列表顺序下，节点侧采用最小负载优先”。

### 4.6 基础设施层

#### `TaskClient` / `TaskClientStub`

负责和 `task-service` 交互，当前已支持：

- 拉取排队任务
- 标记任务已调度
- 标记任务已下发
- 标记任务调度失败
- 节点离线时批量标记任务失败

这是调度服务和任务服务的核心协作接口。

#### `NodeAgentClient` / `NodeAgentClientStub`

负责和节点代理通信，当前已支持：

- 下发任务
- 转发取消任务

它会根据节点 `host` 拼装 node-agent 地址，并将调度所需元数据一并发送给节点侧。

#### 仓储层

当前主要包括：

- `ComputeNodeRepository`
- `NodeSolverCapabilityRepository`
- `ScheduleRecordRepository`

这些仓储完成节点主数据、节点能力和调度记录的持久化。

其中 `NodeSolverCapabilityRepositoryImpl` 的一个重要特点是：

- 节点重新注册时会整体替换能力集合

这意味着平台始终以节点最新上报的能力信息为准。

#### 支撑组件

当前还有一些支撑类：

- `NodeHeartbeatChecker`：离线检测与补偿触发
- `AvailableNodeSelector`：可派发节点过滤辅助
- `NodeLoadCalculator`：负载评分预留能力
- `ScheduleLogWriter`：调度日志输出辅助

其中 `NodeLoadCalculator` 目前更偏预留扩展点，当前实际调度主要还是由 `FcfsLeastLoadStrategy` 完成。

## 5. 核心业务流程

### 5.1 节点注册与发现流程

```text
node-agent 启动
  -> 调用 /api/node-agent/register
  -> NodeAgentController
  -> NodeAppService.registerNodeFromAgent
  -> NodeAppService.registerNode
  -> 写入/更新 compute_node
  -> 写入/替换 node_solver_capability
  -> 生成或复用 nodeToken
  -> 返回 nodeId + nodeToken
```

这个流程说明：

- 节点发现不是写死在数据库里
- 节点可以重复注册并刷新自身元数据
- 平台会根据 `nodeCode` 识别已有节点并更新其主数据

### 5.2 节点心跳与动态状态刷新流程

```text
node-agent 定期发送心跳
  -> /api/node-agent/heartbeat
  -> 携带 X-Node-Token
  -> NodeAppService.heartbeat
  -> 校验 nodeToken
  -> 更新 cpuUsage / memoryUsage / runningCount / lastHeartbeatTime
  -> 节点标记为 ONLINE
```

这条流程非常关键，因为它说明前端看到的节点状态、负载和运行数并不是纯静态数据库演示数据，而是会被节点持续刷新。

### 5.3 节点离线检测与补偿流程

```text
NodeOfflineCheckJob 定时运行
  -> NodeHeartbeatChecker.markOfflineNodes
  -> 查找 ONLINE 节点
  -> 超过阈值未心跳则判定离线
  -> 调用 task-service 批量标记该节点影响任务失败
  -> 节点标记 OFFLINE
  -> 清零 runningCount / cpuUsage / memoryUsage
```

这说明“离线”不是管理员手工改状态，而是由调度器根据心跳超时自动判定。

### 5.4 调度与派发流程

```text
TaskScheduleJob 定时运行
  -> TaskClient.listPendingTasks(20)
  -> ScheduleAppService.scheduleTask
  -> ScheduleDomainService 过滤可用节点
  -> FcfsLeastLoadStrategy 选节点
  -> 预占节点 runningCount +1
  -> task-service markScheduled
  -> node-agent dispatch-task
  -> task-service markDispatched
  -> 保存 SUCCESS 调度记录
```

若中途失败，则会：

- 释放节点预占
- 在必要时回写任务失败
- 保存 FAILED 调度记录

### 5.5 任务取消流程

```text
task-service 请求取消运行中任务
  -> scheduler-service /internal/nodes/{nodeId}/cancel-task
  -> ScheduleAppService.cancelTaskOnNode
  -> NodeAgentClient.cancelTask
  -> node-agent 执行取消
```

因此，运行中取消并不是直接改数据库，而是通过调度器向执行侧转发。

## 6. 核心设计

### 6.1 设计一：节点状态由“注册 + 心跳 + 离线检查”动态驱动

这是 `scheduler-service` 最重要的设计点之一。  
当前节点状态不是静态维护，而是靠三步构成闭环：

1. 节点启动时注册
2. 节点运行中持续心跳
3. 调度器定时离线检查

这正是计算节点动态管理的核心机制，也使节点状态真正能够反映实际调度环境。

### 6.2 设计二：将节点“主状态”和“求解器能力”分离

系统没有把“节点是否支持某求解器”写成节点表中的单个字段，而是单独维护 `node_solver_capability`。  
这样做的好处是：

- 一个节点可以支持多个求解器
- 每个求解器能力可以单独启停
- 可以记录版本信息
- 未来更容易扩展多求解器场景

### 6.3 设计三：调度链路分成“选节点”和“下发任务”两个阶段

当前调度过程不是一步到位，而是分成：

- 先筛选并选择节点
- 再通知任务服务更新调度状态
- 再向节点代理发送下发请求

这种分段式设计使系统更容易定位问题，例如是“没有可用节点”“调度成功但下发失败”还是“节点执行阶段失败”。

### 6.4 设计四：用调度记录沉淀调度轨迹

当前系统不仅做调度动作，还会记录：

- 任务调度到哪个节点
- 使用什么策略
- 成功还是失败
- 失败原因是什么

这使调度过程具备了可查询、可解释和可答辩的依据。

### 6.5 设计五：通过节点 Token 保证节点侧接口可信

节点注册后会获得 `nodeToken`，后续心跳和任务回传都围绕该令牌展开。  
当前令牌虽然是轻量级 Base64 方案，但已经体现出：

- 节点身份需要显式认证
- 并非所有请求都可伪造为某个节点
- `task-service` 与 `scheduler-service` 共享节点身份校验职责

## 7. 架构难点与解决方案

### 7.1 难点一：如何让节点状态反映真实运行状态

问题：

- 如果节点状态只靠数据库初始数据，前端展示就会是静态假象
- 调度器也无法判断节点是否真的还活着

当前解决方案：

- 节点代理启动时主动注册
- 运行中持续心跳更新负载与并发
- 调度器定时检查超时并自动标记离线

这正是当前实现中“节点状态动态化”的核心。

### 7.2 难点二：如何在调度前同时考虑节点资源与节点能力

问题：

- 节点在线并不意味着它能运行当前任务
- 节点支持求解器也不意味着它当前有空闲并发

当前解决方案：

- `ScheduleDomainService` 同时检查节点能力和节点可派发状态
- 调度策略只在通过前置过滤后的节点集合中进行选择

这样避免了把能力判断和资源判断混在调度算法内部。

### 7.3 难点三：节点离线后如何补偿任务状态

问题：

- 如果节点离线但任务仍停留在 `RUNNING` 或 `DISPATCHED`，系统状态会失真

当前解决方案：

- 离线检测后，调度器调用 `task-service` 批量标记相关任务失败
- 失败原因中写明是节点心跳超时导致

这让系统能够从真实执行故障中恢复到一致状态。

### 7.4 难点四：任务派发失败如何避免节点资源“假占用”

问题：

- 调度器在选择节点后已经预占了 `runningCount`
- 如果后续通知节点失败，就必须回收预占

当前解决方案：

- `scheduleTask()` 先预占并发
- 若派发失败则调用 `releaseNodeReservation()`

这说明当前实现已经考虑了派发失败时的资源回滚。

### 7.5 难点五：如何在原型复杂度和调度真实性之间平衡

问题：

- 真正的分布式调度器通常包含锁、队列、中间件、抢占、优先级、多实例一致性等复杂能力
- 本科原型项目不能直接做成生产级调度中台

当前解决方案：

- 采用定时轮询 + 单策略 + 同步下发的轻量方案
- 保留节点能力、动态心跳、离线补偿和调度记录这些最关键能力

这使系统既有真实调度闭环，又保持了实现可控。

## 8. 关键技术手段

### 8.1 Spring 定时任务

当前依赖两个定时任务驱动系统核心行为：

- `TaskScheduleJob`：周期性调度
- `NodeOfflineCheckJob`：周期性离线检测

这是当前调度系统的“时钟”。

### 8.2 策略模式

通过 `ScheduleStrategy` 抽象调度算法，并以 `FcfsLeastLoadStrategy` 作为当前实现。  
这样未来可以扩展更多策略，而不需要重写整体调度流程。

### 8.3 微服务内部 HTTP 协作

当前 `scheduler-service` 通过内部 HTTP 与：

- `task-service`
- `node-agent`

进行协作，形成完整调度闭环，体现了微服务边界下的职责分离。

### 8.4 节点能力映射模型

通过 `ComputeNode + NodeSolverCapability` 的双层模型表达：

- 节点基础状态
- 节点支持的求解器能力

这是实现多求解器多节点调度的关键数据结构。

### 8.5 运行数预占与心跳反馈结合

当前节点负载管理并不是单纯依赖心跳，也不是单纯依赖数据库统计，而是采用：

- 调度瞬间先预占 `runningCount`
- 节点后续再通过心跳持续刷新实际 `runningCount`

这是一种适合原型平台的简化运行数管理方案。

## 9. 当前实现的优点

- 已实现节点注册、心跳、离线检测的动态节点状态闭环。
- 已建立节点与求解器能力映射，能够支持多求解器场景下的节点筛选。
- 已实现调度器轮询任务、选择节点、下发任务、记录结果的完整调度主链。
- 已实现节点离线后的任务失败补偿，避免任务长期卡死在运行态。
- 已实现节点 Token 生成与校验，内部安全性优于纯白名单放行。
- 已提供节点查询、调度记录查询和可用节点查询能力，便于前端展示和问题定位。

## 10. 当前实现的局限与边界

### 10.1 当前更适合单实例调度器原型

当前调度链路中没有看到分布式锁、任务抢占保护或多实例幂等协调机制。  
这意味着如果未来扩展成多实例调度器，可能出现重复轮询和重复调度风险。

### 10.2 调度触发仍为轮询模式

当前调度器依赖 `TaskScheduleJob` 周期性轮询 `task-service`。  
因此任务提交后不会立即被推送调度，而是等待下一轮扫描。

### 10.3 调度策略较基础

当前仅实现了 `FcfsLeastLoadStrategy`，主要考虑：

- 当前并发数
- CPU 使用率
- 内存使用率

尚未进一步纳入：

- 严格任务优先级排序控制
- 任务预估资源需求
- 节点亲和性
- 公平调度
- 抢占式调度

### 10.4 节点 Token 仍是轻量原型方案

当前 `nodeToken` 是基于 `nodeCode + UUID` 的 Base64 编码生成结果。  
它能满足原型系统的节点身份识别，但还不是生产级的节点证书或强签名方案。

### 10.5 缺少更强的调度治理能力

当前还没有实现：

- 调度任务队列中间件
- 调度重试策略
- 分布式锁
- 多副本高可用协调
- 调度事件流审计
- 节点黑名单和故障熔断

这些都更适合作为扩展点，而不是当前本科毕设原型的首版目标。

## 11. 对本科毕设的价值

从本科毕业设计角度看，`scheduler-service` 的价值非常高，主要体现在：

1. 它体现了平台不是静态任务管理系统，而是真正具备“资源调度”能力的任务平台。
2. 它能够说明系统中的节点状态是动态变化的，而不是展示层假数据。
3. 它将微服务拆分、节点管理、任务调度、故障补偿这些关键技术点连接起来。
4. 它足以支撑“分布式计算节点调度原型”这一核心技术指标的展示。

因此，`scheduler-service` 是答辩时非常值得重点讲解的模块。

## 12. 答辩时可采用的表述

可以将该模块概括为：

> `scheduler-service` 是平台的调度与节点管理中心，负责计算节点的注册、能力维护、心跳接收、离线检测以及排队任务的节点分配。系统通过节点代理注册和周期性心跳实现节点状态动态刷新，通过节点能力映射和负载筛选实现任务与节点的匹配，通过定时调度作业完成任务轮询、节点选择和任务下发，并在节点离线时触发任务失败补偿，从而形成面向 CAE 仿真任务的轻量级调度闭环。

## 13. 后续可扩展方向

在不改变当前原型定位的前提下，后续可以继续扩展：

- 引入提交后主动触发调度，而不只依赖轮询
- 增加更丰富的调度策略，如优先级调度、资源感知调度
- 引入分布式锁或一致性机制，支持多实例调度器
- 增加节点故障熔断、黑名单和重试机制
- 将节点 Token 升级为更强的安全认证方案
- 增加更精细的资源模型，如 GPU、磁盘、许可证等资源维度
- 完善调度日志、审计和运行监控能力

这些内容更适合作为扩展能力，而不是当前首版必须全部完成的内容。

## 14. 当前结论

`scheduler-service` 当前已经完成了本项目原型平台中与“调度”和“动态节点管理”相关的核心能力：

- 可实现节点注册、心跳上报与离线检测
- 可动态维护节点在线状态、负载和并发数
- 可维护节点支持的求解器能力集合
- 可轮询排队任务并执行节点选择与任务下发
- 可记录调度成功与失败轨迹
- 可在节点离线时触发任务失败补偿

从实现深度看，它已经超出了“数据库里写几个节点状态供前端展示”的静态演示层面，而是真正实现了一个具备动态节点状态管理、基础调度策略和故障补偿能力的轻量级调度原型。  
从本科毕设要求看，这个模块已经能够很好支撑“微服务 + 分布式节点 + 任务调度”这几个核心技术指标的展示。
