package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.BoxContents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class AmuletSelectorSupport {
    public static final Identifier BACKGROUND = SharedTextures.bg("misc", "amulet_box");
    public static final int BACKGROUND_WIDTH = 78;
    public static final int BACKGROUND_HEIGHT = 80;

    private static ItemStack currentHoveringItemStack = ItemStack.EMPTY;
    private static int maxSelection = -1;
    private static @Nullable Layout layout = null;
    private static @Nullable BoxContents contents = null;

    public static void render(GuiGraphicsExtractor graphics, int x, int y) {
        int left = x - BACKGROUND_WIDTH / 2;
        int top = y - BACKGROUND_HEIGHT - 5;
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            BACKGROUND,
            left,
            top,
            0,
            0,
            BACKGROUND_WIDTH,
            BACKGROUND_HEIGHT,
            BACKGROUND_WIDTH,
            BACKGROUND_HEIGHT
        );
        if (layout != null && contents != null) {
            layout.extract(graphics, left, top, contents);
        }
    }

    public static boolean hasHoveringItem() {
        return !currentHoveringItemStack.isEmpty();
    }

    public static void setCurrentHoveringItemStack(ItemStack itemStack) {
        if (ItemStack.isSameItemSameComponents(currentHoveringItemStack, itemStack)) return;
        AmuletSelectorSupport.currentHoveringItemStack = itemStack;
        if (itemStack.isEmpty()) {
            AmuletSelectorSupport.contents = null;
            AmuletSelectorSupport.layout = null;
            maxSelection = -1;
            return;
        }

        BoxContents contents = itemStack.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY);
        if (Objects.equals(AmuletSelectorSupport.contents, contents)) return;
        AmuletSelectorSupport.contents = contents;
        if (contents.isEmpty()) {
            AmuletSelectorSupport.layout = Layout.EMPTY;
            maxSelection = -1;
            setCurrentSelectedIndex(-1);
        } else {
            AmuletSelectorSupport.layout = Layout.layout(contents);
            maxSelection = contents.getMaxSelection();
            setCurrentSelectedIndex(contents.selection());
        }
    }

    public static void mouseScrolled(int amount) {
        if (getCurrentSelectedIndex() == -1) return;
        if (amount > 0) {
            next();
        } else {
            if (amount < 0) {
                previous();
            }
        }
    }

    public static void previous() {
        selectDelta(-1);
    }

    public static void next() {
        selectDelta(1);
    }

    public static void selectDelta(int delta) {
        int index = getCurrentSelectedIndex() + delta;
        if (index < 0) {
            index = maxSelection - 1;
        } else if (index > maxSelection - 1) {
            index = 0;
        }
        setCurrentSelectedIndex(index);
    }

    private static int getCurrentSelectedIndex() {
        if (contents == null) return -1;
        return contents.selection();
    }

    private static void setCurrentSelectedIndex(int selection) {
        if (!hasHoveringItem() || contents == null) return;
        if (maxSelection <= 0) return;
        selection = Math.clamp(selection, 0, Math.max(0, maxSelection - 1));
        if (contents.selection() == selection) return;
        BoxContents.Mutable mutable = contents.mutable();
        mutable.select(selection);
        contents = mutable.immutable();
        currentHoveringItemStack.set(ModComponents.BOX_CONTENTS, contents);
    }

    public enum Layout {
        EMPTY((byte) 0, new boolean[][]{
            new boolean[]{false, false, false, false},
            new boolean[]{false, false, false, false},
            new boolean[]{false, false, false, false},
            new boolean[]{false, false, false, false}}
        ) {
            @Override
            public void extract(GuiGraphicsExtractor graphics, int x, int y, BoxContents content) {
            }
        },
        NO_AMULET((byte) 0, new boolean[][]{
            new boolean[]{false, false, false, false},
            new boolean[]{false, false, false, false},
            new boolean[]{false, false, false, false},
            new boolean[]{false, false, false, false}}
        ) {
            @Override
            public void extract(GuiGraphicsExtractor graphics, int x, int y, BoxContents content) {
                Matrix3x2fStack pose = graphics.pose();
                pose.pushMatrix();
                pose.translate(0, 0);
                this.extractTotem(graphics, x + 3, y + 3, content);
                pose.popMatrix();
            }
        },
        BIG_AMULET_1((byte) 1, new boolean[][]{
            new boolean[]{true, true, true, false},
            new boolean[]{true, true, true, false},
            new boolean[]{true, true, true, false},
            new boolean[]{false, false, false, false}}
        ) {
            @Override
            public void extract(GuiGraphicsExtractor graphics, int x, int y, BoxContents content) {
                List<ItemStack> amulets = content.amulets();
                if (amulets.isEmpty()) return;

                Matrix3x2fStack pose = graphics.pose();
                pose.pushMatrix();
                pose.translate(0, 0);
                graphics.fill(x + 3, y + 3, x + 3 + 53, y + 3 + 53, COLOR_FIRST);
                this.extractTotem(graphics, x + 3, y + 3, content);

                if (getCurrentSelectedIndex() == 0) {
                    this.renderSelectionBox(graphics, x + 3, y + 3, x + 3 + 53, y + 3 + 53);
                }
                pose.popMatrix();

                pose.pushMatrix();
                pose.translate(x + 4 + 2, y + 4 + 2);
                pose.scale(47F / 16, 47F / 16);
                ItemStack amulet1 = amulets.getFirst();
                graphics.fakeItem(amulet1, 0, 0);
                graphics.itemDecorations(Minecraft.getInstance().font, amulet1, 0, 0);
                pose.popMatrix();
            }
        },
        SMALL_AMULET_1((byte) 1, new boolean[][]{
            new boolean[]{true, true, false, false},
            new boolean[]{true, true, false, false},
            new boolean[]{true, true, false, false},
            new boolean[]{false, false, false, false}}
        ) {
            @Override
            public void extract(GuiGraphicsExtractor graphics, int x, int y, BoxContents content) {
                List<ItemStack> amulets = content.amulets();
                if (amulets.isEmpty()) return;

                Matrix3x2fStack pose = graphics.pose();
                pose.pushMatrix();
                pose.translate(0, 0);
                graphics.fill(x + 3, y + 3, x + 3 + 35, y + 3 + 53, COLOR_FIRST);
                this.extractTotem(graphics, x + 3, y + 3, content);

                if (getCurrentSelectedIndex() == 0) {
                    this.renderSelectionBox(graphics, x + 3, y + 3, x + 3 + 35, y + 3 + 53);
                }
                pose.popMatrix();

                pose.pushMatrix();
                pose.translate(x + 4, y + 4 + 9);
                pose.scale(34F / 16, 34F / 16);
                ItemStack amulet1 = amulets.getFirst();
                graphics.fakeItem(amulet1, 0, 0);
                graphics.itemDecorations(Minecraft.getInstance().font, amulet1, 0, 0);
                pose.popMatrix();
            }
        },
        SMALL_AMULET_2((byte) 2, new boolean[][]{
            new boolean[]{true, true, true, true},
            new boolean[]{true, true, true, true},
            new boolean[]{true, true, true, true},
            new boolean[]{false, false, false, false}}
        ) {
            @Override
            public void extract(GuiGraphicsExtractor graphics, int x, int y, BoxContents content) {
                List<ItemStack> amulets = content.amulets();
                if (amulets.size() < 2) return;

                Matrix3x2fStack pose = graphics.pose();
                pose.pushMatrix();
                pose.translate(0, 0);
                graphics.fill(x + 3, y + 3, x + 3 + 35, y + 3 + 53, COLOR_FIRST);
                graphics.fill(x + 39, y + 3, x + 39 + 35, y + 3 + 53, COLOR_SECOND);
                this.extractTotem(graphics, x + 3, y + 3, content);

                switch (getCurrentSelectedIndex()) {
                    case 0 -> this.renderSelectionBox(graphics, x + 3, y + 3, x + 3 + 35, y + 3 + 53);
                    case 1 -> this.renderSelectionBox(graphics, x + 39, y + 3, x + 39 + 35, y + 3 + 53);
                    default -> {
                    }
                }
                pose.popMatrix();

                pose.pushMatrix();
                pose.translate(x + 4, y + 4 + 9);
                pose.scale(34F / 16, 34F / 16);
                ItemStack amulet1 = amulets.getFirst();
                graphics.fakeItem(amulet1, 0, 0);
                graphics.itemDecorations(Minecraft.getInstance().font, amulet1, 0, 0);
                pose.popMatrix();

                pose.pushMatrix();
                pose.translate(x + 40, y + 4 + 9);
                pose.scale(34F / 16, 34F / 16);
                ItemStack amulet2 = amulets.get(1);
                graphics.fakeItem(amulet2, 0, 0);
                graphics.itemDecorations(Minecraft.getInstance().font, amulet2, 0, 0);
                pose.popMatrix();
            }
        };

        private static final int COLOR_FIRST = 0x5522b14c;
        private static final int COLOR_SECOND = 0x5500a2e8;
        private static final int COLOR_TOTEM = 0x55Ffc90e;
        private static final int COLOR_SELECTION_BOX_FRAME = 0xff663112;

        private final byte alreadyUsedIndexes;
        private final boolean[][] alreadyUsed;

        Layout(byte alreadyUsedIndexes, boolean[][] alreadyUsed) {
            this.alreadyUsedIndexes = alreadyUsedIndexes;
            this.alreadyUsed = alreadyUsed;
        }

        void extractTotem(GuiGraphicsExtractor graphics, int x, int y, BoxContents content) {
            List<ItemStack> totems = content.totems();
            if (totems.isEmpty()) return;
            int index = 0;
            for (int i = 0; i < 16; i++) {
                if (index >= totems.size()) return;
                if (this.alreadyUsed[i / 4][i % 4]) continue;
                ItemStack totem = totems.get(index++);
                int minX = x + i % 4 * 18;
                int minY = y + i / 4 * 18;
                graphics.fill(minX, minY, minX + 17, minY + 17, COLOR_TOTEM);
                graphics.fakeItem(totem, minX + 1, minY + 1);
                graphics.itemDecorations(Minecraft.getInstance().font, totem, minX + 1, minY + 1);

                if (index + this.alreadyUsedIndexes - 1 != getCurrentSelectedIndex()) continue;
                this.renderSelectionBox(graphics, minX, minY, minX + 18, minY + 18);
            }
        }

        @SuppressWarnings("UnusedAssignment")
        void renderSelectionBox(GuiGraphicsExtractor graphics, int minX, int minY, int maxX, int maxY) {
            maxX -= 9;
            maxY -= 9;
            graphics.blit(RenderPipelines.GUI_TEXTURED, SharedTextures.BOX_SELECTION, minX, minY, 0, 0, 9, 9, 18, 18);
            graphics.blit(RenderPipelines.GUI_TEXTURED, SharedTextures.BOX_SELECTION, maxX, minY, 9, 0, 9, 9, 18, 18);
            graphics.blit(RenderPipelines.GUI_TEXTURED, SharedTextures.BOX_SELECTION, minX, maxY, 0, 9, 9, 9, 18, 18);
            graphics.blit(RenderPipelines.GUI_TEXTURED, SharedTextures.BOX_SELECTION, maxX, maxY, 9, 9, 9, 9, 18, 18);

            int widthU = maxX - minX - 9;
            int heightV = maxY - minY - 9;
            if (widthU != 0) {
                minX += 9;
                maxY += 9;
                graphics.fill(minX, minY, minX + widthU, minY + 1, COLOR_SELECTION_BOX_FRAME);
                graphics.fill(minX, maxY - 1, minX + widthU, maxY, COLOR_SELECTION_BOX_FRAME);
                minX -= 9;
                maxY -= 9;
            }
            if (heightV != 0) {
                minY += 9;
                maxX += 9;
                graphics.fill(minX, minY, minX + 1, minY + heightV, COLOR_SELECTION_BOX_FRAME);
                graphics.fill(maxX - 1, minY, maxX, minY + heightV, COLOR_SELECTION_BOX_FRAME);
                minY -= 9;
                maxX -= 9;
            }
        }

        public abstract void extract(GuiGraphicsExtractor graphics, int x, int y, BoxContents content);

        public static Layout layout(BoxContents content) {
            if (content.isEmpty()) {
                return EMPTY;
            }
            if (content.isAmuletEmpty()) {
                return NO_AMULET;
            }
            List<ItemStack> amulets = content.amulets();
            boolean firstBigAmulet = amulets.getFirst().has(ModComponents.AMULET)
                                     && amulets.getFirst().get(ModComponents.AMULET).getWeight() > 6;
            boolean firstSmallAmulet = amulets.getFirst().has(ModComponents.AMULET)
                                       && amulets.getFirst().get(ModComponents.AMULET).getWeight() <= 6;
            if (firstBigAmulet) {
                return BIG_AMULET_1;
            }
            if (firstSmallAmulet) {
                if (amulets.size() == 1) {
                    return SMALL_AMULET_1;
                }
                return SMALL_AMULET_2;
            }
            return Layout.EMPTY;
        }
    }
}
