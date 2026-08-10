package dev.dubhe.anvilcraft.block.cauldron;

import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.dubhe.anvilcraft.api.block.IIgnitableCauldron;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.power.consumer.HeaterBlock;
import dev.dubhe.anvilcraft.block.special.PlasmaJetsBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.util.ModInteractionMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class OilCauldronBlock extends Layered4LevelCauldronBlock implements IHammerRemovable, IIgnitableCauldron {
    public static final BooleanProperty IGNITED = BooleanProperty.create("ignited");

    public OilCauldronBlock(Properties properties) {
        super(properties, ModInteractionMap.OIL);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(Layered4LevelCauldronBlock.LEVEL, 1)
                .setValue(OilCauldronBlock.IGNITED, false)
        );
    }

    public static void ignite(LevelAccessor level, BlockPos pos) {
        level.setBlock(pos, level.getBlockState(pos).setValue(OilCauldronBlock.IGNITED, true), 3);
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
        if (level.isClientSide()) return;
        if (entity.getType().equals(EntityType.ARROW) && entity.isOnFire()) {
            OilCauldronBlock.ignite(level, pos);
            return;
        }
        if (!(entity instanceof ItemEntity itemEntity)) return;
        if (itemEntity.getItem().is(ModItemTags.FIRE_STARTER)) {
            OilCauldronBlock.ignite(level, pos);
            itemEntity.getItem().setCount(itemEntity.getItem().getCount() - 1);
            return;
        }
        if (itemEntity.getItem().is(ModItemTags.UNBROKEN_FIRE_STARTER)) {
            OilCauldronBlock.ignite(level, pos);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OilCauldronBlock.IGNITED);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (this.isIgnited(new BlockCache(level), pos) && level.getBlockState(pos.below()).is(ModBlocks.HEATER)) {
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
        if (this.isIgnited(new BlockCache(level), pos) && level.getBlockState(pos.below()).is(ModBlocks.HEATER)) {
            level.scheduleTick(pos, this, 2);
        }
    }

    @Override
    protected void tick(BlockState cauldronState, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        if (below.is(ModBlocks.HEATER) && !below.getValue(HeaterBlock.OVERLOAD) && !PlasmaJetsBlock.trySpawn(pos.above(), level)) {
            level.scheduleTick(pos, this, 10);
        }
    }

    @Override
    public boolean isIgnited(BlockCache cache, BlockPos pos) {
        return cache.getBlockState(pos).getValue(OilCauldronBlock.IGNITED);
    }

    @Override
    public void setIgnited(BlockCache cache, BlockPos pos, boolean ignited) {
        cache.setBlock(pos, cache.getBlockState(pos).setValue(OilCauldronBlock.IGNITED, ignited));
    }

    @Override
    public Fluid getFluid(BlockCache cache, BlockPos pos) {
        return ModFluids.OIL.get();
    }

    @Override
    public int getFluidAmount(BlockCache cache, BlockPos pos) {
        return cache.getBlockState(pos).getValue(OilCauldronBlock.LEVEL) * 250;
    }

    @Override
    public boolean consumeOnce(BlockCache cache, BlockPos pos) {
        BlockState state = cache.getBlockState(pos);
        int layer = state.getValue(OilCauldronBlock.LEVEL) - 1;
        if (layer <= 0) {
            cache.setBlock(pos, Blocks.CAULDRON);
            return true;
        }
        cache.setBlock(pos, state.setValue(OilCauldronBlock.LEVEL, layer));
        return true;
    }
}
