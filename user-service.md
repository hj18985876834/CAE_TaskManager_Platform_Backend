# 五、user-service 项目结构设计

用户服务相对简单，适合采用标准分层。

## 1. 推荐结构

```text
user-service/
└── src/main/java/com/yourorg/user/
    ├── UserApplication.java
    ├── interfaces/
    │   ├── controller/
    │   ├── request/
    │   └── response/
    ├── application/
    │   ├── service/
    │   ├── facade/
    │   └── assembler/
    ├── domain/
    │   ├── model/
    │   ├── repository/
    │   ├── service/
    │   └── enums/
    ├── infrastructure/
    │   ├── persistence/
    │   │   ├── entity/
    │   │   ├── mapper/
    │   │   └── repository/
    │   └── security/
    ├── config/
    └── support/
```

## 2. 推荐核心类划分

### interfaces/controller

- `AuthController`
- `UserController`

### interfaces/request

- `LoginRequest`
- `CreateUserRequest`
- `UpdateUserRequest`
- `UpdateUserStatusRequest`

### interfaces/response

- `LoginResponse`
- `UserDetailResponse`
- `UserListItemResponse`

### application/service

- `AuthAppService`
- `UserAppService`

这里负责流程，例如：

- 登录校验
- 创建用户流程
- 修改状态流程

### application/facade

如果你想让 Controller 更薄，可以让 Controller 只调 Facade：

- `AuthFacade`
- `UserFacade`

### application/assembler

专门做转换：

- `UserAssembler`

### domain/model

- `User`
- `Role`

### domain/repository

- `UserRepository`
- `RoleRepository`

### domain/service

- `PasswordDomainService`
- `UserDomainService`

### infrastructure/persistence/entity

- `UserPO`
- `RolePO`

### infrastructure/persistence/mapper

- `UserMapper`
- `RoleMapper`

### infrastructure/persistence/repository

- `UserRepositoryImpl`
- `RoleRepositoryImpl`

### infrastructure/security

- `PasswordEncoderSupport`
- `JwtTokenService`

## 3. 这样分的好处

用户服务虽然不复杂，但这样拆以后：

- 登录逻辑不会和 CRUD 混在一起
- DTO/VO/PO 不会混用
- 后面接 RBAC 也容易扩展

------

# 五、user-service 完整包树

用户服务整体比较标准，主要负责登录、用户管理、角色控制。后端设计里也明确它不做复杂权限系统，只保留 `roleCode` 判断。

## 1. 完整结构

```text
user-service/
└── src/main/java/com/example/cae/user/
    ├── UserApplication.java
    ├── interfaces/
    │   ├── controller/
    │   │   ├── AuthController.java
    │   │   └── UserController.java
    │   ├── request/
    │   │   ├── LoginRequest.java
    │   │   ├── CreateUserRequest.java
    │   │   ├── UpdateUserRequest.java
    │   │   ├── UpdateUserStatusRequest.java
    │   │   └── ResetPasswordRequest.java
    │   └── response/
    │       ├── LoginResponse.java
    │       ├── CurrentUserResponse.java
    │       ├── UserListItemResponse.java
    │       └── UserDetailResponse.java
    ├── application/
    │   ├── service/
    │   │   ├── AuthAppService.java
    │   │   └── UserAppService.java
    │   ├── facade/
    │   │   ├── AuthFacade.java
    │   │   └── UserFacade.java
    │   └── assembler/
    │       └── UserAssembler.java
    ├── domain/
    │   ├── model/
    │   │   ├── User.java
    │   │   └── Role.java
    │   ├── repository/
    │   │   ├── UserRepository.java
    │   │   └── RoleRepository.java
    │   ├── service/
    │   │   ├── UserDomainService.java
    │   │   └── PasswordDomainService.java
    │   └── enums/
    │       └── UserStatusEnum.java
    ├── infrastructure/
    │   ├── persistence/
    │   │   ├── entity/
    │   │   │   ├── UserPO.java
    │   │   │   └── RolePO.java
    │   │   ├── mapper/
    │   │   │   ├── UserMapper.java
    │   │   │   └── RoleMapper.java
    │   │   └── repository/
    │   │       ├── UserRepositoryImpl.java
    │   │       └── RoleRepositoryImpl.java
    │   └── security/
    │       ├── JwtTokenService.java
    │       └── PasswordEncoderService.java
    ├── config/
    │   ├── UserServiceConfig.java
    │   └── MybatisPlusConfig.java
    └── support/
        └── UserQueryBuilder.java
```

