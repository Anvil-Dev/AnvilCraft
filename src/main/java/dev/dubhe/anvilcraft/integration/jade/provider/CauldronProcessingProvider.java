package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler.ResourceKey;
import dev.dubhe.anvilcraft.api.tooltip.impl.CauldronProcessingTooltip;
import dev.dubhe.anvilcraft.api.tooltip.impl.CauldronProcessingTooltip.Annotations;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity.FluidKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import java.util.HashMap;
import java.util.Map;

/** 按物品与流体的完整组件向 Jade 同步服务端匹配到的加工预览。 */
public enum CauldronProcessingProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final ResourceLocation UID = AnvilCraft.of("cauldron_processing_preview");
    private static final Annotations EMPTY = new Annotations(Map.of(), Map.of());
    private static final StreamCodec<RegistryFriendlyByteBuf, ResourceKey> ITEM = ItemStack.STREAM_CODEC
        .map(ResourceKey::of, ResourceKey::stack);
    private static final StreamCodec<RegistryFriendlyByteBuf, FluidKey> FLUID = FluidStack.STREAM_CODEC
        .map(FluidKey::of, key -> key.toStack(1));
    private static final StreamCodec<RegistryFriendlyByteBuf, Annotations> CODEC = StreamCodec.composite(
        ByteBufCodecs.map(HashMap::new, ITEM, ComponentSerialization.STREAM_CODEC), Annotations::items,
        ByteBufCodecs.map(HashMap::new, FLUID, ComponentSerialization.STREAM_CODEC), Annotations::fluids,
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
    public ResourceLocation getUid() {
        return UID;
    }
}
