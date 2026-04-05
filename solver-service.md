# solver-service 模块分析文档

## 1. 模块定位

`solver-service` 是平台中的求解器配置中心，负责描述“平台支持哪些求解器、每类求解器有哪些任务模板、每个模板需要哪些输入文件与参数规则”。

在当前项目中，它承担的核心职责主要有五类：

- 求解器定义管理
- 模板管理
- 文件规则管理
- 上传规范生成
- 面向其他微服务的模板与求解器内部查询

它不负责实际执行仿真任务，也不负责任务状态流转与调度决策。它更像是整个平台的“元数据与规则中心”。

这也是当前系统扩展性的关键所在：当平台要接入新的求解器或新的任务类型时，优先修改的是 `solver-service` 中的配置与模板，而不是去改任务主链路或调度主链路。

## 2. 模块在系统中的作用

从系统整体看，`solver-service` 主要解决以下问题：

1. 平台能支持哪些求解器，由谁统一维护。
2. 某个求解器支持哪些任务模板与任务类型，由谁统一定义。
3. 某个模板需要哪些输入文件、文件命名规则、是否必填、排序顺序，由谁统一管理。
4. 前端在用户选定模板后，如何得到上传说明与参数说明。
5. `task-service` 在任务校验时，如何获取模板详情、参数模式和文件规则。

因此，`solver-service` 在系统中起到的是“规则驱动入口”和“求解配置基础设施”的作用。

## 3. 当前实现架构

### 3.1 技术栈

- Spring Boot
- 分层架构：`interfaces / application / domain / infrastructure`
- MyBatis 注解 Mapper
- `common-lib` 中的错误码、返回体与跨服务 DTO

### 3.2 当前目录结构

当前代码结构如下：

```text
solver-service/
└── src/main/java/com/example/cae/solver/
    ├── SolverApplication.java
    ├── application/
    │   ├── assembler/
    │   │   ├── FileRuleAssembler.java
    │   │   ├── ProfileAssembler.java
    │   │   └── SolverAssembler.java
    │   ├── facade/
    │   │   ├── ProfileFacade.java
    │   │   └── SolverFacade.java
    │   └── service/
    │       ├── FileRuleAppService.java
    │       ├── ProfileAppService.java
    │       ├── SolverAppService.java
    │       └── UploadSpecAppService.java
    ├── config/
    │   └── SolverServiceConfig.java
    ├── domain/
    │   ├── model/
    │   │   ├── SolverDefinition.java
    │   │   ├── SolverProfileFileRule.java
    │   │   └── SolverTaskProfile.java
    │   ├── repository/
    │   │   ├── FileRuleRepository.java
    │   │   ├── ProfileRepository.java
    │   │   └── SolverRepository.java
    │   └── service/
    │       ├── ProfileRuleDomainService.java
    │       └── SolverDomainService.java
    ├── infrastructure/
    │   ├── persistence/
    │   │   ├── entity/
    │   │   │   ├── SolverDefinitionPO.java
    │   │   │   ├── SolverProfileFileRulePO.java
    │   │   │   └── SolverTaskProfilePO.java
    │   │   ├── mapper/
    │   │   │   ├── SolverDefinitionMapper.java
    │   │   │   ├── SolverProfileFileRuleMapper.java
    │   │   │   └── SolverTaskProfileMapper.java
    │   │   └── repository/
    │   │       ├── FileRuleRepositoryImpl.java
    │   │       ├── ProfileRepositoryImpl.java
    │   │       └── SolverRepositoryImpl.java
    │   └── support/
    │       ├── ProfileRuleValidator.java
    │       └── UploadSpecBuilder.java
    ├── interfaces/
    │   ├── controller/
    │   │   ├── FileRuleController.java
    │   │   ├── ProfileController.java
    │   │   └── SolverController.java
    │   ├── internal/
    │   │   ├── InternalProfileController.java
    │   │   └── InternalSolverController.java
    │   ├── request/
    │   │   ├── CreateFileRuleRequest.java
    │   │   ├── CreateProfileRequest.java
    │   │   ├── CreateSolverRequest.java
    │   │   ├── ProfilePageQueryRequest.java
    │   │   ├── SolverPageQueryRequest.java
    │   │   ├── UpdateFileRuleRequest.java
    │   │   ├── UpdateProfileRequest.java
    │   │   ├── UpdateProfileStatusRequest.java
    │   │   ├── UpdateSolverRequest.java
    │   │   └── UpdateSolverStatusRequest.java
    │   └── response/
    │       ├── FileRuleCreateResponse.java
    │       ├── FileRuleResponse.java
    │       ├── InternalProfileDetailResponse.java
    │       ├── ProfileCreateResponse.java
    │       ├── ProfileDetailResponse.java
    │       ├── ProfileListItemResponse.java
    │       ├── SolverCreateResponse.java
    │       ├── SolverDetailResponse.java
    │       ├── SolverListItemResponse.java
    │       ├── SolverTaskOptionResponse.java
    │       └── UploadSpecResponse.java
    └── support/
        └── SolverQueryBuilder.java
```

