package com.yukimods.backtotfccore.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 工作台等级工具 — 全标签驱动，零配置文件。
 *
 * 【工作台等级】KubeJS block tag：
 *   未标记 → 0（不限制）
 *   backtotfccore:workbench_tier_1 → minecraft:crafting_table
 *
 * 【配方等级】KubeJS item tag（打在配方产出物上）：
 *   未标记 → 0（全部工作台可用）
 *   backtotfccore:recipe_tier_1 → 仅有 tier >= 1 的工作台可用
 *   backtotfccore:recipe_tier_2 → 仅有 tier >= 2 的工作台可用
 */
public final class WorkbenchTierHelper {

    private static final String MOD_NS = "backtotfccore";
    private static final int MAX_TIER = 10;

    private WorkbenchTierHelper() {}

    /** 从 block tag 提取工作台等级。未标记 → 0。 */
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

    /** 从产物 item tag 提取配方等级。未标记 → 0。 */
    public static int getResultItemTier(ItemStack result) {
        if (result.isEmpty()) return 0;
        Holder<Item> holder = result.getItemHolder();
        for (int t = 1; t <= MAX_TIER; t++) {
            TagKey<Item> tag = ItemTags.create(
                    ResourceLocation.fromNamespaceAndPath(MOD_NS, "recipe_tier_" + t));
            if (holder.is(tag)) {
                return t;
            }
        }
        return 0;
    }
}
