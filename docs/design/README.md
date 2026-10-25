---
title: Minecraft Access Design Knowledge Base
status: implementation-active
language: zh-CN
project_version: 1.12.1-SNAPSHOT
verified_commit: 0442bfc1b8ce4a6e0c9bef194e75a50f0bfde315
verified_at: 2026-10-25
audiences:
  - contributor
  - maintainer
---

# 设计知识库

这里是 Minecraft Access 的开发设计资料入口，面向需要理解系统、定位代码或继续设计的开发者和维护者。用户文档仍位于 `docs/content/`，本目录不参与 Hugo 网站发布。

## 阅读顺序

1. 先读 [系统总览](architecture/system-overview.md)，理解平台入口、功能模块、事件、配置和朗读链路。
2. 任何跨 Fabric/NeoForge 改动先读 [跨加载器兼容设计](architecture/cross-loader-compatibility.md)。
3. 修改背包功能前读 [背包导航现状](inventory-navigation/current-state.md)。
4. 调查配方书延迟或第二页静默时读 [延迟与静默分析](inventory-navigation/latency-analysis.md)。
5. 比较交互方案时读 [交互方案研究](inventory-navigation/alternatives.md)。
6. 实现新的背包交互前读 [目标设计](inventory-navigation/target-design.md) 和对应 ADR。
7. 编写测试前读 [测试策略](inventory-navigation/test-strategy.md)。
8. 核对外部资料时读 [证据与源码索引](evidence/source-index.md)。

## 文档职责

| 文档 | 回答的问题 | 权威性 |
|---|---|---|
| `architecture/system-overview.md` | 项目由哪些部分组成，控制如何流经系统？ | 当前状态 |
| `architecture/cross-loader-compatibility.md` | 如何保证 Fabric 和 NeoForge 使用同一套核心行为？ | 架构约束 |
| `inventory-navigation/current-state.md` | 今天的背包导航、朗读和鼠标操作如何工作？ | 当前状态 |
| `inventory-navigation/latency-analysis.md` | 配方书为什么慢，哪些结论已证实？ | 问题分析 |
| `inventory-navigation/alternatives.md` | 成熟模组采用什么方案，各自代价是什么？ | 外部研究 |
| `inventory-navigation/target-design.md` | 推荐怎样改造，接口和时序是什么？ | 目标设计 |
| `inventory-navigation/test-strategy.md` | 怎样证明功能、时序和无障碍体验没有回归？ | 验收设计 |
| `adr/0001-logical-focus-and-lazy-cursor-sync.md` | 为什么导航不移动鼠标，而操作时才同步？ | 架构决定 |
| `evidence/source-index.md` | 结论依据在哪里，如何复现？ | 证据索引 |

## 维护约定

- 把 `verified_commit` 作为事实基线。代码更新后，先确认相关代码是否变化，再修改文档。
- 优先读取目标文档及其引用的 `source_refs`，不要从文件名猜测实现。
- 文档中的“已确认”必须有源码、测试或权威文档证据；“假设”必须保留验证方法。
- 不复制外部模组实现。外部源码只用于模式研究，并遵守 [证据索引](evidence/source-index.md) 中记录的许可证。
- 一处事实只保留一个权威位置。其他文档使用链接，不复制大段实现说明。
- 修改公共交互、线程模型、叙述顺序或跨平台行为时，新增 ADR，并更新本索引。

## 证据等级

| 等级 | 含义 | 示例 |
|---|---|---|
| E1 | 当前仓库源码直接证明 | 键位回调调用 `moveToSlotItem` |
| E2 | 当前构建使用的 Minecraft 源码或自动化测试证明 | 配方按钮按 `time / 30` 选取显示物 |
| E3 | 权威外部源码或文档提供参考 | Controlify 使用逻辑焦点和吸附虚拟光标 |
| H | 合理假设，尚需运行时验证 | 某个延迟在用户设备上的具体占比 |

## 核心术语

- **逻辑焦点**：Minecraft Access 自己维护的当前目标，不要求硬件鼠标停在该目标上。
- **光标同步**：真正执行鼠标操作前，把硬件鼠标移动到逻辑焦点的一次性动作。
- **配方按钮**：原版 `RecipeButton`。一个按钮可能对应多个配方结果，并按原版动画轮换。
- **变体**：一个配方按钮当前轮换显示出的具体 `ItemStack`，例如不同木材的木板。
- **动作手势**：`[`、`]`、`\` 等触发鼠标按下、释放或点击的 Minecraft Access 键位。
- **直接朗读**：由 [MainClass.java](../../src/main/java/org/mcaccess/minecraftaccess/MainClass.java#L180) 调用现有屏幕阅读器，不依赖鼠标悬停副作用。

## 当前快照

- 当前分支：`dev`
- 当前提交：`0442bfc1b8ce4a6e0c9bef194e75a50f0bfde315`
- Minecraft：`26.3`
- Gradle Wrapper：`9.8.2`
- Java toolchain：`25`
- 已验证构建：`.\gradlew.bat build test -x check --offline`

背包导航最小改造已实现：

- 导航直接朗读，不移动硬件鼠标。
- 鼠标动作前同步光标。
- 配方变体按显示内容实时朗读。
- 配方只导航可见按钮。
- 仅纵向边界自动翻页。
- 分类切换朗读名称和位置。

Fabric 与 NeoForge 开发客户端均完成初始化验证；真实合成台逐项人工验收仍待执行。
