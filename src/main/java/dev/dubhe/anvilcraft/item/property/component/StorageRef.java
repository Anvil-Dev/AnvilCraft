package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.storage.ModStorageTypes;
import dev.dubhe.anvilcraft.saved.storage.IStorageType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;

public record StorageRef(Holder<IStorageType<?>> type, Optional<UUID> id) implements TooltipProvider {
    public static final MapCodec<StorageRef> CODEC = CodecUtil.mapCodec(
        IStorageType.HOLDER_CODEC
            .fieldOf("type")
            .forGetter(StorageRef::type),
        UUIDUtil.CODEC
            .optionalFieldOf("id")
            .forGetter(StorageRef::id),
        StorageRef::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, StorageRef> STREAM_CODEC = StreamCodec.composite(
        IStorageType.HOLDER_STREAM_CODEC,
        StorageRef::type,
        ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
        StorageRef::id,
        StorageRef::new
    );

    public StorageRef(Holder<IStorageType<?>> type) {
        this(type, Optional.empty());
    }

    public StorageRef(Holder<IStorageType<?>> type, @Nullable UUID id) {
        this(type, Optional.ofNullable(id));
    }

    public static StorageRef crate() {
        return new StorageRef(ModStorageTypes.CRATE);
    }

    public static StorageRef largeCrate() {
        return new StorageRef(ModStorageTypes.LARGE_CRATE);
    }

    public static StorageRef shulkerContainer() {
        return new StorageRef(ModStorageTypes.SHULKER_CONTAINER);
    }

    public static StorageRef hyperdimension() {
        return new StorageRef(ModStorageTypes.HYPERDIMENSION);
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> builder, TooltipFlag flag) {
        if (!AnvilCraft.CLIENT_CONFIG.showStorageStoredId) {
            return;
        }
        builder.accept(Component.translatable(
            "tooltip.anvilcraft.property.storage.id",
            this.id.map(id -> Component.literal(id.toString()))
                .orElseGet(() -> Component.translatable("tooltip.anvilcraft.property.storage.id.null"))
        ).withStyle(ChatFormatting.GRAY));
    }
}
