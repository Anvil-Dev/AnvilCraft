package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.building.BlueprintPlacement;
import dev.dubhe.anvilcraft.building.BuildingEntityTransform;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.client.building.BuildingRodClient;
import dev.dubhe.anvilcraft.client.building.BuildingRodItemRenderer;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class BuildingRodVisualParityScene {
    private static final String VERSION = "26.1";
    private static final String[] NAMES = {"empty", "stone", "slab", "torch", "pole", "cfa", "disk",
        "off-stone", "off-pole", "off-cfa", "off-empty", "unpowered", "bed", "chest", "shulker", "head", "blueprint", "blueprint-rotated"};
    private static boolean creating;
    private static volatile boolean prepared;
    private static volatile int supplied = -1;
    private static volatile Throwable failure;
    private static int stage;
    private static int phase;
    private static long next;
    private static long deadline;
    private static boolean capturing;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void freezeMotion(RenderFrameEvent.Pre event) {
        if (!Boolean.getBoolean("anvilcraft.portBuildingVisualParityScene") || !prepared) return;
        Object motion = read(BuildingRodItemRenderer.class, "motion");
        set(motion, "angle", 35.0);
        set(motion, "lastTime", Double.NaN);
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) {
        if (!Boolean.getBoolean("anvilcraft.portBuildingVisualParityScene")) return;
        var client = Minecraft.getInstance();
        client.options.pauseOnLostFocus = false;
        AnvilCraft.CLIENT_CONFIG.buildingRodControls = dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.BuildingRodControls.OPTIMIZED;
        client.options.fov().set(70);
        client.options.bobView().set(false);
        client.options.guiScale().set(2);
        client.options.hideGui = false;
        if (!creating) {
            if (client.screen == null || client.getOverlay() != null) return;
            creating = true;
            String name = "building-parity-" + System.currentTimeMillis();
            client.createWorldOpenFlows().createFreshLevel(name,
                new LevelSettings(name, GameType.CREATIVE,
                    new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT),
                new WorldOptions(121261L, false, false), WorldPresets::createFlatWorldDimensions, client.screen);
            return;
        }
        if (client.level == null || client.player == null || client.getSingleplayerServer() == null) return;
        if (deadline == 0) {
            deadline = System.currentTimeMillis() + 150000;
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    var player = server.getPlayerList().getPlayers().getFirst();
                    for (int x = 187; x <= 213; x++) {
                        for (int z = 185; z <= 215; z++) {
                            level.setBlock(new BlockPos(x, 100, z), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                    player.setNoGravity(true);
                    player.setPos(200.5, 130, 207);
                    player.getInventory().clearContent();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick freeze");
                    prepared = true;
                } catch (Throwable error) {
                    failure = error;
                }
            });
        }
        if (failure != null || System.currentTimeMillis() > deadline) {
            throw new IllegalStateException("Parity stage " + stage, failure);
        }
        if (!prepared || capturing || System.currentTimeMillis() < next) return;
        if (client.screen != null) client.setScreen(null);
        client.gui.getChat().clearMessages(false);
        client.getToastManager().clear();
        client.player.setNoGravity(true);
        client.player.setPos(200.5, stage < 16 ? 130 : 105, 207);
        client.player.setYRot(180);
        client.player.setXRot(stage < 16 ? 0 : 30);
        if (phase == 0) {
            BuildingRodClient.cancel();
            int current = stage;
            client.getSingleplayerServer().execute(() -> {
                var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.setPos(200.5, current < 16 ? 130 : 105, 207);
                var rod = ModItems.BUILDING_ROD.asStack();
                rod.set(ModComponents.STORED_ENERGY, new StoredEnergy(current == 11 ? 0 : 10000));
                boolean off = current >= 7 && current <= 10;
                player.setItemInHand(off ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, rod);
                player.setItemInHand(off ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, payload(current));
                player.inventoryMenu.broadcastChanges();
                supplied = current;
            });
            phase = 1;
            next = System.currentTimeMillis() + 1200;
            return;
        }
        if (supplied != stage) return;
        if (phase == 1) {
            ((Map<?, ?>) read(BuildingRodItemRenderer.class, "PAYLOAD_POSES")).clear();
            if (stage >= 16) prepareProjection(client);
            phase = 2;
            next = System.currentTimeMillis() + 1200;
            return;
        }
        AnvilCraft.LOGGER.info("PORT_ROD_PARITY {} {}: {}", VERSION, NAMES[stage],
            currentFit());
        capturing = true;
        Screenshot.grab(client.gameDirectory, "rod-parity-" + VERSION + "-" + NAMES[stage] + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                phase = 0;
                stage++;
                next = System.currentTimeMillis() + 400;
                if (stage == NAMES.length) {
                    AnvilCraft.LOGGER.info("PORT_ROD_PARITY_PASSED {}", VERSION);
                    client.stop();
                }
            }));
    }

    private static ItemStack payload(int index) {
        return switch (index) {
            case 1, 7, 11 -> new ItemStack(Items.STONE);
            case 2 -> new ItemStack(Items.STONE_SLAB);
            case 3 -> new ItemStack(Items.TORCH);
            case 4, 8 -> ModBlocks.TRANSMISSION_POLE.asStack();
            case 5, 9 -> ModBlocks.CELESTIAL_FORGING_ANVIL.asStack();
            case 6 -> ModItems.STRUCTURE_DISK.asStack();
            case 12 -> new ItemStack(Items.RED_BED);
            case 13 -> new ItemStack(Items.CHEST);
            case 14 -> new ItemStack(Items.SHULKER_BOX);
            case 15 -> new ItemStack(Items.SKELETON_SKULL);
            case 16, 17 -> {
                var disk = ModItems.STRUCTURE_DISK.asStack();
                disk.set(ModComponents.STRUCTURE_DISK_DATA, new StructureDiskData("parity_00000000-0000-0000-0000-000000000001.nbt",
                    "Parity", new UUID(0, 1), Direction.NORTH, 3, 3, 2, false));
                yield disk;
            }
            default -> ItemStack.EMPTY;
        };
    }

    private static void prepareProjection(Minecraft client) {
        var positions = List.of(new BlockPos(0, 0, 0), new BlockPos(1, 0, 0), new BlockPos(2, 0, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 1, 0));
        final var palette = List.of(Blocks.STONE.defaultBlockState(), Blocks.GLASS.defaultBlockState(), Blocks.CHEST.defaultBlockState(),
            Blocks.WATER.defaultBlockState(), Blocks.TORCH.defaultBlockState());
        var entries = new java.util.ArrayList<StructureSnapshot.BlockEntry>();
        for (int i = 0; i < positions.size(); i++) entries.add(new StructureSnapshot.BlockEntry(positions.get(i), i, Optional.empty()));
        var nbt = new CompoundTag();
        nbt.putString("id", "minecraft:armor_stand");
        nbt = BuildingEntityTransform.withWorldPos(nbt, new Vec3(2.5, 1, 0.5));
        final var entity = new StructureSnapshot.EntityEntry(new Vec3(2.5, 1, 0.5), new BlockPos(2, 1, 0), nbt);
        set(BuildingRodClient.class, "sessionLevel", client.level);
        set(BuildingRodClient.class, "disk", client.player.getOffhandItem().get(ModComponents.STRUCTURE_DISK_DATA));
        set(BuildingRodClient.class, "selected", client.player.getOffhandItem().copy());
        set(BuildingRodClient.class, "snapshot", new StructureSnapshot(new Vec3i(3, 3, 2), palette, entries, List.of(entity)));
        set(BuildingRodClient.class, "locked", true);
        set(BuildingRodClient.class, "placement", new BlueprintPlacement(new BlockPos(199, 101, 198),
            stage == 16 ? Rotation.NONE : Rotation.CLOCKWISE_90, stage == 16 ? Mirror.NONE : Mirror.FRONT_BACK));
    }

    private static Object currentFit() {
        try {
            Object state = read(BuildingRodItemRenderer.class, "FIRST_PERSON_STATE");
            return (Boolean) read(state, "hasPayload") ? read(state, "payloadPose") : List.of();
        } catch (IllegalStateException legacyRenderer) {
            return ((Map<?, ?>) read(BuildingRodItemRenderer.class, "PAYLOAD_POSES")).values();
        }
    }

    private static Object read(Object owner, String name) {
        try {
            var type = owner instanceof Class<?> cls ? cls : owner.getClass();
            var field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner instanceof Class<?> ? null : owner);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void set(Object owner, String name, Object value) {
        try {
            var type = owner instanceof Class<?> cls ? cls : owner.getClass();
            var field = type.getDeclaredField(name);
            field.setAccessible(true);
            field.set(owner instanceof Class<?> ? null : owner, value);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }
}
