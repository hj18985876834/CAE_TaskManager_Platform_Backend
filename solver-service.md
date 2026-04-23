# solver-service 模块说明文档

## 1. 文档目的

这份文档继续沿用 `user-service` 的说明方式，不只介绍“这个模块是干什么的”，而是把 `solver-service` 拆到“模块根目录 -> 分层目录 -> 代码文件”的粒度，方便下面几类工作：

- 快速定位某个接口对应哪个文件
- 理清求解器定义、模板、文件规则、上传规范之间的关系
- 写论文、模块设计说明、答辩稿时区分“设计目标”和“当前真实实现”
- 后续改代码时知道先去哪个文件夹看

分析范围以 `solver-service/src/main/java` 与 `solver-service/src/main/resources` 为主，不把 `target/` 编译产物作为设计分析对象。

---

## 2. 模块定位

`solver-service` 是平台中的“求解器配置中心”和“规则元数据中心”，它负责回答下面几个问题：

1. 平台当前支持哪些求解器。
2. 每个求解器下面有哪些可选任务模板。
3. 每个模板要求上传哪些输入文件。
4. 每个模板的执行命令模板、参数模式、超时时间、结果解析器是什么。
5. 任务创建和任务校验时，其他服务该去哪里拿这些元数据。

因此，`solver-service` 在系统中的定位不是执行服务，而是“配置中心型服务”。

它主要负责四类对象：

- `SolverDefinition`：求解器定义
- `SolverTaskProfile`：求解器任务模板
- `SolverProfileFileRule`：模板文件规则
- `UploadSpecResponse`：面向前端和任务服务生成的上传规范

它不负责：

- 不直接执行仿真任务
- 不负责任务状态流转
- 不做节点调度
- 不做结果回传

简单说：

- `user-service` 解决“谁在用系统”
- `solver-service` 解决“系统支持什么求解器和规则”
- `task-service` 解决“任务如何创建与流转”
- `scheduler-service` 解决“任务分配给谁执行”

---

## 3. 模块根目录与源码入口

模块根目录是：

```text
solver-service/
```

当前最重要的几个位置是：

- `solver-service/pom.xml`
- `solver-service/src/main/java/com/example/cae/solver/`
- `solver-service/src/main/resources/application.yml`

不作为设计分析重点的目录：

```text
solver-service/target/
```

`target/` 下的是编译产物，不应该写成源码层设计内容。

---

## 4. 分层与文件夹映射

| 分层 | 对应文件夹 | 作用 |
| --- | --- | --- |
| 启动入口层 | `solver-service/src/main/java/com/example/cae/solver/` | Spring Boot 启动入口 |
| 配置层 | `solver-service/src/main/java/com/example/cae/solver/config/` | Spring 与 MyBatis 相关配置 |
| 接口层 | `solver-service/src/main/java/com/example/cae/solver/interfaces/` | 对外接口、内部接口、请求对象、响应对象 |
| 应用层 | `solver-service/src/main/java/com/example/cae/solver/application/` | 业务编排、Facade、对象组装 |
| 领域层 | `solver-service/src/main/java/com/example/cae/solver/domain/` | 领域模型、仓储抽象、领域规则、枚举 |
| 基础设施层 | `solver-service/src/main/java/com/example/cae/solver/infrastructure/` | 持久化实现、规则构造辅助类 |
| 支撑层 | `solver-service/src/main/java/com/example/cae/solver/support/` | 预留的查询构造辅助类 |
| 资源配置层 | `solver-service/src/main/resources/` | 端口、数据库连接等运行配置 |

如果只想快速找代码，可以按下面记：

- 看 REST 接口：`interfaces/controller`
- 看内部接口：`interfaces/internal`
- 看 DTO：`interfaces/request`、`interfaces/response`
- 看“求解器/模板/文件规则”主流程：`application/service`
- 看领域规则：`domain/service`
- 看数据库访问：`infrastructure/persistence`
- 看上传规范构造：`infrastructure/support/UploadSpecBuilder`

---

## 5. 模块结构总览

当前模块源码结构如下：