这个结构和 `solver-service` 的定位是匹配的：它不是执行型服务，而是规则配置型服务，因此重点落在模型、规则与数据组装上。

## 4. 各部分功能与职责

### 4.1 启动与配置层

#### `SolverApplication`

职责：

- 启动 `solver-service`
- 扫描各层组件

#### `SolverServiceConfig`

当前为轻量配置承载类，主要提供基础 Spring 配置容器。

### 4.2 接口层

#### `SolverController`

职责：

- 求解器分页查询
- 求解器详情查询
- 求解器创建
- 求解器更新
- 求解器启停
- 查询某求解器下的模板列表
- 查询某求解器可用于任务创建的模板选项

这里的“任务选项”接口很重要，它把模板选择能力向任务创建前台或任务服务调用方暴露出来。

#### `ProfileController`

职责：

- 模板分页查询
- 模板详情查询
- 模板创建
- 模板更新
- 模板启停
- 查询模板上传规范
- 查询模板文件规则列表

这是当前 `solver-service` 最核心的控制器之一。

#### `FileRuleController`

职责：

- 为模板新增文件规则
- 修改文件规则
- 删除文件规则

它体现了“文件规则围绕模板管理”的设计思路。

#### `InternalSolverController`

职责：

- 向其他微服务暴露求解器详情内部接口 `/internal/solvers/{solverId}`

#### `InternalProfileController`

职责：

- 向其他微服务暴露模板详情内部接口 `/internal/profiles/{profileId}`
- 返回模板核心字段与对应文件规则

这两个内部接口主要服务于 `task-service` 的任务创建与校验流程。

### 4.3 应用层

#### `SolverFacade`

职责：

- 对求解器相关流程做一层薄封装
- 保持 Controller 轻量

#### `ProfileFacade`

职责：

- 统一编排模板、文件规则、上传规范相关应用服务

它实际上是当前模板域的应用层入口。

#### `SolverAppService`

职责：

- 编排求解器分页、详情、创建、更新、启停
- 编排求解器下模板查询与任务选项查询

#### `ProfileAppService`

职责：

- 编排模板分页、详情、创建、更新、启停
- 查询模板文件规则列表

#### `FileRuleAppService`

职责：

- 编排文件规则新增、修改、删除
- 校验文件规则基本合法性
- 检查模板存在性与规则冲突

#### `UploadSpecAppService`

职责：

- 根据模板与文件规则生成上传规范响应
- 确保模板已启用

这是当前系统里前端上传引导与任务输入校验的桥梁服务。

### 4.4 领域层

#### `SolverDefinition`

职责：

- 表达求解器定义
- 承载启用、禁用等基础行为

它描述的是“平台支持什么求解器”这一层。

#### `SolverTaskProfile`

职责：

- 表达某求解器下的具体任务模板
- 承载启停、超时修改等行为

它描述的是“这个求解器支持什么任务类型、如何上传、如何构造命令、如何解析结果”。

当前核心字段包括：

