package com.yukimods.backtotfccore.mixin;

import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.gui.bookmarks.BookmarkFactory;
import mezz.jei.library.ingredients.TypedIngredient;
import net.dries007.tfc.common.component.TFCComponents;
import net.dries007.tfc.common.component.heat.HeatComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = BookmarkFactory.class, remap = false)
public abstract class JeiBookmarkFactoryMixin {

    @ModifyArg(method = "create",
            at = @At(value = "INVOKE",
                     target = "Lmezz/jei/gui/bookmarks/IngredientBookmark;<init>(Lmezz/jei/api/ingredients/ITypedIngredient;Ljava/lang/Object;)V"),
            index = 0,
            remap = false)
    private ITypedIngredient<?> freezeBookmarkIngredient(ITypedIngredient<?> ingredient) {
        Object raw = ingredient.getIngredient();
        if (!(raw instanceof ItemStack stack)) return ingredient;

        HeatComponent heat = stack.get(TFCComponents.HEAT);
        if (heat == null) return ingredient;

        // copy 独立副本，invokeWith 创建全新 HeatComponent，彻底切断引用
        ItemStack copy = stack.copy();
        HeatComponent heatCopy = copy.get(TFCComponents.HEAT);
        HeatComponentAccessor accessor = (HeatComponentAccessor) (Object) heatCopy;
        float temp = accessor.backtotfccore$getLastTemperature();
        copy.set(TFCComponents.HEAT, accessor.backtotfccore$invokeWith(temp, -2L));

        @SuppressWarnings({"unchecked", "rawtypes"})
        var frozen = TypedIngredient.createUnvalidated(
            (mezz.jei.api.ingredients.IIngredientType) ingredient.getType(), copy);
        return frozen;
    }
}