```text
solver-service/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/cae/solver/
│       │       ├── SolverApplication.java
│       │       ├── application/
│       │       │   ├── assembler/
│       │       │   │   ├── FileRuleAssembler.java
│       │       │   │   ├── ProfileAssembler.java
│       │       │   │   └── SolverAssembler.java
│       │       │   ├── facade/
│       │       │   │   ├── ProfileFacade.java
│       │       │   │   └── SolverFacade.java
│       │       │   └── service/
│       │       │       ├── FileRuleAppService.java
│       │       │       ├── ProfileAppService.java
│       │       │       ├── SolverAppService.java
│       │       │       └── UploadSpecAppService.java
│       │       ├── config/
│       │       │   ├── MybatisPlusConfig.java
│       │       │   └── SolverServiceConfig.java
│       │       ├── domain/
│       │       │   ├── enums/
│       │       │   │   ├── RuleFileTypeEnum.java
│       │       │   │   └── SolverExecModeEnum.java
│       │       │   ├── model/
│       │       │   │   ├── SolverDefinition.java
│       │       │   │   ├── SolverProfileFileRule.java
│       │       │   │   └── SolverTaskProfile.java
│       │       │   ├── repository/
│       │       │   │   ├── FileRuleRepository.java
│       │       │   │   ├── ProfileRepository.java
│       │       │   │   └── SolverRepository.java
│       │       │   └── service/
│       │       │       ├── ProfileRuleDomainService.java
│       │       │       └── SolverDomainService.java
│       │       ├── infrastructure/
│       │       │   ├── persistence/
│       │       │   │   ├── entity/
│       │       │   │   │   ├── SolverDefinitionPO.java
│       │       │   │   │   ├── SolverProfileFileRulePO.java
│       │       │   │   │   └── SolverTaskProfilePO.java
│       │       │   │   ├── mapper/
│       │       │   │   │   ├── SolverDefinitionMapper.java
│       │       │   │   │   ├── SolverProfileFileRuleMapper.java
│       │       │   │   │   └── SolverTaskProfileMapper.java
│       │       │   │   └── repository/
│       │       │   │       ├── FileRuleRepositoryImpl.java
│       │       │   │       ├── ProfileRepositoryImpl.java
│       │       │   │       └── SolverRepositoryImpl.java
│       │       │   └── support/
│       │       │       ├── CommandTemplateResolver.java
│       │       │       ├── ProfileRuleValidator.java
│       │       │       └── UploadSpecBuilder.java
│       │       ├── interfaces/
│       │       │   ├── controller/
│       │       │   │   ├── FileRuleController.java
│       │       │   │   ├── ProfileController.java
│       │       │   │   └── SolverController.java
│       │       │   ├── internal/
│       │       │   │   ├── InternalProfileController.java
│       │       │   │   └── InternalSolverController.java
│       │       │   ├── request/
│       │       │   │   ├── CreateFileRuleRequest.java
│       │       │   │   ├── CreateProfileRequest.java
│       │       │   │   ├── CreateSolverRequest.java
│       │       │   │   ├── ProfilePageQueryRequest.java
│       │       │   │   ├── SolverPageQueryRequest.java
│       │       │   │   ├── UpdateFileRuleRequest.java
│       │       │   │   ├── UpdateProfileRequest.java
│       │       │   │   ├── UpdateProfileStatusRequest.java
│       │       │   │   ├── UpdateSolverRequest.java
│       │       │   │   └── UpdateSolverStatusRequest.java
│       │       │   └── response/
│       │       │       ├── FileRuleCreateResponse.java
│       │       │       ├── FileRuleResponse.java
│       │       │       ├── InternalProfileDetailResponse.java
│       │       │       ├── ProfileCreateResponse.java
│       │       │       ├── ProfileDetailResponse.java
│       │       │       ├── ProfileListItemResponse.java
│       │       │       ├── SolverCreateResponse.java
│       │       │       ├── SolverDetailResponse.java
│       │       │       ├── SolverListItemResponse.java
│       │       │       ├── SolverTaskOptionResponse.java
│       │       │       └── UploadSpecResponse.java
│       │       └── support/
│       │           └── SolverQueryBuilder.java
│       └── resources/
│           └── application.yml
└── target/
```

---

## 6. 根目录文件说明

### 6.1 `solver-service/pom.xml`

作用：

- 声明 `solver-service` 是父工程子模块。
- 引入 Web、校验、MyBatis、MySQL 和 `common-lib` 依赖。
- 配置 Spring Boot 打包。

当前关键依赖：

- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `mybatis-plus-spring-boot3-starter`
- `mysql-connector-j`
- `common-lib`

说明：

- 和 `user-service` 一样，虽然引入了 MyBatis-Plus starter，但当前实现以 MyBatis 注解 SQL 为主。

### 6.2 `solver-service/src/main/resources/application.yml`

作用：

- 配置服务名、端口、编码和数据源。

当前关键配置：

- 服务名：`solver-service`
- 默认端口：`8082`
- 数据库：`solver_db`

这意味着 `solver-service` 当前采用独立服务、独立数据库的微服务方式。

---

## 7. 启动入口层

### 7.1 对应文件夹

```text
solver-service/src/main/java/com/example/cae/solver/
```

### 7.2 文件说明

#### `SolverApplication.java`

作用：

- `solver-service` 的 Spring Boot 启动入口。
- 使用 `@SpringBootApplication(scanBasePackages = "com.example.cae")` 统一扫描组件。

定位意义：

- 服务启动问题、Bean 扫描问题、自动装配问题，首先从这里看。

---

## 8. 配置层

### 8.1 对应文件夹

```text
solver-service/src/main/java/com/example/cae/solver/config/
```

### 8.2 文件说明

#### `SolverServiceConfig.java`

作用：

- 当前是空的 `@Configuration` 配置类。

状态判断：

- 主要起预留配置承载作用。
- 当前没有额外显式 Bean 定义。

#### `MybatisPlusConfig.java`

作用：

- 当前也是空的 `@Configuration` 配置类。

状态判断：

- 文件名说明原本预期承载 MyBatis-Plus 的增强配置。
- 当前没有分页插件、拦截器等具体内容。

