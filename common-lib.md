# common-lib 模块说明文档

## 1. 文档目的

这份文档把 `common-lib` 按“模块根目录 -> 文件夹 -> 具体代码文件 -> 文件职责”的粒度重新整理，目标不是简单列工具类清单，而是说明它作为整个后端“共享契约层”的真实作用。

它主要解决下面几类问题：

- 各服务公共返回体、异常、错误码、枚举、DTO 到底放在哪里；
- `gateway-service`、`user-service`、`task-service`、`scheduler-service`、`node-agent` 分别依赖了 `common-lib` 的哪些内容；
- 哪些类是“真正稳定的公共契约”，哪些只是当前阶段的简化实现；
- 写论文或模块设计说明时，如何准确描述“公共基础模块”的边界和价值。

分析范围以 `common-lib/src/main/java` 和 `common-lib/pom.xml` 为主。

---

## 2. 模块定位

`common-lib` 不是一个业务服务，也不是独立可运行程序，而是整个后端的“共享基础库”。

它承担的核心职责包括：

1. 统一 API 返回格式；
2. 统一异常体系与错误码口径；
3. 统一全局枚举定义；
4. 提供跨服务传输用 DTO；
5. 提供少量通用工具类；
6. 提供可被各服务复用的基础配置占位。

从架构上看，它的定位更准确地说是：

> 所有服务共同依赖的“共享契约层 + 基础支撑层”。

如果没有它，当前系统会迅速出现这些问题：

- 各服务返回格式不一致；
- 相同错误含义在不同服务里用不同 code；
- 任务状态、节点状态、失败类型口径不一致；
- 服务间调用的请求/响应对象各自定义、相互漂移；
- JWT、JSON、ID 生成、文件名处理等基础能力重复实现。

因此，`common-lib` 在系统中的作用不是“方便”，而是“统一”。

---

## 3. 模块根目录与源码入口

### 3.1 模块根目录

模块根目录是：

```text
common-lib/
```

最重要的入口位置包括：

- `common-lib/pom.xml`
- `common-lib/src/main/java/com/example/cae/common/`

### 3.2 模块类型说明

`common-lib` 是一个库模块，不是可运行服务，因此它和其他服务有一个本质不同点：

- 没有 `Application.java`
- 没有自己的 `application.yml`
- 没有控制器、应用服务、仓储、数据库

所以它的重点不是“请求处理流程”，而是“共享定义与公共能力”。

### 3.3 不需要作为设计分析重点的目录

```text
common-lib/target/
```

`target/` 仅是编译产物，不应写进源码设计结构。

---

## 4. 分层与文件夹映射

`common-lib` 虽然不是典型微服务，但仍可以按职责做出清晰分层。

| 分层 | 对应文件夹 | 作用 |
| --- | --- | --- |
| 根配置层 | `common-lib/` | Maven 依赖与模块打包配置 |
| 共享响应层 | `common-lib/src/main/java/com/example/cae/common/response/` | 统一 API 返回体和分页返回体 |
| 共享异常层 | `common-lib/src/main/java/com/example/cae/common/exception/` | 统一异常类型与全局异常处理 |
| 常量层 | `common-lib/src/main/java/com/example/cae/common/constant/` | 统一错误码、Header 名、权限常量、系统默认值 |
| 枚举层 | `common-lib/src/main/java/com/example/cae/common/enums/` | 统一任务状态、节点状态、文件角色等口径 |
| DTO 层 | `common-lib/src/main/java/com/example/cae/common/dto/` | 服务间传输的共享数据对象 |
| 共享模型层 | `common-lib/src/main/java/com/example/cae/common/model/` | 通用分页基类、登录用户模型 |
| 工具类层 | `common-lib/src/main/java/com/example/cae/common/utils/` | 通用 JSON、JWT、ID、Bean 拷贝等工具 |
| 共享配置层 | `common-lib/src/main/java/com/example/cae/common/config/` | 预留的公共 Jackson/MVC 配置 |

如果只想快速定位代码，可以这样记：

