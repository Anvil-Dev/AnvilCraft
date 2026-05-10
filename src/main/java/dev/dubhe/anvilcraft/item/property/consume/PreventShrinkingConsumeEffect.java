package dev.dubhe.anvilcraft.item.property.consume;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.init.item.ModConsumeEffects;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.level.Level;

public class PreventShrinkingConsumeEffect implements ConsumeEffect {
    public static final ThreadLocal<InteractionHand> USED_HAND = new ThreadLocal<>();
    public static final PreventShrinkingConsumeEffect INSTANCE = new PreventShrinkingConsumeEffect();
    public static final MapCodec<PreventShrinkingConsumeEffect> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<ByteBuf, PreventShrinkingConsumeEffect> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends ConsumeEffect> getType() {
        return ModConsumeEffects.PREVENT_SHRINKING.get();
    }

    @Override
    public boolean apply(Level level, ItemStack stack, LivingEntity user) {
        InteractionHand hand = PreventShrinkingConsumeEffect.USED_HAND.get();
        if (hand == null) return false;
        ItemStack inHand = user.getItemInHand(hand);
        if (!inHand.is(stack.getItem())) {
            user.setItemInHand(hand, stack.copyWithCount(1));
        } else if (!inHand.isEmpty()) {
            inHand.grow(1);
        }
        return true;
    }
}
