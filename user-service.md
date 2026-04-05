# user-service 模块分析文档

## 1. 模块定位

`user-service` 是平台中的用户与认证基础服务，负责管理“谁可以进入系统、当前用户是谁、用户基本资料如何维护”这类通用能力。

在当前项目中，它承担的核心职责主要有四类：

- 用户登录
- 当前登录用户信息查询
- 用户管理
- 面向其他微服务的内部用户基础信息查询

它不负责网关鉴权拦截，不负责任务调度、节点管理、求解器管理等业务能力，也不实现复杂 RBAC 权限平台。

当前系统将认证与用户基础管理聚合在同一个服务中，这种设计对于本科毕设原型平台是合理的，既能体现服务拆分，也不会把认证体系做得过重。

## 2. 模块在系统中的作用

从整体架构上看，`user-service` 主要解决以下问题：

1. 接收用户登录请求并颁发访问 Token。
2. 维护平台中的用户账号、角色、状态、密码等基础数据。
3. 为网关透传身份后的业务访问提供“当前用户信息”查询能力。
4. 为其他服务按用户 ID 提供基础资料查询能力，减少跨服务直接访问用户库的需求。

因此，`user-service` 在整个系统中既是认证入口的一部分，也是平台用户主数据服务。

## 3. 当前实现架构

### 3.1 技术栈

- Spring Boot
- MVC 风格 Controller
- 分层架构：`interfaces / application / domain / infrastructure`
- MyBatis 注解 Mapper
- `common-lib` 中的异常、返回体、错误码、Token 工具

### 3.2 当前目录结构

当前代码结构如下：

```text
user-service/
└── src/main/java/com/example/cae/user/
    ├── UserApplication.java
    ├── application/
    │   ├── assembler/
    │   │   └── UserAssembler.java
    │   ├── facade/
    │   │   ├── AuthFacade.java
    │   │   └── UserFacade.java
    │   └── service/
    │       ├── AuthAppService.java
    │       └── UserAppService.java
    ├── config/
    │   ├── MybatisPlusConfig.java
    │   └── UserServiceConfig.java
    ├── domain/
    │   ├── enums/
    │   │   └── UserStatusEnum.java
    │   ├── model/
    │   │   ├── Role.java
    │   │   └── User.java
    │   ├── repository/
    │   │   ├── RoleRepository.java
    │   │   └── UserRepository.java
    │   └── service/
    │       ├── PasswordDomainService.java
    │       └── UserDomainService.java
    ├── infrastructure/
    │   ├── persistence/
    │   │   ├── entity/
    │   │   │   ├── RolePO.java
    │   │   │   └── UserPO.java
    │   │   ├── mapper/
    │   │   │   ├── RoleMapper.java
    │   │   │   └── UserMapper.java
    │   │   └── repository/
    │   │       ├── RoleRepositoryImpl.java
    │   │       └── UserRepositoryImpl.java
    │   └── security/
    │       ├── JwtTokenService.java
    │       └── PasswordEncoderService.java
    ├── interfaces/
    │   ├── controller/
    │   │   ├── AuthController.java
    │   │   ├── InternalUserController.java
    │   │   └── UserController.java
    │   ├── request/
    │   │   ├── CreateUserRequest.java
    │   │   ├── LoginRequest.java
    │   │   ├── ResetPasswordRequest.java
    │   │   ├── UpdateUserRequest.java
    │   │   ├── UpdateUserStatusRequest.java
    │   │   └── UserPageQueryRequest.java
    │   └── response/
    │       ├── CurrentUserResponse.java
    │       ├── InternalUserBasicResponse.java
    │       ├── LoginResponse.java
    │       ├── UserCreateResponse.java
    │       ├── UserDetailResponse.java
    │       └── UserListItemResponse.java
    └── support/
        └── UserQueryBuilder.java
```

这种结构比较标准，适合用户服务这类“认证 + 基础管理”型服务。

## 4. 各部分功能与职责

### 4.1 启动与配置层

#### `UserApplication`

职责：

- 启动 `user-service`
- 扫描各层 Bean

#### `UserServiceConfig`

当前为轻量配置类，主要起到配置容器承载作用。

#### `MybatisPlusConfig`

当前已预留，但实际仓储仍主要使用 MyBatis 注解 Mapper，而不是复杂的 MyBatis-Plus 特性。