- `profileCode`
- `taskType`
- `profileName`
- `uploadMode`
- `commandTemplate`
- `paramsSchemaJson`
- `parserName`
- `timeoutSeconds`

#### `SolverProfileFileRule`

职责：

- 表达模板关联的文件规则
- 区分是否必需
- 提供基于模式的文件名匹配能力

当前核心字段包括：

- `fileKey`
- `pathPattern`
- `fileNamePattern`
- `fileType`
- `requiredFlag`
- `sortOrder`
- `ruleJson`

#### `SolverRepository / ProfileRepository / FileRuleRepository`

职责：

- 定义求解器、模板、文件规则的数据访问抽象

#### `SolverDomainService`

职责：

- 维护求解器领域规则
- 当前主要用于校验 `solverCode` 唯一性

#### `ProfileRuleDomainService`

职责：

- 维护模板与文件规则相关领域规则
- 校验模板编码唯一性
- 校验模板是否启用
- 校验文件规则是否冲突

它是当前模板规则域的核心领域服务。

### 4.5 基础设施层

#### `SolverDefinitionMapper`

职责：

- 操作 `solver_definition` 相关数据
- 提供求解器查询、分页、计数、插入、更新能力

#### `SolverTaskProfileMapper`

职责：

- 操作 `solver_task_profile` 相关数据
- 提供模板查询、分页、按求解器查询、按启用状态查询等能力

#### `SolverProfileFileRuleMapper`

职责：

- 操作 `solver_profile_file_rule` 相关数据
- 提供文件规则查询、插入、更新、删除能力

#### `SolverRepositoryImpl / ProfileRepositoryImpl / FileRuleRepositoryImpl`

职责：

- 将领域仓储接口落地为数据库实现
- 完成 `PO <-> Domain` 转换

#### `UploadSpecBuilder`

职责：

- 将模板与文件规则组装为统一上传规范
- 输出 `requiredFiles / optionalFiles / fileRules`
- 构造归档上传规则 `archiveRule`

这是当前实现里非常关键的“规则翻译层”。

#### `ProfileRuleValidator`

职责：

- 做文件规则创建与更新时的基础字段校验

它与领域服务分工不同：

- `ProfileRuleValidator` 负责请求参数完整性；
- `ProfileRuleDomainService` 负责业务语义冲突。

### 4.6 组装层

#### `SolverAssembler`

职责：

- 负责求解器领域对象和响应对象之间的转换

#### `ProfileAssembler`

职责：

- 负责模板领域对象和响应对象之间的转换

#### `FileRuleAssembler`

职责：

- 负责文件规则领域对象和响应对象之间的转换

这些 Assembler 让控制器与仓储都不直接操作复杂 DTO 细节。

## 5. 核心业务流程

### 5.1 求解器管理流程

```text
SolverController
  -> SolverFacade
  -> SolverAppService
  -> SolverDomainService
  -> SolverRepository
  -> Response
```

### 5.2 模板管理流程

```text
ProfileController
  -> ProfileFacade
  -> ProfileAppService
  -> SolverRepository / ProfileRepository / FileRuleRepository
  -> Response
```

### 5.3 文件规则管理流程

```text
FileRuleController
  -> ProfileFacade
  -> FileRuleAppService
  -> ProfileRuleValidator
  -> ProfileRuleDomainService
  -> FileRuleRepository
  -> Response
```

### 5.4 上传规范生成流程

```text
ProfileController
  -> ProfileFacade
  -> UploadSpecAppService
  -> ProfileRepository
  -> FileRuleRepository
  -> ProfileRuleDomainService.checkProfileEnabled
  -> UploadSpecBuilder
  -> UploadSpecResponse
```

### 5.5 内部模板查询流程

```text
task-service 等其他服务
  -> /internal/profiles/{profileId}
  -> InternalProfileController
  -> ProfileFacade
  -> ProfileAppService.getProfileDetail
  -> ProfileFacade.getFileRules
  -> InternalProfileDetailResponse
```

这条链路在当前系统中非常重要，因为任务校验就是建立在模板元数据之上的。

## 6. 核心设计

