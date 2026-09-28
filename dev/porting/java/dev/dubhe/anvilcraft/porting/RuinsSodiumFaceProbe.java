package dev.dubhe.anvilcraft.porting;

import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class RuinsSodiumFaceProbe extends AbstractBlockRenderContext {
    public static void verify(Minecraft client, BlockPos original, BlockPos disguised, boolean expected) {
        var context = new RuinsSodiumFaceProbe();
        context.level = client.level;
        context.pos = original;
        context.state = client.level.getBlockState(original);
        boolean plain = context.shouldDrawSide(Direction.EAST);
        context.pos = disguised;
        context.state = client.level.getBlockState(disguised);
        boolean ruins = context.shouldDrawSide(Direction.EAST);
        if (plain != expected || ruins != expected) throw new IllegalStateException("Sodium disguised face at " + disguised);
    }

    @Override
    protected void processQuad(MutableQuadViewImpl quad) {
        throw new UnsupportedOperationException("Only side visibility is probed");
    }
}
