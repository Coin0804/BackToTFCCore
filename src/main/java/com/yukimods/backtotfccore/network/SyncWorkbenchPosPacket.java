package com.yukimods.backtotfccore.network;

import com.yukimods.backtotfccore.BackToTFCCore;
import com.yukimods.backtotfccore.util.WorkbenchTierHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncWorkbenchPosPacket(int containerId, BlockPos pos)
        implements CustomPacketPayload {

    public static final Type<SyncWorkbenchPosPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    BackToTFCCore.MOD_ID, "sync_workbench_pos"));

    public static final StreamCodec<ByteBuf, SyncWorkbenchPosPacket> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.VAR_INT,
                SyncWorkbenchPosPacket::containerId,
                BlockPos.STREAM_CODEC,
                SyncWorkbenchPosPacket::pos,
                SyncWorkbenchPosPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendToPlayer(ServerPlayer player, int containerId, BlockPos pos) {
        PacketDistributor.sendToPlayer(player, new SyncWorkbenchPosPacket(containerId, pos));
    }

    public static void handle(SyncWorkbenchPosPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            int cid = packet.containerId();
            BlockPos p = packet.pos();

            if (BlockPos.ZERO.equals(p)) {
                BackToTFCCore.LOGGER.warn(
                    "Received ZERO BlockPos from server for containerId={}. "
                        + "JEI tier check will pass-through (no restriction).",
                    cid);
            }

            WorkbenchTierHelper.setClientPos(cid, p);
        }).exceptionally(e -> {
            BackToTFCCore.LOGGER.error(
                "Failed to handle SyncWorkbenchPosPacket for containerId={}: {}",
                packet.containerId(), e.getMessage(), e);
            return null;
        });
    }
}