文档建议：

- 不要写成“已完成 MyBatis-Plus 高级配置”。

---

## 9. 接口层

### 9.1 对应文件夹

```text
solver-service/src/main/java/com/example/cae/solver/interfaces/
```

该目录分成：

- `controller/`
- `internal/`
- `request/`
- `response/`

---

### 9.2 对外控制器目录

对应文件夹：

```text
solver-service/src/main/java/com/example/cae/solver/interfaces/controller/
```

#### `SolverController.java`

作用：

- 管理“求解器定义”相关接口。
- 根路径是 `/api/solvers`。

当前接口：

- `GET /api/solvers`
- `GET /api/solvers/{solverId}`
- `POST /api/solvers`
- `PUT /api/solvers/{solverId}`
- `POST /api/solvers/{solverId}/status`
- `GET /api/solvers/{solverId}/profiles`
- `GET /api/solvers/{solverId}/task-options`

各方法作用：

- `pageSolvers(...)`：求解器分页
- `getSolverDetail(...)`：求解器详情
- `createSolver(...)`：新建求解器
- `updateSolver(...)`：更新求解器元数据
- `updateSolverStatus(...)`：更新求解器启停状态
- `updateSolverStatusPost(...)`：兼容式 POST 状态接口
- `getSolverProfiles(...)`：查看某求解器下的全部模板
- `getSolverTaskOptions(...)`：查看某求解器下可用于任务创建的模板选项

关键说明：

- 这里同时提供 `PUT` 和 `POST` 两种状态变更接口，属于兼容式接口设计。

#### `ProfileController.java`

作用：

- 管理“模板”相关接口。
- 根路径是 `/api/profiles`。

当前接口：

- `GET /api/profiles`
- `GET /api/profiles/{profileId}`
- `POST /api/profiles`
- `PUT /api/profiles/{profileId}`
- `POST /api/profiles/{profileId}/status`
- `GET /api/profiles/{profileId}/upload-spec`
- `GET /api/profiles/{profileId}/file-rules`

各方法作用：

- `pageProfiles(...)`：模板分页
- `getProfileDetail(...)`：模板详情
- `createProfile(...)`：创建模板
- `updateProfile(...)`：更新模板元数据
- `updateProfileStatus(...)`：更新模板启停状态
- `updateProfileStatusPost(...)`：兼容式 POST 状态接口
- `getUploadSpec(...)`：生成模板上传规范
- `getFileRules(...)`：查询模板文件规则列表

#### `FileRuleController.java`

作用：

- 管理“模板文件规则”接口。
- 根路径是 `/api`，但实际路径组合后分别挂在：
  - `/api/profiles/{profileId}/file-rules`
  - `/api/file-rules/{id}`

当前接口：

- `POST /api/profiles/{profileId}/file-rules`
- `PUT /api/file-rules/{id}`
- `DELETE /api/file-rules/{id}`

各方法作用：

- `createFileRule(...)`：给模板新增文件规则
- `updateFileRule(...)`：修改规则
- `deleteFileRule(...)`：删除规则

---

### 9.3 内部接口目录

对应文件夹：

```text
solver-service/src/main/java/com/example/cae/solver/interfaces/internal/
```

#### `InternalSolverController.java`

作用：

- 对其他微服务暴露内部求解器详情接口。
- 根路径是 `/internal/solvers`。

当前接口：

- `GET /internal/solvers/{solverId}`

返回：

- 直接复用 `SolverDetailResponse`。

说明：

- 当前没有单独定义 “内部求解器 DTO”，而是直接复用外部详情响应对象。

#### `InternalProfileController.java`

作用：

- 对其他微服务暴露内部模板详情接口。
- 根路径是 `/internal/profiles`。

当前接口：

- `GET /internal/profiles/{profileId}`

实际行为：

1. 先通过 `profileFacade.getProfileDetail(profileId)` 查模板详情。
2. 再通过 `profileFacade.getFileRules(profileId)` 查文件规则。
3. 手动组装成 `InternalProfileDetailResponse`。

说明：

- 它没有直接返回 `ProfileDetailResponse`，而是生成内部专用返回对象。
- 这样做更适合给 `task-service` 做任务校验时使用。

---

### 9.4 请求对象目录

对应文件夹：

```text
solver-service/src/main/java/com/example/cae/solver/interfaces/request/
```

#### `CreateSolverRequest.java`

作用：

- 创建求解器请求体。

主要字段：

- `solverCode`
- `solverName`
- `version`
- `execMode`
- `execPath`
- `enabled`
- `description`

注意点：

- 当前已统一使用 `description` 字段，不再保留 `remark` 兼容口径。

#### `UpdateSolverRequest.java`

作用：

- 更新求解器请求体。

字段与创建类似，但不包含 `solverCode` 和 `enabled`。

#### `UpdateSolverStatusRequest.java`

作用：

- 更新求解器启停状态请求体。

核心字段：

- `enabled`

#### `CreateProfileRequest.java`

作用：

- 创建模板请求体。

主要字段：

