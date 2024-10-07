package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.block.CementCauldronBlock;
import dev.dubhe.anvilcraft.block.FireCauldronBlock;
import dev.dubhe.anvilcraft.block.OilCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LayeredCauldronBlock;

public class ModInteractionMap {
    public static final CauldronInteraction.InteractionMap LAYERED_LAVA = CauldronInteraction.newInteractionMap("layered_lava");
    public static final CauldronInteraction.InteractionMap OIL = CauldronInteraction.newInteractionMap("oil");
    public static final CauldronInteraction.InteractionMap CEMENT = CauldronInteraction.newInteractionMap("cement");

    public static void initInteractionMap() {
        var oilInteractionMap = OIL.map();
        oilInteractionMap.put(
            Items.BUCKET,
            (state, level, pos, player, hand, stack) -> CauldronInteraction.fillBucket(
                state,
                level,
                pos,
                player,
                hand,
                stack,
                ModItems.OIL_BUCKET.asStack(),
                (s) -> s.getValue(OilCauldronBlock.LEVEL) == 3,
                SoundEvents.BUCKET_FILL
            )
        );
        oilInteractionMap.put(
            Items.FLINT_AND_STEEL,
            (state, level, pos, player, hand, stack) -> {
                level.setBlockAndUpdate(
                    pos,
                    ModBlocks.FIRE_CAULDRON.getDefaultState()
                        .setValue(FireCauldronBlock.LEVEL, state.getValue(OilCauldronBlock.LEVEL))
                );
                stack.hurtAndBreak(2, player, LivingEntity.getSlotForHand(hand));
                level.playSound(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS);
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        );
        oilInteractionMap.put(
            Items.FIRE_CHARGE,
            (state, level, pos, player, hand, stack) -> {
                level.setBlockAndUpdate(
                    pos,
                    ModBlocks.FIRE_CAULDRON.getDefaultState()
                        .setValue(FireCauldronBlock.LEVEL, state.getValue(OilCauldronBlock.LEVEL))
                );
                stack.shrink(1);
                level.playSound(player, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS);
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        );

        var layeredLavaInteractionMap = LAYERED_LAVA.map();
        layeredLavaInteractionMap.put(
            Items.BUCKET,
            (blockState, level, blockPos, player, interactionHand, itemStack) -> CauldronInteraction.fillBucket(
                blockState,
                level,
                blockPos,
                player,
                interactionHand,
                itemStack,
                Items.LAVA_BUCKET.getDefaultInstance(),
                (state) -> state.getValue(LayeredCauldronBlock.LEVEL) == 3,
                SoundEvents.BUCKET_FILL
            )
        );

        var cementInteractionMap = ModInteractionMap.CEMENT.map();
        cementInteractionMap.put(
            Items.BUCKET,
            (state, level, pos, player, hand, stack) -> {
                if (level.getBlockState(pos).getBlock() instanceof CementCauldronBlock cauldronBlock) {
                    Color color = cauldronBlock.getColor();
                    return CauldronInteraction.fillBucket(state, level, pos, player, hand, stack, ModItems.CEMENT_BUCKETS.get(color).asStack(), (s) -> true, SoundEvents.BUCKET_FILL);
                }
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
        );

        var emptyInteractionMap = CauldronInteraction.EMPTY.map();
        ModItems.CEMENT_BUCKETS.forEach((k, v) -> emptyInteractionMap.put(
                v.get(),
                (state, level, pos, player, hand, stack) -> CauldronInteraction.emptyBucket(
                    level,
                    pos,
                    player,
                    hand,
                    stack,
                    ModBlocks.CEMENT_CAULDRONS.get(k).getDefaultState(),
                    SoundEvents.BUCKET_EMPTY
                )
            )
        );
        emptyInteractionMap.put(
            ModItems.OIL_BUCKET.get(),
            (state, level, pos, player, hand, stack) -> CauldronInteraction.emptyBucket(
                level,
                pos,
                player,
                hand,
                stack,
                ModBlocks.OIL_CAULDRON.getDefaultState().setValue(OilCauldronBlock.LEVEL, 3),
                SoundEvents.BUCKET_EMPTY
            )
        );
    }
}
