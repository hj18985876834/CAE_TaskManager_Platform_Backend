# 六、solver-service 项目结构设计

这个服务是“配置中心型服务”，重点在模板、规则、求解器定义。

## 1. 推荐结构

```text
solver-service/
└── src/main/java/com/yourorg/solver/
    ├── SolverApplication.java
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
    │   └── support/
    ├── config/
    └── support/
```

## 2. 推荐子模块职责

### interfaces/controller

- `SolverController`
- `ProfileController`
- `FileRuleController`

### application/service

- `SolverAppService`
- `ProfileAppService`
- `FileRuleAppService`
- `UploadSpecAppService`

### domain/model

- `SolverDefinition`
- `SolverTaskProfile`
- `SolverProfileFileRule`

### domain/service

建议单独抽：

- `ProfileRuleDomainService`

它负责：

- 模板规则组合
- 上传规范组装
- 模板启停可用性判断

### infrastructure/support

可以放：

- `CommandTemplateResolver`
- `ProfileRuleValidator`

## 3. 这个服务的关键规范

建议明确：

- **模板是核心聚合根**
- 文件规则围绕模板管理
- 求解器定义与模板是一对多

这样你在代码里就会更清楚：

- 修改模板时要不要级联规则
- 删除模板时如何校验依赖
- 获取上传规范时从哪里组装数据

-----------

# 六、solver-service 完整包树

这个服务是“求解器配置中心”，负责求解器、模板、文件规则。后端设计已经把它作为系统扩展性的核心。

## 1. 完整结构

```text
solver-service/
└── src/main/java/com/example/cae/solver/
    ├── SolverApplication.java
    ├── interfaces/
    │   ├── controller/
    │   │   ├── SolverController.java
    │   │   ├── ProfileController.java
    │   │   └── FileRuleController.java
    │   ├── request/
    │   │   ├── CreateSolverRequest.java
    │   │   ├── UpdateSolverRequest.java
    │   │   ├── UpdateSolverStatusRequest.java
    │   │   ├── CreateProfileRequest.java
    │   │   ├── UpdateProfileRequest.java
    │   │   ├── CreateFileRuleRequest.java
    │   │   └── UpdateFileRuleRequest.java
    │   └── response/
    │       ├── SolverListItemResponse.java
    │       ├── SolverDetailResponse.java
    │       ├── ProfileListItemResponse.java
    │       ├── ProfileDetailResponse.java
    │       ├── FileRuleResponse.java
    │       └── UploadSpecResponse.java
    ├── application/
    │   ├── service/
    │   │   ├── SolverAppService.java
    │   │   ├── ProfileAppService.java
    │   │   ├── FileRuleAppService.java
    │   │   └── UploadSpecAppService.java
    │   ├── facade/
    │   │   ├── SolverFacade.java
    │   │   └── ProfileFacade.java
    │   └── assembler/
    │       ├── SolverAssembler.java
    │       ├── ProfileAssembler.java
    │       └── FileRuleAssembler.java
    ├── domain/
    │   ├── model/
    │   │   ├── SolverDefinition.java
    │   │   ├── SolverTaskProfile.java
    │   │   └── SolverProfileFileRule.java
    │   ├── repository/
    │   │   ├── SolverRepository.java
    │   │   ├── ProfileRepository.java
    │   │   └── FileRuleRepository.java
    │   ├── service/
    │   │   ├── SolverDomainService.java
    │   │   └── ProfileRuleDomainService.java
    │   └── enums/
    │       ├── SolverExecModeEnum.java
    │       └── RuleFileTypeEnum.java
    ├── infrastructure/
    │   ├── persistence/
    │   │   ├── entity/
    │   │   │   ├── SolverDefinitionPO.java
    │   │   │   ├── SolverTaskProfilePO.java
    │   │   │   └── SolverProfileFileRulePO.java
    │   │   ├── mapper/
    │   │   │   ├── SolverDefinitionMapper.java
    │   │   │   ├── SolverTaskProfileMapper.java
    │   │   │   └── SolverProfileFileRuleMapper.java
    │   │   └── repository/
    │   │       ├── SolverRepositoryImpl.java
    │   │       ├── ProfileRepositoryImpl.java
    │   │       └── FileRuleRepositoryImpl.java
    │   └── support/
    │       ├── UploadSpecBuilder.java
    │       ├── ProfileRuleValidator.java
    │       └── CommandTemplateResolver.java
    ├── config/
    │   └── SolverServiceConfig.java
    └── support/
        └── SolverQueryBuilder.java
```

## 2. 最关键的几个类

- `ProfileController`
- `FileRuleController`
- `UploadSpecAppService`
- `ProfileRuleDomainService`
- `UploadSpecBuilder`

-------------

# 五、solver-service 初始化代码骨架清单

你的后端设计里已经明确：这个服务是“扩展能力核心”，新增求解器尽量通过配置完成，不通过改代码完成。

## 1. Controller

### `interfaces/controller/SolverController.java`

职责：求解器管理

建议方法：

* `pageSolvers(SolverPageQueryRequest request)`
* `getSolverDetail(Long solverId)`
* `createSolver(CreateSolverRequest request)`
* `updateSolver(Long solverId, UpdateSolverRequest request)`
* `updateSolverStatus(Long solverId, UpdateSolverStatusRequest request)`

---

### `interfaces/controller/ProfileController.java`

职责：模板管理

建议方法：