- `solverId`
- `profileCode`
- `taskType`
- `profileName`
- `uploadMode`
- `commandTemplate`
- `paramsSchema`
- `paramsSchemaJson`
- `parserName`
- `timeoutSeconds`
- `enabled`
- `description`

注意点：

- `paramsSchema` 和 `paramsSchemaJson` 同时存在。
- 当前实现会优先使用 `paramsSchema`，否则回退到 `paramsSchemaJson`。

#### `UpdateProfileRequest.java`

作用：

- 更新模板请求体。

主要字段与创建类似，但不包含 `solverId`、`profileCode`、`enabled`。

#### `UpdateProfileStatusRequest.java`

作用：

- 更新模板启停状态请求体。

核心字段：

- `enabled`

#### `CreateFileRuleRequest.java`

作用：

- 创建模板文件规则请求体。

主要字段：

- `fileKey`
- `pathPattern`
- `fileNamePattern`
- `fileType`
- `requiredFlag`
- `sortOrder`
- `description`
- `ruleJson`

注意点：

- 当前已统一使用 `description` 字段。

#### `UpdateFileRuleRequest.java`

作用：

- 更新文件规则请求体。

字段与创建类似，但不包含 `fileKey`。

#### `SolverPageQueryRequest.java`

作用：

- 求解器分页查询条件对象。

主要字段：

- `pageNum`
- `pageSize`
- `solverCode`
- `solverName`
- `enabled`

#### `ProfilePageQueryRequest.java`

作用：

- 模板分页查询条件对象。

主要字段：

- `pageNum`
- `pageSize`
- `solverId`
- `taskType`
- `profileCode`
- `enabled`

---

### 9.5 响应对象目录

对应文件夹：

```text
solver-service/src/main/java/com/example/cae/solver/interfaces/response/
```

这一层的类主要承载对外或对内返回结构。

#### `SolverListItemResponse.java`

作用：

- 求解器分页列表中的单条记录。

#### `SolverDetailResponse.java`

作用：

- 求解器详情返回对象，包含执行模式、执行路径、启停状态、描述等。

说明：

- 当前已统一为 `description` 风格字段。

#### `SolverCreateResponse.java`

作用：

- 创建求解器后的最小结果对象。

#### `ProfileListItemResponse.java`

作用：

- 模板分页列表中的单条记录。

包含：

- 模板标识
- 所属求解器
- 任务类型
- 上传模式
- 命令模板
- 参数模式
- 解析器
- 超时时间
- 启停状态

#### `ProfileDetailResponse.java`

作用：

- 模板详情对象。

特点：

- 包含 `fileRules` 字段，表示模板详情会一并带出文件规则列表。

#### `ProfileCreateResponse.java`

作用：

- 创建模板后的返回对象。

#### `FileRuleResponse.java`

作用：

- 文件规则详情/列表返回对象。

特点：

- 同时存在 `id` 和 `ruleId`
- 当前已统一使用 `description`

这属于接口兼容式冗余字段设计。

#### `FileRuleCreateResponse.java`

作用：

- 创建文件规则后的返回对象。

#### `SolverTaskOptionResponse.java`

作用：

- 面向任务创建阶段的模板选项返回对象。

只保留：

- `profileId`
- `taskType`
- `profileName`

#### `UploadSpecResponse.java`

作用：

- 模板上传规范返回对象。

这是 `solver-service` 中最重要的响应结构之一，主要给前端和 `task-service` 使用。

包含：

- 模板基本信息
- 参数模式
- 上传模式
- 文件规则总表
- 必选文件列表
- 可选文件列表
- `ArchiveRule` 压缩包规范

#### `InternalProfileDetailResponse.java`

作用：

- 内部模板详情返回对象。

用途：

- 主要供 `task-service` 做模板校验和任务准备时使用。

---

## 10. 应用层

### 10.1 对应文件夹

```text
solver-service/src/main/java/com/example/cae/solver/application/
```

该层分为：

- `assembler/`
- `facade/`
- `service/`

---

### 10.2 Assembler 目录

对应文件夹：

```text
solver-service/src/main/java/com/example/cae/solver/application/assembler/
```

#### `SolverAssembler.java`

作用：

- 在 `CreateSolverRequest`、`SolverDefinition`、`SolverDefinitionPO`、求解器响应对象之间做转换。

关键方法：

- `toSolver(...)`
- `toListItemResponse(...)`
- `toDetailResponse(...)`
- `toCreateResponse(...)`
- `fromPO(...)`
- `toPO(...)`

实现细节：

- `resolveDescription(...)` 已统一按 `description` 读取并写入领域对象的 `description` 字段。

#### `ProfileAssembler.java`

作用：

- 在模板请求、模板领域对象、模板 PO、模板响应之间做转换。

关键方法：

- `toProfile(...)`
- `toListItemResponse(...)`
- `toDetailResponse(...)`
- `toCreateResponse(...)`
- `toTaskOptionResponse(...)`
- `fromPO(...)`
- `toPO(...)`

实现细节：

- `resolveParamsSchema(...)` 会把 `paramsSchema` / `paramsSchemaJson` 统一落到领域对象的 `paramsSchemaJson` 字段中。

