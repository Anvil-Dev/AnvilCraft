package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CreativeGeneratorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PowerConverterBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

public final class PowerConverterClientScene {
    private static final BlockPos CENTER = new BlockPos(0, 161, 4);
    private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP};
    private static int stage;
    private static long deadline;
    private static long next;
    private static volatile boolean ready;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 120000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Power converter scene " + stage);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        if (stage == 0) {
            client.getSingleplayerServer().execute(() -> {
                var server = client.getSingleplayerServer();
                var level = server.overworld();
                level.setBlockAndUpdate(CENTER, ModBlocks.CREATIVE_GENERATOR.getDefaultState());
                ((CreativeGeneratorBlockEntity) level.getBlockEntity(CENTER)).setPower(65536);
                level.setBlockAndUpdate(CENTER.below(), ModBlocks.CREATIVE_GENERATOR.getDefaultState());
                ((CreativeGeneratorBlockEntity) level.getBlockEntity(CENTER.below())).setPower(65536);
                var blocks = List.<Block>of(ModBlocks.POWER_CONVERTER_SMALL.get(), ModBlocks.POWER_CONVERTER_MIDDLE.get(),
                    ModBlocks.POWER_CONVERTER_BIG.get(), ModBlocks.POWER_CONVERTER_SUPER_BIG.get(),
                    ModBlocks.POWER_CONVERTER_EXTREMELY_BIG.get());
                for (int index = 0; index < blocks.size(); index++) {
                    level.setBlockAndUpdate(CENTER.relative(SIDES[index]), blocks.get(index).defaultBlockState());
                }
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
            });
            stage = 1;
            return;
        }
        if (stage == 1) {
            if (!ready) {
                client.getSingleplayerServer().execute(() -> {
                    var level = client.getSingleplayerServer().overworld();
                    for (Direction side : SIDES) {
                        if (!(level.getBlockEntity(CENTER.relative(side)) instanceof PowerConverterBlockEntity converter)
                            || converter.getGrid() == null || !converter.getGrid().isWorking() || converter.getEnergyStored() <= 0) return;
                    }
                    ready = true;
                });
                next = System.currentTimeMillis() + 200;
                return;
            }
            PowerConverterModelProbe.verify(client);
            client.options.guiScale().set(2);
            client.resizeGui();
            client.setScreen(new Preview());
            stage = 2;
            next = System.currentTimeMillis() + 1000;
            return;
        }
        if (stage == 2) {
            capturing = true;
            Screenshot.grab(client.gameDirectory, "power-converters-26.1.png", client.getMainRenderTarget(), 1,
                message -> client.execute(() -> {
                    capturing = false;
                    stage = 3;
                }));
            return;
        }
        if (stage == 3) {
            AnvilCraft.LOGGER.info("PORT_POWER_CONVERTERS_CLIENT_PASSED: live grid generation and 120 native model states");
            client.stop();
            stage = 4;
        }
    }

    private static final class Preview extends Screen {
        private final List<ItemStack> items = List.of(ModBlocks.POWER_CONVERTER_SMALL.asStack(), ModBlocks.POWER_CONVERTER_MIDDLE.asStack(),
            ModBlocks.POWER_CONVERTER_BIG.asStack(), ModBlocks.POWER_CONVERTER_SUPER_BIG.asStack(),
            ModBlocks.POWER_CONVERTER_EXTREMELY_BIG.asStack());

        private Preview() {
            super(Component.literal("Power converter parity"));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xFF303030);
            for (int index = 0; index < this.items.size(); index++) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(this.width / 2 - 176 + index * 72, this.height / 2 - 32);
                graphics.pose().scale(4, 4);
                graphics.item(this.items.get(index), 0, 0);
                graphics.pose().popMatrix();
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
