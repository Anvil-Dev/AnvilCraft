package dev.dubhe.anvilcraft.init.command;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.container.ContainerStorages;
import dev.dubhe.anvilcraft.saved.multiphase.Multiphases;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.synchronization.SuggestionProviders;

import java.util.UUID;

public class ModSuggestionProviders {
    public static final SuggestionProvider<CommandSourceStack> ALL_MULTIPHASES_ID = SuggestionProviders.register(
        AnvilCraft.of("all_multiphases_id"),
        (ctx, builder) -> {
            for (UUID id : Multiphases.get().getIDs()) {
                if (id == null) continue;
                builder.suggest(id.toString());
            }
            return builder.buildFuture();
        }
    );
    public static final SuggestionProvider<CommandSourceStack> ALL_RECOVERABLE_MULTIPHASES_ID = SuggestionProviders.register(
        AnvilCraft.of("all_recoverable_multiphases_id"),
        (ctx, builder) -> {
            for (UUID id : Multiphases.get().getRecoverableIDs()) {
                if (id == null) continue;
                builder.suggest(id.toString());
            }
            return builder.buildFuture();
        }
    );

    public static final SuggestionProvider<CommandSourceStack> ALL_SHULKER_CONTAINERS_ID = SuggestionProviders.register(
        AnvilCraft.of("all_shulker_containers_id"),
        (ctx, builder) -> {
            for (UUID containerID : ContainerStorages.get().getContainerIDs()) {
                builder.suggest(containerID.toString());
            }
            return builder.buildFuture();
        }
    );
    public static final SuggestionProvider<CommandSourceStack> ALL_RECOVERABLE_SHULKER_CONTAINERS_ID = SuggestionProviders.register(
        AnvilCraft.of("all_recoverable_shulker_containers_id"),
        (ctx, builder) -> {
            for (UUID containerID : ContainerStorages.get().getRecoverableContainerIDs()) {
                builder.suggest(containerID.toString());
            }
            return builder.buildFuture();
        }
    );

    public static void register() {
    }
}
