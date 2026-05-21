package dev.dubhe.anvilcraft.item.property.consume;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.item.ModConsumeEffects;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.level.Level;

public record SetFoodLevelConsumeEffect(int level) implements ConsumeEffect {
    public static final MapCodec<SetFoodLevelConsumeEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        Codec.INT
            .fieldOf("level")
            .forGetter(SetFoodLevelConsumeEffect::level)
    ).apply(inst, SetFoodLevelConsumeEffect::new));
    public static final StreamCodec<ByteBuf, SetFoodLevelConsumeEffect> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        SetFoodLevelConsumeEffect::level,
        SetFoodLevelConsumeEffect::new
    );

    @Override
    public Type<SetFoodLevelConsumeEffect> getType() {
        return ModConsumeEffects.SET_FOOD_LEVEL.get();
    }

    @Override
    public boolean apply(Level level, ItemStack stack, LivingEntity user) {
        if (!(user instanceof ServerPlayer player)) return false;
        player.getFoodData().setFoodLevel(this.level);
        return true;
    }
}
