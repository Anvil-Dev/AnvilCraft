package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class OilCauldronBlock extends LayeredCauldronBlock implements IHammerRemovable {
    public OilCauldronBlock(Properties properties) {
        super(properties, p -> false, CauldronInteraction.EMPTY);
    }

    @Override
    public void entityInside(
            @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Entity entity
    ) {
        if (entity.getType().equals(EntityType.ARROW) && entity.isOnFire()) {
            level.setBlockAndUpdate(
                    pos,
                    ModBlocks.FIRE_CAULDRON.getDefaultState().setValue(
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
                        ModBlocks.FIRE_CAULDRON.getDefaultState().setValue(
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
                        ModBlocks.FIRE_CAULDRON.getDefaultState().setValue(
                                LayeredCauldronBlock.LEVEL,
                                level.getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL)
                        )
                );
            }
        }
    }

    @Override
    public @NotNull InteractionResult use(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull InteractionHand hand,
            @NotNull BlockHitResult hit
    ) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.is(Items.FLINT_AND_STEEL)) {
            level.setBlockAndUpdate(
                    pos,
                    ModBlocks.FIRE_CAULDRON.getDefaultState().setValue(
                            LayeredCauldronBlock.LEVEL,
                            level.getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL)
                    )
            );
            itemStack.hurtAndBreak(2, player, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            return InteractionResult.SUCCESS;
        }
        if (itemStack.is(Items.FIRE_CHARGE)) {
            level.setBlockAndUpdate(
                    pos,
                    ModBlocks.FIRE_CAULDRON.getDefaultState().setValue(
                            LayeredCauldronBlock.LEVEL,
                            level.getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL)
                    )
            );
            itemStack.setCount(0);
            return InteractionResult.SUCCESS;
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(
        @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull BlockState state
    ) {
        return new ItemStack(Items.CAULDRON);
    }
}
