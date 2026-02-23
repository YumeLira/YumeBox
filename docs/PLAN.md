# App 子模块拆分总计划（PLAN）

> 最后更新：2026-02-23 23:58:00（本地，含结构审计/空目录清理与下一阶段计划重定）
> 目标：为 `app/` 做职责清晰、低耦合、可迭代迁移的子模块化拆分；本文件作为唯一执行台账，持续维护状态、风险、实施记录与检查结果。

## 1. 执行原则

- `职责分明`：模块按能力边界承载，不按“文件夹名”机械拆。
- `拒绝耦合`：不接受 `data -> presentation`、`domain -> service` 这类反向依赖继续扩散。
- `先解耦再搬家`：先清边界，再做 Gradle 模块迁移，避免边迁边炸。
- `小步可编译`：每个阶段都要求能 `:app:compileDebugKotlin`。
- `计划即代码`：每完成一项，必须回写本文件状态与检查结果。

## 1.1 本次重规划确认（根目录 Gradle 子模块）

- 用户要求的目标是 `根目录 Gradle 子模块` 拆分，不是仅在 `app` 模块内按 package 重排。
- 本次重规划参考同级工程 `2fas-pass-android` 的模块组织思想（`app + core/* + data/* + feature/* + testing/*`），但结合本仓库实际采用“根级基础模块 + `data/feature/runtime` 分组”。
- 已完成的 `P1`（解耦）属于 `Gradle 模块拆分前置工作`，保留并继续作为后续拆分基础。
- 从本节起，所有“模块”默认指 `settings.gradle.kts` 中的根级 Gradle 子模块。

## 2. 范围与终态

### 2.1 范围（本轮）

- `app/src/main/kotlin/**` 的职责重构与模块拆分。
- `app/build.gradle.kts` 依赖瘦身（按子模块迁移）。
- `AndroidManifest.xml` 中 Activity/Service/Provider 组件分流到对应模块（通过 manifest merge）。
- `di/AppModule.kt` 拆为多模块 Koin 定义。

### 2.2 预期终态（阶段化达成）

- `:app` 只保留 App Shell（Application / MainActivity / 导航装配 / 顶层 DI 聚合）。
- 根目录功能模块（更新、WebView、SubStore 等）独立承载 UI + 用例 + 资源。
- Runtime 服务相关代码从 `app` 脱离到根目录 `:service` / `:client` 等模块。
- 共享 UI 与共享契约从 `app` 抽到根目录基础模块。

## 3. Kotlin 基线盘点（仓库）

### 3.1 模块级统计（排除 `build/**`）

| Module | Files | Lines |
| --- | --- | --- |
| app | 245 | 26546 |
| core | 26 | 1213 |
| build-logic | 5 | 414 |

结论：本次拆分工作几乎全部集中在 `app`（245 文件 / 26546 行）。

### 3.2 app 顶层包统计

| Scope | Files | Lines |
| --- | --- | --- |
| presentation | 115 | 16713 |
| service | 59 | 3955 |
| data | 31 | 1879 |
| common | 18 | 1531 |
| domain | 8 | 797 |
| MainActivity.kt | 1 | 326 |
| substore | 4 | 320 |
| update | 1 | 263 |
| ProxySheetActivity.kt | 1 | 225 |
| clash | 2 | 170 |
| remote | 2 | 100 |
| App.kt | 1 | 98 |
| di | 1 | 89 |
| WebViewActivity.kt | 1 | 80 |

结论：`presentation`（16713 行）+ `service`（3955 行）是主战场。

### 3.3 presentation 二级热点桶（Top 20）

| Bucket | Files | Lines |
| --- | --- | --- |
| icon/yume | 31 | 1991 |
| screen/Profiles.kt | 1 | 1465 |
| screen/node | 5 | 980 |
| screen/onboarding | 7 | 862 |
| screen/home | 7 | 829 |
| screen/KeyValueEditor.kt | 1 | 566 |
| viewmodel/AccessControlViewModel.kt | 1 | 521 |
| screen/AccessControl.kt | 1 | 462 |
| screen/Proxy.kt | 1 | 412 |
| viewmodel/ProfilesViewModel.kt | 1 | 386 |
| screen/AppSettings.kt | 1 | 379 |
| screen/Override.kt | 1 | 357 |
| screen/MetaFeature.kt | 1 | 348 |
| viewmodel/OverrideViewModel.kt | 1 | 306 |
| screen/Providers.kt | 1 | 300 |
| component/EmasUpdateDialogHost.kt | 1 | 281 |
| screen/Setting.kt | 1 | 262 |
| viewmodel/HomeViewModel.kt | 1 | 251 |
| screen/LogDetail.kt | 1 | 244 |
| viewmodel/ProxyViewModel.kt | 1 | 231 |

### 3.4 大文件热点（Top 30）

| File | Lines |
| --- | --- |
| presentation/screen/Profiles.kt | 1465 |
| service/ProfileProcessor.kt | 637 |
| presentation/screen/KeyValueEditor.kt | 566 |
| presentation/viewmodel/AccessControlViewModel.kt | 521 |
| presentation/screen/AccessControl.kt | 462 |
| presentation/screen/Proxy.kt | 412 |
| domain/facade/ProxyFacade.kt | 405 |
| presentation/viewmodel/ProfilesViewModel.kt | 386 |
| presentation/screen/AppSettings.kt | 379 |
| presentation/screen/Override.kt | 357 |
| presentation/screen/MetaFeature.kt | 348 |
| MainActivity.kt | 326 |
| presentation/screen/node/NodeContent.kt | 315 |
| presentation/viewmodel/OverrideViewModel.kt | 306 |
| presentation/screen/Providers.kt | 300 |
| presentation/screen/onboarding/OnboardingWizard.kt | 297 |
| presentation/component/EmasUpdateDialogHost.kt | 281 |
| presentation/screen/node/NodeCard.kt | 271 |
| update/EmasUpdateManager.kt | 263 |
| presentation/screen/Setting.kt | 262 |
| presentation/viewmodel/HomeViewModel.kt | 251 |
| common/util/DownloadUtil.kt | 244 |
| presentation/screen/LogDetail.kt | 244 |
| service/TunService.kt | 242 |
| service/ProfileManager.kt | 240 |
| presentation/screen/node/NodeGroups.kt | 237 |
| common/native/NativeLibraryManager.kt | 233 |
| presentation/viewmodel/ProxyViewModel.kt | 231 |
| presentation/component/ProfileCard.kt | 230 |
| data/store/MMKVPreference.kt | 228 |

拆分策略提示：先拆低耦合功能块，不先动 `Profiles.kt` 这种高耦合巨文件。

## 4. 耦合扫描（app 内部）

### 4.1 粗粒度依赖方向（import 计数 Top 30）

> 说明：该表是本次拆分启动时的基线快照（用于对比），后续实时状态以第 6 节台账和第 9 节检查命令结果为准。

| Src | Dst | Count |
| --- | --- | --- |
| presentation | data | 44 |
| presentation | domain | 28 |
| presentation | common | 27 |
| presentation | core | 21 |
| service | core | 20 |
| data | core | 10 |
| domain | service | 7 |
| data | domain | 7 |
| domain | core | 6 |
| presentation | service | 5 |
| remote | service | 5 |
| service | common | 4 |
| service | domain | 4 |
| presentation | WebViewActivity | 4 |
| common | domain | 4 |
| common | data | 4 |
| presentation | R | 4 |
| service | MainActivity | 3 |
| domain | remote | 3 |
| service | R | 3 |
| presentation | BuildConfig | 3 |
| presentation | substore | 3 |
| presentation | update | 2 |
| service | remote | 2 |
| service | data | 2 |
| common | App | 2 |
| di | domain | 2 |
| di | data | 2 |
| data | remote | 2 |
| data | presentation | 2 |

### 4.2 当前阻塞性反向依赖（必须先修）

| File | Import |
| --- | --- |
| （当前无） | `data/` 与 `domain/` 范围已无直接 `import ...presentation.* / service.*` | 

关键问题（已确认）：

- `data` 依赖 `presentation.theme.AppColorTheme`（边界反了）`已修复 2026-02-23`：`AppColorTheme` 已迁移至 `data.model`。
- `domain/facade/*` 实际是 Android + Service 客户端适配层，不是纯 domain。`已修复 2026-02-23`：已归位为 `runtime/client/*`。
- `service.runtime.entity.ProfileExtensions` 含 UI/文案逻辑，污染 Service 实体层。`已修复 2026-02-23`：已迁移至 `presentation/util/ProfileUiExtensions.kt`。
- `data/repository/LogRepository.kt` 直接依赖 `service.LogRecordService`，数据层与运行时服务实现绑死。`已修复 2026-02-23`：已通过 `LogRecordGateway` 解耦。

