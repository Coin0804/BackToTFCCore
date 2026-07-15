package com.yukimods.backtotfccore.mixin;

import com.hermitowo.advancedtfctech.common.recipes.FleshingMachineRecipe;
import com.yukimods.backtotfccore.util.FleshingMachineExtraDrops;
import malte0811.dualcodecs.DualCompositeMapCodecs;
import malte0811.dualcodecs.DualMapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 为 FleshingMachineRecipe 的 codec 增加可选的 {@code extra_drop} 字段。
 */
@Mixin(value = FleshingMachineRecipe.Serializer.class, remap = false)
public abstract class FleshingMachineRecipeSerializerMixin {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Inject(method = "codecs", at = @At("RETURN"), cancellable = true)
    private void backtotfccore$addExtraDropCodec(
            CallbackInfoReturnable<DualMapCodec<RegistryFriendlyByteBuf, FleshingMachineRecipe>> cir) {

        DualMapCodec<RegistryFriendlyByteBuf, FleshingMachineRecipe> original = cir.getReturnValue();

        DualMapCodec<RegistryFriendlyByteBuf, ItemStack> extraCodec =
                (DualMapCodec) malte0811.dualcodecs.DualCodecs.ITEM_STACK
                        .optionalFieldOf("extra_drop", ItemStack.EMPTY);

        Function<FleshingMachineRecipe, ItemStack> extraGetter =
                FleshingMachineExtraDrops::get;

        BiFunction<FleshingMachineRecipe, ItemStack, FleshingMachineRecipe> combiner = (recipe, extra) -> {
            FleshingMachineExtraDrops.put(recipe, extra);
            return recipe;
        };

        cir.setReturnValue(DualCompositeMapCodecs.composite(
                original, (Function) Function.identity(),
                extraCodec, extraGetter,
                combiner
        ));
    }
}
