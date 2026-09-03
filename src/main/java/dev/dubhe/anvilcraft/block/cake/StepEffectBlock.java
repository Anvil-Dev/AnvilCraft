package dev.dubhe.anvilcraft.block.cake;

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
    public static final int EFFECT_PERIOD = 80;
    public static final int EFFECT_DURATION = 180;

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
        // 仅在服务端施加效果，客户端通过数据包同步，避免客户端残留无法清除的幽灵效果
        if (entity.level().isClientSide()) return;
        if (entity.level().getGameTime() % StepEffectBlock.EFFECT_PERIOD != 0) return;
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, StepEffectBlock.EFFECT_DURATION, 9, true, true));
    }

    public static void stepOnBlackChocolateBlock(Entity entity) {
        if (!(entity instanceof Player player)) return;
        if (entity.level().isClientSide()) return;
        if (entity.level().getGameTime() % StepEffectBlock.EFFECT_PERIOD != 0) return;
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, StepEffectBlock.EFFECT_DURATION, 4, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.HASTE, StepEffectBlock.EFFECT_DURATION, 3, true, true));
    }

    public static void stepOnWhiteChocolateBlock(Entity entity) {
        if (!(entity instanceof Player player)) return;
        if (entity.level().isClientSide()) return;
        if (entity.level().getGameTime() % StepEffectBlock.EFFECT_PERIOD != 0) return;
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, StepEffectBlock.EFFECT_DURATION, 4, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, StepEffectBlock.EFFECT_DURATION, 5, true, true));
    }
}