### 4.3 参考工程（`2fas-pass-android`）观察结论

- `settings.gradle.kts` 在 2fas 中采用分组命名：`app` + `testing/*` + `core/*` + `data/*` + `feature/*`（本仓库当前基础能力已改为根级模块）。
- `app` 作为壳层模块，显式依赖所有基础层与功能层模块，而不是把实现都塞在 `app`。
- 2fas 的 `core/*` 承载共享能力（Android/设计系统/DI/网络/语言等）；本仓库当前对应能力使用根级模块（`platform/common/locale/ui/di`），`data/*` 承载数据实现，`feature/*` 承载业务功能。
- 每个模块目录结构统一：`<group>/<name>/build.gradle.kts + src/main/...`，Android library 模块可自带 `AndroidManifest.xml` 与资源。
- 通过 `buildlogic` convention plugin 统一 Android Library / Compose / Lint 配置，降低模块新增成本。
- `:di` 这类基础模块通过 `api` 暴露 DI 框架，减少 feature 重复声明（可借鉴，但需控制 API 扩散）。

## 5. 子模块职责规划（目标架构）

### 5.1 模块清单（规划）

| 模块 | 角色 | 计划承载 | 状态 | 说明 |
| --- | --- | --- | --- | --- |
| `:app` | App Shell | `App.kt`、`MainActivity.kt`、导航装配、DI 聚合、manifest 聚合 | 部分实施 | `service/common/ui 公共资产` 已大幅迁出；当前仍承载主要 screen/VM（阶段性可接受，后续以“稳定化治理”而非继续大拆分为主） |
| `:core`（现有） | 核心引擎桥接 | 当前 `core/` 模块（Clash bridge/model/util） | 已存在 | 保持为底层核心模块 |
| `:platform` | 基础层 | Android 通用封装（Context/Intent/权限/平台 helper） | 已实施 | 已承接 `StartupGate` 与全部 `app/common` Android 工具（`openUrl`/`toast`/`VpnUtils`/`AppIconHelper`/`SystemProxyHelper`/`DeviceUtil`） |
| `:common` | 基础层 | 无业务共享工具/模型（格式化、通用 helper） | 已实施 | 已承接通用工具（`LocaleUtil`/`PlatformIdentifier`/`FormatUtils`），并统一消除重复类风险 |
| `:locale` | 基础层 | `MLang` / fytxt 本地化代码生成与导出 | 已实施 | 已从 `:app` 抽离生成职责，供 feature 模块直接依赖；本地化源文件已并入模块目录 `locale/lang` |
| `:ui` | 基础层 | 主题、通用 Compose 组件、图标、UI 扩展 | 已实施（阶段性） | 已迁最小可用主题/组件，并扩容迁移图标包（`presentation/icon/*`）及多批通用组件/主题（`AnimationSpecs`、`TrafficBarChart`、`LoadingAnimation`、`NavigationIcon`、`BottomBar` 内容组件、`ProfileCard`、`WindowBlurEffect` 等）；`app/presentation/theme` 与 `app/presentation/component` 已清空（主题与通用组件完全归属 `:ui`，导航/状态桥接内联到调用方壳层） |
| `:di` | 基础层 | Koin 基础定义/qualifier/模块聚合协议 | 部分实施 | 已迁基础单例/数据仓库/runtime client/collector 模块；`app` 保留集成绑定（`LogRecordGateway`、`ProfilesStore`）与剩余 ViewModel 模块，`feature:substore` ViewModel 注册已下沉到模块内 |
| `:data:settings` | 数据层 | MMKV stores + settings repositories（App/Network/Feature/Display/Traffic） | 已实施 | 已包含 `ProfileLinks` 与 MMKV 基础设施；`domain/model/ProxyDisplaySettings.kt` 暂寄于本模块（保留原 package） |
| `:data:log` | 数据层 | `LogRepository` + log 读写与导出 | 已实施 | `LogRepository`/`LogRecordGateway` 已迁移；`LogRecordServiceGateway` 实现已归属 `:runtime:service` |
| `:data:proxy` | 数据层 | providers/override/networkInfo/proxyChain/traffic collector 等 | 部分实施 | 已迁第一批 `NetworkInfoService` / `OverrideRepository` / `ProvidersRepository` / `ProxyChainResolver` / `TrafficStatisticsCollector` + `TrafficData` |
| `:runtime:api` | 运行时层 | runtime 共享契约（remote 接口、异常、广播常量、共享实体） | 已实施 | `:runtime:client` / `:runtime:service` 拆分前置契约层 |
| `:runtime:client` | 运行时层 | `runtime/client/*` + `remote/*` | 已实施 | 已迁移 `ProxyFacade` / `ProfilesRepository` / `ServiceClient` / `ProxyGroupInfo`；当前本地单进程网关临时依赖 `:runtime:service` |
| `:runtime:service` | 运行时层 | `service/**` + manifest 组件 | 已实施 | `service/**` 与 Service/Receiver/Provider/Tile 组件已迁移并通过 manifest merge 生效；`app/service` 已清空 |
| `:feature:update` | 功能层 | 更新能力与更新弹窗 UI（EMAS/Taobao Update） | 已实施 | 第一刀功能模块，代码/资源已迁移 |
| `:feature:web` | 功能层 | WebView Activity + WebView Screen/工具 | 已实施 | 第二刀功能模块，`WebViewUtils` 已拆分职责 |
| `:feature:about` | 功能层 | About / OpenSourceLicenses / AboutLibraries UI | 已实施 | `OpenSourceLicenses` 与 `About` 主页均已迁移为模块内容组件，`app` 仅保留导航/系统能力 wrapper |
| `:feature:substore` | 功能层 | `substore/*` + SubStore UI/VM/工具 | 部分实施 | 已迁 SubStore runtime/service、native loader、download/archive/path/web assets、`FeatureViewModel`/`SettingViewModel` 与相关模型/工具；`Feature.kt` 已改为 `app` wrapper + 模块内 `FeatureContent`，`Setting.kt` 等 UI 壳仍在 `app` |
| `:feature:settings`（后续） | 功能层 | 设置页与相关 ViewModel（逐步收敛） | 待规划 | 依赖 `core:ui` / `data:*` / `runtime:client` |
| `:feature:profiles`（后续） | 功能层 | Profiles/扫码/导入编辑（大模块） | 待规划 | 高耦合巨块，后置 |
| `:feature:home`（后续） | 功能层 | Home/Traffic/Node UI 与 VM | 待规划 | 待 `runtime:client` 稳定后拆 |
| `:feature:proxy` | 功能层 | Proxy/Providers/Override UI 与 VM | 部分实施 | 已迁 `ProxyPager`、`ProxySheetContent`、`ProvidersContent`、`OverrideContent`、`node/*`、`Proxy/Providers/Override ViewModel` 与 `ProxyNameFormat`；`app` 保留 `Providers/Override` 的 `@Destination` + editor/navigation wrapper 与 `ProxySheetActivity` 主题壳 |
| `:testing:core`（可选） | 测试基础 | 测试工具、fake、test utils | 待评估 | 对标 2fas `:testing:core` |
| `:core:contract`（可选） | 共享契约 | 跨层稳定契约（共享模型/接口） | 待评估 | 边界稳定后再抽，避免过早设计 |

### 5.2 边界规则（强约束）

- 禁止任意 `:feature:*` / `:data:*` / `:runtime:*` / `:platform` / `:common` / `:locale` / `:ui` / `:di` / `:core` 依赖 `:app`。
- `:app` 只能做装配，不实现业务逻辑、不定义跨功能共享 UI 组件。
- `:feature:*` 允许依赖 `:platform` / `:common` / `:locale` / `:ui` / `:di` / `:core`、`:data:*`、`:runtime:client`，禁止直接依赖 `:runtime:service`。
- `:data:*` 禁止依赖 `:feature:*`，禁止依赖 `presentation` 包。
- `:runtime:service` 不依赖 `:feature:*`；如需 UI/数据访问，走契约/网关。
- 禁止 `data -> presentation`。
- 禁止把 UI 文案/展示扩展函数放在 Service 实体包。
- Manifest 组件必须归属于其能力模块（通过 manifest merge 接入）。

### 5.3 模块命名规则（简单清晰）

