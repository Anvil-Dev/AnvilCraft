package dev.dubhe.anvilcraft.client.support;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureRendererSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger("anvilcraft:FeatureRendererSupport");

    public static BlockModelRenderState initialize(BlockState blockState, BlockEntity be) {
        BlockModelRenderState state = new BlockModelRenderState();
        Minecraft mc = Minecraft.getInstance();
        mc.getModelManager().getBlockStateModelSet().get(blockState).collectParts(
            mc.level,
            be.getBlockPos(),
            blockState,
            RandomSource.create(),
            state.setupModel(new Matrix4f(), false)
        );
        return state;
    }

    public static BlockModelRenderState initialize(StandaloneModelKey<BlockStateModel> standalone, BlockEntity be) {
        BlockModelRenderState state = new BlockModelRenderState();
        Minecraft mc = Minecraft.getInstance();
        BlockStateModel model = mc.getModelManager().getStandaloneModel(standalone);
        if (model == null) {
            LOGGER.warn("Standalone model '{}' is null, returning empty render state", standalone);
            return state;
        }
        model.collectParts(
            mc.level,
            be.getBlockPos(),
            be.getBlockState(),
            RandomSource.create(),
            state.setupModel(new Matrix4f(), false)
        );
        return state;
    }

    public static ItemClusterRenderState initialize(ItemStack stack, ItemModelResolver resolver) {
        ItemClusterRenderState state = new ItemClusterRenderState();
        state.seed = ItemClusterRenderState.getSeedForItemStack(stack);
        resolver.updateForTopItem(state.item, stack, ItemDisplayContext.GROUND, null, null, state.seed);
        state.count = ItemClusterRenderState.getRenderedAmount(stack.count());
        return state;
    }
}
