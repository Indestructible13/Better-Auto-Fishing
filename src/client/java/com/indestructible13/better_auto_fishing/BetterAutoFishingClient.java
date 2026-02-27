package com.indestructible13.better_auto_fishing;

import com.indestructible13.better_auto_fishing.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.FishingBobberEntity;
import com.indestructible13.better_auto_fishing.mixin.FishingBobberEntityAccessor;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class BetterAutoFishingClient implements ClientModInitializer {
    public static final String MOD_ID = "better_auto_fishing";
    public static ModConfig config;
    public static final Logger LOGGER = LoggerFactory.getLogger("Better Auto Fishing");

    private enum AutoFishState {
        IDLE,
        CHECKING_WATER, // Used to check if fishing in open water
        WAITING_FOR_FISH,
        REELING,
        WAITING_FOR_CLEAR,
        CASTING
    }

    private AutoFishState currentState = AutoFishState.IDLE;
    private int tickCounter = 0;
    private int delayValue = 0;
    private static final Random RANDOM = new Random();
    private KeyBinding toggleActiveKey;
    private KeyBinding testKey;

    private MinecraftClient client;
    private PlayerEntity player;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Better Auto Fishing mod initializing...");

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

        // Custom key binding for testing
        testKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.better_auto_fishing.test", // The translation key for the key mapping.
                        InputUtil.Type.KEYSYM, // The type of the keybinding; KEYSYM for keyboard, MOUSE for mouse.
                        GLFW.GLFW_KEY_BACKSLASH, // The GLFW keycode of the key.
                        CATEGORY // The category of the mapping.
                ));

        // Register custom HUD renderer
        LootTableRenderer lootTableRenderer = new LootTableRenderer();
        HudElementRegistry.addLast(
                Identifier.of(MOD_ID, "test_overlay"),
                (drawContext, tickDeltaManager) -> {
                    if (config.extraOptions.showFishingLootTable) {
                        lootTableRenderer.renderTable(drawContext);
                    }
                }
        );
    }

    private void onTick(MinecraftClient client) {
        this.client = client;
        this.player = client.player;
        if (player == null) return;

        FishingBobberEntity bobber = player.fishHook;

        // Toggle the mod active state when the toggle keybind is pressed
        while (toggleActiveKey.wasPressed()) {
            config.active = !config.active;
            AutoConfig.getConfigHolder(ModConfig.class).save();

            Text activatedMessage = Text.literal("Better Auto Fishing ").append(Text.literal("activated").formatted(Formatting.GREEN));
            Text deactivatedMessage = Text.literal("Better Auto Fishing ").append(Text.literal("deactivated").formatted(Formatting.RED));
            Utils.sendActionBarMessage(client, config.active ? activatedMessage : deactivatedMessage);
        }

        // Do stuff when I press the test key
        while (testKey.wasPressed()) {
            LOGGER.info("Test key was pressed");

            if (bobber == null) {
                Utils.sendDebugChatMessage(player, "bobber is null");
                return;
            }

            // Tell me what block the bobber is in
            World world = bobber.getEntityWorld();
            BlockPos bobberPos = bobber.getBlockPos();
            BlockState state = world.getBlockState(bobberPos);
            //Utils.sendDebugChatMessage(player, state.getBlock().toString());
            Utils.sendDebugChatMessage(player, "=== Test =====================================");
            Utils.sendDebugChatMessage(player, String.format("In water: %s", state.isOf(Blocks.WATER)));
            boolean inSourceBlock = false;
            if (state.isOf(Blocks.WATER)) {
                FluidState fluidState = state.getFluidState();
                if (fluidState.isIn(FluidTags.WATER) && fluidState.isStill()) {
                    inSourceBlock = true;
                }
            }
            Utils.sendDebugChatMessage(player, String.format("In source block: %s", inSourceBlock));
            Utils.sendDebugChatMessage(player, String.format("In bubble column: %s", state.isOf(Blocks.BUBBLE_COLUMN)));
            boolean inWaterloggedBlockWithoutCollision = false;
            if (state.getFluidState().isIn(FluidTags.WATER)) {
                if (state.getCollisionShape(world, bobberPos).isEmpty()) {
                    inWaterloggedBlockWithoutCollision = true;
                }
            }
            Utils.sendDebugChatMessage(player, String.format("In collision-less waterlogged block: %s", inWaterloggedBlockWithoutCollision));
        }

        // Reset the state machine and return if either of these conditions are met:
        // The mod is inactive
        // pauseOnGui setting is set to true and a GUI is open
        if (!config.active || (config.extraOptions.pauseOnGui && client.currentScreen != null)) {
            resetState();
            return;
        }

        switch (currentState) {
            case IDLE:
                // While the bobber is null in the IDLE state, do nothing
                // If the bobber appears while in IDLE, it means the player cast the rod
                if (bobber != null) {
                    if (config.extraOptions.openWaterDetection) {
                        currentState = AutoFishState.CHECKING_WATER;
                    } else {
                        currentState = AutoFishState.WAITING_FOR_FISH;
                    }
                }
                break;

            case CHECKING_WATER:
                if (bobber != null) {
                    // Wait for the bobber to hit the water before attempting to check
                    World world = bobber.getEntityWorld();
                    BlockPos bobberPos = bobber.getBlockPos();
                    BlockState state = world.getBlockState(bobberPos);
                    // Bobber should be in a valid water layer type block
                    if (getBlockLayerType(state, world, bobberPos) != LayerType.WATER_LAYER) { return; }

                    if (!isOpenWater(bobber)) {
                        Utils.sendActionBarMessage(client, Text.literal("You are not fishing in open water!"));
                    }
                } else { // Bobber is null, it must have been reeled in manually again
                    resetState();
                    return;
                }
                currentState = AutoFishState.WAITING_FOR_FISH;
                break;

            case WAITING_FOR_FISH:
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
                } else { // Bobber is null, it must have been reeled in manually again
                    resetState();
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
                    if (protectRod()) {
                        Utils.sendActionBarMessage(client, Text.literal("Rod break protection activated!"));
                        resetState();
                        if (config.extraOptions.autoSwap) { swapFishingRod(); } // If the Auto Swap feature is enabled, swap rods when the break protection activates
                        return;
                    }
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
                    if (protectRod()) {
                        Utils.sendActionBarMessage(client, Text.literal("Rod break protection activated!"));
                        resetState();
                        if (config.extraOptions.autoSwap) { // If the Auto Swap feature is enabled, swap rods when the break protection activates
                            if (!swapFishingRod()) { return; } // Only return if the swap fails, otherwise just cast again because you have a new rod
                        } else {
                            return; // If the Auto Swap feature is off, then just return like normal
                        }
                    }
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

    // Returns true if the rod is at or below the rod break protection limit
    private boolean protectRod() {
        ItemStack handContent = player.getMainHandStack();
        if (!(handContent.getItem() instanceof FishingRodItem)) { return false; } // Not holding a fishing rod
        int rodDurability = handContent.getMaxDamage() - handContent.getDamage();
        return (rodDurability <= config.extraOptions.rodBreakProtectionThreshold);
    }

    // Searches the player's hotbar, swaps to a hotbar slot that contains a valid fishing rod
    private boolean swapFishingRod() {
        PlayerInventory inventory = player.getInventory();

        // Loop through hotbar slots, find a slot with a valid fishing rod
        for (int slot = 0; slot <= 8; slot++) {
            ItemStack itemStack = inventory.getStack(slot);
            //Utils.sendDebugChatMessage(player, String.format("Inventory slot %s contains %s", slot, itemStack.toString()));

            if (itemStack.getItem() instanceof FishingRodItem) {
                // Verify the rod's durability
                int durability = itemStack.getMaxDamage() - itemStack.getDamage();
                if (durability > config.extraOptions.rodBreakProtectionThreshold) {
                    inventory.setSelectedSlot(slot);
                    return true; // Return true if the swap was successful
                }
            }
        }

        // If no valid rod is found, say so
        Utils.sendActionBarMessage(client, Text.literal("No valid fishing rods in your hotbar to swap to!"));
        return false;
    }

    private boolean isOpenWater(FishingBobberEntity bobber) {
        World world = bobber.getEntityWorld();
        BlockPos bobberPos = bobber.getBlockPos();

        /*
         * Check 5x4x5 area around the bobber (2 blocks in each horizontal direction, -1 to +2 vertically)
         * Each horizontal layer must be entirely one type:
         * - EITHER: air and lily pads only
         * - OR: water source blocks, waterlogged blocks without collision, and bubble columns only
         * Mixing types in a single layer = not open water
         */

        // Check each of the 4 vertical layers
        for (int y = -1; y <= 2; y++) {
            LayerType expectedType = null;

            // Check all horizontal positions in this layer (5x5 grid)
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos checkPos = bobberPos.add(x, y, z);
                    BlockState state = world.getBlockState(checkPos);

                    LayerType blockType = getBlockLayerType(state, world, checkPos);

                    // Invalid block type in this layer, so the layer is invalid
                    if (blockType == LayerType.INVALID) {
                        return false;
                    }

                    // Determine what type this layer should be by checking the first block
                    if (expectedType == null) {
                        expectedType = blockType;
                    }

                    // This block doesn't match the layer type!
                    if (blockType != expectedType) {
                        return false;
                    }
                }
            }
        }

        return true; // All layers are consistent, this is open water!
    }

    /**
     * Determines what layer type a block belongs to
     */
    private LayerType getBlockLayerType(BlockState state, World world, BlockPos pos) {
        // Air or lily pad = AIR_LAYER type
        if (state.isAir() || state.isOf(Blocks.LILY_PAD)) {
            return LayerType.AIR_LAYER;
        }

        // Water source block = WATER_LAYER type
        if (state.isOf(Blocks.WATER)) {
            FluidState fluidState = state.getFluidState();
            if (fluidState.isIn(FluidTags.WATER) && fluidState.isStill()) {
                return LayerType.WATER_LAYER;
            }
        }

        // Bubble column = WATER_LAYER type
        if (state.isOf(Blocks.BUBBLE_COLUMN)) {
            return LayerType.WATER_LAYER;
        }

        // Waterlogged block without collision (signs, kelp, coral, etc.) = WATER_LAYER type
        if (state.getFluidState().isIn(FluidTags.WATER)) {
            if (state.getCollisionShape(world, pos).isEmpty()) {
                return LayerType.WATER_LAYER;
            }
        }

        // Anything else is invalid
        return LayerType.INVALID;
    }

    /**
     * Enum to categorize blocks into layer types
     */
    private enum LayerType {
        AIR_LAYER,    // Air and lily pads
        WATER_LAYER,  // Water, waterlogged blocks, bubble columns
        INVALID       // Anything else (dirt, stone, flowing water, etc.)
    }
}