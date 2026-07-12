package dev.dubhe.anvilcraft.api.rendering;

import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class BlockStateModelRenderer {
    public static final BlockStateModelRenderer INSTANCE = new BlockStateModelRenderer();
    private final Map<BlockStateModelTessellateState, WrappedBlockStateModel> cache = new HashMap<>();

    @Getter
    private final ModelBlockRenderer tessellator = new ModelBlockRenderer(
        false,
        true,
        Minecraft.getInstance().getBlockColors()
    );

    @Nullable
    public WrappedBlockStateModel getModel(BlockStateModelTessellateState state) {
        return this.cache.get(state);
    }

    public BlockStateModelTessellateState prepare(
        StandaloneModelKey<BlockStateModel> key,
        boolean translucent,
        boolean lighting
    ) {
        RenderType renderType;
        if (!lighting) {
            if (translucent) {
                renderType = ModRenderTypes.TRANSLUCENT_BLOCK;
            } else {
                renderType = ModRenderTypes.CUTOUT_BLOCK;
            }
        } else {
            if (translucent) {
                renderType = Sheets.translucentBlockSheet();
            } else {
                renderType = Sheets.cutoutBlockSheet();
            }
        }
        BlockStateModelTessellateState state = new BlockStateModelTessellateState(
            key,
            renderType,
            translucent,
            lighting
        );
        WrappedBlockStateModel model = this.cache.get(state);
        if (model == null) {
            this.cache.put(
                state,
                new WrappedBlockStateModel(state, this)
            );
        }
        return state;
    }
}
