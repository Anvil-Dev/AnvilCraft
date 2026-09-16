package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.item.StoragePortItemRenderer;
import dev.dubhe.anvilcraft.client.support.ScaledGuiItemAtlases;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.joml.Matrix3x2f;

import java.util.ArrayList;
import java.util.List;

public final class StoragePortItemScene {
    private static final int[] SELECTED = {1, 3, 2, 4};
    private static final String[] LABELS = {"Empty", "Marked iron", "Zero diamond", "Unmarked gold", "Self mark", "Vanilla"};
    private static ItemScreen screen;
    private static int stage;
    private static int frames;
    private static boolean capturing;
    private static long warmAllocations;

    public static void frame(Minecraft client) {
        if (screen == null) {
            client.options.guiScale().set(2);
            client.resizeGui();
            screen = new ItemScreen(items(client));
            client.setScreen(screen);
        }
        if (stage >= SELECTED.length + 4) {
            AnvilCraft.LOGGER.info("PORT_STORAGE_ITEM_SCENE_PASSED");
            client.stop();
            return;
        }
        if (client.screen != screen || capturing) return;
        frames++;
        if (stage == 0 && frames == 30) warmAllocations = atlases(client).allocations();
        if (stage >= SELECTED.length && frames == 30) {
            int count = atlases(client).cachedAtlases();
            var requested = atlases(client).requestedResolutions();
            int maximum = requested.size() + 2;
            if (count > maximum) throw new IllegalStateException("闲置的放大图标缓存没有及时回收");
            AnvilCraft.LOGGER.info("PORT_SCALED_ITEM_CACHE_BOUNDED: stage={}, atlases={}, requested={}", stage, count, requested);
        }
        if (frames < 90) return;
        if (stage == 0) {
            long current = atlases(client).allocations();
            if (current == 0 || current != warmAllocations) throw new IllegalStateException("静态放大图标没有稳定复用图集");
            AnvilCraft.LOGGER.info("PORT_SCALED_ITEM_ATLAS_REUSED: {} allocations", current);
        }
        capturing = true;
        Screenshot.grab(client.gameDirectory, "storage-port-item-26.1-" + stage + ".png", client.getMainRenderTarget(), 1, message -> {
            AnvilCraft.LOGGER.info("PORT_STORAGE_ITEM_CAPTURED: {}", message.getString());
            client.execute(() -> {
                stage++;
                frames = 0;
                capturing = false;
            });
        });
    }

    private static List<ItemStack> items(Minecraft client) {
        List<ItemStack> items = new ArrayList<>();
        items.add(new ItemStack(ModBlocks.STORAGE_PORT.asItem()));
        for (int index = 1; index <= 4; index++) {
            var be = new StoragePortBlockEntity(ModBlockEntities.STORAGE_PORT.get(), BlockPos.ZERO,
                ModBlocks.STORAGE_PORT.getDefaultState());
            ItemStack mark = switch (index) {
                case 1 -> new ItemStack(Items.IRON_INGOT);
                case 2 -> new ItemStack(Items.DIAMOND);
                case 3 -> ItemStack.EMPTY;
                default -> new ItemStack(ModBlocks.STORAGE_PORT.asItem());
            };
            be.setMarkedItem(mark);
            if (index == 1) {
                be.getBuffer().set(0, ItemResource.of(Items.IRON_INGOT), 64);
                be.getBuffer().set(1, ItemResource.of(Items.IRON_INGOT), 12);
            } else if (index == 3) {
                be.getBuffer().set(0, ItemResource.of(Items.GOLD_INGOT), 5);
            }
            ItemStack stack = new ItemStack(ModBlocks.STORAGE_PORT.asItem());
            be.saveToDrop(stack, client.level.registryAccess());
            items.add(stack);
        }
        StoragePortItemRenderer renderer = new StoragePortItemRenderer();
        var first = renderer.extractArgument(items.get(1));
        var second = renderer.extractArgument(items.get(1));
        if (first == null || !first.equals(second) || first.hashCode() != second.hashCode()) {
            throw new IllegalStateException("静态标记未复用稳定的模型身份");
        }
        var animatedPort = new StoragePortBlockEntity(ModBlockEntities.STORAGE_PORT.get(), BlockPos.ZERO,
            ModBlocks.STORAGE_PORT.getDefaultState());
        ItemStack foil = new ItemStack(Items.DIAMOND);
        foil.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        animatedPort.setMarkedItem(foil);
        ItemStack animated = new ItemStack(ModBlocks.STORAGE_PORT.asItem());
        animatedPort.saveToDrop(animated, client.level.registryAccess());
        if (renderer.extractArgument(animated).equals(renderer.extractArgument(animated))) {
            throw new IllegalStateException("动画标记被静态图集身份冻结");
        }
        AnvilCraft.LOGGER.info("PORT_STORAGE_ITEM_IDENTITY_PASSED");
        items.add(new ItemStack(Items.DIAMOND));
        checkResolutions();
        return items;
    }

    private static ScaledGuiItemAtlases atlases(Minecraft client) {
        try {
            for (var field : client.gameRenderer.getClass().getDeclaredFields()) {
                if (field.getType() != GuiRenderer.class) continue;
                field.setAccessible(true);
                var renderer = field.get(client.gameRenderer);
                var atlases = GuiRenderer.class.getDeclaredField("anvilcraft$scaledItems");
                atlases.setAccessible(true);
                return (ScaledGuiItemAtlases) atlases.get(renderer);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("无法读取放大图标缓存", exception);
        }
        throw new IllegalStateException("缺少 GUI 渲染器");
    }

    private static void checkResolutions() {
        if (ScaledGuiItemAtlases.resolution(new Matrix3x2f(), 2, 4096) != 0
            || ScaledGuiItemAtlases.resolution(new Matrix3x2f().scale(0.75F), 2, 4096) != 0
            || ScaledGuiItemAtlases.resolution(new Matrix3x2f().scale(3), 2, 4096) != 96
            || ScaledGuiItemAtlases.resolution(new Matrix3x2f().scale(1.25F), 2, 4096) != 40
            || ScaledGuiItemAtlases.resolution(new Matrix3x2f().scale(2, 3), 2, 4096) != 96
            || ScaledGuiItemAtlases.resolution(new Matrix3x2f().rotate(0.5F).scale(-2, 2), 2, 4096) != 64) {
            throw new IllegalStateException("图标缩放与分辨率计算不符");
        }
        AnvilCraft.LOGGER.info("PORT_SCALED_ITEM_RESOLUTIONS_PASSED");
    }

    private static final class ItemScreen extends Screen {
        private final List<ItemStack> items;

        private ItemScreen(List<ItemStack> items) {
            super(Component.literal("Storage port item parity"));
            this.items = items;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xff252525);
            graphics.text(this.font, this.title, 40, 24, -1, false);
            for (int index = 0; index < this.items.size(); index++) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(40 + index * 100, 60);
                float scale = switch (stage) {
                    case 4 -> 2;
                    case 5 -> 4;
                    case 6 -> 2.5F;
                    case 7 -> 1;
                    default -> 3;
                };
                graphics.pose().scale(scale);
                graphics.item(this.items.get(index), 0, 0);
                graphics.pose().popMatrix();
                graphics.text(this.font, LABELS[index], 40 + index * 100, 116, -1, false);
                graphics.item(this.items.get(index), 40 + index * 100, 144);
            }
            ItemStack selected = this.items.get(SELECTED[Math.min(stage, SELECTED.length - 1)]);
            graphics.setTooltipForNextFrame(this.font, List.of(selected.getHoverName()), selected.getTooltipImage(), selected, 100, 220);
        }
    }
}
