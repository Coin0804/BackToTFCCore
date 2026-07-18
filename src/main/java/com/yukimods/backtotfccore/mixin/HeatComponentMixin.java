package com.yukimods.backtotfccore.mixin;

import net.dries007.tfc.common.component.heat.HeatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * lastTick == -2 时冻结温度：sanitize 跳过更新，calculateTemperature 直接返回原温度。
 */
@Mixin(HeatComponent.class)
public abstract class HeatComponentMixin {

    @Shadow
    private long lastTick;

    @Shadow
    private float lastTemperature;

    @Inject(method = "sanitize", at = @At("HEAD"), cancellable = true, remap = false)
    private void onSanitize(CallbackInfoReturnable<HeatComponent> cir) {
        if (lastTick == -2L) {
            cir.setReturnValue((HeatComponent) (Object) this);
        }
    }

    @Inject(method = "calculateTemperature", at = @At("HEAD"), cancellable = true, remap = false)
    private void onCalculateTemperature(CallbackInfoReturnable<Float> cir) {
        if (lastTick == -2L) {
            cir.setReturnValue(lastTemperature);
        }
    }
}
