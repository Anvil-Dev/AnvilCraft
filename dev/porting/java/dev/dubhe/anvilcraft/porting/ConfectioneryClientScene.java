package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cake.StepEffectBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ConfectioneryClientScene {
    private static int stage;
    private static long deadline;
    private static long next;
    private static boolean capturing;
    private static boolean clientEffectsVerified;
    private static Preview preview;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 120000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Confectionery client stage " + stage);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        if (stage == 0) {
            client.getSingleplayerServer().execute(() -> {
                var server = client.getSingleplayerServer();
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
            });
            client.options.guiScale().set(2);
            client.resizeGui();
            preview = new Preview();
            client.setScreen(preview);
            stage = 1;
            return;
        }
        if (stage == 1) {
            if (client.level.getGameTime() % 80 != 0) return;
            client.player.removeEffect(MobEffects.SPEED);
            client.player.removeEffect(MobEffects.HASTE);
            client.player.removeEffect(MobEffects.JUMP_BOOST);
            StepEffectBlock.stepOnChocolateBlock(client.player);
            StepEffectBlock.stepOnBlackChocolateBlock(client.player);
            StepEffectBlock.stepOnWhiteChocolateBlock(client.player);
            StepEffectBlock.stepOnBlackWhiteChocolateBlock(client.player);
            if (client.player.hasEffect(MobEffects.SPEED) || client.player.hasEffect(MobEffects.HASTE)
                || client.player.hasEffect(MobEffects.JUMP_BOOST)) {
                throw new IllegalStateException("Chocolate effects must be server-authoritative");
            }
            clientEffectsVerified = true;
            next = System.currentTimeMillis() + 1000;
            stage = 2;
            return;
        }
        if (stage == 2) {
            capturing = true;
            Screenshot.grab(client.gameDirectory, "confectionery-26.1.png", client.getMainRenderTarget(), 1,
                message -> client.execute(() -> {
                    capturing = false;
                    stage = 3;
                }));
            return;
        }
        if (stage == 3) {
            if (!clientEffectsVerified) throw new IllegalStateException("Client authority check did not run");
            AnvilCraft.LOGGER.info("PORT_CONFECTIONERY_CLIENT_PASSED: seven source models/textures and client effect authority");
            client.stop();
            stage = 4;
        }
    }

    private static final class Preview extends Screen {
        private final List<ItemStack> items = List.of(ModBlocks.HONEY_CREAM_BLOCK.asStack(), ModBlocks.HONEY_CAKE_BLOCK.asStack(),
            ModBlocks.MATCHA_CREAM_BLOCK.asStack(), ModBlocks.MATCHA_CAKE_BLOCK.asStack(), ModBlocks.COOKIE_BLOCK.asStack(),
            ModBlocks.COOKIE_PILLAR.asStack(), ModBlocks.BLACK_WHITE_CHOCOLATE_BLOCK.asStack());

        private Preview() {
            super(Component.literal("Confectionery source parity"));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xFF303030);
            for (int index = 0; index < this.items.size(); index++) {
                int x = this.width / 2 - 144 + index % 4 * 80;
                int y = this.height / 2 - 82 + index / 4 * 90;
                graphics.pose().pushMatrix();
                graphics.pose().translate(x, y);
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