## 2. 你真正会高频写的类

- `AuthController`
- `UserController`
- `AuthAppService`
- `UserAppService`
- `UserRepositoryImpl`
- `UserMapper`

-----------

# 四、user-service 初始化代码骨架清单

你的设计里，user-service 不做复杂权限系统，只做登录、用户管理、角色判断，所以结构可以标准一些。

## 1. Controller

### `interfaces/controller/AuthController.java`

职责：

* 登录
* 获取当前用户
* 退出登录

建议方法：

* `login(LoginRequest request)`
* `me()`
* `logout()`

---

### `interfaces/controller/UserController.java`

职责：

* 用户列表
* 创建用户
* 修改用户
* 启停用户
* 重置密码

建议方法：

* `pageUsers(UserPageQueryRequest request)`
* `createUser(CreateUserRequest request)`
* `updateUser(Long userId, UpdateUserRequest request)`
* `updateStatus(Long userId, UpdateUserStatusRequest request)`
* `resetPassword(Long userId, ResetPasswordRequest request)`

---

## 2. Application

### `application/service/AuthAppService.java`

职责：登录流程编排

建议方法：

* `login(String username, String password)`
* `getCurrentUser(Long userId)`
* `logout(Long userId)`

---

### `application/service/UserAppService.java`

职责：用户管理流程编排

建议方法：

* `pageUsers(UserPageQueryRequest request)`
* `createUser(CreateUserRequest request)`
* `updateUser(Long userId, UpdateUserRequest request)`
* `updateStatus(Long userId, Integer status)`
* `resetPassword(Long userId, String newPassword)`

---

### `application/assembler/UserAssembler.java`

职责：

* Request -> Domain
* Domain -> Response

建议方法：

* `toDomain(CreateUserRequest request)`
* `toDetailResponse(User user)`
* `toListItemResponse(User user)`

---

## 3. Domain

### `domain/model/User.java`

职责：用户领域对象

建议方法：

* `enable()`
* `disable()`
* `resetPassword(String encodedPassword)`
* `checkEnabled()`

---

### `domain/model/Role.java`

职责：角色领域对象

---

### `domain/repository/UserRepository.java`

职责：用户仓储接口

建议方法：

* `findById(Long userId)`
* `findByUsername(String username)`
* `save(User user)`
* `update(User user)`
* `page(UserPageQuery query)`

---

### `domain/service/UserDomainService.java`

职责：用户领域规则

建议方法：

* `checkUsernameUnique(String username)`
* `checkLogin(User user, String rawPassword)`
* `checkCanModify(User operator, User target)`

---

### `domain/service/PasswordDomainService.java`

职责：密码相关规则

建议方法：

* `encode(String rawPassword)`
* `matches(String rawPassword, String encodedPassword)`

---

## 4. Infrastructure

### `infrastructure/persistence/entity/UserPO.java`

### `infrastructure/persistence/entity/RolePO.java`

---

### `infrastructure/persistence/mapper/UserMapper.java`

建议方法：

* 继承 `BaseMapper<UserPO>`

---

### `infrastructure/persistence/mapper/RoleMapper.java`

---

### `infrastructure/persistence/repository/UserRepositoryImpl.java`

职责：仓储落地实现

建议方法：

* `findById`
* `findByUsername`
* `save`
* `update`
* `page`

