package com.yukimods.backtotfccore.mixin;

import net.dries007.tfc.common.blockentities.ComposterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.dries007.tfc.util.tooltip.BlockEntityTooltips;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(BlockEntityTooltips.class)
public abstract class ComposterJadeTooltipMixin {

    @Inject(
        method = "lambda$static$14",
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 0
    )
    private static void onComposterTooltip(
        Level level, BlockState state, BlockPos pos,
        BlockEntity be, Consumer<Component> tooltip, CallbackInfo ci
    ) {
        if (!(be instanceof ComposterBlockEntity c)) return;

        if (c.isRotten()) {
            tooltip.accept(Component.translatable("tfc.composter.rotten"));
        } else if (!c.isReady()) {
            int green = c.getGreen();
            int brown = c.getBrown();
            if (green >= 4 && brown >= 4) {
                long remain = c.getReadyTicks() - c.getTicksSinceUpdate();
                if (remain > 0) {
                    tooltip.accept(Component.translatable("tfc.jade.time_left",
                        net.dries007.tfc.util.calendar.Calendars.get(level).getTimeDelta(remain)));
                } else {
                    tooltip.accept(Component.translatable("backtotfccore.composter.ready_soon"));
                }
            }
        }
        ci.cancel();
    }
}
