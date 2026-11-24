package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModAttributes {
    private static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, AnvilCraft.MOD_ID);

    public static final DeferredHolder<Attribute, Attribute> POCKET_COUNT = ATTRIBUTES.register(
        "pocket_count",
        () -> new RangedAttribute(
            "attribute.name.anvilcraft.pocket_count",
            0.0,
            0.0,
            PocketInventory.MAX_SIZE
        ).setSyncable(true)
    );

    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
        eventBus.addListener(ModAttributes::addPlayerAttributes);
    }

    private static void addPlayerAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, POCKET_COUNT);
    }
}
