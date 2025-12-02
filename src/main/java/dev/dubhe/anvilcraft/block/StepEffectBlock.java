package dev.dubhe.anvilcraft.block;

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
        stepAction.accept(entity);
    }

    public static void stepOnChocolateBlock(Entity entity) {
        if (!(entity instanceof Player player)) return;
        if (entity.level().getGameTime() % EFFECT_PERIOD != 0) return;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_DURATION, 1, true, true));
    }

    public static void stepOnWhiteChocolateBlock(Entity entity) {
        if (!(entity instanceof Player player)) return;
        if (entity.level().getGameTime() % EFFECT_PERIOD != 0) return;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_DURATION, 0, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, EFFECT_DURATION, 0, true, true));
    }

    public static void stepOnBlackChocolateBlock(Entity entity) {
        if (!(entity instanceof Player player)) return;
        if (entity.level().getGameTime() % EFFECT_PERIOD != 0) return;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_DURATION, 0, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, EFFECT_DURATION, 0, true, true));
    }
}