- 看统一返回格式：`response/`
- 看统一错误处理：`exception/`
- 看系统级错误码和 Header：`constant/`
- 看任务、节点、角色这些跨服务口径：`enums/`
- 看服务间传输对象：`dto/`
- 看基础工具：`utils/`

---

## 5. 模块级结构总览

当前源码结构如下：

```text
common-lib/
├── pom.xml
└── src/main/java/com/example/cae/common/
    ├── config/
    │   ├── JacksonConfig.java
    │   └── WebMvcCommonConfig.java
    ├── constant/
    │   ├── ErrorCodeConstants.java
    │   ├── HeaderConstants.java
    │   ├── SecurityConstants.java
    │   ├── SystemConstants.java
    │   └── TaskConstants.java
    ├── dto/
    │   ├── FileRuleDTO.java
    │   ├── NodeDTO.java
    │   ├── SolverDTO.java
    │   ├── SolverProfileDTO.java
    │   ├── TaskDTO.java
    │   ├── TaskFileDTO.java
    │   ├── TaskResultFileDTO.java
    │   ├── TaskResultSummaryDTO.java
    │   └── UserContextDTO.java
    ├── enums/
    │   ├── FailTypeEnum.java
    │   ├── FileRoleEnum.java
    │   ├── NodeStatusEnum.java
    │   ├── OperatorTypeEnum.java
    │   ├── ResultFileTypeEnum.java
    │   ├── RoleCodeEnum.java
    │   └── TaskStatusEnum.java
    ├── exception/
    │   ├── BizException.java
    │   ├── ForbiddenException.java
    │   ├── GlobalExceptionHandler.java
    │   ├── NotFoundException.java
    │   └── UnauthorizedException.java
    ├── model/
    │   ├── BasePageQuery.java
    │   └── LoginUser.java
    ├── response/
    │   ├── PageResult.java
    │   └── Result.java
    └── utils/
        ├── BeanCopyUtil.java
        ├── DateTimeUtil.java
        ├── FileNameUtil.java
        ├── IdGenerator.java
        ├── JsonUtil.java
        └── JwtUtil.java
```

这个结构说明 `common-lib` 的核心并不是“很多代码”，而是“为全系统提供稳定、统一、可复用的公共定义”。

---

## 6. 根目录文件说明

### 6.1 `common-lib/pom.xml`

模块的 Maven 描述文件，作用包括：

- 声明当前模块是 `common-lib`
- 继承父工程 `cae-taskmanager-backend`
- 引入 `spring-boot-starter-validation`
- 引入 `jackson-databind`
- 引入 `spring-web`、`spring-context`、`spring-beans`
- 可选引入 `lombok`

从依赖上可以看出：

- 这个模块既不是纯 Java 工具包，也不是重框架模块；
- 它依赖少量 Spring 和 Jackson 能力，目的是让共享异常处理、配置类、公共工具能够被其他服务直接复用。

---

## 7. 共享响应层

### 7.1 对应文件夹

对应文件夹：

```text
common-lib/src/main/java/com/example/cae/common/response/
```

### 7.2 文件说明

#### `Result.java`

全系统统一返回体。

字段包括：

- `code`
- `message`
- `data`

并提供静态工厂方法：

- `success()`
- `success(data)`
- `fail(code, message)`
- `fail(code, message, data)`

当前全平台的控制器几乎都使用它作为最外层返回结构，所以它实际上定义了整个系统的 API 包装协议。

#### `PageResult.java`

统一分页返回体。

字段包括：

- `total`
- `pageNum`
- `pageSize`
- `records`

它通过静态方法 `of(...)` 创建实例，当前主要用于各服务的分页查询接口。

---

## 8. 共享异常层

### 8.1 对应文件夹

对应文件夹：

```text
common-lib/src/main/java/com/example/cae/common/exception/
```

### 8.2 文件说明

#### `BizException.java`

业务异常基类。

核心特点：

- 继承 `RuntimeException`
- 可携带业务错误码 `code`
- 可携带附加数据 `data`

这是各服务最常用的异常类型，用来表达“这是业务预期内的失败，而不是系统崩溃”。

