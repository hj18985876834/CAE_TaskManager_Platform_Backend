# task-service 模块分析文档

## 1. 模块定位

`task-service` 是整个平台中最核心的业务服务，负责承接 CAE 仿真任务从创建到结束的完整生命周期管理。  
如果说 `user-service` 解决“谁可以使用系统”，`solver-service` 解决“系统支持什么求解器与任务模板”，`scheduler-service` 解决“任务应分配到哪个节点”，那么 `task-service` 解决的就是“一个具体任务如何被创建、校验、提交、调度、执行、回传结果并沉淀全过程记录”。

在当前项目中，它承担的职责明显比其他服务更重，主要包括：

- 任务创建、修改、归档上传、校验、提交
- 未提交任务清除、排队任务取消、失败任务重试、管理员调优优先级
- 任务状态流转与状态历史记录
- 任务文件、日志、结果摘要、结果文件管理
- 面向调度器的内部派发接口
- 面向节点代理的内部状态与结果回传接口
- 任务列表、详情、历史、文件、日志、结果与看板汇总查询

因此，`task-service` 不是简单的 CRUD 服务，而是平台的“任务生命周期中枢”。

## 2. 模块在系统中的作用

从整个系统协作关系看，`task-service` 主要解决以下问题：

1. 把前端提交的任务意图转化为系统内部可调度、可执行、可追踪的任务对象。
2. 将 `solver-service` 提供的模板元数据真正落到任务校验和执行上下文中。
3. 将任务从“用户视角的业务单据”转化为“调度器与节点可消费的执行对象”。
4. 将节点执行过程中产生的状态、日志、结果重新汇聚为用户可查询的任务记录。
5. 在任务全生命周期中持续保留可解释信息，例如状态历史、失败原因、排队原因、是否可重试等。

所以，`task-service` 在架构上处于前端任务操作、模板规则中心、调度服务和节点代理之间的中枢位置。

## 3. 当前实现架构

### 3.1 技术栈

- Spring Boot
- MVC 风格 REST 接口
- 分层架构：`interfaces / application / domain / infrastructure`
- MyBatis Mapper + Repository 实现
- `RestTemplate` 方式调用 `solver-service` 与 `scheduler-service`
- 本地文件系统存储任务输入与结果文件
- `common-lib` 中的通用枚举、错误码、DTO、返回体与异常体系

### 3.2 当前目录结构

当前代码结构如下：

```text
task-service/
├─ src/main/java/com/example/cae/task/
│  ├─ TaskApplication.java
│  ├─ config/
│  │  ├─ TaskServiceConfig.java
│  │  ├─ TaskStorageProperties.java
│  │  ├─ TaskRemoteServiceProperties.java
│  │  ├─ MultipartConfig.java
│  │  └─ FeignClientConfig.java
│  ├─ interfaces/
│  │  ├─ controller/
│  │  │  ├─ TaskController.java
│  │  │  ├─ TaskQueryController.java
│  │  │  ├─ TaskLogController.java
│  │  │  ├─ TaskResultController.java
│  │  │  ├─ AdminTaskController.java
│  │  │  └─ AdminDashboardController.java
│  │  ├─ internal/
│  │  │  ├─ InternalTaskDispatchController.java
│  │  │  └─ InternalTaskReportController.java
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
│  │  ├─ rule/
│  │  └─ service/
│  └─ infrastructure/
│     ├─ client/
│     ├─ persistence/
│     ├─ storage/
│     └─ support/
└─ src/main/resources/application.yml
```

这个结构和 `task-service` 的复杂度是匹配的。相比其他模块，它明显多出了 `manager` 层与内部接口层，用于承接复杂生命周期编排。

## 4. 各部分功能与职责

### 4.1 接口层

#### `TaskController`

负责面向普通用户的任务命令类接口，包括：

- 创建任务
- 修改任务
- 上传输入归档
- 校验任务
- 提交任务
- 清除未提交任务
- 取消任务

这一层保持较薄，主要做参数接收和请求头中的 `X-User-Id` 透传，不直接承载复杂业务规则。

#### `TaskQueryController`

负责面向用户的查询类接口，包括：

- 我的任务分页
- 任务详情
- 状态历史
- 文件列表

其中详情和列表都会返回经过组装后的衍生字段，例如 `canRetry` 和 `queueReason`。

#### `AdminTaskController`

负责管理员任务操作，包括：

- 全量任务分页查询
- 调整任务优先级
- 重试失败或超时任务

这体现出系统已经将普通用户操作和管理员运维操作明确分离。

#### `InternalTaskDispatchController`

负责向 `scheduler-service` 暴露内部调度接口，包括：

- 拉取排队任务
- 标记任务已调度
- 标记任务已下发
- 标记下发失败
- 节点离线时批量回收受影响任务

这是 `task-service` 和调度器之间的核心协作边界。

#### `InternalTaskReportController`

负责向 `node-agent` 暴露内部回传接口，包括：

- 上报状态
- 上报日志
- 上报结果摘要
- 上报结果文件
- 标记完成
- 标记失败

所有这些接口都要求通过 `X-Node-Token` 做节点身份校验，而不是只依赖网关层的用户 Token。

### 4.2 应用层

#### `TaskCommandAppService`

负责对外统一暴露任务命令流程入口，将创建、修改、上传、校验、提交、取消、清除、优先级调整、重试等动作分发给对应管理器。

#### `TaskQueryAppService`

负责查询编排，是当前查询侧最重要的服务。它除了基本查询外，还负责：

- 组装求解器名称、模板名称、节点名称
- 统一计算 `queueReason`
- 统一返回 `canRetry`
- 汇总首页看板统计数据

这意味着查询侧不是简单查表，而是承担“状态解释层”的作用。

#### `TaskLogAppService` 与 `TaskResultAppService`

