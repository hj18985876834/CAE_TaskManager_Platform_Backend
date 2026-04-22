# gateway-service 模块说明文档

## 1. 文档目的

这份文档将 `gateway-service` 按“模块根目录 -> 分层目录 -> 具体代码文件 -> 文件职责”的颗粒度重新整理，目标是让网关模块的设计说明不再停留在“统一入口”这种概念层面，而是能直接定位到真实代码。

它主要解决下面几类问题：

- 网关的启动入口、配置层、过滤器层、支撑层分别在哪个文件夹；
- 每个代码文件具体承担什么职责；
- 请求从进入网关到转发给下游服务，实际经过了哪些步骤；
- 白名单、管理员路径控制、用户上下文透传、异常处理分别落在哪些类；
- 设计文档中“统一入口、统一鉴权、统一转发”的目标，当前代码具体实现到了什么程度。

分析范围以 `gateway-service/src/main/java`、`gateway-service/src/main/resources` 和 `gateway-service/pom.xml` 为主。

---

## 2. 模块定位

`gateway-service` 是整个平台的统一入口服务，位于前端与各个后端业务服务之间。

它在当前系统中的核心职责有四类：

1. 统一接收外部请求；
2. 按路径把请求转发到对应后端服务；
3. 在网关层完成基础 token 校验和粗粒度权限控制；
4. 统一记录请求日志、补充追踪头、收口安全异常响应。

它明确不承担这些职责：

- 不直接处理业务数据；
- 不直接访问数据库；
- 不做任务调度；
- 不做节点管理；
- 不做求解器管理；
- 不做任务生命周期流转。

因此，`gateway-service` 的准确定位是：

> 统一入口层 + 基础安全前置层，而不是业务服务层。

---

## 3. 模块根目录与源码入口

### 3.1 模块根目录

模块根目录是：

```text
gateway-service/
```

当前最重要的入口位置包括：

- `gateway-service/pom.xml`
- `gateway-service/src/main/java/com/example/cae/gateway/`
- `gateway-service/src/main/resources/application.yml`

### 3.2 不需要作为设计分析重点的目录

```text
gateway-service/target/
```

`target/` 是编译产物，不属于源码设计主体。

---

## 4. 分层与文件夹映射

| 分层 | 对应文件夹 | 作用 |
| --- | --- | --- |
| 启动入口层 | `gateway-service/src/main/java/com/example/cae/gateway/` | Spring Boot 启动入口 |
| 配置层 | `gateway-service/src/main/java/com/example/cae/gateway/config/` | 路由、CORS、Bean、白名单配置 |
| 过滤器层 | `gateway-service/src/main/java/com/example/cae/gateway/filter/` | TraceId、请求日志、JWT 鉴权过滤链 |
| 异常处理层 | `gateway-service/src/main/java/com/example/cae/gateway/handler/` | 网关层异常统一响应 |
| 属性配置层 | `gateway-service/src/main/java/com/example/cae/gateway/properties/` | 路由地址和安全路径配置绑定 |
| 路由支撑层 | `gateway-service/src/main/java/com/example/cae/gateway/router/` | 预留路由加载扩展点 |
| 支撑层 | `gateway-service/src/main/java/com/example/cae/gateway/support/` | Token 解析、路径匹配、请求变更等辅助组件 |
| 资源配置层 | `gateway-service/src/main/resources/` | 端口、路由地址、白名单和管理员路径配置 |

如果只想快速找代码，可以这样记：

- 看转发规则：`config/GatewayRouteConfig.java`
- 看鉴权逻辑：`filter/JwtAuthFilter.java`
- 看日志与 trace：`filter/TraceIdFilter.java`、`filter/RequestLogFilter.java`
- 看路径匹配和 token 解析：`support/`
- 看配置绑定：`properties/`
- 看网关异常收口：`handler/GatewayExceptionHandler.java`

---

## 5. 模块级结构总览

当前源码结构如下：

