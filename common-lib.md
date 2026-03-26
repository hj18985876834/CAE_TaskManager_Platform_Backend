# 三、common-lib 设计

`common-lib` 只放**真正跨服务通用**的内容，不能变成“大杂烩”。

## 1. 推荐结构

```text
common-lib/
└── src/main/java/com/yourorg/common/
    ├── response/
    ├── exception/
    ├── enums/
    ├── constants/
    ├── dto/
    ├── utils/
    ├── model/
    └── config/
```

## 2. 各目录职责

### `response/`

放统一返回结构：

- `Result<T>`
- `PageResult<T>`

### `exception/`

放公共异常：

- `BizException`
- `UnauthorizedException`
- `ForbiddenException`
- `GlobalExceptionCode`

### `enums/`

只放所有服务都可能共用的枚举：

- `TaskStatusEnum`
- `FailTypeEnum`
- `NodeStatusEnum`
- `RoleCodeEnum`

### `constants/`

放公共常量：

- Header 常量
- Token 前缀常量
- 默认分页常量

### `dto/`

只放跨服务传输对象：

- `TaskDTO`
- `NodeDTO`
- `SolverProfileDTO`
- `FileRuleDTO`

### `utils/`

放纯工具类：

- `JwtUtil`
- `DateTimeUtil`
- `JsonUtil`
- `IdUtil`

### `model/`

放特别通用的基础模型：

- `BasePageQuery`
- `BaseUserContext`

### `config/`

尽量少放，最多放一些公共 Jackson / 时间格式配置。

## 3. 不建议放进 common-lib 的内容

不要放：

- 各服务自己的 entity
- 各服务自己的 request/response vo
- 各服务 mapper
- 业务过强的 service

原则是：**common-lib 越小越好**。

------

# 三、common-lib 完整包树

`common-lib` 只放“跨服务真正通用”的内容。你修改后的设计里已经明确它承担公共配置、枚举、DTO、异常、响应结构、工具类等职责。

## 1. 完整结构

```text
common-lib/
└── src/main/java/com/example/cae/common/
    ├── config/
    │   ├── JacksonConfig.java
    │   └── WebMvcCommonConfig.java
    ├── constant/
    │   ├── HeaderConstants.java
    │   ├── SecurityConstants.java
    │   ├── TaskConstants.java
    │   └── SystemConstants.java
    ├── dto/
    │   ├── TaskDTO.java
    │   ├── TaskFileDTO.java
    │   ├── TaskResultSummaryDTO.java
    │   ├── TaskResultFileDTO.java
    │   ├── NodeDTO.java
    │   ├── SolverDTO.java
    │   ├── SolverProfileDTO.java
    │   ├── FileRuleDTO.java
    │   └── UserContextDTO.java
    ├── enums/
    │   ├── TaskStatusEnum.java
    │   ├── FailTypeEnum.java
    │   ├── NodeStatusEnum.java
    │   ├── RoleCodeEnum.java
    │   ├── OperatorTypeEnum.java
    │   ├── FileRoleEnum.java
    │   └── ResultFileTypeEnum.java
    ├── exception/
    │   ├── BizException.java
    │   ├── UnauthorizedException.java
    │   ├── ForbiddenException.java
    │   ├── NotFoundException.java
    │   └── GlobalExceptionHandler.java
    ├── response/
    │   ├── Result.java
    │   └── PageResult.java
    ├── utils/
    │   ├── JwtUtil.java
    │   ├── DateTimeUtil.java
    │   ├── JsonUtil.java
    │   ├── IdGenerator.java
    │   ├── BeanCopyUtil.java
    │   └── FileNameUtil.java
    └── model/
        ├── BasePageQuery.java
        └── LoginUser.java
```

## 2. 关键原则

- 不放各服务自己的 `entity`
- 不放各服务自己的 `mapper`
- 不放业务很强的 `service`
- DTO 只保留“跨服务调用需要”的对象

---------

# 二、common-lib 初始化代码骨架清单

`common-lib` 最先建，因为所有服务都依赖它。

## 1. 必建类

### `response/Result.java`

职责：统一返回结构

建议方法：

