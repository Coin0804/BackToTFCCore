package com.yukimods.backtotfccore.event;

import com.yukimods.backtotfccore.BackToTFCCore;
import com.yukimods.backtotfccore.logic.WaterConvertManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.IdentityHashMap;

/**
 * 水浸配方触发 — 物品实体进入淡水累计停留时间，达到阈值后执行转换。
 * <p>
 * 用 {@link EntityTickEvent.Post}（零 Mixin，符合 Mixin 准则：事件优先）。
 * 性能设计（仿 AE2 的 ItemEntityMixin，但去掉字节码注入）：
 * <ol>
 *   <li>非物品实体直接返回</li>
 *   <li>可转换物品 Set 快速短路（每 tick 开销 = 一次 HashSet 查询）</li>
 *   <li>仅在目标流体中累计停留计时（60 tick = 3 秒），离开流体清零</li>
 * </ol>
 * 停留计时用 {@link IdentityHashMap} — 实体对象作 key，实体被回收时自动清理，无泄漏。
 */
@EventBusSubscriber(modid = BackToTFCCore.MOD_ID)
public class WaterConvertHandler {

    /** 实体 → 已停留 tick 数（IdentityHashMap：对象引用作 key，无 UUID 泄漏问题） */
    private static final IdentityHashMap<ItemEntity, Integer> STAY_TICKS = new IdentityHashMap<>();

    private WaterConvertHandler() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) {
            return;
        }
        if (!(itemEntity.level() instanceof ServerLevel level)) {
            return;
        }

        // 惰性缓存：/reload 后首次 tick 重建（null 检查开销可忽略）
        WaterConvertManager.ensureCache(level);
        // 快速短路：非可转换物品直接跳过
        if (!WaterConvertManager.canConvert(itemEntity.getItem().getItem())) {
            return;
        }

        // 实体包围盒中点位置的流体状态（仿 AE2 ItemEntityMixin）
        double midY = (itemEntity.getBoundingBox().minY + itemEntity.getBoundingBox().maxY) / 2.0;
        BlockPos pos = BlockPos.containing(itemEntity.getX(), midY, itemEntity.getZ());
        FluidState fluid = level.getFluidState(pos);

        if (!WaterConvertManager.isTriggerFluid(fluid)) {
            // 离开流体：清零停留计时
            STAY_TICKS.remove(itemEntity);
            return;
        }

        Integer prev = STAY_TICKS.get(itemEntity);
        int ticks = (prev == null ? 0 : prev) + 1;
        STAY_TICKS.put(itemEntity, ticks);
        if (ticks >= WaterConvertManager.STAY_TICKS) {
            if (WaterConvertManager.tryConvert(level, itemEntity)) {
                STAY_TICKS.remove(itemEntity);
            } else {
                // 转换失败：重置计时（防每 tick 重复尝试配方匹配）
                STAY_TICKS.put(itemEntity, 0);
            }
        }
    }
}
