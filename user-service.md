# user-service 模块说明文档

## 1. 文档目的

这份文档不是只做“模块概览”，而是按“模块目录 -> 分层目录 -> 具体代码文件”的方式说明 `user-service`，目的是让人能快速回答下面几个问题：

- `user-service` 在系统中到底负责什么
- 每一层代码对应哪个文件夹
- 每个 Java 文件具体做什么
- 设计文档里的职责，当前代码到底实现到了哪一步
- 真正要改代码时，应该先去哪个目录找

本文档基于当前仓库中的真实源码编写，分析范围以 `user-service/src/main/java` 与 `user-service/src/main/resources` 为主，不把 `target/` 下的编译产物作为设计分析对象。

---

## 2. 模块定位

`user-service` 是平台中的用户与认证基础服务，负责解决“谁可以进入系统、进入后是谁、管理员如何维护用户账户”这类问题。

结合需求分析、接口设计和当前代码实现，它的职责主要有 4 类：

1. 用户登录，校验账号密码并返回访问令牌。
2. 查询当前登录用户信息。
3. 提供管理员使用的用户管理能力，包括分页、创建、详情、修改、启停、重置密码。
4. 提供给其他微服务使用的内部用户基础信息查询接口。

它不负责下面这些事：

- 不负责 API 网关侧的统一拦截与转发。
- 不负责任务、调度、求解器、节点等业务。
- 不负责复杂 RBAC、菜单权限、资源权限。
- 不负责真正生产级的认证中心能力，如刷新令牌、吊销机制、黑名单等。

也就是说，当前 `user-service` 的定位是“原型平台中的认证与用户主数据服务”，而不是一个完整的 IAM 或权限中台。

---

## 3. 模块根目录与源码入口

### 3.1 模块根目录

模块根目录是：

```text
user-service/
```

在这个目录下，当前真正需要重点关注的是：

- `pom.xml`
- `src/main/java/com/example/cae/user/`
- `src/main/resources/application.yml`

其中：

- `pom.xml` 决定模块依赖和打包方式。
- `src/main/java/com/example/cae/user/` 是主源码目录。
- `src/main/resources/application.yml` 是服务运行配置。

### 3.2 不需要作为设计分析重点的目录

下面这些目录主要是构建产物，不是源代码设计层：

```text
user-service/target/
```

`target/` 下的 `.class`、`.jar`、`maven-status` 等内容，属于编译输出，不建议写入模块设计说明中。

---

## 4. 分层与文件夹映射

为了方便定位，先给出 `user-service` 当前分层与文件夹的直接映射关系。

| 分层 | 对应文件夹 | 作用 |
| --- | --- | --- |
| 启动入口层 | `user-service/src/main/java/com/example/cae/user/` | Spring Boot 启动入口 |
| 配置层 | `user-service/src/main/java/com/example/cae/user/config/` | 当前服务的 Spring 配置与预留配置 |
| 接口层 | `user-service/src/main/java/com/example/cae/user/interfaces/` | 对外 REST 接口、内部接口、请求对象、响应对象 |
| 应用层 | `user-service/src/main/java/com/example/cae/user/application/` | 业务流程编排、Facade 封装、对象组装 |
| 领域层 | `user-service/src/main/java/com/example/cae/user/domain/` | 用户/角色领域对象、仓储抽象、领域规则 |
| 基础设施层 | `user-service/src/main/java/com/example/cae/user/infrastructure/` | 数据库访问、仓储实现、安全实现 |
| 支撑层 | `user-service/src/main/java/com/example/cae/user/support/` | 预留的辅助组件 |
| 资源配置层 | `user-service/src/main/resources/` | 服务端口、数据源等运行配置 |

如果只想快速找代码，可以直接按下面的规则记：

- 想看接口定义：去 `interfaces/controller`
- 想看请求和返回结构：去 `interfaces/request`、`interfaces/response`
- 想看核心流程：去 `application/service`
- 想看业务编排入口：去 `application/facade`
- 想看用户/角色模型：去 `domain/model`
- 想看数据库访问：去 `infrastructure/persistence`
- 想看密码和 token：去 `infrastructure/security`

---

## 5. 模块级结构总览

当前源码结构如下：

