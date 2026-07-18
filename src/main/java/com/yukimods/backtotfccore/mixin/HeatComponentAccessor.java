package com.yukimods.backtotfccore.mixin;

import net.dries007.tfc.common.component.heat.HeatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HeatComponent.class)
public interface HeatComponentAccessor {

    @Accessor("lastTemperature")
    float backtotfccore$getLastTemperature();

    @Invoker("with")
    HeatComponent backtotfccore$invokeWith(float temperature, long tick);
}
