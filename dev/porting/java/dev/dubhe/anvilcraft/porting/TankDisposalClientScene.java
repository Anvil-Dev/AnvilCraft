package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.JadeIds;
import snownee.jade.impl.WailaClientRegistration;

public final class TankDisposalClientScene {
    private static final BlockPos POS = new BlockPos(0, 162, 0);
    private static int stage;
    private static long next;
    private static long deadline;
    private static String jadeName = "";
    private static boolean capturing;
    private static long captureReady;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Tank disposal stage " + stage + " Jade=" + jadeName);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                advance(1);
                WailaClientRegistration.instance().addTooltipCollectedCallback((box, accessor) -> {
                    if (accessor instanceof BlockAccessor block && block.getPosition().equals(POS)) {
                        jadeName = box.getTooltip().getString(JadeIds.CORE_OBJECT_NAME);
                    }
                });
                WailaClientRegistration.instance().tooltipCollectedCallback.sort();
                client.options.guiScale().set(2);
                client.resizeGui();
                client.setScreen(null);
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    level.setBlockAndUpdate(POS, ModBlocks.FLUID_TANK.getDefaultState());
                    fill((FluidTankBlockEntity) level.getBlockEntity(POS));
                    level.setBlockAndUpdate(POS.south(4).below(), Blocks.STONE.defaultBlockState());
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 162 4.5 180 16");
                    server.getPlayerList().getPlayers().forEach(player -> {
                        player.setGameMode(GameType.SURVIVAL);
                        player.setNoGravity(true);
                        player.getInventory().clearContent();
                        player.containerMenu.broadcastChanges();
                    });
                });
            }
            case 1 -> {
                if (!jadeName.equals("Fluid Tank")) return;
                capture(client, "normal", 2);
            }
            case 2 -> {
                advance(3);
                jadeName = "";
                client.getSingleplayerServer().execute(() -> {
                    client.getSingleplayerServer().overworld().setBlockAndUpdate(POS.north(), ModBlocks.MENGER_SPONGE.getDefaultState());
                    giveBucket(client);
                });
            }
            case 3 -> {
                if (!jadeName.equals("Overflow Disposal Fluid Tank") || !client.player.getMainHandItem().is(Items.WATER_BUCKET)) return;
                advance(4);
                use(client);
            }
            case 4 -> {
                if (!client.player.getMainHandItem().is(Items.BUCKET)) return;
                checkAmount(client);
                capture(client, "disposal", 5);
            }
            case 5 -> {
                advance(6);
                jadeName = "";
                client.getSingleplayerServer().execute(() -> {
                    client.getSingleplayerServer().overworld().setBlockAndUpdate(POS.north(), Blocks.AIR.defaultBlockState());
                    giveBucket(client);
                });
            }
            case 6 -> {
                if (!jadeName.equals("Fluid Tank") || !client.player.getMainHandItem().is(Items.WATER_BUCKET)) return;
                advance(7);
                use(client);
            }
            case 7 -> {
                if (!client.player.getMainHandItem().is(Items.WATER_BUCKET)) {
                    throw new IllegalStateException("Full normal tank consumed input");
                }
                checkAmount(client);
                capture(client, "restored", 8);
            }
            case 8 -> {
                stage = 9;
                AnvilCraft.LOGGER.info("PORT_TANK_DISPOSAL_CLIENT_PASSED: Jade name toggles, "
                    + "full-tank bucket accepted only in disposal mode");
                client.stop();
            }
            default -> {
            }
        }
    }

    private static void fill(FluidTankBlockEntity tank) {
        try (Transaction tx = Transaction.openRoot()) {
            tank.getFluidHandler().insert(FluidResource.of(Fluids.WATER), 16000, tx);
            tx.commit();
        }
    }

    private static void checkAmount(Minecraft client) {
        var tank = (FluidTankBlockEntity) client.level.getBlockEntity(POS);
        if (tank.getFluidHandler().getAmountAsInt(0) != 16000) throw new IllegalStateException("Tank amount changed");
    }

    private static void giveBucket(Minecraft client) {
        client.getSingleplayerServer().getPlayerList().getPlayers().forEach(player -> {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            player.containerMenu.broadcastChanges();
        });
    }

    private static void use(Minecraft client) {
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(POS), Direction.SOUTH, POS, false));
    }

    private static void capture(Minecraft client, String name, int target) {
        if (!client.levelRenderer.isSectionCompiledAndVisible(POS)) {
            captureReady = 0;
            return;
        }
        if (captureReady == 0) captureReady = System.currentTimeMillis() + 2000;
        if (System.currentTimeMillis() < captureReady) return;
        captureReady = 0;
        capturing = true;
        Screenshot.grab(client.gameDirectory, "tank-disposal-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(target);
            }));
    }

    private static void advance(int target) {
        stage = target;
        next = System.currentTimeMillis() + 2000;
    }
}
