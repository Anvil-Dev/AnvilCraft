package dev.dubhe.anvilcraft.mixin.accessor;

import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * {@link FallingBlockEntity} 访问器
 */
@Mixin(FallingBlockEntity.class)
public interface FallingBlockEntityAccessor {
    @Accessor("blockState")
    void setBlockState(BlockState state);
}
