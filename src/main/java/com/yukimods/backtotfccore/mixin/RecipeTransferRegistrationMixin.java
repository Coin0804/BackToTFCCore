package com.yukimods.backtotfccore.mixin;

import com.mojang.logging.LogUtils;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.library.load.registration.RecipeTransferRegistration;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RecipeTransferRegistration.class, remap = false)
public abstract class RecipeTransferRegistrationMixin {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(
        method = "addRecipeTransferHandler(Ljava/lang/Class;"
                + "Lnet/minecraft/world/inventory/MenuType;"
                + "Lmezz/jei/api/recipe/RecipeType;IIII)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void beforeAddHandler(Class<?> containerClass,
                                   MenuType<?> menuType,
                                   RecipeType<?> recipeType,
                                   int recipeSlotStart,
                                   int recipeSlotCount,
                                   int inventorySlotStart,
                                   int inventorySlotCount,
                                   CallbackInfo ci) {
        if (containerClass == CraftingMenu.class
                && recipeType == RecipeTypes.CRAFTING) {
            LOGGER.info(
                "BLOCKED vanilla crafting transfer handler registration. "
                    + "containerClass=CraftingMenu, recipeType=crafting. "
                    + "Our BackToTFCJeiPlugin will register the replacement.");
            ci.cancel();
        }
    }
}
