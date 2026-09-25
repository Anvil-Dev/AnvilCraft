package dev.dubhe.anvilcraft.block.cauldron;

import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.block.IIgnitableCauldron;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.power.consumer.HeaterBlock;
import dev.dubhe.anvilcraft.block.special.PlasmaJetsBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

import java.util.OptionalInt;

public class FireCauldronBlock extends Layered4LevelCauldronBlock implements IHammerRemovable, IIgnitableCauldron {
    public FireCauldronBlock(Properties properties) {
        super(properties, CauldronInteractions.EMPTY);
    }

    @Override
    protected void entityInside(
        BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean precise
    ) {
        if (this.isEntityInsideContent(state, pos, entity)) {
            entity.lavaHurt();
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData, Player player) {
        return new ItemStack(Items.CAULDRON);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean ignored) {
        if (level.getBlockState(pos.below()).is(ModBlocks.HEATER)) {
            level.scheduleTick(pos, this, 2);
        }
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        @Nullable Orientation orientation,
        boolean movedByPiston
    ) {
        if (level.getBlockState(pos.below()).is(ModBlocks.HEATER)) {
            level.scheduleTick(pos, this, 2);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        if (below.is(ModBlocks.HEATER) && !below.getValue(HeaterBlock.OVERLOAD) && !PlasmaJetsBlock.trySpawn(pos.above(), level)) {
            level.scheduleTick(pos, this, 10);
        }
    }

    @Override
    public boolean isIgnited(BlockCache cache, BlockPos pos) {
        return true;
    }

    @Override
    public void setIgnited(BlockCache cache, BlockPos pos, boolean ignited) {
        if (ignited) return;
        cache.setBlock(
            pos,
            ModBlocks.OIL_CAULDRON.getDefaultState()
                .setValue(OilCauldronBlock.LEVEL, cache.getBlockState(pos).getValue(FireCauldronBlock.LEVEL))
        );
    }

    @Override
    public Fluid getFluid(BlockCache cache, BlockPos pos) {
        return ModFluids.OIL.get();
    }

    @Override
    public int getFluidAmount(BlockCache cache, BlockPos pos) {
        return cache.getBlockState(pos).getValue(FireCauldronBlock.LEVEL) * 250;
    }

    @Override
    public OptionalInt consumeOnce(BlockCache cache, BlockPos pos, boolean simulate) {
        BlockState state = cache.getBlockState(pos);
        int remaining = state.getValue(FireCauldronBlock.LEVEL) - AnvilCraft.CONFIG.plasmaJetsCauldronConsumeAmount;
        if (remaining < 0) {
            return OptionalInt.empty();
        }
        if (!simulate) {
            if (remaining == 0) {
                cache.setBlock(pos, Blocks.CAULDRON.defaultBlockState());
            } else {
                cache.setBlock(pos, state.setValue(FireCauldronBlock.LEVEL, remaining));
            }
        }
        return OptionalInt.of(AnvilCraft.CONFIG.plasmaJetsCauldronExtraDuration);
    }
}
