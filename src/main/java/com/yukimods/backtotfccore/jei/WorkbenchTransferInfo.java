package com.yukimods.backtotfccore.jei;

import com.yukimods.backtotfccore.util.WorkbenchTierHelper;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WorkbenchTransferInfo
        implements IRecipeTransferInfo<CraftingMenu, RecipeHolder<CraftingRecipe>> {

    private final IRecipeTransferHandlerHelper helper;

    public WorkbenchTransferInfo(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
    }

    @Override
    public Class<? extends CraftingMenu> getContainerClass() {
        return CraftingMenu.class;
    }

    @Override
    public Optional<MenuType<CraftingMenu>> getMenuType() {
        return Optional.of(MenuType.CRAFTING);
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public boolean canHandle(CraftingMenu menu, RecipeHolder<CraftingRecipe> recipe) {
        int containerId = menu.containerId;

        BlockPos pos = WorkbenchTierHelper.getClientPos(containerId);
        if (BlockPos.ZERO.equals(pos)) {
            return true; // 未知位置 → 允许
        }

        int blockTier = WorkbenchTierHelper.getClientBlockTier(pos);
        if (blockTier == 0) {
            return true; // 无 tier tag → 无限制
        }

        ItemStack result = recipe.value()
            .getResultItem(WorkbenchTierHelper.getRegistryAccess());
        int recipeTier = WorkbenchTierHelper.getResultItemTier(result);

        return recipeTier <= blockTier;
    }

    @Override
    public IRecipeTransferError getHandlingError(CraftingMenu menu,
                                                   RecipeHolder<CraftingRecipe> recipe) {
        int recipeTier = WorkbenchTierHelper.getResultItemTier(
            recipe.value().getResultItem(WorkbenchTierHelper.getRegistryAccess()));

        return helper.createUserErrorWithTooltip(
            Component.translatable(
                "backtotfccore.jei.transfer.error.workbench_tier_too_low", recipeTier));
    }

    @Override
    public List<Slot> getRecipeSlots(CraftingMenu menu,
                                      RecipeHolder<CraftingRecipe> recipe) {
        List<Slot> slots = new ArrayList<>(9);
        for (int i = 1; i <= 9; i++) {
            slots.add(menu.getSlot(i));
        }
        return slots;
    }

    @Override
    public List<Slot> getInventorySlots(CraftingMenu menu,
                                         RecipeHolder<CraftingRecipe> recipe) {
        List<Slot> slots = new ArrayList<>(36);
        for (int i = 10; i <= 45; i++) {
            slots.add(menu.getSlot(i));
        }
        return slots;
    }
}
