package com.indestructible13.better_auto_fishing.mods;

import com.indestructible13.better_auto_fishing.config.BetterAutoFishingConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BetterAutoFishingConfigScreen::new;
    }
}
