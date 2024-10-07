package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.util.ModInteractionMap;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class OilCauldronBlock extends LayeredCauldronBlock implements IHammerRemovable, BucketPickup {
    public OilCauldronBlock(Properties properties) {
        super(Biome.Precipitation.NONE, ModInteractionMap.OIL, properties);
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
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        CauldronInteraction interaction = this.interactions.map().get(stack.getItem());
        if (interaction == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return interaction.interact(state, level, pos, player, hand, stack);
    }

    @Override
    public ItemStack pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state) {
        if (state.is(ModBlocks.OIL_CAULDRON.get()) && state.getValue(LayeredCauldronBlock.LEVEL) == 3) {
            level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
            return ModItems.OIL_BUCKET.asStack();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.BUCKET_FILL_LAVA);
    }
}
