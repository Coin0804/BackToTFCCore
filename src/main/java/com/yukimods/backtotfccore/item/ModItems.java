package com.yukimods.backtotfccore.item;

import com.yukimods.backtotfccore.BackToTFCCore;
import com.yukimods.backtotfccore.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(BackToTFCCore.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BackToTFCCore.MOD_ID);

    /** 异常 — 无功能占位物品 */
    public static final DeferredItem<Item> ANOMALY =
        ITEMS.register("anomaly", () -> new Item(new Item.Properties()));

    /** 创造模式矿脉扫描仪 — 右键扫描矿脉并标记路径点（算法见 VeinScannerHelper） */
    public static final DeferredItem<VeinScannerItem> VEIN_SCANNER =
        ITEMS.register("veinscanner", () -> new VeinScannerItem(new Item.Properties().stacksTo(1)));

    /** 生命维持装置（方块物品） */
    public static final DeferredItem<BlockItem> LIFE_SUPPORT_DEVICE =
        ITEMS.register("life_support_device", () -> new BlockItem(ModBlocks.LIFE_SUPPORT_DEVICE.get(), new Item.Properties().fireResistant()) {
            @Override
            public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
                tooltipComponents.add(Component.translatable("block." + BackToTFCCore.MOD_ID + ".life_support_device.tooltip"));
                super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
            }

            /** 掉落物永不消失（5 分钟 → 无限） */
            @Override
            public int getEntityLifespan(ItemStack stack, net.minecraft.world.level.Level level) {
                return Integer.MAX_VALUE;
            }
        });

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
        CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(LIFE_SUPPORT_DEVICE.get()))
            .title(Component.translatable("itemGroup." + BackToTFCCore.MOD_ID))
            .displayItems((params, output) -> {
                output.accept(LIFE_SUPPORT_DEVICE.get());
                output.accept(VEIN_SCANNER.get());
                output.accept(ANOMALY.get());
            })
            .build());
}