---

### `infrastructure/security/JwtTokenService.java`

职责：JWT 发放

建议方法：

* `generateToken(User user)`
* `parseUserContext(String token)`

---

### `infrastructure/security/PasswordEncoderService.java`

职责：密码加密实现

建议方法：

* `encode`
* `matches`

-------

继续。下面我给你 **`user-service` 的完整初始化代码骨架**，保持和前面几个模块同样的粒度，并严格对齐你已经统一后的后端设计口径：

- `user-service` 负责：**登录、当前用户信息、用户管理、角色管理**
- 系统只保留两类角色：`ADMIN / USER`
- 不做复杂 RBAC
- 网关负责鉴权，`user-service` 负责登录与用户数据管理
- token 统一走 `Authorization: Bearer {token}` 体系

------

# 一、user-service 的定位

`user-service` 在整个系统里承担的是**认证与用户基础管理**职责，重点有四类：

1. 用户登录
2. 获取当前登录用户信息
3. 用户管理
4. 角色基础管理

它是一个相对“标准 CRUD + 认证”的服务，不像 `task-service` 那样流程复杂，所以可以用比较标准的分层结构，不必额外引入太多复杂抽象。

但也要避免两个常见问题：

- 把登录逻辑和用户管理全塞进一个 `UserServiceImpl`
- DTO / VO / PO / Domain 对象混用

正确做法是：

- **Controller**：收参和返回
- **Application**：流程编排
- **Domain**：规则与领域对象
- **Infrastructure**：数据库与 token 落地实现

------

# 二、user-service 最终推荐包树

```text
user-service/
└── src/main/java/com/example/cae/user/
    ├── UserApplication.java
    ├── interfaces/
    │   ├── controller/
    │   │   ├── AuthController.java
    │   │   └── UserController.java
    │   ├── request/
    │   │   ├── LoginRequest.java
    │   │   ├── CreateUserRequest.java
    │   │   ├── UpdateUserRequest.java
    │   │   ├── UpdateUserStatusRequest.java
    │   │   ├── ResetPasswordRequest.java
    │   │   └── UserPageQueryRequest.java
    │   └── response/
    │       ├── LoginResponse.java
    │       ├── CurrentUserResponse.java
    │       ├── UserListItemResponse.java
    │       └── UserDetailResponse.java
    ├── application/
    │   ├── service/
    │   │   ├── AuthAppService.java
    │   │   └── UserAppService.java
    │   ├── facade/
    │   │   ├── AuthFacade.java
    │   │   └── UserFacade.java
    │   └── assembler/
    │       └── UserAssembler.java
    ├── domain/
    │   ├── model/
    │   │   ├── User.java
    │   │   └── Role.java
    │   ├── repository/
    │   │   ├── UserRepository.java
    │   │   └── RoleRepository.java
    │   ├── service/
    │   │   ├── UserDomainService.java
    │   │   └── PasswordDomainService.java
    │   └── enums/
    │       └── UserStatusEnum.java
    ├── infrastructure/
    │   ├── persistence/
    │   │   ├── entity/
    │   │   │   ├── UserPO.java
    │   │   │   └── RolePO.java
    │   │   ├── mapper/
    │   │   │   ├── UserMapper.java
    │   │   │   └── RoleMapper.java
    │   │   └── repository/
    │   │       ├── UserRepositoryImpl.java
    │   │       └── RoleRepositoryImpl.java
    │   └── security/
    │       ├── JwtTokenService.java
    │       └── PasswordEncoderService.java
    ├── config/
    │   ├── UserServiceConfig.java
    │   └── MybatisPlusConfig.java
    └── support/
        └── UserQueryBuilder.java
```

这版结构和你前面已经确定的用户服务职责边界是一致的。

------

# 三、user-service 的核心调用链

## 1. 登录链路

```
AuthController -> AuthAppService -> UserRepository / PasswordDomainService / JwtTokenService
```

