package dev.dubhe.anvilcraft.item.property;

import dev.dubhe.anvilcraft.item.IonoCraftBackpackItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class FlightTimePropertyFunction implements ClampedItemPropertyFunction {
    @Override
    public float unclampedCall(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        if (IonoCraftBackpackItem.getFlightTime(stack) > 0) {
            return 1;
        }
        return 0;
    }
}
