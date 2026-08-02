package com.yukimods.backtotfccore.block;

import com.yukimods.backtotfccore.BackToTFCCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.world.chunk.LoadingValidationCallback;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import org.slf4j.Logger;

/**
 * 生命维持装置方块实体 — 存储营养信息，
 * 点击"恢复"按钮时读取并生成 foodNbt。
 * 附带区块强加载功能（装置周边 5×5 区块），状态持久化于 NBT。
 */
public class LifeSupportBlockEntity extends BlockEntity {

    private static final Logger LOGGER = BackToTFCCore.LOGGER;
    private static final int LOAD_RADIUS = 2;

    /**
     * 区块强加载控制器（id: backtotfccore:life_support）。
     * 世界加载时会先回调 validateTickets —— 清空本控制器全部 ticket，
     * 之后由各装置的 {@link #onLoad()} 按持久化状态重新注册，防止残留。
     */
    public static final TicketController CONTROLLER = new TicketController(
            ResourceLocation.fromNamespaceAndPath(BackToTFCCore.MOD_ID, "life_support"),
            new LoadingValidationCallback() {
                @Override
                public void validateTickets(ServerLevel level, TicketHelper ticketHelper) {
                    for (BlockPos owner : ticketHelper.getBlockTickets().keySet()) {
                        ticketHelper.removeAllTickets(owner);
                    }
                    for (var owner : ticketHelper.getEntityTickets().keySet()) {
                        ticketHelper.removeAllTickets(owner);
                    }
                }
            }
    );

    private final float[] targets = {0.4f, 0.4f, 0.4f, 0.4f, 0f};
    private boolean chunkLoading = false;

    /**
     * 标记"本次移除来自区块卸载"（onChunkUnloaded → setRemoved 成对发生）。
     * 为 true 时 setRemoved 跳过释放 ticket —— 区块重载后由 onLoad 重建；
     * 为 false（方块被真正移除）时立即释放，防止区块永久加载。
     */
    private boolean unloaded = false;

    public LifeSupportBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.LIFE_SUPPORT_BE.get(), pos, state);
    }

    /** 获取 5 个目标值：[grain, fruit, veg, protein, dairy] */
    public float[] getTargets() {
        return targets;
    }

    /** 外部修改目标值（例如后续通过 UI 编辑） */
    public void setTarget(int index, float value) {
        if (index >= 0 && index < 5) {
            targets[index] = value;
            setChanged();
        }
    }

    /** 将 Targets 数据写入给定的 CompoundTag（供掉落物保存） */
    public void writeTargetsToTag(CompoundTag tag) {
        CompoundTag t = new CompoundTag();
        for (int i = 0; i < 5; i++) {
            t.putFloat("t" + i, targets[i]);
        }
        tag.put("Targets", t);
    }

    /** 写入完整掉落数据（Targets + ChunkLoading），供收起按钮使用 */
    public void writeFullDataToTag(CompoundTag tag) {
        writeTargetsToTag(tag);
        tag.putBoolean("ChunkLoading", chunkLoading);
    }

    /** 当前是否开启区块强加载 */
    public boolean isChunkLoading() {
        return chunkLoading;
    }

    /** GUI 按钮调用：切换强加载状态并立即生效 */
    public void toggleChunkLoading() {
        chunkLoading = !chunkLoading;
        if (chunkLoading) {
            activateChunks();
        } else {
            deactivateChunks();
        }
        setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel && chunkLoading) {
            activateChunks();
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        unloaded = true;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel && !unloaded && chunkLoading) {
            deactivateChunks();
        }
    }

    private void activateChunks() {
        setChunksForced(true);
    }

    private void deactivateChunks() {
        setChunksForced(false);
    }

    /**
     * 强制/释放装置周边 5×5 区块（自身区块 ±2），满 tick 加载。
     */
    private void setChunksForced(boolean force) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ChunkPos center = new ChunkPos(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
        int count = 0;
        for (int dx = -LOAD_RADIUS; dx <= LOAD_RADIUS; dx++) {
            for (int dz = -LOAD_RADIUS; dz <= LOAD_RADIUS; dz++) {
                CONTROLLER.forceChunk(serverLevel, worldPosition, center.x + dx, center.z + dz, force, true);
                count++;
            }
        }
        LOGGER.info("[LifeSupport] Chunk loading {} for {} chunks at {}",
            force ? "enabled" : "disabled", count, worldPosition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Targets")) {
            CompoundTag t = tag.getCompound("Targets");
            for (int i = 0; i < 5; i++) {
                targets[i] = t.getFloat("t" + i);
            }
        }
        chunkLoading = tag.getBoolean("ChunkLoading");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag t = new CompoundTag();
        for (int i = 0; i < 5; i++) {
            t.putFloat("t" + i, targets[i]);
        }
        tag.put("Targets", t);
        tag.putBoolean("ChunkLoading", chunkLoading);
    }
}
