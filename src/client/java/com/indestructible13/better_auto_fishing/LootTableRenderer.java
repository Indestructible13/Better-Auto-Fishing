package com.indestructible13.better_auto_fishing;

import com.indestructible13.better_auto_fishing.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

public class LootTableRenderer {
    private MinecraftClient client;
    private ModConfig config;
    private PlayerEntity player;
    public static final Logger LOGGER = LoggerFactory.getLogger("Better Auto Fishing");

    // Layout constants — change these to adjust the table's appearance
    private static final int ROW_HEIGHT = 10;      // Height of each row in pixels
    private static final int COLUMN_WIDTH = 50;    // Width of each column in pixels
    private static final int PADDING = 2;          // Buffer space around content edges
    private static final int HEADER_ROWS = 2;      // Number of text rows in the header section
    private static final int FOOTER_ROWS = 2;      // Number of text rows in the footer section
    private static final int SEPARATOR_OFFSET = 4; // Pixels of space between a separator line and the content below it
    private static final int NUM_COLUMNS = 3;
    private static final int TABLE_WIDTH = COLUMN_WIDTH * NUM_COLUMNS;

    // Represents one item row in the table body — add new rows by adding entries to the bodyCells list in renderTable
    private record BodyCell(ItemStack icon, LootCategory category, LootItem subType, int column, int row) {}

    // Item icons - initialized once lazily on first render
    private ItemStack codIcon;
    private ItemStack salmonIcon;
    private ItemStack pufferfishIcon;
    private ItemStack tropicalFishIcon;
    private ItemStack enchantedBowIcon;
    private ItemStack enchantedBookIcon;
    private ItemStack enchantedFishingRodIcon;
    private ItemStack nameTagIcon;
    private ItemStack nautilusShellIcon;
    private ItemStack saddleIcon;
    private ItemStack lilyPadIcon;
    private ItemStack boneIcon;
    private ItemStack bowlIcon;
    private ItemStack leatherIcon;
    private ItemStack leatherBootsIcon;
    private ItemStack rottenFleshIcon;
    private ItemStack waterBottleIcon;
    private ItemStack tripwireHookIcon;
    private ItemStack stickIcon;
    private ItemStack stringIcon;
    private ItemStack fishingRodIcon;
    private ItemStack inkSacIcon;

    private void initIcons() {
        if (codIcon != null) return; // Already initialized

        // Fish
        codIcon = new ItemStack(Items.COD);
        salmonIcon = new ItemStack(Items.SALMON);
        pufferfishIcon = new ItemStack(Items.PUFFERFISH);
        tropicalFishIcon = new ItemStack(Items.TROPICAL_FISH);

        // Treasure
        enchantedBowIcon = new ItemStack(Items.BOW);
        enchantedBowIcon.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        enchantedBookIcon = new ItemStack(Items.ENCHANTED_BOOK);
        enchantedFishingRodIcon = new ItemStack(Items.FISHING_ROD);
        enchantedFishingRodIcon.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        nameTagIcon = new ItemStack(Items.NAME_TAG);
        nautilusShellIcon = new ItemStack(Items.NAUTILUS_SHELL);
        saddleIcon = new ItemStack(Items.SADDLE);

        // Junk
        lilyPadIcon = new ItemStack(Items.LILY_PAD);
        boneIcon = new ItemStack(Items.BONE);
        bowlIcon = new ItemStack(Items.BOWL);
        leatherIcon = new ItemStack(Items.LEATHER);
        leatherBootsIcon = new ItemStack(Items.LEATHER_BOOTS);
        rottenFleshIcon = new ItemStack(Items.ROTTEN_FLESH);
        waterBottleIcon = new ItemStack(Items.POTION);
        tripwireHookIcon = new ItemStack(Items.TRIPWIRE_HOOK);
        stickIcon = new ItemStack(Items.STICK);
        stringIcon = new ItemStack(Items.STRING);
        fishingRodIcon = new ItemStack(Items.FISHING_ROD);
        inkSacIcon = new ItemStack(Items.INK_SAC);
    }