### 4.2 接口层

#### `AuthController`

职责：

- 提供登录接口 `/api/auth/login`
- 提供当前用户接口 `/api/auth/me`
- 提供登出接口 `/api/auth/logout`

其中：

- 登录由 `user-service` 自身完成用户名密码校验并签发 Token；
- `/api/auth/me` 不自己解析前端 Token，而是读取网关透传的 `X-User-Id`；
- `logout` 当前为无状态 Token 模式下的空操作。

#### `UserController`

职责：

- 用户分页查询
- 创建用户
- 查询用户详情
- 更新用户资料
- 修改用户启停状态
- 重置密码

这一层对外暴露的是典型管理接口，主要面向管理员使用。

#### `InternalUserController`

职责：

- 提供内部接口 `/internal/users/{id}`
- 供其他服务按用户 ID 查询最基础的用户信息

这体现了微服务间“通过接口拿数据，而不是直接跨库”的设计原则。

### 4.3 应用层

#### `AuthFacade`

职责：

- 对认证相关流程做一层薄封装
- 保持 Controller 更轻

#### `UserFacade`

职责：

- 对用户管理相关流程做一层薄封装
- 向 Controller 屏蔽应用服务的细节

#### `AuthAppService`

职责：

- 编排登录流程
- 编排当前用户查询流程
- 编排登出流程

当前登录流程包括：

1. 校验用户名和密码是否为空。
2. 根据用户名查询用户。
3. 检查用户状态是否启用。
4. 校验密码是否匹配。
5. 根据 `roleId` 查询角色。
6. 生成 Token。
7. 组装登录响应。

#### `UserAppService`

职责：

- 用户分页查询
- 创建用户
- 查询详情
- 更新用户资料
- 更新用户状态
- 重置密码
- 生成内部用户基础信息响应

它是当前 `user-service` 里最主要的业务编排层。

### 4.4 领域层

#### `User`

职责：

- 表达用户领域对象
- 承载启用、禁用、重置密码等基础领域行为

当前已内聚的方法有：

- `enable()`
- `disable()`
- `isEnabled()`
- `resetPassword()`

#### `Role`

职责：

- 表达角色领域对象
- 当前主要承载 `id / roleCode / roleName`

#### `UserStatusEnum`

职责：

- 统一表达用户状态码语义

不过当前主流程里仍更多直接使用 `0 / 1` 进行判断，枚举使用深度还比较浅。

#### `UserRepository`

职责：

- 定义用户数据访问抽象
- 向上层提供 `findById / findByUsername / save / update / page / count`

#### `RoleRepository`

职责：

- 定义角色数据访问抽象
- 当前只提供按 ID 查询

#### `UserDomainService`

职责：

- 承载用户领域规则
- 当前主要实现“用户名唯一性检查”

#### `PasswordDomainService`

职责：

- 对密码编码与匹配做领域封装
- 避免上层直接依赖具体加密实现

### 4.5 基础设施层

#### `UserMapper`

职责：

- 对 `sys_user` 表执行查询、插入、更新、分页和计数操作
- 使用 MyBatis 注解 SQL 实现动态查询

#### `RoleMapper`

职责：

- 对角色表执行按 ID 查询

#### `UserRepositoryImpl`

职责：

- 将 `UserRepository` 领域接口落地为数据库实现
- 完成 `PO <-> Domain` 转换

#### `RoleRepositoryImpl`

职责：

- 将 `RoleRepository` 落地为数据库实现

#### `JwtTokenService`

职责：

- 封装 Token 生成逻辑
- 当前底层直接调用 `common-lib` 中的 `JwtUtil`

#### `PasswordEncoderService`

职责：

- 封装密码编码与匹配实现
- 当前采用 `SHA-256`

这个类是当前服务最需要在文档中说明“原型版边界”的部分之一。

### 4.6 组装层

#### `UserAssembler`

职责：

- 将领域对象组装成不同响应对象
- 将创建用户请求转换为领域对象

当前采用静态方法形式，主要负责：

- 登录响应组装
- 当前用户响应组装
- 用户详情响应组装
- 用户创建响应组装
- 用户列表项组装

## 5. 核心业务流程

### 5.1 登录流程

```text
客户端 -> AuthController
      -> AuthFacade
      -> AuthAppService
      -> UserRepository.findByUsername
      -> PasswordEncoderService.matches
      -> RoleRepository.findById
      -> JwtTokenService.generateToken
      -> LoginResponse
```

