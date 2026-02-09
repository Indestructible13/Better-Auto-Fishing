package com.indestructible13.better_auto_fishing.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(EnvType.CLIENT)
public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Text.literal("Better Auto Fishing Settings"));
        this.parent = parent;
    }

    @Override
    public void init() {
        int labelWidth = 200;
        int labelHeight = 20;

        int widgetWidth = 50;
        int widgetHeight = 20;

        int labelX = 10;
        int widgetX = 250;

        int row1Y = 10;
        int row2Y = 30;
        int row3Y = 50;
        int errorRowY = row3Y + 50;

        // Error label to display error messages to the user
        TextWidget errorLabel = new TextWidget(labelX, errorRowY, labelWidth, labelHeight, Text.literal(""), this.textRenderer);
        this.addDrawableChild(errorLabel);

        // Row 1: Activate or deactivate mod
        TextWidget modActiveLabel = new TextWidget(labelX, row1Y, labelWidth, labelHeight, Text.of("Mod Active:"), this.textRenderer);
        this.addDrawableChild(modActiveLabel);

        Text yesText = Text.literal("Yes").formatted(Formatting.GREEN);
        Text noText = Text.literal("No").formatted(Formatting.RED);
        this.addDrawableChild(ButtonWidget.builder(
                ConfigManager.getActive() ? yesText : noText,
                button -> {
                    ConfigManager.setActive(!ConfigManager.getActive());
                    button.setMessage(ConfigManager.getActive() ? yesText : noText);
                    System.out.println(ConfigManager.getActive() ? "Better Auto Fishing activated" : "Better Auto Fishing deactivated");

                    //BetterAutoFishingConfigScreen.this.clearAndInit();
                }).dimensions(widgetX, row1Y, widgetWidth, widgetHeight).build());

        // Row 2: Set reel delay
        TextWidget reelDelayLabel = new TextWidget(labelX, row2Y, labelWidth, labelHeight, Text.of("Reel Delay:"), this.textRenderer);
        this.addDrawableChild(reelDelayLabel);

        TextFieldWidget reelDelayInputField = new TextFieldWidget(this.textRenderer, widgetX, row2Y, widgetWidth, widgetHeight, Text.of("reelDelayInputField"));
        reelDelayInputField.setText(Integer.toString(ConfigManager.getReelDelay())); // Fill in saved value from config
        reelDelayInputField.setChangedListener(text -> {
            if (text.matches("\\d*") && !text.isBlank()) {
                errorLabel.setMessage(Text.of(""));
                ConfigManager.setReelDelay(Integer.parseInt(text));
                System.out.println("Reel Delay updated: " + text);
            } else {
                errorLabel.setMessage(Text.of("Error: Reel delay must be a whole number!"));
            }
        });
        this.addDrawableChild(reelDelayInputField);
        this.addDrawableChild(new TextWidget(widgetX + widgetWidth + 5, row2Y, widgetWidth, labelHeight, Text.of("ticks"), this.textRenderer));

        // Row 3: Set cast delay
        TextWidget castDelayLabel = new TextWidget(labelX, row3Y, labelWidth, labelHeight, Text.of("Cast Delay:"), this.textRenderer);
        this.addDrawableChild(castDelayLabel);

        TextFieldWidget castDelayInputField = new TextFieldWidget(this.textRenderer, widgetX, row3Y, widgetWidth, widgetHeight, Text.of("castDelayInputField"));
        castDelayInputField.setText(Integer.toString(ConfigManager.getCastDelay())); // Fill in saved value from config
        castDelayInputField.setChangedListener(text -> {
            if (text.matches("\\d*") && !text.isBlank()) {
                errorLabel.setMessage(Text.of(""));
                ConfigManager.setCastDelay(Integer.parseInt(text));
                System.out.println("Cast Delay updated: " + text);
            } else {
                errorLabel.setMessage(Text.of("Error: Cast delay must be a whole number!"));
            }
        });
        this.addDrawableChild(castDelayInputField);
        this.addDrawableChild(new TextWidget(widgetX + widgetWidth + 5, row3Y, widgetWidth, labelHeight, Text.of("ticks"), this.textRenderer));
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