```text
user-service/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/cae/user/
│       │       ├── UserApplication.java
│       │       ├── application/
│       │       │   ├── assembler/
│       │       │   │   └── UserAssembler.java
│       │       │   ├── facade/
│       │       │   │   ├── AuthFacade.java
│       │       │   │   └── UserFacade.java
│       │       │   └── service/
│       │       │       ├── AuthAppService.java
│       │       │       └── UserAppService.java
│       │       ├── config/
│       │       │   ├── MybatisPlusConfig.java
│       │       │   └── UserServiceConfig.java
│       │       ├── domain/
│       │       │   ├── enums/
│       │       │   │   └── UserStatusEnum.java
│       │       │   ├── model/
│       │       │   │   ├── Role.java
│       │       │   │   └── User.java
│       │       │   ├── repository/
│       │       │   │   ├── RoleRepository.java
│       │       │   │   └── UserRepository.java
│       │       │   └── service/
│       │       │       ├── PasswordDomainService.java
│       │       │       └── UserDomainService.java
│       │       ├── infrastructure/
│       │       │   ├── persistence/
│       │       │   │   ├── entity/
│       │       │   │   │   ├── RolePO.java
│       │       │   │   │   └── UserPO.java
│       │       │   │   ├── mapper/
│       │       │   │   │   ├── RoleMapper.java
│       │       │   │   │   └── UserMapper.java
│       │       │   │   └── repository/
│       │       │   │       ├── RoleRepositoryImpl.java
│       │       │   │       └── UserRepositoryImpl.java
│       │       │   └── security/
│       │       │       ├── JwtTokenService.java
│       │       │       └── PasswordEncoderService.java
│       │       ├── interfaces/
│       │       │   ├── controller/
│       │       │   │   ├── AuthController.java
│       │       │   │   ├── InternalUserController.java
│       │       │   │   └── UserController.java
│       │       │   ├── request/
│       │       │   │   ├── CreateUserRequest.java
│       │       │   │   ├── LoginRequest.java
│       │       │   │   ├── ResetPasswordRequest.java
│       │       │   │   ├── UpdateUserRequest.java
│       │       │   │   ├── UpdateUserStatusRequest.java
│       │       │   │   └── UserPageQueryRequest.java
│       │       │   └── response/
│       │       │       ├── CurrentUserResponse.java
│       │       │       ├── InternalUserBasicResponse.java
│       │       │       ├── LoginResponse.java
│       │       │       ├── UserCreateResponse.java
│       │       │       ├── UserDetailResponse.java
│       │       │       └── UserListItemResponse.java
│       │       └── support/
│       │           └── UserQueryBuilder.java
│       └── resources/
│           └── application.yml
└── target/
```

---

## 6. 根目录文件说明

这一部分描述模块根目录下最关键的文件。

### 6.1 `user-service/pom.xml`

作用：

- 声明 `user-service` 是父工程 `cae-taskmanager-backend` 的一个子模块。
- 引入当前服务运行所需依赖。
- 配置 Spring Boot 打包插件，生成可运行 jar。

当前关键依赖有：

- `spring-boot-starter-web`：提供 Web/MVC/REST 能力。
- `mybatis-plus-spring-boot3-starter`：提供 MyBatis 集成能力。
- `mysql-connector-j`：连接 MySQL。
- `common-lib`：复用公共错误码、返回体、异常、工具类。

说明：

- 虽然引入了 MyBatis-Plus starter，但当前代码主要还是使用 MyBatis 注解 Mapper。
- 这意味着当前模块“依赖上具备 MyBatis-Plus 条件”，但“实现上并没有重度使用其高级能力”。

### 6.2 `user-service/src/main/resources/application.yml`

作用：

- 配置 `user-service` 的端口、字符编码、Spring 应用名和数据源。

当前配置重点如下：

- 服务名：`user-service`
- 默认端口：`8081`
- 数据库连接：`user_db`
- 数据源地址、用户名、密码通过环境变量支持覆盖

这说明当前模块默认是一个独立部署、独立连接用户库的服务。

---

## 7. 启动入口层

### 7.1 对应文件夹

```text
user-service/src/main/java/com/example/cae/user/
```

### 7.2 文件说明

#### `UserApplication.java`

作用：

- `user-service` 的 Spring Boot 启动类。
- 使用 `@SpringBootApplication(scanBasePackages = "com.example.cae")` 扫描整个项目命名空间下的 Bean。

定位意义：

- 这是模块启动入口。
- 只要服务启动失败、Bean 没注入、扫描不到组件，首先就要看这里和相关配置。

---

## 8. 配置层

### 8.1 对应文件夹

```text
user-service/src/main/java/com/example/cae/user/config/
```

### 8.2 文件说明

#### `UserServiceConfig.java`

作用：

- 一个空的 `@Configuration` 配置类。

当前状态：