这是 `user-service` 最核心的认证流程。

### 5.2 当前用户查询流程

```text
客户端 -> gateway-service 完成鉴权并透传 X-User-Id
      -> AuthController
      -> AuthFacade
      -> AuthAppService
      -> UserRepository.findById
      -> RoleRepository.findById
      -> CurrentUserResponse
```

这里体现了当前系统中“网关负责入口鉴权，user-service 负责用户数据读取”的职责分离。

### 5.3 用户管理流程

```text
UserController
  -> UserFacade
  -> UserAppService
  -> UserDomainService / PasswordDomainService
  -> UserRepository / RoleRepository
  -> Response
```

### 5.4 内部用户查询流程

```text
其他微服务 -> /internal/users/{id}
         -> InternalUserController
         -> UserFacade
         -> UserAppService
         -> UserRepository
         -> InternalUserBasicResponse
```

## 6. 核心设计

### 6.1 设计一：认证与用户管理合并在一个服务内

当前项目没有把认证中心再单独拆成 `auth-service`，而是将登录、当前用户、用户管理统一放在 `user-service` 中。

这样做的原因是：

- 对本科毕设原型平台来说，实现复杂度更可控；
- 用户规模与权限模型较简单；
- 仍然能够体现微服务拆分，而不会把认证体系做得过重。

### 6.2 设计二：网关负责入口鉴权，用户服务负责登录签发

当前系统将认证流程拆成两段：

1. `user-service` 负责用户名密码登录和 Token 生成。
2. `gateway-service` 负责后续请求的 Token 校验和用户头透传。

这种设计有两个好处：

- `user-service` 专注认证源数据；
- 网关统一拦截后，业务服务无需重复校验前端 Token。

### 6.3 设计三：角色模型保持简单

当前系统只保留基础角色概念，核心角色为：

- `ADMIN`
- `USER`

没有继续实现复杂的 RBAC 权限点、菜单权限、资源权限等体系。

这是有意为之，因为当前平台更关注任务调度原型，而不是完整权限中台。

### 6.4 设计四：通过内部接口向其他服务暴露用户主数据

当前 `user-service` 提供内部接口查询基础用户信息，而不是让其他服务直接访问用户库。

这符合微服务设计原则：

- 数据归属清晰
- 服务间通过接口通信
- 避免跨库耦合

### 6.5 设计五：密码与 Token 实现先满足原型可用

当前密码与 Token 实现都采用了较简化方案：

- 密码：`SHA-256`
- Token：`JwtUtil` 中的简化 Base64 方案

这样做的目的不是追求生产级安全，而是优先完成“可演示、可联调、可答辩”的认证闭环。

## 7. 架构难点与解决方案

### 7.1 难点一：如何在不过度复杂的前提下完成认证闭环

问题：

- 如果直接引入完整 JWT、刷新令牌、黑名单、会话失效等机制，工程复杂度会显著上升；
- 如果完全没有 Token 机制，又无法体现前后端分离与统一鉴权架构。

当前解决方案：

- 登录时由 `user-service` 发放简化 Token；
- 业务访问时由网关统一校验；
- `logout` 暂时保持无状态空操作。

这个方案完成了原型闭环，也控制了实现复杂度。

### 7.2 难点二：如何避免用户服务和网关职责重叠

问题：

- 用户服务和网关都与认证有关，很容易边界不清。

当前解决方案：

- `user-service` 只负责登录签发与用户信息查询；
- `gateway-service` 只负责后续访问时的 Token 校验与 Header 透传。

这样边界比较清晰，不会重复造轮子。

### 7.3 难点三：如何让其他服务使用用户数据又不跨库

问题：

- 任务服务等其他模块经常需要用户名、真实姓名等基础信息；
- 如果直接跨库读取，会破坏微服务边界。

当前解决方案：

- 由 `user-service` 提供 `/internal/users/{id}` 内部接口；
- 其他服务通过接口获取最小必要用户信息。

### 7.4 难点四：如何兼顾“标准分层”和“实现简洁”

问题：

- 用户服务本身业务不复杂，如果分层过度，代码会显得很重；
- 如果完全不分层，又不利于论文写作和后续维护。

当前解决方案：

