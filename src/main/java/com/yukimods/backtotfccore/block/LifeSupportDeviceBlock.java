package com.yukimods.backtotfccore.block;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType.BlockUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.yukimods.backtotfccore.client.LifeSupportBlockUI;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class LifeSupportDeviceBlock extends BaseEntityBlock implements BlockUIMenuType.BlockUI {

    public LifeSupportDeviceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(LifeSupportDeviceBlock::new);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LifeSupportBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        // 由 LDLib2 的 BlockUIMenuType 接管 GUI 创建
        BlockUIMenuType.openUI((ServerPlayer) player, pos);
        return InteractionResult.CONSUME;
    }

    /** LDLib2 回调 — 创建 ModularUI */
    @Override
    public ModularUI createUI(BlockUIHolder holder) {
        return LifeSupportBlockUI.createUI(holder);
    }

    /** 检查方块是否仍在原处（防止玩家走远后操作） */
    @Override
    public boolean stillValid(BlockUIHolder holder) {
        return holder.player.level().getBlockState(holder.pos)
            .is(ModBlocks.LIFE_SUPPORT_DEVICE.get());
    }

    @Override
    public Component getUIDisplayName(BlockUIHolder holder) {
        return Component.translatable("block.backtotfccore.life_support_device");
    }
}