## 2. 当前用户信息链路

```
AuthController -> AuthAppService -> UserRepository
```

## 3. 用户管理链路

```
UserController -> UserAppService -> UserDomainService -> UserRepository
```

这样划分后，登录和用户管理不会混在一起。

------

# 四、request / response 设计

------

## 1. interfaces/request

### LoginRequest.java

```java
package com.example.cae.user.interfaces.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
```

------

### CreateUserRequest.java

```java
package com.example.cae.user.interfaces.request;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String username;
    private String password;
    private String realName;
    private Long roleId;
}
```

------

### UpdateUserRequest.java

```java
package com.example.cae.user.interfaces.request;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String realName;
    private Long roleId;
}
```

------

### UpdateUserStatusRequest.java

```java
package com.example.cae.user.interfaces.request;

import lombok.Data;

@Data
public class UpdateUserStatusRequest {
    private Integer status;
}
```

------

### ResetPasswordRequest.java

```java
package com.example.cae.user.interfaces.request;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String newPassword;
}
```

------

### UserPageQueryRequest.java

```java
package com.example.cae.user.interfaces.request;

import lombok.Data;

@Data
public class UserPageQueryRequest {
    private Integer pageNum;
    private Integer pageSize;
    private String username;
    private String realName;
    private Integer status;
    private Long roleId;
}
```

------

## 2. interfaces/response

### LoginResponse.java

```java
package com.example.cae.user.interfaces.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String token;
    private String tokenType;
}
```

------

### CurrentUserResponse.java

```java
package com.example.cae.user.interfaces.response;

import lombok.Data;

@Data
public class CurrentUserResponse {
    private Long userId;
    private String username;
    private String realName;
    private String roleCode;
}
```

------

### UserListItemResponse.java

```java
package com.example.cae.user.interfaces.response;

import lombok.Data;

@Data
public class UserListItemResponse {
    private Long userId;
    private String username;
    private String realName;
    private Long roleId;
    private String roleCode;
    private Integer status;
}
```

------

### UserDetailResponse.java

```java
package com.example.cae.user.interfaces.response;

import lombok.Data;

@Data
public class UserDetailResponse {
    private Long userId;
    private String username;
    private String realName;
    private Long roleId;
    private String roleCode;
    private Integer status;
}
```

------

# 五、Controller 层完整骨架

------

## 1. AuthController.java

职责：

- 登录
- 获取当前用户
- 退出登录

```java
package com.example.cae.user.interfaces.controller;

import com.example.cae.common.response.Result;
import com.example.cae.user.application.service.AuthAppService;
import com.example.cae.user.interfaces.request.LoginRequest;
import com.example.cae.user.interfaces.response.CurrentUserResponse;
import com.example.cae.user.interfaces.response.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthAppService authAppService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.success(authAppService.login(request));
    }

    @GetMapping("/me")
    public Result<CurrentUserResponse> me(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(authAppService.getCurrentUser(userId));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
```

### 说明

- `/api/auth/me` 不再自己解析 token，而是依赖网关透传的 `X-User-Id`
- 这样符合你整套网关统一鉴权的架构口径

------

## 2. UserController.java

职责：

- 用户分页
- 创建用户
- 更新用户
- 启停用户
- 重置密码

