package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.api.rendering.BlockStateModelRenderer;
import dev.dubhe.anvilcraft.api.rendering.BlockStateModelTessellateState;
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

    private static final Logger LOGGER = LoggerFactory.getLogger("FeatureRendererSupport");

    public static BlockStateModelTessellateState createTessellation(
        StandaloneModelKey<BlockStateModel> key,
        boolean lighting
    ) {
        return createTessellation(
            key,
            false,
            lighting
        );
    }

    public static BlockStateModelTessellateState createTessellation(
        StandaloneModelKey<BlockStateModel> key,
        boolean translucent,
        boolean lighting
    ) {
        return BlockStateModelRenderer.INSTANCE.prepare(key, translucent, lighting);
    }

    public static BlockModelRenderState initialize(StandaloneModelKey<BlockStateModel> standalone, BlockEntity be) {
        return initialize(standalone, be, false);
    }

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

    /**
     * 初始化独立方块模型，并明确指定其是否使用半透明方块渲染层。
     */
    public static BlockModelRenderState initialize(
        StandaloneModelKey<BlockStateModel> standalone,
        BlockEntity be,
        boolean translucent
    ) {
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
            state.setupModel(new Matrix4f(), translucent)
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