#### `FileRuleAssembler.java`

作用：

- 在文件规则请求、领域对象、PO、响应对象之间做转换。

关键方法：

- `toRule(...)`
- `toResponse(...)`
- `toCreateResponse(...)`
- `fromPO(...)`
- `toPO(...)`

实现细节：

- 已统一为 `description` 字段映射逻辑。

---

### 10.3 Facade 目录

对应文件夹：

```text
solver-service/src/main/java/com/example/cae/solver/application/facade/
```

#### `SolverFacade.java`

作用：

- 对求解器相关应用服务做薄封装。

当前方法：

- `pageSolvers(...)`
- `getSolverDetail(...)`
- `createSolver(...)`
- `updateSolver(...)`
- `updateSolverStatus(...)`
- `getSolverTaskOptions(...)`
- `getSolverProfiles(...)`

#### `ProfileFacade.java`

作用：

- 对模板、文件规则、上传规范相关能力做统一封装。

当前方法：

- `pageProfiles(...)`
- `getProfileDetail(...)`
- `createProfile(...)`
- `updateProfile(...)`
- `updateProfileStatus(...)`
- `getFileRules(...)`
- `buildUploadSpec(...)`
- `createFileRule(...)`
- `updateFileRule(...)`
- `deleteFileRule(...)`

说明：

- 这是当前 `solver-service` 里最像“模板域入口”的类。

---

### 10.4 应用服务目录

对应文件夹：

```text
solver-service/src/main/java/com/example/cae/solver/application/service/
```

#### `SolverAppService.java`

作用：

- 编排“求解器定义”相关主流程。

当前主要方法：

- `pageSolvers(...)`
- `getSolverDetail(...)`
- `createSolver(...)`
- `updateSolver(...)`
- `updateSolverStatus(...)`
- `getSolverTaskOptions(...)`
- `getSolverProfiles(...)`

关键行为：

- 创建求解器时先做 `solverCode` 唯一性校验。
- 默认启用，除非请求明确传 `enabled = 0`。
- 查询任务选项时，实际取的是该求解器下启用状态的模板。

实现细节：

- `updateSolver(...)` 已统一按 `description` 更新描述字段。

#### `ProfileAppService.java`

作用：

- 编排“模板”相关主流程。

当前主要方法：

- `pageProfiles(...)`
- `getProfileDetail(...)`
- `createProfile(...)`
- `updateProfile(...)`
- `updateProfileStatus(...)`
- `getFileRules(...)`

关键行为：

- 创建模板前先校验对应求解器存在且已启用。
- 同一求解器下的 `profileCode` 必须唯一。
- 获取模板详情时，会把文件规则列表一并组装进去。

实现细节：

- `updateProfile(...)` 会把 `paramsSchema` 或 `paramsSchemaJson` 统一写入 `paramsSchemaJson`。

#### `FileRuleAppService.java`

作用：

- 编排“模板文件规则”相关流程。

当前主要方法：

- `createFileRule(...)`
- `updateFileRule(...)`
- `deleteFileRule(...)`

关键行为：

- 创建规则前校验模板存在。
- 调用 `ProfileRuleValidator` 做基础字段检查。
- 调用 `ProfileRuleDomainService.checkRuleConflict` 防止同一模板下 `fileKey` 冲突。

需要注意：

- `deleteFileRule(...)` 当前直接按 ID 删除，没有先做存在性校验。

#### `UploadSpecAppService.java`

作用：

- 为指定模板生成上传规范。

当前主要方法：

- `buildUploadSpec(Long profileId)`

关键流程：

1. 查询模板。
2. 校验模板已启用。
3. 查询该模板全部文件规则。
4. 调用 `UploadSpecBuilder.build(...)` 组装上传规范。

---

## 11. 领域层

### 11.1 对应文件夹

```text
solver-service/src/main/java/com/example/cae/solver/domain/
```

该层包含：

- `enums/`
- `model/`
- `repository/`
- `service/`

---

### 11.2 枚举目录

对应文件夹：

```text
solver-service/src/main/java/com/example/cae/solver/domain/enums/
```

#### `RuleFileTypeEnum.java`

作用：

- 定义文件规则类型枚举：
  - `FILE`
  - `DIR`
  - `ZIP`

当前状态：

- 已定义，但当前请求校验并没有严格按枚举值校验。

#### `SolverExecModeEnum.java`

作用：

- 定义求解器执行模式枚举：
  - `LOCAL`
  - `CONTAINER`

当前状态：

- 已定义，但当前请求侧也没有做强约束。

---

### 11.3 领域模型目录

对应文件夹：

```text
solver-service/src/main/java/com/example/cae/solver/domain/model/
```

#### `SolverDefinition.java`

作用：

- 表达求解器定义领域对象。

主要字段：

- `id`
- `solverCode`
- `solverName`
- `version`
- `execMode`
- `execPath`
- `enabled`
- `description`

领域行为：

- `enable()`
- `disable()`
- `isEnabled()`

#### `SolverTaskProfile.java`

作用：

- 表达模板领域对象。

主要字段：

