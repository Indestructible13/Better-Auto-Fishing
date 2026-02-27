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

public class LootTableRenderer {
    private MinecraftClient client;
    private ModConfig config;
    private PlayerEntity player;
    public static final Logger LOGGER = LoggerFactory.getLogger("Better Auto Fishing");

    // Item icons - initialized once lazily
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
        stickIcon= new ItemStack(Items.STICK);
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
        RAW_COD(60.0), // 60% chance
        RAW_SALMON(25.0), // 25% chance
        PUFFERFISH(13.0), // 13% chance
        TROPICAL_FISH(2.0); // 2% chance

        private final double weight;

        FishType(double weight) {
            this.weight = weight;
        }

        @Override
        public double getWeight() {
            return weight;
        }
    }

    private enum TreasureType implements LootItem {
        BOW((1.0 / 6.0) * 100.0), // ~16.67% chance
        ENCHANTED_BOOK((1.0 / 6.0) * 100.0), // ~16.67% chance
        FISHING_ROD((1.0 / 6.0) * 100.0), // ~16.67% chance
        NAME_TAG((1.0 / 6.0) * 100.0), // ~16.67% chance
        NAUTILUS_SHELL((1.0 / 6.0) * 100.0), // ~16.67% chance
        SADDLE((1.0 / 6.0) * 100.0); // ~16.67% chance

        private final double weight;

        TreasureType(double weight) {
            this.weight = weight;
        }

        @Override
        public double getWeight() {
            return weight;
        }
    }

    private enum JunkType implements LootItem {
        LILY_PAD(17.0), // 17% chance
        BONE(10.0), // 10% chance
        BOWL(10.0), // 10% chance
        LEATHER(10.0), // 10% chance
        LEATHER_BOOTS(10.0), // 10% chance
        ROTTEN_FLESH(10.0), // 10% chance
        WATER_BOTTLE(10.0), // 10% chance
        TRIPWIRE_HOOK(10.0), // 10% chance
        STICK(5.0), // 5% chance
        STRING_ITEM(5.0), // 5% chance
        FISHING_ROD(2.0), // 2% chance
        INK_SAC(1.0); // 1% chance

        private final double weight;

        JunkType(double weight) {
            this.weight = weight;
        }

        @Override
        public double getWeight() {
            return weight;
        }
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

        // Initialize icons
        initIcons();

        // Get screen dimensions
        int screenWidth = drawContext.getScaledWindowWidth();
        int screenHeight = drawContext.getScaledWindowHeight();

        // Get table scale
        float scale = config.extraOptions.tableScale / 100f;

        // Calculate the root position of the table using the screen size and config percentages
        int rootX = (int)(screenWidth * (config.extraOptions.tableRootX / 100f));
        int rootY = (int)(screenHeight * (config.extraOptions.tableRootY / 100f));

        int rowHeight = 10; // pixels between rows
        int columnWidth = 50;  // pixels between columns
        int tableWidth = columnWidth * 3; // For 3 columns
        int tableHeight = rowHeight * 16; // For 16 rows, 2 for header, 2 for footer, and the rest for items

        // Draw with scaling
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().scale(scale, scale);

        // Add opacity to the table background
        int opacity = config.extraOptions.tableBackgroundOpacity;
        int alpha = (int)(opacity / 100f * 255);
        int backgroundColor = (alpha << 24);
        drawContext.fill(rootX, rootY - 2, rootX + tableWidth, rootY + tableHeight, backgroundColor);

        // Draw header text
        drawHeaderCell(drawContext, "FISH", String.format("(%s%%)", LootCategory.FISH.getWeight(lotsLevel)), rootX, rootY, 0, 0, columnWidth, rowHeight * 2 + 2);
        drawHeaderCell(drawContext, "TREASURE", String.format("(%s%%)", LootCategory.TREASURE.getWeight(lotsLevel)), rootX, rootY, 1, 0, columnWidth, rowHeight * 2 + 2);
        drawHeaderCell(drawContext, "JUNK", String.format("(%s%%)", LootCategory.JUNK.getWeight(lotsLevel)), rootX, rootY, 2, 0, columnWidth, rowHeight * 2 + 2);

        // Horizontal line under the header row (between row 0 and row 1)
        int lineY = rootY + (rowHeight * 2) - 2; // 2 pixels above row 1
        drawContext.fill(rootX, lineY, rootX + tableWidth, lineY + 1, 0xFFFFFFFF);

        // Vertical lines between columns
        int line1X = rootX + columnWidth * 1 - 2;
        int line2X = rootX + columnWidth * 2 + 2;
        int footerHeight = (rowHeight * 2) + 2; // Footer is 2 rows + separator line gap
        int bodyHeight = tableHeight - footerHeight + 2; // Necessary so the lines don't appear over the footer
        drawContext.fill(line1X, rootY - 2, line1X + 1, rootY + bodyHeight, 0xFFFFFFFF);
        drawContext.fill(line2X, rootY - 2, line2X + 1, rootY + bodyHeight, 0xFFFFFFFF);

        // Draw table body cells
        // Fish
        drawBodyCell(drawContext, codIcon, calculatePercentage(LootCategory.FISH, FishType.RAW_COD, lotsLevel), rootX, rootY, 0, 2, columnWidth, rowHeight);
        drawBodyCell(drawContext, salmonIcon, calculatePercentage(LootCategory.FISH, FishType.RAW_SALMON, lotsLevel), rootX, rootY, 0, 3, columnWidth, rowHeight);
        drawBodyCell(drawContext, pufferfishIcon, calculatePercentage(LootCategory.FISH, FishType.PUFFERFISH, lotsLevel), rootX, rootY, 0, 4, columnWidth, rowHeight);
        drawBodyCell(drawContext, tropicalFishIcon, calculatePercentage(LootCategory.FISH, FishType.TROPICAL_FISH, lotsLevel), rootX, rootY, 0, 5, columnWidth, rowHeight);
        // Treasure
        drawBodyCell(drawContext, enchantedBowIcon, calculatePercentage(LootCategory.TREASURE, TreasureType.BOW, lotsLevel), rootX, rootY, 1, 2, columnWidth, rowHeight);
        drawBodyCell(drawContext, enchantedBookIcon, calculatePercentage(LootCategory.TREASURE, TreasureType.ENCHANTED_BOOK, lotsLevel), rootX, rootY, 1, 3, columnWidth, rowHeight);
        drawBodyCell(drawContext, enchantedFishingRodIcon, calculatePercentage(LootCategory.TREASURE, TreasureType.FISHING_ROD, lotsLevel), rootX, rootY, 1, 4, columnWidth, rowHeight);
        drawBodyCell(drawContext, nameTagIcon, calculatePercentage(LootCategory.TREASURE, TreasureType.NAME_TAG, lotsLevel), rootX, rootY, 1, 5, columnWidth, rowHeight);
        drawBodyCell(drawContext, nautilusShellIcon, calculatePercentage(LootCategory.TREASURE, TreasureType.NAUTILUS_SHELL, lotsLevel), rootX, rootY, 1, 6, columnWidth, rowHeight);
        drawBodyCell(drawContext, saddleIcon, calculatePercentage(LootCategory.TREASURE, TreasureType.SADDLE, lotsLevel), rootX, rootY, 1, 7, columnWidth, rowHeight);
        // Junk
        drawBodyCell(drawContext, lilyPadIcon, calculatePercentage(LootCategory.JUNK, JunkType.LILY_PAD, lotsLevel), rootX, rootY, 2, 2, columnWidth, rowHeight);
        drawBodyCell(drawContext, boneIcon, calculatePercentage(LootCategory.JUNK, JunkType.BONE, lotsLevel), rootX, rootY, 2, 3, columnWidth, rowHeight);
        drawBodyCell(drawContext, bowlIcon, calculatePercentage(LootCategory.JUNK, JunkType.BOWL, lotsLevel), rootX, rootY, 2, 4, columnWidth, rowHeight);
        drawBodyCell(drawContext, leatherIcon, calculatePercentage(LootCategory.JUNK, JunkType.LEATHER, lotsLevel), rootX, rootY, 2, 5, columnWidth, rowHeight);
        drawBodyCell(drawContext, leatherBootsIcon, calculatePercentage(LootCategory.JUNK, JunkType.LEATHER_BOOTS, lotsLevel), rootX, rootY, 2, 6, columnWidth, rowHeight);
        drawBodyCell(drawContext, rottenFleshIcon, calculatePercentage(LootCategory.JUNK, JunkType.ROTTEN_FLESH, lotsLevel), rootX, rootY, 2, 7, columnWidth, rowHeight);
        drawBodyCell(drawContext, waterBottleIcon, calculatePercentage(LootCategory.JUNK, JunkType.WATER_BOTTLE, lotsLevel), rootX, rootY, 2, 8, columnWidth, rowHeight);
        drawBodyCell(drawContext, tripwireHookIcon, calculatePercentage(LootCategory.JUNK, JunkType.TRIPWIRE_HOOK, lotsLevel), rootX, rootY, 2, 9, columnWidth, rowHeight);
        drawBodyCell(drawContext, stickIcon, calculatePercentage(LootCategory.JUNK, JunkType.STICK, lotsLevel), rootX, rootY, 2, 10, columnWidth, rowHeight);
        drawBodyCell(drawContext, stringIcon, calculatePercentage(LootCategory.JUNK, JunkType.STRING_ITEM, lotsLevel), rootX, rootY, 2, 11, columnWidth, rowHeight);
        drawBodyCell(drawContext, fishingRodIcon, calculatePercentage(LootCategory.JUNK, JunkType.FISHING_ROD, lotsLevel), rootX, rootY, 2, 12, columnWidth, rowHeight);
        drawBodyCell(drawContext, inkSacIcon, calculatePercentage(LootCategory.JUNK, JunkType.INK_SAC, lotsLevel), rootX, rootY, 2, 13, columnWidth, rowHeight);

        // Draw footer
        String footerLine1 = String.format("Luck of the Sea %s  Lure %s", Utils.numToRomanNumeral(lotsLevel), Utils.numToRomanNumeral(lureLevel));
        String footerLine2 = String.format("Time to lure: %s seconds", getLureTimeRange(lureLevel));
        drawFooter(drawContext, footerLine1, footerLine2, rootX, rootY, tableHeight + 2, rowHeight, tableWidth);

        drawContext.getMatrices().popMatrix();
    }

    private void drawHeaderCell(DrawContext drawContext, String line1, String line2, int rootX, int rootY, int column, int row, int colWidth, int rowHeight) {
        int line1Width = client.textRenderer.getWidth(line1);
        int line2Width = client.textRenderer.getWidth(line2);
        int centeredLine1X = rootX + colWidth * column + (colWidth - line1Width) / 2;
        int centeredLine2X = rootX + colWidth * column + (colWidth - line2Width) / 2;
        int cellY = rootY + rowHeight * row;

        drawContext.drawText(client.textRenderer, line1, centeredLine1X, cellY, 0xFFFFFFFF, false);
        drawContext.drawText(client.textRenderer, line2, centeredLine2X, cellY + 9, 0xFFFFFFFF, false);
    }

    private void drawBodyCell(DrawContext drawContext, ItemStack icon, String text, int rootX, int rootY, int column, int row, int colWidth, int rowHeight) {
        int padding = 2;
        float iconScale = rowHeight / 16f; // Scale icon to fit row height
        int scaledIconSize = rowHeight; // After scaling, icon will be rowHeight pixels wide

        int textWidth = client.textRenderer.getWidth(text);
        int totalWidth = scaledIconSize + padding + textWidth;

        int startX = rootX + colWidth * column + (colWidth - totalWidth) / 2;
        int cellY = rootY + rowHeight * row;

        // Scale and draw the item icon
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(startX, cellY);
        drawContext.getMatrices().scale(iconScale, iconScale);
        drawContext.drawItem(icon, 0, 0);
        drawContext.getMatrices().popMatrix();

        // Draw the percentage text next to it
        drawContext.drawText(client.textRenderer, text, startX + scaledIconSize + padding, cellY, 0xFFFFFFFF, false);
    }

    private void drawFooter(DrawContext drawContext, String line1, String line2, int rootX, int rootY, int tableHeight, int rowHeight, int tableWidth) {
        int footer1Y = rootY + tableHeight - rowHeight * 2;
        int footer2Y = rootY + tableHeight - rowHeight;

        // Separator line above footer
        drawContext.fill(rootX, footer1Y - 2, rootX + tableWidth, footer1Y - 1, 0xFFFFFFFF);

        // Footer text
        drawContext.drawText(client.textRenderer, line1, rootX + 2, footer1Y, 0xFFFFFFFF, false);
        drawContext.drawText(client.textRenderer, line2, rootX + 2, footer2Y, 0xFFFFFFFF, false);
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

    private String getLureTimeRange(int lureLevel) {
        return switch (lureLevel){
            case 0 -> "5-30";
            case 1 -> "<25";
            case 2 -> "<20";
            case 3 -> "<15";
            default -> "Invalid Lure level";
        };
    }
}