    private enum LootCategory {
        FISH,
        TREASURE,
        JUNK;

        private double getWeight(int luckOfTheSeaLevel) {
            if (luckOfTheSeaLevel == -1) return 0;
            return switch (this) {
                // Base 85% - 0.15% per level
                case FISH -> 85.0 - (luckOfTheSeaLevel * 0.15); // Fish chance drops slightly

                // Base 5% + 2.1% per level
                case TREASURE -> 5.0 + (luckOfTheSeaLevel * 2.1); // Treasure chance rises

                // Base 10% - 1.95% per level
                case JUNK -> 10.0 - (luckOfTheSeaLevel * 1.95); // Junk chance drops
            };
        }
    }

    public interface LootItem {
        double getWeight();
    }

    private enum FishType implements LootItem {
        RAW_COD(60.0),       // 60% chance
        RAW_SALMON(25.0),    // 25% chance
        PUFFERFISH(13.0),    // 13% chance
        TROPICAL_FISH(2.0);  // 2% chance

        private final double weight;
        FishType(double weight) { this.weight = weight; }

        @Override
        public double getWeight() { return weight; }
    }

    private enum TreasureType implements LootItem {
        BOW((1.0 / 6.0) * 100.0),            // ~16.67% chance
        ENCHANTED_BOOK((1.0 / 6.0) * 100.0), // ~16.67% chance
        FISHING_ROD((1.0 / 6.0) * 100.0),    // ~16.67% chance
        NAME_TAG((1.0 / 6.0) * 100.0),       // ~16.67% chance
        NAUTILUS_SHELL((1.0 / 6.0) * 100.0), // ~16.67% chance
        SADDLE((1.0 / 6.0) * 100.0);         // ~16.67% chance

        private final double weight;
        TreasureType(double weight) { this.weight = weight; }

        @Override
        public double getWeight() { return weight; }
    }

    private enum JunkType implements LootItem {
        LILY_PAD(17.0),      // 17% chance
        BONE(10.0),          // 10% chance
        BOWL(10.0),          // 10% chance
        LEATHER(10.0),       // 10% chance
        LEATHER_BOOTS(10.0), // 10% chance
        ROTTEN_FLESH(10.0),  // 10% chance
        WATER_BOTTLE(10.0),  // 10% chance
        TRIPWIRE_HOOK(10.0), // 10% chance
        STICK(5.0),          // 5% chance
        STRING_ITEM(5.0),    // 5% chance
        FISHING_ROD(2.0),    // 2% chance
        INK_SAC(1.0);        // 1% chance

        private final double weight;
        JunkType(double weight) { this.weight = weight; }

        @Override
        public double getWeight() { return weight; }
    }

    public LootTableRenderer() {
        client = MinecraftClient.getInstance();
        config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }

