package com.yukimods.backtotfccore.logic;

import com.yukimods.backtotfccore.BackToTFCCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import org.slf4j.Logger;

import net.dries007.tfc.common.recipes.InstantBarrelRecipe;
import net.dries007.tfc.common.recipes.TFCRecipeTypes;
import net.dries007.tfc.common.recipes.ingredients.HeatIngredient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * 水浸配方 — 物品投水自动转换（由 KubeJS barrel_water.js 迁移，2026-08-02）。
 * <p>
 * 数据源：TFC 的 {@code barrel_instant} 配方，运行时遍历（TFC 新水配方自动纳入，无需维护 JSON）。
 * 筛选条件（与 KJS 版一致）：
 * <ol>
 *   <li>输入流体匹配水（KJS 版字面比较 "minecraft:water"，此处用 test 等价判断，含水的 tag 配方也会纳入）</li>
 *   <li>无输出液体（纯物品转换）</li>
 *   <li>输入不是 TFC heat ingredient（排除降温配方，KJS 版检查 {@code type != "tfc:heat"}）</li>
 * </ol>
 * 输出：TFC {@code ItemStackProvider} — 支持 modifiers（copy_input / empty_bowl 等），
 * 实现 KJS 版无法表达的真正 modify 语义（如食物投水变"输入里的碗"而非硬编码碗）。
 * <p>
 * 触发见 {@link com.yukimods.backtotfccore.event.WaterConvertHandler}（EntityTickEvent，零 Mixin）。
 * 性能：可转换物品 Set 快速短路，每 tick 开销 = 一次 HashSet 查询（仿 AE2 TransformLogic 模式）。
 */
public final class WaterConvertManager {

    private static final Logger LOGGER = BackToTFCCore.LOGGER;

    /** 停留 tick 数（=3 秒）— 物品需在水里持续停留才能转换，防误转换（仿 AE2 的 60 tick） */
    public static final int STAY_TICKS = 60;

    /** 淡水触发 tag — 与 KJS 版 AE2 transformFluid 配方的 fluid 条件一致 */
    private static final TagKey<Fluid> FRESH_WATER = TagKey.create(Registries.FLUID, ResourceLocation.parse("tfc:any_fresh_water"));

    /** 可转换物品快速短路集合 */
    private static Set<Item> convertableItems = Set.of();
    /** 物品 → 候选配方缓存（null = 未初始化，惰性重建） */
    @Nullable
    private static Map<Item, List<RecipeHolder<InstantBarrelRecipe>>> recipeCache = null;

    private WaterConvertManager() {
    }

    /** /reload 时清空缓存（下次查询惰性重建） */
    public static void invalidate() {
        recipeCache = null;
    }

    /** 确保缓存已构建（仅服务端调用，reload 后首次 tick 触发重建） */
    public static void ensureCache(ServerLevel level) {
        if (recipeCache == null) {
            rebuildCache(level);
        }
    }

    /** 实体所在流体是否满足触发条件（淡水） */
    public static boolean isTriggerFluid(FluidState state) {
        return !state.isEmpty() && state.is(FRESH_WATER);
    }

    /** 物品是否可能转换（O(1) 快速短路，每 tick 调用） */
    public static boolean canConvert(Item item) {
        return convertableItems.contains(item);
    }

    /**
     * 尝试转换实体物品。匹配配方后消耗 1 个输入，输出由 TFC ItemStackProvider 生成
     * （modifiers 原生生效），生成新实体。返回是否成功（成功由调用方重置停留计时）。
     */
    public static boolean tryConvert(ServerLevel level, ItemEntity entity) {
        ensureCache(level);
        ItemStack stack = entity.getItem();
        if (stack.isEmpty()) {
            return false;
        }
        List<RecipeHolder<InstantBarrelRecipe>> candidates = recipeCache.get(stack.getItem());
        if (candidates == null) {
            return false;
        }

        for (RecipeHolder<InstantBarrelRecipe> holder : candidates) {
            InstantBarrelRecipe recipe = holder.value();
            if (!recipe.getInputItem().test(stack)) {
                continue;
            }

            // 匹配：消耗 1 个输入
            ItemStack input = stack.copyWithCount(1);
            stack.split(1);
            if (stack.isEmpty()) {
                entity.discard();
            }

            ItemStack output = recipe.getOutputItem().getStack(input);
            if (!output.isEmpty()) {
                spawnOutput(level, entity, output);
            }
            return true;
        }
        return false;
    }

    // ===== 缓存构建 =====

    private static void rebuildCache(ServerLevel level) {
        Map<Item, List<RecipeHolder<InstantBarrelRecipe>>> newCache = new HashMap<>();
        for (RecipeHolder<InstantBarrelRecipe> holder : level.getRecipeManager().getAllRecipesFor(TFCRecipeTypes.BARREL_INSTANT.get())) {
            InstantBarrelRecipe recipe = holder.value();
            // 筛选 1：输入流体匹配水。
            // SizedFluidIngredient.test 会检查 amount（大桶装够量才触发），KJS 版只比较 fluid id；
            // 用最大量测试等价于只验证流体类型，与 KJS 版行为一致。
            if (!recipe.getInputFluid().test(new FluidStack(Fluids.WATER, Integer.MAX_VALUE))) {
                continue;
            }
            // 筛选 2：无输出液体（纯物品转换）
            if (!recipe.getOutputFluid().isEmpty()) {
                continue;
            }
            SizedIngredient inputItem = recipe.getInputItem();
            // 无输入物品的配方无法触发投水转换
            if (inputItem.ingredient() == Ingredient.EMPTY) {
                continue;
            }
            // 筛选 3：排除降温配方（TFC heat ingredient）
            if (inputItem.ingredient().getCustomIngredient() instanceof HeatIngredient) {
                continue;
            }
            // 枚举输入物品加入缓存
            for (ItemStack item : inputItem.ingredient().getItems()) {
                List<RecipeHolder<InstantBarrelRecipe>> list = newCache.get(item.getItem());
                if (list == null) {
                    list = new ArrayList<>();
                    newCache.put(item.getItem(), list);
                }
                list.add(holder);
            }
        }
        recipeCache = newCache;
        convertableItems = newCache.keySet();
        LOGGER.debug("水浸配方缓存重建: {} 种可转换物品", convertableItems.size());
    }

    /** 生成输出实体 — 原地小偏移 + 随机初速度（仿 AE2 TransformLogic） */
    private static void spawnOutput(ServerLevel level, ItemEntity source, ItemStack output) {
        double x = Math.floor(source.getX()) + .25 + level.random.nextDouble() * .5;
        double y = Math.floor(source.getY()) + .25 + level.random.nextDouble() * .5;
        double z = Math.floor(source.getZ()) + .25 + level.random.nextDouble() * .5;
        ItemEntity newEntity = new ItemEntity(level, x, y, z, output);
        newEntity.setDeltaMovement(
            level.random.nextDouble() * .25 - .125,
            level.random.nextDouble() * .25 - .125,
            level.random.nextDouble() * .25 - .125);
        level.addFreshEntity(newEntity);
    }
}