负责日志和结果查询、下载等读取逻辑，配合权限校验器使用，保证用户只能访问自己的任务数据，管理员可访问全部任务数据。

#### `NodeAgentAuthService`

负责节点回传安全校验，主要校验：

- 任务是否存在
- 任务是否已经绑定节点
- 上报节点是否与任务绑定节点一致
- `nodeToken` 是否通过 `scheduler-service` 验证

这个设计避免了任意节点伪造任务执行回传。

### 4.3 Manager 层

#### `TaskLifecycleManager`

这是当前模块最核心的编排类，负责：

- 创建任务并记录初始状态
- 修改任务
- 归档上传与替换旧归档
- 提交任务进入队列
- 取消任务
- 清除未提交任务
- 管理员调整优先级
- 管理员重试失败任务
- 标记已调度、已下发
- 接收节点状态变更上报
- 定时清理陈旧未提交任务

当前“重试复用原 taskId、清理上一轮运行态字段、刷新重新入队时间”的语义就是通过 `TaskStatusDomainService.transfer(..., QUEUED, ...)` 最终调用 `Task.submit()` 来落实的。

#### `TaskValidationManager`

专门负责任务校验编排，当前校验流程已经比较完整，包含：

- 校验任务是否可编辑
- 校验模板是否存在
- 校验 `solverId` 与 `profileId` 是否匹配
- 校验 `taskType` 与模板定义是否一致
- 校验是否存在输入归档
- 校验归档后缀是否为 zip
- 解压归档到任务工作目录
- 防止压缩包目录穿越
- 按模板文件规则校验路径、文件名模式、类型、数量、后缀约束
- 生成结构化 `ValidationIssue`

它已经不再是“只检查是否上传文件”的简单实现，而是一个真正的任务输入校验器。

#### `TaskDispatchManager`

负责把任务转换成调度器需要的内部任务对象，主要功能包括：

- 查询排队任务
- 将排队任务组装为 `TaskDTO`
- 补充执行元数据，如 `solverCode`、`commandTemplate`、`parserName`、`timeoutSeconds`
- 将任务标记为 `SCHEDULED`
- 将任务标记为 `DISPATCHED`
- 节点离线时批量标记相关任务失败

它是“业务任务”到“可执行任务”的桥梁。

#### `TaskResultManager`

负责执行结果回收，包括：

- 追加日志分片
- 保存结果摘要
- 保存结果文件
- 根据节点回传标记成功、失败或超时结束

它把分散上报的执行数据重新落入统一任务记录中。

### 4.4 领域层

#### `Task`

`Task` 是任务聚合根，承载任务生命周期中的关键状态字段与行为。  
当前领域行为包括：

- `markValidated()`
- `resetToCreated()`
- `submit()`
- `bindNode()`
- `adjustPriority()`
- `markScheduled()`
- `markDispatched()`
- `markRunning()`
- `markSuccess()`
- `markFailed()`
- `cancel()`
- `markTimeout()`

其中 `submit()` 非常关键，它会在重新入队时统一清理：

- `nodeId`
- `failType`
- `failMessage`
- `startTime`
- `endTime`

并刷新 `submitTime`，这使“失败重试”和“重新提交入队”在语义上保持一致。

#### `TaskStatusRule`

负责统一定义合法状态流转关系。当前已支持：

- `CREATED -> VALIDATED`
- `VALIDATED -> CREATED / QUEUED`
- `QUEUED -> SCHEDULED / CANCELED`
- `SCHEDULED -> DISPATCHED / FAILED`
- `DISPATCHED -> RUNNING / FAILED`
- `RUNNING -> SUCCESS / FAILED / TIMEOUT / CANCELED`
- `FAILED -> QUEUED`
- `TIMEOUT -> QUEUED`

这说明当前系统已经把“修改任务后回退到待校验”和“失败后管理员重试”纳入统一状态机。

#### `TaskStatusDomainService`

负责集中处理状态迁移和状态历史写入。  
它的价值在于：

- 所有状态变更都走同一入口
- 非法状态流转会统一拦截
- 每次状态变化都会写入 `task_status_history`
- 初始状态也会显式记录

这使任务生命周期具备可追溯性和一致性。

#### `TaskValidationDomainService`

负责纯规则类校验，例如：

- 任务当前是否可编辑
- 任务当前是否允许提交
- 必需文件规则检查

它和 `TaskValidationManager` 的关系是：

- `Manager` 负责流程编排
- `DomainService` 负责抽象规则

#### `TaskDomainService` 与 `TaskCancelRule`

负责取消和清除等动作的领域规则约束。当前规则是：

- 只有 `QUEUED`、`RUNNING` 可取消
- 只有 `CREATED`、`VALIDATED` 可清除

这种限制符合当前原型平台“未提交任务可丢弃，已排队或运行任务不能直接删除”的设计。

### 4.5 基础设施层

#### 仓储层

`TaskRepository`、`TaskFileRepository`、`TaskStatusHistoryRepository`、`TaskLogRepository`、`TaskResultSummaryRepository`、`TaskResultFileRepository` 共同完成任务主数据、文件、状态历史、日志与结果的持久化。

其中 `TaskRepositoryImpl` 已支持：

- 用户分页
- 管理员分页
- 按状态查询
- 按节点和状态集合查询
- 看板统计
- 查询陈旧未提交任务

这为任务管理、调度协作和运维统计提供了数据支撑。

#### `SolverClient`

负责从 `solver-service` 获取模板和求解器元数据，包括：

- 模板详情
- 模板对应求解器
- 模板任务类型
- 模板文件规则
- 上传规格
- 求解器编码和名称
- 模板执行元数据

`task-service` 当前大量规则都建立在该客户端提供的数据之上，因此它是配置驱动执行链路的关键接口。

#### `SchedulerClient`

负责调用 `scheduler-service`，当前已实现：

