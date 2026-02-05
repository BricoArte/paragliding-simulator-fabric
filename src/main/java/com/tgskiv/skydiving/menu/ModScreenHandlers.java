package com.tgskiv.skydiving.menu;

import com.tgskiv.ParaglidingSimulator;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class ModScreenHandlers {
    public static final ScreenHandlerType<VarioScreenHandler> VARIO =
            Registry.register(Registries.SCREEN_HANDLER,
                    Identifier.of(ParaglidingSimulator.MOD_ID, "vario"),
                    new ScreenHandlerType<>(VarioScreenHandler::new, FeatureSet.empty()));

    private ModScreenHandlers() {
    }

    public static void register() {
        ParaglidingSimulator.LOGGER.info("Registering ModScreenHandlers for " + ParaglidingSimulator.MOD_ID);
    }
}
