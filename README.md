# BackToTFC Core

重返群峦（Return to TerraFirmaCraft）整合包核心模组。

通过 Mixin 注入原版 `CraftingMenu`，实现基于标签（Tag）的工作台配方等级系统。

## 设计理念

借鉴 MITE（Minecraft Is Too Easy）的工作台分级思路：**同一个 3×3 合成 GUI，不同等级的工作台开放不同范围的配方**。全部走标签驱动，KubeJS `/reload` 即刻生效。

## 标签系统

| 标签类型 | 格式 | 示例 |
|----------|------|------|
| 工作台等级 | `backtotfccore:workbench_tier_N` | `workbench_tier_1` → 原版工作台 |
| 配方等级 | `backtotfccore:recipe_tier_N` | `recipe_tier_1` → TFC 有序配方 |

- 未标记 → 无限制（通用）
- 无序配方 → 永远可用
- `recipeTier > blockTier` → 配方不可见

## 版本

- Minecraft: 1.21.1
- NeoForge: 21.1.233
- Mod 版本: 0.0.1

## 许可

MIT License