- 使用 `根级基础模块 + 分组业务模块`：基础能力使用根级模块（`platform`、`common`、`locale`、`ui`、`di`、`core`），业务按分组（`data/*`、`feature/*`、`runtime/*`、`testing/*`）。
- 能力名使用单词或短词：`ui`、`di`、`settings`、`log`、`update`、`web`、`substore`。
- 模块路径与目录路径一致：例如 `feature/update/` 对应 `:feature:update`。
- `core/` 目录仅保留现有 `:core` 主核心模块，禁止继续新增 `core/*` 子模块（基础能力统一平铺到根目录）。
- 不使用含糊命名（如 `common2`、`utils2`、`feature-misc`）。

## 6. 分阶段执行台账（设计 / 评审 / 实施 / 检查）

| ID | 阶段 | 任务 | 设计 | 评审 | 实施 | 检查 | 状态 | 验收标准 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| P0-1 | 基线 | Kotlin 文件盘点与热点统计 | 已完成 | 已完成 | 已完成 | 已完成 | 完成 | 本文件已记录统计结果 |
| P0-2 | 基线 | app 耦合扫描与阻塞点登记 | 已完成 | 已完成 | 已完成 | 已完成 | 完成 | 本文件已记录反向依赖与耦合矩阵 |
| P1-1 | 解耦 | `AppColorTheme` 从 `presentation` 移出 | 已完成 | 已完成 | 已完成 | 已完成 | 完成 | 消除 `data -> presentation` |
| P1-2 | 解耦 | `ProfileExtensions` 移到 `presentation` | 已完成 | 已完成 | 已完成 | 已完成 | 完成 | Service 实体层不含 UI 文案逻辑 |
| P1-3 | 解耦 | `domain/facade` 归位为 runtime client 层（改包/命名） | 已完成 | 已完成 | 已完成 | 已完成 | 完成 | 语义与依赖方向一致 |
| P1-4 | 解耦 | `LogRepository` 与 `LogRecordService` 解耦（接口/网关） | 已完成 | 已完成 | 已完成 | 已完成 | 完成 | 数据层不直接依赖 service 实现 |
| P2-0 | 模块化 | 对齐 2fas 风格并结合本仓库约束：定稿“根级基础模块 + `data/feature/runtime` 分组”拓扑与 `settings.gradle.kts` 命名 | 已完成 | 已完成 | 已完成 | 已完成 | 完成 | 模块树与目录树一一对应 |
| P2-1 | 模块化 | 新建 `:locale`（迁移 fytxt/MLang 生成职责） | 已完成 | 已完成 | 已完成 | 已完成 | 完成 | 功能模块可无 `:app` 依赖使用 `MLang` |
| P2-2 | 模块化 | 新建 `:ui`（先迁最小可用主题/组件/图标集） | 已完成 | 已完成 | 已完成（扩容） | 已完成（扩容） | 完成（阶段性） | `app.presentation` 公共主题/组件资产已基本收口到 `:ui`（`app/presentation/theme` 与 `component` 已清空）；后续按需增量迁移，不再作为当前主任务 |
| P2-3 | 模块化 | 新建 `:feature:update` 并迁移更新代码/资源 | 已完成 | 已完成 | 已完成 | 已完成 | 完成 | `app` 移除 taobao update 依赖 |
| P2-4 | 模块化 | 新建 `:feature:web` 并迁移 WebView 相关 | 已完成 | 已完成 | 已完成 | 已完成 | 完成 | Activity/Compose/工具迁移成功 |
| P2-5 | 模块化 | 新建 `:feature:about` 并迁移 About/License 相关 | 已完成 | 已完成 | 已完成 | 已完成 | 完成 | About/License UI 迁移完成，`app` 仅保留 wrapper |
| P2-6 | 模块化 | 新建 `:di` 并拆 Koin 装配（`app` 仅聚合） | 已完成 | 已完成 | 已完成（第二批） | 已完成（第二批） | 进行中 | `App.kt` 已仅聚合 `modules(appModule)`；基础单例/仓库/runtime-client 已迁入 `:di`，`feature:substore` ViewModel Koin 模块已迁到功能模块，剩余 app 集成绑定与其他 ViewModel 模块后续继续收口 |
| P3-1 | 模块化 | 新建 `:data:settings`（迁移 MMKV settings stores/repos） | 已完成 | 已完成 | 已完成 | 已完成 | 完成 | settings 数据能力独立 |
| P3-2 | 模块化 | 新建 `:data:log`（迁移 LogRepository + 网关接口） | 已完成 | 已完成 | 已完成 | 已完成 | 完成 | log 数据能力独立 |
| P3-3 | 模块化 | 新建 `:feature:substore` 并迁移 SubStore 能力 | 已完成 | 已完成 | 已完成（第二批） | 已完成（第二批） | 进行中 | SubStore runtime/service + native/download/path + `FeatureViewModel`/`SettingViewModel` 已独立；`Feature.kt` 已切为 `app` wrapper，后续继续迁移设置入口等 UI 层 |
| P3-4 | 模块化 | 新建 `:feature:proxy` 并迁移 Proxy/Providers/Override/node/相关 VM | 已完成 | 已完成 | 已完成（第一批） | 已完成（第一批） | 进行中 | 已迁 `ProxyPager`（回调化去除 `app` 生成导航依赖）、`ProxySheetContent`、`ProvidersContent`、`OverrideContent`、`node/*`、`Proxy/Providers/Override ViewModel` 与模块内 Koin 注册；`app` 保留 `@Destination` / editor 导航 wrapper 与 `ProxySheetActivity` 主题壳 |
| P4-0 | 模块化 | 新建 `:runtime:api`（抽 shared runtime contracts/api） | 已完成 | 已完成 | 已完成 | 已完成 | 完成 | 为 `:runtime:client` / `:runtime:service` 提供共享契约 |
| P4-1 | 模块化 | 新建 `:runtime:client`（迁移 `runtime/client` + `remote`） | 已完成 | 已完成 | 已完成 | 已完成 | 完成 | `ProxyFacade`/`ProfilesRepository`/`ServiceClient`/`ProxyGroupInfo` 已迁移，UI/Data 通过 `:runtime:client` 访问 runtime |
| P4-2 | 模块化 | 新建 `:runtime:service`（迁移 `service/**`） | 已完成 | 已完成 | 已完成 | 已完成 | 完成 | `service/**`（含 Service/Receiver/Provider/QS Tile）已迁移至 `:runtime:service` 并编译通过；`app/service` 已清空 |
| P4-3 | 模块化 | 新建 `:data:proxy`（迁移 proxy/providers/override/network 相关 repository） | 已完成 | 已完成 | 已完成（第一批） | 已完成（第一批） | 进行中 | 第一批 proxy 数据能力已独立，后续继续迁移剩余 proxy/network 相关实现 |
| P4-4 | 模块化 | 新建 `:platform` / `:common`（按实际依赖拆 `common/*`） | 已设计 | 已完成 | 已完成 | 已完成 | 完成 | `app/common/**` 已清空；`StartupGate`/`IntentController`/`ProxyAutoStartHelper` 与通用工具均已迁移到根级基础模块 / `runtime:client` 合适归属 |
| P5-1 | 收口 | 当前子模块结构/职责审计（含 `app` 剩余内容盘点） | 已完成 | 已完成 | 已完成 | 已完成 | 完成 | 已确认基础模块平铺、功能模块承载有效，`app` 剩余内容已完成分类盘点 |
| P5-2 | 收口 | 迁移残留空目录与历史废目录清理 | 已完成 | 已完成 | 已完成 | 已完成 | 完成 | `app`/`runtime:service` 残留空包目录已清，根目录历史 `service/` 构建残留已删除 |
| P5-3 | 稳定化 | `app` 剩余 screen/VM 分类治理（壳层/暂留/候选迁移） | 已设计 | 待评审 | 待实施 | 待检查 | 待执行 | 明确“继续留在 app”与“后续可拆”边界，减少无计划漂移 |
| P5-4 | 稳定化 | `app/build.gradle.kts` 依赖瘦身与归属复核 | 已设计 | 待评审 | 待实施 | 待检查 | 待执行 | 依赖按模块归属清晰、无明显过度依赖 |
| P5-5 | 稳定化 | 编译检查 + 回归清单执行 | 已设计 | 待评审 | 待实施 | 待检查 | 待执行 | `:app:compileDebugKotlin` 与核心路径 smoke check 通过 |

## 7. 实施顺序（当前建议）

