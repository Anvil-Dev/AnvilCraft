package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.tooltip.impl.CauldronProcessingTooltip;
import dev.dubhe.anvilcraft.api.tooltip.impl.CauldronProcessingTooltip.Annotations;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import java.util.HashMap;
import java.util.Map;

/** 按物品与流体的完整组件向 Jade 同步服务端匹配到的加工预览。 */
public enum CauldronProcessingProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final Identifier UID = AnvilCraft.of("cauldron_processing_preview");
    private static final Annotations EMPTY = new Annotations(Map.of(), Map.of());
    private static final StreamCodec<RegistryFriendlyByteBuf, Annotations> CODEC = StreamCodec.composite(
        ByteBufCodecs.map(HashMap::new, ItemResource.STREAM_CODEC, ComponentSerialization.STREAM_CODEC), Annotations::items,
        ByteBufCodecs.map(HashMap::new, FluidResource.STREAM_CODEC, ComponentSerialization.STREAM_CODEC), Annotations::fluids,
        Annotations::new
    );

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        var container = accessor.getBlockEntity();
        if (container == null) {
            return;
        }
        Annotations annotations = CauldronProcessingTooltip.annotations(container);
        if (!annotations.items().isEmpty() || !annotations.fluids().isEmpty()) {
            tag.put(UID.toString(), accessor.encodeAsNbt(CODEC, annotations));
        }
    }

    public static Annotations readAnnotations(BlockAccessor accessor) {
        var encoded = accessor.getServerData().get(UID.toString());
        return encoded == null ? EMPTY : accessor.decodeFromNbt(CODEC, encoded).orElse(EMPTY);
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
