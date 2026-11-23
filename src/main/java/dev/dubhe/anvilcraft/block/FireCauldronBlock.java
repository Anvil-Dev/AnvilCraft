package dev.dubhe.anvilcraft.block;

import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.block.IIgnitableCauldron;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.util.CauldronIgnition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.OptionalInt;

public class FireCauldronBlock extends Layered4LevelCauldronBlock implements IHammerRemovable, IIgnitableCauldron {
    public FireCauldronBlock(Properties properties) {
        super(properties, CauldronInteraction.EMPTY);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (this.isEntityInsideContent(state, pos, entity)) {
            entity.lavaHurt();
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
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
        BlockPos neighborPos,
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

    /** 用锹熄灭燃烧的油锅，原油保留。 */
    public static void extinguish(LevelAccessor level, BlockPos pos, BlockState beforeConvert) {
        level.setBlock(pos, ModBlocks.OIL_CAULDRON.get().copyLevelFrom(beforeConvert), 3);
    }

    @Override
    public ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (CauldronIgnition.isShovel(stack)) {
            return CauldronIgnition.useShovelToExtinguish(
                level, pos, player, hand, stack, () -> FireCauldronBlock.extinguish(level, pos, state));
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
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
        int remaining = state.getValue(FireCauldronBlock.LEVEL) - AnvilCraft.CONFIG.machines.plasmaJets.cauldronConsumeAmount;
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
        return OptionalInt.of(AnvilCraft.CONFIG.machines.plasmaJets.cauldronExtraDuration);
    }
}