* `pageProfiles(ProfilePageQueryRequest request)`
* `getProfileDetail(Long profileId)`
* `createProfile(CreateProfileRequest request)`
* `updateProfile(Long profileId, UpdateProfileRequest request)`
* `updateProfileStatus(Long profileId, UpdateProfileStatusRequest request)`
* `getUploadSpec(Long profileId)`

---

### `interfaces/controller/FileRuleController.java`

职责：模板文件规则管理

建议方法：

* `listFileRules(Long profileId)`
* `createFileRule(Long profileId, CreateFileRuleRequest request)`
* `updateFileRule(Long ruleId, UpdateFileRuleRequest request)`
* `deleteFileRule(Long ruleId)`

---

## 2. Application

### `application/service/SolverAppService.java`

建议方法：

* `pageSolvers`
* `getSolverDetail`
* `createSolver`
* `updateSolver`
* `updateSolverStatus`

---

### `application/service/ProfileAppService.java`

建议方法：

* `pageProfiles`
* `getProfileDetail`
* `createProfile`
* `updateProfile`
* `updateProfileStatus`

---

### `application/service/FileRuleAppService.java`

建议方法：

* `listFileRules`
* `createFileRule`
* `updateFileRule`
* `deleteFileRule`

---

### `application/service/UploadSpecAppService.java`

职责：组装上传规范

建议方法：

* `buildUploadSpec(Long profileId)`

---

### `application/assembler/ProfileAssembler.java`

### `application/assembler/FileRuleAssembler.java`

---

## 3. Domain

### `domain/model/SolverDefinition.java`

建议方法：

* `enable()`
* `disable()`

---

### `domain/model/SolverTaskProfile.java`

建议方法：

* `enable()`
* `disable()`
* `changeTimeout(Integer timeoutSeconds)`

---

### `domain/model/SolverProfileFileRule.java`

建议方法：

* `isRequired()`
* `matches(String fileName)`

---

### `domain/repository/SolverRepository.java`

### `domain/repository/ProfileRepository.java`

### `domain/repository/FileRuleRepository.java`

建议方法：

* `findById`
* `save`
* `update`
* `page`
* `listByProfileId`

---

### `domain/service/ProfileRuleDomainService.java`

职责：模板规则聚合

建议方法：

* `buildUploadSpec(SolverTaskProfile profile, List<SolverProfileFileRule> rules)`
* `checkProfileEnabled(SolverTaskProfile profile)`
* `checkRuleConflict(List<SolverProfileFileRule> rules)`

---

## 4. Infrastructure

### `infrastructure/support/UploadSpecBuilder.java`

职责：真正拼装前端上传规范

建议方法：

* `build(...)`

---

### `infrastructure/support/ProfileRuleValidator.java`

职责：规则合法性检查

建议方法：

* `validateCreateRule(...)`
* `validateUpdateRule(...)`

---

### `infrastructure/support/CommandTemplateResolver.java`

职责：命令模板解析

建议方法：

* `resolve(String commandTemplate, Map<String, Object> params)`

-----------

下面我继续给你 **`solver-service` 的完整初始化代码骨架**，保持和前面几个服务同样的粒度，并且严格对齐你现在已经统一后的整体设计口径：

- `solver-service` 负责 **求解器定义管理、模板管理、文件规则管理、上传规范生成**
- 它是系统的 **扩展能力核心**
- 核心思想是：**新增求解器尽量通过“配数据 + 少量适配代码”完成，而不是改 task-service 主流程**
- `task-service` 通过它查询 `profile + fileRules + uploadSpec` 来完成任务校验与引导上传。

------

# 一、solver-service 的定位

`solver-service` 负责四类核心能力：

1. 维护平台支持哪些求解器
2. 维护“求解器 + 任务类型”的模板
3. 维护模板所要求的输入文件规则
4. 为前端和 `task-service` 生成统一的上传规范与模板信息

它不是执行服务，不负责跑任务；也不是任务服务，不负责存任务状态。
它更像系统里的 **“求解器配置中心 + 模板中心”**。

------

# 二、solver-service 最终推荐包树