1. `P1-1` 移出 `AppColorTheme`（最快清掉反向依赖）
2. `P1-2` 移走 `ProfileExtensions`（清 Service 层 UI 污染）
3. `P1-3` `domain/facade` 改包/命名为 runtime client 层
4. `P2-0` 定稿“根级基础模块 + data/feature/runtime/testing 分组”拓扑
5. `P2-1` 建 `:locale`（先解耦 `MLang` 生成）
6. `P2-2` 建 `:ui`（最小可用共享 UI）
7. `P2-3 ~ P2-5` 先拆低风险功能模块：`:feature:update` / `:feature:web` / `:feature:about`
8. `P2-6` 建 `:di`，让 `app` 只做模块聚合
9. `P3-1 ~ P3-2` 拆低风险数据模块：`:data:settings` / `:data:log`
10. `P3-3` 拆 `:feature:substore`
11. `P4-0` 先抽 `:runtime:api`（共享契约层），再执行 `P4-1 ~ P4-2` 拆 `:runtime:client` / `:runtime:service`（高风险）
12. `P4-3 ~ P4-4` 收口 `:data:proxy`、`:platform`、`:common`

### 7.1 阶段性评估（2026-02-23）

- 模块化主目标已达到：基础能力平铺到根级（`platform/common/di/locale/ui`），`runtime/*`、`data/*`、`feature/*` 已形成清晰承载。
- `app` 已显著瘦身：`service/common/theme/component/web/update/substore` 等大块已迁出；当前主要剩余 `screen + viewmodel + Activity/App shell`。
- 当前状态适合“停止继续大拆分”，转入稳定化与治理阶段；继续强拆 `screen`（尤其 `Profiles/Settings/Home`）收益下降、风险上升。
- 仍有可优化点，但属于“质量治理”而非“结构救火”：`app/di` 收口、依赖瘦身、回归验证、文档/职责矩阵维护。

### 7.2 下一阶段计划（冻结大拆分，进入稳定化）

1. 执行 `P5-3`：给 `app` 剩余 screen/VM 建立分类台账（`壳层` / `暂留` / `候选迁移`）。
2. 执行 `P5-4`：复核 `app/build.gradle.kts` 依赖归属，清掉明显已经迁出模块后仍残留在 `app` 的依赖。
3. 执行 `P5-5`：完成编译与核心路径 smoke check（启动、主页、代理页、设置页、WebView、更新弹窗、日志页）。
4. 文档收口：补一份模块职责矩阵（可单独 `docs/MODULE_AUDIT.md`，也可继续维护在本 `PLAN`）。
5. 后续若再拆分，只接受“明确收益 + 低风险”的增量项，不再做全局大迁移。

## 8. 评审与检查要点（每阶段通用）

- 依赖方向是否保持单向（`feature -> data/runtime-client -> core`）。
- 是否出现新模块反向依赖 `:app`。
- 是否出现 `feature` 直接依赖 `runtime:service`（禁止）。
- Manifest authority / exported / permission 是否迁移后保持一致。
- `AppModule` 是否继续膨胀（若是，立即拆 Koin 子模块）。
- 资源是否与功能代码一起迁移。

## 9. 检查命令（每次更新本计划前后执行）

```powershell
.\\gradlew.bat :app:compileDebugKotlin --no-daemon
.\\gradlew.bat --% :app:compileDebugKotlin --no-daemon -Dkotlin.compiler.execution.strategy=in-process -Pkotlin.incremental=false
rg --files -g "*.kt" -g "!**/build/**"
rg -n "import com\\.github\\.yumelira\\.yumebox\\.(presentation|service)\\." app/src/main/kotlin/com/github/yumelira/yumebox/data app/src/main/kotlin/com/github/yumelira/yumebox/domain -g "*.kt"
```

## 10. 工作日志（维护区）