    // Renders the table showing % chances of each item you can catch
    // based on the enchantments of the held rod
    public void renderTable(DrawContext drawContext) {
        if (player == null) {
            player = client.player;
            return;
        }

        // Get rod and enchantment info
        ItemStack handContent = player.getMainHandStack();
        if (!(handContent.getItem() instanceof FishingRodItem)) { return; } // Don't do anything if not holding a rod
        int lotsLevel = getLotsLevel(handContent);
        int lureLevel = getLureLevel(handContent);

        initIcons();

        int screenWidth = drawContext.getScaledWindowWidth();
        int screenHeight = drawContext.getScaledWindowHeight();
        float scale = config.extraOptions.tableScale / 100f;

        // Calculate root position, compensating for scale so percentage-based positioning is accurate
        int rootX = (int)((screenWidth * (config.extraOptions.tableRootX / 100f)) / scale);
        int rootY = (int)((screenHeight * (config.extraOptions.tableRootY / 100f)) / scale);

        // Body cells — to add a new row, just add a new BodyCell entry here
        // Column 0 = Fish, Column 1 = Treasure, Column 2 = Junk
        // Rows are 0-indexed relative to the start of the body section
        List<BodyCell> bodyCells = List.of(
                // Fish
                new BodyCell(codIcon,                LootCategory.FISH,     FishType.RAW_COD,              0, 0),
                new BodyCell(salmonIcon,             LootCategory.FISH,     FishType.RAW_SALMON,           0, 1),
                new BodyCell(pufferfishIcon,         LootCategory.FISH,     FishType.PUFFERFISH,           0, 2),
                new BodyCell(tropicalFishIcon,       LootCategory.FISH,     FishType.TROPICAL_FISH,        0, 3),
                // Treasure
                new BodyCell(enchantedBowIcon,       LootCategory.TREASURE, TreasureType.BOW,              1, 0),
                new BodyCell(enchantedBookIcon,      LootCategory.TREASURE, TreasureType.ENCHANTED_BOOK,   1, 1),
                new BodyCell(enchantedFishingRodIcon,LootCategory.TREASURE, TreasureType.FISHING_ROD,      1, 2),
                new BodyCell(nameTagIcon,            LootCategory.TREASURE, TreasureType.NAME_TAG,         1, 3),
                new BodyCell(nautilusShellIcon,      LootCategory.TREASURE, TreasureType.NAUTILUS_SHELL,   1, 4),
                new BodyCell(saddleIcon,             LootCategory.TREASURE, TreasureType.SADDLE,           1, 5),
                // Junk
                new BodyCell(lilyPadIcon,            LootCategory.JUNK,     JunkType.LILY_PAD,             2, 0),
                new BodyCell(boneIcon,               LootCategory.JUNK,     JunkType.BONE,                 2, 1),
                new BodyCell(bowlIcon,               LootCategory.JUNK,     JunkType.BOWL,                 2, 2),
                new BodyCell(leatherIcon,            LootCategory.JUNK,     JunkType.LEATHER,              2, 3),
                new BodyCell(leatherBootsIcon,       LootCategory.JUNK,     JunkType.LEATHER_BOOTS,        2, 4),
                new BodyCell(rottenFleshIcon,        LootCategory.JUNK,     JunkType.ROTTEN_FLESH,         2, 5),
                new BodyCell(waterBottleIcon,        LootCategory.JUNK,     JunkType.WATER_BOTTLE,         2, 6),
                new BodyCell(tripwireHookIcon,       LootCategory.JUNK,     JunkType.TRIPWIRE_HOOK,        2, 7),
                new BodyCell(stickIcon,              LootCategory.JUNK,     JunkType.STICK,                2, 8),
                new BodyCell(stringIcon,             LootCategory.JUNK,     JunkType.STRING_ITEM,          2, 9),
                new BodyCell(fishingRodIcon,         LootCategory.JUNK,     JunkType.FISHING_ROD,          2, 10),
                new BodyCell(inkSacIcon,             LootCategory.JUNK,     JunkType.INK_SAC,              2, 11)
        );

        // --- Section Y positions — the table is divided into 3 sections, each positioned below the last ---

        // Header: drawn at rootY, occupies HEADER_ROWS rows
        int headerSeparatorY = rootY + (HEADER_ROWS * ROW_HEIGHT);

        // Body: starts below the header separator, rows are 0-indexed relative to bodyStartY
        int bodyStartY = headerSeparatorY + SEPARATOR_OFFSET;
        int maxBodyRow = bodyCells.stream().mapToInt(BodyCell::row).max().orElse(0);
        int bodyEndY = bodyStartY + (maxBodyRow + 1) * ROW_HEIGHT;

        // Footer: starts below the body separator, lines are drawn relative to footerStartY
        int footerSeparatorY = bodyEndY;
        int footerStartY = footerSeparatorY + SEPARATOR_OFFSET;
        int footerEndY = footerStartY + FOOTER_ROWS * ROW_HEIGHT;

        // Background spans the full height of all three sections with a small buffer on top and bottom
        int backgroundTop    = rootY - PADDING;
        int backgroundBottom = footerEndY + PADDING;

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().scale(scale, scale);

        // Draw semi-transparent background behind the entire table
        int opacity = config.extraOptions.tableBackgroundOpacity;
        int alpha = (int)(opacity / 100f * 255);
        drawContext.fill(rootX, backgroundTop, rootX + TABLE_WIDTH, backgroundBottom, alpha << 24);

        // Draw header text (centered in each column)
        drawHeaderCell(drawContext, "FISH",     String.format("(%s%%)", LootCategory.FISH.getWeight(lotsLevel)),     rootX, rootY, 0);
        drawHeaderCell(drawContext, "TREASURE", String.format("(%s%%)", LootCategory.TREASURE.getWeight(lotsLevel)), rootX, rootY, 1);
        drawHeaderCell(drawContext, "JUNK",     String.format("(%s%%)", LootCategory.JUNK.getWeight(lotsLevel)),     rootX, rootY, 2);

        // Horizontal separator between header and body
        drawContext.fill(rootX, headerSeparatorY, rootX + TABLE_WIDTH, headerSeparatorY + 1, 0xFFFFFFFF);

        // Vertical column separators — run from background top to body end only (not through footer)
        int line1X = rootX + COLUMN_WIDTH - PADDING;
        int line2X = rootX + COLUMN_WIDTH * 2 + PADDING;
        drawContext.fill(line1X, backgroundTop, line1X + 1, footerSeparatorY, 0xFFFFFFFF);
        drawContext.fill(line2X, backgroundTop, line2X + 1, footerSeparatorY, 0xFFFFFFFF);

        // Draw all body cells, positioned relative to bodyStartY
        for (BodyCell cell : bodyCells) {
            drawBodyCell(drawContext, cell.icon(),
                    calculatePercentage(cell.category(), cell.subType(), lotsLevel),
                    rootX, bodyStartY, cell.column(), cell.row());
        }

        // Horizontal separator between body and footer
        drawContext.fill(rootX, footerSeparatorY, rootX + TABLE_WIDTH, footerSeparatorY + 1, 0xFFFFFFFF);

        // Draw footer lines positioned relative to footerStartY
        String footerLine1 = String.format("Luck of the Sea %s  Lure %s", Utils.numToRomanNumeral(lotsLevel), Utils.numToRomanNumeral(lureLevel));
        boolean isRaining = BetterAutoFishingClient.isRaining();
        boolean isSkyVisible = BetterAutoFishingClient.isSkyVisible();
        String footerLine2 = String.format("Time to lure: %ss %s%s", calculateLureTime(lureLevel, isRaining, isSkyVisible), (isRaining && BetterAutoFishingClient.getBobber() != null) ? "(raining)" : "", !isSkyVisible ? "(no sky)" : "");
        drawFooter(drawContext, footerLine1, footerLine2, rootX, footerStartY);

        drawContext.getMatrices().popMatrix();
    }

