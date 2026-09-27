package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public final class GasFluidClientScene {
    private static int stage;
    private static long deadline;
    private static long next;
    private static boolean capturing;
    private static boolean colorsVerified;
    private static Preview preview;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 120000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Gas fluid client stage " + stage);
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
            var fluids = List.of(ModFluids.HYDROGEN.get(), ModFluids.OXYGEN.get(), ModFluids.HELIUM.get(), ModFluids.DEUTERIUM.get(),
                ModFluids.XENON.get(), ModFluids.KRYPTON.get(), ModFluids.PRIMORDIAL_MATTER.get());
            int[] colors = {0xFFC9E4F7, 0xFF9CCCF8, 0xFFF0C8E0, 0xFFA8E8DC, 0xFFC9C2F0, 0xFFB0E8A8, 0xFFE6CFFF};
            for (int index = 0; index < fluids.size(); index++) {
                if (tint(client, fluids.get(index)) != colors[index]) throw new IllegalStateException("Source fluid tint " + index);
            }
            colorsVerified = true;
            GasBucketMeshProbe.verify(client);
            next = System.currentTimeMillis() + 1000;
            stage = 2;
            return;
        }
        if (stage == 2) {
            capturing = true;
            Screenshot.grab(client.gameDirectory, "gas-fluids-26.1.png", client.getMainRenderTarget(), 1,
                message -> client.execute(() -> {
                    capturing = false;
                    stage = 3;
                }));
            return;
        }
        if (stage == 3) {
            if (!colorsVerified) throw new IllegalStateException("Fluid color check did not run");
            AnvilCraft.LOGGER.info("PORT_GAS_FLUIDS_CLIENT_PASSED: seven native bucket models and source fluid tints");
            client.stop();
            stage = 4;
        }
    }

    private static int tint(Minecraft client, Fluid fluid) {
        var model = FluidRenderHelper.getModel(client.getModelManager().getFluidStateModelSet(), fluid);
        return model.fluidTintSource().colorAsStack(new FluidStack(fluid, 1000));
    }

    private static final class Preview extends Screen {
        private final List<ItemStack> items = List.of(ModItems.HYDROGEN_BUCKET.asStack(), ModItems.OXYGEN_BUCKET.asStack(),
            ModItems.HELIUM_BUCKET.asStack(), ModItems.DEUTERIUM_BUCKET.asStack(), ModItems.XENON_BUCKET.asStack(),
            ModItems.KRYPTON_BUCKET.asStack(), ModItems.PRIMORDIAL_MATTER_BUCKET.asStack());

        private Preview() {
            super(Component.literal("Gas fluid source parity"));
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
