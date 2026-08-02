package com.yukimods.backtotfccore.item;

import com.yukimods.backtotfccore.BackToTFCCore;
import com.yukimods.backtotfccore.util.VeinScannerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 创造模式矿脉扫描仪 — 右键扫描周围 128 格内的矿脉并生成 Journeymap 路径点按钮。
 * 算法见 {@link VeinScannerHelper}（由 KubeJS 版 veinscanner.js 迁移）。
 */
public class VeinScannerItem extends Item {

    public VeinScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            player.sendSystemMessage(Component.literal("已执行扫描，请查看聊天栏的指令反馈。"));
            VeinScannerHelper.startScan(serverLevel, serverPlayer);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item." + BackToTFCCore.MOD_ID + ".veinscanner.tooltip.1"));
        tooltipComponents.add(Component.translatable("item." + BackToTFCCore.MOD_ID + ".veinscanner.tooltip.2"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
