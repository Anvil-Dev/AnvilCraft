package dev.dubhe.anvilcraft.client.renderer.item;

import com.google.common.reflect.TypeToken;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.item.armor.WeatherproofChestplateItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import org.jspecify.annotations.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class EquipmentPoweredProperty implements ConditionalItemModelProperty {
    public static final EquipmentPoweredProperty INSTANCE = new EquipmentPoweredProperty();
    public static final MapCodec<EquipmentPoweredProperty> CODEC = MapCodec.unit(INSTANCE);
    public static final ContextKey<Boolean> IN_GRID = new ContextKey<>(AnvilCraft.of("equipment_in_grid"));

    @Override
    public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext context) {
        if (stack.getItem() instanceof WeatherproofChestplateItem) return WeatherproofChestplateItem.getEnergyStored(stack) > 0;
        return owner != null && owner.getData(ModDataAttachments.IN_POWER_GRID);
    }

    @Override
    public MapCodec<? extends ConditionalItemModelProperty> type() {
        return CODEC;
    }

    @SubscribeEvent
    public static void register(RegisterConditionalItemModelPropertyEvent event) {
        event.register(AnvilCraft.of("equipment_powered"), CODEC);
    }

    @SubscribeEvent
    public static void renderState(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
            (entity, state) -> state.setRenderData(IN_GRID, entity.getData(ModDataAttachments.IN_POWER_GRID)));
    }
}
