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
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Matrix4f;

public class FeatureRendererSupport {
    public static BlockModelRenderState initialize(StandaloneModelKey<BlockStateModel> standalone, BlockEntity be) {
        BlockModelRenderState state = new BlockModelRenderState();
        Minecraft mc = Minecraft.getInstance();
        BlockStateModel model = mc.getModelManager().getStandaloneModel(standalone);
        if (model == null) {
            // Model JSON not yet added — return empty state to avoid NPE
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
