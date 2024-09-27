package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.util.Util;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class OilCauldronBlock extends LayeredCauldronBlock implements IHammerRemovable {
    public OilCauldronBlock(Properties properties) {
        super(Biome.Precipitation.NONE, CauldronInteraction.EMPTY, properties);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity.getType().equals(EntityType.ARROW) && entity.isOnFire()) {
            level.setBlockAndUpdate(
                pos,
                ModBlocks.FIRE_CAULDRON
                    .getDefaultState()
                    .setValue(
                        LayeredCauldronBlock.LEVEL,
                        level.getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL)
                    )
            );
            return;
        }
        if (entity instanceof ItemEntity itemEntity) {
            if (itemEntity.getItem().is(ModItemTags.FIRE_STARTER)) {
                level.setBlockAndUpdate(
                    pos,
                    ModBlocks.FIRE_CAULDRON
                        .getDefaultState()
                        .setValue(
                            LayeredCauldronBlock.LEVEL,
                            level.getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL)
                        )
                );
                itemEntity.getItem().setCount(itemEntity.getItem().getCount() - 1);
                return;
            }
            if (itemEntity.getItem().is(ModItemTags.UNBROKEN_FIRE_STARTER)) {
                level.setBlockAndUpdate(
                    pos,
                    ModBlocks.FIRE_CAULDRON
                        .getDefaultState()
                        .setValue(
                            LayeredCauldronBlock.LEVEL,
                            level.getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL)
                        )
                );
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack pStack,
        BlockState pState,
        Level pLevel,
        BlockPos pPos,
        Player pPlayer,
        InteractionHand pHand,
        BlockHitResult pHitResult) {
        return Util.interactionResultConverter().apply(this.use(pState, pLevel, pPos, pPlayer, pHand, pHitResult));
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
        return this.use(pState, pLevel, pPos, pPlayer, InteractionHand.MAIN_HAND, pHitResult);
    }

    /**
     *
     */
    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.is(Items.FLINT_AND_STEEL)) {
            level.setBlockAndUpdate(
                pos,
                ModBlocks.FIRE_CAULDRON
                    .getDefaultState()
                    .setValue(
                        LayeredCauldronBlock.LEVEL,
                        level.getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL)));
            itemStack.hurtAndBreak(2, player, Util.convertToSlot(hand));
            return InteractionResult.SUCCESS;
        }
        if (itemStack.is(Items.FIRE_CHARGE)) {
            level.setBlockAndUpdate(
                pos,
                ModBlocks.FIRE_CAULDRON
                    .getDefaultState()
                    .setValue(
                        LayeredCauldronBlock.LEVEL,
                        level.getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL)));
            itemStack.setCount(0);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
