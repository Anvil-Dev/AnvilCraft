package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.item.FilterItemRenderer;
import dev.dubhe.anvilcraft.client.support.DiskDisplaySupport;
import dev.dubhe.anvilcraft.client.support.FittedItemRenderer;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.DiskData;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class FittedItemScene {
    private static ItemScreen screen;
    private static int frames;
    private static int stage;
    private static boolean capturing;
    private static FittedItemRenderer.Icon stable;
    private static FittedItemRenderer.Icon animated;
    private static long phaseStarted;

    public static void frame(Minecraft client) {
        if (screen == null) {
            client.options.guiScale().set(2);
            client.resizeGui();
            checkSelection();
            var diamond = filter(Items.DIAMOND.getDefaultInstance(), false);
            var sword = Items.DIAMOND_SWORD.getDefaultInstance();
            sword.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            screen = new ItemScreen(List.of(ModItems.FILTER.asStack(), diamond, filter(Items.DIAMOND.getDefaultInstance(), true),
                filter(sword, false), filter(Items.CLOCK.getDefaultInstance(), false), filter(diamond, false),
                filter(Items.CHEST.getDefaultInstance(), false), disk("minecraft:birch_sign"), disk("invalid:block")));
            if (!DiskDisplaySupport.recordedBlock(screen.items.get(7)).is(Items.BIRCH_SIGN)
                || !DiskDisplaySupport.recordedBlock(ModItems.DISK.asStack()).isEmpty()
                || !DiskDisplaySupport.recordedBlock(screen.items.get(8)).is(Items.ACACIA_SIGN)) {
                throw new IllegalStateException("磁盘方块标识未正确解析");
            }
            client.setScreen(screen);
        }
        if (capturing) return;
        if (frames == 0) phaseStarted = System.currentTimeMillis();
        frames++;
        var current = FittedItemRenderer.prepare(Items.DIAMOND.getDefaultInstance());
        var clock = FittedItemRenderer.prepare(Items.PRISMARINE.getDefaultInstance());
        if (frames == 40) {
            if (current == null || current.width() < 1 || current.height() < 1 || clock == null) {
                throw new IllegalStateException("离屏图标未生成");
            }
            try {
                var texture = (net.minecraft.client.renderer.texture.DynamicTexture)
                    client.getTextureManager().getTexture(current.texture());
                texture.getPixels().writeToFile(client.gameDirectory.toPath().resolve("screenshots/fitted-diamond-icon.png"));
                AnvilCraft.LOGGER.info("PORT_FITTED_ICON_PIXEL: {}", Integer.toHexString(texture.getPixels().getPixel(
                    texture.getPixels().getWidth() / 2, texture.getPixels().getHeight() / 2)));
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
            stable = current;
            animated = clock;
        }
        if (frames < 120 || System.currentTimeMillis() - phaseStarted < 1000) return;
        if (current != stable) throw new IllegalStateException("静态图标未复用缓存");
        if (clock == animated) throw new IllegalStateException("动态物品图标未刷新");
        if (stage >= 3) {
            AnvilCraft.LOGGER.info(
                "PORT_FITTED_ITEM_SCENE_PASSED: selection, disk identity, static reuse, animated refresh, nested capture");
            client.stop();
            return;
        }
        capturing = true;
        if (stage == 0) {
            Screenshot.takeScreenshot(client.getMainRenderTarget(), image -> {
                try (image) {
                    int pixel = image.getPixel(224, 176);
                    if ((pixel >> 8 & 255) <= (pixel >> 16 & 255)) throw new IllegalStateException("钻石预览颜色丢失");
                    AnvilCraft.LOGGER.info("PORT_FITTED_DISPLAY_COLOR_PASSED: {}", Integer.toHexString(pixel));
                }
            });
        }
        Screenshot.grab(client.gameDirectory, "fitted-items-26.1-" + stage + ".png", client.getMainRenderTarget(), 1, message -> {
            client.execute(() -> {
                AnvilCraft.LOGGER.info("PORT_FITTED_ITEM_CAPTURED: {}", message.getString());
                stage++;
                frames = 0;
                capturing = false;
            });
        });
    }

    private static ItemStack filter(ItemStack item, boolean blacklist) {
        var result = ModItems.FILTER.asStack();
        var content = new FilterContent();
        content.list().set(5, item);
        result.set(ModComponents.FILTER_CONTENT, content.setBlackList(blacklist));
        return result;
    }

    private static ItemStack disk(String block) {
        var result = ModItems.DISK.asStack();
        var tag = new CompoundTag();
        tag.putString("StoredFrom", "minecraft:sign");
        tag.putString("StoredBlock", block);
        result.set(ModComponents.DISK_DATA, new DiskData(tag));
        return result;
    }

    private static void checkSelection() {
        var content = new FilterContent(NonNullList.withSize(18, ItemStack.EMPTY), false, false);
        if (!FilterItemRenderer.selectDisplayed(content, 0).isEmpty()) throw new IllegalStateException("空过滤器应无预览");
        content.list().set(2, Items.DIAMOND.getDefaultInstance());
        content.list().set(17, Items.EMERALD.getDefaultInstance());
        if (!FilterItemRenderer.selectDisplayed(content, 999).is(Items.DIAMOND)
            || !FilterItemRenderer.selectDisplayed(content, 1000).is(Items.EMERALD)
            || !FilterItemRenderer.selectDisplayed(content, 2000).is(Items.DIAMOND)
            || !FilterItemRenderer.selectDisplayed(content, -1000).is(Items.EMERALD)) {
            throw new IllegalStateException("轮播必须跳过空格且按秒循环");
        }
    }

    private static final class ItemScreen extends Screen {
        private final List<ItemStack> items;

        private ItemScreen(List<ItemStack> items) {
            super(Component.literal("Filter and disk content parity"));
            this.items = items;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xff252525);
            graphics.text(this.font, this.title, 24, 20, -1, false);
            String[] labels = {"Empty", "Diamond", "Blacklist", "Foil", "Clock", "Nested", "Chest", "Birch sign", "Legacy"};
            for (int i = 0; i < this.items.size(); i++) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(20 + i * 68, 64);
                if (stage == 1) graphics.pose().rotate(0.5F);
                graphics.pose().scale(stage == 2 ? 2 : 3);
                graphics.item(this.items.get(i), 0, 0);
                graphics.pose().popMatrix();
                graphics.text(this.font, labels[i], 20 + i * 68, 122, -1, false);
                graphics.item(this.items.get(i), 20 + i * 68, 154);
            }
            graphics.item(Items.DIAMOND.getDefaultInstance(), 24, 210);
            graphics.item(Items.CHEST.getDefaultInstance(), 60, 210);
            graphics.text(this.font, "Vanilla references", 96, 214, -1, false);
        }
    }
}