- 取消运行中任务
- 获取节点名称
- 获取排队可见性相关快照
- 获取在线节点汇总
- 校验节点 Token

但 `notifyTaskSubmitted()` 目前仍是空实现，说明当前调度链路仍以“调度器轮询排队任务”为主，而不是“提交后主动推调度”。

#### `TaskFileStorageService` 与 `LocalTaskFileStorageService`

负责任务文件落盘与清理。当前实现特征包括：

- 将输入文件存储在本地任务目录
- 当前上传入口以归档上传为主
- 支持删除整任务目录下的全部制品
- 文件路径会通过 `TaskStoragePathSupport` 做规范化处理

这是一套适合原型系统演示的本地文件存储方案。

#### `TaskPathResolver` 与 `TaskStoragePathSupport`

两者共同处理任务目录和路径展示问题：

- `TaskPathResolver` 负责生成任务目录、输入目录、日志目录、输出目录
- `TaskStoragePathSupport` 负责相对路径、绝对路径和展示路径之间的规范化转换

这样可以避免路径在不同层散落拼接。

### 4.6 定时任务

#### `StaleUnsubmittedTaskCleanupJob`

这是一个很有代表性的工程化补充设计。  
它会按固定周期清理长时间未提交的陈旧任务，避免：

- 无效任务长时间占用存储目录
- 草稿任务越积越多
- 原型平台演示环境持续膨胀

虽然逻辑简单，但很符合平台可维护性的要求。

## 5. 核心业务流程

### 5.1 任务创建与修改流程

```text
前端 -> TaskController.createTask/updateTask
    -> TaskCommandAppService
    -> TaskLifecycleManager
    -> TaskAssembler / TaskRepository
    -> TaskStatusDomainService.recordInitialStatus 或状态回退
```

其中修改流程有一个关键点：

- 如果任务参数发生变化且当前已处于 `VALIDATED`，系统会自动回退到 `CREATED`
- 同时写入状态历史，提示需要重新校验

这说明当前设计不是“校验一次永久有效”，而是“校验结果依赖当前输入和参数内容”。

### 5.2 文件上传与任务校验流程

```text
前端上传 zip 归档
  -> TaskController.uploadTaskFile
  -> TaskLifecycleManager
  -> LocalTaskFileStorageService 落盘
  -> 如替换旧归档则清理旧制品并视情况回退 CREATED

前端触发校验
  -> TaskController.validateTask
  -> TaskValidationManager
  -> SolverClient 获取模板规则
  -> 解压 workdir
  -> 校验路径/类型/名称/数量/后缀
  -> 通过后转为 VALIDATED
```

这一链路体现出当前系统已经形成“上传归档 -> 解压 -> 结构化校验 -> 状态晋级”的完整闭环。

### 5.3 提交、调度与执行流程

```text
用户提交任务
  -> TaskController.submitTask
  -> TaskLifecycleManager.submitTask
  -> TaskStatusDomainService.transfer(..., QUEUED)
  -> TaskRepository.update

scheduler-service 轮询
  -> InternalTaskDispatchController.listQueuedTasks
  -> TaskDispatchManager 组装 TaskDTO
  -> scheduler-service 选节点
  -> markScheduled / markDispatched

node-agent 执行
  -> InternalTaskReportController.reportStatus/log/result
  -> NodeAgentAuthService 验证 nodeToken
  -> TaskLifecycleManager / TaskResultManager
  -> RUNNING / SUCCESS / FAILED / TIMEOUT
```

这里可以看出，当前系统是典型的“任务服务管理生命周期，调度服务做资源分配，节点代理做实际执行”的三段式协作模型。

### 5.4 取消、清除与重试流程

#### 清除任务

仅允许 `CREATED`、`VALIDATED` 状态的未提交任务执行清除。执行时会：

- 删除任务文件记录
- 删除任务目录制品
- 将任务逻辑删除

#### 取消任务

- `QUEUED` 状态可直接取消
- `RUNNING` 状态则调用 `scheduler-service` 转发取消到节点

这说明运行中取消不是简单改数据库状态，而是要求和执行侧联动。

#### 重试任务

管理员可对 `FAILED`、`TIMEOUT` 状态任务进行重试。当前实现特征非常重要：

- 复用原 `taskId`
- 通过再次进入 `QUEUED` 实现重新入队
- 自动清理上一轮运行态字段
- 刷新 `submitTime`
- 保留完整状态历史

这使得“同一任务多轮执行”的轨迹可被追踪，而不是每次重试都新建任务。

## 6. 核心设计

### 6.1 设计一：用状态机统一管理复杂生命周期

`task-service` 最大的复杂度不在 CRUD，而在生命周期控制。  
当前项目通过：

- `Task`
- `TaskStatusRule`
- `TaskStatusDomainService`
- `TaskStatusHistory`

形成了统一状态机机制。这样做的好处是：

- 状态迁移规则集中
- 各处不会随意直接改状态
- 可以记录每次状态变化原因
- 便于查询和答辩时解释全流程

### 6.2 设计二：将“任务校验”独立成专门编排能力

很多原型项目会把校验写成一个很轻的接口，只判断参数是否为空。  
当前实现没有这样做，而是把校验拆成独立流程，体现出三个层次：

1. 任务元数据一致性校验
2. 输入归档可解压与安全校验
3. 按模板规则进行文件结构校验

这使系统更像一个真正的仿真任务平台，而不是普通工单系统。

### 6.3 设计三：查询层承担“任务状态解释”职责

当前查询接口不仅返回状态码，还额外返回：

- `canRetry`
- `queueReason`

其中 `queueReason` 会先结合任务真实排队顺序，再结合 `scheduler-service` 的节点快照动态解释：

- 前方仍有更高优先级或更早提交任务
- 暂无满足条件的可用节点
- 候选节点满载
- 调度器暂未处理

