package com.indestructible13.better_auto_fishing;

import com.indestructible13.better_auto_fishing.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class LootTableRenderer {
    private final MinecraftClient client;
    private final ModConfig config;

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
        waterBottleIcon = new ItemStack(Items.GLASS_BOTTLE);
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
        BOW(1.0 / 6.0),
        ENCHANTED_BOOK(1.0 / 6.0),
        FISHING_ROD(1.0 / 6.0),
        NAME_TAG(1.0 / 6.0),
        NAUTILUS_SHELL(1.0 / 6.0),
        SADDLE(1.0 / 6.0);

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
        LILY_PAD(17.0),
        BONE(10.0),
        BOWL(10.0),
        LEATHER(10.0),
        LEATHER_BOOTS(10.0),
        ROTTEN_FLESH(10.0),
        WATER_BOTTLE(10.0),
        TRIPWIRE_HOOK(10.0),
        STICK(5.0),
        STRING_ITEM(5.0),
        FISHING_ROD(2.0),
        INK_SAC(1.0);

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
        int tableHeight = rowHeight * 14; // For 14 rows, 2 for header, and the rest for items

        // Draw with scaling
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().scale(scale, scale);

        // Add opacity to the table background
        int opacity = config.extraOptions.tableBackgroundOpacity;
        int alpha = (int)(opacity / 100f * 255);
        int backgroundColor = (alpha << 24);
        drawContext.fill(rootX, rootY, rootX + tableWidth, rootY + tableHeight, backgroundColor);

        // Draw header text
        drawHeaderCell(drawContext, "FISH", String.format("(%s%%)", LootCategory.FISH.getWeight(0)), rootX, rootY, 0, 0, columnWidth, rowHeight * 2 + 2);
        drawHeaderCell(drawContext, "TREASURE", String.format("(%s%%)", LootCategory.TREASURE.getWeight(0)), rootX, rootY, 1, 0, columnWidth, rowHeight * 2 + 2);
        drawHeaderCell(drawContext, "JUNK", String.format("(%s%%)", LootCategory.JUNK.getWeight(0)), rootX, rootY, 2, 0, columnWidth, rowHeight * 2 + 2);

        // Horizontal line under the header row (between row 0 and row 1)
        int lineY = rootY + (rowHeight * 2) - 2; // 2 pixels above row 1
        drawContext.fill(rootX, lineY, rootX + tableWidth, lineY + 1, 0xFFFFFFFF);

        // Vertical lines between columns
        int line1X = rootX + columnWidth * 1 - 4;
        int line2X = rootX + columnWidth * 2 + 4;
        drawContext.fill(line1X, rootY, line1X + 1, rootY + tableHeight, 0xFFFFFFFF);
        drawContext.fill(line2X, rootY, line2X + 1, rootY + tableHeight, 0xFFFFFFFF);

        // Draw table body cells
        // Fish
        drawBodyCell(drawContext, codIcon, "25%", rootX, rootY, 0, 2, columnWidth, rowHeight);
        drawBodyCell(drawContext, salmonIcon, "25%", rootX, rootY, 0, 3, columnWidth, rowHeight);
        drawBodyCell(drawContext, pufferfishIcon, "25%", rootX, rootY, 0, 4, columnWidth, rowHeight);
        drawBodyCell(drawContext, tropicalFishIcon, "25%", rootX, rootY, 0, 5, columnWidth, rowHeight);
        // Treasure
        drawBodyCell(drawContext, enchantedBowIcon, "25%", rootX, rootY, 1, 2, columnWidth, rowHeight);
        drawBodyCell(drawContext, enchantedBookIcon, "25%", rootX, rootY, 1, 3, columnWidth, rowHeight);
        drawBodyCell(drawContext, enchantedFishingRodIcon, "25%", rootX, rootY, 1, 4, columnWidth, rowHeight);
        drawBodyCell(drawContext, nameTagIcon, "25%", rootX, rootY, 1, 5, columnWidth, rowHeight);
        drawBodyCell(drawContext, nautilusShellIcon, "25%", rootX, rootY, 1, 6, columnWidth, rowHeight);
        drawBodyCell(drawContext, saddleIcon, "25%", rootX, rootY, 1, 7, columnWidth, rowHeight);
        // Junk
        drawBodyCell(drawContext, lilyPadIcon, "25%", rootX, rootY, 2, 2, columnWidth, rowHeight);
        drawBodyCell(drawContext, boneIcon, "25%", rootX, rootY, 2, 3, columnWidth, rowHeight);
        drawBodyCell(drawContext, bowlIcon, "25%", rootX, rootY, 2, 4, columnWidth, rowHeight);
        drawBodyCell(drawContext, leatherIcon, "25%", rootX, rootY, 2, 5, columnWidth, rowHeight);
        drawBodyCell(drawContext, leatherBootsIcon, "25%", rootX, rootY, 2, 6, columnWidth, rowHeight);
        drawBodyCell(drawContext, rottenFleshIcon, "25%", rootX, rootY, 2, 7, columnWidth, rowHeight);
        drawBodyCell(drawContext, waterBottleIcon, "25%", rootX, rootY, 2, 8, columnWidth, rowHeight);
        drawBodyCell(drawContext, tripwireHookIcon, "25%", rootX, rootY, 2, 9, columnWidth, rowHeight);
        drawBodyCell(drawContext, stickIcon, "25%", rootX, rootY, 2, 10, columnWidth, rowHeight);
        drawBodyCell(drawContext, stringIcon, "25%", rootX, rootY, 2, 11, columnWidth, rowHeight);
        drawBodyCell(drawContext, fishingRodIcon, "25%", rootX, rootY, 2, 12, columnWidth, rowHeight);
        drawBodyCell(drawContext, inkSacIcon, "25%", rootX, rootY, 2, 13, columnWidth, rowHeight);

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
        drawContext.drawItem(icon, 0, 0); // draw at 0,0 since we already translated
        drawContext.getMatrices().popMatrix();

        // Draw the percentage text next to it
        drawContext.drawText(client.textRenderer, text, startX + scaledIconSize + padding, cellY, 0xFFFFFFFF, false);
    }

    public <T extends LootItem> double calculatePercentage(LootCategory category, T subType, int lotsLevel) {
        return category.getWeight(lotsLevel) * subType.getWeight();
    }
}