```java
package com.example.cae.user.interfaces.controller;

import com.example.cae.common.response.PageResult;
import com.example.cae.common.response.Result;
import com.example.cae.user.application.service.UserAppService;
import com.example.cae.user.interfaces.request.*;
import com.example.cae.user.interfaces.response.UserDetailResponse;
import com.example.cae.user.interfaces.response.UserListItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserAppService userAppService;

    @GetMapping
    public Result<PageResult<UserListItemResponse>> pageUsers(UserPageQueryRequest request) {
        return Result.success(userAppService.pageUsers(request));
    }

    @PostMapping
    public Result<Void> createUser(@RequestBody CreateUserRequest request) {
        userAppService.createUser(request);
        return Result.success();
    }

    @GetMapping("/{userId}")
    public Result<UserDetailResponse> getUserDetail(@PathVariable Long userId) {
        return Result.success(userAppService.getUserDetail(userId));
    }

    @PutMapping("/{userId}")
    public Result<Void> updateUser(@PathVariable Long userId,
                                   @RequestBody UpdateUserRequest request) {
        userAppService.updateUser(userId, request);
        return Result.success();
    }

    @PutMapping("/{userId}/status")
    public Result<Void> updateStatus(@PathVariable Long userId,
                                     @RequestBody UpdateUserStatusRequest request) {
        userAppService.updateStatus(userId, request);
        return Result.success();
    }

    @PostMapping("/{userId}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long userId,
                                      @RequestBody ResetPasswordRequest request) {
        userAppService.resetPassword(userId, request);
        return Result.success();
    }
}
```

------

# 六、Application 层完整骨架

------

## 1. AuthAppService.java

职责：登录流程与当前用户信息流程。

```java
package com.example.cae.user.application.service;

import com.example.cae.common.exception.BizException;
import com.example.cae.user.domain.model.User;
import com.example.cae.user.domain.repository.UserRepository;
import com.example.cae.user.domain.service.PasswordDomainService;
import com.example.cae.user.infrastructure.security.JwtTokenService;
import com.example.cae.user.interfaces.request.LoginRequest;
import com.example.cae.user.interfaces.response.CurrentUserResponse;
import com.example.cae.user.interfaces.response.LoginResponse;
import com.example.cae.user.application.assembler.UserAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthAppService {

    private final UserRepository userRepository;
    private final PasswordDomainService passwordDomainService;
    private final JwtTokenService jwtTokenService;
    private final UserAssembler userAssembler;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername());
        if (user == null) {
            throw new BizException("用户名或密码错误");
        }

        if (!user.isEnabled()) {
            throw new BizException("用户已被禁用");
        }

        boolean matched = passwordDomainService.matches(request.getPassword(), user.getPassword());
        if (!matched) {
            throw new BizException("用户名或密码错误");
        }

        String token = jwtTokenService.generateToken(user);
        return new LoginResponse(token, "Bearer");
    }

    public CurrentUserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return userAssembler.toCurrentUserResponse(user);
    }
}
```

------

## 2. UserAppService.java

职责：用户管理流程。

```java
package com.example.cae.user.application.service;

import com.example.cae.common.exception.BizException;
import com.example.cae.common.response.PageResult;
import com.example.cae.user.application.assembler.UserAssembler;
import com.example.cae.user.domain.model.Role;
import com.example.cae.user.domain.model.User;
import com.example.cae.user.domain.repository.RoleRepository;
import com.example.cae.user.domain.repository.UserRepository;
import com.example.cae.user.domain.service.PasswordDomainService;
import com.example.cae.user.domain.service.UserDomainService;
import com.example.cae.user.interfaces.request.*;
import com.example.cae.user.interfaces.response.UserDetailResponse;
import com.example.cae.user.interfaces.response.UserListItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAppService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserDomainService userDomainService;
    private final PasswordDomainService passwordDomainService;
    private final UserAssembler userAssembler;

    public PageResult<UserListItemResponse> pageUsers(UserPageQueryRequest request) {
        PageResult<User> page = userRepository.page(request);
        List<UserListItemResponse> records = page.getRecords()
                .stream()
                .map(userAssembler::toListItemResponse)
                .toList();
        return PageResult.of(page.getTotal(), page.getPageNum(), page.getPageSize(), records);
    }

    public void createUser(CreateUserRequest request) {
        userDomainService.checkUsernameUnique(request.getUsername());

        Role role = roleRepository.findById(request.getRoleId());
        if (role == null) {
            throw new BizException("角色不存在");
        }

        User user = userAssembler.toUser(request);
        user.setPassword(passwordDomainService.encode(request.getPassword()));
        user.enable();
        userRepository.save(user);
    }

    public UserDetailResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return userAssembler.toDetailResponse(user);
    }

    public void updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }

        user.setRealName(request.getRealName());
        user.setRoleId(request.getRoleId());
        userRepository.update(user);
    }

    public void updateStatus(Long userId, UpdateUserStatusRequest request) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }

        if (request.getStatus() != null && request.getStatus() == 1) {
            user.enable();
        } else {
            user.disable();
        }

        userRepository.update(user);
    }

    public void resetPassword(Long userId, ResetPasswordRequest request) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }

        user.resetPassword(passwordDomainService.encode(request.getNewPassword()));
        userRepository.update(user);
    }
}
```

