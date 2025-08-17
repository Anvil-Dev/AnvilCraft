package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.property.BoxContents;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.item.amulet.AmuletItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

public class AmuletSelectorSupport {
    public static final ResourceLocation BACKGROUND = AnvilCraft.of("textures/gui/container/amulet_box/background.png");
    public static final ResourceLocation SELECTION_BOX = AnvilCraft.of("textures/gui/container/amulet_box/selection_box.png");
    public static final int BACKGROUND_WIDTH = 78;
    public static final int BACKGROUND_HEIGHT = 80;

    private static ItemStack currentHoveringItemStack = ItemStack.EMPTY;
    private static int maxSelection = -1;
    private static Layout layout = null;
    private static BoxContents contents = null;

    public static void render(GuiGraphics guiGraphics, int x, int y) {
        if (currentHoveringItemStack == null) return;
        int left = x - BACKGROUND_WIDTH / 2;
        int top = y - BACKGROUND_HEIGHT - 5;
        RenderSystem.disableDepthTest();
        guiGraphics.blit(
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
            RenderSystem.disableDepthTest();
            layout.render(guiGraphics, left, top, contents);
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
            setCurrentSelectedIndex(contents.getSelection());
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
        return contents.getSelection();
    }

    private static void setCurrentSelectedIndex(int selection) {
        if (!hasHoveringItem() || contents == null) return;
        if (maxSelection <= 0) return;
        selection = Math.clamp(selection, 0, Math.max(0, maxSelection - 1));
        if (contents.getSelection() == selection) return;
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
            public void render(GuiGraphics guiGraphics, int x, int y, BoxContents content) {
            }
        },
        NO_AMULET((byte) 0, new boolean[][]{
            new boolean[]{false, false, false, false},
            new boolean[]{false, false, false, false},
            new boolean[]{false, false, false, false},
            new boolean[]{false, false, false, false}}
        ) {
            @Override
            public void render(GuiGraphics guiGraphics, int x, int y, BoxContents content) {
                PoseStack poseStack = guiGraphics.pose();
                poseStack.pushPose();
                poseStack.translate(0, 0, 1000);
                this.renderTotem(guiGraphics, x + 3, y + 3, content);
                poseStack.popPose();
            }
        },
        BIG_AMULET_1((byte) 1, new boolean[][]{
            new boolean[]{true, true, true, false},
            new boolean[]{true, true, true, false},
            new boolean[]{true, true, true, false},
            new boolean[]{false, false, false, false}}
        ) {
            @Override
            public void render(GuiGraphics guiGraphics, int x, int y, BoxContents content) {
                List<ItemStack> amulets = content.getAmulets();
                if (amulets.isEmpty()) return;

                PoseStack poseStack = guiGraphics.pose();
                poseStack.pushPose();
                poseStack.translate(0, 0, 1000);
                guiGraphics.fill(x + 3, y + 3, x + 3 + 53, y + 3 + 53, COLOR_FIRST);
                this.renderTotem(guiGraphics, x + 3, y + 3, content);

                if (getCurrentSelectedIndex() == 0) {
                    this.renderSelectionBox(guiGraphics, x + 3, y + 3, x + 3 + 53, y + 3 + 53);
                }
                poseStack.popPose();

                poseStack.pushPose();
                poseStack.translate(x + 4 + 2, y + 4 + 2, 1001);
                poseStack.scale(47f / 16, 47f / 16, 0);
                ItemStack amulet1 = amulets.getFirst();
                guiGraphics.renderFakeItem(amulet1, 0, 0);
                guiGraphics.renderItemDecorations(Minecraft.getInstance().font, amulet1, 0, 0);
                poseStack.popPose();
            }
        },
        SMALL_AMULET_1((byte) 1, new boolean[][]{
            new boolean[]{true, true, false, false},
            new boolean[]{true, true, false, false},
            new boolean[]{true, true, false, false},
            new boolean[]{false, false, false, false}}
        ) {
            @Override
            public void render(GuiGraphics guiGraphics, int x, int y, BoxContents content) {
                List<ItemStack> amulets = content.getAmulets();
                if (amulets.isEmpty()) return;

                PoseStack poseStack = guiGraphics.pose();
                poseStack.pushPose();
                poseStack.translate(0, 0, 1000);
                guiGraphics.fill(x + 3, y + 3, x + 3 + 35, y + 3 + 53, COLOR_FIRST);
                this.renderTotem(guiGraphics, x + 3, y + 3, content);

                if (getCurrentSelectedIndex() == 0) {
                    this.renderSelectionBox(guiGraphics, x + 3, y + 3, x + 3 + 35, y + 3 + 53);
                }
                poseStack.popPose();

                poseStack.pushPose();
                poseStack.translate(x + 4, y + 4 + 9, 1001);
                poseStack.scale(34f / 16, 34f / 16, 0);
                ItemStack amulet1 = amulets.getFirst();
                guiGraphics.renderFakeItem(amulet1, 0, 0);
                guiGraphics.renderItemDecorations(Minecraft.getInstance().font, amulet1, 0, 0);
                poseStack.popPose();
            }
        },
        SMALL_AMULET_2((byte) 2, new boolean[][]{
            new boolean[]{true, true, true, true},
            new boolean[]{true, true, true, true},
            new boolean[]{true, true, true, true},
            new boolean[]{false, false, false, false}}
        ) {
            @Override
            public void render(GuiGraphics guiGraphics, int x, int y, BoxContents content) {
                List<ItemStack> amulets = content.getAmulets();
                if (amulets.size() < 2) return;

                PoseStack poseStack = guiGraphics.pose();
                poseStack.pushPose();
                poseStack.translate(0, 0, 1000);
                guiGraphics.fill(x + 3, y + 3, x + 3 + 35, y + 3 + 53, COLOR_FIRST);
                guiGraphics.fill(x + 39, y + 3, x + 39 + 35, y + 3 + 53, COLOR_SECOND);
                this.renderTotem(guiGraphics, x + 3, y + 3, content);

                switch (getCurrentSelectedIndex()) {
                    case 0 -> this.renderSelectionBox(guiGraphics, x + 3, y + 3, x + 3 + 35, y + 3 + 53);
                    case 1 -> this.renderSelectionBox(guiGraphics, x + 39, y + 3, x + 39 + 35, y + 3 + 53);
                }
                poseStack.popPose();

                poseStack.pushPose();
                poseStack.translate(x + 4, y + 4 + 9, 1001);
                poseStack.scale(34f / 16, 34f / 16, 0);
                ItemStack amulet1 = amulets.getFirst();
                guiGraphics.renderFakeItem(amulet1, 0, 0);
                guiGraphics.renderItemDecorations(Minecraft.getInstance().font, amulet1, 0, 0);
                poseStack.popPose();

                poseStack.pushPose();
                poseStack.translate(x + 40, y + 4 + 9, 1001);
                poseStack.scale(34f / 16, 34f / 16, 0);
                ItemStack amulet2 = amulets.get(1);
                guiGraphics.renderFakeItem(amulet2, 0, 0);
                guiGraphics.renderItemDecorations(Minecraft.getInstance().font, amulet2, 0, 0);
                poseStack.popPose();
            }
        };

        private static final int COLOR_FIRST = 0x5522b14c;
        private static final int COLOR_SECOND = 0x5500a2e8;
        private static final int COLOR_TOTEM = 0x55ffc90e;
        private static final int COLOR_SELECTION_BOX_FRAME = 0xff663112;

        private final byte alreadyUsedIndexes;
        private final boolean[][] alreadyUsed;

        Layout(byte alreadyUsedIndexes, boolean[][] alreadyUsed) {
            this.alreadyUsedIndexes = alreadyUsedIndexes;
            this.alreadyUsed = alreadyUsed;
        }

        void renderTotem(GuiGraphics guiGraphics, int x, int y, BoxContents content) {
            List<ItemStack> totems = content.getTotems();
            if (totems.isEmpty()) return;
            int index = 0;
            for (int i = 0; i < 16; i++) {
                if (index >= totems.size()) return;
                if (this.alreadyUsed[i / 4][i % 4]) continue;
                ItemStack totem = totems.get(index++);
                int pX = x + i % 4 * 18;
                int pY = y + i / 4 * 18;
                guiGraphics.fill(pX, pY, pX + 17, pY + 17, COLOR_TOTEM);
                guiGraphics.renderFakeItem(totem, pX + 1, pY + 1);
                guiGraphics.renderItemDecorations(Minecraft.getInstance().font, totem, pX + 1, pY + 1);

                if (index + this.alreadyUsedIndexes - 1 != getCurrentSelectedIndex()) continue;
                this.renderSelectionBox(guiGraphics, pX, pY, pX + 18, pY + 18);
            }
        }

        @SuppressWarnings("UnusedAssignment")
        void renderSelectionBox(GuiGraphics guiGraphics, int minX, int minY, int maxX, int maxY) {
            maxX -= 9;
            maxY -= 9;
            guiGraphics.blit(SELECTION_BOX, minX, minY, 9, 9, 0, 0, 9, 9, 18, 18);
            guiGraphics.blit(SELECTION_BOX, maxX, minY, 9, 9, 9, 0, 9, 9, 18, 18);
            guiGraphics.blit(SELECTION_BOX, minX, maxY, 9, 9, 0, 9, 9, 9, 18, 18);
            guiGraphics.blit(SELECTION_BOX, maxX, maxY, 9, 9, 9, 9, 9, 9, 18, 18);

            int uWidth = maxX - minX - 9;
            int vHeight = maxY - minY - 9;
            if (uWidth != 0) {
                minX += 9;
                maxY += 9;
                guiGraphics.fill(minX, minY, minX + uWidth, minY + 1, COLOR_SELECTION_BOX_FRAME);
                guiGraphics.fill(minX, maxY - 1, minX + uWidth, maxY, COLOR_SELECTION_BOX_FRAME);
                minX -= 9;
                maxY -= 9;
            }
            if (vHeight != 0) {
                minY += 9;
                maxX += 9;
                guiGraphics.fill(minX, minY, minX + 1, minY + vHeight, COLOR_SELECTION_BOX_FRAME);
                guiGraphics.fill(maxX - 1, minY, maxX, minY + vHeight, COLOR_SELECTION_BOX_FRAME);
                minY -= 9;
                maxX -= 9;
            }
        }

        public abstract void render(GuiGraphics guiGraphics, int x, int y, BoxContents content);

        public static Layout layout(BoxContents content) {
            if (content.isEmpty()) {
                return EMPTY;
            }
            if (content.isAmuletEmpty()) {
                return NO_AMULET;
            }
            List<ItemStack> amulets = content.getAmulets();
            boolean firstBigAmulet = amulets.getFirst().getItem() instanceof AmuletItem amuletItem && amuletItem.getWeight() > 6;
            boolean firstSmallAmulet = amulets.getFirst().getItem() instanceof AmuletItem amuletItem && amuletItem.getWeight() <= 6;
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
