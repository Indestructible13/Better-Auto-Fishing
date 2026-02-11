package com.indestructible13.better_auto_fishing;

import com.indestructible13.better_auto_fishing.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import com.indestructible13.better_auto_fishing.mixin.client.FishingBobberEntityAccessor;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class BetterAutoFishingClient implements ClientModInitializer {
    public static final String MOD_ID = "better_auto_fishing";
    public static ModConfig config;

    private enum AutoFishState {
        IDLE,
        REELING,
        WAITING_FOR_CLEAR,
        CASTING
    }

    private AutoFishState currentState = AutoFishState.IDLE;
    private int tickCounter = 0;
    private int delayValue = 0;
    private static final Random RANDOM = new Random();
    private KeyBinding toggleActiveKey;

    private MinecraftClient client;
    private PlayerEntity player;

    @Override
    public void onInitializeClient() {
        System.out.println("Better Auto Fishing mod initializing...");

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);

        // Initialize config
        config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

        // Custom key binding
        KeyBinding.Category CATEGORY = new KeyBinding.Category(
                Identifier.of(MOD_ID, "custom_category")
        );

        toggleActiveKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.better_auto_fishing.toggle_active", // The translation key for the key mapping.
                        InputUtil.Type.KEYSYM, // The type of the keybinding; KEYSYM for keyboard, MOUSE for mouse.
                        GLFW.GLFW_KEY_B, // The GLFW keycode of the key.
                        CATEGORY // The category of the mapping.
                ));
    }

    private void onTick(MinecraftClient client) {
        this.client = client;
        this.player = client.player;

        if (player == null) return;

        // Toggle the mod active state when the toggle keybind is pressed
        while (toggleActiveKey.wasPressed()) {
            config.active = !config.active;
            AutoConfig.getConfigHolder(ModConfig.class).save();

            Text activatedMessage = Text.literal("Better Auto Fishing ").append(Text.literal("activated").formatted(Formatting.GREEN));
            Text deactivatedMessage = Text.literal("Better Auto Fishing ").append(Text.literal("deactivated").formatted(Formatting.RED));
            Utils.sendActionBarMessage(client, config.active ? activatedMessage : deactivatedMessage);
        }

        if (!config.active) { // If the mod is inactive, do nothing
            resetState();
            return;
        }

        FishingBobberEntity bobber = player.fishHook;

        switch (currentState) {
            case IDLE:
                // While the bobber is null in the IDLE state, do nothing
                // If the bobber appears while in IDLE, it means the player cast the rod
                if (bobber != null) {
                    // Use the Accessor Mixin to check the private caughtFish boolean
                    boolean caughtFish = ((FishingBobberEntityAccessor) bobber).getCaughtFish();
                    if (caughtFish) {
                        currentState = AutoFishState.REELING;
                        tickCounter = 0;
                        //Utils.sendDebugChatMessage(player, "Caught fish");
                        //Utils.sendDebugChatMessage(player, "Current state: " + currentState);
                        setReelDelay(); // When a fish is on the line, decide on the reel delay
                    }
                }
                break;

            case REELING:
                // If bobber is lost (e.g. manual reel in by the player), reset
                if (bobber == null) {
                    resetState();
                    //Utils.sendDebugChatMessage(player, "Player manually reeled in");
                    //Utils.sendDebugChatMessage(player, "Current state: " + currentState);
                    return;
                }

                tickCounter++; // Count up to the reel delay, then reel in
                //Utils.sendDebugChatMessage(player, "Reel counter: " + tickCounter);
                if (tickCounter >= delayValue) {
                    reelIn();
                    //Utils.sendDebugChatMessage(player, "Auto reeled in");
                    currentState = AutoFishState.WAITING_FOR_CLEAR;
                    //Utils.sendDebugChatMessage(player, "Current state: " + currentState);
                    tickCounter = 0;
                    setCastDelay(); // After the reel in happens, decide how long the cast delay will be
                }
                break;

            case WAITING_FOR_CLEAR:
                // Wait for the old bobber entity to be removed
                // Prevents issues where the casting state would get reset because
                // the old bobber was still in the water, but the mod thought
                // the player had manually cast
                if (bobber == null) {
                    currentState = AutoFishState.CASTING;
                    tickCounter = 0;
                    //Utils.sendDebugChatMessage(player, "Bobber cleared");
                    //Utils.sendDebugChatMessage(player, "Current state: " + currentState);
                }
                break;

            case CASTING:
                // If bobber appears (manual cast by player), reset
                if (bobber != null) {
                    resetState();
                    //Utils.sendDebugChatMessage(player, "Player manually cast");
                    //Utils.sendDebugChatMessage(player, "Current state: " + currentState);
                    return;
                }

                tickCounter++; // Count up to the cast delay, then cast
                //Utils.sendDebugChatMessage(player, "Cast counter: " + tickCounter);
                if (tickCounter >= delayValue) {
                    castRod();
                    //Utils.sendDebugChatMessage(player, "Auto cast");
                    resetState();
                    //Utils.sendDebugChatMessage(player, "Current state: " + currentState);
                }
                break;
        }
    }

    private void resetState() {
        currentState = AutoFishState.IDLE;
        tickCounter = 0;
    }

    private void reelIn() {
        if (player != null && client.interactionManager != null) {
            player.swingHand(Hand.MAIN_HAND);
            client.interactionManager.interactItem(player, Hand.MAIN_HAND);
            player.playSound(SoundEvents.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0F, 1.0F);
        }
    }

    private void castRod() {
        if (player != null && client.interactionManager != null) {
            player.swingHand(Hand.MAIN_HAND);
            client.interactionManager.interactItem(player, Hand.MAIN_HAND);
        }
    }

    private void setReelDelay() {
        if (config.randomizeDelays != null && config.randomizeDelays.enableRandomReelDelay) {
            if (config.randomizeDelays.reelDelayMax < config.randomizeDelays.reelDelayMin) {
                disableModWithError("Max Reel Delay cannot be less than Min Reel Delay!");
                return;
            }
            delayValue = config.randomizeDelays.reelDelayMin + RANDOM.nextInt(config.randomizeDelays.reelDelayMax - config.randomizeDelays.reelDelayMin + 1);
        } else {
            delayValue = config.reelDelay;
        }
    }

    private void setCastDelay() {
        if (config.randomizeDelays != null && config.randomizeDelays.enableRandomCastDelay) {
            if (config.randomizeDelays.castDelayMax < config.randomizeDelays.castDelayMin) {
                disableModWithError("Max Cast Delay cannot be less than Min Cast Delay!");
                return;
            }
            delayValue = config.randomizeDelays.castDelayMin + RANDOM.nextInt(config.randomizeDelays.castDelayMax - config.randomizeDelays.castDelayMin + 1);
        } else {
            delayValue = config.castDelay;
        }
    }

    private void disableModWithError(String error) {
        Utils.sendErrorMessage(player, error);
        config.active = false;
        Utils.sendActionBarMessage(client, Text.literal("Better Auto Fishing ").append(Text.literal("deactivated").formatted(Formatting.RED)));
        resetState();
    }
}