------

## 3. Facade 层说明

`user-service` 这里其实 **可以先不强制使用 facade**。
如果你想和其他服务风格完全一致，可以预留：

- `AuthFacade`
- `UserFacade`

但初期完全可以不写实现，把复杂度控制住。

------

## 4. UserAssembler.java

职责：对象转换。

```java
package com.example.cae.user.application.assembler;

import com.example.cae.user.domain.model.User;
import com.example.cae.user.infrastructure.persistence.entity.UserPO;
import com.example.cae.user.interfaces.request.CreateUserRequest;
import com.example.cae.user.interfaces.response.CurrentUserResponse;
import com.example.cae.user.interfaces.response.UserDetailResponse;
import com.example.cae.user.interfaces.response.UserListItemResponse;
import org.springframework.stereotype.Component;

@Component
public class UserAssembler {

    public User toUser(CreateUserRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setRealName(request.getRealName());
        user.setRoleId(request.getRoleId());
        return user;
    }

    public CurrentUserResponse toCurrentUserResponse(User user) {
        CurrentUserResponse response = new CurrentUserResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRoleCode(user.getRoleCode());
        return response;
    }

    public UserListItemResponse toListItemResponse(User user) {
        UserListItemResponse response = new UserListItemResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRoleId(user.getRoleId());
        response.setRoleCode(user.getRoleCode());
        response.setStatus(user.getStatus());
        return response;
    }

    public UserDetailResponse toDetailResponse(User user) {
        UserDetailResponse response = new UserDetailResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRoleId(user.getRoleId());
        response.setRoleCode(user.getRoleCode());
        response.setStatus(user.getStatus());
        return response;
    }

    public User fromPO(UserPO po) {
        if (po == null) {
            return null;
        }
        User user = new User();
        user.setId(po.getId());
        user.setUsername(po.getUsername());
        user.setPassword(po.getPassword());
        user.setRealName(po.getRealName());
        user.setRoleId(po.getRoleId());
        user.setStatus(po.getStatus());
        return user;
    }

    public UserPO toPO(User user) {
        if (user == null) {
            return null;
        }
        UserPO po = new UserPO();
        po.setId(user.getId());
        po.setUsername(user.getUsername());
        po.setPassword(user.getPassword());
        po.setRealName(user.getRealName());
        po.setRoleId(user.getRoleId());
        po.setStatus(user.getStatus());
        return po;
    }
}
```

------

# 七、Domain 层完整骨架

------

## 1. User.java

这是用户聚合根。

```java
package com.example.cae.user.domain.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private Long roleId;
    private String roleCode;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void enable() {
        this.status = 1;
    }

    public void disable() {
        this.status = 0;
    }

    public boolean isEnabled() {
        return this.status != null && this.status == 1;
    }

    public void resetPassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
```

------

## 2. Role.java

```java
package com.example.cae.user.domain.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Role {
    private Long id;
    private String roleCode;
    private String roleName;
    private LocalDateTime createdAt;
}
```

------

## 3. UserStatusEnum.java

虽然状态也可以直接用 `1/0`，但建议加一个本地枚举。