* `success()`
* `success(T data)`
* `fail(Integer code, String message)`

---

### `response/PageResult.java`

职责：统一分页结果

建议方法：

* `of(long total, long pageNum, long pageSize, List<T> records)`

---

### `exception/BizException.java`

职责：业务异常基类

建议方法：

* 构造函数 `BizException(String message)`
* 构造函数 `BizException(Integer code, String message)`

---

### `exception/UnauthorizedException.java`

职责：未登录异常

---

### `exception/ForbiddenException.java`

职责：无权限异常

---

### `enums/TaskStatusEnum.java`

职责：统一任务状态

建议方法：

* `isFinished()`
* `canCancel()`

---

### `enums/FailTypeEnum.java`

职责：统一失败类型

---

### `enums/NodeStatusEnum.java`

职责：节点状态枚举

---

### `enums/RoleCodeEnum.java`

职责：角色枚举

---

### `constant/HeaderConstants.java`

职责：统一请求头常量

建议字段：

* `AUTHORIZATION`
* `X_USER_ID`
* `X_ROLE_CODE`
* `X_NODE_TOKEN`

---

### `utils/JwtUtil.java`

职责：token 生成与解析

建议方法：

* `generateToken(Long userId, String roleCode)`
* `parseUserId(String token)`
* `parseRoleCode(String token)`
* `validateToken(String token)`

---

### `utils/JsonUtil.java`

职责：JSON 序列化/反序列化

建议方法：

* `toJson(Object obj)`
* `fromJson(String text, Class<T> clazz)`

---

### `utils/IdGenerator.java`

职责：生成任务编号、节点编码辅助值

建议方法：

* `nextTaskNo()`
* `nextBizId()`

---

### `dto/UserContextDTO.java`

职责：跨服务透传用户上下文

字段建议：

* `userId`
* `username`
* `roleCode`

---

### `dto/TaskDTO.java`

职责：调度服务、node-agent 间透传任务数据

字段建议：

* `taskId`
* `taskNo`
* `taskName`
* `solverId`
* `profileId`
* `taskType`
* `paramsJson`
* `nodeId`

---

### `dto/NodeDTO.java`

职责：节点注册、心跳、节点展示

字段建议：

* `nodeId`
* `nodeCode`
* `host`
* `port`
* `cpuUsage`
* `memoryUsage`
* `runningCount`

---------

下面我把 **`common-lib` 的完整初始化代码骨架** 补齐。它是整个后端工程最先要稳定下来的模块，因为你当前这套后端设计已经明确采用多模块微服务结构，且 `common-lib` 负责承载统一返回结构、公共枚举、DTO、异常、工具类等跨服务共享能力。

这部分我会重点解决两件事：

1. **哪些东西应该放进 common-lib**
2. **哪些东西绝对不要放进去**

否则后面很容易把 `common-lib` 写成“公共垃圾场”。

------

# 一、common-lib 的定位

`common-lib` 的唯一目标是：

> **给多个微服务提供真正通用、稳定、低业务耦合的基础能力**

根据你现在的后端设计，它至少应该承担这些职责：

- 统一返回结构
- 统一异常模型
- 公共枚举
- 跨服务 DTO
- 公共常量
- 基础工具类
- 少量通用配置

所以它应该是：

- **小而稳**
- **依赖少**
- **不要掺杂具体业务**

------

# 二、哪些东西该放，哪些不该放

## 1. 应该放进 common-lib 的

这些是推荐放进去的：

- `Result / PageResult`
- `BizException / UnauthorizedException / ForbiddenException`
- `TaskStatusEnum / FailTypeEnum / NodeStatusEnum / RoleCodeEnum`
- `TaskDTO / NodeDTO / SolverProfileDTO / FileRuleDTO / UserContextDTO`
- `HeaderConstants / SecurityConstants`
- `JwtUtil / JsonUtil / DateTimeUtil / IdGenerator`
- 一些轻量级基础配置

## 2. 不应该放进 common-lib 的

这些不要放：

- 各服务自己的数据库实体类 `TaskPO / UserPO / ComputeNodePO`
- 各服务自己的 Request / Response
- 各服务自己的 Mapper
- 各服务自己的 Repository
- 强业务逻辑 Service
- 依赖特定服务的数据结构

