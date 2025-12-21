package dev.dubhe.anvilcraft.init.sc;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.sc.upgrade.EntryLimitUpgrade;
import dev.dubhe.anvilcraft.api.sc.upgrade.IUpgrade;
import dev.dubhe.anvilcraft.api.sc.upgrade.StackPowerUpgrade;
import dev.dubhe.anvilcraft.api.sc.upgrade.TransferUpgrade;
import dev.dubhe.anvilcraft.init.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModUpgradeTypes {
    private static final DeferredRegister<IUpgrade.Type<?>> REGISTER = DeferredRegister.create(
        ModRegistries.UPGRADE_TYPE_KEY,
        AnvilCraft.MOD_ID
    );

    public static final DeferredHolder<IUpgrade.Type<?>, StackPowerUpgrade.Type> STACK_POWER = REGISTER
        .register("stack_power", StackPowerUpgrade.Type::new);

    public static final DeferredHolder<IUpgrade.Type<?>, EntryLimitUpgrade.Type> ENTRY_LIMIT = REGISTER
        .register("entry_limit", EntryLimitUpgrade.Type::new);

    public static final DeferredHolder<IUpgrade.Type<?>, TransferUpgrade.Type> TRANSFER = REGISTER
        .register("transfer", TransferUpgrade.Type::new);

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
