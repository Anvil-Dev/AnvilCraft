package dev.dubhe.anvilcraft.init.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.def.AmuletDefinition;
import dev.dubhe.anvilcraft.api.amulet.def.IAmuletDefinition;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.item.amulet.def.AbnormalAmuletDefinition;
import dev.dubhe.anvilcraft.item.amulet.def.ComradeAmuletDefinition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModAmuletDefinitionTypes {
    private static final DeferredRegister<IAmuletDefinition.Type<?>> REGISTER = DeferredRegister.create(
        ModRegistryKeys.AMULET_DEF_TYPE,
        AnvilCraft.MOD_ID
    );

    public static final DeferredHolder<IAmuletDefinition.Type<?>, AmuletDefinition.Type> NORMAL = REGISTER.register(
        "normal",
        AmuletDefinition.Type::new
    );
    public static final DeferredHolder<IAmuletDefinition.Type<?>, ComradeAmuletDefinition.Type> COMRADE = REGISTER.register(
        "comrade",
        ComradeAmuletDefinition.Type::new
    );
    public static final DeferredHolder<IAmuletDefinition.Type<?>, AbnormalAmuletDefinition.Type> ABNORMAL = REGISTER.register(
        "abnormal",
        AbnormalAmuletDefinition.Type::new
    );

    public static void register(IEventBus modEventBus) {
        ModAmuletDefinitionTypes.REGISTER.register(modEventBus);
    }
}