## 3. 一个实用判断标准

如果一个类满足下面任意一个条件，就**不要放 common-lib**：

- 只被一个服务使用
- 明显依赖某张具体业务表
- 改动频率很高
- 改它会牵一大片服务一起改

------

# 三、common-lib 最终推荐包树

建议统一成下面这版：

```text
common-lib/
└── src/main/java/com/example/cae/common/
    ├── config/
    │   ├── JacksonConfig.java
    │   └── WebMvcCommonConfig.java
    ├── constant/
    │   ├── HeaderConstants.java
    │   ├── SecurityConstants.java
    │   ├── TaskConstants.java
    │   └── SystemConstants.java
    ├── dto/
    │   ├── TaskDTO.java
    │   ├── TaskFileDTO.java
    │   ├── TaskResultSummaryDTO.java
    │   ├── TaskResultFileDTO.java
    │   ├── NodeDTO.java
    │   ├── SolverDTO.java
    │   ├── SolverProfileDTO.java
    │   ├── FileRuleDTO.java
    │   └── UserContextDTO.java
    ├── enums/
    │   ├── TaskStatusEnum.java
    │   ├── FailTypeEnum.java
    │   ├── NodeStatusEnum.java
    │   ├── RoleCodeEnum.java
    │   ├── OperatorTypeEnum.java
    │   ├── FileRoleEnum.java
    │   └── ResultFileTypeEnum.java
    ├── exception/
    │   ├── BizException.java
    │   ├── UnauthorizedException.java
    │   ├── ForbiddenException.java
    │   ├── NotFoundException.java
    │   └── GlobalExceptionHandler.java
    ├── model/
    │   ├── BasePageQuery.java
    │   └── LoginUser.java
    ├── response/
    │   ├── Result.java
    │   └── PageResult.java
    └── utils/
        ├── JwtUtil.java
        ├── DateTimeUtil.java
        ├── JsonUtil.java
        ├── IdGenerator.java
        ├── BeanCopyUtil.java
        └── FileNameUtil.java
```

这版和你现在的后端设计里“`config / constant / dto / enums / exception / response / utils`”的方向一致，只是在工程层面进一步细化了。

------

# 四、response 层完整骨架

------

## 1. Result.java

这是全系统统一返回结构，建议直接固定下来。

```java
package com.example.cae.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(0, "success", data);
    }

    public static Result<Void> success() {
        return new Result<>(0, "success", null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }
}
```

### 说明

这里统一采用：

- `code = 0` 表示成功
- `code != 0` 表示失败

这和你前面已经统一过的接口/前端/后端终稿口径保持一致。虽然旧版本骨架里出现过 `200/500` 和不完整状态枚举，但你现在已经在修订中统一成完整版本了，所以这里应固定为统一口径。

------

## 2. PageResult.java

```java
package com.example.cae.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {

    private Long total;
    private Long pageNum;
    private Long pageSize;
    private List<T> records;

    public static <T> PageResult<T> of(Long total, Long pageNum, Long pageSize, List<T> records) {
        return new PageResult<>(total, pageNum, pageSize, records);
    }
}
```

------

# 五、exception 层完整骨架

------

## 1. BizException.java

```java
package com.example.cae.common.exception;

public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(String message) {
        super(message);
        this.code = 500;
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
```

------

## 2. UnauthorizedException.java

```java
package com.example.cae.common.exception;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
```

------

## 3. ForbiddenException.java

```java
package com.example.cae.common.exception;

public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
```

------

## 4. NotFoundException.java

```java
package com.example.cae.common.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
```

------

## 5. GlobalExceptionHandler.java

这个类可以放公共模块，也可以放到各服务单独覆盖。
如果你想统一一版基础处理，推荐这样写：

```java
package com.example.cae.common.exception;

import com.example.cae.common.response.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException ex) {
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public Result<Void> handleUnauthorizedException(UnauthorizedException ex) {
        return Result.fail(401, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public Result<Void> handleForbiddenException(ForbiddenException ex) {
        return Result.fail(403, ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public Result<Void> handleNotFoundException(NotFoundException ex) {
        return Result.fail(404, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        return Result.fail(500, ex.getMessage());
    }
}
```

