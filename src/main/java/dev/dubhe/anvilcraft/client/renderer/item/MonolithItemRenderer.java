package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.block.GiantMonolithCoreBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.client.renderer.MonolithSurfaceRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class MonolithItemRenderer extends BaseBlockItemRenderer<MonolithItemRenderer.Argument> {
    private final BlockState state;

    public enum Argument { INSTANCE }

    private MonolithItemRenderer(BlockState state, boolean giant) {
        super(state, giant ? -1 : 0, giant ? 2 : 1);
        this.state = state;
    }

    @Override
    public Argument extractArgument(ItemStack stack) {
        return Argument.INSTANCE;
    }

    @Override
    public void submit(@Nullable Argument argument, PoseStack pose, SubmitNodeCollector collector, int light, int overlay,
                       boolean foil, int outlineColor) {
        this.submitShell(pose, collector, light, overlay, foil, outlineColor);
        MonolithSurfaceRenderer.submitFaces(pose, collector, MonolithSurfaceRenderer.faces(this.state));
    }

    public record Unbaked(Block block) implements SpecialModelRenderer.Unbaked<Argument> {
        public static final MapCodec<Unbaked> CODEC = BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block")
            .xmap(Unbaked::new, Unbaked::block);

        @Override
        public MonolithItemRenderer bake(BakingContext context) {
            BlockState state = this.block.defaultBlockState();
            boolean giant = state.getBlock() instanceof GiantMonolithCoreBlock;
            if (giant) {
                state = state.setValue(GiantMonolithCoreBlock.HALF, Cube3x3PartHalf.MID_CENTER)
                    .setValue(GiantMonolithCoreBlock.CUBE, GiantAnvilCube.CENTER);
            }
            return new MonolithItemRenderer(state, giant);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return CODEC;
        }
    }
}