这说明当前系统已经考虑到“任务为什么在排队”的可见性问题，而不是只给前端一个静态状态码。

### 6.4 设计四：通过内部接口衔接调度与节点，而不是共享数据库

当前 `task-service` 通过内部 HTTP 接口与 `scheduler-service` 和 `node-agent` 协作，而不是跨库访问或直接耦合实现。  
这种设计体现了微服务边界意识：

- 调度器只关心“哪些任务可调度”
- 任务服务只关心“任务生命周期是否变化”
- 节点只关心“如何执行与如何回传”

即使当前实现仍偏原型，也已经具备清晰的职责分工。

### 6.5 设计五：重试语义不是简单回改状态

这是当前版本一个很关键的改进点。  
系统并不是把失败任务简单从 `FAILED` 改回 `QUEUED`，而是借由 `Task.submit()` 统一重建运行态语义，清理失败信息和时间字段，并刷新重新入队时间。  
这样才能保证：

- 调度排序与“重新提交”语义一致
- 用户不会看到上一轮的运行态残留
- 列表和详情能正确表达当前轮次状态

## 7. 架构难点与解决方案

### 7.1 难点一：生命周期复杂，容易出现状态混乱

问题：

- 创建、修改、校验、提交、调度、运行、取消、失败、重试之间关系复杂
- 如果每个接口都直接更新状态，极易出现状态失控

当前解决方案：

- 用 `TaskStatusRule` 集中定义允许的状态迁移
- 用 `TaskStatusDomainService` 统一迁移入口
- 每次迁移自动写状态历史

### 7.2 难点二：任务校验既要灵活，又不能做成复杂规则引擎

问题：

- CAE 输入文件天然有结构性要求
- 但本科原型项目又不适合引入过重的规则引擎

当前解决方案：

- 以 `solver-service` 的模板文件规则为规则源
- 通过 zip 解压 + glob 路径匹配 + 基础约束校验完成第一版
- 使用 `ruleJson` 预留数量、后缀等扩展能力

这样兼顾了可演示性和实现复杂度。

### 7.3 难点三：如何让排队状态“可解释”

问题：

- 如果前端只看到 `QUEUED`，用户很难理解任务为什么没被执行

当前解决方案：

- 查询接口先基于 `QUEUED` 任务真实排序判断是否存在排在前面的任务
- 再调用 `scheduler-service` 获取节点快照
- 综合排队顺序、可调度节点数、在线可用节点数等信息生成 `queueReason`

这使系统在演示时具备更好的可理解性。

### 7.4 难点四：节点回传存在安全风险

问题：

- 如果节点回传接口没有额外鉴权，任意请求都可能伪造任务状态

当前解决方案：

- 节点回传必须带 `X-Node-Token`
- 任务必须已绑定节点
- 上报节点必须与绑定节点一致
- `scheduler-service` 负责校验节点 Token 有效性

这种分层安全方案已经能满足原型平台的基本可信要求。

### 7.5 难点五：运行中取消与节点离线不是纯数据库问题

问题：

- 运行中任务不能只改数据库状态
- 节点离线时，调度中和运行中的任务都需要被回收

当前解决方案：

- 运行中取消转发给 `scheduler-service`
- 节点离线时由内部接口批量将相关任务标记失败

这体现出调度协作已从“静态状态管理”走向“面向真实执行场景的状态补偿”。

## 8. 关键技术手段

### 8.1 分层架构与 Manager 模式

相比普通业务服务，`task-service` 额外引入 `manager` 层承接重编排逻辑。  
这种做法的好处是：

- Controller 保持轻量
- AppService 负责统一入口
- Manager 负责复杂流程
- DomainService 负责规则
- Repository 负责持久化

非常适合当前这个高复杂度业务模块。

### 8.2 本地文件系统 + 任务目录组织

当前通过 `TaskPathResolver` 和 `LocalTaskFileStorageService` 实现：

- 每个任务独立目录
- 输入、日志、输出分目录管理
- 制品可整体清理

这对原型平台来说实现简单、展示直观、便于调试。

### 8.3 模板驱动校验

借助 `solver-service` 提供的：

- `taskType`
- 文件规则
- 上传规格
- 执行元数据

`task-service` 已经从“写死某个求解器逻辑”转向“根据模板元数据驱动校验和执行上下文组装”的方案。

### 8.4 任务状态历史沉淀

通过 `task_status_history` 与统一状态迁移服务，系统能够完整记录：

- 状态从哪里到哪里
- 为什么变化
- 谁触发变化
- 变化时间

这对过程追踪、问题排查和论文答辩都非常重要。

### 8.5 节点令牌校验

节点侧回传通过 `NodeAgentAuthService + SchedulerClient.verifyNodeToken()` 实现安全校验，体现出当前系统不是只做前端接口鉴权，也考虑了执行侧接口安全。

## 9. 当前实现的优点

- 已形成完整的任务生命周期闭环，不再只是任务创建和状态展示。
- 已支持任务修改后回退待校验、失败后复用原任务重试、管理员调整优先级等关键能力。
- 已具备基于模板规则的 zip 归档解压与结构化校验能力。
- 已实现查询侧 `canRetry` 和 `queueReason` 等衍生字段，用户可见性更好。
- 已通过内部接口打通调度器和节点代理协作边界。
- 已实现节点 Token 校验，内部回传接口安全性优于单纯白名单放行。
- 已支持看板汇总、状态历史、日志、结果、文件等多维查询能力。

## 10. 当前实现的局限与边界

### 10.1 调度触发仍以轮询为主

`SchedulerClient.notifyTaskSubmitted()` 目前仍为空实现。  
这意味着：

- 用户提交或管理员重试后，任务不会主动触发一次调度
- 仍需要等待调度器下一轮轮询