- `id`
- `solverId`
- `profileCode`
- `taskType`
- `profileName`
- `uploadMode`
- `commandTemplate`
- `paramsSchemaJson`
- `parserName`
- `timeoutSeconds`
- `enabled`
- `description`

领域行为：

- `enable()`
- `disable()`
- `isEnabled()`
- `changeTimeout(...)`

#### `SolverProfileFileRule.java`

作用：

- 表达模板文件规则领域对象。

主要字段：

- `id`
- `profileId`
- `fileKey`
- `pathPattern`
- `fileNamePattern`
- `fileType`
- `requiredFlag`
- `sortOrder`
- `ruleJson`
- `description`

领域行为：

- `isRequired()`
- `matches(String fileName)`

说明：

- `matches(...)` 当前只基于 `fileNamePattern` 做简单通配匹配。

---

### 11.4 仓储抽象目录

对应文件夹：

```text
solver-service/src/main/java/com/example/cae/solver/domain/repository/
```

#### `SolverRepository.java`

作用：

- 定义求解器仓储抽象。

主要方法：

- `findById(...)`
- `findBySolverCode(...)`
- `save(...)`
- `update(...)`
- `page(...)`
- `count(...)`

#### `ProfileRepository.java`

作用：

- 定义模板仓储抽象。

主要方法：

- `findById(...)`
- `findBySolverIdAndProfileCode(...)`
- `save(...)`
- `update(...)`
- `page(...)`
- `count(...)`
- `listEnabledBySolverId(...)`
- `listBySolverId(...)`

#### `FileRuleRepository.java`

作用：

- 定义文件规则仓储抽象。

主要方法：

- `findById(...)`
- `save(...)`
- `update(...)`
- `delete(...)`
- `listByProfileId(...)`

---

### 11.5 领域服务目录

对应文件夹：

```text
solver-service/src/main/java/com/example/cae/solver/domain/service/
```

#### `SolverDomainService.java`

作用：

- 承载求解器领域规则。

当前规则：

- `solverCode` 不能为空
- `solverCode` 必须唯一

主要方法：

- `checkSolverCodeUnique(String solverCode)`

#### `ProfileRuleDomainService.java`

作用：

- 承载模板和文件规则相关领域规则。

当前主要方法：

- `buildUploadSpec(...)`
- `checkProfileCodeUnique(...)`
- `checkProfileEnabled(...)`
- `checkRuleConflict(...)`

实际使用情况：

- `checkProfileCodeUnique(...)`、`checkProfileEnabled(...)`、`checkRuleConflict(...)` 正在被应用层使用。
- `buildUploadSpec(...)` 当前并没有成为上传规范主构造入口，真正负责组装的是 `UploadSpecBuilder`。

这一点在文档里建议写清楚，避免把它写成“当前上传规范就是由领域服务直接生成”。

---

## 12. 基础设施层

### 12.1 对应文件夹

```text
solver-service/src/main/java/com/example/cae/solver/infrastructure/
```

它分为：

- `persistence/`
- `support/`

---

### 12.2 持久化目录

对应文件夹：

```text
solver-service/src/main/java/com/example/cae/solver/infrastructure/persistence/
```

#### 12.2.1 `entity/`

对应文件夹：

```text
solver-service/src/main/java/com/example/cae/solver/infrastructure/persistence/entity/
```

##### `SolverDefinitionPO.java`

作用：

- `solver_definition` 表的持久化对象。

##### `SolverTaskProfilePO.java`

作用：

- `solver_task_profile` 表的持久化对象。

##### `SolverProfileFileRulePO.java`

作用：

- `solver_profile_file_rule` 表的持久化对象。

#### 12.2.2 `mapper/`

对应文件夹：

```text
solver-service/src/main/java/com/example/cae/solver/infrastructure/persistence/mapper/
```

##### `SolverDefinitionMapper.java`

作用：

- 负责 `solver_definition` 表的查询、插入、更新和分页。

主要方法：

- `selectById(...)`
- `selectBySolverCode(...)`
- `insert(...)`
- `updateById(...)`
- `selectPage(...)`
- `count(...)`

实现细节：

- 数据库中的 `description` 字段已直接映射到 PO 的 `description` 字段。

##### `SolverTaskProfileMapper.java`

作用：

- 负责 `solver_task_profile` 表的查询、插入、更新和分页。

主要方法：

- `selectById(...)`
- `selectBySolverIdAndProfileCode(...)`
- `insert(...)`
- `updateById(...)`
- `selectPage(...)`
- `count(...)`
- `selectEnabledBySolverId(...)`
- `selectBySolverId(...)`

##### `SolverProfileFileRuleMapper.java`

作用：

- 负责 `solver_profile_file_rule` 表的查询、插入、更新、删除和按模板列表查询。

主要方法：

- `selectById(...)`
- `insert(...)`
- `updateById(...)`
- `deleteById(...)`
- `selectByProfileId(...)`

#### 12.2.3 `repository/`

对应文件夹：

```text
solver-service/src/main/java/com/example/cae/solver/infrastructure/persistence/repository/
```

##### `SolverRepositoryImpl.java`

