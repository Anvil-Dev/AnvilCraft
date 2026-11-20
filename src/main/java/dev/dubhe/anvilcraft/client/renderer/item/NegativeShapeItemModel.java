package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NegativeShapeItemModel extends BakedModelWrapper<BakedModel> {
    private final List<BakedModel> passes;

    public NegativeShapeItemModel(BakedModel original) {
        super(original);
        Map<TextureAtlasSprite, RenderType> layers = new HashMap<>();
        Map<RenderType, Pass> groups = new LinkedHashMap<>();
        List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
        sides.add(null);
        for (Direction side : sides) {
            for (BakedQuad quad : original.getQuads(null, side, RandomSource.create(42))) {
                RenderType layer = quad.isShade() ? null : layers.computeIfAbsent(quad.getSprite(), NegativeShapeItemModel::unlitLayer);
                groups.computeIfAbsent(layer, key -> new Pass(original, key)).quads
                    .computeIfAbsent(side, key -> new ArrayList<>()).add(quad);
            }
        }
        this.passes = List.copyOf(groups.values());
    }

    private static RenderType unlitLayer(TextureAtlasSprite sprite) {
        var image = sprite.contents().getOriginalImage();
        for (int row = 0; row < image.getHeight(); row++) {
            for (int column = 0; column < image.getWidth(); column++) {
                int alpha = image.getPixelRGBA(column, row) >>> 24;
                if (alpha > 0 && alpha < 255) {
                    return ModRenderTypes.UNLIT_ITEM.apply(sprite.atlasLocation(), true);
                }
            }
        }
        return ModRenderTypes.UNLIT_ITEM.apply(sprite.atlasLocation(), false);
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
        BakedModel transformed = this.originalModel.applyTransform(context, pose, leftHand);
        return transformed == this.originalModel ? this : transformed;
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        return this.passes;
    }

    private static final class Pass extends BakedModelWrapper<BakedModel> {
        private final @Nullable RenderType layer;
        private final Map<Direction, List<BakedQuad>> quads = new HashMap<>();

        private Pass(BakedModel original, @Nullable RenderType layer) {
            super(original);
            this.layer = layer;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
            return this.quads.getOrDefault(side, List.of());
        }

        @Override
        public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
            return this.layer == null ? super.getRenderTypes(stack, fabulous) : List.of(this.layer);
        }
    }
}