这属于当前原型版本保留的扩展点，而不是功能缺陷没有被考虑到。

### 10.2 上传模式当前只支持 ZIP_ONLY

虽然上传规格来自模板，但 `TaskLifecycleManager` 当前只真正支持归档上传模式。  
这意味着目录直传、多文件逐个上传等模式尚未展开。

### 10.3 文件校验仍以结构规则为主

当前已经支持路径、文件名、类型、数量、后缀等校验，但尚未深入到：

- 求解器级参数语义校验
- 文件内容级合法性校验
- 跨文件依赖校验

这更适合定义为后续扩展能力。

### 10.4 远程调用方式较轻量

当前 `task-service` 使用配置化 `RestTemplate` 调用其他服务，依赖静态服务地址，而不是完整服务发现、熔断、重试链路。  
这符合原型平台简化实现的目标，但不属于生产级服务治理方案。

### 10.5 文件存储为本地磁盘方案

当前没有接入对象存储、共享文件系统或制品仓库。  
因此它更适合单机演示、Docker 演示或小规模原型环境。

## 11. 对本科毕设的价值

从本科毕业设计角度看，`task-service` 是最值得重点讲解的模块之一，原因主要有三点：

1. 它最能体现“CAE 仿真任务调度与管理平台”的业务核心，而不是通用后台管理功能。
2. 它同时覆盖了任务建模、流程控制、规则校验、调度协作、节点回传、结果管理等多个关键技术点。
3. 它既有一定工程复杂度，又没有复杂到超出本科原型项目的实现与答辩范围。

因此，在论文和答辩中，`task-service` 可以作为系统核心模块重点展开。

## 12. 答辩时可采用的表述

可以将该模块概括为：

> `task-service` 是平台的任务生命周期管理中枢，负责 CAE 仿真任务从创建、修改、输入归档上传、模板规则校验、提交入队、调度协同、节点执行回传到结果查询的全过程管理。系统通过状态机和状态历史机制保证任务状态流转一致性，通过模板驱动的文件规则校验保证任务输入合法性，通过内部接口与调度服务和节点代理协作完成任务执行闭环，并在查询侧提供重试能力标识和排队原因解释，从而支撑平台形成可演示、可追踪、可解释的任务管理原型。

## 13. 后续可扩展方向

在不改变当前原型定位的前提下，后续可以继续扩展：

- 补上“提交或重试后主动触发一次调度”的能力
- 支持更多上传模式，如目录上传、多文件直传
- 引入更细粒度的参数模式校验与内容级校验
- 将本地文件存储升级为共享存储或对象存储
- 将 `RestTemplate` 调用升级为更完整的服务治理方案
- 增加任务重试轮次、执行批次等更细粒度运行记录
- 对日志与结果回传增加更强的幂等控制和断点续传能力

这些内容都更适合作为扩展点，而不是当前首版必须完成内容。

## 14. 当前结论

`task-service` 当前已经完成了本项目原型平台中最关键、最有难度的一组能力：

- 可创建、修改、校验、提交、取消、清除和重试任务
- 可维护完整任务状态机和状态历史
- 可基于模板规则完成输入归档解压与文件结构校验
- 可与调度服务协同完成任务派发状态管理
- 可接收节点侧状态、日志和结果回传
- 可对用户和管理员提供较完整的查询与可见性能力

从实现深度看，它已经不是简单的任务表管理，而是一个具备生命周期编排、规则校验、调度协作和结果汇聚能力的任务中枢原型。  
从本科毕设要求看，这个模块已经足以支撑“CAE 仿真任务调度与管理平台”的核心业务表达，也是系统中最具代表性的关键模块。

## 15. 快速定位版：层级目录与文件夹

为了方便直接查代码，下面把 `task-service` 每一层与对应文件夹再明确列一次。

| 分层 | 对应文件夹 | 说明 |
| --- | --- | --- |
| 启动入口层 | `task-service/src/main/java/com/example/cae/task/` | Spring Boot 启动入口 |
| 配置层 | `task-service/src/main/java/com/example/cae/task/config/` | `RestTemplate`、上传、远程地址、存储根目录等配置 |
| 接口层 | `task-service/src/main/java/com/example/cae/task/interfaces/` | 对外接口、内部接口、请求体、响应体 |
| 应用层 | `task-service/src/main/java/com/example/cae/task/application/` | Facade、应用服务、Manager、定时任务、Assembler |
| 领域层 | `task-service/src/main/java/com/example/cae/task/domain/` | 任务模型、仓储抽象、状态规则、校验规则 |
| 基础设施层 | `task-service/src/main/java/com/example/cae/task/infrastructure/` | 远程客户端、数据库实现、文件存储、辅助组件 |
| 资源配置层 | `task-service/src/main/resources/` | 服务端口、数据源、multipart、存储和远程服务配置 |

如果想最快定位问题，可以按下面记：

- 看任务命令入口：`interfaces/controller/TaskController.java`
- 看任务查询入口：`interfaces/controller/TaskQueryController.java`
- 看调度器对接：`interfaces/internal/InternalTaskDispatchController.java`
- 看节点回传：`interfaces/internal/InternalTaskReportController.java`
- 看生命周期总编排：`application/manager/TaskLifecycleManager.java`
- 看任务校验：`application/manager/TaskValidationManager.java`
- 看调度载荷组装：`application/manager/TaskDispatchManager.java`
- 看日志和结果回收：`application/manager/TaskResultManager.java`
- 看状态流转规则：`domain/service/TaskStatusDomainService.java`
- 看文件存储：`infrastructure/storage/LocalTaskFileStorageService.java`

## 16. 逐文件索引

这一节按“文件夹 -> 文件”的方式列出当前主要源码文件及其作用，方便后续定位。

### 16.1 根目录与启动入口

对应文件夹：

