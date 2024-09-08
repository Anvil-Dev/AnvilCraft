package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HoneyCauldronBlock extends LayeredCauldronBlock implements IHammerRemovable {

    public HoneyCauldronBlock(
        Properties properties
    ) {
        super(Biome.Precipitation.NONE, CauldronInteraction.EMPTY, properties);
    }

    @Override
    public void entityInside(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Entity entity
    ) {
    }

    @Override
    public void handlePrecipitation(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Biome.Precipitation precipitation
    ) {
    }

    @Override
    protected boolean canReceiveStalactiteDrip(@NotNull Fluid fluid) {
        return false;
    }

    @Override
    protected void receiveStalactiteDrip(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Fluid fluid
    ) {
    }

    @Override
    public ItemStack getCloneItemStack(
        BlockState state,
        HitResult target,
        LevelReader level,
        BlockPos pos,
        Player player
    ) {
        return new ItemStack(Items.CAULDRON);
    }
}
