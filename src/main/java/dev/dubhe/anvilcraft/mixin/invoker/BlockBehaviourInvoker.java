package dev.dubhe.anvilcraft.mixin.invoker;

import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

@Mixin(BlockBehaviour.class)
public interface BlockBehaviourInvoker {
    @Invoker
    @Nullable
    MenuProvider invokeGetMenuProvider(BlockState state, Level level, BlockPos pos);
}
