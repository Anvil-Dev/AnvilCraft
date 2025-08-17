package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModBuiltinPacks {
    public static final PackSource BUILT_IN = PackSource.create(decorateWithSource("pack.source.builtin"), false);

    @SubscribeEvent
    public static void packSetup(@NotNull AddPackFindersEvent event) {
        event.addPackFinders(
            AnvilCraft.of("resourcepacks/transparent_cauldron"),
            PackType.CLIENT_RESOURCES,
            Component.translatable("pack.anvilcraft.builtin_pack"),
            ModBuiltinPacks.BUILT_IN,
            false,
            Pack.Position.TOP
        );
        event.addPackFinders(
            AnvilCraft.of("resourcepacks/first_ancient_debris"),
            PackType.SERVER_DATA,
            Component.translatable("pack.anvilcraft.builtin_data_pack"),
            ModBuiltinPacks.BUILT_IN,
            false,
            Pack.Position.TOP
        );
    }

    @SuppressWarnings("SameParameterValue")
    private static @NotNull UnaryOperator<Component> decorateWithSource(String translationKey) {
        Component component = Component.translatable(translationKey);
        return component1 -> Component.translatable("pack.nameAndSource", component1, component).withStyle(ChatFormatting.GRAY);
    }
}