```text
solver-service/
└── src/main/java/com/example/cae/solver/
    ├── SolverApplication.java
    ├── interfaces/
    │   ├── controller/
    │   │   ├── SolverController.java
    │   │   ├── ProfileController.java
    │   │   └── FileRuleController.java
    │   ├── request/
    │   │   ├── CreateSolverRequest.java
    │   │   ├── UpdateSolverRequest.java
    │   │   ├── UpdateSolverStatusRequest.java
    │   │   ├── SolverPageQueryRequest.java
    │   │   ├── CreateProfileRequest.java
    │   │   ├── UpdateProfileRequest.java
    │   │   ├── UpdateProfileStatusRequest.java
    │   │   ├── ProfilePageQueryRequest.java
    │   │   ├── CreateFileRuleRequest.java
    │   │   └── UpdateFileRuleRequest.java
    │   └── response/
    │       ├── SolverListItemResponse.java
    │       ├── SolverDetailResponse.java
    │       ├── ProfileListItemResponse.java
    │       ├── ProfileDetailResponse.java
    │       ├── FileRuleResponse.java
    │       └── UploadSpecResponse.java
    ├── application/
    │   ├── service/
    │   │   ├── SolverAppService.java
    │   │   ├── ProfileAppService.java
    │   │   ├── FileRuleAppService.java
    │   │   └── UploadSpecAppService.java
    │   ├── facade/
    │   │   ├── SolverFacade.java
    │   │   └── ProfileFacade.java
    │   └── assembler/
    │       ├── SolverAssembler.java
    │       ├── ProfileAssembler.java
    │       └── FileRuleAssembler.java
    ├── domain/
    │   ├── model/
    │   │   ├── SolverDefinition.java
    │   │   ├── SolverTaskProfile.java
    │   │   └── SolverProfileFileRule.java
    │   ├── repository/
    │   │   ├── SolverRepository.java
    │   │   ├── ProfileRepository.java
    │   │   └── FileRuleRepository.java
    │   ├── service/
    │   │   ├── SolverDomainService.java
    │   │   └── ProfileRuleDomainService.java
    │   └── enums/
    │       ├── SolverExecModeEnum.java
    │       └── RuleFileTypeEnum.java
    ├── infrastructure/
    │   ├── persistence/
    │   │   ├── entity/
    │   │   │   ├── SolverDefinitionPO.java
    │   │   │   ├── SolverTaskProfilePO.java
    │   │   │   └── SolverProfileFileRulePO.java
    │   │   ├── mapper/
    │   │   │   ├── SolverDefinitionMapper.java
    │   │   │   ├── SolverTaskProfileMapper.java
    │   │   │   └── SolverProfileFileRuleMapper.java
    │   │   └── repository/
    │   │       ├── SolverRepositoryImpl.java
    │   │       ├── ProfileRepositoryImpl.java
    │   │       └── FileRuleRepositoryImpl.java
    │   └── support/
    │       ├── UploadSpecBuilder.java
    │       ├── ProfileRuleValidator.java
    │       └── CommandTemplateResolver.java
    ├── config/
    │   ├── SolverServiceConfig.java
    │   └── MybatisPlusConfig.java
    └── support/
        └── SolverQueryBuilder.java
```

这版结构对应你前面已经统一的后端设计里“`SolverController / ProfileController / FileRuleController` 三核心模块”和“模板查询主链：`solver -> profiles -> fileRules`”的思路。

------

# 三、solver-service 的核心调用链

## 1. 求解器管理链路

```
SolverController -> SolverAppService -> SolverRepository
```

## 2. 模板管理链路

```
ProfileController -> ProfileAppService -> ProfileRepository
```

## 3. 文件规则管理链路

```
FileRuleController -> FileRuleAppService -> FileRuleRepository
```

## 4. 上传规范生成链路

```
ProfileController -> UploadSpecAppService -> ProfileRuleDomainService -> UploadSpecBuilder
```

其中第 4 条最重要，因为前端“选模板后展示上传规范”、`task-service`“按模板校验文件”都依赖它。

------

# 四、数据库对象对应关系

按照你已经统一后的数据库设计，`solver-service` 对应 3 张核心表：

- `solver_definition`
- `solver_task_profile`
- `solver_profile_file_rule`

这正好对应 3 个核心领域对象：

- `SolverDefinition`
- `SolverTaskProfile`
- `SolverProfileFileRule`

------

# 五、request / response 设计

## 1. interfaces/request

### CreateSolverRequest.java

```java
@Data
public class CreateSolverRequest {
    private String solverCode;
    private String solverName;
    private String version;
    private String execMode;
    private String execPath;
    private String remark;
}
```

### UpdateSolverRequest.java

```java
@Data
public class UpdateSolverRequest {
    private String solverName;
    private String version;
    private String execMode;
    private String execPath;
    private String remark;
}
```

### UpdateSolverStatusRequest.java

```java
@Data
public class UpdateSolverStatusRequest {
    private Integer enabled;
}
```

### SolverPageQueryRequest.java

```java
@Data
public class SolverPageQueryRequest {
    private Integer pageNum;
    private Integer pageSize;
    private String solverCode;
    private String solverName;
    private Integer enabled;
}
```

### CreateProfileRequest.java

```java
@Data
public class CreateProfileRequest {
    private Long solverId;
    private String profileCode;
    private String taskType;
    private String profileName;
    private String commandTemplate;
    private String parserName;
    private Integer timeoutSeconds;
}
```

### UpdateProfileRequest.java

```java
@Data
public class UpdateProfileRequest {
    private String taskType;
    private String profileName;
    private String commandTemplate;
    private String parserName;
    private Integer timeoutSeconds;
}
```

### UpdateProfileStatusRequest.java

```java
@Data
public class UpdateProfileStatusRequest {
    private Integer enabled;
}
```

### ProfilePageQueryRequest.java

```java
@Data
public class ProfilePageQueryRequest {
    private Integer pageNum;
    private Integer pageSize;
    private Long solverId;
    private String taskType;
    private String profileCode;
    private Integer enabled;
}
```

### CreateFileRuleRequest.java

```java
@Data
public class CreateFileRuleRequest {
    private String fileKey;
    private String fileNamePattern;
    private String fileType;
    private Integer requiredFlag;
    private Integer sortOrder;
    private String remark;
}
```

### UpdateFileRuleRequest.java

```java
@Data
public class UpdateFileRuleRequest {
    private String fileNamePattern;
    private String fileType;
    private Integer requiredFlag;
    private Integer sortOrder;
    private String remark;
}
```

------

## 2. interfaces/response

### SolverListItemResponse.java

```java
@Data
public class SolverListItemResponse {
    private Long solverId;
    private String solverCode;
    private String solverName;
    private String version;
    private String execMode;
    private Integer enabled;
}
```

### SolverDetailResponse.java

```java
@Data
public class SolverDetailResponse {
    private Long solverId;
    private String solverCode;
    private String solverName;
    private String version;
    private String execMode;
    private String execPath;
    private String remark;
    private Integer enabled;
}
```

