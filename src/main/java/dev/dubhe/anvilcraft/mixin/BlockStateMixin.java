package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.injection.block.state.IBlockStateExtension;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockState.class)
abstract class BlockStateMixin implements IBlockStateExtension {
}