- [2026-02-23] 初始化 `PLAN.md`：完成 Kotlin 基线盘点、耦合扫描、目标模块职责设计、阶段台账登记。
- [2026-02-23] 完成 `P1-1`：`AppColorTheme` 从 `presentation.theme` 迁移到 `data.model`，并通过 `:app:compileDebugKotlin --no-daemon`。
- [2026-02-23] 完成 `P1-2`：`service/runtime/entity/ProfileExtensions.kt` 迁移到 `presentation/util/ProfileUiExtensions.kt`，并通过 `:app:compileDebugKotlin --no-daemon`。
- [2026-02-23] 完成 `P1-3`：`domain/facade/*` 迁移至 `runtime/client/*`，全局引用已更新，并通过 `:app:compileDebugKotlin --no-daemon`。
- [2026-02-23] 完成 `P1-4`：新增 `LogRecordGateway` + `LogRecordServiceGateway`，`LogRepository` 不再直接依赖 `service.LogRecordService`，并通过 `:app:compileDebugKotlin --no-daemon`。
- [2026-02-23] 按用户要求重规划：明确目标为根目录 Gradle 子模块拆分，模块命名改为简单能力名（`ui` / `client` / `service` / `update` / `web` / `substore` / `about`）。
- [2026-02-23] 参考 `2fas-pass-android` 模块分层（`core/* + data/* + feature/* + runtime/* + app`）重制定计划，改为分组模块命名方案。
- [2026-02-23] 完成 `P2-0`：`settings.gradle.kts` 按分组模块拓扑接入（已新增 `:locale`、`:ui`、`:feature:update`、`:feature:web`）。
- [2026-02-23] 完成 `P2-1`：新建 `:locale`，迁移 `fytxt/MLang` 生成职责；`:app` 改为依赖 `:locale`。
- [2026-02-23] 完成 `P2-2`（最小集）：新建 `:ui`，迁移共享 UI 最小集（`Dimens` / `Modifiers` / `Card`）。
- [2026-02-23] 完成 `P2-3`：新建 `:feature:update`，迁移 `EmasUpdateManager`、`EmasUpdateDialogHost`、`update.jpg`，`app` 移除 Taobao Update 依赖。
- [2026-02-23] 完成 `P2-4`：新建 `:feature:web`，迁移 `WebViewActivity` 与 `presentation/webview/*`；`WebViewUtils` 拆分为 `:feature:web` 的面板 URL 工具与 `app` 内 `SubStoreWebAssets`（SubStore 本地资源职责）。
- [2026-02-23] 编译检查通过：`--% :app:compileDebugKotlin --no-daemon -Dkotlin.compiler.execution.strategy=in-process -Pkotlin.incremental=false`（本机 Kotlin 增量缓存文件锁问题，采用 fallback 验证）。 
- [2026-02-23] 完成 `P2-5`（License 子屏）：新建 `:feature:about`，迁移 `OpenSourceLicenses` 为模块内容组件；`app` 保留 `@Destination` wrapper，AboutLibraries 依赖从 `app` 移至 `:feature:about`。
- [2026-02-23] 再次编译检查通过：`--% :app:compileDebugKotlin --no-daemon -Dkotlin.compiler.execution.strategy=in-process -Pkotlin.incremental=false`（包含 `:feature:about` 接入）。 
- [2026-02-23] 完成 `P3-2`：新建 `:data:log`，迁移 `LogRepository` 与 `LogRecordGateway` 至根级数据模块；`app` 保留 `LogRecordServiceGateway` 实现与 DI 绑定。
- [2026-02-23] 编译检查通过：`--% :app:compileDebugKotlin --no-daemon -Dkotlin.compiler.execution.strategy=in-process -Pkotlin.incremental=false`（包含 `:data:log` 接入）。 
- [2026-02-23] 评估 `P3-1 (:data:settings)`：最小迁移切片还需连带处理 `MMKVPreference/MMKVProvider/Preference`、部分 `data.model`（`ThemeMode`/`AppColorTheme`/`ProxyMode`/`TunStack`/`AccessControlMode`）以及从 `ProfileLinksStorage.kt` 拆出的 `LinkOpenMode`。 
- [2026-02-23] 完成 `P3-1` 第一批：新建 `:data:settings` 并迁移 `MMKVPreference/MMKVProvider`、`App/Network/Feature/ProfileLinks` stores 与 repositories、相关 `data.model` 枚举（保留原 package）。
- [2026-02-23] 完成 `P3-1` 第二批：迁移 `ProxyDisplaySettingsStore/Repository`、`TrafficStatisticsStore/Repository`、`data/model/TrafficStatistics.kt`，以及 `domain/model/ProxyDisplaySettings.kt`（临时放入 `:data:settings` 模块，后续可再抽 `:core:contract`）。
- [2026-02-23] 编译检查通过：`--% :app:compileDebugKotlin --no-daemon -Dkotlin.compiler.execution.strategy=in-process -Pkotlin.incremental=false`（包含 `:data:settings` 大批量迁移）。 
- [2026-02-23] 完成 `P4-0`：新建 `:runtime:api`，迁移 `service.remote/*`、`service.runtime.entity.Profile`、`service.runtime.util.UUIDSerializer`、`remote/ServiceExceptions.kt`、`service.common.constants/*`、`service.common.util.Global.kt`、`service/ProxyServiceContracts.kt`。
- [2026-02-23] 编译检查通过：`--% :app:compileDebugKotlin --no-daemon -Dkotlin.compiler.execution.strategy=in-process -Pkotlin.incremental=false`（包含 `:runtime:api` 契约层抽取）。 
- [2026-02-23] 完成 `P2-5` 收口：迁移 `About.kt` 主页面为 `:feature:about` 的 `AboutContent`，`app` 改为 `@Destination` wrapper（负责 `Bridge`、`BuildConfig`、更新入口、导航与外链）。
- [2026-02-23] 编译检查通过：`--% :app:compileDebugKotlin --no-daemon -Dkotlin.compiler.execution.strategy=in-process -Pkotlin.incremental=false`（包含 `feature:about` 主页迁移）。 
- [2026-02-23] 推进 `P4-2`：新建 `:runtime:service` 并完成 `service/**` 主体迁移；将强耦合集成组件（`AutoRestartService`、`ProxyTileService`、`LogRecordService`、`DialerReceiver/RestartReceiver`、`StatusProvider`）回迁 `:app`，`runtime/service` manifest 仅保留核心运行时服务组件；补齐 `androidx.core:core-ktx` 与模块 `R` 引用后恢复编译通过。
- [2026-02-23] 完成 `P4-1`：新建 `:runtime:client`，迁移 `runtime/client/ProxyFacade.kt`、`runtime/client/ProfilesRepository.kt`、`remote/ServiceClient.kt` 与 `domain/model/ProxyGroupInfo.kt`（保留原 package）；`:app` 改为依赖 `:runtime:client`，编译检查通过。
- [2026-02-23] 推进 `P4-3` 第一批：新建 `:data:proxy`，迁移 `NetworkInfoService`、`OverrideRepository`、`ProvidersRepository`、`ProxyChainResolver`、`TrafficStatisticsCollector` 与 `domain/model/TrafficData.kt`（保留原 package）；补齐 `:runtime:api` 依赖后编译检查通过。
- [2026-02-23] 推进 `P3-3` 第一批：新建 `:feature:substore`，迁移 `substore/*`、`common/native/NativeLibraryManager.kt`、`common/util/{AppUtil,ArchiveUtil,DownloadUtil,SubStoreWebAssets,FormatUtils}.kt`，并将 `SubStoreService` manifest 声明迁移到模块 manifest；同时把 `App.instance` 依赖改为 `core.Global.application`，编译检查通过。
- [2026-02-23] 推进 `P3-3` 第二批：迁移 `presentation/viewmodel/FeatureViewModel.kt`、`data/model/AutoCloseMode.kt`、`common/util/DeviceUtil.kt` 到 `:feature:substore`（保留原 package），补齐 `:locale` 与 lifecycle 依赖，编译检查通过。
- [2026-02-23] 推进 `P2-6` 第一批：新建 `:di`，迁移 Koin 基础单例/MMKV/stores/repositories/runtime-client/collector 绑定；`app/di/AppModule.kt` 缩减为 app 集成绑定（`LogRecordGateway`、`ProfilesStore`）+ ViewModel 模块聚合，编译检查通过。
- [2026-02-23] 推进 `P4-2` 收口：将 `LogRecordService` 与 `LogRecordServiceGateway` 迁回 `:runtime:service`，去除 `LogRecordService -> runtime:client.ServiceClient` 依赖（改为直接使用 `ClashManager` + `Components.MAIN_ACTIVITY`），并完成 manifest 声明回迁；编译检查通过。
- [2026-02-23] 推进 `P3-3` 第三批：迁移 `presentation/viewmodel/SettingViewModel.kt` 到 `:feature:substore`（保留原 package），编译检查通过。
- [2026-02-23] 推进 `P4-2` 进一步收口：拆分 `BroadcastReceiver.kt`，将 `DialerReceiver` 迁移至 `:runtime:service`（使用 `Components.MAIN_ACTIVITY` 启动主界面），`RestartReceiver` 先独立为单文件；随后将 `AutoRestartService` 与 `RestartReceiver` 迁移至 `:runtime:service`，并将 `AutoRestartService` 重写为纯 `runtime:service + data:settings` 实现（去除对 `runtime:client`、Koin 和 `ProxyAutoStartHelper` 的依赖）；两侧 manifest 已完成回迁，编译检查通过。
- [2026-02-24] 完成 `P4-2`：将 `ProxyTileService` 迁移至 `:runtime:service` 并重写为纯 `runtime:service` 实现（去除 `runtime:client` / Koin 依赖，改用 `ProfileManager` / `ClashManager` / `StatusProvider` / `Components`），同时回迁 QS Tile manifest 声明与 `tile_proxy_label` 资源；编译检查通过，`app/service` 目录已清空。
- [2026-02-24] 推进 `P3-3` UI 收口：新增 `feature:substore` 内 `presentation/screen/FeatureContent.kt`，并将 `app/presentation/screen/Feature.kt` 改为 `@Destination` wrapper（通过 `onOpenExternalUrl` 回调桥接 `openUrl`）；编译检查通过。
- [2026-02-24] 推进 `P2-2` 扩容（共享 UI 第一批大迁移）：将 `presentation/icon/*`（含 `yume/*` 图标集）整体迁移至 `:ui`（保留原 package），编译检查通过。
- [2026-02-24] 启动 `P4-4` 第一批：新建 `:platform` 与 `:common`，并迁移 `common/util/{Link,ToastExt,VpnUtils,AppIconHelper,SystemProxyHelper,LocaleUtil,PlatformIdentifier}.kt`；`app` 已接入新模块依赖，编译检查通过。
- [2026-02-24] 推进 `P4-4` 第一批收口：将通用 `FormatUtils.kt` 统一迁入 `:common`，将 `DeviceUtil.kt` 迁入 `:platform`；删除 `:runtime:service` 与 `:feature:substore` 中的重复 `FormatUtils` 副本，消除潜在重复类打包风险；补齐 `:runtime:service` / `:feature:substore` 对 `core:*` 依赖后编译检查通过。
- [2026-02-24] 推进 `P2-2` 扩容（共享 UI 第二批大迁移）：迁移 `presentation/theme/{AnimationSpecs,TrafficChartConfig}.kt` 与一批通用组件到 `:ui`（`CenteredText`、`ConfirmDialog`、`CountryFlagCircle`、`DialogButtonRow`、`EditBottomSheet`、`LinkItem`、`LoadingAnimation`、`LoadingDotsWave`、`MessageHost`、`NullableSelector`、`RotatingRefreshButton`、`TrafficBarChart`）；补齐 `core:ui` 的 `core:android/core:common/core:locale`、`coil-compose`、`miuix-icons` 依赖并修复跨模块 `AnimationSpecs.IconTransition` 类型推断问题（`ProxyControlButton.kt` 显式 `FiniteAnimationSpec` cast）；编译检查通过。
- [2026-02-24] 推进 `P4-4` 第二批：将 `common/runtime/StartupGate.kt` 迁移至 `:platform` 并去除对 `:app BuildConfig` 的依赖（改用 `ApplicationInfo.FLAG_DEBUGGABLE` 判定）；`AppConstants.kt` 迁移至 `:ui`，`apksig` 依赖从 `:app` 移除并归属 `:platform`；编译检查通过。
- [2026-02-24] 完成 `P4-4` 收口：将 `IntentController.kt` 与 `ProxyAutoStartHelper.kt` 从 `app/common/util` 迁移至 `:runtime:client`（客户端集成辅助层），补齐 `:runtime:client` 的 `:data:settings` / `MMKV` / `koin-core` 依赖；`app/common/**` 目录已清空，编译检查通过。
- [2026-02-24] 推进 `P2-6` 第二批：在 `:feature:substore` 新增 `FeatureSubStoreModules.kt`，承接 `FeatureViewModel` / `SettingViewModel` 的 Koin 注册；`app/di/AppModule.kt` 不再直接注册上述 ViewModel，改为聚合 `featureSubStoreModules`；编译检查通过。
- [2026-02-24] 推进 `P2-2` 扩容（共享 UI 第三批）：迁移 `presentation/component/NavigationIcon.kt` 至 `:ui`，补齐 `core:ui` 的 `lifecycle-runtime-compose` 与 `compose-destinations` 依赖；编译检查通过。
- [2026-02-24] 修复构建一致性（MMKV 分架构版本）：将 `:di`、`:runtime:client`、`:runtime:service`、`:data:settings` 中硬编码 `MMKV 2.2.4` 改为与 `:app` 相同的按 `android.injected.build.abi` 动态选择版本（64 位 `2.2.4`，其他 `1.3.14`）；编译检查通过。
- [2026-02-24] 推进 `P2-2` 扩容（共享 UI 第四批）：将 `BottomBar` 重构为 `:ui` 的 `BottomBarContent` + `app` wrapper（Koin 状态注入保留在 `app`），并迁移 `ProfileCard.kt` 与 `presentation/util/ProfileUiExtensions.kt` 到 `:ui`（补齐 `:runtime:api` 依赖）；编译检查通过。
- [2026-02-23] 推进 `P3-4` 第一批：新建 `:feature:proxy` 并接入 `settings.gradle.kts` / `app` 依赖，迁移 `presentation/screen/{Proxy,Providers}.kt`、`presentation/screen/node/*`、`presentation/viewmodel/{ProxyViewModel,ProvidersViewModel,OverrideViewModel}.kt` 与 `presentation/util/ProxyNameFormat.kt`（保留原 package）；`ProxyPager` 重构为 `onNavigateToProviders` / `onOpenPanel` 回调，消除对 `app` compose-destinations 生成类与 `FeatureViewModel`/`WebView` 的直接依赖；`Providers` 改为模块内 `ProvidersContent` + `app` `@Destination` wrapper；新增 `FeatureProxyModules.kt` 承接 `Proxy/Providers/Override` ViewModel Koin 注册并从 `app/di` 移除对应注册；因 `OverrideScreen` 仍依赖 `EditorDataHolder + compose-destinations` 编辑器桥接，UI 文件暂回退留在 `app`（VM 已留在模块）；放开 `NodeGroupSheetContent` / `NodeSortPopup` 可见性以兼容 `ProxySheetActivity`；编译检查通过（`--% :app:compileDebugKotlin --no-daemon -Dkotlin.compiler.execution.strategy=in-process -Pkotlin.incremental=false`）。
- [2026-02-23] 推进 `P3-4` 第一批补完：新增 `feature/proxy` 内 `OverrideContent.kt`，将 `OverrideScreen` 重构为模块内容组件 + `app` wrapper（`EditorDataHolder` + `StringListEditor/KeyValueEditor` 导航桥接）；通过本地 helper composable 将 `PortInput/StringInput/StringListInput/StringMapInput` 适配为 `core:ui` 内容组件，避免 `feature:proxy` 依赖 `:app`；再次编译检查通过（同上 fallback 命令）。
- [2026-02-23] 推进 `P3-4` 第一批收口 + `P2-2` 扩容补充：将 `ProxySheetActivity` 中的 `ProxySheet` Compose UI 抽离为 `feature/proxy` 的 `ProxySheetContent.kt`（含分组/节点弹窗动画与 `ProxyViewModel` 交互），`app/ProxySheetActivity.kt` 缩减为主题装配 + `finish()` 壳层；同时将仅被该弹窗使用的 `presentation/util/WindowBlurEffect.kt` 迁移至 `:ui`；编译检查通过（同上 fallback 命令）。
- [2026-02-23] `feature:proxy` 收口清理：`ProxySheetActivity` 不再直接依赖 `node/*` 后，将 `NodeGroupSheetContent` / `NodeSortPopup` 可见性从临时 `public` 恢复为 `internal`；编译检查通过（同上 fallback 命令）。
- [2026-02-23] 推进 `P2-1` / `P2-2` 收口：将根目录 `lang/` 本地化源文件并入 `locale/lang/`，`locale/build.gradle.kts` 的 `fytxt.langSrcs` 改为模块内相对路径；同步更新 `AGENTS.md` 与文档中的翻译目录说明。随后将 `app/presentation/theme/{Color,Theme,SystemUi,NavigationTransitions}.kt` 迁移到 `:ui`，为 `:ui` 补齐 `:data:settings` 与 `androidx.core:core-ktx` 依赖；编译检查通过（同上 fallback 命令）。
- [2026-02-23] 架构重整（用户指令）：将 `:core:android` / `:core:common` / `:core:di` / `:core:locale` / `:core:ui` 平铺为根级模块 `:platform` / `:common` / `:di` / `:locale` / `:ui`，对应目录迁移到根目录（`platform/`、`common/`、`di/`、`locale/`、`ui/`）；统一修复 `settings.gradle.kts` 与全仓 Gradle `project()` 引用并通过编译检查（同上 fallback 命令）。
- [2026-02-23] 继续 `P2-2` / `app` UI 壳层清理：确认全仓已无 `:core:*` 基础子模块引用后，在 `PLAN` 中固化“`core/` 目录禁止新增子模块”的命名规则；将仅被 `MetaFeatureScreen` 使用的 `app/presentation/component/ConfigInput.kt` 删除，并把 `StringListInput` 的编辑器导航桥接内联为 `MetaFeature.kt` 私有 composable（直接依赖 `:ui` 的 `StringListInputContent`），使 `app/presentation/component` 仅剩 `BottomBar` 壳层组件。
- [2026-02-23] 编译检查通过：`--% :app:compileDebugKotlin --no-daemon -Dkotlin.compiler.execution.strategy=in-process -Pkotlin.incremental=false`（包含 `MetaFeature` 内联 `ConfigInput` 桥接、删除 `app/presentation/component/ConfigInput.kt` 与 `PLAN` 命名规则收口）。
- [2026-02-23] 继续 `P2-2` / `app` UI 壳层清理：将 `BottomBar` 的 Koin 状态桥接从 `app/presentation/component/BottomBar.kt` 内联到 `MainActivity.kt`（直接向 `:ui` 的 `BottomBarContent` 传入设置状态），删除 `app` 层 `BottomBar` wrapper；`app/presentation/component` 目录已清空；再次编译检查通过（同上 fallback 命令）。
- [2026-02-23] 完成 `P5-1`（阶段性结构审计）：确认基础模块已平铺为根级（`platform/common/di/locale/ui`），`runtime/*` / `data/*` / `feature/*` 已形成承载；盘点 `app` 当前剩余代码主要集中在 `presentation`（screen/viewmodel）与 `App/MainActivity/DI` 壳层。
- [2026-02-23] 完成 `P5-2`（清理）：删除迁移后残留空目录（`app`/`runtime:service` 空包、`locale/src/main/kotlin`、`app/src/main/res/font`）并清理根目录历史 `service/` 构建残留目录与 `scripts/__pycache__/`。
- [2026-02-23] 死代码清理（全局检查后删除）：确认 `app/clash/*`、`app/data/*`、`app/domain/*` 剩余文件均无有效调用（仅自引用或无消费者 DI 绑定）后，删除 `ImportExceptions`/`ImportState`、旧 `Profile/ProfilesStore/ExtensionStore`、`VpnState/VpnStatus`、`ProxyState/RunningMode/HealthStatus`，并移除 `AppModule` 中无用 `ProfilesStore` 注册；编译检查通过。清理后 `app` 仅剩 `presentation/`、`di/` 与 `App/MainActivity/ProxySheetActivity` 顶层文件。
- [2026-02-24] `app` 目录扁平化（按用户要求）：将 `app/presentation/{screen,viewmodel}` 物理迁移到与 `MainActivity.kt` 同级目录 `app/{screen,viewmodel}`，并同步将 app 本地 package 从 `com.github.yumelira.yumebox.presentation.(screen|viewmodel)` 改为 `com.github.yumelira.yumebox.(screen|viewmodel)`；清理 app 内导入归属（app 本地引用改用根级 `screen/viewmodel`，feature 模块内容组件/VM 仍保留 `presentation.*` 导入）；编译检查通过。迁移后 `app` 仅剩 `screen/`、`viewmodel/`、`di/` 与 `App/MainActivity/ProxySheetActivity` 顶层文件。

