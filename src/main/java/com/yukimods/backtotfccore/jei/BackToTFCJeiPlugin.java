package com.yukimods.backtotfccore.jei;

import com.yukimods.backtotfccore.BackToTFCCore;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class BackToTFCJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(BackToTFCCore.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration reg) {
        BackToTFCCore.LOGGER.info(
            "BackToTFC JEI Plugin: registering WorkbenchTransferInfo for (CraftingMenu, CRAFTING)");

        WorkbenchTransferInfo info = new WorkbenchTransferInfo(reg.getTransferHelper());
        reg.addRecipeTransferHandler(info);

        BackToTFCCore.LOGGER.info(
            "BackToTFC JEI Plugin: WorkbenchTransferInfo registered successfully.");
    }
}