### ProfileListItemResponse.java

```java
@Data
public class ProfileListItemResponse {
    private Long profileId;
    private Long solverId;
    private String profileCode;
    private String taskType;
    private String profileName;
    private Integer timeoutSeconds;
    private Integer enabled;
}
```

### ProfileDetailResponse.java

```java
@Data
public class ProfileDetailResponse {
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

### FileRuleResponse.java

```java
@Data
public class FileRuleResponse {
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

### UploadSpecResponse.java

```java
@Data
public class UploadSpecResponse {
    private Long profileId;
    private String profileCode;
    private String taskType;
    private String profileName;
    private Integer timeoutSeconds;
    private List<FileRuleResponse> requiredFiles;
    private List<FileRuleResponse> optionalFiles;
}
```

------

# 六、Controller 层完整骨架

## 1. SolverController.java

```java
@RestController
@RequestMapping("/api/solvers")
@RequiredArgsConstructor
public class SolverController {

    private final SolverAppService solverAppService;

    @GetMapping
    public Result<PageResult<SolverListItemResponse>> pageSolvers(SolverPageQueryRequest request) {
        return Result.success(solverAppService.pageSolvers(request));
    }

    @GetMapping("/{solverId}")
    public Result<SolverDetailResponse> getSolverDetail(@PathVariable Long solverId) {
        return Result.success(solverAppService.getSolverDetail(solverId));
    }

    @PostMapping
    public Result<Void> createSolver(@RequestBody CreateSolverRequest request) {
        solverAppService.createSolver(request);
        return Result.success();
    }

    @PutMapping("/{solverId}")
    public Result<Void> updateSolver(@PathVariable Long solverId,
                                     @RequestBody UpdateSolverRequest request) {
        solverAppService.updateSolver(solverId, request);
        return Result.success();
    }

    @PutMapping("/{solverId}/status")
    public Result<Void> updateSolverStatus(@PathVariable Long solverId,
                                           @RequestBody UpdateSolverStatusRequest request) {
        solverAppService.updateSolverStatus(solverId, request);
        return Result.success();
    }

    @GetMapping("/{solverId}/task-options")
    public Result<List<ProfileListItemResponse>> getSolverTaskOptions(@PathVariable Long solverId) {
        return Result.success(solverAppService.getSolverTaskOptions(solverId));
    }
}
```

------

## 2. ProfileController.java

```java
@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileAppService profileAppService;
    private final UploadSpecAppService uploadSpecAppService;

    @GetMapping
    public Result<PageResult<ProfileListItemResponse>> pageProfiles(ProfilePageQueryRequest request) {
        return Result.success(profileAppService.pageProfiles(request));
    }

    @GetMapping("/{profileId}")
    public Result<ProfileDetailResponse> getProfileDetail(@PathVariable Long profileId) {
        return Result.success(profileAppService.getProfileDetail(profileId));
    }

    @PostMapping
    public Result<Void> createProfile(@RequestBody CreateProfileRequest request) {
        profileAppService.createProfile(request);
        return Result.success();
    }

    @PutMapping("/{profileId}")
    public Result<Void> updateProfile(@PathVariable Long profileId,
                                      @RequestBody UpdateProfileRequest request) {
        profileAppService.updateProfile(profileId, request);
        return Result.success();
    }

    @PutMapping("/{profileId}/status")
    public Result<Void> updateProfileStatus(@PathVariable Long profileId,
                                            @RequestBody UpdateProfileStatusRequest request) {
        profileAppService.updateProfileStatus(profileId, request);
        return Result.success();
    }

    @GetMapping("/{profileId}/upload-spec")
    public Result<UploadSpecResponse> getUploadSpec(@PathVariable Long profileId) {
        return Result.success(uploadSpecAppService.buildUploadSpec(profileId));
    }

    @GetMapping("/{profileId}/file-rules")
    public Result<List<FileRuleResponse>> getFileRules(@PathVariable Long profileId) {
        return Result.success(profileAppService.getFileRules(profileId));
    }
}
```

------

## 3. FileRuleController.java

```java
@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class FileRuleController {

    private final FileRuleAppService fileRuleAppService;

    @PostMapping("/{profileId}/file-rules")
    public Result<Void> createFileRule(@PathVariable Long profileId,
                                       @RequestBody CreateFileRuleRequest request) {
        fileRuleAppService.createFileRule(profileId, request);
        return Result.success();
    }

    @PutMapping("/file-rules/{ruleId}")
    public Result<Void> updateFileRule(@PathVariable Long ruleId,
                                       @RequestBody UpdateFileRuleRequest request) {
        fileRuleAppService.updateFileRule(ruleId, request);
        return Result.success();
    }

    @DeleteMapping("/file-rules/{ruleId}")
    public Result<Void> deleteFileRule(@PathVariable Long ruleId) {
        fileRuleAppService.deleteFileRule(ruleId);
        return Result.success();
    }
}
```

这些接口和你已经统一后的接口设计、前端设计、任务校验链路都是匹配的。系统要求管理员能够管理求解器定义、求解器任务模板，并定义必需文件、可选文件、命令模板与结果解析器。

------

# 七、Application 层完整骨架

## 1. SolverAppService.java

```java
@Service
@RequiredArgsConstructor
public class SolverAppService {

    private final SolverRepository solverRepository;
    private final ProfileRepository profileRepository;
    private final SolverDomainService solverDomainService;
    private final SolverAssembler solverAssembler;
    private final ProfileAssembler profileAssembler;

    public PageResult<SolverListItemResponse> pageSolvers(SolverPageQueryRequest request) {
        PageResult<SolverDefinition> page = solverRepository.page(request);
        List<SolverListItemResponse> records = page.getRecords()
                .stream()
                .map(solverAssembler::toListItemResponse)
                .toList();
        return PageResult.of(page.getTotal(), page.getPageNum(), page.getPageSize(), records);
    }

    public SolverDetailResponse getSolverDetail(Long solverId) {
        SolverDefinition solver = solverRepository.findById(solverId);
        if (solver == null) {
            throw new BizException("求解器不存在");
        }
        return solverAssembler.toDetailResponse(solver);
    }

    public void createSolver(CreateSolverRequest request) {
        solverDomainService.checkSolverCodeUnique(request.getSolverCode());
        SolverDefinition solver = solverAssembler.toSolver(request);
        solver.enable();
        solverRepository.save(solver);
    }

    public void updateSolver(Long solverId, UpdateSolverRequest request) {
        SolverDefinition solver = solverRepository.findById(solverId);
        if (solver == null) {
            throw new BizException("求解器不存在");
        }
        solver.setSolverName(request.getSolverName());
        solver.setVersion(request.getVersion());
        solver.setExecMode(request.getExecMode());
        solver.setExecPath(request.getExecPath());
        solver.setRemark(request.getRemark());
        solverRepository.update(solver);
    }

    public void updateSolverStatus(Long solverId, UpdateSolverStatusRequest request) {
        SolverDefinition solver = solverRepository.findById(solverId);
        if (solver == null) {
            throw new BizException("求解器不存在");
        }
        if (request.getEnabled() != null && request.getEnabled() == 1) {
            solver.enable();
        } else {
            solver.disable();
        }
        solverRepository.update(solver);
    }

    public List<ProfileListItemResponse> getSolverTaskOptions(Long solverId) {
        return profileRepository.listEnabledBySolverId(solverId)
                .stream()
                .map(profileAssembler::toListItemResponse)
                .toList();
    }
}
```

------

## 2. ProfileAppService.java

```java
@Service
@RequiredArgsConstructor
public class ProfileAppService {

    private final SolverRepository solverRepository;
    private final ProfileRepository profileRepository;
    private final FileRuleRepository fileRuleRepository;
    private final ProfileRuleDomainService profileRuleDomainService;
    private final ProfileAssembler profileAssembler;
    private final FileRuleAssembler fileRuleAssembler;

    public PageResult<ProfileListItemResponse> pageProfiles(ProfilePageQueryRequest request) {
        PageResult<SolverTaskProfile> page = profileRepository.page(request);
        List<ProfileListItemResponse> records = page.getRecords()
                .stream()
                .map(profileAssembler::toListItemResponse)
                .toList();
        return PageResult.of(page.getTotal(), page.getPageNum(), page.getPageSize(), records);
    }

    public ProfileDetailResponse getProfileDetail(Long profileId) {
        SolverTaskProfile profile = profileRepository.findById(profileId);
        if (profile == null) {
            throw new BizException("模板不存在");
        }
        return profileAssembler.toDetailResponse(profile);
    }

    public void createProfile(CreateProfileRequest request) {
        SolverDefinition solver = solverRepository.findById(request.getSolverId());
        if (solver == null) {
            throw new BizException("所属求解器不存在");
        }
        profileRuleDomainService.checkProfileCodeUnique(request.getSolverId(), request.getProfileCode());

        SolverTaskProfile profile = profileAssembler.toProfile(request);
        profile.enable();
        profileRepository.save(profile);
    }

    public void updateProfile(Long profileId, UpdateProfileRequest request) {
        SolverTaskProfile profile = profileRepository.findById(profileId);
        if (profile == null) {
            throw new BizException("模板不存在");
        }
        profile.setTaskType(request.getTaskType());
        profile.setProfileName(request.getProfileName());
        profile.setCommandTemplate(request.getCommandTemplate());
        profile.setParserName(request.getParserName());
        profile.setTimeoutSeconds(request.getTimeoutSeconds());
        profileRepository.update(profile);
    }

    public void updateProfileStatus(Long profileId, UpdateProfileStatusRequest request) {
        SolverTaskProfile profile = profileRepository.findById(profileId);
        if (profile == null) {
            throw new BizException("模板不存在");
        }
        if (request.getEnabled() != null && request.getEnabled() == 1) {
            profile.enable();
        } else {
            profile.disable();
        }
        profileRepository.update(profile);
    }

    public List<FileRuleResponse> getFileRules(Long profileId) {
        return fileRuleRepository.listByProfileId(profileId)
                .stream()
                .map(fileRuleAssembler::toResponse)
                .toList();
    }
}
```

------

## 3. FileRuleAppService.java

```java
@Service
@RequiredArgsConstructor
public class FileRuleAppService {

    private final ProfileRepository profileRepository;
    private final FileRuleRepository fileRuleRepository;
    private final ProfileRuleDomainService profileRuleDomainService;
    private final FileRuleAssembler fileRuleAssembler;

    public void createFileRule(Long profileId, CreateFileRuleRequest request) {
        SolverTaskProfile profile = profileRepository.findById(profileId);
        if (profile == null) {
            throw new BizException("模板不存在");
        }
        SolverProfileFileRule rule = fileRuleAssembler.toRule(profileId, request);
        profileRuleDomainService.checkRuleConflict(profileId, rule);
        fileRuleRepository.save(rule);
    }

    public void updateFileRule(Long ruleId, UpdateFileRuleRequest request) {
        SolverProfileFileRule rule = fileRuleRepository.findById(ruleId);
        if (rule == null) {
            throw new BizException("文件规则不存在");
        }
        rule.setFileNamePattern(request.getFileNamePattern());
        rule.setFileType(request.getFileType());
        rule.setRequiredFlag(request.getRequiredFlag());
        rule.setSortOrder(request.getSortOrder());
        rule.setRemark(request.getRemark());
        fileRuleRepository.update(rule);
    }

    public void deleteFileRule(Long ruleId) {
        fileRuleRepository.delete(ruleId);
    }
}
```

------

## 4. UploadSpecAppService.java

这是 `solver-service` 最关键的应用服务之一。

```java
@Service
@RequiredArgsConstructor
public class UploadSpecAppService {

    private final ProfileRepository profileRepository;
    private final FileRuleRepository fileRuleRepository;
    private final ProfileRuleDomainService profileRuleDomainService;
    private final UploadSpecBuilder uploadSpecBuilder;

    public UploadSpecResponse buildUploadSpec(Long profileId) {
        SolverTaskProfile profile = profileRepository.findById(profileId);
        if (profile == null) {
            throw new BizException("模板不存在");
        }

        profileRuleDomainService.checkProfileEnabled(profile);

        List<SolverProfileFileRule> rules = fileRuleRepository.listByProfileId(profileId);
        return uploadSpecBuilder.build(profile, rules);
    }
}
```

前端设计里明确要求：选定“求解器 + 模板”后，系统要展示自动生成的输入要求；任务校验也要按“表单校验 + 文件校验 + 模板校验”三层模型执行。这个 `upload-spec` 正是那个桥梁。

------

# 八、Domain 层完整骨架

## 1. SolverDefinition.java

```java
@Data
public class SolverDefinition {
    private Long id;
    private String solverCode;
    private String solverName;
    private String version;
    private String execMode;
    private String execPath;
    private Integer enabled;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void enable() {
        this.enabled = 1;
    }

    public void disable() {
        this.enabled = 0;
    }

    public boolean isEnabled() {
        return this.enabled != null && this.enabled == 1;
    }
}
```

------

## 2. SolverTaskProfile.java

```java
@Data
public class SolverTaskProfile {
    private Long id;
    private Long solverId;
    private String profileCode;
    private String taskType;
    private String profileName;
    private String commandTemplate;
    private String parserName;
    private Integer timeoutSeconds;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void enable() {
        this.enabled = 1;
    }

    public void disable() {
        this.enabled = 0;
    }

    public boolean isEnabled() {
        return this.enabled != null && this.enabled == 1;
    }
}
```

------

## 3. SolverProfileFileRule.java

```java
@Data
public class SolverProfileFileRule {
    private Long id;
    private Long profileId;
    private String fileKey;
    private String fileNamePattern;
    private String fileType;
    private Integer requiredFlag;
    private Integer sortOrder;
    private String remark;

    public boolean isRequired() {
        return this.requiredFlag != null && this.requiredFlag == 1;
    }
}
```

------

## 4. enums

### SolverExecModeEnum.java

```java
public enum SolverExecModeEnum {
    LOCAL,
    CONTAINER
}
```

### RuleFileTypeEnum.java

```java
public enum RuleFileTypeEnum {
    FILE,
    DIR,
    ZIP
}
```

这些值与你数据库设计中的 `exec_mode`、`file_type` 一致。

------

## 5. Repository 接口

### SolverRepository.java

```java
public interface SolverRepository {
    SolverDefinition findById(Long solverId);
    SolverDefinition findBySolverCode(String solverCode);
    void save(SolverDefinition solver);
    void update(SolverDefinition solver);
    PageResult<SolverDefinition> page(SolverPageQueryRequest request);
}
```

### ProfileRepository.java

```java
public interface ProfileRepository {
    SolverTaskProfile findById(Long profileId);
    SolverTaskProfile findBySolverIdAndProfileCode(Long solverId, String profileCode);
    void save(SolverTaskProfile profile);
    void update(SolverTaskProfile profile);
    PageResult<SolverTaskProfile> page(ProfilePageQueryRequest request);
    List<SolverTaskProfile> listEnabledBySolverId(Long solverId);
}
```

### FileRuleRepository.java

```java
public interface FileRuleRepository {
    SolverProfileFileRule findById(Long ruleId);
    void save(SolverProfileFileRule rule);
    void update(SolverProfileFileRule rule);
    void delete(Long ruleId);
    List<SolverProfileFileRule> listByProfileId(Long profileId);
}
```

------

## 6. Domain Service

### SolverDomainService.java

```java
@Service
@RequiredArgsConstructor
public class SolverDomainService {

    private final SolverRepository solverRepository;

    public void checkSolverCodeUnique(String solverCode) {
        SolverDefinition exists = solverRepository.findBySolverCode(solverCode);
        if (exists != null) {
            throw new BizException("solverCode 已存在");
        }
    }
}
```

### ProfileRuleDomainService.java

```java
@Service
@RequiredArgsConstructor
public class ProfileRuleDomainService {

    private final ProfileRepository profileRepository;
    private final FileRuleRepository fileRuleRepository;

    public void checkProfileCodeUnique(Long solverId, String profileCode) {
        SolverTaskProfile exists = profileRepository.findBySolverIdAndProfileCode(solverId, profileCode);
        if (exists != null) {
            throw new BizException("profileCode 已存在");
        }
    }

    public void checkProfileEnabled(SolverTaskProfile profile) {
        if (!profile.isEnabled()) {
            throw new BizException("模板未启用");
        }
    }

    public void checkRuleConflict(Long profileId, SolverProfileFileRule rule) {
        List<SolverProfileFileRule> rules = fileRuleRepository.listByProfileId(profileId);
        boolean duplicated = rules.stream()
                .anyMatch(item -> item.getFileKey().equals(rule.getFileKey()));
        if (duplicated) {
            throw new BizException("fileKey 冲突");
        }
    }
}
```

------

# 九、Infrastructure 层完整骨架

## 1. persistence/entity

### SolverDefinitionPO.java

```java
@Data
public class SolverDefinitionPO {
    private Long id;
    private String solverCode;
    private String solverName;
    private String version;
    private String execMode;
    private String execPath;
    private Integer enabled;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### SolverTaskProfilePO.java

```java
@Data
public class SolverTaskProfilePO {
    private Long id;
    private Long solverId;
    private String profileCode;
    private String taskType;
    private String profileName;
    private String commandTemplate;
    private String parserName;
    private Integer timeoutSeconds;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### SolverProfileFileRulePO.java

```java
@Data
public class SolverProfileFileRulePO {
    private Long id;
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

## 2. mapper

```java
@Mapper
public interface SolverDefinitionMapper extends BaseMapper<SolverDefinitionPO> {
}
@Mapper
public interface SolverTaskProfileMapper extends BaseMapper<SolverTaskProfilePO> {
}
@Mapper
public interface SolverProfileFileRuleMapper extends BaseMapper<SolverProfileFileRulePO> {
}
```

------

## 3. repository impl

### SolverRepositoryImpl.java

```java
@Repository
@RequiredArgsConstructor
public class SolverRepositoryImpl implements SolverRepository {