## 11. app Kotlin 文件全量清单（基线快照）

> 用途：后续迁移时逐项勾稽“保留 / 迁移 / 重命名 / 删除”。

- `App.kt` (98 行)
- `clash/exception/ImportExceptions.kt` (107 行)
- `clash/importer/ImportState.kt` (63 行)
- `common/AppConstants.kt` (53 行)
- `common/native/NativeLibraryManager.kt` (233 行)
- `common/runtime/StartupGate.kt` (164 行)
- `common/util/AppIconHelper.kt` (80 行)
- `common/util/AppUtil.kt` (78 行)
- `common/util/ArchiveUtil.kt` (161 行)
- `common/util/DeviceUtil.kt` (38 行)
- `common/util/DownloadUtil.kt` (244 行)
- `common/util/FormatUtils.kt` (77 行)
- `common/util/IntentController.kt` (83 行)
- `common/util/Link.kt` (27 行)
- `common/util/LocaleUtil.kt` (46 行)
- `common/util/PlatformIdentifier.kt` (24 行)
- `common/util/ProxyAutoStartHelper.kt` (74 行)
- `common/util/SystemProxyHelper.kt` (37 行)
- `common/util/ToastExt.kt` (32 行)
- `common/util/VpnUtils.kt` (31 行)
- `common/util/WebViewUtils.kt` (49 行)
- `data/model/AccessControlMode.kt` (37 行)
- `data/model/AutoCloseMode.kt` (37 行)
- `data/model/Profile.kt` (122 行)
- `data/model/ProxyMode.kt` (24 行)
- `data/model/Theme.kt` (26 行)
- `data/model/TrafficStatistics.kt` (90 行)
- `data/model/TunStack.kt` (28 行)
- `data/model/VpnState.kt` (31 行)
- `data/model/VpnStatus.kt` (26 行)
- `data/repository/AppSettingsRepository.kt` (32 行)
- `data/repository/FeatureSettingsRepository.kt` (13 行)
- `data/repository/LogRepository.kt` (138 行)
- `data/repository/NetworkInfoService.kt` (116 行)
- `data/repository/NetworkSettingsRepository.kt` (19 行)
- `data/repository/OverrideRepository.kt` (50 行)
- `data/repository/ProfileLinksRepository.kt` (12 行)
- `data/repository/ProvidersRepository.kt` (81 行)
- `data/repository/ProxyChainResolver.kt` (85 行)
- `data/repository/ProxyDisplaySettingsRepository.kt` (14 行)
- `data/repository/TrafficStatisticsCollector.kt` (123 行)
- `data/repository/TrafficStatisticsRepository.kt` (14 行)
- `data/store/AppSettingsStore.kt` (49 行)
- `data/store/ExtensionStore.kt` (28 行)
- `data/store/FeatureStore.kt` (49 行)
- `data/store/MMKVPreference.kt` (228 行)
- `data/store/MMKVProvider.kt` (29 行)
- `data/store/NetworkSettingsStore.kt` (35 行)
- `data/store/ProfileLinksStorage.kt` (44 行)
- `data/store/ProfilesStore.kt` (70 行)
- `data/store/ProxyDisplaySettingsStore.kt` (31 行)
- `data/store/TrafficStatisticsStore.kt` (198 行)
- `di/AppModule.kt` (89 行)
- `domain/facade/ProfilesRepository.kt` (177 行)
- `domain/facade/ProxyFacade.kt` (405 行)
- `domain/model/HealthStatus.kt` (25 行)
- `domain/model/ProxyDisplaySettings.kt` (54 行)
- `domain/model/ProxyGroupInfo.kt` (30 行)
- `domain/model/ProxyState.kt` (36 行)
- `domain/model/RunningMode.kt` (25 行)
- `domain/model/TrafficData.kt` (45 行)
- `MainActivity.kt` (326 行)
- `presentation/component/BottomBar.kt` (151 行)
- `presentation/component/BottomBarScrollBehavior.kt` (94 行)
- `presentation/component/Card.kt` (35 行)
- `presentation/component/CenteredText.kt` (59 行)
- `presentation/component/ConfigInput.kt` (188 行)
- `presentation/component/ConfirmDialog.kt` (116 行)
- `presentation/component/CountryFlagCircle.kt` (29 行)
- `presentation/component/DialogButtonRow.kt` (53 行)
- `presentation/component/EditBottomSheet.kt` (106 行)
- `presentation/component/EmasUpdateDialogHost.kt` (281 行)
- `presentation/component/EnumSelector.kt` (44 行)
- `presentation/component/LinkItem.kt` (47 行)
- `presentation/component/LoadingAnimation.kt` (159 行)
- `presentation/component/LoadingDotsWave.kt` (70 行)
- `presentation/component/MessageHost.kt` (133 行)
- `presentation/component/NavigationIcon.kt` (78 行)
- `presentation/component/NullableSelector.kt` (75 行)
- `presentation/component/ProfileCard.kt` (230 行)
- `presentation/component/RotatingRefreshButton.kt` (62 行)
- `presentation/component/ScreenLazyColumn.kt` (87 行)
- `presentation/component/SmallTitle.kt` (31 行)
- `presentation/component/TopAppBar.kt` (92 行)
- `presentation/component/TrafficBarChart.kt` (166 行)
- `presentation/icon/__Yume.kt` (25 行)
- `presentation/icon/yume/Activity.kt` (62 行)
- `presentation/icon/yume/Arrow-down-up.kt` (51 行)
- `presentation/icon/yume/Atom.kt` (53 行)
- `presentation/icon/yume/Badge-plus.kt` (51 行)
- `presentation/icon/yume/Bolt.kt` (51 行)
- `presentation/icon/yume/Chart-column.kt` (51 行)
- `presentation/icon/yume/Chromium.kt` (59 行)
- `presentation/icon/yume/Circle-fading-arrow-up.kt` (68 行)
- `presentation/icon/yume/Git-merge.kt` (48 行)
- `presentation/icon/yume/Github.kt` (50 行)
- `presentation/icon/yume/House.kt` (50 行)
- `presentation/icon/yume/Link-2.kt` (47 行)
- `presentation/icon/yume/Link.kt` (43 行)
- `presentation/icon/yume/List-chevrons-up-down.kt` (57 行)
- `presentation/icon/yume/Message.kt` (78 行)
- `presentation/icon/yume/Meta.kt` (140 行)
- `presentation/icon/yume/Package-check.kt` (67 行)
- `presentation/icon/yume/Play.kt` (34 行)
- `presentation/icon/yume/Redo-dot.kt` (48 行)
- `presentation/icon/yume/Rocket.kt` (94 行)
- `presentation/icon/yume/Scan-eye.kt` (74 行)
- `presentation/icon/yume/Scroll-text.kt` (63 行)
- `presentation/icon/yume/Settings-2.kt` (53 行)
- `presentation/icon/yume/Sparkles.kt` (66 行)
- `presentation/icon/yume/Speed.kt` (63 行)
- `presentation/icon/yume/Square.kt` (37 行)
- `presentation/icon/yume/Squares-exclude.kt` (56 行)
- `presentation/icon/yume/Substore.kt` (131 行)
- `presentation/icon/yume/Wifi-cog.kt` (99 行)
- `presentation/icon/yume/Zap.kt` (40 行)
- `presentation/icon/yume/Zashboard.kt` (107 行)
- `presentation/screen/About.kt` (151 行)
- `presentation/screen/AccessControl.kt` (462 行)
- `presentation/screen/AppSettings.kt` (379 行)
- `presentation/screen/Feature.kt` (191 行)
- `presentation/screen/Home.kt` (208 行)
- `presentation/screen/home/HomeIdleContent.kt` (113 行)
- `presentation/screen/home/HomeRunningContent.kt` (64 行)
- `presentation/screen/home/IpInfoDisplay.kt` (157 行)
- `presentation/screen/home/NodeInfoDisplay.kt` (110 行)
- `presentation/screen/home/ProxyControlButton.kt` (161 行)
- `presentation/screen/home/SpeedChart.kt` (59 行)
- `presentation/screen/home/TrafficDisplay.kt` (165 行)
- `presentation/screen/KeyValueEditor.kt` (566 行)
- `presentation/screen/Log.kt` (155 行)
- `presentation/screen/LogDetail.kt` (244 行)
- `presentation/screen/MetaFeature.kt` (348 行)
- `presentation/screen/NetworkSettings.kt` (183 行)
- `presentation/screen/node/NodeCard.kt` (271 行)
- `presentation/screen/node/NodeContent.kt` (315 行)
- `presentation/screen/node/NodeGrid.kt` (113 行)
- `presentation/screen/node/NodeGroups.kt` (237 行)
- `presentation/screen/node/NodeSortPopup.kt` (44 行)
- `presentation/screen/onboarding/ActivationWizardScreen.kt` (64 行)
- `presentation/screen/onboarding/AppStartScreen.kt` (52 行)
- `presentation/screen/onboarding/OnboardingModels.kt` (58 行)
- `presentation/screen/onboarding/OnboardingPage.kt` (148 行)
- `presentation/screen/onboarding/OnboardingState.kt` (159 行)
- `presentation/screen/onboarding/OnboardingWizard.kt` (297 行)
- `presentation/screen/onboarding/PrivacyPolicySheet.kt` (84 行)
- `presentation/screen/OpenSourceLicenses.kt` (211 行)
- `presentation/screen/Override.kt` (357 行)
- `presentation/screen/Profiles.kt` (1465 行)
- `presentation/screen/Providers.kt` (300 行)
- `presentation/screen/Proxy.kt` (412 行)
- `presentation/screen/Setting.kt` (262 行)
- `presentation/screen/TrafficStatistics.kt` (207 行)
- `presentation/theme/AnimationSpecs.kt` (65 行)
- `presentation/theme/Color.kt` (222 行)
- `presentation/theme/Dimens.kt` (53 行)
- `presentation/theme/Modifiers.kt` (77 行)
- `presentation/theme/NavigationTransitions.kt` (59 行)
- `presentation/theme/SystemUi.kt` (58 行)
- `presentation/theme/Theme.kt` (62 行)
- `presentation/theme/TrafficChartConfig.kt` (45 行)
- `presentation/util/ProxyNameFormat.kt` (34 行)
- `presentation/util/WindowBlurEffect.kt` (32 行)
- `presentation/viewmodel/AccessControlViewModel.kt` (521 行)
- `presentation/viewmodel/AppSettingsViewModel.kt` (63 行)
- `presentation/viewmodel/FeatureViewModel.kt` (223 行)
- `presentation/viewmodel/HomeViewModel.kt` (251 行)
- `presentation/viewmodel/LogViewModel.kt` (121 行)
- `presentation/viewmodel/NetworkSettingsViewModel.kt` (161 行)
- `presentation/viewmodel/OverrideViewModel.kt` (306 行)
- `presentation/viewmodel/ProfilesViewModel.kt` (386 行)
- `presentation/viewmodel/ProvidersViewModel.kt` (135 行)
- `presentation/viewmodel/ProxyViewModel.kt` (231 行)
- `presentation/viewmodel/SettingViewModel.kt` (56 行)
- `presentation/viewmodel/TrafficStatisticsViewModel.kt` (110 行)
- `presentation/webview/LocalWebView.kt` (215 行)
- `presentation/webview/WebViewScreen.kt` (53 行)
- `ProxySheetActivity.kt` (225 行)
- `remote/ServiceClient.kt` (74 行)
- `remote/ServiceExceptions.kt` (26 行)
- `service/AutoRestartService.kt` (94 行)
- `service/BaseService.kt` (18 行)
- `service/BroadcastReceiver.kt` (76 行)
- `service/clash/ClashRuntime.kt` (54 行)
- `service/clash/module/AppListCacheModule.kt` (42 行)
- `service/clash/module/CloseModule.kt` (15 行)
- `service/clash/module/ConfigurationModule.kt` (72 行)
- `service/clash/module/Module.kt` (64 行)
- `service/clash/module/NetworkObserveModule.kt` (131 行)
- `service/clash/module/SuspendModule.kt` (59 行)
- `service/clash/module/TimeZoneModule.kt` (17 行)
- `service/clash/module/TunModule.kt` (69 行)
- `service/ClashManager.kt` (168 行)
- `service/ClashService.kt` (96 行)
- `service/common/compat/Context.kt` (21 行)
- `service/common/compat/Intents.kt` (14 行)
- `service/common/constants/Components.kt` (10 行)
- `service/common/constants/Intents.kt` (42 行)
- `service/common/log/Log.kt` (9 行)
- `service/common/util/CoreRuntimeConfig.kt` (15 行)
- `service/common/util/Global.kt` (14 行)
- `service/common/util/Ticker.kt` (16 行)
- `service/DialerLaunchService.kt` (57 行)
- `service/LogRecordService.kt` (202 行)
- `service/notification/ServiceNotificationManager.kt` (134 行)
- `service/ProfileManager.kt` (240 行)
- `service/ProfileProcessor.kt` (637 行)
- `service/ProxyServiceContracts.kt` (28 行)
- `service/ProxyTileService.kt` (152 行)
- `service/remote/IClashManager.kt` (20 行)
- `service/remote/IFetchObserver.kt` (5 行)
- `service/remote/ILogObserver.kt` (5 行)
- `service/remote/IProfileManager.kt` (18 行)
- `service/runtime/config/AccessControlMode.kt` (7 行)
- `service/runtime/config/MMKVStoreProvider.kt` (49 行)
- `service/runtime/config/ServiceStore.kt` (50 行)
- `service/runtime/config/Store.kt` (83 行)
- `service/runtime/config/StoreProvider.kt` (18 行)
- `service/runtime/entity/Imported.kt` (19 行)
- `service/runtime/entity/Pending.kt` (19 行)
- `service/runtime/entity/Profile.kt` (48 行)
- `service/runtime/entity/ProfileExtensions.kt` (105 行)
- `service/runtime/entity/Selection.kt` (12 行)
- `service/runtime/records/ImportedDao.kt` (45 行)
- `service/runtime/records/PendingDao.kt` (45 行)
- `service/runtime/records/ProfileStore.kt` (77 行)
- `service/runtime/records/SelectionDao.kt` (45 行)
- `service/runtime/util/ContextUtils.kt` (47 行)
- `service/runtime/util/Coroutine.kt` (18 行)
- `service/runtime/util/InetAddressUtils.kt` (9 行)
- `service/runtime/util/Net.kt` (10 行)
- `service/runtime/util/Parcel.kt` (80 行)
- `service/runtime/util/ProfileUtils.kt` (11 行)
- `service/runtime/util/UUID.kt` (32 行)
- `service/runtime/util/UUIDSerializer.kt` (18 行)
- `service/ServiceNetworkObserver.kt` (109 行)
- `service/ServicePowerController.kt` (67 行)
- `service/StatusProvider.kt` (76 行)
- `service/TunService.kt` (242 行)
- `substore/CaseEngine.kt` (127 行)
- `substore/NetworkUtil.kt` (31 行)
- `substore/SubStorePaths.kt` (55 行)
- `substore/SubStoreService.kt` (107 行)
- `update/EmasUpdateManager.kt` (263 行)
- `WebViewActivity.kt` (80 行)