```java
package com.example.cae.user.domain.enums;

public enum UserStatusEnum {
    DISABLED(0),
    ENABLED(1);

    private final Integer code;

    UserStatusEnum(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
```

------

## 4. UserRepository.java

```java
package com.example.cae.user.domain.repository;

import com.example.cae.common.response.PageResult;
import com.example.cae.user.domain.model.User;
import com.example.cae.user.interfaces.request.UserPageQueryRequest;

public interface UserRepository {
    User findById(Long userId);
    User findByUsername(String username);
    void save(User user);
    void update(User user);
    PageResult<User> page(UserPageQueryRequest request);
}
```

------

## 5. RoleRepository.java

```java
package com.example.cae.user.domain.repository;

import com.example.cae.user.domain.model.Role;

public interface RoleRepository {
    Role findById(Long roleId);
}
```

------

## 6. UserDomainService.java

职责：用户相关规则。

```java
package com.example.cae.user.domain.service;

import com.example.cae.common.exception.BizException;
import com.example.cae.user.domain.model.User;
import com.example.cae.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDomainService {

    private final UserRepository userRepository;

    public void checkUsernameUnique(String username) {
        User user = userRepository.findByUsername(username);
        if (user != null) {
            throw new BizException("用户名已存在");
        }
    }
}
```

------

## 7. PasswordDomainService.java

```java
package com.example.cae.user.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordDomainService {

    private final com.example.cae.user.infrastructure.security.PasswordEncoderService passwordEncoderService;

    public String encode(String rawPassword) {
        return passwordEncoderService.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoderService.matches(rawPassword, encodedPassword);
    }
}
```

------

# 八、Infrastructure 层完整骨架

------

## 1. persistence/entity

### UserPO.java

```java
package com.example.cae.user.infrastructure.persistence.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserPO {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private Long roleId;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

------

### RolePO.java

```java
package com.example.cae.user.infrastructure.persistence.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RolePO {
    private Long id;
    private String roleCode;
    private String roleName;
    private LocalDateTime createdAt;
}
```

------

## 2. mapper

### UserMapper.java

```java
package com.example.cae.user.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.cae.user.infrastructure.persistence.entity.UserPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserPO> {
}
```

------

### RoleMapper.java

```java
package com.example.cae.user.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.cae.user.infrastructure.persistence.entity.RolePO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoleMapper extends BaseMapper<RolePO> {
}
```

------

## 3. repository impl

### UserRepositoryImpl.java

```java
package com.example.cae.user.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.cae.common.response.PageResult;
import com.example.cae.user.application.assembler.UserAssembler;
import com.example.cae.user.domain.model.User;
import com.example.cae.user.domain.repository.UserRepository;
import com.example.cae.user.infrastructure.persistence.entity.UserPO;
import com.example.cae.user.infrastructure.persistence.mapper.UserMapper;
import com.example.cae.user.interfaces.request.UserPageQueryRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;
    private final UserAssembler userAssembler;

    @Override
    public User findById(Long userId) {
        UserPO po = userMapper.selectById(userId);
        return userAssembler.fromPO(po);
    }

    @Override
    public User findByUsername(String username) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getUsername, username).last("limit 1");
        UserPO po = userMapper.selectOne(wrapper);
        return userAssembler.fromPO(po);
    }

    @Override
    public void save(User user) {
        userMapper.insert(userAssembler.toPO(user));
    }

    @Override
    public void update(User user) {
        userMapper.updateById(userAssembler.toPO(user));
    }

    @Override
    public PageResult<User> page(UserPageQueryRequest request) {
        // 第一版先简化实现，不接入 MP 分页对象也可以
        List<UserPO> list = userMapper.selectList(new LambdaQueryWrapper<>());
        List<User> users = list.stream().map(userAssembler::fromPO).toList();
        return PageResult.of((long) users.size(),
                request.getPageNum() == null ? 1L : request.getPageNum().longValue(),
                request.getPageSize() == null ? 10L : request.getPageSize().longValue(),
                users);
    }
}
```

------

### RoleRepositoryImpl.java

```java
package com.example.cae.user.infrastructure.persistence.repository;

