package dev.dubhe.anvilcraft.api.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;

@NullMarked
public class WrappedBlockStateModel extends Model<BlockStateModelTessellateState> {
    private static final ModelPart EMPTY = new ModelPart(List.of(), Map.of());

    private final BlockStateModelTessellateState state;
    private final BlockStateModelRenderer renderer;

    public WrappedBlockStateModel(BlockStateModelTessellateState state, BlockStateModelRenderer renderer) {
        super(
            EMPTY,
            RenderTypes::entityCutout
        );
        this.state = state;
        this.renderer = renderer;
    }

    public void renderToBuffer(
        PoseStack poseStack,
        VertexConsumer buffer,
        int lightCoords,
        int overlayCoords,
        int color
    ) {
        Minecraft mc = Minecraft.getInstance();
        StandaloneModelKey<BlockStateModel> key = this.state.key();
        ModelBlockRenderer tessellator = this.renderer.getTessellator();
        BlockStateModel model = mc.getModelManager().getStandaloneModel(key);

        tessellator.tesselateBlock(
            ((_, _, _, bakedQuad, quadInstance) -> {
                quadInstance.setLightCoords(lightCoords);
                quadInstance.setOverlayCoords(overlayCoords);
                buffer.putBakedQuad(
                    poseStack.last(),
                    bakedQuad,
                    quadInstance
                );
            }),
            0,
            0,
            0,
            BlockAndTintGetter.EMPTY,
            BlockPos.ZERO,
            Blocks.AIR.defaultBlockState(),
            model,
            42
        );
    }

}