------

# 六、enums 层完整骨架

你当前后端设计里已经明确至少要有：

- `TaskStatusEnum`
- `FailTypeEnum`
- `NodeStatusEnum`

我建议再补齐两个工程上常用的枚举：

- `RoleCodeEnum`
- `OperatorTypeEnum`

------

## 1. TaskStatusEnum.java

```java
package com.example.cae.common.enums;

import java.util.Set;

public enum TaskStatusEnum {
    CREATED,
    VALIDATED,
    QUEUED,
    SCHEDULED,
    DISPATCHED,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELED,
    TIMEOUT;

    public boolean isFinished() {
        return Set.of(SUCCESS, FAILED, CANCELED, TIMEOUT).contains(this);
    }

    public boolean canCancel() {
        return Set.of(CREATED, VALIDATED, QUEUED, RUNNING).contains(this);
    }
}
```

------

## 2. FailTypeEnum.java

```java
package com.example.cae.common.enums;

public enum FailTypeEnum {
    VALIDATION_ERROR,
    DISPATCH_ERROR,
    NODE_OFFLINE,
    EXECUTOR_START_ERROR,
    RUNTIME_ERROR,
    TIMEOUT,
    MANUAL_CANCEL
}
```

------

## 3. NodeStatusEnum.java

```java
package com.example.cae.common.enums;

public enum NodeStatusEnum {
    ONLINE,
    OFFLINE,
    DISABLED
}
```

------

## 4. RoleCodeEnum.java

```java
package com.example.cae.common.enums;

public enum RoleCodeEnum {
    ADMIN,
    USER
}
```

------

## 5. OperatorTypeEnum.java

```java
package com.example.cae.common.enums;

public enum OperatorTypeEnum {
    SYSTEM,
    USER,
    NODE,
    ADMIN
}
```

------

## 6. FileRoleEnum.java

```java
package com.example.cae.common.enums;

public enum FileRoleEnum {
    INPUT,
    CONFIG,
    ARCHIVE
}
```

------

## 7. ResultFileTypeEnum.java

```java
package com.example.cae.common.enums;

public enum ResultFileTypeEnum {
    RESULT,
    LOG,
    REPORT,
    IMAGE
}
```

------

# 七、dto 层完整骨架

这里要特别注意：

> DTO 只放“跨服务调用需要传输”的对象，不放前端接口专用 VO。

------

## 1. UserContextDTO.java

```java
package com.example.cae.common.dto;

import lombok.Data;

@Data
public class UserContextDTO {
    private Long userId;
    private String username;
    private String roleCode;
}
```

------

## 2. TaskDTO.java

`scheduler-service` 调度、`node-agent` 执行最常用。

```java
package com.example.cae.common.dto;

import lombok.Data;

import java.util.Map;

@Data
public class TaskDTO {
    private Long taskId;
    private String taskNo;
    private String taskName;
    private Long solverId;
    private String solverCode;
    private Long profileId;
    private String taskType;
    private Long nodeId;
    private String commandTemplate;
    private String parserName;
    private Integer timeoutSeconds;
    private Map<String, Object> paramsJson;
}
```

------

## 3. TaskFileDTO.java

```java
package com.example.cae.common.dto;

import lombok.Data;

@Data
public class TaskFileDTO {
    private Long taskId;
    private String fileKey;
    private String originName;
    private String storagePath;
    private Long fileSize;
}
```

------

## 4. TaskResultSummaryDTO.java

```java
package com.example.cae.common.dto;

import lombok.Data;

@Data
public class TaskResultSummaryDTO {
    private Long taskId;
    private Boolean success;
    private Integer durationSeconds;
    private String summaryText;
    private String metricsJson;
}
```

------

## 5. TaskResultFileDTO.java

```java
package com.example.cae.common.dto;

import lombok.Data;

@Data
public class TaskResultFileDTO {
    private Long taskId;
    private String fileType;
    private String fileName;
    private String storagePath;
    private Long fileSize;
}
```