```text
gateway-service/
├── pom.xml
├── src/main/java/com/example/cae/gateway/
│   ├── GatewayApplication.java
│   ├── config/
│   │   ├── CorsConfig.java
│   │   ├── GatewayBeanConfig.java
│   │   ├── GatewayRouteConfig.java
│   │   └── WhiteListConfig.java
│   ├── filter/
│   │   ├── JwtAuthFilter.java
│   │   ├── RequestLogFilter.java
│   │   └── TraceIdFilter.java
│   ├── handler/
│   │   └── GatewayExceptionHandler.java
│   ├── properties/
│   │   ├── GatewayRouteProperties.java
│   │   └── GatewaySecurityProperties.java
│   ├── router/
│   │   └── RouteDefinitionLoader.java
│   └── support/
│       ├── GatewayRequestMutator.java
│       ├── GatewayResponseWriter.java
│       ├── PathMatcherSupport.java
│       └── TokenParser.java
└── src/main/resources/application.yml
```

和其他服务相比，`gateway-service` 的结构明显更轻量：

- 没有 `interfaces/`
- 没有 `application/`
- 没有 `domain/`
- 没有 `infrastructure/persistence/`

因为它的重点不是业务处理，而是“请求入口控制”。

---

## 6. 根目录文件说明

### 6.1 `gateway-service/pom.xml`

模块的 Maven 描述文件，作用包括：

- 声明当前模块是 `gateway-service`
- 继承父工程 `cae-taskmanager-backend`
- 引入 `spring-cloud-starter-gateway`
- 引入共享基础模块 `common-lib`
- 配置 `spring-boot-maven-plugin`

这说明网关模块的技术核心是 Spring Cloud Gateway，而公共 Header、异常、JWT 工具等则来自 `common-lib`。

### 6.2 `gateway-service/src/main/resources/application.yml`

网关运行配置文件，定义了：

- 服务端口：默认 `8080`
- 服务名：`gateway-service`
- Gateway 最大内存缓冲：`10MB`
- `spring.cloud.gateway.discovery.locator.enabled = true`
- 各业务服务基础地址：
  - `user-service-uri`
  - `solver-service-uri`
  - `scheduler-service-uri`
  - `task-service-uri`
- 安全配置：
  - `white-list`
  - `admin-only-paths`
  - `admin-write-paths`

虽然配置里打开了 `discovery.locator.enabled`，但当前真正使用的还是静态 URI 路由，不是服务发现驱动的动态路由。

---

## 7. 启动入口层

### 7.1 对应文件夹

对应文件夹：

```text
gateway-service/src/main/java/com/example/cae/gateway/
```

### 7.2 文件说明

#### `GatewayApplication.java`

Spring Boot 启动入口类。

作用很纯粹：

- 启动网关服务；
- 扫描网关的配置类、过滤器、处理器和属性配置类。

它本身不承载业务逻辑。

---

## 8. 配置层

### 8.1 对应文件夹

对应文件夹：

```text
gateway-service/src/main/java/com/example/cae/gateway/config/
```

### 8.2 文件说明

#### `GatewayRouteConfig.java`

网关最核心的配置类之一，负责声明实际的路由规则。

当前定义的路由映射关系如下：

- `user-service`
  - `/api/auth/**`
  - `/api/users/**`
- `solver-service`
  - `/api/solvers/**`
  - `/api/profiles/**`
  - `/api/file-rules/**`
- `scheduler-service`
  - `/api/node-agent/**`
  - `/api/nodes/**`
  - `/api/schedules/**`
- `task-service`
  - `/api/tasks/**`
  - `/api/admin/tasks/**`
  - `/api/admin/dashboard/**`

这里有一个很关键的实现细节：

- `/api/tasks/**` 统一走 `task-service`
- 单任务调度记录查询 `/api/tasks/{taskId}/schedules` 也由 `task-service` 承担权限校验后再访问调度记录

这说明网关不再把任务归属敏感接口直接路由到 `scheduler-service`，避免绕过任务归属或管理员权限边界。

#### `CorsConfig.java`

全局 CORS 配置。

当前策略是：

- 允许所有来源
- 允许所有 Header
- 允许所有方法
- 允许携带凭证

这对联调和毕设原型比较友好，但在生产环境并不算严格策略。

#### `GatewayBeanConfig.java`

网关基础 Bean 配置。

当前只注册了一个：