#### `UnauthorizedException.java`

未认证异常，对应 HTTP 语义上的 401。

#### `ForbiddenException.java`

无权限异常，对应 HTTP 语义上的 403。

#### `NotFoundException.java`

资源不存在异常，对应 HTTP 语义上的 404。

#### `GlobalExceptionHandler.java`

全局异常处理器，是 `common-lib` 中最具“框架能力”的类之一。

它统一处理：

- `BizException`
- `UnauthorizedException`
- `ForbiddenException`
- `NotFoundException`
- `BindException`
- `ConstraintViolationException`
- `MethodArgumentTypeMismatchException`
- `HttpMessageNotReadableException`
- 兜底 `Exception`

并统一转换成 `Result.fail(...)`。

这意味着只要服务扫描到了 `com.example.cae` 包，公共异常处理能力就能直接生效。

---

## 9. 常量层

### 9.1 对应文件夹

对应文件夹：

```text
common-lib/src/main/java/com/example/cae/common/constant/
```

### 9.2 文件说明

#### `ErrorCodeConstants.java`

系统统一错误码定义类。

它包含几类错误码：

- 通用 HTTP 风格错误码：`400/401/403/404/409/502`
- 任务相关错误码：`4001` 开始
- 登录和用户相关错误码：`4101` 开始
- 求解器与模板相关错误码：`4201` 开始
- 节点 token 与节点鉴权相关错误码：`4301` 开始
- 各类“找不到”错误码：`4401` 开始
- 调度与节点执行协作错误码：`4501` 开始
- 节点代理空响应等错误码：`5501`

它的意义不是“多几个常量”，而是统一全系统的错误表达口径。

#### `HeaderConstants.java`

统一 Header 名常量，包含：

- `Authorization`
- `X-User-Id`
- `X-Role-Code`
- `X-Node-Token`
- `X-Trace-Id`

这使网关、调度器、任务服务、节点代理之间的透传协议保持一致。

#### `SecurityConstants.java`

安全相关常量，包含：

- `TOKEN_PREFIX = "Bearer "`
- `ROLE_ADMIN`
- `ROLE_USER`

主要用于网关和用户服务的认证授权链路。

#### `SystemConstants.java`

系统通用默认值，当前包含：

- 默认页码
- 默认页大小

#### `TaskConstants.java`

任务域默认值，当前包含：

- 默认优先级
- 默认超时时间
- 默认日志分页大小

---

## 10. 枚举层

### 10.1 对应文件夹

对应文件夹：

```text
common-lib/src/main/java/com/example/cae/common/enums/
```

### 10.2 文件说明

#### `TaskStatusEnum.java`

全局任务状态枚举，定义：

- `CREATED`
- `VALIDATED`
- `QUEUED`
- `SCHEDULED`
- `DISPATCHED`
- `RUNNING`
- `SUCCESS`
- `FAILED`
- `CANCELED`
- `TIMEOUT`

并提供：

- `isFinished()`
- `canCancel()`

这基本定义了整个平台任务生命周期状态机的公共语言。

#### `FailTypeEnum.java`

统一失败类型枚举，定义：

- `VALIDATION_ERROR`
- `DISPATCH_ERROR`
- `NODE_OFFLINE`
- `EXECUTOR_START_ERROR`
- `RUNTIME_ERROR`
- `TIMEOUT`
- `MANUAL_CANCEL`

主要被 `task-service`、`scheduler-service`、`node-agent` 共同使用。

#### `NodeStatusEnum.java`

节点在线状态枚举：

- `ONLINE`
- `OFFLINE`

#### `RoleCodeEnum.java`

角色枚举：

- `ADMIN`
- `USER`

#### `OperatorTypeEnum.java`

操作人类型枚举：

- `SYSTEM`
- `USER`
- `NODE`
- `ADMIN`

它被任务状态历史记录和操作审计场景广泛使用。

#### `FileRoleEnum.java`

任务文件角色枚举：

- `INPUT`
- `CONFIG`
- `ARCHIVE`

#### `ResultFileTypeEnum.java`

