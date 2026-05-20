package dev.dubhe.anvilcraft.item.property.consume;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModConsumeEffects;
import dev.dubhe.anvilcraft.item.property.component.BoxContents;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.level.Level;

public class TryTotemsInBoxConsumeEffect implements ConsumeEffect {
    public static final TryTotemsInBoxConsumeEffect INSTANCE = new TryTotemsInBoxConsumeEffect();
    public static final MapCodec<TryTotemsInBoxConsumeEffect> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<ByteBuf, TryTotemsInBoxConsumeEffect> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends ConsumeEffect> getType() {
        return ModConsumeEffects.TRY_TOTEMS_IN_BOX.get();
    }

    @Override
    public boolean apply(Level level, ItemStack stack, LivingEntity user) {
        if (!stack.has(ModComponents.BOX_CONTENTS)) return false;
        BoxContents.Mutable boxContents = stack.get(ModComponents.BOX_CONTENTS).mutable();
        for (ItemStack item : boxContents.getAmulets()) {
            if (!item.has(DataComponents.DEATH_PROTECTION)) continue;
            item.get(DataComponents.DEATH_PROTECTION).applyEffects(stack, user);
            return true;
        }
        for (ItemStack item : boxContents.getTotems()) {
            if (!item.has(DataComponents.DEATH_PROTECTION)) continue;
            item.get(DataComponents.DEATH_PROTECTION).applyEffects(stack, user);
            item.shrink(1);
            boxContents.purge();
            stack.set(ModComponents.BOX_CONTENTS, boxContents.immutable());
            return true;
        }
        return false;
    }
}
