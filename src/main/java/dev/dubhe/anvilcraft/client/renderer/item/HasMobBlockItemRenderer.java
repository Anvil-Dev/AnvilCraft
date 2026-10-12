package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.item.block.HasMobBlockItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

public final class HasMobBlockItemRenderer extends BaseBlockItemRenderer<EntityRenderState> {
    private final CameraRenderState camera = new CameraRenderState();

    private HasMobBlockItemRenderer(Block block) {
        super(block.defaultBlockState(), 0, 1, true);
    }

    @Override
    public @Nullable EntityRenderState extractArgument(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(stack.getItem() instanceof HasMobBlockItem)) return null;
        Entity entity = HasMobBlockItem.getMobFromItem(minecraft.level, stack);
        if (entity == null) return null;
        entity.setPos(0, 0, 0);
        entity.setOldPosAndRot();
        EntityRenderState state = minecraft.getEntityRenderDispatcher().extractEntity(entity, 0);
        state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
        state.nameTag = null;
        state.leashStates = null;
        state.shadowPieces.clear();
        return state;
    }

    @Override
    public void submit(
        @Nullable EntityRenderState argument, PoseStack pose, SubmitNodeCollector collector,
        int light, int overlay, boolean foil, int outlineColor
    ) {
        if (argument != null) {
            pose.pushPose();
            pose.translate(0.5F, 0.196F, 0.5F);
            float size = 0.52943F * 0.8F / 0.625F;
            float max = Math.max(argument.boundingBoxWidth, argument.boundingBoxHeight);
            if (max > 1) size /= max;
            pose.scale(size, size, size);
            Minecraft.getInstance().getEntityRenderDispatcher().submit(argument, this.camera, 0, 0, 0, pose, collector);
            pose.popPose();
        }
        this.submitShell(pose, collector, light, overlay, foil, outlineColor);
    }

    public record Unbaked(Block block) implements SpecialModelRenderer.Unbaked<EntityRenderState> {
        public static final MapCodec<Unbaked> CODEC = BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block")
            .xmap(Unbaked::new, Unbaked::block);

        @Override
        public HasMobBlockItemRenderer bake(BakingContext context) {
            return new HasMobBlockItemRenderer(this.block);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }
    }
}
