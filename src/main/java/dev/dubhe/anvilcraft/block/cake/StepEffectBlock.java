package dev.dubhe.anvilcraft.block.cake;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

public class StepEffectBlock extends Block {
    private final Consumer<Entity> stepAction;

    public StepEffectBlock(Properties properties, Consumer<Entity> stepAction) {
        super(properties);
        this.stepAction = stepAction;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        this.stepAction.accept(entity);
    }

    public static void stepOnChocolateBlock(Entity entity) {
        if (!(entity instanceof Player player)) return;
        if (entity.level().isClientSide()) return;
        int period = AnvilCraft.CONFIG.world.chocolateBlockEffectPeriod;
        int duration = AnvilCraft.CONFIG.world.chocolateBlockEffectDuration;
        if (entity.level().getGameTime() % period != 0) return;
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 9, true, true));
    }

    public static void stepOnBlackChocolateBlock(Entity entity) {
        if (!(entity instanceof Player player)) return;
        if (entity.level().isClientSide()) return;
        int period = AnvilCraft.CONFIG.world.chocolateBlockEffectPeriod;
        int duration = AnvilCraft.CONFIG.world.chocolateBlockEffectDuration;
        if (entity.level().getGameTime() % period != 0) return;
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 4, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.HASTE, duration, 3, true, true));
    }

    public static void stepOnWhiteChocolateBlock(Entity entity) {
        if (!(entity instanceof Player player)) return;
        if (entity.level().isClientSide()) return;
        int period = AnvilCraft.CONFIG.world.chocolateBlockEffectPeriod;
        int duration = AnvilCraft.CONFIG.world.chocolateBlockEffectDuration;
        if (entity.level().getGameTime() % period != 0) return;
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 4, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, duration, 5, true, true));
    }

    public static void stepOnBlackWhiteChocolateBlock(Entity entity) {
        if (!(entity instanceof Player player)) return;
        if (entity.level().isClientSide()) return;
        int period = AnvilCraft.CONFIG.world.chocolateBlockEffectPeriod;
        int duration = AnvilCraft.CONFIG.world.chocolateBlockEffectDuration;
        if (entity.level().getGameTime() % period != 0) return;
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 4, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.HASTE, duration, 3, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, duration, 5, true, true));
    }
}
