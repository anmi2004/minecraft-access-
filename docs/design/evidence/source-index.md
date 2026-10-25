---
title: Evidence and Source Index
status: active
document_type: evidence-index
verified_commit: 0442bfc1b8ce4a6e0c9bef194e75a50f0bfde315
verified_at: 2026-10-25
implementation_snapshot: 2026-10-25 working-tree
---

# 证据与源码索引

## 固定版本

| 来源 | 版本或提交 | 日期 |
|---|---|---|
| Minecraft Access | `0442bfc1b8ce4a6e0c9bef194e75a50f0bfde315` (`dev`) | 2026-10-22 |
| Minecraft / NeoForge 源码 | Minecraft `26.3`，NeoForge `26.3.0.95` | 构建生成 |
| Controlify | `68dba946a2732c0ab2112fddbc3abe05a491a565` | 2026-10-19 |
| MidnightControls | `719dfdfe9f137b55d7314c9ed87b94c95c170292` | 2026-08-02 |
| Fabric Docs | `cd29ea69537011556c268ce0e8c6f08480435aef` | 2026-10-24 |

## 项目源码

### 改造前基线

### 架构与入口

| 事实 | 源码 |
|---|---|
| Fabric 和 NeoForge 共用 `MainClass.init` | `src/main/java/org/mcaccess/minecraftaccess/fabric/MainClassFabric.java`, `src/main/java/org/mcaccess/minecraftaccess/neoforge/MainClassNeoForge.java`, `src/main/java/org/mcaccess/minecraftaccess/MainClass.java` |
| 模块在全局客户端 tick 中注册 | `MainClass.java`, `InventoryControls.java` |
| 主导航朗读出口 | `MainClass.narrate(String, boolean)` |
| 原生叙述转发到平台屏幕阅读器 | `mixin/GameNarratorMixin.java` |
| 配置使用 AutoConfig | `Config.java` |
| 焦点工具已存在但背包未使用 | `utils/ui/NavigationUtils.java` |
| NeoForge 额外搜索分类管理器是加载器专属 | `net/neoforged/neoforge/client/RecipeBookManager.java` |

### 背包导航

| 事实 | 源码符号 |
|---|---|
| 导航时移动鼠标 | `InventoryControls.focusSlotItem`, `InventoryControls.moveToSlotItem` |
| 每约 150 ms 重建槽位组 | `InventoryControls.tick`, `Config.InventoryControls.delayMilliseconds` |
| 方向图由索引预连线 | `SlotsGroup.mapTheGroupList` |
| 普通槽位直接朗读 | `InventoryControls.getCurrentSlotNarrationText` |
| 配方按钮没有真实 `Slot` | `GroupGenerator.recipeBookGroups`, `SlotItem(int, int)` |
| 配方集合保存在静态字段 | `GroupGenerator.recipesOnTheScreen`, `RecipeBookPageMixin` |
| 配方导航未过滤可见性 | `GroupGenerator.java:697-704` |
| 延迟光标使用 Timer | `MouseUtils.moveAfterDelay` |
| 鼠标动作键 | `MouseSimulation.initialize` |
| 切换分类只写 debug 日志 | `InventoryControls.changeRecipeTab` |
| 横向边界当前朗读无项目 | `InventoryControls.focusSlotItemAt` |

### 2026-10-25 工作区实现

| 事实 | 源码 |
|---|---|
| 导航直接朗读且不移动光标 | `InventoryControls.focusSlotItem` |
| 动作前只同步一次光标 | `MouseSimulation`, `CursorActionSequence` |
| 逻辑目标失效时取消鼠标按压 | `InventoryControls.syncFocusedCursorForMouseAction`, `CursorActionSequence.begin` |
| 配方按钮保存为正式目标 | `SlotItem.recipeButton`, `GroupGenerator.recipeBookGroups` |
| 配方只收录可见按钮 | `GroupGenerator.recipeBookGroups` |
| 配方变体每 tick 检测 | `InventoryControls.tickRecipeVariant`, `FocusedVariantTracker` |
| 变体键包含组件和数量 | `InventoryControls.RecipeVariant` |
| 只有纵向边界自动翻页 | `InventoryControls.focusSlotItemAt`, `changeRecipeBookPage` |
| 分类名称集中解析 | `RecipeBookCategoryLabels` |
| 分类和页按钮使用原版 GUI 点击 | `InventoryControls.clickRecipeBookWidget` |
| Timer 延迟移动已删除 | `MouseUtils` 不再包含 `moveAfterDelay` |
| 普通控件焦点不阻断背包快捷键 | `InventoryControls` 的 Kuma 映射调用 `ignoreScreenFocus()` |
| 文本输入优先于背包快捷键 | `InventoryScreenInput.shouldHandle` |

新增纯逻辑测试：

- `RecipeBookPageNavigatorTest`
- `CursorActionSequenceTest`
- `FocusedVariantTrackerTest`
- `RecipeBookNarrationTest`
- `RecipeBookCategoryLabelsTest`
- `InventoryScreenInputTest`

### 配方选择后的快捷键回归

