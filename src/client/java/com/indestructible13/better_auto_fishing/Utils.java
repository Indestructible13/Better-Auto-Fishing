package com.indestructible13.better_auto_fishing;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class Utils {
    public static void sendChatMessage(PlayerEntity player, String message) {
        player.sendMessage(Text.of(message), false);
    }

    public static void sendDebugChatMessage(PlayerEntity player, String message) {
        player.sendMessage(Text.of("[Debug]: " + message), false);
    }

    public static void sendActionBarMessage(MinecraftClient client, Text message) {
        client.inGameHud.setOverlayMessage(message, false);
    }
}
