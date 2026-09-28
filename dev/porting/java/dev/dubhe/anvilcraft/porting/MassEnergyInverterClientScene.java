package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CreativeGeneratorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.MassEnergyInverterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.SpaceOvercompressorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class MassEnergyInverterClientScene {
    private static final BlockPos POWER = new BlockPos(0, 155, 4);
    private static int stage;
    private static long deadline;
    private static long next;
    private static volatile boolean ready;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 120000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Mass energy scene " + stage);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        if (stage == 0) {
            client.getSingleplayerServer().execute(() -> {
                var server = client.getSingleplayerServer();
                var level = server.overworld();
                level.setBlockAndUpdate(POWER, ModBlocks.MASS_ENERGY_INVERTER.getDefaultState());
                level.setBlockAndUpdate(POWER.above(), ModBlocks.CREATIVE_GENERATOR.getDefaultState());
                ((CreativeGeneratorBlockEntity) level.getBlockEntity(POWER.above())).setPower(8192);
                for (var direction : Direction.Plane.HORIZONTAL) {
                    level.setBlockAndUpdate(POWER.relative(direction), ModBlocks.SPACE_OVERCOMPRESSOR.getDefaultState());
                }
                for (int x = -7; x <= 8; x++) {
                    for (int y = 161; y <= 167; y++) {
                        level.setBlock(new BlockPos(x, y, -2), Blocks.BLACK_CONCRETE.defaultBlockState(), Block.UPDATE_ALL);
                    }
                    for (int z = -2; z <= 2; z++) {
                        level.setBlock(new BlockPos(x, 161, z), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
                for (int index = 0; index < 3; index++) {
                    var pos = new BlockPos(-4 + index * 3, 162, 0);
                    level.setBlockAndUpdate(pos, ModBlocks.WIP_BLOCK.getDefaultState());
                    var wip = (WipBlockEntity) level.getBlockEntity(pos);
                    wip.setInitialBlock(ModBlocks.LASER_RECEIVER.getDefaultState());
                    wip.setRecipeId(AnvilCraft.of("procedural_process/mass_energy_inverter_mass_first"));
                    wip.setStepCount(1 + index * 3);
                    wip.setChanged();
                    level.sendBlockUpdated(pos, wip.getBlockState(), wip.getBlockState(), Block.UPDATE_ALL);
                }
                level.setBlockAndUpdate(new BlockPos(5, 162, 0), ModBlocks.MASS_ENERGY_INVERTER.getDefaultState());
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode spectator @a");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 1 164 10 180 14");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
            });
            client.options.hideGui = true;
            client.options.fov().set(60);
            client.options.guiScale().set(2);
            client.resizeGui();
            client.setScreen(null);
            stage = 1;
            next = System.currentTimeMillis() + 3000;
            return;
        }
        if (stage == 1) {
            if (!ready) {
                client.getSingleplayerServer().execute(() -> {
                    var level = client.getSingleplayerServer().overworld();
                    var inverter = (MassEnergyInverterBlockEntity) level.getBlockEntity(POWER);
                    if (inverter == null || !inverter.isGridWorking() || inverter.getInputPower() != 1024) return;
                    for (var direction : Direction.Plane.HORIZONTAL) {
                        if (((SpaceOvercompressorBlockEntity) level.getBlockEntity(POWER.relative(direction))).getStoredMass() < 50) return;
                    }
                    ready = true;
                });
                next = System.currentTimeMillis() + 200;
                return;
            }
            stage = 2;
            next = System.currentTimeMillis() + 2500;
            return;
        }
        if (stage == 2) {
            capture(client, "world", 3);
            return;
        }
        if (stage == 3) {
            client.setScreen(new Preview());
            stage = 4;
            next = System.currentTimeMillis() + 1000;
            return;
        }
        if (stage == 4) {
            capture(client, "item", 5);
            return;
        }
        if (stage == 5) {
            AnvilCraft.LOGGER.info("PORT_MASS_ENERGY_CLIENT_PASSED: real power grid, 1024 kW demand, "
                + "four compressors, WIP stages and item");
            client.stop();
            stage = 6;
        }
    }

    private static void capture(Minecraft client, String name, int target) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "mass-energy-inverter-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                stage = target;
            }));
    }

    private static final class Preview extends Screen {
        private Preview() {
            super(Component.literal("Mass-energy inverter"));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xFF303030);
            graphics.pose().pushMatrix();
            graphics.pose().translate(this.width / 2.0F - 32, this.height / 2.0F - 32);
            graphics.pose().scale(4, 4);
            graphics.item(ModBlocks.MASS_ENERGY_INVERTER.asStack(), 0, 0);
            graphics.pose().popMatrix();
        }
    }
}
