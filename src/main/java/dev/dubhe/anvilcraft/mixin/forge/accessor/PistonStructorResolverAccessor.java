package dev.dubhe.anvilcraft.mixin.forge.accessor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Mixin
 */
@Mixin(PistonStructureResolver.class)
public interface PistonStructorResolverAccessor {
    @Accessor
    Level getLevel();

    @Accessor
    List<BlockPos> getToPush();
}