结果文件类型枚举：

- `RESULT`
- `LOG`
- `REPORT`
- `IMAGE`

---

## 11. DTO 层

### 11.1 对应文件夹

对应文件夹：

```text
common-lib/src/main/java/com/example/cae/common/dto/
```

这里放的不是前端接口请求响应对象，而是“跨服务协作时复用的共享传输对象”。

### 11.2 文件说明

#### `UserContextDTO.java`

用户上下文 DTO，字段包括：

- `userId`
- `username`
- `roleCode`

主要用于：

- 网关解析 token 后透传用户身份
- 服务间共享用户上下文

#### `TaskDTO.java`

最核心的跨服务 DTO 之一。

它承载的内容已经明显超过“任务基础信息”，包含：

- 任务 ID、编号、名称
- 求解器 ID、求解器编码
- 模板 ID、任务类型
- 命令模板、解析器名、超时
- 优先级
- 参数 JSON 和参数 Map
- 输入文件列表
- 绑定节点 ID
- 提交时间

它在当前系统中的典型作用是：

- `task-service` 把待调度任务打包成 `TaskDTO`
- `scheduler-service` 根据它选节点
- `scheduler-service` 再把其中的执行相关字段透传给 `node-agent`

因此它实际上是“调度与执行协同 DTO”。

#### `TaskFileDTO.java`

任务文件 DTO，字段包括：

- `taskId`
- `fileKey`
- `originName`
- `storagePath`
- `unpackDir`
- `relativePath`
- `archiveFlag`
- `fileSize`

用于在任务服务和节点代理之间描述输入文件。

#### `TaskResultSummaryDTO.java`

任务结果摘要 DTO，字段包括：

- `taskId`
- `success`
- `durationSeconds`
- `summaryText`
- `metricsJson`

它适合在任务服务内部或跨服务场景中描述执行结果摘要。

#### `TaskResultFileDTO.java`

任务结果文件 DTO，字段包括：

- `taskId`
- `fileType`
- `fileName`
- `storagePath`
- `fileSize`

#### `NodeDTO.java`

节点 DTO，字段包括：

- 节点基础信息
- 状态
- 启用状态
- 并发
- CPU/内存
- 运行数
- 支持求解器列表

它表达的是一个“可用于跨服务传输的节点摘要”。

#### `SolverDTO.java`

求解器定义 DTO，字段包括：

- `solverId`
- `solverCode`
- `solverName`
- `version`
- `execMode`
- `execPath`
- `enabled`

#### `SolverProfileDTO.java`

求解器模板 DTO，字段包括：

- `profileId`
- `solverId`
- `profileCode`
- `taskType`
- `profileName`
- `commandTemplate`
- `paramsSchemaJson`
- `parserName`
- `timeoutSeconds`
- `description`
- `enabled`

#### `FileRuleDTO.java`

文件规则 DTO，字段包括：

- `ruleId`
- `profileId`
- `fileKey`
- `pathPattern`
- `fileNamePattern`
- `fileType`
- `requiredFlag`
- `sortOrder`
- `ruleJson`
- `remark`

---

## 12. 共享模型层

### 12.1 对应文件夹

对应文件夹：

```text
common-lib/src/main/java/com/example/cae/common/model/
```

### 12.2 文件说明

#### `BasePageQuery.java`

通用分页基类，提供：

- `pageNum`
- `pageSize`

当前默认值分别是 `1` 和 `10`。

它适合被通用分页请求对象继承，但当前各服务里更多还是各自定义独立查询对象，因此它更偏“公共基础模型预留”。

#### `LoginUser.java`

登录用户模型，字段包括：

- `userId`
- `username`
- `roleCode`

和 `UserContextDTO` 很接近，但语义更偏“服务内部已登录用户视图”。

---

## 13. 工具类层

### 13.1 对应文件夹

对应文件夹：

```text
common-lib/src/main/java/com/example/cae/common/utils/
```

### 13.2 文件说明

#### `JwtUtil.java`

当前项目中的 token 工具类。

它提供：

