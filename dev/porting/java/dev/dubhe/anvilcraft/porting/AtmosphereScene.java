package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.util.Atmosphere;
import dev.dubhe.anvilcraft.util.AtmosphereManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class AtmosphereScene {
    private static int stage;
    private static long next;
    private static long deadline;
    private static boolean capturing;
    private static volatile Throwable failure;

    public static void frame(Minecraft client) {
        if (deadline == 0) {
            client.screen.onClose();
            deadline = System.currentTimeMillis() + 120000;
        }
        if (failure != null) throw new IllegalStateException("大气客户端验证失败", failure);
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("大气客户端验证超时：" + stage);
        if (capturing || System.currentTimeMillis() < next) return;
        client.getToastManager().clear();
        switch (stage) {
            case 0 -> {
                server(client, () -> {
                    var server = client.getSingleplayerServer();
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.SURVIVAL);
                    player.getAbilities().invulnerable = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                    player.setNoGravity(true);
                    player.removeAllEffects();
                    player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                    player.setAirSupply(300);
                    AtmosphereManager.registerDimensionAtmosphere(Level.OVERWORLD, Atmosphere.VACUUM);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 20.5 81 12.5 180 0");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                });
                advance(1);
            }
            case 1 -> {
                if (client.player.getAirSupply() >= 250) return;
                require(client.player.getAirSupply() > 0 && !client.player.isEyeInFluid(FluidTags.WATER), "真空空气应同步显示气息消耗");
                capture(client, "vacuum", 2);
            }
            case 2 -> {
                server(client, () -> client.getSingleplayerServer().getPlayerList().getPlayers().getFirst()
                    .setItemSlot(EquipmentSlot.HEAD, ModItems.BREATHING_HELMET.asStack()));
                advance(3);
            }
            case 3 -> {
                if (client.player.getAirSupply() != 300
                    || !client.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BREATHING_HELMET)) return;
                capture(client, "helmet", 4);
            }
            case 4 -> {
                server(client, () -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                    player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 1200));
                });
                advance(5);
            }
            case 5 -> {
                if (client.player.getAirSupply() >= 250) return;
                require(client.player.hasEffect(MobEffects.WATER_BREATHING), "水下呼吸药水在真空空气中不能供氧");
                server(client, () -> {
                    var level = client.getSingleplayerServer().overworld();
                    for (int x = 18; x <= 22; x++) {
                        for (int z = 10; z <= 14; z++) {
                            for (int y = 80; y <= 84; y++) {
                                level.setBlock(new BlockPos(x, y, z), Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
                            }
                        }
                    }
                });
                advance(6);
            }
            case 6 -> {
                if (!client.player.isEyeInFluid(FluidTags.WATER)) return;
                require(!AtmosphereManager.isSuffocating(client.player), "真空水体内的水下呼吸药水应允许呼吸");
                capture(client, "water-breathing", 7);
            }
            case 7 -> {
                server(client, () -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    player.setAirSupply(300);
                    player.removeEffect(MobEffects.WATER_BREATHING);
                });
                advance(8);
            }
            case 8 -> {
                if (client.player.getAirSupply() >= 250) return;
                require(!client.player.hasEffect(MobEffects.WATER_BREATHING)
                    && AtmosphereManager.isSuffocating(client.player), "移除水下呼吸后应恢复实际溺水耗气");
                AtmosphereManager.registerDimensionAtmosphere(Level.OVERWORLD, Atmosphere.OVERWORLD);
                AnvilCraft.LOGGER.info("PORT_ATMOSPHERE_PASSED: vacuum air sync, helmet refill, potion in air and water, drowning");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown atmosphere stage " + stage);
        }
    }

    private static void server(Minecraft client, Runnable action) {
        client.getSingleplayerServer().execute(() -> {
            try {
                action.run();
            } catch (Throwable error) {
                failure = error;
            }
        });
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int value) {
        AnvilCraft.LOGGER.info("PORT_ATMOSPHERE_STAGE: {} -> {}", stage, value);
        stage = value;
        next = System.currentTimeMillis() + 1500;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "atmosphere-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
