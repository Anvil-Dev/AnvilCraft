package dev.dubhe.anvilcraft.item.property.consume;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.init.item.ModConsumeEffects;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.level.Level;

public class SetRagedConsumeEffect implements ConsumeEffect {
    public static final SetRagedConsumeEffect INSTANCE = new SetRagedConsumeEffect();
    public static final MapCodec<SetRagedConsumeEffect> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<ByteBuf, SetRagedConsumeEffect> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    
    @Override
    public Type<? extends ConsumeEffect> getType() {
        return ModConsumeEffects.SET_RAGED.get();
    }

    @Override
    public boolean apply(Level level, ItemStack stack, LivingEntity user) {
        user.anvilcraft$setRaged();
        return true;
    }
}
