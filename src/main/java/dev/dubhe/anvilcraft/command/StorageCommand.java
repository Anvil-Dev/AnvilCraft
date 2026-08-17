package dev.dubhe.anvilcraft.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

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
            literal("storage")
                .requires(source -> source.hasPermission(2))
                .executes(StorageCommand::storageInfo)
                .then(literal("info").executes(StorageCommand::storageInfo))
                .then(
                    literal("list")
                        .executes(StorageCommand::storageList)
                        .then(
                            argument("type", StringArgumentType.word())
                                .executes(StorageCommand::storageListFiltered)
                        )
                )
                .then(
                    literal("bind")
                        .then(
                            argument("id", StringArgumentType.word())
                                .executes(StorageCommand::storageBind)
                        )
                )
                .then(literal("unbind").executes(StorageCommand::storageUnbind))
        );
    }

    /** 显示手持物品的存储信息（仓储方块 BlockItem 的 StorageRef，或超维终端的绑定）。 */
    private static int storageInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ItemStack stack = context.getSource().getPlayerOrException().getMainHandItem();
        if (stack.isEmpty()) throw ERROR_NO_HAND_ITEM.create();

        MutableComponent message = Component.translatable(
            "command.anvilcraft.storage.info.item",
            stack.getHoverName()
        ).withStyle(ChatFormatting.LIGHT_PURPLE);

        if (stack.is(ModItems.HYPERDIMENSION_TERMINAL)) {
            TerminalBinding binding = stack.getOrDefault(ModComponents.TERMINAL_BINDING, TerminalBinding.EMPTY);
            message.append(Component.literal("\n")).append(Component.translatable(
                "command.anvilcraft.storage.info.terminal",
                binding.id().map(id -> Component.literal(id.toString()))
                    .orElseGet(() -> Component.translatable("command.anvilcraft.storage.info.none"))
            ));
        } else {
            StorageRef ref = stack.get(ModComponents.STORAGE);
            if (ref == null) {
                throw ERROR_NO_STORAGE.create();
            }
            message.append(Component.literal("\n")).append(Component.translatable(
                "command.anvilcraft.storage.info.ref",
                Component.literal(ref.type().getSerializedName()),
                ref.id().map(id -> Component.literal(id.toString()))
                    .orElseGet(() -> Component.translatable("command.anvilcraft.storage.info.none"))
            ));
        }
        return CommandUtil.sendSuccess(context.getSource(), () -> message);
    }

    /** 列出所有存储（可选按类型过滤）。 */
    private static int storageList(CommandContext<CommandSourceStack> context) {
        return StorageCommand.storageListFiltered(context, null);
    }

    private static int storageListFiltered(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String typeName = StringArgumentType.getString(context, "type");
        StorageType type = parseType(typeName);
        if (type == null) throw ERROR_INVALID_TYPE.create();
        return StorageCommand.storageListFiltered(context, type);
    }

    private static int storageListFiltered(CommandContext<CommandSourceStack> context, @Nullable StorageType filter) {
        Map<UUID, BaseStorage<?>> storages = Storages.get().getStorages();
        MutableComponent message = Component.translatable(
            "command.anvilcraft.storage.list.head",
            storages.size()
        ).withStyle(ChatFormatting.LIGHT_PURPLE);
        for (Map.Entry<UUID, BaseStorage<?>> entry : storages.entrySet()) {
            StorageType type = StorageType.find(entry.getValue());
            if (filter != null && type != filter) continue;
            message.append(Component.literal("\n")).append(Component.translatable(
                "command.anvilcraft.storage.list.entry",
                Component.literal(type.getSerializedName()),
                Component.literal(entry.getKey().toString())
            ));
        }
        return CommandUtil.sendSuccess(context.getSource(), () -> message);
    }

    /** 将手持的仓储方块/超维终端绑定到指定存储 id（类型从手持物品推断）。 */
    private static int storageBind(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        UUID id = parseUuid(StringArgumentType.getString(context, "id"));
        if (id == null) throw ERROR_INVALID_ID.create();
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) throw ERROR_NO_HAND_ITEM.create();

        StorageType type;
        if (stack.is(ModItems.HYPERDIMENSION_TERMINAL)) {
            // 终端绑定到存储 id（类型仅用于显示）
            type = StorageType.HYPERDIMENSION;
            stack.set(ModComponents.TERMINAL_BINDING, new TerminalBinding(Optional.of(id)));
        } else {
            StorageRef existing = stack.get(ModComponents.STORAGE);
            if (existing == null) throw ERROR_NO_STORAGE.create();
            type = existing.type();
            stack.set(ModComponents.STORAGE, new StorageRef(type, id));
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        return CommandUtil.sendSuccess(
            context.getSource(),
            "command.anvilcraft.storage.bind.success",
            Component.literal(type.getSerializedName()),
            Component.literal(id.toString())
        );
    }

    /** 清除手持超维终端的绑定。 */
    private static int storageUnbind(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) throw ERROR_NO_HAND_ITEM.create();
        if (stack.is(ModItems.HYPERDIMENSION_TERMINAL)) {
            stack.remove(ModComponents.TERMINAL_BINDING);
        } else {
            StorageRef ref = stack.get(ModComponents.STORAGE);
            if (ref == null) throw ERROR_NO_STORAGE.create();
            stack.set(ModComponents.STORAGE, new StorageRef(ref.type()));
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        return CommandUtil.sendSuccess(context.getSource(), "command.anvilcraft.storage.unbind.success");
    }

    private static @Nullable StorageType parseType(String name) {
        for (StorageType type : StorageType.values()) {
            if (type.getSerializedName().equalsIgnoreCase(name)) return type;
        }
        return null;
    }

    private static @Nullable UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
