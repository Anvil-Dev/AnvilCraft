package dev.dubhe.anvilcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;

public class RedhotMetalBlock extends Block {
    private final float steppingHarmAmount;

    public RedhotMetalBlock(Properties properties) {
        super(properties);
        this.steppingHarmAmount = 1;
    }

    public RedhotMetalBlock(Properties properties, float steppingHarmAmount) {
        super(properties);
        this.steppingHarmAmount = steppingHarmAmount;
    }

    @Override
    public void stepOn(
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull BlockState state,
            @NotNull Entity entity) {
        if (!entity.isSteppingCarefully()
                && entity instanceof LivingEntity living
                && !EnchantmentHelper.hasFrostWalker(living)) {
            if (entity.hurt(level.damageSources().hotFloor(), steppingHarmAmount)) {
                entity.playSound(
                        SoundEvents.GENERIC_BURN, 0.4F, 2.0F + living.getRandom().nextFloat() * 0.4F);
            }
        }
        super.stepOn(level, pos, state, entity);
    }
}
