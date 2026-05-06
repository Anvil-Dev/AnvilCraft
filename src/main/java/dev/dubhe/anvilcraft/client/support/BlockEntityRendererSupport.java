package dev.dubhe.anvilcraft.client.support;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Matrix4f;

public class BlockEntityRendererSupport {
    public static BlockModelRenderState initialize(StandaloneModelKey<BlockStateModel> standalone, BlockEntity be) {
        BlockModelRenderState state = new BlockModelRenderState();
        Minecraft mc = Minecraft.getInstance();
        mc.getModelManager().getStandaloneModel(standalone).collectParts(
            mc.level,
            be.getBlockPos(),
            be.getBlockState(),
            RandomSource.create(),
            state.setupModel(new Matrix4f(), false)
        );
        return state;
    }
}