- `AntPathMatcher`

这个 Bean 主要供 `PathMatcherSupport` 使用。

#### `WhiteListConfig.java`

白名单配置封装类。

它做了两件事：

1. 定义默认白名单：
   - `/api/auth/login`
   - `/api/auth/logout`
   - `/api/node-agent/register`
   - `/api/node-agent/heartbeat`
   - `/actuator/health`
2. 将默认白名单与配置文件中的白名单合并去重

这意味着：

- 用户登录和节点注册/心跳无需 token
- 其它业务请求默认都要进入鉴权链

---

## 9. 过滤器层

### 9.1 对应文件夹

对应文件夹：

```text
gateway-service/src/main/java/com/example/cae/gateway/filter/
```

网关的真实请求主链就在这里。

### 9.2 文件说明

#### `TraceIdFilter.java`

全局追踪 ID 过滤器。

它的职责是：

- 为每个进入网关的请求生成一个 traceId
- 写入请求头 `X-Trace-Id`
- 再继续传给下游服务

当前执行顺序：

- `getOrder() = -300`

说明它是整个过滤链中最早执行的一批过滤器之一。

#### `RequestLogFilter.java`

请求日志过滤器。

它会记录：

- 请求方法
- 请求路径
- traceId
- 请求耗时

输出日志格式由 `buildAccessLog(...)` 统一构造。

当前执行顺序：

- `getOrder() = -200`

所以它会在 TraceId 已补充后执行，并能拿到 traceId。

#### `JwtAuthFilter.java`

网关最核心的安全过滤器。

它负责：

1. 判断当前路径是否在白名单中；
2. 如果不在白名单，则从 `Authorization` 头中提取 token；
3. 调 `JwtUtil.validateToken(token)` 校验 token；
4. 解析 `userId` 与 `roleCode`；
5. 对管理员路径做粗粒度角色判断；
6. 将用户上下文透传到下游请求头；
7. 再放行给后续链路与最终路由。

当前执行顺序：

- `getOrder() = -100`

说明它在 trace 和日志基础能力之后执行，但在真正路由转发前完成鉴权。

它内部有两类默认管理员路径：

1. `DEFAULT_ADMIN_ONLY_PATHS`
   - `/api/admin/**`
   - `/api/users/**`
   - `/api/nodes/**`
   - `/api/schedules/**`
2. `DEFAULT_ADMIN_WRITE_PATHS`
   - `/api/solvers/**`
   - `/api/profiles/**`
   - `/api/file-rules/**`

判定逻辑是：

- 命中 `admin-only` 路径，必须是 `ADMIN`
- 命中 `admin-write` 路径，且方法是 `POST/PUT/DELETE`，必须是 `ADMIN`

这体现了当前网关的权限模型：

- 用户级鉴权前置在网关
- 更细粒度的业务权限仍应由下游服务继续控制

---

## 10. 异常处理层

### 10.1 对应文件夹

对应文件夹：

```text
gateway-service/src/main/java/com/example/cae/gateway/handler/
```

### 10.2 文件说明

#### `GatewayExceptionHandler.java`

网关层统一异常处理器，实现了 `ErrorWebExceptionHandler`。

它负责将网关过滤链抛出的异常统一包装成 JSON：

- `UnauthorizedException` -> 401
- `ForbiddenException` -> 403
- 其它异常 -> 500

返回体仍保持 `Result.fail(...)` 的统一格式。

这保证了：

- 网关侧错误和业务服务错误风格一致；
- 前端不会拿到风格完全不同的默认 WebFlux 错误页面。

---

## 11. 属性与支撑层

### 11.1 属性配置目录

对应文件夹：

```text
gateway-service/src/main/java/com/example/cae/gateway/properties/
```

#### `GatewayRouteProperties.java`

绑定前缀 `gateway.routes` 的配置类。

当前维护四个下游服务基础地址：

- `userServiceUri`
- `solverServiceUri`
- `schedulerServiceUri`
- `taskServiceUri`

所有静态路由最终都从这里读取目标地址。

#### `GatewaySecurityProperties.java`

绑定前缀 `gateway.security` 的配置类。

负责接收：