    private final SolverDefinitionMapper solverDefinitionMapper;
    private final SolverAssembler solverAssembler;

    @Override
    public SolverDefinition findById(Long solverId) { ... }

    @Override
    public SolverDefinition findBySolverCode(String solverCode) { ... }

    @Override
    public void save(SolverDefinition solver) { ... }

    @Override
    public void update(SolverDefinition solver) { ... }

    @Override
    public PageResult<SolverDefinition> page(SolverPageQueryRequest request) { ... }
}
```

### ProfileRepositoryImpl.java

```java
@Repository
@RequiredArgsConstructor
public class ProfileRepositoryImpl implements ProfileRepository {

    private final SolverTaskProfileMapper solverTaskProfileMapper;
    private final ProfileAssembler profileAssembler;

    @Override
    public SolverTaskProfile findById(Long profileId) { ... }

    @Override
    public SolverTaskProfile findBySolverIdAndProfileCode(Long solverId, String profileCode) { ... }

    @Override
    public void save(SolverTaskProfile profile) { ... }

    @Override
    public void update(SolverTaskProfile profile) { ... }

    @Override
    public PageResult<SolverTaskProfile> page(ProfilePageQueryRequest request) { ... }

    @Override
    public List<SolverTaskProfile> listEnabledBySolverId(Long solverId) { ... }
}
```

### FileRuleRepositoryImpl.java

```java
@Repository
@RequiredArgsConstructor
public class FileRuleRepositoryImpl implements FileRuleRepository {