作用：

- `SolverRepository` 的数据库实现。

职责：

- 调用 `SolverDefinitionMapper`
- 使用 `SolverAssembler` 做领域对象与 PO 转换
- 实现分页、保存、更新、按编码/按 ID 查询

##### `ProfileRepositoryImpl.java`

作用：

- `ProfileRepository` 的数据库实现。

职责：

- 调用 `SolverTaskProfileMapper`
- 使用 `ProfileAssembler` 做转换
- 实现分页、保存、更新、按编码查询、按求解器列出模板

##### `FileRuleRepositoryImpl.java`

作用：

- `FileRuleRepository` 的数据库实现。

职责：

- 调用 `SolverProfileFileRuleMapper`
- 使用 `FileRuleAssembler` 做转换
- 实现规则查询、保存、更新、删除、按模板列出

---

### 12.3 基础设施辅助目录

对应文件夹：

```text
solver-service/src/main/java/com/example/cae/solver/infrastructure/support/
```

#### `ProfileRuleValidator.java`

作用：

- 做文件规则创建/修改时的基础字段校验。

当前方法：

- `validateCreateRule(...)`
- `validateUpdateRule(...)`

特点：

- 它不依赖 Bean Validation 注解结果，而是做一层手工补充校验。

#### `UploadSpecBuilder.java`

作用：

- 真正负责把“模板 + 文件规则”组装成上传规范对象。

关键行为：

- 复制模板基本信息
- 生成 `requiredFiles`
- 生成 `optionalFiles`
- 合并生成 `fileRules`
- 构造 `ArchiveRule`

非常关键的实现细节：

- 当前压缩包规范固定为：
  - `fileKey = input_archive`
  - `allowSuffix = ["zip"]`
  - `maxSizeMb = 2048`

这意味着：

- 上传规范中的压缩包大小限制当前是写死在代码里的，不是动态配置。

#### `CommandTemplateResolver.java`

作用：

- 用于把命令模板中的 `${key}` 占位符替换成实际参数值。

当前状态：

- 逻辑已实现。
- 但当前模块内没有发现它被主流程直接调用。

所以更准确的说法是：

- “已具备命令模板解析工具类”
- 不是“执行流程已经全面接入命令模板解析”

---

## 13. 支撑层

### 13.1 对应文件夹

```text
solver-service/src/main/java/com/example/cae/solver/support/
```

### 13.2 文件说明

#### `SolverQueryBuilder.java`

作用：

- 当前是一个空的 `@Component`。

现状判断：

- 从命名上看，原本预期用于封装求解器查询条件构造逻辑。
- 但当前分页 SQL 直接写在 Mapper 中，因此它仍是预留类。

---

## 14. 与数据库表的对应关系

结合数据库设计，`solver-service` 当前主要对应 3 张表：

### 14.1 `solver_definition`

主要对应代码：

- `domain/model/SolverDefinition.java`
- `infrastructure/persistence/entity/SolverDefinitionPO.java`
- `infrastructure/persistence/mapper/SolverDefinitionMapper.java`
- `infrastructure/persistence/repository/SolverRepositoryImpl.java`

### 14.2 `solver_task_profile`

主要对应代码：

- `domain/model/SolverTaskProfile.java`
- `infrastructure/persistence/entity/SolverTaskProfilePO.java`
- `infrastructure/persistence/mapper/SolverTaskProfileMapper.java`
- `infrastructure/persistence/repository/ProfileRepositoryImpl.java`

### 14.3 `solver_profile_file_rule`

主要对应代码：

- `domain/model/SolverProfileFileRule.java`
- `infrastructure/persistence/entity/SolverProfileFileRulePO.java`
- `infrastructure/persistence/mapper/SolverProfileFileRuleMapper.java`
- `infrastructure/persistence/repository/FileRuleRepositoryImpl.java`

---

## 15. 核心调用链

### 15.1 求解器创建流程

```text
SolverController.createSolver
  -> SolverFacade.createSolver
  -> SolverAppService.createSolver
  -> SolverDomainService.checkSolverCodeUnique
  -> SolverAssembler.toSolver
  -> SolverRepository.save
  -> SolverRepositoryImpl
  -> SolverDefinitionMapper.insert
  -> SolverAssembler.toCreateResponse
```

### 15.2 模板创建流程

```text
ProfileController.createProfile
  -> ProfileFacade.createProfile
  -> ProfileAppService.createProfile
  -> SolverRepository.findById
  -> SolverDefinition.isEnabled
  -> ProfileRuleDomainService.checkProfileCodeUnique
  -> ProfileAssembler.toProfile
  -> ProfileRepository.save
  -> SolverTaskProfileMapper.insert
  -> ProfileAssembler.toCreateResponse
```

### 15.3 文件规则创建流程

```text
FileRuleController.createFileRule
  -> ProfileFacade.createFileRule
  -> FileRuleAppService.createFileRule
  -> ProfileRepository.findById
  -> ProfileRuleValidator.validateCreateRule
  -> FileRuleAssembler.toRule
  -> ProfileRuleDomainService.checkRuleConflict
  -> FileRuleRepository.save
  -> SolverProfileFileRuleMapper.insert
  -> FileRuleAssembler.toCreateResponse
```