- `generateToken(userId, roleCode)`
- `parseUserContext(token)`
- `parseUserId(token)`
- `parseRoleCode(token)`
- `validateToken(token)`

但必须强调一个非常关键的实现事实：

> 当前它并不是真正的 JWT，而是把 `userId:roleCode` 做了 Base64 编码。

也就是说：

- 没有签名
- 没有过期时间
- 没有标准 JWT Header/Payload/Signature 结构

这对毕设原型是够用的，但文档里不能把它写成“完整 JWT 安全实现”。

#### `JsonUtil.java`

共享 JSON 工具类，基于静态 `ObjectMapper` 提供：

- `toJson(Object obj)`
- `fromJson(String text, Class<T> clazz)`

供各服务在参数序列化、结果指标序列化等场景中复用。

#### `IdGenerator.java`

ID/编号生成工具，提供：

- `nextTaskNo()`：生成形如 `TASK-XXXXXXXXXXXX` 的任务编号
- `nextBizId()`：生成业务 UUID

#### `FileNameUtil.java`

文件名后缀工具，提供：

- `getSuffix(String fileName)`

#### `DateTimeUtil.java`

时间格式化工具，提供：

- `format(LocalDateTime dateTime)`

当前统一格式为 `yyyy-MM-dd HH:mm:ss`。

#### `BeanCopyUtil.java`

对象属性拷贝工具，内部使用 Spring `BeanUtils.copyProperties(...)`。

提供：

- `copy(source, targetClass)`

适合做简单 DTO/VO 转换，但复杂对象映射仍建议在各服务自己的 Assembler 里完成。

---

## 14. 共享配置层

### 14.1 对应文件夹

对应文件夹：

```text
common-lib/src/main/java/com/example/cae/common/config/
```

### 14.2 文件说明

#### `JacksonConfig.java`

当前是一个空的占位类，注释说明它预留给共享 Jackson 配置。

意味着架构上已经留好了“统一 JSON 序列化策略”的位置，但目前还没真正放入全局配置。

#### `WebMvcCommonConfig.java`

当前是一个空的 `@Configuration` 类。

它表明架构上预留了“公共 MVC 配置”的位置，但当前还没有放具体拦截器、转换器或参数解析器。

---

## 15. common-lib 与各服务的关系

`common-lib` 的价值主要不是“它自己做了什么”，而是“它让所有服务之间如何统一”。

### 15.1 与 `gateway-service` 的关系

`gateway-service` 典型依赖：

- `HeaderConstants`
- `SecurityConstants`
- `Result`
- `UnauthorizedException`
- `ForbiddenException`
- `JwtUtil`
- `UserContextDTO`

它主要使用 `common-lib` 来统一网关鉴权和用户上下文透传协议。

### 15.2 与 `user-service` 的关系

`user-service` 典型依赖：

- `Result`
- `PageResult`
- `BizException`
- `RoleCodeEnum`
- `ErrorCodeConstants`
- `JwtUtil`

它主要使用 `common-lib` 来统一登录和用户管理接口的返回体与错误口径。

### 15.3 与 `solver-service` 的关系

`solver-service` 典型依赖：

- `Result`
- `PageResult`
- `BizException`
- `FileRuleDTO`
- `SolverDTO`
- `SolverProfileDTO`
- `FileRoleEnum`
- `TaskConstants`

它主要使用 `common-lib` 来统一求解器、模板、文件规则的共享描述。

### 15.4 与 `task-service` 的关系

`task-service` 是 `common-lib` 依赖最重的服务之一，典型依赖：

- `TaskStatusEnum`
- `FailTypeEnum`
- `OperatorTypeEnum`
- `ResultFileTypeEnum`
- `TaskDTO`
- `TaskFileDTO`
- `TaskResultSummaryDTO`
- `TaskResultFileDTO`
- `Result`
- `PageResult`
- `ErrorCodeConstants`

它主要依赖 `common-lib` 统一任务状态机、失败类型和跨服务调度传输对象。

### 15.5 与 `scheduler-service` 的关系

`scheduler-service` 典型依赖：

