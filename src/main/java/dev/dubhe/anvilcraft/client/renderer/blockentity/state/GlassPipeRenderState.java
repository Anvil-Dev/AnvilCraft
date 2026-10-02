package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public class GlassPipeRenderState extends PipeCheckValveRenderState {
    public FluidResource fluid = FluidResource.EMPTY;
    public BlockState blockState = Blocks.AIR.defaultBlockState();
    public Set<Direction> directions = Set.of();
    public float alpha = 1.0F;
    public int color = -1;
    public boolean opaque;
    @Nullable public TextureAtlasSprite sprite;
}
