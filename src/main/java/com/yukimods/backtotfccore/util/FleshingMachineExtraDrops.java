package com.yukimods.backtotfccore.util;

import com.hermitowo.advancedtfctech.common.recipes.FleshingMachineRecipe;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 存储 FleshingMachineRecipe 的 extra_drop 副产物映射。
 * 由 {@code FleshingMachineRecipeSerializerMixin} 在解码时填充，
 * 由 {@code FleshingMachineBlockEntityMixin} 在配方完成时读取。
 */
public final class FleshingMachineExtraDrops {

    private static final Map<FleshingMachineRecipe, ItemStack> MAP = new IdentityHashMap<>();

    private FleshingMachineExtraDrops() {}

    public static ItemStack get(FleshingMachineRecipe recipe) {
        return MAP.getOrDefault(recipe, ItemStack.EMPTY);
    }

    public static void put(FleshingMachineRecipe recipe, ItemStack stack) {
        if (!stack.isEmpty()) {
            MAP.put(recipe, stack.copy());
        }
    }
}