### 15.4 上传规范生成流程

```text
ProfileController.getUploadSpec
  -> ProfileFacade.buildUploadSpec
  -> UploadSpecAppService.buildUploadSpec
  -> ProfileRepository.findById
  -> ProfileRuleDomainService.checkProfileEnabled
  -> FileRuleRepository.listByProfileId
  -> UploadSpecBuilder.build
  -> UploadSpecResponse
```

### 15.5 模板内部详情查询流程

```text
InternalProfileController.getProfileDetail
  -> ProfileFacade.getProfileDetail
  -> ProfileAppService.getProfileDetail
  -> ProfileRepository.findById
  -> FileRuleRepository.listByProfileId
  -> ProfileDetailResponse
  -> 再次通过 ProfileFacade.getFileRules
  -> 手动组装 InternalProfileDetailResponse
```

---

## 16. 设计文档与当前实现的对照

### 16.1 已经较好落地的部分

- 求解器定义管理已经实现。
- 模板管理已经实现。
- 模板文件规则管理已经实现。
- 上传规范生成已经实现。
- 内部模板/求解器查询接口已经实现。
- 求解器、模板、文件规则三层模型已经分开。

### 16.2 当前是“简化实现”或“兼容式实现”的部分

- `description` 字段已统一为正式说明字段，不再保留 `remark` 双口径。
- `paramsSchema` / `paramsSchemaJson` 双字段并存，也是兼容式设计。
- 状态更新接口同时支持 `PUT` 和 `POST`。
- 多个响应对象同时返回 `id` 和 `solverId/profileId/ruleId`。

### 16.3 当前是“预留/未完全接入”的部分

- `MybatisPlusConfig.java`
- `SolverServiceConfig.java` 中的扩展配置能力
- `SolverQueryBuilder.java`
- `CommandTemplateResolver.java` 在主执行链路中的接入
- `ProfileRuleDomainService.buildUploadSpec(...)` 的实际使用

### 16.4 当前代码中值得在论文里说明的实现细节

- 上传压缩包规范中的 `maxSizeMb = 2048` 当前写死在 `UploadSpecBuilder`。
- 枚举 `RuleFileTypeEnum` 和 `SolverExecModeEnum` 已定义，但请求侧没有强制按枚举校验。
- `FileRuleAppService.deleteFileRule(...)` 当前不先查存在性。
- `InternalSolverController` 直接复用外部 `SolverDetailResponse`。
- `InternalProfileController` 会二次调用 Facade 获取文件规则并手动组装内部 DTO。

---

## 17. 推荐的阅读顺序

如果是第一次看 `solver-service`，建议按下面顺序：

### 17.1 先看接口定义

1. `interfaces/controller/SolverController.java`
2. `interfaces/controller/ProfileController.java`
3. `interfaces/controller/FileRuleController.java`
4. `interfaces/internal/InternalProfileController.java`
5. `interfaces/internal/InternalSolverController.java`

### 17.2 再看主业务编排

1. `application/facade/SolverFacade.java`
2. `application/service/SolverAppService.java`
3. `application/facade/ProfileFacade.java`
4. `application/service/ProfileAppService.java`
5. `application/service/FileRuleAppService.java`
6. `application/service/UploadSpecAppService.java`

### 17.3 再看规则与模型

1. `domain/model/SolverDefinition.java`
2. `domain/model/SolverTaskProfile.java`
3. `domain/model/SolverProfileFileRule.java`
4. `domain/service/SolverDomainService.java`
5. `domain/service/ProfileRuleDomainService.java`

### 17.4 最后看数据库实现

1. `domain/repository/*`
2. `infrastructure/persistence/repository/*`
3. `infrastructure/persistence/mapper/*`
4. `application/assembler/*`

---

## 18. 当前结论

`solver-service` 当前已经形成了一个结构比较清晰的“求解器配置中心型服务”，它把求解器定义、模板定义、文件规则和上传规范四块能力拆分得比较明确，已经能够稳定支撑前端任务创建阶段和 `task-service` 的任务校验阶段。

从代码结构看，它的分层比 `user-service` 稍复杂，但仍然比较规范：

- 接口层负责暴露求解器、模板、文件规则相关接口
- 应用层负责业务编排与对象组装
- 领域层负责表达模型与规则
- 基础设施层负责数据库访问和上传规范辅助构建

从实现成熟度看，它属于：

- 元数据管理能力已基本完整
- 与任务主链路的协作接口已形成
- 规则校验和配置构造能力已可用
- 部分工具类与配置类仍停留在预留阶段

因此，在论文或设计文档里比较准确的表述应该是：

> `solver-service` 已实现平台求解器定义、任务模板、模板文件规则及上传规范生成等核心能力，作为平台中的规则与元数据中心，为前端任务创建和 task-service 模板校验提供统一配置来源；当前实现重点满足原型平台的可配置性与可扩展性需求，在执行模式约束、动态配置能力和部分工具类接入深度方面仍保留后续增强空间。
