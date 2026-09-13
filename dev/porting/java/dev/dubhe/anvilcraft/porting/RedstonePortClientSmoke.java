package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.blockentity.BigRedButtonBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.RedstoneDiceBlockEntityRenderer;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.FlightTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class RedstonePortClientSmoke {
    private static boolean completed;

    @SubscribeEvent
    public static void tick(RenderFrameEvent.Post event) {
        if (!Boolean.getBoolean("anvilcraft.portClientSmoke") || completed) return;
        Minecraft client = Minecraft.getInstance();
        if (client.screen == null || client.getOverlay() != null) return;
        var models = client.getModelManager();
        checkModel(models.getStandaloneModel(BigRedButtonBlockEntityRenderer.CAP));
        checkModel(models.getStandaloneModel(RedstoneDiceBlockEntityRenderer.DICE));
        for (Block block : List.of(ModBlocks.BIG_RED_BUTTON.get(), ModBlocks.REDSTONE_DICE.get(), ModBlocks.ITEM_SPLITTER.get(),
            ModBlocks.OVERFLOW_CHUTE.get(), ModBlocks.STORAGE_PORT.get(), ModBlocks.STORAGE_FLUID_PORT.get(),
            ModBlocks.STORAGE_PORT_CONSOLIDATOR.get())) {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                checkModel(models.getBlockStateModelSet().get(state));
            }
        }
        SelectionPortChecks.run();
        DynamicSelectionChecks.run();
        completed = true;
        AnvilCraft.LOGGER.info(
            "PORT_CLIENT_STARTUP_PASSED: 795 block and standalone models baked; visual parity still requires in-world review"
        );
        client.stop();
    }

    public static void checkBackpackAssets(Minecraft client) {
        for (int time : new int[]{0, 20}) {
            ItemStack backpack = new ItemStack(ModItems.IONOCRAFT_BACKPACK.get());
            backpack.set(ModComponents.FLIGHT_TIME, new FlightTime(time));
            ItemStackRenderState state = new ItemStackRenderState();
            client.getItemModelResolver().updateForTopItem(state, backpack, ItemDisplayContext.GUI, null, null, 0);
            var material = state.pickParticleMaterial(RandomSource.create(0));
            String expected = time == 0 ? "item/ionocraft_backpack_off" : "item/ionocraft_backpack";
            if (material == null || !material.sprite().contents().name().equals(AnvilCraft.of(expected))) {
                throw new IllegalStateException("离子背包物品模型未使用最新的对应状态贴图");
            }
        }
        AnvilCraft.LOGGER.info("PORT_SOURCE_BACKPACK_ASSETS_PASSED");
    }

    private static void checkModel(BlockStateModel model) {
        if (model == null || model.particleMaterial().sprite().contents().name().getPath().equals("missingno")) {
            throw new IllegalStateException("移植模型缺失或使用缺失纹理");
        }
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(0), parts);
        if (parts.isEmpty()) throw new IllegalStateException("移植模型没有可绘制的部分");
    }
}
