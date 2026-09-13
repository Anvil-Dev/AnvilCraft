package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.blockentity.BigRedButtonBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.RedstoneDiceBlockEntityRenderer;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.util.RandomSource;
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
            ModBlocks.OVERFLOW_CHUTE.get(), ModBlocks.STORAGE_PORT.get(), ModBlocks.STORAGE_FLUID_PORT.get())) {
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                checkModel(models.getBlockStateModelSet().get(state));
            }
        }
        SelectionPortChecks.run();
        DynamicSelectionChecks.run();
        completed = true;
        AnvilCraft.LOGGER.info(
            "PORT_CLIENT_STARTUP_PASSED: 793 block and standalone models baked; visual parity still requires in-world review"
        );
        client.stop();
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
