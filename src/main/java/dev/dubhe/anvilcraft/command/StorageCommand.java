package dev.dubhe.anvilcraft.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.anvilcraft.lib.v2.util.ComponentUtil;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.StorageType;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import dev.dubhe.anvilcraft.util.CommandUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 存储信息与绑定命令：显示仓储方块/超维终端的存储 ID 与类型，
 * 将对应类型与手持的仓储方块/超维终端绑定。
 */
public class StorageCommand {
    private static final SimpleCommandExceptionType ERROR_NO_HAND_ITEM = new SimpleCommandExceptionType(
        Component.translatable("command.anvilcraft.storage.no_hand_item")
    );
    private static final SimpleCommandExceptionType ERROR_NO_STORAGE = new SimpleCommandExceptionType(
        Component.translatable("command.anvilcraft.storage.no_storage")
    );
    private static final SimpleCommandExceptionType ERROR_INVALID_TYPE = new SimpleCommandExceptionType(
        Component.translatable("command.anvilcraft.storage.invalid_type")
    );
    private static final SimpleCommandExceptionType ERROR_INVALID_ID = new SimpleCommandExceptionType(
        Component.translatable("command.anvilcraft.storage.invalid_id")
    );

    public static void registerCommand(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(
            Commands.literal("storage")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(StorageCommand::storageInfo)
                .then(Commands.literal("info").executes(StorageCommand::storageInfo))
                .then(
                    Commands.literal("list")
                        .executes(StorageCommand::storageList)
                        .then(
                            Commands.argument("type", StringArgumentType.greedyString())
                                .suggests(StorageCommand::suggestStorageTypes)
                                .executes(StorageCommand::storageListFiltered)
                        )
                )
                .then(
                    Commands.literal("bind")
                        .then(
                            Commands.argument("id", StringArgumentType.word())
                                .suggests(StorageCommand::suggestStorageIds)
                                .executes(StorageCommand::storageBind)
                        )
                )
                .then(Commands.literal("unbind").executes(StorageCommand::storageUnbind))
        );
    }

    /** 显示手持物品的存储信息（仓储方块 BlockItem 的 StorageRef，或超维终端的绑定）。 */
    private static int storageInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack stack = ctx.getSource().getPlayerOrException().getMainHandItem();
        if (stack.isEmpty()) throw StorageCommand.ERROR_NO_HAND_ITEM.create();

        MutableComponent message = Component.translatable(
            "command.anvilcraft.storage.info.item",
            stack.getHoverName()
        ).withStyle(ChatFormatting.LIGHT_PURPLE);

