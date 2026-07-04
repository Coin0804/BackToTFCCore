package com.yukimods.backtotfccore.util;

import com.mojang.logging.LogUtils;
import com.yukimods.backtotfccore.BackToTFCCore;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class WorkbenchTierHelper {

    private static final Logger LOGGER = LogUtils.getLogger();
    static final String MOD_NS = BackToTFCCore.MOD_ID;
    static final int MAX_TIER = 10;

    // ===== 服务端侧 =====

    private static final Map<CraftingMenu, BlockPos> WORKBENCH_POSITIONS =
            new WeakHashMap<>();

    public static void setWorkbenchPos(CraftingMenu menu, BlockPos pos) {
        WORKBENCH_POSITIONS.put(menu, pos);
    }

    public static BlockPos getWorkbenchPos(CraftingMenu menu) {
        return WORKBENCH_POSITIONS.getOrDefault(menu, BlockPos.ZERO);
    }

    // ===== 客户端侧 =====

    private static final Map<Integer, BlockPos> CLIENT_POSITIONS = new HashMap<>();

    public static void setClientPos(int containerId, BlockPos pos) {
        CLIENT_POSITIONS.put(containerId, pos);
    }

    public static BlockPos getClientPos(int containerId) {
        return CLIENT_POSITIONS.getOrDefault(containerId, BlockPos.ZERO);
    }

    public static int getClientBlockTier(BlockPos pos) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            LOGGER.error("Client: Minecraft.getInstance().level is NULL! "
                + "Cannot query block tier for pos {}, {}, {}.",
                pos.getX(), pos.getY(), pos.getZ());
            return 0;
        }

        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            LOGGER.warn("Client: block at {}, {}, {} is AIR — was the workbench broken?",
                pos.getX(), pos.getY(), pos.getZ());
            return 0;
        }

        for (int t = 1; t <= MAX_TIER; t++) {
            TagKey<Block> tag = BlockTags.create(
                ResourceLocation.fromNamespaceAndPath(MOD_NS, "workbench_tier_" + t));
            if (state.is(tag)) {
                return t;
            }
        }

        LOGGER.debug("Client: block at {}, {}, {} is {} but has no workbench_tier_N tag.",
            pos.getX(), pos.getY(), pos.getZ(),
            BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        return 0;
    }

    // ===== 通用 =====

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

    // ===== RegistryAccess =====

    private static RegistryAccess cachedRegistryAccess;

    public static RegistryAccess getRegistryAccess() {
        if (cachedRegistryAccess != null) {
            return cachedRegistryAccess;
        }
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            cachedRegistryAccess = level.registryAccess();
            return cachedRegistryAccess;
        }
        LOGGER.error("Cannot get RegistryAccess: client level is null!");
        return RegistryAccess.EMPTY;
    }

    private WorkbenchTierHelper() {}
}
