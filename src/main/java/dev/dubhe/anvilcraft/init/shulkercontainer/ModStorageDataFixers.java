package dev.dubhe.anvilcraft.init.shulkercontainer;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.container.datafixer.StorageDataFixer;
import dev.dubhe.anvilcraft.init.ModRegistries;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

public class ModStorageDataFixers {
    private static final DeferredRegister<StorageDataFixer> REGISTER = DeferredRegister.create(
        ModRegistries.FIXER_KEY,
        AnvilCraft.MOD_ID
    );

    public static final DeferredHolder<StorageDataFixer, StorageDataFixer> V1 = ModStorageDataFixers.register(
        1.0, tag -> tag
    );

    private static DeferredHolder<StorageDataFixer, StorageDataFixer> register(
        double version,
        UnaryOperator<CompoundTag> fixer
    ) {
        String name = Double.toString(version).replace('.', '_');
        return REGISTER.register(
            name,
            () -> new StorageDataFixer(AnvilCraft.of(name), version, fixer)
        );
    }

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
