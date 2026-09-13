package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.DefinitionSerialization;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.DiskDisplaySupport;
import dev.dubhe.anvilcraft.client.support.FittedItemRenderer;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.storage.LevelResource;
import org.joml.Matrix4f;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class StructureDiskScene {
    private static boolean started;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static List<StructureDiskData> disks;
    private static ItemScreen screen;
    private static int stage;
    private static int frames;
    private static boolean capturing;
    private static boolean reloading;
    private static volatile boolean reloaded;
    private static long startedAt;
    private static Object oldTexture;
    private static TextureTransform clock;
    private static Object originalClock;
    private static Field clockSupplier;

    public static void frame(Minecraft client) {
        if (!started) {
            started = true;
            startedAt = System.currentTimeMillis();
            freezeClock();
            client.getSingleplayerServer().execute(() -> prepare(client));
        }
        if (failure != null) throw new IllegalStateException("结构磁盘实景验证失败", failure);
        if (System.currentTimeMillis() - startedAt > 120000) throw new IllegalStateException("结构磁盘实景验证超时");
        if (!prepared || client.getOverlay() != null || capturing) return;
        if (screen == null) {
            List<ItemStack> items = new ArrayList<>();
            items.add(ModItems.STRUCTURE_DISK.asStack());
            for (int index = 0; index < 3; index++) {
                var item = client.player.getInventory().getItem(9 + index);
                if (!disks.get(index).equals(item.get(ModComponents.STRUCTURE_DISK_DATA))) return;
                if (DiskDisplaySupport.getDisplay(item).isEmpty()) return;
                items.add(item.copy());
            }
            var bad = items.get(1).copy();
            var data = disks.getFirst();
            bad.set(ModComponents.STRUCTURE_DISK_DATA,
                new StructureDiskData(data.file(), "Mismatch", data.uuid(), Direction.NORTH, 3, 2, 3));
            if (!DiskDisplaySupport.getDisplay(bad).isEmpty()) throw new IllegalStateException("不匹配的结构不应显示图标");
            items.add(bad);
            client.options.guiScale().set(2);
            client.resizeGui();
            screen = new ItemScreen(items);
            client.setScreen(screen);
        }
        if (++frames < 150) return;
        if (stage == 2) {
            restoreClock();
            var icon = FittedItemRenderer.prepare(Items.DIAMOND_BLOCK.getDefaultInstance());
            if (icon == null) return;
            oldTexture = icon.texture();
            stage++;
            frames = 0;
            return;
        }
        if (stage == 4 && !reloading) {
            reloading = true;
            client.reloadResourcePacks().whenComplete((ignored, error) -> {
                failure = error;
                reloaded = true;
            });
            return;
        }
        if (stage == 4) {
            if (!reloaded) return;
            var icon = FittedItemRenderer.prepare(Items.DIAMOND_BLOCK.getDefaultInstance());
            if (icon == null) return;
            if (icon.texture().equals(oldTexture)) throw new IllegalStateException("资源重载后未重建蓝图纹理");
            client.getSingleplayerServer().execute(() -> {
                var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.getInventory().setItem(0, player.getInventory().getItem(9).copy());
                player.inventoryMenu.broadcastChanges();
            });
            client.player.getInventory().setSelectedSlot(0);
            client.setScreen(null);
            stage++;
            frames = 0;
            return;
        }
        if (stage >= 6) {
            AnvilCraft.LOGGER.info(
                "PORT_STRUCTURE_DISK_SCENE_PASSED: network structures, matching, scan material, resource reload, held item");
            client.stop();
            return;
        }
        capturing = true;
        Screenshot.grab(client.gameDirectory, "structure-disk-26.1-" + stage + ".png", client.getMainRenderTarget(), 1, message -> {
            client.execute(() -> {
                AnvilCraft.LOGGER.info("PORT_STRUCTURE_DISK_CAPTURED: {}", message.getString());
                stage++;
                frames = 0;
                capturing = false;
            });
        });
    }

    private static void prepare(Minecraft client) {
        try {
            var server = client.getSingleplayerServer();
            var directory = server.getWorldPath(LevelResource.ROOT).resolve("anvilcraft/structures");
            Files.createDirectories(directory);
            var player = server.getPlayerList().getPlayers().getFirst();
            var recipes = server.getRecipeManager().recipeMap();
            String[] names = {"diamond_block", "giant_anvil_1", "spawner"};
            List<StructureDiskData> data = new ArrayList<>();
            for (int index = 0; index < names.length; index++) {
                String name = names[index];
                MultiblockDefinition definition;
                if (index == 2) {
                    definition = recipes.byType(ModRecipeTypes.MULTIBLOCK_CONVERSION.get()).stream()
                        .filter(holder -> holder.id().identifier().getPath().equals("multiblock_conversion/" + name))
                        .findFirst().orElseThrow().value().getInputPattern();
                } else {
                    definition = recipes.byType(ModRecipeTypes.MULTIBLOCK.get()).stream()
                        .filter(holder -> holder.id().identifier().getPath().equals("multiblock/" + name))
                        .findFirst().orElseThrow().value().getPattern();
                }
                int size = DefinitionSerialization.fromDefinition(definition).grid().length;
                Direction direction = index == 1 ? Direction.EAST : Direction.NORTH;
                boolean flipped = index == 1;
                UUID id = UUID.randomUUID();
                var diskData = new StructureDiskData("scan_" + id + ".nbt", name, id, direction, size, size, size, flipped);
                NbtIo.writeCompressed(StructureDiskDisplayTests.structure(definition, direction, Rotation.NONE, flipped),
                    directory.resolve(diskData.file()));
                var stack = ModItems.STRUCTURE_DISK.asStack();
                stack.set(ModComponents.STRUCTURE_DISK_DATA, diskData);
                stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
                player.getInventory().setItem(9 + index, stack);
                data.add(diskData);
            }
            player.inventoryMenu.broadcastChanges();
            disks = List.copyOf(data);
            prepared = true;
        } catch (Throwable error) {
            failure = error;
        }
    }

    private static void freezeClock() {
        try {
            var field = FittedItemRenderer.class.getDeclaredField("SCAN_CLOCK");
            field.setAccessible(true);
            clock = (TextureTransform) field.get(null);
            clockSupplier = TextureTransform.class.getDeclaredField("supplier");
            clockSupplier.setAccessible(true);
            originalClock = clockSupplier.get(clock);
            clockSupplier.set(clock, (Supplier<Matrix4f>) () -> new Matrix4f().translation(12.5F, 0, 0));
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void restoreClock() {
        try {
            clockSupplier.set(clock, originalClock);
        } catch (IllegalAccessException error) {
            throw new IllegalStateException(error);
        }
    }

    private static final class ItemScreen extends Screen {
        private final List<ItemStack> items;

        private ItemScreen(List<ItemStack> items) {
            super(Component.literal("Structure disk scan parity"));
            this.items = items;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xff252525);
            graphics.text(this.font, this.title, 24, 24, -1, false);
            String[] labels = {"Empty", "Diamond", "Flipped anvil", "Conversion", "Mismatch"};
            for (int i = 0; i < this.items.size(); i++) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(24 + i * 112, 64);
                if (stage == 1) graphics.pose().rotate(0.4F);
                graphics.pose().scale(4);
                graphics.item(this.items.get(i), 0, 0);
                graphics.pose().popMatrix();
                graphics.text(this.font, labels[i], 24 + i * 112, 145, -1, false);
                graphics.item(this.items.get(i), 24 + i * 112, 176);
            }
        }
    }
}