### 6.1 设计一：把求解器配置、模板配置和文件规则集中到一个服务

当前系统没有把“求解器中心”“模板中心”“规则中心”再拆成多个服务，而是统一放在 `solver-service` 中。

这样做的原因是：

- 它们本身强相关，天然属于同一配置域；
- 拆太细会增加跨服务通信复杂度；
- 对本科毕设原型平台来说，集中管理更利于实现和讲解。

### 6.2 设计二：模板是连接求解器和任务的核心对象

在当前架构中，真正连接“求解器”和“任务”的不是求解器本身，而是模板 `SolverTaskProfile`。

模板承载了以下关键内容：

- 任务类型
- 上传模式
- 参数模式
- 命令模板
- 结果解析器
- 超时时间

这意味着：

- 新增求解器时，不只是新增一条求解器记录；
- 真正让任务可创建、可校验、可执行的是模板。

### 6.3 设计三：文件规则围绕模板管理

文件规则不直接挂在求解器上，而是挂在模板上。

这样设计的原因是：

- 同一个求解器可能对应多种任务类型；
- 不同任务类型对输入文件要求可能完全不同；
- 文件规则必须随着模板粒度变化，而不是随着求解器粒度变化。

### 6.4 设计四：上传规范由后端统一生成

当前系统不是让前端硬编码上传要求，而是由后端根据模板和规则动态生成 `UploadSpecResponse`。

这样做的好处是：

- 前端只负责展示，不负责维护规则逻辑；
- 规则变更后，前端无需同步改代码；
- `task-service` 也可以复用同一套模板规则做校验。

### 6.5 设计五：对其他服务暴露内部查询接口，而不是共享数据库

当前 `solver-service` 提供：

- `/internal/solvers/{solverId}`
- `/internal/profiles/{profileId}`

用于向其他服务暴露元数据。

这符合微服务架构中“服务拥有数据，其他服务通过接口访问”的原则。

## 7. 架构难点与解决方案

### 7.1 难点一：如何让平台具备“新增求解器尽量少改代码”的能力

问题：

- 如果每增加一个求解器都要改任务服务、调度服务、节点执行逻辑，扩展成本会很高。

当前解决方案：

- 将求解器定义、模板、文件规则集中到 `solver-service`；
- 通过模板字段描述执行方式与输入要求；
- 让任务服务更多依赖模板元数据，而不是写死求解器逻辑。

这正是当前系统可扩展性的核心设计。

### 7.2 难点二：如何统一前端上传引导与后端校验依据

问题：

- 如果前端显示的上传说明和后端校验逻辑来自两套规则，很容易不一致。

当前解决方案：

- 由 `solver-service` 统一生成 `UploadSpecResponse`；
- 规则源头统一来自模板和文件规则表。

这样前端展示和后端校验至少具备同源基础。

### 7.3 难点三：如何在规则灵活性和实现复杂度之间平衡

问题：

- 文件规则如果做得过于复杂，会引入规则引擎级别的实现成本；
- 如果太简单，又难以表达不同模板的差异。

当前解决方案：

- 规则层先采用 `fileKey / pathPattern / fileNamePattern / fileType / requiredFlag / sortOrder / ruleJson` 这样的轻量模型；
- 用 `ruleJson` 预留后续扩展空间；
- 当前主流程只做基础合法性检查与冲突检查。

### 7.4 难点四：如何处理模板启停与规则生效范围

问题：

- 已存在的模板可能暂时不希望继续用于新任务创建；
- 但历史任务记录仍然需要保留原模板信息。

当前解决方案：

- 模板保留 `enabled` 状态；
- 通过 `checkProfileEnabled()` 控制上传规范获取与任务选项查询；
- 历史数据仍可通过详情接口查询。

这是一种典型的“软停用”策略。

## 8. 关键技术手段

### 8.1 分层架构

通过 `controller / facade / service / domain / repository` 分层，清晰表达：

- 接口暴露
- 流程编排
- 领域规则
- 数据访问

### 8.2 MyBatis 注解 SQL

用于实现：

