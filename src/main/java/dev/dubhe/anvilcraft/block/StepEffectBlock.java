package dev.dubhe.anvilcraft.block;

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
        stepAction.accept(entity);
    }

    public static void stepOnChocolateBlock(Entity entity) {
        if (!(entity instanceof Player player)) return;
        // 仅在服务端施加效果，客户端通过数据包同步，避免客户端残留无法清除的幽灵效果
        if (entity.level().isClientSide()) return;
        int period = AnvilCraft.CONFIG.world.chocolateBlockEffectPeriod;
        int duration = AnvilCraft.CONFIG.world.chocolateBlockEffectDuration;
        if (entity.level().getGameTime() % period != 0) return;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 9, true, true));
    }

    public static void stepOnBlackChocolateBlock(Entity entity) {
        if (!(entity instanceof Player player)) return;
        if (entity.level().isClientSide()) return;
        int period = AnvilCraft.CONFIG.world.chocolateBlockEffectPeriod;
        int duration = AnvilCraft.CONFIG.world.chocolateBlockEffectDuration;
        if (entity.level().getGameTime() % period != 0) return;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 4, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, duration, 3, true, true));
    }

    public static void stepOnWhiteChocolateBlock(Entity entity) {
        if (!(entity instanceof Player player)) return;
        if (entity.level().isClientSide()) return;
        int period = AnvilCraft.CONFIG.world.chocolateBlockEffectPeriod;
        int duration = AnvilCraft.CONFIG.world.chocolateBlockEffectDuration;
        if (entity.level().getGameTime() % period != 0) return;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 4, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, 5, true, true));
    }

    public static void stepOnBlackWhiteChocolateBlock(Entity entity) {
        if (!(entity instanceof Player player)) return;
        if (entity.level().isClientSide()) return;
        int period = AnvilCraft.CONFIG.world.chocolateBlockEffectPeriod;
        int duration = AnvilCraft.CONFIG.world.chocolateBlockEffectDuration;
        if (entity.level().getGameTime() % period != 0) return;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 4, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, duration, 3, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, 5, true, true));
    }
}
