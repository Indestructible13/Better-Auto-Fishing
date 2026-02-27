package com.indestructible13.better_auto_fishing.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "better_auto_fishing")
public class ModConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip
    public boolean active = true;

    @ConfigEntry.BoundedDiscrete(min = 1, max = 40)
    public int reelDelay = 10; // Time in ticks to wait before reeling in a fish

    @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
    public int castDelay = 20; // Time in ticks to wait before casting the rod

    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip
    public RandomizeDelays randomizeDelays = new RandomizeDelays();

    public static class RandomizeDelays {
        @ConfigEntry.Gui.Tooltip
        public boolean enableRandomReelDelay = false;

        @ConfigEntry.BoundedDiscrete(min = 1, max = 40)
        public int reelDelayMin = 5;

        @ConfigEntry.BoundedDiscrete(min = 1, max = 40)
        public int reelDelayMax = 15;

        @ConfigEntry.Gui.Tooltip
        public boolean enableRandomCastDelay = false;

        @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
        public int castDelayMin = 10;

        @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
        public int castDelayMax = 30;
    }

    @ConfigEntry.Gui.CollapsibleObject
    public ExtraOptions extraOptions = new ExtraOptions();

    public static class ExtraOptions {

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 64)
        public int rodBreakProtectionThreshold = 3; // Stop using the rod at this durability threshold

        @ConfigEntry.Gui.Tooltip
        public boolean autoSwap = false;

        @ConfigEntry.Gui.Tooltip
        public boolean openWaterDetection = true;

        @ConfigEntry.Gui.Tooltip
        public boolean pauseOnGui = false;

        @ConfigEntry.Gui.Tooltip
        public boolean showFishingLootTable = false;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int tableRootX = 2; // Table will be rooted 2% of the way from the left edge of the screen

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int tableRootY = 2; // Table will be rooted 2% of the way from the top edge of the screen

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
        public int tableScale = 50; // Percentage value the table will be scaled by

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int tableBackgroundOpacity = 20; // %
    }
}