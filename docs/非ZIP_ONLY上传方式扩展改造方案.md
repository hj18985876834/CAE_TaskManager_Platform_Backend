# 非 ZIP_ONLY 上传方式扩展改造方案

## 1. 文档目的

当前系统已经完成了以 `ZIP_ONLY` 为核心的任务上传、校验、调度与执行链路，并且在前期改造中，已经将求解器差异逐步收敛到模板、文件规则和命令模板之中。这为后续支持更多上传方式打下了基础。

但从代码现状来看，系统目前仍然是“**求解器执行层较通用，上传入口层仍偏 ZIP_ONLY 专用**”。因此，如果后续要增加其他上传方式，例如：

- 多文件分别上传
- 主文件 + 附件上传
- 远程包导入
- 非 zip 格式归档上传
- 前端按目录结构上传

则需要在现有架构上进一步完成上传层的策略化改造。

本文档用于说明：

- 当前实现是否便于扩展
- 当前实现中哪些部分已经具备扩展基础
- 哪些部分仍然存在 `ZIP_ONLY` 硬编码
- 后续支持多上传方式时推荐采用的改造方案
- 各服务、各层、各类的改造建议

## 2. 当前实现现状分析

### 2.1 已具备扩展基础的部分

#### 2.1.1 profile 已具备上传方式元数据

在求解器任务模板模型中，`uploadMode` 已经是显式字段，说明设计层面已经预留了“不同任务模板可采用不同上传方式”的能力。

对应文件：

