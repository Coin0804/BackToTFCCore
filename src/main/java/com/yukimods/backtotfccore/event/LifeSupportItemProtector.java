package com.yukimods.backtotfccore.event;

import com.yukimods.backtotfccore.BackToTFCCore;
import com.yukimods.backtotfccore.block.ModBlocks;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * 生命维持装置掉落物保护 — 防止火焰和爆炸摧毁。
 * <p>
 * 火焰防护：BlockItem 已设置 {@code .fireResistant()}（原版机制）。<br>
 * 爆炸防护：ItemEntity 生成时设为 invulnerable，免疫一切伤害（拾取不受影响）。
 */
@EventBusSubscriber(modid = BackToTFCCore.MOD_ID)
public class LifeSupportItemProtector {

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            if (itemEntity.getItem().is(ModBlocks.LIFE_SUPPORT_DEVICE.get().asItem())) {
                itemEntity.setInvulnerable(true);
            }
        }
    }
}
