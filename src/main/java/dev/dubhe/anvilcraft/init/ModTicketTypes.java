package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.TicketType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModTicketTypes {
    private static final DeferredRegister<TicketType> REGISTER = DeferredRegister.create(Registries.TICKET_TYPE, AnvilCraft.MOD_ID);
    public static final Supplier<TicketType> DRAIN = REGISTER.register(
        "drain", () -> new TicketType(100, TicketType.FLAG_LOADING | TicketType.FLAG_CAN_EXPIRE_IF_UNLOADED)
    );

    private ModTicketTypes() {
    }

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
