package com.yukimods.backtotfccore;

import com.mojang.brigadier.Command;
import com.yukimods.backtotfccore.block.LifeSupportBlockEntity;
import com.yukimods.backtotfccore.block.ModBlocks;
import com.yukimods.backtotfccore.item.ModItems;
import com.yukimods.backtotfccore.logic.WaterConvertManager;
import com.yukimods.backtotfccore.network.SyncWorkbenchPosPacket;
import com.yukimods.backtotfccore.util.PlayerFullResetHelper;
import net.minecraft.commands.Commands;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(BackToTFCCore.MOD_ID)
public class BackToTFCCore {

    public static final String MOD_ID = "backtotfccore";
    public static final Logger LOGGER = LoggerFactory.getLogger("BackToTFC Core");

    public BackToTFCCore(IEventBus modEventBus) {
        LOGGER.info("BackToTFC Core initializing — workbench tier system (tag-driven, max 10 tiers)");

        // 注册方块 + 方块实体
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.BLOCK_ENTITIES.register(modEventBus);
        // 注册物品 & 创造模式标签页
        ModItems.ITEMS.register(modEventBus);
        ModItems.CREATIVE_TABS.register(modEventBus);

        modEventBus.addListener(this::onRegisterPayloadHandlers);
        modEventBus.addListener(RegisterTicketControllersEvent.class, this::onRegisterTicketControllers);
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(AddReloadListenerEvent.class, this::onAddReloadListeners);
    }

    /**
     * /reload 时清空水浸配方缓存（下次 tick 惰性重建，TFC 配方变更立即生效）。
     */
    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
                WaterConvertManager.invalidate();
            }
        });
    }

    /**
     * 注册生命维持装置的区块强加载控制器。
     * 控制器未注册时，其名下所有 ticket 会在世界加载时被丢弃。
     */
    private void onRegisterTicketControllers(RegisterTicketControllersEvent event) {
        event.register(LifeSupportBlockEntity.CONTROLLER);
        LOGGER.info("Registered LifeSupport chunk-loading ticket controller");
    }

    private void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        event.registrar(MOD_ID)
            .versioned("1.0")
            .playToClient(
                SyncWorkbenchPosPacket.TYPE,
                SyncWorkbenchPosPacket.STREAM_CODEC,
                SyncWorkbenchPosPacket::handle
            );
        LOGGER.info("Registered SyncWorkbenchPosPacket payload handler");
    }

    // ===== /backtotfccore debug_set_one — 调试用 =====

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("backtotfccore")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("debug_set_one")
                    .executes(ctx -> {
                        var player = ctx.getSource().getPlayerOrException();
                        PlayerFullResetHelper.debugSetOne(player);
                        LOGGER.info(
                            "Debug: set health/thirst/hunger to 1 for player {}",
                            player.getName().getString());
                        return Command.SINGLE_SUCCESS;
                    }))
        );
        LOGGER.info("[Server] Registered /backtotfccore debug_set_one command");
    }
}
