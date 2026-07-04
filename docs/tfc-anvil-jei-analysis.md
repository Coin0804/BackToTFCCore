# TFC 砧等级 JEI 集成 — 技术分析

Minecraft 1.21.1 NeoForge / TFC 4.1.3 / JEI 19.27.0.340

## 架构概览

TFC 砧的 JEI "+" 按钮按等级禁用，分为两层：

### 第一层：Recipe 自带 tier

`AnvilRecipe` 有 `getMinTier()` 方法，每个配方对象知道自己需要几级砧。

### 第二层：IRecipeTransferInfo 运行时比较

关键类：`AnvilRecipeTransferInfo`（实现 `IRecipeTransferInfo<AnvilContainer, RecipeHolder<AnvilRecipe>>`）

```java
public boolean canHandle(AnvilContainer container, RecipeHolder<AnvilRecipe> recipe) {
    int recipeTier = recipe.value().getMinTier();                 // 配方需要几级
    AnvilBlockEntity anvil = container.getBlockEntity();          // 拿到方块实体
    int blockTier  = anvil.getTier();                             // 当前砧实际几级
    return recipe.isCorrectTier(blockTier);                       // recipeTier <= blockTier ?
}

public IRecipeTransferError getHandlingError(...) {
    return transferHelper.createUserErrorWithTooltip(
        Component.translatable("tfc.jei.transfer.error.anvil_forging_tier_too_low")
    );
    // ↑ JEI 收到错误 → 自动灰掉 "+" 按钮 + 鼠标悬停显示 tooltip
}
```

## 完整流程

```
1. 玩家打开青铜砧 (tier 2) → AnvilContainer 持有 AnvilBlockEntity(tier=2)
2. 玩家在 JEI 中查看秘银配方 (minTier=3)
3. JEI 调用 AnvilRecipeTransferInfo.canHandle(container, recipe)
4. recipeTier(3) > blockTier(2) → 返回 false
5. JEI 调用 getHandlingError() → 返回 IRecipeTransferError
6. JEI UI 灰掉 "+" 按钮，鼠标悬停显示 "砧等级不足"
```

## 关键 API

```java
// JEI SPI
public interface IRecipeTransferInfo<C extends AbstractContainerMenu, R> {
    Class<? extends C> getContainerClass();
    Optional<MenuType<C>> getMenuType();
    RecipeType<R> getRecipeType();
    boolean canHandle(C container, R recipe);
    IRecipeTransferError getHandlingError(C container, R recipe);  // default
    List<Slot> getRecipeSlots(C container, R recipe);
    List<Slot> getInventorySlots(C container, R recipe);
}

// JEI 辅助
public interface IRecipeTransferHandlerHelper {
    IRecipeTransferError createUserErrorWithTooltip(Component tooltip);
}

// 注册（JEI Plugin）
@JeiPlugin
public class MyPlugin implements IModPlugin {
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration reg) {
        reg.addRecipeTransferHandler(new MyTransferInfo(reg.getTransferHelper()));
    }
}
```

## 套用到我们工作台的对应关系

| TFC 砧 | 我们工作台 |
|--------|-----------|
| `AnvilRecipe.getMinTier()` | item tag `backtotfccore:recipe_tier_N` |
| `AnvilBlockEntity.getTier()` | block tag `backtotfccore:workbench_tier_N` |
| `AnvilContainer.getBlockEntity()` | `WorkbenchTierHelper.getWorkbenchPos(menu)` |
| `AnvilRecipeTransferInfo` | `WorkbenchRecipeTransferInfo` |
