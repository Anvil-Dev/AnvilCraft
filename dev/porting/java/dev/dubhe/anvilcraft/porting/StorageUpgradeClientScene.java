package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.tooltip.impl.ShulkerContainerTooltipProvider;
import dev.dubhe.anvilcraft.block.entity.storage.HyperdimensionStorageStationBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.ShulkerContainerBlockEntity;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.event.anvil.AnvilEventListener;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.UUID;

public final class StorageUpgradeClientScene {
    private static final BlockPos POS = new BlockPos(0, 162, 0);
    private static final ShulkerContainerTooltipProvider TOOLTIP = new ShulkerContainerTooltipProvider();
    private static int stage;
    private static long deadline;
    private static long next;
    private static boolean capturing;
    private static volatile boolean prepared;
    private static volatile boolean upgraded;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 120000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Storage upgrade stage " + stage);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> prepare(client));
                client.options.hideGui = false;
                client.options.guiScale().set(2);
                client.resizeGui();
                client.setScreen(null);
                advance(1);
            }
            case 1 -> {
                if (!prepared || !(client.level.getBlockEntity(POS) instanceof ShulkerContainerBlockEntity entity)) return;
                var lines = TOOLTIP.tooltip(entity);
                if (lines.size() != 4 || !lines.getFirst().getString().contains("Singularity")) return;
                AnvilCraft.LOGGER.info("PORT_STORAGE_UPGRADE_HINT_PASSED: {}", lines);
                capture(client, "before", 2);
            }
            case 2 -> {
                client.getSingleplayerServer().execute(() -> upgrade(client));
                advance(3);
            }
            case 3 -> {
                if (!upgraded || !(client.level.getBlockEntity(POS) instanceof HyperdimensionStorageStationBlockEntity)) return;
                capture(client, "after", 4);
            }
            case 4 -> {
                advance(5);
                StorageScreen.openScreen(POS);
            }
            case 5 -> {
                if (!(client.screen instanceof StorageScreen screen) || !screen.canTransferRecipe()) return;
                var materials = screen.getTransferMaterials();
                if (materials.getOrDefault(ItemResource.of(Items.DIAMOND), 0L) != 50000
                    || materials.getOrDefault(ItemResource.of(Items.EMERALD), 0L) != 3) return;
                capture(client, "contents", 6);
            }
            case 6 -> {
                AnvilCraft.LOGGER.info("PORT_STORAGE_UPGRADE_CLIENT_PASSED: full upgrade hint, multipart replacement, "
                    + "50000 diamonds and crafting unlock/materials over RPC");
                client.stop();
                stage = 7;
            }
            default -> {
            }
        }
    }

    private static void prepare(Minecraft client) {
        var server = client.getSingleplayerServer();
        var level = server.overworld();
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 6; z++) level.setBlockAndUpdate(new BlockPos(x, 161, z), Blocks.SMOOTH_STONE.defaultBlockState());
        }
        var block = ModBlocks.SHULKER_CONTAINER.get();
        var state = block.defaultBlockState();
        for (var part : block.getParts()) {
            level.setBlock(POS.offset(block.offsetFrom(state, part)),
                block.placedState(part, state), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        var entity = (ShulkerContainerBlockEntity) level.getBlockEntity(POS);
        entity.setId(UUID.randomUUID());
        var storage = Storages.get().getOrCreate(entity.getId(), ShulkerContainerStorage.class);
        storage.getItems().addTypeLimit(value -> 16384);
        storage.getItems().addSpaceSize(value -> 1048576);
        try (var transaction = Transaction.openRoot()) {
            storage.getItems().insert(ItemResource.of(Items.DIAMOND), 50000, transaction);
            storage.getItems().insert(ItemResource.of(Items.CRAFTING_TABLE), 1, transaction);
            storage.getItems().insert(ItemResource.of(Items.STONECUTTER), 1, transaction);
            transaction.commit();
        }
        if (!storage.unlockCrafting()) throw new IllegalStateException("Fixture crafting unlock");
        storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(0, new ItemStack(Items.EMERALD, 3)));
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode creative @a");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 162 5.5 180 0");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
        server.getPlayerList().getPlayers().forEach(player -> {
            player.setNoGravity(true);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ModItems.ANVIL_HAMMER.asStack());
        });
        prepared = true;
    }

    private static void upgrade(Minecraft client) {
        var level = client.getSingleplayerServer().overworld();
        var oldId = ((ShulkerContainerBlockEntity) level.getBlockEntity(POS)).getId();
        var pos = POS.above(3);
        for (var stack : new ItemStack[]{ModBlocks.SINGULARITY_CRYSTAL.asStack(), ModBlocks.HYPERCUBE.asStack(16)}) {
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
        }
        var anvil = new FallingBlockEntity(level, pos.getX(), pos.getY(), pos.getZ(), Blocks.ANVIL.defaultBlockState());
        AnvilEventListener.onLand(new AnvilEvent.OnLand(level, pos, anvil, 1));
        var station = (HyperdimensionStorageStationBlockEntity) level.getBlockEntity(POS);
        var storage = Storages.get().get(station.getId(), HyperdimensionStorage.class).orElseThrow();
        if (Storages.get().get(oldId).isPresent() || !storage.isCraftingUnlocked()) throw new IllegalStateException("Upgrade transfer");
        upgraded = true;
    }

    private static void advance(int target) {
        stage = target;
        next = System.currentTimeMillis() + 2000;
    }

    private static void capture(Minecraft client, String name, int target) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "storage-upgrade-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(target);
            }));
    }
}