## 2026-02-23 后续治理日志（Gradle / Build Logic 收尾）

- 已完成 `build-logic/convention` 收尾修复：
  - 修复 `GolangTasksPlugin` 中 `doFirst { this }` receiver 类型错误（`Task`/`Exec` 混淆），改为捕获 `execTask`
  - `build-logic/convention/build.gradle.kts` 增加 `java` toolchain/source/target/release 配置，解决 `compileJava(24)` 与 `compileKotlin(17)` JVM target 不一致警告
  - `ConfigProvider` 反射调用匹配参数个数，避免误命中重载方法
  - `GropifyAccessor` 只匹配 `getPropertyValue(String)`，减少反射脆弱性
  - `AndroidSigning` 移除无意义中间变量，简化签名配置读取逻辑

- 已完成 `app/build.gradle.kts` 重构收尾：
  - 引入 `addAppProjectDependencies()` / `implementationAll(...)` 实际落地到依赖块，减少重复声明
  - `downloadGeoFiles` 改用 `tasks.registerGeoFilesDownloadTask(...)` helper，去掉脚本内重复 task 配置
  - `EMAS` 配置读取统一走 `readEmasConfigValues()`
  - `resourceConfigurations`（废弃）迁移为 `androidResources.localeFilters`
  - 修复 Kotlin DSL 顶层 `private type` 暴露问题（`private val emasConfig`）

- 已完成 `app` 依赖归属清理（基于 `app/src/main/kotlin` 全局 import 扫描）：
  - 从 `app/build.gradle.kts` 移除已迁出模块依赖：`okhttp`、`ktor-*`、`javet-node-android`、`pangutext-android`、`commons-compress`、`kotlinx-serialization-json`
  - 这些能力已由 `feature:*` / `data:*` 子模块承载，`app` 不再直接持有

- 验证：
  - `:app:compileDebugKotlin`（fallback 参数）通过


