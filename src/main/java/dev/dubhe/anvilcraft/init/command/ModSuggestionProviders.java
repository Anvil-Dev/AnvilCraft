package dev.dubhe.anvilcraft.init.command;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.saved.multiphase.Multiphases;
import dev.dubhe.anvilcraft.saved.sc.client.ClientSCStorages;
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

    public static final SuggestionProvider<CommandSourceStack> ALL_SC_STORAGES_ID = SuggestionProviders.register(
        AnvilCraft.of("all_sc_storages_id"),
        (ctx, builder) -> {
            for (UUID id : ClientSCStorages.getIDs()) {
                if (id == null) continue;
                builder.suggest(id.toString());
            }
            return builder.buildFuture();
        }
    );
    public static final SuggestionProvider<CommandSourceStack> ALL_RECOVERABLE_SC_STORAGES_ID = SuggestionProviders.register(
        AnvilCraft.of("all_recoverable_sc_storages_id"),
        (ctx, builder) -> {
            for (UUID id : ClientSCStorages.getRecoverableIDs()) {
                if (id == null) continue;
                builder.suggest(id.toString());
            }
            return builder.buildFuture();
        }
    );

    public static void register() {
    }
}
