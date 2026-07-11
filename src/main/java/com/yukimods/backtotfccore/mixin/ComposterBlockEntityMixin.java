package com.yukimods.backtotfccore.mixin;

import net.dries007.tfc.common.blockentities.ComposterBlockEntity;
import net.dries007.tfc.common.blocks.devices.TFCComposterBlock;
import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.util.Helpers;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(ComposterBlockEntity.class)
public abstract class ComposterBlockEntityMixin {

    @Unique
    private int backtotfccore$getGreen() {
        return ((ComposterBlockEntity) (Object) this).getGreen();
    }

    @Unique
    private int backtotfccore$getBrown() {
        return ((ComposterBlockEntity) (Object) this).getBrown();
    }

    @Unique
    private IItemHandlerModifiable backtotfccore$getInventory() {
        try {
            Class<?> c = ComposterBlockEntity.class;
            while (c != null) {
                try {
                    Field f = c.getDeclaredField("inventory");
                    f.setAccessible(true);
                    return (IItemHandlerModifiable) f.get(this);
                } catch (NoSuchFieldException nsfe) {
                    c = c.getSuperclass();
                }
            }
        } catch (Exception e) {}
        return null;
    }

    // ============ randomTick — 改门槛 + 产量 ============
    // 原版: green>=16 && brown>=16 && !isRotten && timePassed → 产 1 个
    // 新版: green+brown>=4 && !isRotten && timePassed → 产 total 个
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void onRandomTick(CallbackInfo ci) {
        var self = (ComposterBlockEntity) (Object) this;
        int green = backtotfccore$getGreen();
        int brown = backtotfccore$getBrown();
        int total = green + brown;
        var inv = backtotfccore$getInventory();
        if (inv == null) return;

        // checkAndSetEmpty
        if (self.isReady() && inv.getStackInSlot(0).isEmpty()) {
            self.reset();
            return;
        }

        // 新条件: green >= 4 && brown >= 4 且没腐烂 且时间到
        if (green >= 4 && brown >= 4 && !self.isRotten()
            && self.getTicksSinceUpdate() > self.getReadyTicks()) {
            inv.setStackInSlot(0, new ItemStack(TFCItems.COMPOST.get(), Math.floorDiv(total, 4)));
            self.setState(TFCComposterBlock.CompostType.READY);
        }

        // 腐烂扩散
        if (self.isRotten()) {
            Helpers.tickInfestation(self.getLevel(), self.getBlockPos(), 5, null);
        }
        ci.cancel();
    }

    // ============ use() 取货量 = 槽内全部 ============
    @ModifyArg(
        method = "use",
        at = @At(value = "INVOKE",
                 target = "Lnet/neoforged/neoforge/items/ItemStackHandler;extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;",
                 ordinal = 0),
        index = 1,
        remap = false
    )
    private int modifyExtractNormal(int original) {
        var inv = backtotfccore$getInventory();
        if (inv != null) {
            ItemStack s = inv.getStackInSlot(0);
            if (!s.isEmpty()) return s.getCount();
        }
        return 1;
    }

    @ModifyArg(
        method = "use",
        at = @At(value = "INVOKE",
                 target = "Lnet/neoforged/neoforge/items/ItemStackHandler;extractItem(IIZ)Lnet/minecraft/world/item/ItemStack;",
                 ordinal = 1),
        index = 1,
        remap = false
    )
    private int modifyExtractRotten(int original) {
        var inv = backtotfccore$getInventory();
        if (inv != null) {
            ItemStack s = inv.getStackInSlot(0);
            if (!s.isEmpty()) return s.getCount();
        }
        return 1;
    }
}
