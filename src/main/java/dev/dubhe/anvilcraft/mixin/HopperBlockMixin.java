package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(HopperBlock.class)
abstract class HopperBlockMixin implements IHammerChangeable {
    @Shadow
    @Final
    public static DirectionProperty FACING;

    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public boolean change(Player player, BlockPos blockPos, @NotNull Level level, ItemStack anvilHammer) {
        return level.setBlockAndUpdate(blockPos, level.getBlockState(blockPos).cycle(FACING));
    }

    @Override
    @SuppressWarnings("AddedMixinMembersNamePattern")
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return FACING;
    }
}