    private void drawHeaderCell(DrawContext drawContext, String line1, String line2, int rootX, int rootY, int column) {
        int line1Width = client.textRenderer.getWidth(line1);
        int line2Width = client.textRenderer.getWidth(line2);
        int centeredLine1X = rootX + COLUMN_WIDTH * column + (COLUMN_WIDTH - line1Width) / 2;
        int centeredLine2X = rootX + COLUMN_WIDTH * column + (COLUMN_WIDTH - line2Width) / 2;

        drawContext.drawText(client.textRenderer, line1, centeredLine1X, rootY,              0xFFFFFFFF, false);
        drawContext.drawText(client.textRenderer, line2, centeredLine2X, rootY + ROW_HEIGHT, 0xFFFFFFFF, false);
    }

    private void drawBodyCell(DrawContext drawContext, ItemStack icon, String text, int rootX, int bodyStartY, int column, int row) {
        float iconScale = ROW_HEIGHT / 16f; // Scale icon down to match row height (icons are natively 16x16)
        int scaledIconSize = ROW_HEIGHT;

        int textWidth = client.textRenderer.getWidth(text);
        int totalWidth = scaledIconSize + PADDING + textWidth;

        int startX = rootX + COLUMN_WIDTH * column + (COLUMN_WIDTH - totalWidth) / 2;
        int cellY = bodyStartY + ROW_HEIGHT * row;

        // Scale and draw the item icon, translating first so scaling is relative to the icon's position
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(startX, cellY);
        drawContext.getMatrices().scale(iconScale, iconScale);
        drawContext.drawItem(icon, 0, 0);
        drawContext.getMatrices().popMatrix();

        // Draw the percentage text next to the icon
        drawContext.drawText(client.textRenderer, text, startX + scaledIconSize + PADDING, cellY, 0xFFFFFFFF, false);
    }

