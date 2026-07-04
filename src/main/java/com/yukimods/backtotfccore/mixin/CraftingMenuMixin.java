package com.yukimods.backtotfccore.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 注入 CraftingMenu，在配方匹配阶段按工作台等级屏蔽配方。
 *
 * 设计参考 MITE：同一个 3×3 GUI，不同方块开放不同配方。
 * 全标签驱动：block tag 定工作台等级，item tag 定产物配方等级。
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    /** 此 GUI 对应的工作台方块坐标 */
    @Unique
    private BlockPos backtotfccore$workbenchPos = BlockPos.ZERO;

    /**
     * 构造注入：从 ContainerLevelAccess 中捕获方块坐标。
     */
    @Inject(
        method = "<init>(ILnet/minecraft/world/entity/player/Inventory;"
                + "Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
        at = @At("TAIL")
    )
    private void onConstruct(int containerId,
                             net.minecraft.world.entity.player.Inventory inventory,
                             ContainerLevelAccess access,
                             CallbackInfo ci) {
        access.evaluate((level, pos) -> {
            this.backtotfccore$workbenchPos = pos.immutable();
            return null;
        });
    }

    /**
     * TAIL 注入：配方匹配完成后检查等级。
     *
     * 产物已写入 resultSlots。如果 workbench tier 不足，
     * 清空产物槽，玩家看到的是"配方不匹配"的效果。
     */
    @Inject(
        method = "slotChangedCraftingGrid("
                + "Lnet/minecraft/world/inventory/AbstractContainerMenu;"
                + "Lnet/minecraft/world/level/Level;"
                + "Lnet/minecraft/world/entity/player/Player;"
                + "Lnet/minecraft/world/inventory/CraftingContainer;"
                + "Lnet/minecraft/world/inventory/ResultContainer;"
                + ")V",
        at = @At("TAIL")
    )
    private static void afterSlotChanged(AbstractContainerMenu menu, Level level,
                                         net.minecraft.world.entity.player.Player player,
                                         CraftingContainer craftSlots,
                                         ResultContainer resultSlots,
                                         CallbackInfo ci) {
        if (resultSlots.isEmpty()) return;

        // 获取工作台方块等级
        BlockPos pos = ((CraftingMenuMixin) (Object) menu).backtotfccore$workbenchPos;
        if (pos == null || pos.equals(BlockPos.ZERO)) return;

        int blockTier = WorkbenchTierHelper.getBlockTier(level, pos);
        if (blockTier == 0) return; // 未标记 = 无限制

        // 通过产物 item tag 获取配方等级
        ItemStack result = resultSlots.getItem(0);
        int recipeTier = WorkbenchTierHelper.getResultItemTier(result);

        if (recipeTier > blockTier) {
            resultSlots.setItem(0, ItemStack.EMPTY);
            menu.broadcastChanges();
        }
    }
}
