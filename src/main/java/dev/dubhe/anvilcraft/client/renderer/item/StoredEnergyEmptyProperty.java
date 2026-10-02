package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import org.jspecify.annotations.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class StoredEnergyEmptyProperty implements ConditionalItemModelProperty {
    public static final StoredEnergyEmptyProperty INSTANCE = new StoredEnergyEmptyProperty();
    public static final MapCodec<StoredEnergyEmptyProperty> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext context) {
        return stack.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value() == 0;
    }

    @Override
    public MapCodec<? extends ConditionalItemModelProperty> type() {
        return CODEC;
    }

    @SubscribeEvent
    public static void register(RegisterConditionalItemModelPropertyEvent event) {
        event.register(AnvilCraft.of("stored_energy_empty"), CODEC);
    }
}
