package dev.dubhe.anvilcraft.block.cauldron;

import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.block.IIgnitableCauldron;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.block.PlasmaJetsBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class FireCauldronBlock extends Layered4LevelCauldronBlock implements IHammerRemovable, IIgnitableCauldron {
    public FireCauldronBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(
        BlockState state,
        Level level,
        BlockPos pos,
        Entity entity,
        InsideBlockEffectApplier effectApplier,
        boolean isPrecise
    ) {
        if (level.isClientSide()) {
            if (entity.hurtClient(level.damageSources().inFire())) {
                level.playSound(
                    null,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    SoundEvents.GENERIC_BURN,
                    entity.getSoundSource(),
                    0.4F,
                    2.0F + entity.getRandom().nextFloat() * 0.4F
                );
            }
        } else {
            if (entity.hurtServer(Util.cast(level), level.damageSources().inFire(), 4.0F)) {
                level.playSound(
                    null,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    SoundEvents.GENERIC_BURN,
                    entity.getSoundSource(),
                    0.4F,
                    2.0F + entity.getRandom().nextFloat() * 0.4F
                );
            }
        }
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
        Block block,
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
    public boolean consumeOnce(BlockCache cache, BlockPos pos) {
        BlockState state = cache.getBlockState(pos);
        int newLevel = state.getValue(LEVEL) - 1;
        if (newLevel > 0) {
            state = state.setValue(LEVEL, newLevel);
        } else {
            state = Blocks.CAULDRON.defaultBlockState();
        }
        cache.setBlock(pos, state);
        return true;
    }
}
