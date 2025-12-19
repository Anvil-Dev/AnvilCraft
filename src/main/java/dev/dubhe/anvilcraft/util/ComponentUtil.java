package dev.dubhe.anvilcraft.util;

import com.mojang.brigadier.Message;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

public class ComponentUtil {
    public static final Component TAB = Component.literal("  ");
    public static final Component LF = Component.literal("\n");
    public static final Component SPLITTER = Component.literal(",");
    public static final Component LIST_HEAD = Component.literal("[");
    public static final Component LIST_TAIL = Component.literal("]");
    public static final Component ITEM_HEAD = Component.literal("{");
    public static final Component ITEM_TAIL = Component.literal("}");

    public static Object[] argValidate(Object... args) {
        for (int i = 0, argsLength = args.length; i < argsLength; i++) {
            args[i] = ComponentUtil.argValidate(args[i]);
        }
        return args;
    }

    public static Component argValidate(Object arg) {
        return switch (arg) {
            case Component arg1 -> arg1;
            case String arg1 -> Component.literal(arg1);
            case Number arg1 -> Component.literal(arg1.toString());
            case Boolean arg1 -> Component.literal(arg1.toString());
            case Date date -> Component.translationArg(date);
            case Message msg -> Component.translationArg(msg);
            case UUID id -> Component.translationArg(id);
            case ResourceLocation location -> Component.translationArg(location);
            case ChunkPos pos -> Component.translationArg(pos);
            case URI uri -> Component.translationArg(uri);
            case null -> Component.literal("null");
            default -> Component.literal(arg.toString());
        };
    }
}
