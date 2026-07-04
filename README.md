# BackToTFC Core

重返群峦（Return to TerraFirmaCraft）整合包核心模组。

实现基于标签（Tag）的工作台配方等级系统：通过 Mixin 注入原版 `CraftingMenu` + JEI 客户端集成，等级不足时服务端拦截合成 + JEI "+" 按钮灰掉。

## 功能

- **服务端配方拦截**：`CraftingMenuMixin.afterSlotChanged` 在工作台 tier 不足时清空产物槽
- **JEI 客户端集成**：`WorkbenchTransferInfo` 灰掉 JEI "+" 按钮并显示"需要 N 级工作台"
- **网络同步**：`SyncWorkbenchPosPacket` 将工作台坐标从服务端发到客户端（解决 `ContainerLevelAccess.NULL` 问题）
- **纯标签驱动**：block tag 定工作台 tier，item tag 定配方 tier，KubeJS `/reload` 即时生效

## 架构

```
服务端: CraftingMenu 构造 → 捕获 BlockPos → SyncWorkbenchPosPacket → 客户端存储
                                                                          ↓
JEI 刷新: canHandle() ← getClientPos() ← getClientBlockTier() ← block tag
                ↓                              ↓
           recipeTier vs blockTier       getResultItemTier() ← item tag
```

## 标签系统

| 标签类型 | 格式 | 说明 |
|----------|------|------|
| 工作台等级 | `backtotfccore:workbench_tier_N` | block tag，打在方块上 |
| 配方等级 | `backtotfccore:recipe_tier_N` | item tag，打在产物 item 上 |
| 未标记 | — | 无限制，所有工作台通用 |

KubeJS 脚本 `server_scripts/workbench_tiers.js`：
- `#tfc:workbenches` → `workbench_tier_1`
- IE/Create namespace item → `recipe_tier_2`
- Mekanism/AE2 namespace item → `recipe_tier_3`

## 源码结构

```
src/main/java/com/yukimods/backtotfccore/
├── BackToTFCCore.java                        # Mod 入口 + 网络包注册
├── mixin/
│   ├── CraftingMenuMixin.java                # 服务端配方拦截 + 发网络包
│   └── RecipeTransferRegistrationMixin.java  # 阻断原版 JEI handler
├── network/
│   └── SyncWorkbenchPosPacket.java           # 客户端同步工作台坐标
├── jei/
│   ├── BackToTFCJeiPlugin.java               # @JeiPlugin
│   └── WorkbenchTransferInfo.java            # IRecipeTransferInfo 实现
└── util/
    └── WorkbenchTierHelper.java              # 纯 tag 驱动的 tier 查询
```

## 注意事项

- `CraftingMenu.slotChangedCraftingGrid` 在 1.21.1 有 **6 个参数**（多了 `RecipeHolder`），不是 5 个
- `ContainerLevelAccess.evaluate()` 要求 lambda 返回非 null（内部有 `requireNonNull`）
- `slotChangedCraftingGrid` 也被 `InventoryMenu` 调用，需要 `instanceof CraftingMenu` 检查
- JEI Plugin 需要 `META-INF/services/mezz.jei.api.IModPlugin` 手动创建

## 版本

- Minecraft: 1.21.1
- NeoForge: 21.1.233
- JEI: 19.27.0.340
- TFC: 4.1.3
- Mod 版本: 0.0.1

## 许可

MIT License