- 当前没有定义 Bean。
- 更像是为后续扩展预留的配置承载点。

适合后续放什么：

- 业务 Bean 装配
- RestTemplate / Feign / 安全相关扩展配置
- 自定义 Converter、拦截器等

#### `MybatisPlusConfig.java`

作用：

- 也是一个空的 `@Configuration` 配置类。

当前状态：

- 文件名表明它原本预期承载 MyBatis-Plus 相关配置。
- 但当前实现并未在其中配置分页插件、SQL 审计器等内容。

结论：

- 当前模块虽然依赖了 MyBatis-Plus starter，但配置类仍是占位状态。
- 文档里不应把它描述为“已实现复杂 MyBatis-Plus 配置”。

---

## 9. 接口层

### 9.1 对应文件夹

```text
user-service/src/main/java/com/example/cae/user/interfaces/
```

这个目录又分成 3 个子目录：

- `controller/`：控制器，定义 HTTP 接口
- `request/`：请求对象
- `response/`：响应对象

---

### 9.2 控制器目录

对应文件夹：

```text
user-service/src/main/java/com/example/cae/user/interfaces/controller/
```

#### `AuthController.java`

作用：

- 对外提供认证相关接口。
- 控制器根路径是 `/api/auth`。

当前实际接口有：

- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/logout`

每个方法的实际作用：

- `login(...)`
  - 接收 `LoginRequest`
  - 调用 `AuthFacade.login`
  - 返回 `LoginResponse`

- `me(...)`
  - 从请求头读取 `X-User-Id`
  - 调用 `AuthFacade.currentUser`
  - 返回当前用户信息
  - 这里说明当前实现依赖网关完成 token 解析后透传用户 ID

- `logout(...)`
  - 当前直接 `return Result.success()`
  - 实际上没有调用 `AuthFacade` 或 `AuthAppService`
  - 这说明“登出”目前只是一个语义接口，占位为无状态成功响应

要点：

- 文档里如果写“logout 已完成服务端会话清理”是不准确的。
- 当前它只是保留接口形式，符合无状态 token 原型的实现方式。

#### `UserController.java`

作用：

- 对外提供用户管理接口。
- 控制器根路径是 `/api/users`。

当前实际接口有：

- `GET /api/users`
- `POST /api/users`
- `GET /api/users/{id}`
- `PUT /api/users/{id}`
- `POST /api/users/{id}/status`
- `POST /api/users/{id}/reset-password`

每个方法的实际作用：

- `pageUsers(...)`
  - 用户分页查询
  - 查询参数来自 `UserPageQueryRequest`

- `createUser(...)`
  - 创建用户
  - 请求体为 `CreateUserRequest`

- `getById(...)`
  - 查询用户详情

- `updateUser(...)`
  - 更新真实姓名和角色

- `updateStatus(...)`
  - 以 `PUT` 方式更新启停状态

- `updateStatusPost(...)`
  - 以 `POST` 方式更新启停状态
  - 这是一个兼容式接口设计，说明当前实现同时兼容两种调用方式

- `resetPassword(...)`
  - 重置指定用户密码

要点：

- 当前用户状态接口同时提供 `PUT` 和 `POST` 两种写法，实际代码里都走同一条业务逻辑。

#### `InternalUserController.java`

作用：

- 提供内部服务调用接口。
- 控制器根路径是 `/internal/users`。

当前实际接口：

- `GET /internal/users/{id}`

用途：

- 供其他微服务按用户 ID 获取最小必要的用户基础信息。

返回内容：

- `id`
- `username`
- `realName`
- `status`

设计意义：

- 体现“通过内部接口共享用户主数据，而不是跨库查询”。

---

### 9.3 请求对象目录

对应文件夹：

```text
user-service/src/main/java/com/example/cae/user/interfaces/request/
```

#### `LoginRequest.java`

作用：

- 登录接口请求体。

字段：

- `username`
- `password`

校验特点：

- 两个字段都要求非空。
- 长度都限制在 64 以内。

#### `CreateUserRequest.java`

作用：

- 创建用户请求体。

字段：

- `username`
- `password`
- `realName`
- `roleId`

校验特点：

- `username`、`password`、`realName` 不能为空。
- `password` 长度 6 到 64。
- `roleId` 必须大于 0。

#### `UpdateUserRequest.java`

作用：

- 更新用户基本信息请求体。

字段：

- `realName`
- `roleId`

特点：

- 两个字段都允许为空，表示按需更新。
- `roleId` 若传入，必须大于 0。

#### `UpdateUserStatusRequest.java`

作用：

- 更新启停状态请求体。

字段：

- `status`

校验特点：

- 不允许为空。
- 只允许 `0` 或 `1`。

#### `ResetPasswordRequest.java`

作用：

- 管理员重置密码请求体。

字段：

- `newPassword`

校验特点：

- 不能为空。
- 长度 6 到 64。

#### `UserPageQueryRequest.java`

作用：

- 用户分页查询请求对象。

字段：

- `pageNum`
- `pageSize`
- `username`
- `realName`
- `status`
- `roleId`

校验特点：

- `pageNum >= 1`
- `pageSize` 在 `1 ~ 200`
- `roleId` 若传入必须大于 0

说明：

- 这是一个查询 DTO，不是数据库实体。
- 同时也被 `UserMapper` 直接拿来做分页过滤条件。

---

### 9.4 响应对象目录

对应文件夹：

```text
user-service/src/main/java/com/example/cae/user/interfaces/response/
```

这一层的类主要是“对外返回结构”，本身不包含业务逻辑。

#### `LoginResponse.java`

作用：

- 登录成功后的返回对象。

主要字段：

- `token`
- `tokenType`
- `id`
- `userId`
- `username`
- `realName`
- `roleId`
- `roleCode`
- `roleName`

说明：

- 当前实现同时返回 `id` 和 `userId`，二者值相同，属于接口兼容式冗余字段。

#### `CurrentUserResponse.java`

作用：

- `/api/auth/me` 的返回对象。

主要字段：

- `id`
- `userId`
- `username`
- `realName`
- `roleId`
- `roleCode`
- `roleName`
- `status`

#### `InternalUserBasicResponse.java`

作用：

- 内部接口 `/internal/users/{id}` 的返回对象。

主要字段：

- `id`
- `username`
- `realName`
- `status`

特点：

- 字段更少，只保留其他服务最需要的基础信息。

#### `UserCreateResponse.java`

作用：

- 创建用户成功后的返回对象。

主要字段：

- `id`
- `userId`
- `username`
- `realName`
- `roleId`
- `roleCode`
- `roleName`
- `status`

#### `UserDetailResponse.java`

作用：

- 用户详情返回对象。

主要字段：

- `id`
- `userId`
- `username`
- `realName`
- `roleId`
- `roleCode`
- `roleName`
- `status`

#### `UserListItemResponse.java`

作用：

- 用户分页列表中的单条记录对象。

主要字段：

- `id`
- `userId`
- `username`
- `realName`
- `roleId`
- `roleCode`
- `roleName`
- `status`
- `createdAt`

---

## 10. 应用层

### 10.1 对应文件夹

```text
user-service/src/main/java/com/example/cae/user/application/
```

这个目录又分为：

- `assembler/`：对象转换与响应组装
- `facade/`：给控制器调用的薄封装入口
- `service/`：实际业务编排

---

### 10.2 组装器目录

对应文件夹：

```text
user-service/src/main/java/com/example/cae/user/application/assembler/
```

#### `UserAssembler.java`

作用：

- 在领域对象和响应对象之间做转换。
- 把请求对象转换成领域对象。

当前主要方法：

- `toLoginResponse(...)`
- `toCurrentUserResponse(...)`
- `toUserDetailResponse(...)`
- `toUserCreateResponse(...)`
- `toUserListItem(...)`
- `toDomain(CreateUserRequest request)`

设计意义：

- 避免在 Controller 或 Service 里散落大量 DTO 组装代码。

注意点：

- 这里组装的 `id` 和 `userId` 当前是重复赋值。
- 这是接口风格问题，不是业务逻辑问题。

---

### 10.3 Facade 目录

对应文件夹：

```text
user-service/src/main/java/com/example/cae/user/application/facade/
```

#### `AuthFacade.java`

作用：

- 对认证相关应用服务做一层简单包装，供 `AuthController` 调用。

当前方法：

- `login(LoginRequest request)`
- `currentUser(Long userId)`

说明：

- 当前没有 `logout(...)` 方法，因为控制器里的 `logout` 直接返回成功，没有真正下沉到应用层。

#### `UserFacade.java`

作用：

- 对用户管理流程做统一调用入口，供 `UserController` 和 `InternalUserController` 使用。

当前方法：

- `pageUsers(...)`
- `createUser(...)`
- `getById(...)`
- `updateUser(...)`
- `updateStatus(...)`
- `resetPassword(...)`
- `getInternalById(...)`

意义：

- Facade 让控制器更薄。
- 也让控制器不直接依赖多个应用服务或复杂编排逻辑。

---

### 10.4 应用服务目录

对应文件夹：

```text
user-service/src/main/java/com/example/cae/user/application/service/
```

#### `AuthAppService.java`

作用：

- 实现登录和当前用户查询这两条核心认证流程。

当前依赖：

- `UserRepository`
- `RoleRepository`
- `PasswordEncoderService`
- `JwtTokenService`

主要方法：

##### `login(LoginRequest request)`

流程如下：

1. 校验请求和用户名密码是否为空。
2. 通过 `UserRepository.findByUsername` 查用户。
3. 校验用户状态是否为启用。
4. 调用 `PasswordEncoderService.matches` 校验密码。
5. 通过 `RoleRepository.findById` 查角色。
6. 如果查不到角色，则生成一个默认 `USER` 角色作为回退值。
7. 调用 `JwtTokenService.generateToken` 生成 token。
8. 通过 `UserAssembler.toLoginResponse` 组装返回值。

##### `getCurrentUser(Long userId)`

流程如下：

1. 按用户 ID 查询用户。
2. 按角色 ID 查询角色。
3. 如果角色不存在，则构造默认回退角色。
4. 组装 `CurrentUserResponse`。

##### `logout(Long userId)`

当前状态：

- 方法存在。
- 但只是一个 no-op。
- 由于控制器当前没有调用它，实际上仍属于预留方法。

#### `UserAppService.java`

作用：

- 实现用户管理相关的大部分业务编排，是 `user-service` 最核心的业务类之一。

当前依赖：

- `UserRepository`
- `RoleRepository`
- `UserDomainService`
- `PasswordDomainService`

主要方法及作用：

##### `pageUsers(UserPageQueryRequest request)`

作用：

- 完成用户分页查询。
- 给每个用户补齐角色信息并组装分页记录。

实现特点：

- 会先计算分页参数默认值。
- 使用 `userRepository.count` 和 `userRepository.page` 分两次查总数与记录。
- 对每条用户记录再单独查角色。

注意点：

- 当前这里存在典型的 “按用户逐条查角色” 行为，属于 N+1 查询风格。
- 对小规模毕设原型问题不大，但不宜写成高性能最佳实践。

##### `createUser(CreateUserRequest request)`

作用：

- 创建用户。

流程：

1. 判空。
2. 调用 `UserDomainService.checkUsernameUnique` 做用户名唯一性校验。
3. 校验角色是否存在。
4. 使用 `UserAssembler.toDomain` 构造 `User` 领域对象。
5. 调用 `PasswordDomainService.encode` 对密码编码。
6. 默认启用用户。
7. 保存到数据库。
8. 返回创建结果。

##### `getById(Long id)`

作用：

- 查询用户详情。

##### `findUserById(Long id)`

作用：

- 查询用户领域对象。
- 是当前服务内部复用方法，不直接暴露给控制器。

##### `getInternalById(Long id)`

作用：

- 给内部接口生成 `InternalUserBasicResponse`。

##### `updateUser(Long userId, UpdateUserRequest request)`

作用：

- 更新用户真实姓名、角色 ID。

实现特点：

- 先查用户，不存在则抛错。
- 若传入 `roleId`，则先校验角色存在。
- 只更新请求中非空的字段。

##### `updateStatus(Long userId, UpdateUserStatusRequest request)`

作用：

- 更新启停状态。

实现特点：

- 当 `status == 1` 时调用 `user.enable()`。
- 其他情况调用 `user.disable()`。

##### `resetPassword(Long userId, ResetPasswordRequest request)`

作用：

- 重置指定用户密码。

实现特点：

- 使用 `PasswordDomainService.encode` 进行密码编码。
- 调用 `user.resetPassword(...)` 更新领域对象。

---

## 11. 领域层

### 11.1 对应文件夹

```text
user-service/src/main/java/com/example/cae/user/domain/
```

该目录下的职责是“表达业务概念与业务规则”，而不是直接写 SQL 或 HTTP。

它包含 4 个子目录：

- `enums/`
- `model/`
- `repository/`
- `service/`

---

### 11.2 枚举目录

对应文件夹：

```text
user-service/src/main/java/com/example/cae/user/domain/enums/
```

#### `UserStatusEnum.java`

作用：

- 定义用户状态枚举：
  - `DISABLED(0)`
  - `ENABLED(1)`

当前状态：

- 枚举已经定义。
- 但主流程里仍大量直接使用数字 `0/1`。

结论：

- 这是一个“已定义但使用深度较浅”的枚举。
- 后续如果要提升代码一致性，可以把状态判断逐步收敛到该枚举上。

---

### 11.3 领域模型目录

对应文件夹：

```text
user-service/src/main/java/com/example/cae/user/domain/model/
```

#### `User.java`

作用：

- 表达用户领域对象。

当前承载的主要字段：

- `id`
- `username`
- `password`
- `realName`
- `roleId`
- `roleCode`
- `status`
- `createdAt`
- `updatedAt`

当前承载的领域行为：

- `enable()`
- `disable()`
- `isEnabled()`
- `resetPassword(String encodedPassword)`

注意点：

- `roleCode` 字段虽然存在，但当前仓储层并不会给它赋值。
- 当前业务真正使用角色编码时，主要还是通过 `Role` 对象获取。

#### `Role.java`

作用：

- 表达角色领域对象。

主要字段：

- `id`
- `roleCode`
- `roleName`
- `createdAt`

特点：

- 当前角色模型较轻，主要承担展示与 token 生成时的角色编码来源。

---

### 11.4 仓储抽象目录

对应文件夹：

```text
user-service/src/main/java/com/example/cae/user/domain/repository/
```

#### `UserRepository.java`

作用：

- 定义用户仓储抽象接口。

当前方法：

- `findById(Long id)`
- `findByUsername(String username)`
- `save(User user)`
- `update(User user)`
- `page(UserPageQueryRequest request, long offset, long pageSize)`
- `count(UserPageQueryRequest request)`

意义：

- 上层业务依赖领域仓储抽象，而不是直接依赖 MyBatis Mapper。

#### `RoleRepository.java`

作用：

- 定义角色仓储抽象接口。

当前方法：

- `findById(Long id)`

---

### 11.5 领域服务目录

对应文件夹：

```text
user-service/src/main/java/com/example/cae/user/domain/service/
```

#### `UserDomainService.java`

作用：

- 承载用户领域规则。

当前实际规则：

- 校验用户名是否为空。
- 校验用户名是否已存在。

当前方法：

- `checkUsernameUnique(String username)`

#### `PasswordDomainService.java`

作用：

- 对密码编码能力做一层领域封装。

当前方法：

- `encode(String rawPassword)`
- `matches(String rawPassword, String encodedPassword)`

设计意义：

- 上层编排逻辑不直接接触具体加密实现类。
- 后续若把 SHA-256 升级为 BCrypt，这一层有助于减少业务代码改动。

---

## 12. 基础设施层

### 12.1 对应文件夹

```text
user-service/src/main/java/com/example/cae/user/infrastructure/
```

它又分成两大部分：

- `persistence/`：持久化
- `security/`：安全与认证相关实现

---

### 12.2 持久化目录

对应文件夹：

```text
user-service/src/main/java/com/example/cae/user/infrastructure/persistence/
```

它又分成：

- `entity/`：持久化对象 PO
- `mapper/`：MyBatis Mapper
- `repository/`：仓储接口的数据库实现

#### 12.2.1 `entity/`

对应文件夹：

```text
user-service/src/main/java/com/example/cae/user/infrastructure/persistence/entity/
```

##### `UserPO.java`

作用：

- `sys_user` 表对应的持久化对象。

字段：

- `id`
- `username`
- `password`
- `realName`
- `roleId`
- `status`
- `createdAt`
- `updatedAt`

##### `RolePO.java`

作用：

- `sys_role` 表对应的持久化对象。

字段：

- `id`
- `roleCode`
- `roleName`
- `createdAt`

#### 12.2.2 `mapper/`

对应文件夹：

```text
user-service/src/main/java/com/example/cae/user/infrastructure/persistence/mapper/
```

##### `UserMapper.java`

作用：

- 直接承接 `sys_user` 表的 SQL 操作。

当前方法与作用：

- `selectByUsername(String username)`
  - 按用户名查单个用户

- `selectById(Long id)`
  - 按主键查用户

- `insert(UserPO po)`
  - 插入用户
  - 使用自增主键回填 `id`

- `updateById(UserPO po)`
  - 按主键更新用户
  - 当前会一并更新 `real_name`、`role_id`、`status`、`password`

- `selectPage(UserPageQueryRequest request, long offset, long pageSize)`
  - 动态分页查询
  - 支持用户名、真实姓名、状态、角色过滤

- `count(UserPageQueryRequest request)`
  - 查询分页总数

实现特点：

- 当前 Mapper 使用 MyBatis 注解 SQL，而不是 XML。

##### `RoleMapper.java`

作用：

- 负责 `sys_role` 表的按 ID 查询。

当前方法：

- `selectById(Long id)`

#### 12.2.3 `repository/`

对应文件夹：

```text
user-service/src/main/java/com/example/cae/user/infrastructure/persistence/repository/
```

##### `UserRepositoryImpl.java`

作用：

- `UserRepository` 的数据库实现。

当前职责：

- 调用 `UserMapper`
- 负责 `UserPO <-> User` 转换
- 实现分页、保存、更新、按 ID/用户名查询

当前注意点：

- `toPO(User user)` 不会带上 `createdAt/updatedAt`，这些值主要依赖数据库维护。
- `toDomain(UserPO po)` 会把数据库字段映射回领域对象。

##### `RoleRepositoryImpl.java`

作用：

- `RoleRepository` 的数据库实现。

当前职责：

- 调用 `RoleMapper.selectById`
- 把 `RolePO` 转成 `Role`

当前注意点：

- 虽然 `Role` 和 `RolePO` 都有 `createdAt` 字段，但当前 `toDomain(...)` 只映射了 `id`、`roleCode`、`roleName`，没有映射 `createdAt`。
- 这不影响当前主流程，但说明角色创建时间当前没有被真正使用。

---

### 12.3 安全实现目录

对应文件夹：

```text
user-service/src/main/java/com/example/cae/user/infrastructure/security/
```

#### `JwtTokenService.java`

作用：

- 封装 token 生成逻辑。

当前行为：

- 直接调用 `common-lib` 中的 `JwtUtil.generateToken(userId, roleCode)`。

需要特别说明的事实：

- 当前 `JwtUtil` 实际实现是把 `userId:roleCode` 做 Base64 编码。
- 它不是带签名、过期时间、标准头载荷结构的生产级 JWT。

所以在论文和文档里，更准确的表述应该是：

- “统一 token 方案”
- “简化 token 原型实现”

而不应该直接写成：

- “已经实现完整 JWT 安全体系”

#### `PasswordEncoderService.java`

作用：

- 提供密码编码和密码匹配实现。

当前行为：

- `encode(...)` 使用 `SHA-256`。
- `matches(...)` 既支持“编码后比对”，也兼容“明文直接相等”。

这说明什么：

- 当前实现更偏向“原型可用”和“兼容已有测试数据”。
- 不是生产级密码安全方案。

---

## 13. 支撑层

### 13.1 对应文件夹

```text
user-service/src/main/java/com/example/cae/user/support/
```

### 13.2 文件说明

#### `UserQueryBuilder.java`

作用：

- 当前是一个空的 `@Component` 类。

现状判断：

- 从名字看，原本预期用于封装用户查询条件构建逻辑。
- 但当前分页 SQL 直接写在 `UserMapper` 中，因此这个类尚未真正投入使用。

结论：

- 这是一个预留类。
- 文档中不应写成“已经实现复杂查询构造器”。

---

## 14. 与数据库表的对应关系

结合数据库设计，`user-service` 当前主要对应两张表：

### 14.1 `sys_user`

对应代码主要有：

- `domain/model/User.java`
- `infrastructure/persistence/entity/UserPO.java`
- `infrastructure/persistence/mapper/UserMapper.java`
- `infrastructure/persistence/repository/UserRepositoryImpl.java`

### 14.2 `sys_role`

对应代码主要有：

- `domain/model/Role.java`
- `infrastructure/persistence/entity/RolePO.java`
- `infrastructure/persistence/mapper/RoleMapper.java`
- `infrastructure/persistence/repository/RoleRepositoryImpl.java`

---

## 15. 核心调用链

这一部分用于帮助快速理解“一个请求到底会经过哪些文件”。

### 15.1 登录流程

```text
AuthController
  -> AuthFacade
  -> AuthAppService
  -> UserRepository.findByUsername
  -> UserRepositoryImpl
  -> UserMapper.selectByUsername
  -> PasswordEncoderService.matches
  -> RoleRepository.findById
  -> RoleRepositoryImpl
  -> RoleMapper.selectById
  -> JwtTokenService.generateToken
  -> UserAssembler.toLoginResponse
