package com.yukimods.backtotfccore.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 生命维持装置方块实体 — 存储营养信息，
 * 点击"恢复"按钮时读取并生成 foodNbt。
 */
public class LifeSupportBlockEntity extends BlockEntity {

    private final float[] targets = {0.4f, 0.4f, 0.4f, 0.4f, 0f};

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

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Targets")) {
            CompoundTag t = tag.getCompound("Targets");
            for (int i = 0; i < 5; i++) {
                targets[i] = t.getFloat("t" + i);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag t = new CompoundTag();
        for (int i = 0; i < 5; i++) {
            t.putFloat("t" + i, targets[i]);
        }
        tag.put("Targets", t);
    }
}