    private void drawFooter(DrawContext drawContext, String line1, String line2, int rootX, int footerStartY) {
        // Footer text lines, drawn relative to footerStartY
        drawContext.drawText(client.textRenderer, line1, rootX + PADDING, footerStartY,              0xFFFFFFFF, false);
        drawContext.drawText(client.textRenderer, line2, rootX + PADDING, footerStartY + ROW_HEIGHT, 0xFFFFFFFF, false);
    }

    private <T extends LootItem> String calculatePercentage(LootCategory category, T subType, int lotsLevel) {
        double categoryChance = category.getWeight(lotsLevel) / 100f;
        double itemChance = subType.getWeight() / 100f;
        BigDecimal roundedValue = new BigDecimal((categoryChance * itemChance) * 100f).setScale(2, RoundingMode.HALF_UP);
        return roundedValue + "%";
    }

    private int getLotsLevel(ItemStack handContent) {
        int lotsLevel;
        if (handContent.getItem() instanceof FishingRodItem && client.world != null) {
            lotsLevel = handContent.getEnchantments().getLevel(
                    client.world.getRegistryManager()
                            .getOrThrow(RegistryKeys.ENCHANTMENT)
                            .getOrThrow(Enchantments.LUCK_OF_THE_SEA)
            );
        } else {
            lotsLevel = -1;
        }
        return lotsLevel;
    }

    private int getLureLevel(ItemStack handContent) {
        int lureLevel;
        if (handContent.getItem() instanceof FishingRodItem && client.world != null) {
            lureLevel = handContent.getEnchantments().getLevel(
                    client.world.getRegistryManager()
                            .getOrThrow(RegistryKeys.ENCHANTMENT)
                            .getOrThrow(Enchantments.LURE)
            );
        } else {
            lureLevel = -1;
        }
        return lureLevel;
    }

    private String calculateLureTime(int lureLevel, boolean isRaining, boolean skyVisible) {
        double minTicks;
        double maxTicks;
        switch (lureLevel) {
            case 1 -> {
                minTicks = 0;
                maxTicks = 25 * 20; // 25 seconds
            }
            case 2 -> {
                minTicks = 0;
                maxTicks = 20 * 20; // 20 seconds
            }
            case 3 -> {
                minTicks = 0;
                maxTicks = 15 * 20; // 15 seconds
            }
            default -> {
                minTicks = 5 * 20; // 5 seconds
                maxTicks = 30 * 20; // 30 seconds
            }
        }

        // Calculate effect of rain
        // When raining, each tick has a 25% chance of counting down 2 instead of 1
        // Means time will theoretically be reduced by about 1/5 (unless you get REALLY unlucky)
        if (isRaining) {
            minTicks = minTicks * 0.8;
            maxTicks = maxTicks * 0.8;
        }

        // Calculate effect of not being exposed to the sky
        // When there are blocks that stop or diffuse light above the bobber, each tick has a 50% chance of not decrementing the count
        // Means time will theoretically be doubled (unless you get REALLY lucky)
        if (!skyVisible) {
            minTicks = minTicks * 2;
            maxTicks = maxTicks * 2;
        }

        // Convert ticks to seconds
        double minSeconds = minTicks / 20.0;
        double maxSeconds = maxTicks / 20.0;

        // Convert min and max values to printable string output and return
        DecimalFormat df = new DecimalFormat("0.#");
        if (minSeconds <= 0) {
            return "<" + df.format(maxSeconds);
        } else return df.format(minSeconds) + "-" + df.format(maxSeconds);
    }
}