```

### 15.2 当前用户查询流程

```text
gateway-service 先校验 token 并透传 X-User-Id
  -> AuthController.me
  -> AuthFacade.currentUser
  -> AuthAppService.getCurrentUser
  -> UserRepository / RoleRepository
  -> UserAssembler.toCurrentUserResponse
```

### 15.3 用户创建流程

```text
UserController.createUser
  -> UserFacade.createUser
  -> UserAppService.createUser
  -> UserDomainService.checkUsernameUnique
  -> RoleRepository.findById
  -> UserAssembler.toDomain
  -> PasswordDomainService.encode
  -> UserRepository.save
  -> UserRepositoryImpl
  -> UserMapper.insert
  -> UserAssembler.toUserCreateResponse
```

### 15.4 内部用户查询流程

```text
其他微服务
  -> InternalUserController.getById
  -> UserFacade.getInternalById
  -> UserAppService.getInternalById
  -> UserRepository.findById
  -> InternalUserBasicResponse
```

---

## 16. 设计文档与当前实现的对照

这部分非常重要，因为你后面写论文、模块说明、答辩稿时，最好区分“设计要求”和“当前真实实现”。

### 16.1 已经较好落地的部分

- 用户登录已经实现。
- 当前用户查询已经实现。
- 用户分页、创建、详情、修改、启停、重置密码已经实现。
- 内部用户基础信息查询已经实现。
- 用户与角色分表、通过仓储隔离数据访问的结构已经实现。
- 用户服务与网关服务职责分离的基本模式已经实现。

### 16.2 当前是“简化实现”而不是“完整实现”的部分

- token 是简化 Base64 载荷方案，不是完整 JWT 安全体系。
- logout 仅保留接口语义，没有服务端失效逻辑。
- 密码加密是 SHA-256 基础实现，不是 BCrypt/Argon2。
- 权限模型只有基础角色，不是完整 RBAC。

### 16.3 当前是“预留/占位”的部分

- `UserServiceConfig.java`
- `MybatisPlusConfig.java`
- `UserQueryBuilder.java`

这些文件存在，但当前还没有承载实际复杂逻辑。

### 16.4 当前代码中的几个实现细节，文档里建议明确说明

- `UserController` 已收口为只保留正式接口 `POST /api/users/{id}/status`。
- `AuthController.logout` 当前没有调用应用层。
- `UserStatusEnum` 已定义，但主流程仍直接使用 `0/1`。
- `User.roleCode` 字段存在，但仓储层未填充。
- `pageUsers(...)` 中按用户逐条查询角色，存在 N+1 风格。

---

## 17. 推荐的阅读顺序

如果你是第一次接触 `user-service`，建议按下面顺序看代码：

### 17.1 先理解接口

依次看：

1. `interfaces/controller/AuthController.java`
2. `interfaces/controller/UserController.java`
3. `interfaces/controller/InternalUserController.java`

这样可以先明白这个服务对外暴露了什么。

### 17.2 再看业务流程

依次看：

1. `application/facade/AuthFacade.java`
2. `application/service/AuthAppService.java`
3. `application/facade/UserFacade.java`
4. `application/service/UserAppService.java`

这样能知道接口背后的业务编排。

### 17.3 再看数据访问

依次看：

1. `domain/repository/UserRepository.java`
2. `domain/repository/RoleRepository.java`
3. `infrastructure/persistence/repository/UserRepositoryImpl.java`
4. `infrastructure/persistence/repository/RoleRepositoryImpl.java`
5. `infrastructure/persistence/mapper/UserMapper.java`
6. `infrastructure/persistence/mapper/RoleMapper.java`

这样可以把“领域抽象”和“数据库落地”对应起来。

### 17.4 最后看对象结构和边界

依次看：

1. `domain/model/User.java`
2. `domain/model/Role.java`
3. `application/assembler/UserAssembler.java`
4. `interfaces/request/*`
5. `interfaces/response/*`
6. `infrastructure/security/*`

这样可以把参数对象、领域对象、数据库对象、响应对象的边界全部串起来。

---

## 18. 当前结论

`user-service` 当前已经形成了一个结构比较清晰、职责边界比较明确的“认证与用户基础管理服务”原型。

从代码结构上看，它采用的是比较标准的分层方式：

- 接口层负责接收请求
- 应用层负责编排流程
- 领域层负责表达业务对象和规则
- 基础设施层负责数据库与安全实现

从实现成熟度上看，它属于：

- 功能上已经可用
- 架构上已经规范
- 安全和治理上仍是原型级

因此，在论文或模块说明中，最合适的表述应该是：

> `user-service` 已完成平台用户登录、当前用户查询、用户管理和内部用户信息服务等基础能力，采用标准分层架构实现用户主数据管理与认证流程编排，并与网关服务协作形成统一访问闭环；当前实现重点满足原型平台演示与联调需要，在 token 安全性、权限粒度和配置扩展性方面仍保留进一步增强空间。
