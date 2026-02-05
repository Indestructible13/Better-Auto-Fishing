package com.indestructible13.better_auto_fishing.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class BetterAutoFishingConfigScreen extends Screen {
    private final Screen parent;

    public BetterAutoFishingConfigScreen(Screen parent) {
        super(Text.literal("Better Auto Fishing Settings"));
        this.parent = parent;
    }

    @Override
    public void init() {
        int labelWidth = 200;
        int labelHeight = 20;
        int labelLeftX = 10;

        int buttonWidth = 50;
        int buttonHeight = 20;
        int buttonCenterX = 250;

        int row1Y = 10;

        TextWidget modActiveLabel = new TextWidget(labelLeftX, row1Y, labelWidth, labelHeight, Text.of("Mod Active:"), this.textRenderer);
        this.addDrawableChild(modActiveLabel);

        this.addDrawableChild(ButtonWidget.builder(
                Text.of(BetterAutoFishingConfigManager.getActive() ? "Yes" : "No"),
                button -> {
                    System.out.println("Config button pressed!");
                    BetterAutoFishingConfigManager.setActive(!BetterAutoFishingConfigManager.getActive());
                    button.setMessage(Text.of(BetterAutoFishingConfigManager.getActive() ? "Yes" : "No"));

                    //BetterAutoFishingConfigScreen.this.clearAndInit();
                }).dimensions(buttonCenterX, row1Y, buttonWidth, buttonHeight).build());
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
