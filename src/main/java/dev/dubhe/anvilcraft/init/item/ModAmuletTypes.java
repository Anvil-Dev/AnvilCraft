package dev.dubhe.anvilcraft.init.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.item.property.component.amulet.AnvilAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.ComradeAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.DiscountAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.DoNothingAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.GiveEffectAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.IAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.ImmuneDamageAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.ImmuneEntityAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.WrappedOthersAmulet;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModAmuletTypes {
    private static final DeferredRegister<IAmulet.Type<?>> REGISTER = DeferredRegister.create(
        ModRegistryKeys.AMULET_TYPE,
        AnvilCraft.MOD_ID
    );

    public static final DeferredHolder<IAmulet.Type<?>, DiscountAmulet.Type> DISCOUNT = ModAmuletTypes.REGISTER.register(
        "discount",
        DiscountAmulet.Type::new
    );
    public static final DeferredHolder<IAmulet.Type<?>, ImmuneDamageAmulet.Type> IMMUNE_DAMAGE = ModAmuletTypes.REGISTER.register(
        "immune_damage",
        ImmuneDamageAmulet.Type::new
    );
    public static final DeferredHolder<IAmulet.Type<?>, ImmuneEntityAmulet.Type> IMMUNE_ENTITY = ModAmuletTypes.REGISTER.register(
        "immune_entity",
        ImmuneEntityAmulet.Type::new
    );
    public static final DeferredHolder<IAmulet.Type<?>, GiveEffectAmulet.Type> GIVE_EFFECT = ModAmuletTypes.REGISTER.register(
        "give_effect",
        GiveEffectAmulet.Type::new
    );
    public static final DeferredHolder<IAmulet.Type<?>, WrappedOthersAmulet.Type> WRAPPED_OTHERS = ModAmuletTypes.REGISTER.register(
        "wrapped_others",
        WrappedOthersAmulet.Type::new
    );
    public static final DeferredHolder<IAmulet.Type<?>, ComradeAmulet.Type> COMRADE = ModAmuletTypes.REGISTER.register(
        "comrade",
        ComradeAmulet.Type::new
    );
    public static final DeferredHolder<IAmulet.Type<?>, AnvilAmulet.Type> ANVIL = ModAmuletTypes.REGISTER.register(
        "anvil",
        AnvilAmulet.Type::new
    );
    public static final DeferredHolder<IAmulet.Type<?>, DoNothingAmulet.Type> DO_NOTHING = ModAmuletTypes.REGISTER.register(
        "do_nothing",
        DoNothingAmulet.Type::new
    );

    public static void register(IEventBus modEventBus) {
        ModAmuletTypes.REGISTER.register(modEventBus);
    }
}
