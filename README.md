# BackToTFC Core

[English](#english) | [中文](#中文)

---

## English

Core mod for the **Return to TerraFirmaCraft** modpack. Implements a tag-driven workbench tier system, composter overhaul, and various TFC gameplay adjustments.

### Workbench Tier System

Workbenches are no longer a one-time upgrade. Each workbench has a tier (defined by block tag), and recipes have required tiers (defined by item tag). The server blocks crafting when the workbench tier is insufficient, while JEI greys out the "+" button on incompatible recipes.

| Tag Type | Format | Description |
|----------|--------|-------------|
| Workbench tier | `backtotfccore:workbench_tier_N` | Block tag on the workbench |
| Recipe tier | `backtotfccore:recipe_tier_N` | Item tag on the recipe output |
| Untagged | — | No restriction, works on all workbenches |

KubeJS `server_scripts/workbench_tiers.js` handles all tier assignments. Hot-reloadable via `/reload`.

### Composter Overhaul

- **Higher threshold**: requires 4 green + 4 brown items (vanilla TFC only needed 2+2)
- **Doubled output**: composter produces twice as much
- **Jade compatibility**: fixed the progress display in Jade tooltips

All implemented via clean Mixin injection — zero Shadow fields, zero refmaps.

### Mud Brick Workbench

A custom block with right-click GUI, using TFC's workbench container system. Has its own texture and recipes. Allows hide scraping on the TFC workbench surface.

### Architecture

```
src/main/java/com/yukimods/backtotfccore/
├── BackToTFCCore.java
├── mixin/
│   ├── CraftingMenuMixin.java              # Server-side tier enforcement
│   ├── RecipeTransferRegistrationMixin.java # JEI handler override
│   └── ComposterBlockEntityMixin.java       # Composter threshold + yield
├── network/
│   └── SyncWorkbenchPosPacket.java
├── jei/
│   ├── BackToTFCJeiPlugin.java
│   └── WorkbenchTransferInfo.java           # JEI "+" button grey-out
└── util/
    └── WorkbenchTierHelper.java             # Tag-driven tier queries
```

### Notes

- `CraftingMenu.slotChangedCraftingGrid` has **6 parameters** in 1.21.1 (includes `RecipeHolder`), not 5
- `ContainerLevelAccess.evaluate()` requires non-null return (internal `requireNonNull`)
- `slotChangedCraftingGrid` is also called by `InventoryMenu` — must guard with `instanceof CraftingMenu`
- JEI Plugin requires manual `META-INF/services/mezz.jei.api.IModPlugin` file

### Dependencies

- Minecraft 1.21.1 / NeoForge 21.1.233
- TerraFirmaCraft 4.1.3+
- JEI 19.27.0.340+

### License

MIT

---

## 中文

重返群峦整合包核心模组。实现基于标签的工作台等级系统、堆肥桶大修，以及多项 TFC 游戏性调整。

### 工作台等级系统

工作台不再是永久毕业。每个工作台有等级（block tag 定义），每个配方有需求等级（item tag 定义）。工作台等级不足时，服务端拦截合成，JEI 端灰掉 "+" 按钮。

| 标签类型 | 格式 | 说明 |
|----------|------|------|
| 工作台等级 | `backtotfccore:workbench_tier_N` | 打在方块上的 block tag |
| 配方等级 | `backtotfccore:recipe_tier_N` | 打在产物 item 上的 item tag |
| 无标签 | — | 无限制，所有工作台通用 |

KubeJS 脚本 `server_scripts/workbench_tiers.js` 管理所有等级分配，`/reload` 即可热更新。

### 堆肥桶大修

- **提高门槛**：需要 4 绿 + 4 棕物品（原版 TFC 仅需 2+2）
- **产量翻倍**：堆肥桶产出翻两倍
- **Jade 修复**：修正 Jade 工具提示中的进度显示

纯 Mixin 注入实现——零 Shadow、零 refmap。

### 依赖

- Minecraft 1.21.1 / NeoForge 21.1.233
- TerraFirmaCraft 4.1.3+
- JEI 19.27.0.340+

### 许可

MIT 协议
