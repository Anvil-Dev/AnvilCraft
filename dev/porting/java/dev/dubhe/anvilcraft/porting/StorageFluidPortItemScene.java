package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.item.StorageFluidPortItemRenderer;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import java.util.ArrayList;
import java.util.List;

public final class StorageFluidPortItemScene {
    private static final String[] LABELS = {"Empty", "1 mB", "32 B honey", "128 B honey", "64 B water"};
    private static FluidScreen screen;
    private static int stage;
    private static int frames;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (screen == null) {
            client.options.guiScale().set(2);
            client.resizeGui();
            screen = new FluidScreen(items(client));
            client.setScreen(screen);
        }
        if (stage >= LABELS.length) {
            AnvilCraft.LOGGER.info("PORT_STORAGE_FLUID_ITEM_SCENE_PASSED");
            client.stop();
            return;
        }
        if (client.screen != screen || capturing || ++frames < 60) return;
        capturing = true;
        Screenshot.grab(client.gameDirectory, "storage-fluid-item-26.1-" + stage + ".png", client.getMainRenderTarget(), 1, message -> {
            AnvilCraft.LOGGER.info("PORT_STORAGE_FLUID_ITEM_CAPTURED: {}", message.getString());
            client.execute(() -> {
                stage++;
                frames = 0;
                capturing = false;
            });
        });
    }

    private static List<ItemStack> items(Minecraft client) {
        List<ItemStack> items = new ArrayList<>();
        int[] amounts = {0, 1, 32000, 128000, 64000};
        for (int index = 0; index < amounts.length; index++) {
            var port = new StorageFluidPortBlockEntity(ModBlockEntities.STORAGE_FLUID_PORT.get(), BlockPos.ZERO,
                ModBlocks.STORAGE_FLUID_PORT.getDefaultState());
            if (amounts[index] > 0) {
                port.getTank().set(0, FluidResource.of(index == 4 ? Fluids.WATER : ModFluids.HONEY.get()), amounts[index]);
            }
            ItemStack stack = new ItemStack(ModBlocks.STORAGE_FLUID_PORT.asItem());
            port.saveToDrop(stack, client.level.registryAccess());
            items.add(stack);
        }
        var renderer = new StorageFluidPortItemRenderer();
        if (renderer.extractArgument(items.getFirst()) != null) throw new IllegalStateException("空端口错误显示液面");
        if (renderer.extractArgument(items.get(1)).fill() >= 0.025F) throw new IllegalStateException("微量流体错误套用了最低显示高度");
        if (renderer.extractArgument(items.get(2)).fill() != 0.25F || renderer.extractArgument(items.get(3)).fill() != 1) {
            throw new IllegalStateException("物品液位与容量比例不符");
        }
        if (!renderer.extractArgument(items.get(2)).equals(renderer.extractArgument(items.get(2)))) {
            throw new IllegalStateException("静态蜂蜜图标未复用身份");
        }
        if (renderer.extractArgument(items.get(4)).equals(renderer.extractArgument(items.get(4)))) {
            throw new IllegalStateException("水的动态贴图被缓存冻结");
        }
        for (int index = 0; index < items.size(); index++) {
            var tooltip = ClientTooltipComponent.create(items.get(index).getTooltipImage().orElseThrow());
            if (tooltip.getHeight(client.font) != (index == 0 ? 20 : 46)) throw new IllegalStateException("流体提示布局高度不符");
        }
        AnvilCraft.LOGGER.info("PORT_STORAGE_FLUID_ITEM_CHECKS_PASSED");
        return items;
    }

    private static final class FluidScreen extends Screen {
        private final List<ItemStack> items;

        private FluidScreen(List<ItemStack> items) {
            super(Component.literal("Storage fluid port item parity"));
            this.items = items;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xff252525);
            graphics.text(this.font, this.title, 40, 24, -1, false);
            for (int index = 0; index < this.items.size(); index++) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(40 + index * 100, 60);
                graphics.pose().scale(3);
                graphics.item(this.items.get(index), 0, 0);
                graphics.pose().popMatrix();
                graphics.text(this.font, LABELS[index], 40 + index * 100, 116, -1, false);
                graphics.item(this.items.get(index), 40 + index * 100, 144);
            }
            ItemStack selected = this.items.get(Math.min(stage, this.items.size() - 1));
            graphics.setTooltipForNextFrame(this.font, selected, 100, 220);
        }
    }
}
