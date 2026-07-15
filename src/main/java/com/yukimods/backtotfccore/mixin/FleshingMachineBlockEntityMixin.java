package com.yukimods.backtotfccore.mixin;

import com.hermitowo.advancedtfctech.common.blockentities.FleshingMachineBlockEntity;
import com.hermitowo.advancedtfctech.common.recipes.FleshingMachineRecipe;
import com.yukimods.backtotfccore.util.FleshingMachineExtraDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在去肉机配方完成时，将 {@code extra_drop} 副产物弹出到机器前方。
 */
@Mixin(value = FleshingMachineBlockEntity.class, remap = false)
public abstract class FleshingMachineBlockEntityMixin {

    @Inject(method = "tickServer",
            at = @At(value = "INVOKE",
                     target = "Lnet/dries007/tfc/common/recipes/outputs/ItemStackProvider;"
                            + "getStack(Lnet/minecraft/world/item/ItemStack;)"
                            + "Lnet/minecraft/world/item/ItemStack;",
                     shift = At.Shift.AFTER))
    private void backtotfccore$onRecipeComplete(CallbackInfo ci) {
        var self = (FleshingMachineBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide) return;

        FleshingMachineRecipe recipe = self.cachedRecipe.get();
        if (recipe == null) return;

        ItemStack extra = FleshingMachineExtraDrops.get(recipe);
        if (extra.isEmpty()) return;

        BlockPos pos = self.getBlockPos();
        Direction facing = self.getFacing();
        double x = pos.getX() + 0.5 - facing.getStepX() + facing.getClockWise().getStepX();
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5 - facing.getStepZ() + facing.getClockWise().getStepZ();

        var entity = new ItemEntity(level, x, y, z, extra.copy());
        level.addFreshEntity(entity);
    }
}