- `whiteList`
- `adminOnlyPaths`
- `adminWritePaths`

它让白名单和管理员路径规则可以通过配置文件扩展，而不是完全写死在代码里。

### 11.2 路由支撑目录

对应文件夹：

```text
gateway-service/src/main/java/com/example/cae/gateway/router/
```

#### `RouteDefinitionLoader.java`

当前是空实现的预留类。

从命名看，它原本适合承担：

- 动态加载路由定义
- 从配置中心或数据库加载路由

但在当前版本中，真正的路由规则还是直接写在 `GatewayRouteConfig` 里。

### 11.3 支撑目录

对应文件夹：

```text
gateway-service/src/main/java/com/example/cae/gateway/support/
```

#### `TokenParser.java`

Token 提取工具。

它负责：

- 从 `Authorization` 头取值
- 判断是否以 `Bearer ` 开头
- 截掉前缀后返回 token 正文

这个类让 `JwtAuthFilter` 不必自己处理头解析细节。

#### `PathMatcherSupport.java`

路径匹配工具，内部基于 `AntPathMatcher`，提供：

- `matchAny(String path, List<String> patterns)`

当前白名单判断、管理员路径判断都依赖它。

#### `GatewayRequestMutator.java`

请求变更工具。

它当前最重要的能力是：

- 把 `userId` 和 `roleCode` 写入请求头

也就是：

- `X-User-Id`
- `X-Role-Code`

这使下游服务能够直接拿到用户身份上下文，而不必重复解析 token。

#### `GatewayResponseWriter.java`

当前是空实现的预留类。

从命名上看，它适合将来承担：

- 统一响应改写
- 网关层特殊响应生成
- 某些错误返回包装

但当前版本尚未实际使用。

---

## 12. 与下游服务的路由关系

网关模块的核心之一就是“把哪些路径转发到哪个服务”。

### 12.1 路由到 `user-service`

路径包括：

- `/api/auth/**`
- `/api/users/**`

意义：

- 用户登录登出
- 用户管理

### 12.2 路由到 `solver-service`

路径包括：

- `/api/solvers/**`
- `/api/profiles/**`
- `/api/file-rules/**`

意义：

- 求解器定义管理
- 模板管理
- 文件规则管理

### 12.3 路由到 `scheduler-service`

路径包括：

- `/api/node-agent/**`
- `/api/nodes/**`
- `/api/schedules/**`

意义：

- 节点接入
- 节点管理
- 调度记录查询
- 某任务调度记录查询

### 12.4 路由到 `task-service`

路径包括：

- `/api/tasks/**`
- `/api/admin/tasks/**`
- `/api/admin/dashboard/**`

意义：

- 任务创建、校验、提交、查询
- 管理员任务操作
- 仪表盘统计

---

## 13. 核心请求调用链

### 13.1 普通受保护请求链路

典型请求链如下：

1. 请求进入网关
2. `TraceIdFilter` 生成 traceId 并写入请求头
3. `RequestLogFilter` 开始计时
4. `JwtAuthFilter` 判断是否白名单
5. 若不是白名单：
   - 解析 `Authorization`
   - 校验 token
   - 解析用户身份
   - 判断管理员权限
   - 写入 `X-User-Id` 与 `X-Role-Code`
6. 匹配路由规则
7. 转发到下游服务
8. 请求返回后，`RequestLogFilter` 记录耗时日志

### 13.2 白名单请求链路

如：

- `/api/auth/login`
- `/api/node-agent/register`
- `/api/node-agent/heartbeat`

它们的链路是：

1. 进入网关
2. 写 traceId
3. 记录请求日志
4. `JwtAuthFilter` 发现命中白名单
5. 直接放行
6. 路由到对应服务

这也是为什么节点代理注册和心跳不需要先登录。

### 13.3 管理员接口链路

如：

- `/api/users/**`
- `/api/nodes/**`
- `/api/schedules/**`
- 对 `/api/solvers/**` 的写操作

链路上比普通用户多一步：

- `JwtAuthFilter` 在解析出 `roleCode` 后，会额外检查是否为 `ADMIN`