- [SolverTaskProfile.java](/d:/Project/CAE_TaskManager_Platform/Backend/solver-service/src/main/java/com/example/cae/solver/domain/model/SolverTaskProfile.java#L5)

其中：

- `uploadMode` 表示任务模板使用的上传模式
- `commandTemplate` 表示后续执行命令模板
- `paramsSchemaJson` 表示参数结构

这说明平台的“任务模板”本身并不是只能描述 ZIP 上传，而是已经具备描述不同上传模式的入口。

#### 2.1.2 solver-service 已能向前端和 task-service 返回上传规范

`solver-service` 中的上传规范构建器已经会把模板的上传元数据下发出去。

对应文件：

- [UploadSpecBuilder.java](/d:/Project/CAE_TaskManager_Platform/Backend/solver-service/src/main/java/com/example/cae/solver/infrastructure/support/UploadSpecBuilder.java#L22)
- [UploadSpecResponse.java](/d:/Project/CAE_TaskManager_Platform/Backend/solver-service/src/main/java/com/example/cae/solver/interfaces/response/UploadSpecResponse.java#L5)

当前已支持返回：

- `uploadMode`
- `archiveRule`
- `fileRules`
- `requiredFiles`
- `optionalFiles`

这意味着“上传规范由模板驱动”这一方向是正确的，后续扩展时不需要推翻 `solver-service` 的整体思路。

#### 2.1.3 校验逻辑已经逐步转向规则驱动

`task-service` 中的校验管理器已经不再是单纯为某个求解器写死，而是通过 `solver-service` 拉取 `fileRules`，再做规则化匹配、提取参数、校验内容。

对应文件：

- [TaskValidationManager.java](/d:/Project/CAE_TaskManager_Platform/Backend/task-service/src/main/java/com/example/cae/task/application/manager/TaskValidationManager.java#L82)

当前校验器已经具备：

- 根据模板规则检查文件
- 根据规则从文件名、相对路径、文件内容中派生参数
- 将派生参数合并回 `task.paramsJson`

这说明“**任务校验面向标准化后的工作目录进行，而不是面向某个具体求解器写死**”这一目标已经基本成立。

#### 2.1.4 节点执行端已经基本与上传方式解耦

节点端执行逻辑当前采用命令模板驱动，只要任务最终能整理出统一的工作目录和参数上下文，就可以执行。

对应文件：

- [CommandTemplateExecutor.java](/d:/Project/CAE_TaskManager_Platform/Backend/node-agent/src/main/java/com/example/cae/nodeagent/domain/executor/CommandTemplateExecutor.java#L20)
- [CommandBuilder.java](/d:/Project/CAE_TaskManager_Platform/Backend/node-agent/src/main/java/com/example/cae/nodeagent/infrastructure/support/CommandBuilder.java#L17)

当前执行端依赖的是：

- `commandTemplate`
- `solverExecPath`
- `taskDir`
- `workDir`
- `inputDir`
- `params`

因此，只要上传阶段最终能够产出统一目录合同，节点端通常不需要关心用户最初是 ZIP 上传、散文件上传还是远程包导入。

### 2.2 当前仍然存在的主要问题

#### 2.2.1 上传入口明确写死为 ZIP_ONLY

`TaskLifecycleManager` 中的上传约束解析逻辑虽然会读取模板中的 `uploadMode`，但当前只允许 `ZIP_ONLY`。

对应文件：

- [TaskLifecycleManager.java](/d:/Project/CAE_TaskManager_Platform/Backend/task-service/src/main/java/com/example/cae/task/application/manager/TaskLifecycleManager.java#L140)

当前代码行为是：

1. 读取 profile 的 `uploadMode`
2. 如果不是 `ZIP_ONLY`，直接报错
3. 将上传逻辑统一视为“上传一个归档文件”

这说明：

- `uploadMode` 目前只是“数据上存在”
- 还没有真正演化为“策略上可切换”

#### 2.2.2 上传接口语义仍然偏向“单个归档包”

当前上传入口虽然命名为 `uploadTaskFile`，但内部流程本质上是在做：

- 校验归档格式
- 替换旧归档
- 保存单个归档文件
- 强制标记 `archiveFlag = 1`

对应文件：

- [TaskLifecycleManager.java](/d:/Project/CAE_TaskManager_Platform/Backend/task-service/src/main/java/com/example/cae/task/application/manager/TaskLifecycleManager.java#L113)

因此它并不是一个真正通用的“任务物料上传接口”，而是一个“ZIP 归档上传接口”。

#### 2.2.3 校验流程仍然以“先找归档，再解压”作为前提

当前 `TaskValidationManager` 的主流程是：

1. 从任务文件中找到 archive
2. 校验后缀
3. 仅支持 `zip`
4. 解压到工作目录
5. 对解压结果应用规则

对应文件：

- [TaskValidationManager.java](/d:/Project/CAE_TaskManager_Platform/Backend/task-service/src/main/java/com/example/cae/task/application/manager/TaskValidationManager.java#L132)

这意味着当前校验器还不是“处理标准化物料目录”的通用校验器，而是“处理 ZIP 归档并解压后再校验”的校验器。

#### 2.2.4 UploadSpecResponse 结构表达力仍然偏向归档模式

当前上传规范返回结构中，最明确的上传补充说明是 `archiveRule`。

对应文件：

- [UploadSpecResponse.java](/d:/Project/CAE_TaskManager_Platform/Backend/solver-service/src/main/java/com/example/cae/solver/interfaces/response/UploadSpecResponse.java#L124)

它适合表达：

- 压缩包文件键
- 压缩包允许后缀
- 压缩包大小限制

但不够适合表达：

- 多文件分组上传
- 主文件与附件关系
- 目录结构要求
- 远程资源导入参数
- 是否需要服务端标准化处理

#### 2.2.5 存储层虽然能保存普通文件，但缺乏“物料准备”抽象

当前存储服务本身并没有禁止保存普通输入文件。

对应文件：

- [TaskFile.java](/d:/Project/CAE_TaskManager_Platform/Backend/task-service/src/main/java/com/example/cae/task/domain/model/TaskFile.java#L7)
- [LocalTaskFileStorageService.java](/d:/Project/CAE_TaskManager_Platform/Backend/task-service/src/main/java/com/example/cae/task/infrastructure/storage/LocalTaskFileStorageService.java#L19)

从模型看：

- `fileRole`
- `fileKey`
- `relativePath`
- `archiveFlag`
- `unpackDir`

这些字段本身并没有完全把系统锁死在 ZIP 模式。

但当前缺少一个独立的“**任务物料标准化准备层**”，也就是没有把：

- 上传原始物料
- 整理到标准工作目录
- 产出统一校验上下文

从 `TaskValidationManager` 中抽离出来。

## 3. 结论：后续增加其他上传方式方便扩展吗

### 3.1 结论

可以扩展，且不需要推翻现有整体设计，但还不能算“只加配置即可扩展”。

更准确地说：

- **执行层扩展基础较好**
- **模板层扩展基础较好**
- **上传入口层和物料准备层仍需重构**

因此当前状态属于：

- 不是完全封死
- 但也不是高度插件化
- 适合继续沿着现有设计思想做一轮“上传策略化 + 物料标准化”的改造

### 3.2 扩展难度判断

#### 场景一：新增归档格式

例如增加：

- `tar.gz`
- `tgz`
- `7z`

这类扩展难度中等，主要是：

- 增加可识别后缀
- 增加解包器
- 保持最终输出为统一 `workdir`

#### 场景二：新增非归档上传方式

例如增加：

- 多文件逐个上传
- 主文件 + 材料库文件
- 目录结构上传

这类扩展目前难度中等偏高，因为需要：

- 重构上传入口
- 引入上传策略
- 引入物料标准化层
- 调整前端上传契约

#### 场景三：新增远程导入型上传方式

例如增加：

- 从对象存储拉取文件包
- 从指定 URL 导入
- 从共享目录引用

这类扩展难度较高，因为会涉及：

- 安全控制
- 导入任务异步化
- 外部资源校验
- 失败重试

## 4. 推荐的目标架构

后续如果要系统化支持多种上传方式，建议将任务上传链路拆成三层责任。

### 4.1 第一层：上传方式策略层

职责是“处理用户原始输入”。

这一层只负责回答：

- 用户传的是什么
- 该怎么保存
- 该怎么记录成 `task_file`

推荐抽象接口：

```java
public interface UploadModeHandler {
    String supportMode();
    void validateUploadRequest(Task task, UploadRequest request, UploadSpecMeta spec);
    List<TaskFile> saveUploadedMaterials(Task task, UploadRequest request, UploadSpecMeta spec);
}
```

建议实现类：

- `ZipOnlyUploadHandler`
- `MultiFileUploadHandler`
- `MainWithAttachmentsUploadHandler`
- `RemotePackageUploadHandler`

这样 `TaskLifecycleManager` 不再直接写：

- `if (!ZIP_ONLY) throw ...`

而是改为：

1. 查询 profile 的 `uploadMode`
2. 找到对应 handler
3. 委派上传处理

### 4.2 第二层：物料标准化层

职责是“把各种上传结果整理成统一工作目录合同”。

这一层是整个扩展方案的核心。

因为不管上传方式多么不同，后续校验、调度、节点执行都不应关心这些差异，而应只面对统一的标准目录，例如：

- `input/`：原始上传物料
- `workdir/`：标准化后的任务工作目录
- `log/`
- `output/`

推荐抽象接口：

```java
public interface TaskMaterialPreparer {
    PreparedTaskMaterial prepare(Task task, List<TaskFile> files, UploadSpecMeta spec);
}
```

可能的实现思路：

- ZIP 模式：解压归档到 `workdir`
- 多文件模式：按 `relativePath` 直接复制或移动到 `workdir`
- 主文件+附件模式：按模板规则拼装目录结构到 `workdir`
- 远程导入模式：下载后解包或落盘到 `workdir`

这层完成后，后续系统统一只认：

- “`workdir` 是否准备完成”
- “标准化后的目录内容是什么”

### 4.3 第三层：规则校验层

职责是“基于标准化工作目录做规则校验和参数派生”。

这意味着 `TaskValidationManager` 后续应从：

- “找归档 -> 解压 -> 校验”

改为：

- “准备标准物料目录 -> 校验目录内容”

也就是说，校验器应该尽量不再直接感知：

- ZIP
- tar.gz
- 多文件 HTTP 上传

它只处理：

- `PreparedTaskMaterial`
- `workdir`
- `fileRules`

这才真正符合“模板驱动、多求解器、可扩展”的设计目标。

## 5. 推荐的数据与接口演进方向

### 5.1 profile 侧保持 uploadMode，但要扩展 uploadSpec 表达力

当前 `uploadMode` 应保留，因为它是最上层的模式选择开关。

但 `UploadSpecResponse` 应进一步扩展，建议增加如下结构：

```java
public class UploadSpecResponse {
    private String uploadMode;
    private List<UploadPartRule> uploadParts;
    private MaterialPrepareRule materialPrepareRule;
    private List<FileRuleResponse> fileRules;
}
```

建议新增的概念：

- `uploadParts`
  用于描述前端需要上传哪些部件
- `materialPrepareRule`
  用于描述服务端如何将原始物料整理为标准目录

例如：

- ZIP_ONLY：一个 part，类型为 archive
- MULTI_FILE：多个 part，允许携带 `relativePath`
- MAIN_WITH_ATTACHMENTS：一个主文件 part + 多个附件 part

### 5.2 task_file 建议继续复用，但增强语义

`task_file` 现有字段已经具备一定通用性，后续建议继续沿用。

重点建议：

- `fileRole` 不再只理解为 INPUT / ARCHIVE，可支持更细粒度语义
- `relativePath` 真正用于描述散文件上传后的目录结构
- `archiveFlag` 只作为原始物料属性，不应成为后续流程主判断条件
- `unpackDir` 可以进一步泛化为“标准化输出目录”引用，或保留但由物料准备层写入

### 5.3 前端上传接口建议演进

当前接口：

- `POST /api/tasks/{taskId}/files`

对于 ZIP_ONLY 可以继续使用，但后续支持多模式时，建议有两种选择。

#### 方案一：保留单接口，增加元数据

继续使用：

- `POST /api/tasks/{taskId}/files`

但请求中增加：

- `fileKey`
- `fileRole`
- `relativePath`
- `partCode`

优点：

- 对现有接口影响较小

缺点：

- 单接口语义会越来越复杂

#### 方案二：引入“上传会话/上传批次”模型

例如：

1. 创建上传会话
2. 分批上传部件
3. 确认上传完成
4. 服务端执行标准化准备

优点：

- 更适合多文件、大文件、断点续传、目录上传

缺点：

- 改造范围更大

如果项目下一阶段仍以毕业设计为主，建议优先采用方案一，保证复杂度可控。

## 6. 推荐的代码改造点

### 6.1 task-service

#### 6.1.1 改造 TaskLifecycleManager

对应文件：

- [TaskLifecycleManager.java](/d:/Project/CAE_TaskManager_Platform/Backend/task-service/src/main/java/com/example/cae/task/application/manager/TaskLifecycleManager.java#L113)

建议改造为：

- 不再内部写死 `ZIP_ONLY`
- 改为注入 `UploadModeHandlerRegistry`
- 通过 `uploadMode` 找到对应 handler
- 将上传校验、替换逻辑下沉到 handler

当前最需要移除的硬编码点：

- `unsupported uploadMode`
- 归档专用校验
- 强制 `archiveFlag = 1`

#### 6.1.2 拆出物料准备服务

建议新增：

- `TaskMaterialPrepareManager`
- `TaskMaterialPreparer`
- `PreparedTaskMaterial`

职责：

- 根据任务模板和上传方式
- 将原始上传物料整理到 `workdir`
- 返回标准化后的目录信息

#### 6.1.3 改造 TaskValidationManager

对应文件：

- [TaskValidationManager.java](/d:/Project/CAE_TaskManager_Platform/Backend/task-service/src/main/java/com/example/cae/task/application/manager/TaskValidationManager.java#L132)

建议从当前的：

- `validateArchiveAndRules`

演进为：

- `prepareMaterialsAndValidateRules`

即先：

1. 根据上传方式准备工作目录
2. 再统一对 `workdir` 执行规则校验

后续这样可以把：

- zip 解压
- tar 解包
- 散文件复制

都收敛到物料准备层，而不是塞在校验器里。

#### 6.1.4 扩展存储服务

对应文件：

- [LocalTaskFileStorageService.java](/d:/Project/CAE_TaskManager_Platform/Backend/task-service/src/main/java/com/example/cae/task/infrastructure/storage/LocalTaskFileStorageService.java#L19)

建议增强：

- 支持按 `relativePath` 保存
- 支持上传 part 元数据
- 支持区分原始物料区和标准化工作目录区

目前 `resolveInputDir` 固定到 `input/`，后续建议明确两类目录：

- 原始上传目录
- 准备后工作目录

#### 6.1.5 扩展路径解析器

对应文件：

- [TaskPathResolver.java](/d:/Project/CAE_TaskManager_Platform/Backend/task-service/src/main/java/com/example/cae/task/infrastructure/support/TaskPathResolver.java#L16)

建议新增：

- `resolveWorkspaceDir(taskId)`
- `resolvePreparedWorkDir(taskId)`
- `resolveUploadPartDir(taskId, partCode)`

这样目录合同会更清晰，不会让后续不同上传模式混在 `input/` 里。

### 6.2 solver-service

#### 6.2.1 扩展 UploadSpecBuilder

对应文件：

- [UploadSpecBuilder.java](/d:/Project/CAE_TaskManager_Platform/Backend/solver-service/src/main/java/com/example/cae/solver/infrastructure/support/UploadSpecBuilder.java#L22)

当前它主要能生成 `archiveRule`，后续建议增加：

- `uploadParts`
- `materialPrepareRule`
- `supportsDirectoryStructure`
- `supportsChunkUpload`

让模板能表达：

- 上传部件清单
- 每个部件的约束
- 服务端如何整理物料

#### 6.2.2 保持 fileRules 的核心地位

后续新增上传方式时，建议不要把“求解器差异”重新写回 Java 代码，而要继续保持：

- 上传方式差异由 `uploadMode` 和上传规范描述
- 文件校验差异由 `fileRules` 描述
- 执行差异由 `commandTemplate` 描述

这点是当前架构中最值得保留的部分。

### 6.3 node-agent

节点端原则上不需要因为上传方式扩展而做大改造。

当前目标应保持为：

- 节点只接收已经准备好的标准任务目录
- 节点只关心执行上下文和命令模板

因此上传方式扩展不应把复杂度传递到 `node-agent`。

只有当后续设计改成“节点自己负责拉取或整理原始物料”时，才需要调整节点端；但按照当前项目设计，不建议这么做。

## 7. 推荐的分阶段实施方案

### 第一阶段：保持 ZIP_ONLY，但完成内部抽象

这一阶段最关键，且最适合当前项目。

目标：

- 对外功能不变
- 内部为未来多上传方式铺路

建议内容：

1. 引入 `UploadModeHandler` 抽象
2. 先仅实现 `ZipOnlyUploadHandler`
3. 引入 `TaskMaterialPreparer`
4. 将 ZIP 解压逻辑从 `TaskValidationManager` 中下沉到 `TaskMaterialPreparer`
5. 让 `TaskValidationManager` 只校验标准化后的 `workdir`

这样做完后，即便功能还是 ZIP_ONLY，系统内部也已经完成了最关键的架构转折。

### 第二阶段：支持 MULTI_FILE

建议作为未来第一个新增模式，因为它最能验证架构是否真正通用。

建议能力：

- 允许前端按文件逐个上传
- 支持 `relativePath`
- 由服务端整理成 `workdir`
- 继续复用现有 `fileRules`

如果这一步能顺利落地，说明平台已经真正脱离了 ZIP 专用实现。

### 第三阶段：支持扩展归档格式或远程导入

建议顺序：

1. `TAR_GZ`
2. `TGZ`
3. 远程包导入

其中远程导入复杂度最高，可以作为后续工程增强项，不一定要纳入毕业设计核心实现。

## 8. 最推荐的落地原则

后续编码时应始终坚持以下原则。

### 原则一：上传方式差异只存在于上传层和物料准备层

不要让：

- 调度器
- 节点代理
- 求解器执行器

去关心 ZIP、散文件或远程包差异。

### 原则二：校验逻辑只面向标准工作目录

校验器不应继续承担：

- 判断上传方式
- 决定如何解包
- 处理原始上传协议

它只负责：

- 面向 `workdir`
- 应用模板规则
- 生成校验结果和派生参数

### 原则三：求解器差异继续通过模板表达

不要再回到：

- OpenFOAM 一套 Java 分支
- CalculiX 一套 Java 分支
- Fluent 再来一套 Java 分支

而应继续坚持：

- 模板定义上传方式
- 规则定义文件要求
- 命令模板定义执行方式

### 原则四：先完成内部抽象，再增加外部模式

从工程风险看，最合适的顺序不是马上增加一堆新上传方式，而是：

1. 先把 ZIP_ONLY 改造成策略化内部实现
2. 再基于新架构增加第二种模式

这样可以最大限度避免重复改动。

## 9. 最终结论

如果问“后续要增加其他上传方式，方便扩展吗”，答案是：

**方便扩展，但还需要补一轮关键重构；当前系统已经具备正确的演进方向，但上传层尚未完全抽象。**

更具体地说：

- 当前系统的模板驱动思路是对的
- 当前系统的执行模板化思路是对的
- 当前系统的数据模型没有完全把扩展堵死
- 当前最大的限制在 `task-service` 的上传入口和校验入口仍然过度绑定 `ZIP_ONLY`

因此，最推荐的下一步不是直接继续堆新上传方式，而是：

1. 先把 `ZIP_ONLY` 内部实现重构成策略模式
2. 引入物料标准化层
3. 让校验器只面向标准工作目录
4. 再增加 `MULTI_FILE` 作为第一种新增上传方式

这样最符合本项目“多求解器、模板驱动、可扩展”的总体设计思想。