```text
task-service/
task-service/src/main/java/com/example/cae/task/
task-service/src/main/resources/
```

- `pom.xml`
  作用：声明 `task-service` 子模块依赖，打包为可运行 Spring Boot 服务。
- `src/main/resources/application.yml`
  作用：配置服务端口、数据库、multipart 限制、远程服务地址、任务与结果文件根目录。
- `TaskApplication.java`
  作用：服务启动入口；额外启用了 `@EnableScheduling`，说明本模块有定时任务。

### 16.2 配置层

对应文件夹：

```text
task-service/src/main/java/com/example/cae/task/config/
```

- `TaskServiceConfig.java`
  作用：定义 `RestTemplate` Bean，并设置连接/读取超时，供远程调用 `solver-service` 和 `scheduler-service` 使用。
- `TaskStorageProperties.java`
  作用：读取 `cae.storage.*` 配置，统一管理任务文件与结果文件根目录。
- `TaskRemoteServiceProperties.java`
  作用：读取 `cae.remote.*` 配置，统一管理远程求解器服务和调度服务基地址。
- `MultipartConfig.java`
  作用：当前为空配置类，作为上传相关扩展配置的预留位置。
- `FeignClientConfig.java`
  作用：当前为空配置类，文件名说明原本预期用于 Feign 或远程调用配置，但当前实际还是 `RestTemplate`。

### 16.3 接口层：对外 Controller

对应文件夹：

```text
task-service/src/main/java/com/example/cae/task/interfaces/controller/
```

- `TaskController.java`
  作用：普通用户任务命令入口，负责创建、更新、上传输入包、校验、提交、废弃、取消任务。
- `TaskQueryController.java`
  作用：普通用户任务查询入口，负责我的任务分页、详情、状态历史、任务文件列表。
- `TaskLogController.java`
  作用：日志查询与日志下载接口。
- `TaskResultController.java`
  作用：结果摘要查询、结果文件列表查询和结果文件下载接口。
- `AdminTaskController.java`
  作用：管理员任务入口，负责全量任务查询、优先级调整和失败/超时任务重试。
- `AdminDashboardController.java`
  作用：管理员首页看板汇总接口，返回任务总量、运行量、排队量、成功率、在线节点数等摘要数据。

### 16.4 接口层：内部 Controller

对应文件夹：

```text
task-service/src/main/java/com/example/cae/task/interfaces/internal/
```

- `InternalTaskDispatchController.java`
  作用：面向 `scheduler-service` 的内部接口，负责拉取排队任务、标记已调度、标记已下发、标记下发失败、批量处理节点离线受影响任务。
- `InternalTaskReportController.java`
  作用：面向 `node-agent` 的内部回传接口，负责状态回传、日志回传、结果摘要回传、结果文件回传、任务完成、任务失败。

### 16.5 接口层：请求对象

对应文件夹：

```text
task-service/src/main/java/com/example/cae/task/interfaces/request/
```

- `CreateTaskRequest.java`
  作用：创建任务请求体，承载任务名称、求解器、模板、任务类型、优先级、参数等。
- `UpdateTaskRequest.java`
  作用：更新任务请求体，主要修改任务名称、优先级和参数。
- `CancelTaskRequest.java`
  作用：取消任务时传递取消原因。
- `DiscardTaskRequest.java`
  作用：废弃未提交任务时传递原因。
- `ValidateTaskRequest.java`
  作用：当前为空请求类，属于预留类；校验接口当前不需要请求体。
- `SubmitTaskRequest.java`
  作用：当前为空请求类，属于预留类；提交接口当前不需要请求体。
- `TaskListQueryRequest.java`
  作用：任务分页查询过滤条件，支持任务号、任务名、状态、优先级、求解器、模板、节点、用户、任务类型、失败类型、时间范围等筛选。
- `UpdateTaskPriorityRequest.java`
  作用：管理员调整任务优先级请求体。
- `RetryTaskRequest.java`
  作用：管理员重试任务时传递重试原因。
- `TaskNodeMarkRequest.java`
  作用：内部调度接口用于传递 `nodeId` 的简化请求体。
- `InternalTaskFailRequest.java`
  作用：调度器标记下发失败时传递 `failType` 与 `reason`。
- `NodeOfflineTasksRequest.java`
  作用：节点离线时批量标记任务失败的请求体。
- `StatusReportRequest.java`
  作用：节点回传状态变更请求体，正式字段为 `nodeId`、`fromStatus`、`toStatus`、`changeReason`、`operatorType`。
- `LogReportRequest.java`
  作用：节点回传日志分片，请求中包含 `nodeId`、`seqNo`、`logContent`。
- `ResultSummaryReportRequest.java`
  作用：节点回传结果摘要，包含成功标记、运行时长、摘要文本、指标数据。
- `ResultFileReportRequest.java`
  作用：节点回传结果文件元数据，包含文件名、文件类型、存储路径、文件大小等。
- `MarkFinishedRequest.java`
  作用：节点回传任务完成事件，请求中包含 `nodeId` 和可选最终状态。
- `MarkFailedRequest.java`
  作用：节点回传任务失败事件，请求中包含 `nodeId`、失败类型、失败消息。

### 16.6 接口层：响应对象

对应文件夹：

```text
task-service/src/main/java/com/example/cae/task/interfaces/response/
```

- `TaskCreateResponse.java`
  作用：创建任务成功后的返回结构。
- `TaskUpdateResponse.java`
  作用：更新任务后的返回结构。
- `TaskValidateResponse.java`
  作用：任务校验返回结构，包含校验是否通过、当前状态、问题列表。
- `TaskSubmitResponse.java`
  作用：任务提交或重试后的返回结构。
- `TaskListItemResponse.java`
  作用：任务分页列表项，承载列表展示字段以及 `queueReason`、`canRetry` 等衍生信息。