如果不是，就抛 `ForbiddenException`，最后由 `GatewayExceptionHandler` 返回 403。

### 13.4 网关异常链路

当过滤器链抛出异常时：

1. `JwtAuthFilter` 或其它组件抛出异常
2. `GatewayExceptionHandler.handle(...)` 捕获
3. 包装成 `Result.fail(...)`
4. 设置正确的 HTTP 状态码
5. 返回 JSON 给前端

---

## 14. 设计文档与当前实现的对照

### 14.1 已经较好落地的部分

结合现有设计文档和代码，当前网关已经较好落地了这些目标：

- 提供统一入口；
- 按服务进行路径转发；
- 支持白名单；
- 支持基于 token 的统一前置鉴权；
- 支持粗粒度管理员接口保护；
- 支持将用户上下文透传给下游服务；
- 支持统一请求日志与 traceId；
- 支持统一网关异常响应。

### 14.2 当前是“基础网关”而不是“完整服务治理网关”的部分

当前实现仍明显是基础版网关：

- 路由仍是静态 URI，不是动态服务发现；
- 没有熔断、限流、重试、降级；
- 没有黑白名单 IP 控制；
- 没有更细粒度权限模型；
- 没有 API 版本治理；
- 没有请求体级审计或改写；
- 没有统一的响应包装增强；
- 没有网关级缓存。

### 14.3 当前实现里几个必须写清楚的细节

#### 1. 当前 token 校验依赖的是简化版 `JwtUtil`

网关虽然叫 `JwtAuthFilter`，但它依赖的 `JwtUtil` 来自 `common-lib`，当前实际上只是 Base64 编码解析，不是严格的标准 JWT 实现。

所以文档中更准确的说法应是：

- 实现了“基于 token 的统一认证前置”
- 但当前 token 机制仍是简化版

#### 2. 管理员权限控制当前只做粗粒度路径级保护

网关当前能做到的是：

- 哪些路径必须管理员访问
- 哪些写操作必须管理员访问

但它不负责更细的业务权限，例如：

- 某管理员是否能修改某个具体资源
- 某用户是否只能看到自己的任务

这些仍应下沉到业务服务。

#### 3. `discovery.locator.enabled = true` 但当前主要还是静态路由

这是一个很容易在答辩时被误解的点。  
配置里虽然打开了 discovery locator，但当前真正的转发地址还是通过 `GatewayRouteProperties` 明确写死的服务 URI。

#### 4. `RouteDefinitionLoader` 和 `GatewayResponseWriter` 目前都是预留类

这两者适合作为“后续扩展点”写进文档，而不是描述成已完成能力。

---

## 15. 推荐的阅读顺序

### 15.1 先看路由配置

建议先看：

- `GatewayRouteConfig.java`
- `GatewayRouteProperties.java`

这样先理解“哪些请求会被转发到哪里”。

### 15.2 再看鉴权和请求主链

然后看：

- `TraceIdFilter.java`
- `RequestLogFilter.java`
- `JwtAuthFilter.java`
- `WhiteListConfig.java`
- `GatewaySecurityProperties.java`

这样就能看清请求进入网关后的真实过滤链。

### 15.3 最后看支撑与异常处理

最后看：

- `TokenParser.java`
- `PathMatcherSupport.java`
- `GatewayRequestMutator.java`
- `GatewayExceptionHandler.java`

这样就能把“辅助逻辑”和“错误收口”补完整。

---

## 16. 当前结论

`gateway-service` 当前已经完成了毕设原型所需的基础网关能力：

- 统一入口；
- 统一鉴权；
- 粗粒度权限控制；
- 请求日志与 traceId；
- 统一异常响应；
- 静态路由转发。

因此，它已经足以支撑当前这个多服务后端原型的对外访问闭环。

但它还不是生产级服务治理网关。当前更准确的定位是：

> 一个面向毕设原型、以静态路由和基础安全前置为主的轻量网关服务。

如果下一步继续增强，最值得优先补的方向是：

- 标准 JWT 和更完整认证体系；
- 动态服务发现与注册中心联动；
- 限流、熔断、重试、降级；
- 更细粒度的权限模型；
- 更完整的网关可观测性和治理能力。
