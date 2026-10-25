---
title: Cross-Loader Compatibility
status: active
document_type: architecture-guideline
scope: fabric-neoforge
verified_commit: 0442bfc1b8ce4a6e0c9bef194e75a50f0bfde315
verified_at: 2026-10-25
---

# 跨加载器兼容设计

## 目标

Minecraft Access 同时支持 Fabric 和 NeoForge。新增功能必须满足：

- 默认体验在两个加载器上一致。
- 公共行为只实现一次。
- 不以“只有一个加载器支持”作为默认功能前提。
- 加载器专属能力必须有明确回退，不能阻断公共路径。
- 不为同一功能复制两套业务逻辑。

## 公共实现优先

当前项目把大部分代码放在根项目的公共源码中，两个平台入口只负责加载和注册：

- `fabric/MainClassFabric.java`
- `neoforge/MainClassNeoForge.java`
- 两者进入 `MainClass.init`

因此新逻辑默认放在 `org.mcaccess.minecraftaccess` 公共包中。

可以使用：

- Minecraft 公共客户端类。
- 项目现有 Balm/Kuma 跨加载器抽象。
- 项目现有 `MainClass.narrate`。
- 项目现有公共配置、mixin 和 addon API。

不能从公共业务类直接引用只在 Fabric 或只在 NeoForge 运行时存在的类。

## 当前背包改造的兼容矩阵

| 能力 | 公共实现 | 加载器差异 | 回退 |
|---|---|---|---|
| 逻辑焦点与直接朗读 | `InventoryControls` + 公共普通类 | 无 | 不适用 |
| 动作手势前光标同步 | `MouseSimulation`/`MouseUtils` 公共路径 | 无 | 原鼠标模拟路径 |
| 配方变体跟踪 | 原版 `RecipeButton` 公共访问器 | 无 | 现有 hover 叙述 |
| 配方纵向自动翻页 | `RecipeBookPage`/`RecipeBookComponent` | 无 | 显式 `Shift+I/K` |
| 分类注册 ID | 原版 `BuiltInRegistries.RECIPE_BOOK_CATEGORY` | 无 | 格式化 ID |
| 分类显示名称 | 项目语言键 + 标签解析器 | NeoForge 可带额外搜索分类 | 未翻译时朗读 ID |
| 键位重映射 | 现有 Kuma 跨加载器输入层 | 无 | 不适用 |

这四项新能力本身不需要两套加载器实现。

## 加载器专属能力的处理顺序

当某功能只有单一加载器提供 API 时，按以下顺序处理：

1. **寻找公共等价物**：优先使用原版类、项目已有抽象或跨加载器库。
2. **定义窄接口**：在公共包定义最少的行为，不暴露加载器类型。
3. **提供两端适配器**：支持端实现真实行为，另一端提供无操作或已知回退。
4. **保持功能可降级**：缺少可选 API 时，核心导航、朗读和原版操作仍可用。
5. **增加双端测试**：至少验证公共路径在 Fabric 和 NeoForge 构建中都能加载。

示例：

```java
public interface OptionalCategoryMetadata {
    Optional<Component> label(ExtendedRecipeBookCategory category);
}
```

这是设计示意。当前需求不需要新增这个加载器接口，因为分类 ID 和原版分类对象已经足够生成可工作的回退。

## NeoForge 额外搜索分类

NeoForge 的 `RecipeBookManager` 提供模组搜索分类映射，但它是 NeoForge 专属能力。

规则：

- 公共代码不导入 `RecipeBookManager`。
- Fabric 不把缺少该管理器视为错误。
- 普通已注册分类在两端使用相同解析路径。
- 额外搜索分类可通过可选兼容层贡献名称。
- 没有贡献名称时朗读注册 ID。
- 配方导航、分页和原版点击不依赖该可选层。

## 最小代码干预

实现分阶段替换，不创建第二套背包控制器：

1. 在现有 `InventoryControls` 中增加直接朗读和纵向边界分支。
2. 在现有 `MouseSimulation`/`MouseUtils` 旁增加一个待执行光标动作状态。
3. 在现有 `RecipeButtonMixin` 中只保留状态读取，把变体比较交给公共普通类。
4. 在现有 `GroupGenerator` 配方分支过滤不可见按钮。
5. 用一个公共分类标签解析类集中处理名称，不把名称逻辑散落到按键回调。

不为这些改动重写 `SlotsGroup`、`SlotItem` 或全部 Screen 适配。

## 验收

每次背包改动必须运行：

```powershell
.\gradlew.bat :fabric:build :neoforge:build test -x check --offline
```

并确认：

- 两端都成功加载客户端模块。
- 公共类没有直接引用加载器专属运行时类。
- Fabric 和 NeoForge 的行为差异只存在于明确的适配层。
- NeoForge 可选分类缺失时，Fabric 仍有 ID 回退。
- Java 公共测试覆盖边界、朗读和动作顺序。

## 相关文档

- [系统总览](system-overview.md)
- [背包目标设计](../inventory-navigation/target-design.md)
- [测试策略](../inventory-navigation/test-strategy.md)