- `AdminTaskListItemResponse.java`
  作用：管理员任务分页列表项，在普通列表字段基础上补充 `userId`、`username`、`failType`。
- `TaskDetailResponse.java`
  作用：任务详情返回结构，包含基础任务信息、调度/执行相关字段和可解释信息。
- `TaskFileResponse.java`
  作用：任务输入文件或归档文件的返回结构。
- `TaskStatusHistoryResponse.java`
  作用：任务状态历史记录返回结构。
- `TaskLogResponse.java`
  作用：单条日志分片的返回结构。
- `TaskLogPageResponse.java`
  作用：日志分页返回结构，包含日志记录列表和下一序号。
- `TaskResultSummaryResponse.java`
  作用：任务结果摘要返回结构。
- `TaskResultFileResponse.java`
  作用：任务结果文件列表项返回结构。
- `TaskDashboardSummaryResponse.java`
  作用：管理员看板摘要返回结构。

### 16.7 应用层：Assembler

对应文件夹：

```text
task-service/src/main/java/com/example/cae/task/application/assembler/
```

- `TaskAssembler.java`
  作用：在任务请求对象、领域对象、持久化对象和任务详情/列表/创建/更新响应之间做转换，是任务对象转换核心类。
- `TaskLogAssembler.java`
  作用：在日志领域对象、持久化对象、日志响应之间做转换。
- `TaskResultAssembler.java`
  作用：在结果摘要/结果文件领域对象和结果响应之间做转换，并借助 `TaskStoragePathSupport` 处理展示路径。

### 16.8 应用层：Facade

对应文件夹：

```text
task-service/src/main/java/com/example/cae/task/application/facade/
```

- `TaskCommandFacade.java`
  作用：对外封装任务命令类操作，转发创建、更新、上传、校验、提交、废弃、取消、调优、重试等流程。
- `TaskQueryFacade.java`
  作用：对外封装任务查询类操作，统一聚合任务详情、状态历史、文件、日志、结果摘要、结果文件等查询。

### 16.9 应用层：Manager

对应文件夹：

```text
task-service/src/main/java/com/example/cae/task/application/manager/
```

- `TaskLifecycleManager.java`
  作用：任务生命周期总编排器，负责创建、更新、上传归档、提交、取消、废弃、重试、优先级调整、状态回传等核心流程。
- `TaskValidationManager.java`
  作用：任务输入校验编排器，负责读取模板规则、解压 ZIP、校验目录穿越、校验路径/文件名/类型/后缀/数量等。
- `TaskDispatchManager.java`
  作用：把排队任务转换为 `TaskDTO` 派发给调度器，同时负责标记调度状态、下发状态和节点离线受影响任务处理。
- `TaskResultManager.java`
  作用：汇聚节点回传的日志、结果摘要、结果文件，并负责任务完成/失败状态收口。

### 16.10 应用层：定时任务

对应文件夹：

```text
task-service/src/main/java/com/example/cae/task/application/scheduler/
```

- `StaleUnsubmittedTaskCleanupJob.java`
  作用：定时清理长时间未提交的陈旧任务，调用 `TaskLifecycleManager.cleanStaleUnsubmittedTasks()`。

### 16.11 应用层：应用服务

对应文件夹：

```text
task-service/src/main/java/com/example/cae/task/application/service/
```

- `TaskCommandAppService.java`
  作用：任务命令类应用服务，对接控制器并把具体流程分发给 `TaskLifecycleManager` 和 `TaskValidationManager`。
- `TaskQueryAppService.java`
  作用：任务查询类应用服务，负责列表、详情、状态历史、文件列表、队列原因解释、管理员看板汇总。
- `TaskLogAppService.java`
  作用：日志读取服务，负责分页读取日志分片和拼装完整日志文本。
- `TaskResultAppService.java`
  作用：结果读取服务，负责结果摘要查询、结果文件列表和结果文件流式下载。
- `NodeAgentAuthService.java`
  作用：节点回传鉴权服务，校验任务、绑定节点、上报节点与 `nodeToken` 是否匹配。

### 16.12 领域层：枚举

对应文件夹：

```text
task-service/src/main/java/com/example/cae/task/domain/enums/
```

- `TaskQueryScopeEnum.java`
  作用：定义任务查询范围相关枚举，通常用于区分“我的任务”和“管理员全量任务”等查询语义。
- `TaskSortFieldEnum.java`
  作用：定义任务查询支持的排序字段枚举。

### 16.13 领域层：模型

对应文件夹：

```text
task-service/src/main/java/com/example/cae/task/domain/model/
```

- `Task.java`
  作用：任务聚合根，承载任务生命周期字段、状态转换、是否结束、是否归属当前用户、优先级调整等核心行为。
- `TaskFile.java`
  作用：任务文件领域对象，表示输入文件、归档文件等元数据。
- `TaskLogChunk.java`
  作用：任务日志分片领域对象。
- `TaskResultSummary.java`
  作用：任务结果摘要领域对象。
- `TaskResultFile.java`
  作用：任务结果文件领域对象。
- `TaskStatusHistory.java`
  作用：任务状态流转历史领域对象。

### 16.14 领域层：仓储抽象

对应文件夹：

```text
task-service/src/main/java/com/example/cae/task/domain/repository/
```

- `TaskRepository.java`
  作用：任务主表仓储抽象，负责任务读写、分页、状态统计、按节点查询、查陈旧未提交任务等。
- `TaskFileRepository.java`
  作用：任务文件仓储抽象。
- `TaskLogRepository.java`
  作用：任务日志仓储抽象。
- `TaskResultSummaryRepository.java`
  作用：结果摘要仓储抽象。
- `TaskResultFileRepository.java`
  作用：结果文件仓储抽象。
- `TaskStatusHistoryRepository.java`
  作用：状态历史仓储抽象。

### 16.15 领域层：规则

