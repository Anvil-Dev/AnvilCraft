package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.saved.storage.StorageType;
import net.minecraft.ChatFormatting;
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

public record StorageRef(StorageType type, Optional<UUID> id) implements TooltipProvider {
    public static final MapCodec<StorageRef> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        StorageType.CODEC
            .fieldOf("type")
            .forGetter(StorageRef::type),
        UUIDUtil.CODEC
            .optionalFieldOf("id")
            .forGetter(StorageRef::id)
    ).apply(ins, StorageRef::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, StorageRef> STREAM_CODEC = StreamCodec.composite(
        StorageType.STREAM_CODEC,
        StorageRef::type,
        ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
        StorageRef::id,
        StorageRef::new
    );

    public StorageRef(StorageType type) {
        this(type, Optional.empty());
    }

    public StorageRef(StorageType type, @Nullable UUID id) {
        this(type, Optional.ofNullable(id));
    }

    public static StorageRef crate() {
        return new StorageRef(StorageType.CRATE);
    }

    public static StorageRef largeCrate() {
        return new StorageRef(StorageType.LARGE_CRATE);
    }

    public static StorageRef shulkerContainer() {
        return new StorageRef(StorageType.SHULKER_CONTAINER);
    }

    public static StorageRef hyperdimension() {
        return new StorageRef(StorageType.HYPERDIMENSION);
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
