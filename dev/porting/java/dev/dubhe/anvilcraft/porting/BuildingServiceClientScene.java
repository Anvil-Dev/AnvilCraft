package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import dev.dubhe.anvilcraft.network.BuildingRodPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;

public final class BuildingServiceClientScene {
    private static final BlockPos FIRST = new BlockPos(8, 81, 8);
    private static int stage;
    private static long next;
    private static long deadline;
    private static boolean capturing;
    private static volatile boolean prepared;
    private static volatile Throwable failure;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 90000;
        if (failure != null || System.currentTimeMillis() > deadline) {
            throw new IllegalStateException("Service client stage " + stage, failure);
        }
        if (capturing || System.currentTimeMillis() < next) return;
        client.options.hideGui = false;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                        player.setGameMode(GameType.SURVIVAL);
                        player.setNoGravity(true);
                        player.setPos(9.5, 82, 12);
                        player.getInventory().clearContent();
                        var rod = ModItems.BUILDING_ROD.asStack();
                        rod.set(ModComponents.STORED_ENERGY, new StoredEnergy(10000));
                        player.setItemInHand(InteractionHand.MAIN_HAND, rod);
                        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE, 6));
                        player.inventoryMenu.broadcastChanges();
                        prepared = true;
                    } catch (Throwable error) {
                        failure = error;
                    }
                });
                advance(1);
            }
            case 1 -> {
                if (!prepared || !client.player.getMainHandItem().is(ModItems.BUILDING_ROD)
                    || client.player.getOffhandItem().getCount() != 6) return;
                client.player.setPos(9.5, 82, 12);
                client.player.setYRot(180);
                client.player.setXRot(30);
                client.getConnection().send(packet(BuildingRodPacket.Action.START));
                client.getConnection().send(packet(BuildingRodPacket.Action.PLACE));
                advance(2);
            }
            case 2 -> {
                if (!placed(client) || client.player.getOffhandItem().getCount() != 3 || energy(client) != 9700) return;
                capture(client, "placed", 3);
            }
            case 3 -> {
                client.getConnection().send(packet(BuildingRodPacket.Action.UNDO));
                advance(4);
            }
            case 4 -> {
                for (int x = 0; x < 3; x++) {
                    if (!client.level.getBlockState(FIRST.east(x)).isAir()) return;
                }
                int count = 0;
                for (var stack : dev.dubhe.anvilcraft.inventory.PocketInventory.carriedItems(client.player)) {
                    if (stack.is(Items.STONE)) count += stack.getCount();
                }
                if (count != 6 || energy(client) != 9700) return;
                capture(client, "undone", 5);
            }
            case 5 -> {
                AnvilCraft.LOGGER.info("PORT_BUILDING_SERVICE_PASSED: real start/place/undo packets, world, materials, energy sync");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown service scene stage");
        }
    }

    private static boolean placed(Minecraft client) {
        for (int x = 0; x < 3; x++) {
            if (!client.level.getBlockState(FIRST.east(x)).is(Blocks.STONE)) return false;
        }
        return true;
    }

    private static int energy(Minecraft client) {
        return client.player.getMainHandItem().getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
    }

    private static BuildingRodPacket packet(BuildingRodPacket.Action action) {
        return new BuildingRodPacket(FIRST, FIRST.east(2), Direction.UP, false, Rotation.NONE, Mirror.NONE, false, null, action, 42);
    }

    private static void advance(int target) {
        stage = target;
        next = System.currentTimeMillis() + 1000;
    }

    private static void capture(Minecraft client, String name, int target) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "building-service-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(target);
            }));
    }
}
