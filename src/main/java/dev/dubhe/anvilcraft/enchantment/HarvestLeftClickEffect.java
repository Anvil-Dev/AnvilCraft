package dev.dubhe.anvilcraft.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public record HarvestLeftClickEffect(int range) implements EnchantmentEntityEffect {
    public static final MapCodec<HarvestLeftClickEffect> CODEC = RecordCodecBuilder.mapCodec(it ->
        it.group(
            Codec.INT.optionalFieldOf("range", 3)
                .forGetter(HarvestLeftClickEffect::range)
        ).apply(it, HarvestLeftClickEffect::new)
    );

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse enchantedItemInUse, Entity entity, Vec3 vec3) {
        ItemStack itemStack = enchantedItemInUse.itemStack();
        BlockPos pos = BlockPos.containing(vec3);
        BlockState state = level.getBlockState(pos);
        int r = Math.min(enchantmentLevel, 3);
        if (!(entity instanceof Player player)) {
            return;
        }
        if (!itemStack.is(ItemTags.HOES)) {
            return;
        }
        if (isGrass(state)) {
            r = Math.min(enchantmentLevel, 7);
            for (BlockPos blockPos : BlockPos.betweenClosed(pos.offset(r, 0, r), pos.offset(-r, 0, -r))) {
                if (blockPos.equals(pos)) {
                    continue;
                }
                BlockState blockState = level.getBlockState(blockPos);
                if (isGrass(blockState)) {
                    level.destroyBlock(blockPos, true);
                }
            }
            return;
        }
        if (harvestable(state) == null) {
            return;
        }
        Iterable<BlockPos> posIterable = BlockPos.betweenClosed(pos.offset(r, r, r), pos.offset(-r, -r, -r));
        for (BlockPos blockPos : posIterable) {
            BlockState blockState = level.getBlockState(blockPos);
            Block harvestableBlock = harvestable(blockState);
            if (harvestableBlock == null) {
                continue;
            }
            if (blockPos.equals(pos)) {
                continue;
            }
            if (harvestableBlock instanceof SugarCaneBlock) {
                BlockPos below = blockPos.below();
                BlockState state1 = level.getBlockState(below);
                if (state1.is(BlockTags.DIRT) || state1.is(BlockTags.SAND)) {
                    boolean flag = false;
                    for (Direction direction : Direction.Plane.HORIZONTAL) {
                        BlockState state2 = level.getBlockState(below.relative(direction));
                        FluidState fluidState = level.getFluidState(below.relative(direction));
                        if (state2.canBeHydrated(level, below, fluidState, below.relative(direction))
                            || state2.is(Blocks.FROSTED_ICE)) {
                            flag = true;
                        }
                    }
                    if (flag) {
                        continue;
                    }
                }
            }
            if (harvestableBlock instanceof GrowingPlantBlock growingPlantBlock) {
                BlockPos blockPos1 = blockPos.relative(growingPlantBlock.anvilcraft$getGrowthDirection().getOpposite());
                BlockState blockState1 = level.getBlockState(blockPos1);
                if (blockState1.isFaceSturdy(level, blockPos1, growingPlantBlock.anvilcraft$getGrowthDirection())) {
                    continue;
                }
            }
            harvestableBlock.playerDestroy(level, player, blockPos, state, null, itemStack);
            level.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
            itemStack.hurtAndBreak(1, level, player, enchantedItemInUse.onBreak());
        }
    }

    @Nullable
    private Block harvestable(BlockState state) {
        if (
            state.is(Blocks.SUGAR_CANE)
            || state.is(Blocks.KELP)
            || state.is(Blocks.KELP_PLANT)
            || state.is(Blocks.VINE)
            || state.is(Blocks.WEEPING_VINES)
            || state.is(Blocks.WEEPING_VINES_PLANT)
            || state.is(Blocks.TWISTING_VINES)
            || state.is(Blocks.TWISTING_VINES_PLANT)
        ) {
            return state.getBlock();
        }
        return null;
    }

    private boolean isGrass(BlockState state) {
        return state.is(Blocks.SHORT_GRASS)
            || state.is(Blocks.TALL_GRASS);
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return CODEC;
    }
}
