package com.yukimods.backtotfccore.mixin;

import com.yukimods.backtotfccore.BackToTFCCore;
import com.yukimods.backtotfccore.network.SyncWorkbenchPosPacket;
import com.yukimods.backtotfccore.util.WorkbenchTierHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
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

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @Unique
    private BlockPos backtotfccore$workbenchPos = BlockPos.ZERO;

    // ===== 构造注入：捕获坐标 + 发网络包 =====

    @Inject(
        method = "<init>(ILnet/minecraft/world/entity/player/Inventory;"
                + "Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
        at = @At("TAIL")
    )
    private void onConstruct(int containerId,
                             Inventory inventory,
                             ContainerLevelAccess access,
                             CallbackInfo ci) {
        access.evaluate((level, pos) -> {
            this.backtotfccore$workbenchPos = pos.immutable();

            BackToTFCCore.LOGGER.info(
                "CraftingMenu server: containerId={} pos={}, {}, {} block={}",
                containerId, pos.getX(), pos.getY(), pos.getZ(),
                BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()));

            WorkbenchTierHelper.setWorkbenchPos(
                (CraftingMenu) (Object) this, pos);

            Player player = inventory.player;
            if (player instanceof ServerPlayer sp) {
                SyncWorkbenchPosPacket.sendToPlayer(sp, containerId, pos);
            } else {
                BackToTFCCore.LOGGER.warn(
                    "Player is not ServerPlayer (type={}), cannot send packet for containerId={}",
                    player.getClass().getSimpleName(), containerId);
            }

            return pos;
        });
    }

    // ===== 服务端配方等级拦截 =====

    @Inject(
        method = "slotChangedCraftingGrid("
                + "Lnet/minecraft/world/inventory/AbstractContainerMenu;"
                + "Lnet/minecraft/world/level/Level;"
                + "Lnet/minecraft/world/entity/player/Player;"
                + "Lnet/minecraft/world/inventory/CraftingContainer;"
                + "Lnet/minecraft/world/inventory/ResultContainer;"
                + "Lnet/minecraft/world/item/crafting/RecipeHolder;"
                + ")V",
        at = @At("TAIL")
    )
    private static void afterSlotChanged(AbstractContainerMenu menu, Level level,
                                         Player player, CraftingContainer craftSlots,
                                         ResultContainer resultSlots,
                                         net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe> recipe,
                                         CallbackInfo ci) {
        // slotChangedCraftingGrid 也会被 InventoryMenu（玩家 2×2 合成）调用
        if (!(menu instanceof CraftingMenu)) return;
        if (resultSlots.isEmpty()) return;

        BlockPos pos = ((CraftingMenuMixin) (Object) menu).backtotfccore$workbenchPos;
        if (pos == null || BlockPos.ZERO.equals(pos)) {
            BackToTFCCore.LOGGER.warn("Server slotChanged: pos is ZERO/null — skipping tier check");
            return;
        }

        int blockTier = WorkbenchTierHelper.getBlockTier(level, pos);
        if (blockTier == 0) {
            BackToTFCCore.LOGGER.warn("Server slotChanged: blockTier=0 for pos {}, {}, {} — no workbench_tier_N tag?",
                pos.getX(), pos.getY(), pos.getZ());
            return;
        }

        ItemStack result = resultSlots.getItem(0);
        int recipeTier = WorkbenchTierHelper.getResultItemTier(result);

        if (recipeTier > blockTier) {
            BackToTFCCore.LOGGER.info(
                "Server: BLOCKED recipe. blockTier={} recipeTier={} result={}",
                blockTier, recipeTier,
                BuiltInRegistries.ITEM.getKey(result.getItem()));
            resultSlots.setItem(0, ItemStack.EMPTY);
            menu.broadcastChanges();
        } else {
            BackToTFCCore.LOGGER.debug(
                "Server: allowed. blockTier={} recipeTier={} result={}",
                blockTier, recipeTier,
                BuiltInRegistries.ITEM.getKey(result.getItem()));
        }
    }
}
