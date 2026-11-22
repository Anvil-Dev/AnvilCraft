package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.tooltip.impl.CauldronProcessingTooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import java.util.List;

/** 向正在查看容器的玩家同步服务端匹配到的加工预览。 */
public enum CauldronProcessingProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final Identifier UID = AnvilCraft.of("cauldron_processing_preview");
    private static final StreamCodec<RegistryFriendlyByteBuf, List<Component>> LINES =
        ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list());

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        var container = accessor.getBlockEntity();
        if (container == null) {
            return;
        }
        var lines = CauldronProcessingTooltip.previewLines(container);
        if (!lines.isEmpty()) {
            tag.put(UID.toString(), accessor.encodeAsNbt(LINES, lines));
        }
    }

    public static List<Component> readLines(BlockAccessor accessor) {
        var encoded = accessor.getServerData().get(UID.toString());
        return encoded == null ? List.of() : accessor.decodeFromNbt(LINES, encoded).orElse(List.of());
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