| 事实 | 证据 |
|---|---|
| 相关输入重构 | `77e97af1`，Kuma for Inventory Controls |
| 首次发布标签 | `v1.12.0-alpha.2` |
| Kuma 默认焦点保护 | `AbstractManagedKeyMappingBuilder.ignoresScreenFocus = false` |
| Kuma 分发时跳过聚焦界面 | `FabricKumaAPI.lambda$onInitializeClient$3` |
| 原版选中配方后设置焦点 | `AbstractRecipeBookScreen.mouseClicked` 调用 `setFocused(recipeBookComponent)` |
| 当前修复 | 映射使用 `ignoreScreenFocus()`，文本输入由 `getCapturingSearchBox()` 单独保护 |

上一轮尝试用组 key/index 恢复和跨组回退解释该问题，但 Kuma 焦点保护生效时输入回调根本不会执行，因此该实现已删除。

### 配方按钮

| 事实 | 源码 |
|---|---|
| 截获原版叙述 | `mixin/RecipeButtonMixin.java` |
| 同一物品 5 秒重复节流 | `RecipeButtonMixin.interval` |
| 鼠标抖动已移除 | `RecipeButtonMixin` 不再引用 `MouseUtils`，也不包含 `shakeTheMouse` |
| 逻辑焦点变体由客户端 tick 直接检测 | `InventoryControls.tickRecipeVariant`, `FocusedVariantTracker` |

## Minecraft 26.3 生成源码

本地生成位置：

```text
neoforge/build/moddev/artifacts/minecraft-patched-26.3.0.95-sources.jar
```

该 JAR 来自 `:neoforge:createMinecraftArtifacts`，不是仓库提交的一部分。相关源文件：

- `net/minecraft/client/gui/screens/recipebook/RecipeBookComponent.java`
- `net/minecraft/client/gui/screens/recipebook/RecipeBookPage.java`
- `net/minecraft/client/gui/screens/recipebook/RecipeButton.java`
- `net/minecraft/client/gui/screens/recipebook/RecipeCollection.java`
- `net/minecraft/client/gui/screens/recipebook/RecipeBookTabButton.java`
- `net/minecraft/client/gui/screens/recipebook/SearchRecipeBookCategory.java`
- `net/minecraft/client/gui/screens/inventory/AbstractRecipeBookScreen.java`
- `net/minecraft/world/item/crafting/ExtendedRecipeBookCategory.java`
- `net/minecraft/world/item/crafting/RecipeBookCategory.java`
- `net/minecraft/world/item/crafting/RecipeBookCategories.java`

已核对事实：

- `SlotSelectTime` 为 `floor(time / 30)`。
- `RecipeButton.getDisplayStack()` 使用当前轮换索引。
- `RecipeBookPage.buttons` 是固定容量为 20 的按钮列表。
- 原版在按钮不足一页时将多余按钮设为不可见。
- 原版在鼠标点击翻页按钮后立即更新当前页按钮。
- `RecipeBookTabButton` 只保存 `TabInfo` 和分类对象，不保存分类显示名称。
- `ExtendedRecipeBookCategory` 是空标记接口。
- `RecipeBookCategory` 没有名称字段。
- 原版 `en_us.json` 没有配方分类标题翻译。
- 已注册分类可通过 `BuiltInRegistries.RECIPE_BOOK_CATEGORY.getKey(category)` 获得注册 ID。

重新生成：

```powershell
.\gradlew.bat :neoforge:createMinecraftArtifacts
```

## 用户文档

