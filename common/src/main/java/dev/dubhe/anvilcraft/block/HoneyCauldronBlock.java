package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import org.jetbrains.annotations.NotNull;

public class HoneyCauldronBlock extends LayeredCauldronBlock implements IHammerRemovable {

    public HoneyCauldronBlock(Properties properties) {
        super(properties, p -> false, CauldronInteraction.EMPTY);
    }

    @Override
    public void entityInside(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Entity entity) {}

    @Override
    protected void handleEntityOnFireInside(
            @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {}

    @Override
    public void handlePrecipitation(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Biome.Precipitation precipitation) {}

    @Override
    protected boolean canReceiveStalactiteDrip(@NotNull Fluid fluid) {
        return false;
    }

    @Override
    protected void receiveStalactiteDrip(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Fluid fluid) {}

    @Override
    public @NotNull ItemStack getCloneItemStack(
            @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull BlockState state) {
        return new ItemStack(Items.CAULDRON);
    }
}
