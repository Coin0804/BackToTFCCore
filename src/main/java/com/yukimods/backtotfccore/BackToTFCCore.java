package com.yukimods.backtotfccore;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(BackToTFCCore.MOD_ID)
public class BackToTFCCore {

    public static final String MOD_ID = "backtotfccore";
    public static final Logger LOGGER = LoggerFactory.getLogger("BackToTFC Core");

    public BackToTFCCore(IEventBus modEventBus) {
        LOGGER.info("BackToTFC Core initialized.");
    }
}
