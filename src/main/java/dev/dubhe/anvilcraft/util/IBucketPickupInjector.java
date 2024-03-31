package dev.dubhe.anvilcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@SuppressWarnings("unused")
public interface IBucketPickupInjector extends BucketPickup {
    @Override
    default @NotNull ItemStack pickupBlock(LevelAccessor level, BlockPos pos, @NotNull BlockState state) {
        return Items.BUCKET.getDefaultInstance();
    }

    @Override
    default @NotNull Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.EMPTY);
    }
}