- 求解器分页
- 模板分页
- 文件规则查询
- 各类按条件查询

注解 SQL 对当前原型项目来说足够轻量，易于维护。

### 8.3 模板参数模式 `paramsSchemaJson`

通过 `paramsSchemaJson` 字段表达模板参数结构，使：

- 前端可动态生成参数表单
- 后端可基于模式做参数校验

这是一种典型的配置驱动设计手段。

### 8.4 命令模板 `commandTemplate`

通过模板记录命令模板，而不是写死执行命令，为后续 `node-agent` 执行侧提供配置基础。

### 8.5 上传规范组装器 `UploadSpecBuilder`

用于：

- 把规则模型翻译成前端和任务服务可直接消费的响应结构
- 按必填 / 非必填拆分文件列表
- 增加归档上传规则

这是当前模块最有代表性的技术实现之一。

## 9. 当前实现的优点

- 求解器、模板、文件规则三层模型已经建立清楚。
- 已形成上传规范生成能力，不只是简单 CRUD。
- 已对外暴露内部接口，能支撑任务服务校验链路。
- 已将模板参数模式、命令模板、解析器、超时等关键执行元数据纳入模型。
- 规则冲突检查、模板启用校验等核心领域规则已有落地。

## 10. 当前实现的局限与边界

### 10.1 仍是配置中心原型，不是完整规则引擎

当前虽然有 `ruleJson` 和参数模式，但还没有演进成真正的规则引擎或可视化 DSL 平台。

### 10.2 `execMode` 只是配置字段，尚未完全打通执行层

当前求解器与模板里已经有：

- `execMode`
- `commandTemplate`
- `parserName`

但 `execMode` 在整体项目中仍主要停留在配置层，尚未完全形成“本地进程执行 / 容器执行”两种完整运行模式。

### 10.3 上传规则仍以基础表达为主

当前规则主要覆盖：

- 路径模式
- 文件名模式
- 文件类型
- 是否必填
- 排序

更复杂的跨文件依赖、条件规则、版本兼容约束等尚未展开。

### 10.4 参数模式当前主要作为透传元数据

`paramsSchemaJson` 已经进入模板模型和上传规范响应，但更深层的参数模式驱动校验和前端自动表单能力，还需要其他模块配合才能完全体现。

## 11. 对本科毕设的价值

从本科毕设角度看，`solver-service` 的价值非常高，主要体现在：

1. 它说明系统不是“写死流程”的任务平台，而是可扩展的多求解器平台。
2. 它把新增求解器、模板和输入规则抽象成可配置模型，体现了良好的可扩展性设计。
3. 它为任务创建、任务校验和节点执行提供统一元数据来源，是整个系统配置驱动思想的核心体现。

因此，在答辩时，`solver-service` 是非常值得重点讲解的模块之一。

## 12. 答辩时可采用的表述

可以将该模块概括为：

> `solver-service` 负责平台中求解器、任务模板与输入文件规则的统一管理，是系统的求解配置中心。系统通过模板化方式描述任务类型、参数模式、命令模板、结果解析器和输入规则，并由该服务动态生成上传规范，供前端引导用户上传、供任务服务执行校验，从而实现多求解器场景下的配置驱动扩展能力。

## 13. 后续可扩展方向

后续在不改变当前架构前提下，可继续扩展：

- 更复杂的参数模式校验机制
- 更细的文件规则语义
- 规则可视化编辑能力
- 真实的命令模板解析器
- 模板版本化
- 模板导入导出

这些都应作为扩展能力，而不是当前首版已实现能力。

## 14. 当前结论

`solver-service` 当前已经完成了本科毕设原型平台所需的关键职责：

- 可以统一管理求解器定义
- 可以统一管理任务模板
- 可以统一管理模板文件规则
- 可以生成上传规范
- 可以通过内部接口向任务服务提供模板元数据

从系统价值看，它是当前平台实现“多求解器扩展能力”的核心模块；从实现深度看，它已经不是简单配置 CRUD，而是具备规则组织与元数据组装能力的配置中心原型。
