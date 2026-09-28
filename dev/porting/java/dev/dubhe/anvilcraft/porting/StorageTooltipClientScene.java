package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.tooltip.ItemTooltipManager;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.inventory.tooltip.StorageTooltip;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class StorageTooltipClientScene {
    private static final Item[] ICONS = {Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT, Items.IRON_INGOT,
        Items.COPPER_INGOT, Items.REDSTONE, Items.LAPIS_LAZULI, Items.QUARTZ, Items.AMETHYST_SHARD, Items.COAL, Items.STICK, Items.APPLE};
    private static final UUID[] IDS = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
    private static volatile boolean prepared;
    private static int stage;
    private static int selected;
    private static long next;
    private static long deadline;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 120000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Storage tooltip stage " + stage);
        if (client.getOverlay() != null || capturing || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> prepare(client));
                client.options.guiScale().set(2);
                client.resizeGui();
                advance(1);
            }
            case 1 -> {
                if (!prepared) return;
                for (int index = 0; index < 4; index++) {
                    var tooltip = client.player.getInventory().getItem(index).getTooltipImage().orElse(null);
                    if (!(tooltip instanceof StorageTooltip data)) return;
                    int expected = index == 0 ? 0 : index == 2 ? 12 : 3;
                    int limit = index == 3 ? 0 : index == 2 ? 16384 : 1024;
                    if (data.usedTypes() != expected || data.typeLimit() != limit || data.types().size() != Math.min(9, expected)) {
                        throw new IllegalStateException("Tooltip RPC contents " + index);
                    }
                    var renderer = ClientTooltipComponent.create(data);
                    if (renderer.getHeight(client.font) != (index == 0 ? 20 : 36)) throw new IllegalStateException("Tooltip height");
                }
                client.setScreen(new Preview());
                advance(2);
            }
            case 2 -> capture(client, "empty", 3);
            case 3 -> {
                selected = 1;
                advance(4);
            }
            case 4 -> capture(client, "finite", 5);
            case 5 -> {
                selected = 2;
                advance(6);
            }
            case 6 -> capture(client, "many", 7);
            case 7 -> {
                selected = 3;
                advance(8);
            }
            case 8 -> capture(client, "infinite", 9);
            case 9 -> {
                selected = 1;
                client.getSingleplayerServer().execute(() -> stock(Storages.get().get(IDS[1]).orElseThrow(), Items.IRON_INGOT, 20));
                advance(10);
            }
            case 10 -> {
                var tooltip = client.player.getInventory().getItem(1).getTooltipImage().orElseThrow();
                if (((StorageTooltip) tooltip).usedTypes() != 4) return;
                capture(client, "updated", 11);
            }
            case 11 -> {
                client.setScreen(null);
                ItemTooltipManager.clearStorageTooltips();
                client.player.getInventory().getItem(1).getTooltipImage();
                ItemTooltipManager.clearStorageTooltips();
                advance(12);
            }
            case 12 -> {
                try {
                    var field = ItemTooltipManager.class.getDeclaredField("STORAGE_USAGE");
                    field.setAccessible(true);
                    if (!((Map<?, ?>) field.get(null)).isEmpty()) {
                        throw new IllegalStateException("Late response survived cache invalidation");
                    }
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException(exception);
                }
                client.options.hideGui = true;
                client.setScreen(new ScaledPreview());
                advance(13);
            }
            case 13 -> capture(client, "scaled", 14);
            case 14 -> {
                AnvilCraft.LOGGER.info("PORT_STORAGE_TOOLTIP_CLIENT_PASSED: "
                    + "live RPC, finite/infinite, nine icons, refresh, invalidation and simultaneous oversized densities");
                stage = 15;
                client.stop();
            }
            default -> {
            }
        }
    }

    private static void prepare(Minecraft client) {
        var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
        for (int index = 0; index < 4; index++) {
            BaseStorage<?> storage;
            ItemStack stack;
            if (index == 3) {
                storage = Storages.get().getOrCreate(IDS[index], HyperdimensionStorage.class);
                stack = ModBlocks.HYPERDIMENSION_STORAGE_STATION.asStack();
            } else {
                var finite = Storages.get().getOrCreate(IDS[index], ShulkerContainerStorage.class);
                if (index == 2) finite.getItems().addTypeLimit(limit -> 16384);
                storage = finite;
                stack = ModBlocks.SHULKER_CONTAINER.asStack();
            }
            stack.set(ModComponents.STORAGE, new StorageRef(stack.get(ModComponents.STORAGE).type(), IDS[index]));
            int types = index == 0 ? 0 : index == 2 ? 12 : 3;
            for (int item = 0; item < types; item++) stock(storage, ICONS[item], 100);
            player.getInventory().setItem(index, stack);
        }
        player.inventoryMenu.broadcastChanges();
        prepared = true;
    }

    private static void stock(BaseStorage<?> storage, Item item, int count) {
        try (var transaction = Transaction.openRoot()) {
            if (storage.getItems().insert(ItemResource.of(item), count, transaction) != count) {
                throw new IllegalStateException("Fixture stock");
            }
            transaction.commit();
        }
    }

    private static void advance(int target) {
        stage = target;
        next = System.currentTimeMillis() + 2200;
    }

    private static void capture(Minecraft client, String name, int target) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "storage-tooltip-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(target);
            }));
    }

    private static final class Preview extends Screen {
        private Preview() {
            super(Component.literal("Storage Tooltip"));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xFF303030);
            var stack = Minecraft.getInstance().player.getInventory().getItem(selected);
            graphics.setTooltipForNextFrame(this.font, List.of(stack.getHoverName()), stack.getTooltipImage(), stack, 240, 160);
        }
    }

    private static final class ScaledPreview extends Screen {
        private ScaledPreview() {
            super(Component.literal("Oversized item density regression"));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xFF303030);
            var stack = Minecraft.getInstance().player.getInventory().getItem(1);
            float[] scales = {1, 1.1F, 1.5F, 2};
            for (int index = 0; index < scales.length; index++) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(100 + index * 100, 160);
                graphics.pose().scale(scales[index], scales[index]);
                graphics.item(stack, 0, 0);
                graphics.pose().popMatrix();
            }
        }
    }
}
