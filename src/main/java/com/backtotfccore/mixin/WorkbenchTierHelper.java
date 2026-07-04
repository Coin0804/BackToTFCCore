package com.backtotfccore.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 工作台等级工具 — 全标签驱动，零配置文件。
 *
 * ===== 用法 =====
 *
 * 【配方等级】 KubeJS ServerEvents.tags('recipe')：
 *   未标记 → 0（全部工作台可用）
 *   backtotfccore:recipe_tier_1 → 仅有 tier >= 1 的工作台可用
 *   backtotfccore:recipe_tier_2 → 仅有 tier >= 2 的工作台可用
 *   backtotfccore:recipe_tier_3 → 仅有 tier >= 3 的工作台可用
 *
 * 【工作台等级】 KubeJS 给方块打 block tag：
 *   未标记 → 0（不限制，全部配方可用）
 *   backtotfccore:workbench_tier_1 → minecraft:crafting_table
 *   backtotfccore:workbench_tier_2 → 升级工作台
 *
 * 【无序配方】永远 tier 0，无条件可用。
 */
public final class WorkbenchTierHelper {

    private static final String MOD_NS = "backtotfccore";
    private static final int MAX_TIER = 10;

    private WorkbenchTierHelper() {}

    /**
     * 从 block tag 提取工作台等级。未标记 → 0。
     */
    public static int getBlockTier(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        for (int t = 1; t <= MAX_TIER; t++) {
            TagKey<Block> tag = BlockTags.create(
                    ResourceLocation.fromNamespaceAndPath(MOD_NS, "workbench_tier_" + t));
            if (state.is(tag)) {
                return t;
            }
        }
        return 0;
    }

    /**
     * 从 recipe tag 提取配方等级。
     * 无序配方 → 0。
     * 未标记 → 0（默认可用）。
     */
    public static int getRecipeTier(RecipeHolder<CraftingRecipe> recipe) {
        if (recipe.value() instanceof ShapelessRecipe) {
            return 0;
        }
        for (int t = 1; t <= MAX_TIER; t++) {
            if (recipe.is(ResourceLocation.fromNamespaceAndPath(MOD_NS, "recipe_tier_" + t))) {
                return t;
            }
        }
        return 0;
    }
}