------

## 6. NodeDTO.java

```java
package com.example.cae.common.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class NodeDTO {
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
    private List<Long> solverIds;
}
```

------

## 7. SolverDTO.java

```java
package com.example.cae.common.dto;

import lombok.Data;

@Data
public class SolverDTO {
    private Long solverId;
    private String solverCode;
    private String solverName;
    private String version;
    private String execMode;
    private String execPath;
    private Integer enabled;
}
```

------

## 8. SolverProfileDTO.java

```java
package com.example.cae.common.dto;

import lombok.Data;

@Data
public class SolverProfileDTO {
    private Long profileId;
    private Long solverId;
    private String profileCode;
    private String taskType;
    private String profileName;
    private String commandTemplate;
    private String parserName;
    private Integer timeoutSeconds;
    private Integer enabled;
}
```

------

## 9. FileRuleDTO.java

```java
package com.example.cae.common.dto;

import lombok.Data;

@Data
public class FileRuleDTO {
    private Long ruleId;
    private Long profileId;
    private String fileKey;
    private String fileNamePattern;
    private String fileType;
    private Integer requiredFlag;
    private Integer sortOrder;
    private String remark;
}
```

------

# 八、constant 层完整骨架

------

## 1. HeaderConstants.java

```java
package com.example.cae.common.constant;

public interface HeaderConstants {
    String AUTHORIZATION = "Authorization";
    String X_USER_ID = "X-User-Id";
    String X_ROLE_CODE = "X-Role-Code";
    String X_NODE_TOKEN = "X-Node-Token";
    String TRACE_ID = "X-Trace-Id";
}
```

------

## 2. SecurityConstants.java

```java
package com.example.cae.common.constant;

public interface SecurityConstants {
    String TOKEN_PREFIX = "Bearer ";
    String ROLE_ADMIN = "ADMIN";
    String ROLE_USER = "USER";
}
```

------

## 3. TaskConstants.java

```java
package com.example.cae.common.constant;

public interface TaskConstants {
    Integer DEFAULT_PRIORITY = 0;
    Integer DEFAULT_TIMEOUT_SECONDS = 3600;
    Integer DEFAULT_LOG_PAGE_SIZE = 100;
}
```

------

## 4. SystemConstants.java

```java
package com.example.cae.common.constant;

public interface SystemConstants {
    Long DEFAULT_PAGE_NUM = 1L;
    Long DEFAULT_PAGE_SIZE = 10L;
}
```

------

# 九、model 层完整骨架

这个目录只放特别通用的基础模型。

------

## 1. BasePageQuery.java

```java
package com.example.cae.common.model;

import lombok.Data;

@Data
public class BasePageQuery {
    private Long pageNum = 1L;
    private Long pageSize = 10L;
}
```

------

## 2. LoginUser.java

```java
package com.example.cae.common.model;

import lombok.Data;

@Data
public class LoginUser {
    private Long userId;
    private String username;
    private String roleCode;
}
```

------

# 十、utils 层完整骨架

------

## 1. JwtUtil.java

你当前设计文档里提到过“简化版 mock-token”，同时也说明正式实现可替换为标准 JWT。这里建议直接做成**接口不变、实现可切换**的工具类。

```java
package com.example.cae.common.utils;

public class JwtUtil {

    private JwtUtil() {
    }

    public static String generateToken(Long userId, String roleCode) {
        return "mock-token-" + userId + "-" + roleCode;
    }

    public static Long parseUserId(String token) {
        String text = token.replace("mock-token-", "");
        String[] arr = text.split("-");
        return Long.parseLong(arr[0]);
    }

    public static String parseRoleCode(String token) {
        String text = token.replace("mock-token-", "");
        String[] arr = text.split("-");
        return arr.length > 1 ? arr[1] : "USER";
    }

    public static boolean validateToken(String token) {
        return token != null && token.startsWith("mock-token-");
    }
}
```

------

## 2. DateTimeUtil.java

