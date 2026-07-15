package com.yukimods.backtotfccore.block;

import com.yukimods.backtotfccore.BackToTFCCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(BackToTFCCore.MOD_ID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BackToTFCCore.MOD_ID);

    public static final DeferredBlock<LifeSupportDeviceBlock> LIFE_SUPPORT_DEVICE =
            BLOCKS.register("life_support_device",
                    () -> new LifeSupportDeviceBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(-1.0f, 3600000.0f)
                                    .noLootTable()
                                    .sound(SoundType.METAL)
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LifeSupportBlockEntity>> LIFE_SUPPORT_BE =
            BLOCK_ENTITIES.register("life_support_device",
                    () -> BlockEntityType.Builder.of(
                            LifeSupportBlockEntity::new,
                            LIFE_SUPPORT_DEVICE.get()
                    ).build(null));
}