    private final SolverProfileFileRuleMapper solverProfileFileRuleMapper;
    private final FileRuleAssembler fileRuleAssembler;

    @Override
    public SolverProfileFileRule findById(Long ruleId) { ... }

    @Override
    public void save(SolverProfileFileRule rule) { ... }

    @Override
    public void update(SolverProfileFileRule rule) { ... }

    @Override
    public void delete(Long ruleId) { ... }

    @Override
    public List<SolverProfileFileRule> listByProfileId(Long profileId) { ... }
}
```

------

# 十、Assembler 层完整骨架

## 1. SolverAssembler.java

```java
@Component
public class SolverAssembler {

    public SolverDefinition toSolver(CreateSolverRequest request) { ... }

    public SolverListItemResponse toListItemResponse(SolverDefinition solver) { ... }

    public SolverDetailResponse toDetailResponse(SolverDefinition solver) { ... }

    public SolverDefinition fromPO(SolverDefinitionPO po) { ... }

    public SolverDefinitionPO toPO(SolverDefinition solver) { ... }
}
```

## 2. ProfileAssembler.java

```java
@Component
public class ProfileAssembler {

    public SolverTaskProfile toProfile(CreateProfileRequest request) { ... }

    public ProfileListItemResponse toListItemResponse(SolverTaskProfile profile) { ... }

