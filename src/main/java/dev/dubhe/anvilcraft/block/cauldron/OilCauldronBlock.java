package dev.dubhe.anvilcraft.block.cauldron;

import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.dubhe.anvilcraft.api.block.IIgnitableCauldron;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.util.ModInteractionMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public class OilCauldronBlock extends Layered4LevelCauldronBlock implements IHammerRemovable, IIgnitableCauldron {
    public OilCauldronBlock(Properties properties) {
        super(properties, ModInteractionMap.OIL);
    }

    public static void ignite(LevelAccessor level, BlockPos pos, BlockState beforeConvert) {
        level.setBlock(pos, ModBlocks.FIRE_CAULDRON.get().copyLevelFrom(beforeConvert), 3);
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
            ignite(level, pos, state);
            return;
        }
        if (!(entity instanceof ItemEntity itemEntity)) return;
        if (itemEntity.getItem().is(ModItemTags.FIRE_STARTER)) {
            ignite(level, pos, state);
            itemEntity.getItem().setCount(itemEntity.getItem().getCount() - 1);
            return;
        }
        if (itemEntity.getItem().is(ModItemTags.UNBROKEN_FIRE_STARTER)) {
            ignite(level, pos, state);
        }
    }

    @Override
    public boolean isIgnited(BlockCache cache, BlockPos pos) {
        return false;
    }

    @Override
    public void setIgnited(BlockCache cache, BlockPos pos, boolean ignited) {
        if (!ignited) return;
        int level = cache.getBlockState(pos).getValue(OilCauldronBlock.LEVEL);
        cache.setBlock(pos, ModBlocks.FIRE_CAULDRON.getDefaultState().setValue(FireCauldronBlock.LEVEL, level));
    }

    @Override
    public Fluid getFluid(BlockCache cache, BlockPos pos) {
        return ModFluids.OIL.get();
    }
}
