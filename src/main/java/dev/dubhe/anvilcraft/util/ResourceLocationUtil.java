package dev.dubhe.anvilcraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.dubhe.anvilcraft.AnvilCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.ResourceLocationException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public class ResourceLocationUtil {
    public static final Codec<ResourceLocation> ANC_CODEC = Codec.STRING
        .comapFlatMap(ResourceLocationUtil::readWithAncAsDefault, ResourceLocationUtil::toShortStringWithAncAsDefault)
        .stable();
    public static final StreamCodec<ByteBuf, ResourceLocation> ANC_STREAM_CODEC = ByteBufCodecs.STRING_UTF8
        .map(ResourceLocationUtil::parseWithAncAsDefault, ResourceLocationUtil::toShortStringWithAncAsDefault);

    private static DataResult<ResourceLocation> readWithAncAsDefault(String location) {
        try {
            return DataResult.success(ResourceLocationUtil.parseWithAncAsDefault(location));
        } catch (ResourceLocationException resourcelocationexception) {
            return DataResult.error(() -> "Not a valid resource location: " + location + " " + resourcelocationexception.getMessage());
        }
    }

    public static ResourceLocation parseWithAncAsDefault(String raw) {
        return ResourceLocationUtil.parseWithDefault(raw, AnvilCraft.MOD_ID);
    }

    public static ResourceLocation parseWithDefault(String raw, String defaultNamespace) {
        int sep = raw.indexOf(':');
        if (sep >= 0) {
            String path = raw.substring(sep + 1);
            if (sep != 0) {
                String namespace = raw.substring(0, sep);
                return ResourceLocation.fromNamespaceAndPath(namespace, path);
            } else {
                return ResourceLocation.fromNamespaceAndPath(defaultNamespace, path);
            }
        } else {
            return ResourceLocation.fromNamespaceAndPath(defaultNamespace, raw);
        }
    }

    public static String toShortStringWithAncAsDefault(ResourceLocation location) {
        return ResourceLocationUtil.toShortStringWithDefault(location, AnvilCraft.MOD_ID);
    }

    public static String toShortStringWithDefault(ResourceLocation location, String defaultNamespace) {
        return location.getNamespace().equals(defaultNamespace) ? location.getPath() : location.toString();
    }
}