    public ProfileDetailResponse toDetailResponse(SolverTaskProfile profile) { ... }

    public SolverTaskProfile fromPO(SolverTaskProfilePO po) { ... }

    public SolverTaskProfilePO toPO(SolverTaskProfile profile) { ... }
}
```

## 3. FileRuleAssembler.java

```java
@Component
public class FileRuleAssembler {

    public SolverProfileFileRule toRule(Long profileId, CreateFileRuleRequest request) { ... }

    public FileRuleResponse toResponse(SolverProfileFileRule rule) { ... }

    public SolverProfileFileRule fromPO(SolverProfileFileRulePO po) { ... }

    public SolverProfileFileRulePO toPO(SolverProfileFileRule rule) { ... }
}
```

------

# 十一、support 层完整骨架

## 1. UploadSpecBuilder.java

这是最值得单独抽出来的类。

```java
@Component
public class UploadSpecBuilder {

    private final FileRuleAssembler fileRuleAssembler = new FileRuleAssembler();

    public UploadSpecResponse build(SolverTaskProfile profile, List<SolverProfileFileRule> rules) {
        UploadSpecResponse response = new UploadSpecResponse();
        response.setProfileId(profile.getId());
        response.setProfileCode(profile.getProfileCode());
        response.setTaskType(profile.getTaskType());
        response.setProfileName(profile.getProfileName());
        response.setTimeoutSeconds(profile.getTimeoutSeconds());

        List<FileRuleResponse> requiredFiles = rules.stream()
                .filter(SolverProfileFileRule::isRequired)
                .sorted(Comparator.comparing(SolverProfileFileRule::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(fileRuleAssembler::toResponse)
                .toList();

        List<FileRuleResponse> optionalFiles = rules.stream()
                .filter(rule -> !rule.isRequired())
                .sorted(Comparator.comparing(SolverProfileFileRule::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(fileRuleAssembler::toResponse)
                .toList();

        response.setRequiredFiles(requiredFiles);
        response.setOptionalFiles(optionalFiles);
        return response;
    }
}
```

它直接承接了你需求文档里“系统需要根据求解器 + 任务类型自动生成输入要求”的要求。

------

## 2. ProfileRuleValidator.java

```java
@Component
public class ProfileRuleValidator {

    public void validateCreateRule(CreateFileRuleRequest request) {
        if (request.getFileKey() == null || request.getFileKey().isBlank()) {
            throw new BizException("fileKey 不能为空");
        }
        if (request.getFileType() == null || request.getFileType().isBlank()) {
            throw new BizException("fileType 不能为空");
        }
    }

    public void validateUpdateRule(UpdateFileRuleRequest request) {
        if (request.getFileType() == null || request.getFileType().isBlank()) {
            throw new BizException("fileType 不能为空");
        }
    }
}
```

## 3. CommandTemplateResolver.java

```java
@Component
public class CommandTemplateResolver {

    public String resolve(String commandTemplate, Map<String, Object> params) {
        String result = commandTemplate;
        if (params == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }
}
```

这里是给后续接 `node-agent` 执行器留钩子：模板里定义命令，节点实际执行时再解析参数。需求文档和后端设计都把“命令模板 + 解析器名称 + 默认超时”作为模板的核心字段。

------

# 十二、Config / Application 启动类

## 1. SolverApplication.java

```java
@SpringBootApplication
public class SolverApplication {
    public static void main(String[] args) {
        SpringApplication.run(SolverApplication.class, args);
    }
}
```

## 2. MybatisPlusConfig.java

```java
@Configuration
public class MybatisPlusConfig {
}
```

## 3. SolverServiceConfig.java

```java
@Configuration
public class SolverServiceConfig {
}
```

## 4. SolverQueryBuilder.java

第一版可以空壳，后面分页筛选复杂时再扩展。

```java
@Component
public class SolverQueryBuilder {
}
```

------

# 十三、最小可运行实现顺序

不要一口气把 `solver-service` 全写满，按这个顺序最稳。

## 第一批：先做“求解器 + 模板”最小闭环

先建：

- `SolverDefinition`
- `SolverTaskProfile`
- `SolverController`
- `ProfileController`
- `SolverAppService`
- `ProfileAppService`
- `SolverRepository`
- `ProfileRepository`
- `SolverRepositoryImpl`
- `ProfileRepositoryImpl`

先做到：

- 查求解器列表
- 查模板列表
- 查模板详情

## 第二批：补文件规则

再建：

- `SolverProfileFileRule`
- `FileRuleController`
- `FileRuleAppService`
- `FileRuleRepository`
- `FileRuleRepositoryImpl`
- `ProfileRuleDomainService`

做到：

- 查文件规则
- 新增/修改/删除文件规则

## 第三批：补上传规范

再建：

- `UploadSpecAppService`
- `UploadSpecBuilder`
- `FileRuleAssembler`
- `UploadSpecResponse`

做到：

- `GET /api/profiles/{profileId}/upload-spec`
- 前端可直接展示上传要求
- `task-service` 可直接拿规则校验

------

# 十四、你现在最该先建的 12 个类

如果你想立刻开工，我建议 `solver-service` 先建这 12 个：

1. `SolverApplication`
2. `SolverController`
3. `ProfileController`
4. `SolverAppService`
5. `ProfileAppService`
6. `SolverDefinition`
7. `SolverTaskProfile`
8. `SolverRepository`
9. `ProfileRepository`
10. `SolverRepositoryImpl`
11. `ProfileRepositoryImpl`
12. `UploadSpecAppService`

这 12 个类一出来，`solver-service` 的主骨架就有了。

------

# 十五、最终建议

`solver-service` 最重要的不是“代码多”，而是“模型要稳”。

你现在这版骨架把最关键的四层已经分清了：

- `SolverDefinition`：平台支持什么求解器
- `SolverTaskProfile`：某求解器支持什么任务类型、怎么执行
- `SolverProfileFileRule`：这个模板要求什么文件
- `UploadSpecBuilder`：把模板规则翻译给前端和任务服务

这正好对应你整个系统“多求解器接入、模板驱动、文件校验、扩展优先”的核心设计。`solver-service` 的价值就在于：以后新增求解器时，尽量是新增定义、模板、规则，而不是重写主流程。