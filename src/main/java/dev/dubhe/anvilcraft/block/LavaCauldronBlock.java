package dev.dubhe.anvilcraft.block;

import com.google.common.collect.ImmutableMap;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LavaCauldronBlock extends LayeredCauldronBlock implements IHammerRemovable, BucketPickup {
    public static final CauldronInteraction.InteractionMap LAYERED_LAVA = new CauldronInteraction.InteractionMap(
        "layered_lava",
        new ImmutableMap.Builder<Item, CauldronInteraction>()
            .put(
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
            )
            .build()
    );


    public LavaCauldronBlock(Properties properties) {
        super(Biome.Precipitation.NONE, LAYERED_LAVA, properties);
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
    public void entityInside(
        BlockState state, Level level, BlockPos pos, Entity entity) {
        if (this.isEntityInsideContent(state, pos, entity)) {
            entity.lavaHurt();
        }
    }

    @Override
    public ItemStack getCloneItemStack(
        BlockState state,
        HitResult target,
        LevelReader level,
        BlockPos pos,
        Player player
    ) {
        return new ItemStack(Items.CAULDRON);
    }

    @Override
    public void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }
        if (state.getValue(LEVEL) == 3) {
            level.setBlockAndUpdate(pos, Blocks.LAVA_CAULDRON.defaultBlockState());
        }
    }

    @Override
    public ItemStack pickupBlock(@Nullable Player player, LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState) {
        if (blockState.getValue(LEVEL) == 3) {
            return Items.LAVA_BUCKET.getDefaultInstance();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.BUCKET_FILL_LAVA);
    }
}
