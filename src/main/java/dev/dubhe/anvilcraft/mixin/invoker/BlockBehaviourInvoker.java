package dev.dubhe.anvilcraft.mixin.invoker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockBehaviour.class)
public interface BlockBehaviourInvoker {
    @Invoker
    MenuProvider invokeGetMenuProvider(BlockState state, Level level, BlockPos pos);
}
