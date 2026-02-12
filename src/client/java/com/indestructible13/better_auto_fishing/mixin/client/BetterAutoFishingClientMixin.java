package com.indestructible13.better_auto_fishing.mixin.client;

import com.indestructible13.better_auto_fishing.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class BetterAutoFishingClientMixin {
    @Unique
    private static final String MOD_ID = "better_auto_fishing";
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Inject(at = @At("HEAD"), method = "run")
    private void init(CallbackInfo info) {
        // This code is injected into the start of MinecraftClient.run()V
        //LOGGER.info("Successfully injected BetterAutoFishingClientMixin!");
    }
}