```java
package com.example.cae.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {

    private static final DateTimeFormatter DEFAULT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeUtil() {
    }

    public static String format(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.format(DEFAULT_FORMATTER);
    }

    public static LocalDateTime parse(String text) {
        return LocalDateTime.parse(text, DEFAULT_FORMATTER);
    }
}
```

------

## 3. JsonUtil.java

```java
package com.example.cae.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtil() {
    }

    public static String toJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("json serialize failed", e);
        }
    }

    public static <T> T fromJson(String text, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(text, clazz);
        } catch (Exception e) {
            throw new RuntimeException("json deserialize failed", e);
        }
    }
}
```

------

## 4. IdGenerator.java

```java
package com.example.cae.common.utils;

public class IdGenerator {

    private IdGenerator() {
    }

    public static String nextTaskNo() {
        return "TASK" + System.currentTimeMillis();
    }

    public static String nextNodeCode() {
        return "NODE" + System.currentTimeMillis();
    }
}
```

------

## 5. BeanCopyUtil.java

```java
package com.example.cae.common.utils;

import org.springframework.beans.BeanUtils;

public class BeanCopyUtil {

    private BeanCopyUtil() {
    }

    public static <T> T copy(Object source, Class<T> targetClass) {
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("bean copy failed", e);
        }
    }
}
```

------

## 6. FileNameUtil.java

```java
package com.example.cae.common.utils;

public class FileNameUtil {

    private FileNameUtil() {
    }

    public static String getSuffix(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
}
```

------

# 十一、config 层完整骨架

`common-lib` 里的配置尽量少，只留通用基础配置。

------

## 1. JacksonConfig.java

```java
package com.example.cae.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
```

------

## 2. WebMvcCommonConfig.java

如果后面你需要全局时间格式、跨域等轻量配置，可以放这里；没有明确需求时先留空壳即可。

```java
package com.example.cae.common.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class WebMvcCommonConfig {
}
```

------

# 十二、common-lib 最小可运行版本

如果你现在不想一次把 common-lib 铺太大，建议先只建下面这些类：

1. `Result`
2. `PageResult`
3. `BizException`
4. `TaskStatusEnum`
5. `FailTypeEnum`
6. `NodeStatusEnum`
7. `RoleCodeEnum`
8. `TaskDTO`
9. `NodeDTO`
10. `SolverProfileDTO`
11. `HeaderConstants`
12. `JwtUtil`
13. `JsonUtil`
14. `IdGenerator`

这 14 个已经足够支撑你继续写：

- gateway-service
- user-service
- task-service
- scheduler-service
- node-agent

------

# 十三、common-lib 的使用规范

这一段非常重要，建议你后面真的按这个约束执行。

## 1. 所有服务都依赖 common-lib，但不要反过来依赖业务服务

`common-lib` 必须是基础模块，不能出现：

- 依赖 `task-service`
- 依赖 `user-service`

## 2. DTO 只保留跨服务对象

不要把前端展示对象 `TaskDetailResponse` 放进来。

## 3. 枚举必须全局统一

特别是：

- `TaskStatusEnum`
- `FailTypeEnum`
- `NodeStatusEnum`

这些一旦不统一，前后端和服务间会马上乱。

## 4. 工具类只放纯工具

不要把业务逻辑偷偷塞进 `Utils`。

------

# 十四、你现在最该先建的 10 个类

如果你准备立刻开工，我建议 common-lib 先建这 10 个：

1. `Result`
2. `PageResult`
3. `BizException`
4. `TaskStatusEnum`
5. `FailTypeEnum`
6. `NodeStatusEnum`
7. `RoleCodeEnum`
8. `TaskDTO`
9. `NodeDTO`
10. `JwtUtil`

这 10 个一建完，其他服务就能顺着搭起来。

------

# 十五、最终建议

`common-lib` 最怕的不是“写少了”，而是“写杂了”。

你现在这版最稳的策略是：

- **response** 放统一返回
- **exception** 放统一异常
- **enums** 放全局枚举
- **dto** 放跨服务传输对象
- **constant** 放头信息和基础常量
- **utils** 放纯工具
- **config** 只放轻量基础配置

这样后面整个工程会非常清爽，而且和你现有的后端总体设计、微服务划分、调用关系也完全一致。



-------