        if (stack.is(ModItems.HYPERDIMENSION_TERMINAL)) {
            TerminalBinding binding = stack.getOrDefault(ModComponents.TERMINAL_BINDING, TerminalBinding.EMPTY);
            message.append(ComponentUtil.LF).append(Component.translatable(
                "command.anvilcraft.storage.info.terminal",
                binding.id().map(id -> Component.literal(id.toString()))
                    .orElseGet(() -> Component.translatable("command.anvilcraft.storage.info.none"))
            ));
        } else {
            StorageRef ref = stack.get(ModComponents.STORAGE);
            if (ref == null) {
                throw StorageCommand.ERROR_NO_STORAGE.create();
            }
            message.append(ComponentUtil.LF).append(Component.translatable(
                "command.anvilcraft.storage.info.ref",
                Component.literal(StorageCommand.typeName(ref.type())),
                ref.id().map(id -> Component.literal(id.toString()))
                    .orElseGet(() -> Component.translatable("command.anvilcraft.storage.info.none"))
            ));
        }
        return CommandUtil.sendSuccess(ctx.getSource(), () -> message);
    }

    /** 列出所有存储（可选按类型过滤）。 */
    private static int storageList(CommandContext<CommandSourceStack> ctx) {
        return StorageCommand.storageListFiltered(ctx, null);
    }

    private static int storageListFiltered(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String typeName = StringArgumentType.getString(ctx, "type");
        return StorageCommand.storageListFiltered(ctx, StorageCommand.parseType(typeName));
    }

    private static int storageListFiltered(CommandContext<CommandSourceStack> ctx, @Nullable StorageType filter) {
        Map<UUID, BaseStorage<?>> storages = Storages.get().getStorages();
        MutableComponent message = Component.translatable(
            "command.anvilcraft.storage.list.head",
            storages.size()
        ).withStyle(ChatFormatting.LIGHT_PURPLE);
        for (Map.Entry<UUID, BaseStorage<?>> entry : storages.entrySet()) {
            StorageType type = StorageType.find(entry.getValue());
            if (filter != null && !type.equals(filter)) continue;
            message.append(ComponentUtil.LF).append(Component.translatable("command.anvilcraft.storage.list.entry",
                Component.literal(StorageCommand.typeName(type)),
                Component.literal(entry.getKey().toString())
            ));
        }
        return CommandUtil.sendSuccess(ctx.getSource(), () -> message);
    }

    /** 将手持的仓储方块/超维终端绑定到指定存储 id（类型从手持物品推断）。 */
    private static int storageBind(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        UUID id = StorageCommand.parseUuid(StringArgumentType.getString(ctx, "id"));
        if (id == null) throw StorageCommand.ERROR_INVALID_ID.create();
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) throw StorageCommand.ERROR_NO_HAND_ITEM.create();

        StorageType type;
        if (stack.is(ModItems.HYPERDIMENSION_TERMINAL)) {
            // 终端绑定到存储 id（类型仅用于显示）
            type = StorageType.HYPERDIMENSION;
            stack.set(ModComponents.TERMINAL_BINDING, new TerminalBinding(Optional.of(id)));
        } else {
            StorageRef existing = stack.get(ModComponents.STORAGE);
            if (existing == null) throw StorageCommand.ERROR_NO_STORAGE.create();
            type = existing.type();
            stack.set(ModComponents.STORAGE, new StorageRef(type, id));
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        return CommandUtil.sendSuccess(
            ctx.getSource(),
            "command.anvilcraft.storage.bind.success",
            Component.literal(StorageCommand.typeName(type)),
            Component.literal(id.toString())
        );
    }

    /** 清除手持仓储方块或超维终端的绑定。 */
    private static int storageUnbind(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) throw StorageCommand.ERROR_NO_HAND_ITEM.create();
        if (stack.is(ModItems.HYPERDIMENSION_TERMINAL)) {
            stack.remove(ModComponents.TERMINAL_BINDING);
        } else {
            StorageRef ref = stack.get(ModComponents.STORAGE);
            if (ref == null) throw StorageCommand.ERROR_NO_STORAGE.create();
            stack.set(ModComponents.STORAGE, new StorageRef(ref.type()));
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        return CommandUtil.sendSuccess(ctx.getSource(), "command.anvilcraft.storage.unbind.success");
    }

    private static String typeName(StorageType type) {
        return "anvilcraft:" + type.getSerializedName();
    }

    private static StorageType parseType(String name) throws CommandSyntaxException {
        Identifier id = Identifier.tryParse(name.contains(":") ? name : "anvilcraft:" + name);
        if (id != null) {
            for (StorageType type : StorageType.values()) {
                if (StorageCommand.typeName(type).equals(id.toString())) return type;
            }
        }
        throw StorageCommand.ERROR_INVALID_TYPE.create();
    }

    private static @Nullable UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 建议已注册的存储类型名（如 anvilcraft:crate）。 */
    private static CompletableFuture<Suggestions> suggestStorageTypes(
        CommandContext<CommandSourceStack> ctx,
        SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggest(
            Arrays.stream(StorageType.values()).map(StorageCommand::typeName),
            builder
        );
    }

    /** 建议现存存储的 UUID。 */
    private static CompletableFuture<Suggestions> suggestStorageIds(
        CommandContext<CommandSourceStack> ctx,
        SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggest(
            Storages.get().getStorages().keySet().stream().map(UUID::toString),
            builder
        );
    }
}