- [Inventory Controls 已知限制](../../content/features.md#L111)
- [Inventory Controls 键位](../../content/keybindings.md#L58)
- [Inventory Controls 配置](../../content/config.md#L83)

已记录的用户症状包括：

- 配方书项朗读约延迟一秒。
- 第二页以后有时完全不朗读。
- 模组界面持续修改物品名称会导致重复朗读。

## 项目 issue

### #337 - Inventory Control: simplify crafting to list loop selecting

https://github.com/minecraft-access/minecraft-access/issues/337

核心提案：

- 打开合成台。
- 在可制作配方列表中循环选择。
- 激活后自动合成并朗读结果位置。

已有评论还提到：

- 可考虑直接支持 JEI。
- 可考虑选择并适配一个控制器风格合成模组。

### #274 - Support JEI, make its screen accessible

https://github.com/minecraft-access/minecraft-access/issues/274

主要约束：

- JEI/REI 支持更多第三方配方。
- 配方查看器与主 Screen 并行存在。
- 反射不是稳定方案。
- 需要稳定的可选依赖 API 或专门适配层。

## 外部参考模组

### Controlify

仓库：

https://github.com/isXander/Controlify

许可证：LGPL-3.0。

关键源码：

- `src/main/java/dev/isxander/controlify/screenop/ScreenProcessor.java`
- `src/main/java/dev/isxander/controlify/screenop/ComponentProcessor.java`
- `src/main/java/dev/isxander/controlify/screenop/CustomFocus.java`
- `src/main/java/dev/isxander/controlify/screenop/compat/vanilla/AbstractContainerScreenProcessor.java`
- `src/main/java/dev/isxander/controlify/screenop/compat/vanilla/RecipeBookScreenProcessor.java`
- `src/main/java/dev/isxander/controlify/virtualmouse/VirtualMouseHandler.java`
- `src/main/java/dev/isxander/controlify/virtualmouse/SnapUtils.java`
- `src/main/java/dev/isxander/controlify/mixins/feature/virtualmouse/snapping/AbstractRecipeBookScreenMixin.java`
- `src/gametest/java/dev/isxander/controlify/gametest/tests/RecipeBookNavigationTests.java`

采用的参考模式：

- 普通控件使用原版焦点路径。
- 容器使用虚拟光标和吸附点。
- 槽位动作使用 `ContainerInput`。
- 配方书有专用按钮动作。
- 关键导航有 GameTest。

### MidnightControls

仓库：

https://github.com/TeamMidnightDust/MidnightControls

许可证：MIT。

关键源码：

- `src/main/java/eu/midnightdust/midnightcontrols/client/controller/InputHandlers.java`
- `src/main/java/eu/midnightdust/midnightcontrols/client/util/InventoryUtil.java`
- `src/main/java/eu/midnightdust/midnightcontrols/client/controller/InputManager.java`
- `src/main/java/eu/midnightdust/midnightcontrols/client/mixin/HandledScreenMixin.java`
- `src/main/java/eu/midnightdust/midnightcontrols/client/mixin/RecipeBookWidgetAccessor.java`

采用的参考模式：

- 方向键寻找指定方向最近的槽位。
- 虚拟光标使用内部目标和插值。
- 操作时直接调用 `slotClicked`。
- 配方分类有专用输入处理。

## Fabric 官方文档

- Key Mappings: https://docs.fabricmc.net/develop/key-mappings
- Events: https://docs.fabricmc.net/develop/events/
- Custom Screens: https://docs.fabricmc.net/develop/rendering/gui/custom-screens
- Custom Widgets: https://docs.fabricmc.net/develop/rendering/gui/custom-widgets
- GUI Graphics: https://docs.fabricmc.net/develop/rendering/gui-graphics

与本设计有关的结论：

- Screen 和控件输入应在 GUI 事件层处理。
- Widget 可通过 `NarrationSupplier` 和 `NarratableEntry` 提供无障碍信息。
- 官方事件优先于不必要的行为 Mixin。
- 未暴露的区域仍可用 Mixin 创建自定义事件，但业务逻辑应位于普通类中。

## 研究方法

1. 从用户症状和现有文档建立问题范围。
2. 从当前源码追踪导航、配方、鼠标和叙述数据流。
3. 从构建生成的 Minecraft 源码核对原版按钮和分页行为。
4. 读取成熟控制器模组的完整相关实现，不只查看摘要。
5. 对照 Fabric 官方接口和文档。
6. 将结论分为直接证据、外部参考和待验证假设。

## 2026-10-25 验证记录

已执行：

```powershell
.\gradlew.bat clean test :fabric:build :neoforge:build -x check
.\gradlew.bat clean build test -x check --offline --no-daemon
.\gradlew.bat :fabric:build :neoforge:build --offline --no-daemon
.\gradlew.bat :fabric:runClient --no-daemon
.\gradlew.bat :neoforge:runClient --no-daemon
.\gradlew.bat checkstyleMain --offline --no-daemon
```

结果：

- 完整构建和测试成功。
- JUnit：8 个测试套件、21 个测试、0 failures、0 errors。
- 干净构建产物只包含 `InventoryScreenInput`，不包含已回滚的 `InventoryGroupNavigator`。
- Fabric 开发客户端启动到首次辅助功能界面。
- NeoForge 开发客户端完成模组、Mixin、资源和 Tolk/NVDA 初始化，并直接进入首次辅助功能界面。
- `neoforge.mods.toml` 保留 `logoFile` 以兼容旧版本，同时添加方形 `iconFile`；NeoForge 26.3 的弃用警告和加载警告界面已消失。
- `checkstyleTest` 通过；本次修改的 Java 文件没有 Checkstyle error 或 warning。
- `zh_cn.json` 与 `en_us.json` 键集合一致（468/468）。
- 全项目 `checkstyleMain` 仍有仓库既有错误，位于 `addon/worldnarrators/MinecraftAccess.java`。
- 未自动进入世界操作真实合成台，也未完成逐项屏幕阅读器主观验收。

## 已知限制

- 未在本次文档编写过程中运行带 NVDA、VoiceOver 或 Orca 的实机计时。
- 用户设备上的具体延迟贡献仍应通过测试策略中的计数器验证。
- Minecraft 26.3 是快速变化的代码基线，升级游戏版本后必须重新确认内部字段和方法。
- 外部仓库使用浅克隆，研究时固定到本文列出的提交。

## 许可证说明

本文档和项目后续实现不得直接复制 Controlify 的 LGPL 源码。可以依据公开行为、接口思想和独立实现进行设计。

引用外部资料时保留仓库地址、提交号和许可证。Fabric 文档按其仓库许可证使用，仅作链接和事实摘要。