- `TaskDTO`
- `NodeStatusEnum`
- `FailTypeEnum`
- `HeaderConstants`
- `ErrorCodeConstants`
- `Result`
- `PageResult`

它主要使用 `common-lib` 来统一任务调度输入对象、节点状态口径和跨服务回写协议。

### 15.6 与 `node-agent` 的关系

`node-agent` 典型依赖：

- `Result`
- `HeaderConstants`
- `FailTypeEnum`
- `BizException`
- `TaskDTO`
- `TaskFileDTO`

它主要使用 `common-lib` 来统一回传协议、节点 token Header 口径和失败类型表达。

---

## 16. 设计文档与当前实现的对照

### 16.1 已经较好落地的部分

当前 `common-lib` 已经较好完成了这些目标：

- 统一响应结构；
- 统一全局异常处理；
- 统一任务、节点、角色、操作人等全局枚举；
- 统一错误码口径；
- 提供调度和执行链路所需的共享 DTO；
- 提供基础 JSON、ID、Bean 拷贝等通用工具；
- 所有服务都通过 Maven 依赖复用它，而不是复制粘贴。

### 16.2 当前是“基础共享层”而不是“成熟基础框架”的部分

当前实现仍然偏基础版：

- `JwtUtil` 不是标准 JWT 实现；
- `JacksonConfig` 和 `WebMvcCommonConfig` 还是空占位；
- 没有统一的日志规范、Trace 工具、审计工具；
- 没有统一的时间序列化策略真正落地；
- 没有统一的 OpenAPI/接口文档约定；
- 没有更细的错误码分层治理机制。

### 16.3 当前实现里几个必须写清楚的细节

#### 1. `common-lib` 是共享契约，不是“大杂烩工具箱”

真正应该放进来的，是：

- 跨服务稳定复用的对象
- 全局统一的枚举、错误码、Header
- 所有服务都可能依赖的工具和基础异常

不应该放进来的，是：

- 任一单服务的业务实体
- 数据库 PO
- 某服务独有的 Controller 请求响应对象
- 与具体业务强绑定的领域逻辑

#### 2. `JwtUtil` 目前只是“仿 JWT 的简化 token”

这是文档里必须写清楚的实现现实。  
如果答辩时直接说“系统采用 JWT”，容易被追问签名、过期、刷新、撤销等机制，而当前实现并不具备。

#### 3. DTO 只放跨服务对象，不放前端专用 VO

这一点当前整体做得还比较克制，值得在文档里明确写成设计原则。

---

## 17. 推荐的阅读顺序

### 17.1 先看共享契约

建议先看：

- `Result.java`
- `PageResult.java`
- `ErrorCodeConstants.java`
- `HeaderConstants.java`
- `TaskStatusEnum.java`
- `FailTypeEnum.java`

这样先建立整个系统共同语言。

### 17.2 再看跨服务 DTO

然后看：

- `TaskDTO.java`
- `TaskFileDTO.java`
- `NodeDTO.java`
- `SolverDTO.java`
- `SolverProfileDTO.java`

这样能理解服务之间是怎样传数据的。

### 17.3 再看异常与工具

最后看：

- `BizException.java`
- `GlobalExceptionHandler.java`
- `JwtUtil.java`
- `JsonUtil.java`
- `IdGenerator.java`

这样就能理解公共基础层如何真正支撑各服务。

---

## 18. 当前结论

`common-lib` 虽然不是最复杂的模块，但它对整个后端架构非常关键。它的真实价值不在于“代码量”，而在于：

- 统一接口返回口径；
- 统一异常和错误码口径；
- 统一任务、节点、角色等核心状态语义；
- 统一服务间 DTO；
- 降低各服务之间的重复定义和语义漂移。

从当前实现看，它已经足以支撑整个后端原型阶段的共享基础需求。  
但如果后续要进一步工程化，最值得优先增强的方向是：

- 把 `JwtUtil` 升级为真正的标准 JWT 实现；
- 落地统一的 Jackson/MVC 公共配置；
- 继续治理错误码和公共 DTO 边界；
- 增加更强的日志、追踪、审计类公共能力。