import com.example.cae.user.domain.model.Role;
import com.example.cae.user.domain.repository.RoleRepository;
import com.example.cae.user.infrastructure.persistence.entity.RolePO;
import com.example.cae.user.infrastructure.persistence.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryImpl implements RoleRepository {

    private final RoleMapper roleMapper;

    @Override
    public Role findById(Long roleId) {
        RolePO po = roleMapper.selectById(roleId);
        if (po == null) {
            return null;
        }
        Role role = new Role();
        role.setId(po.getId());
        role.setRoleCode(po.getRoleCode());
        role.setRoleName(po.getRoleName());
        role.setCreatedAt(po.getCreatedAt());
        return role;
    }
}
```

------

## 4. security

### JwtTokenService.java

职责：发 token。

```java
package com.example.cae.user.infrastructure.security;

import com.example.cae.common.utils.JwtUtil;
import com.example.cae.user.domain.model.User;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    public String generateToken(User user) {
        return JwtUtil.generateToken(user.getId(), user.getRoleCode());
    }
}
```

------

### PasswordEncoderService.java

```java
package com.example.cae.user.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordEncoderService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
```

------

# 九、support / config 层骨架

------

## 1. UserQueryBuilder.java

如果后面分页筛选逻辑复杂，可以抽这个类。第一版可以先空壳。

```java
package com.example.cae.user.support;

import org.springframework.stereotype.Component;

@Component
public class UserQueryBuilder {
}
```

------

## 2. UserApplication.java

```java
package com.example.cae.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
```

------

## 3. UserServiceConfig.java

```java
package com.example.cae.user.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class UserServiceConfig {
}
```

------

## 4. MybatisPlusConfig.java

```java
package com.example.cae.user.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {
}
```

------

# 十、最小可运行实现顺序

不要一次把 `user-service` 写得太大，按这个顺序最稳。

## 第一批：登录最小闭环

先建：

- `User`
- `AuthController`
- `AuthAppService`
- `UserRepository`
- `UserRepositoryImpl`
- `UserMapper`
- `JwtTokenService`
- `PasswordEncoderService`

先做到：

- 登录
- 返回 Bearer token
- `/api/auth/me`

这和你整个系统先跑认证闭环的思路一致。

------

## 第二批：用户管理

再建：

- `UserController`
- `UserAppService`
- `UserAssembler`
- `Role`
- `RoleRepository`
- `RoleRepositoryImpl`

做到：

- 用户分页
- 创建用户
- 更新用户
- 启停用户

------

## 第三批：补密码和细节

再建：

- `ResetPasswordRequest`
- `PasswordDomainService`
- `UserDomainService`

做到：

- 重置密码
- 用户名唯一校验
- 用户禁用校验

------

# 十一、你现在最该先建的 10 个类

如果你要立刻开工，我建议 `user-service` 先建这 10 个：

1. `UserApplication`
2. `AuthController`
3. `UserController`
4. `AuthAppService`
5. `UserAppService`
6. `User`
7. `UserRepository`
8. `UserRepositoryImpl`
9. `UserMapper`
10. `JwtTokenService`

这 10 个类一出来，`user-service` 的主骨架就有了。

------

# 十二、最终建议

`user-service` 最重要的不是“结构复杂”，而是“边界清楚”。

你现在这版骨架已经把边界处理得比较干净了：

- `AuthController / AuthAppService` 专门管登录
- `UserController / UserAppService` 专门管用户管理
- `UserDomainService` 只处理规则
- `PasswordEncoderService` 只处理密码
- `JwtTokenService` 只负责发 token
- `RepositoryImpl` 只负责落库

这就是比较规范、易于开发、后面也不容易乱的 `user-service` 结构，而且和你当前修订后的整体后端设计完全一致。