对应文件夹：

```text
task-service/src/main/java/com/example/cae/task/domain/rule/
```

- `TaskCancelRule.java`
  作用：定义哪些状态允许取消、哪些状态允许废弃。
- `TaskStatusRule.java`
  作用：定义任务状态转移图和完成态判定，是状态机规则核心文件。
- `TaskValidationRule.java`
  作用：定义任务是否允许执行校验的基础条件。

### 16.16 领域层：领域服务

对应文件夹：

```text
task-service/src/main/java/com/example/cae/task/domain/service/
```

- `TaskDomainService.java`
  作用：对 `TaskCancelRule` 做服务封装，提供 `canCancel`、`canDiscard` 这类业务判断。
- `TaskStatusDomainService.java`
  作用：状态机编排核心，负责合法状态转移并写入状态历史。
- `TaskValidationDomainService.java`
  作用：定义任务是否可编辑、是否可提交，以及基础文件规则匹配逻辑。
- `TaskResultDomainService.java`
  作用：结果摘要与结果文件的基础合法性校验，目前实现较轻。

### 16.17 基础设施层：远程客户端

对应文件夹：

```text
task-service/src/main/java/com/example/cae/task/infrastructure/client/
```

- `SolverClient.java`
  作用：通过 `RestTemplate` 调用 `solver-service`，获取模板详情、模板规则、上传规范、求解器名称、模板名称、执行元数据等。
- `SchedulerClient.java`
  作用：通过 `RestTemplate` 调用 `scheduler-service`，处理任务取消、节点名称查询、排队原因快照、在线节点摘要、节点 token 校验等。

### 16.18 基础设施层：持久化实体

对应文件夹：

```text
task-service/src/main/java/com/example/cae/task/infrastructure/persistence/entity/
```

- `TaskPO.java`
  作用：`sim_task` 主表持久化对象。
- `TaskFilePO.java`
  作用：任务文件表持久化对象。
- `TaskLogChunkPO.java`
  作用：任务日志分片表持久化对象。
- `TaskResultSummaryPO.java`
  作用：结果摘要表持久化对象。
- `TaskResultFilePO.java`
  作用：结果文件表持久化对象。
- `TaskStatusHistoryPO.java`
  作用：状态历史表持久化对象。

### 16.19 基础设施层：Mapper

对应文件夹：

```text
task-service/src/main/java/com/example/cae/task/infrastructure/persistence/mapper/
```

- `TaskMapper.java`
  作用：任务主表 SQL，承载任务查询、分页、状态统计、更新等。
- `TaskFileMapper.java`
  作用：任务文件表 SQL。
- `TaskLogChunkMapper.java`
  作用：任务日志分片表 SQL。
- `TaskResultSummaryMapper.java`
  作用：结果摘要表 SQL。
- `TaskResultFileMapper.java`
  作用：结果文件表 SQL。
- `TaskStatusHistoryMapper.java`
  作用：状态历史表 SQL。

### 16.20 基础设施层：Repository 实现

对应文件夹：

```text
task-service/src/main/java/com/example/cae/task/infrastructure/persistence/repository/
```

- `TaskRepositoryImpl.java`
  作用：`TaskRepository` 的数据库实现。
- `TaskFileRepositoryImpl.java`
  作用：`TaskFileRepository` 的数据库实现。
- `TaskLogRepositoryImpl.java`
  作用：`TaskLogRepository` 的数据库实现。
- `TaskResultSummaryRepositoryImpl.java`
  作用：`TaskResultSummaryRepository` 的数据库实现。
- `TaskResultFileRepositoryImpl.java`
  作用：`TaskResultFileRepository` 的数据库实现。
- `TaskStatusHistoryRepositoryImpl.java`
  作用：`TaskStatusHistoryRepository` 的数据库实现。

### 16.21 基础设施层：文件存储

对应文件夹：

```text
task-service/src/main/java/com/example/cae/task/infrastructure/storage/
```

- `TaskFileStorageService.java`
  作用：文件存储抽象接口，定义保存输入文件、打开文件、删除文件、删除任务产物等能力。
- `LocalTaskFileStorageService.java`
  作用：本地磁盘存储实现，当前项目真实采用的文件存储方式。

### 16.22 基础设施层：辅助组件

对应文件夹：

```text
task-service/src/main/java/com/example/cae/task/infrastructure/support/
```

- `TaskNoGenerator.java`
  作用：生成任务编号 `TASKyyyyMMddHHmmssxxx`。
- `TaskPathResolver.java`
  作用：统一生成任务根目录、输入目录、日志目录、结果目录、日志文件路径。
- `TaskPermissionChecker.java`
  作用：统一做任务访问权限校验，管理员放行，普通用户只允许访问自己的任务。
- `TaskQueryBuilder.java`
  作用：清洗和兜底任务分页查询参数，例如默认页码和分页大小。
- `TaskStoragePathSupport.java`
  作用：在存储路径、绝对路径、展示路径之间做统一转换。
- `LogChunkAppender.java`
  作用：把日志分片追加写入本地日志文件；当前是辅助工具类。

### 16.23 当前最关键的 10 个文件

如果时间有限，最值得优先看的 10 个文件是：

1. `interfaces/controller/TaskController.java`
2. `interfaces/internal/InternalTaskDispatchController.java`
3. `interfaces/internal/InternalTaskReportController.java`
4. `application/manager/TaskLifecycleManager.java`
5. `application/manager/TaskValidationManager.java`
6. `application/manager/TaskDispatchManager.java`
7. `application/manager/TaskResultManager.java`
8. `application/service/TaskQueryAppService.java`
9. `domain/service/TaskStatusDomainService.java`
10. `infrastructure/storage/LocalTaskFileStorageService.java`

这 10 个文件基本就能把 `task-service` 的任务生命周期主链路看清楚。
