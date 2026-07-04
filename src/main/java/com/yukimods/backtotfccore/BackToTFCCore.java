package com.yukimods.backtotfccore;

import com.yukimods.backtotfccore.network.SyncWorkbenchPosPacket;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(BackToTFCCore.MOD_ID)
public class BackToTFCCore {

    public static final String MOD_ID = "backtotfccore";
    public static final Logger LOGGER = LoggerFactory.getLogger("BackToTFC Core");

    public BackToTFCCore(IEventBus modEventBus) {
        LOGGER.info("BackToTFC Core initializing — workbench tier system (tag-driven, max 10 tiers)");

        modEventBus.addListener(this::onRegisterPayloadHandlers);
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
}