- 采用标准分层结构；
- Facade、Service、Repository 各司其职；
- 领域服务只保留必要规则，不人为制造复杂领域模型。

这种深度比较适合本科毕设项目。

## 8. 关键技术手段

### 8.1 Spring Boot 分层服务

用于实现：

- Controller 接口暴露
- 应用服务编排
- 组件注入

### 8.2 MyBatis 注解 SQL

用于实现：

- 用户查询
- 用户分页
- 用户计数
- 用户更新

当前采用注解 SQL 而不是 XML，结构较轻，便于项目原型开发。

### 8.3 Token 统一封装

通过 `JwtTokenService` 和 `common-lib` 中的 `JwtUtil` 实现 Token 生成与解析。

虽然当前不是生产级 JWT，但已经形成了统一的 Token 通路。

### 8.4 密码编码封装

通过 `PasswordEncoderService` 与 `PasswordDomainService` 对密码编码与匹配进行封装，避免密码处理散落在业务代码中。

### 8.5 统一错误码与统一返回体

借助 `common-lib` 中的：

- `ErrorCodeConstants`
- `BizException`
- `Result`
- `PageResult`

实现统一的错误处理与接口响应格式。

## 9. 当前实现的优点

- 认证、当前用户、用户管理、内部用户查询职责完整。
- 结构清晰，分层标准，适合论文和答辩讲解。
- 已与网关鉴权模式形成清晰协作关系。
- 已具备用户分页、创建、更新、启停、重置密码等基础管理能力。
- 已提供内部用户查询接口，符合微服务数据边界原则。

## 10. 当前实现的局限与边界

### 10.1 Token 方案是原型版，不是生产级 JWT

当前 `JwtUtil` 实际是简化的 Base64 编码载荷，没有：

- 签名
- 过期时间
- 刷新机制
- 吊销机制

因此文档中应明确写为“统一 Token 原型方案”，而不是“完整 JWT 安全体系”。

### 10.2 密码加密方案偏基础版

当前使用 `SHA-256`，且 `matches()` 兼容了“明文等于数据库值”的过渡判断。

这说明当前实现更偏向原型可用和兼容已有数据，而不是生产级密码安全方案。

### 10.3 权限模型较简化

当前只实现角色级别的基础控制，没有继续扩展为：

- 菜单权限
- 操作权限点
- 资源级授权
- 组织级权限

这对当前项目范围来说是合理的，但需要在文档中说明边界。

### 10.4 登出是无状态空实现

由于当前是无状态 Token 模式，`logout` 没有服务端失效逻辑。

这意味着它更多是接口语义保留，而不是完整会话注销机制。

## 11. 对本科毕设的价值

从本科毕设角度看，`user-service` 的价值主要体现在：

1. 支撑整个系统的统一登录与身份识别。
2. 体现微服务架构下“用户主数据独立服务”的设计思想。
3. 为网关统一鉴权和下游业务访问提供身份基础。
4. 通过内部接口体现服务间调用而非跨库访问。

虽然它不是平台里最复杂的服务，但它是系统能够形成完整访问闭环的基础模块。

## 12. 答辩时可采用的表述

可以将该模块概括为：

> `user-service` 负责平台用户认证与用户基础数据管理，提供登录、当前用户信息查询、用户管理和内部用户信息查询等能力。系统采用“用户服务签发 Token、网关统一校验 Token”的协作模式，实现了认证闭环，同时通过内部接口向其他微服务提供用户基础信息，保证了数据边界清晰和服务职责分离。

## 13. 后续可扩展方向

在不改变当前原型定位的前提下，后续可扩展：

- 将简化 Token 升级为生产级 JWT
- 将密码方案升级为 BCrypt 或 Argon2
- 增加 Token 过期、刷新、注销失效机制
- 增加登录失败限制和安全审计
- 扩展更细粒度的权限模型

这些内容应作为扩展能力，而不是当前首版已实现内容。

## 14. 当前结论

`user-service` 当前已经完成了本科毕设原型平台所需的基础职责：

- 可以完成登录与身份识别
- 可以支撑当前用户信息查询
- 可以完成用户管理
- 可以通过内部接口向其他服务提供用户基础数据

从实现深度看，它属于“结构规范、功能完整、安全能力基础版”的用户服务；从项目整体看，它已经足够支撑当前平台的认证与用户管理闭环。
