package dev.dubhe.anvilcraft.init.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.display.WithAnyPotionsExcept;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSlotDisplays {
    private static final DeferredRegister<SlotDisplay.Type<?>> DF = DeferredRegister.create(Registries.SLOT_DISPLAY, AnvilCraft.MOD_ID);

    public static final DeferredHolder<SlotDisplay.Type<?>, SlotDisplay.Type<WithAnyPotionsExcept>> WITH_ANY_POTION_EXCEPT = DF
        .register("with_any_potion_except", () -> WithAnyPotionsExcept.TYPE);

    public static void register(IEventBus modEventBus) {
        DF.register(modEventBus);
    }
}
