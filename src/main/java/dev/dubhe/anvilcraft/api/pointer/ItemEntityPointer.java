package dev.dubhe.anvilcraft.api.pointer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.init.ModRegistries;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;

@Getter
public class ItemEntityPointer implements ITargetPointer {
    private final Type type;
    private final UUID id;
    private final ItemStack stack;
    private @Nullable ItemEntity entity;

    public ItemEntityPointer(Type type, UUID id, ItemStack stack) {
        this.id = id;
        this.stack = stack;
        this.type = type;
    }

    private ItemEntityPointer(Type type, ItemEntity entity) {
        this(type, entity.getUUID(), entity.getItem());
    }

    @Nullable
    public ItemEntity getEntity(Level level) {
        if (this.entity == null) {
            Entity entity = level.getEntities().get(this.id);
            if (!(entity instanceof ItemEntity item)) {
                return null;
            }
            this.entity = item;
        }
        return this.entity;
    }

    @Override
    public boolean isStillValid(Level level) {
        ItemEntity entity = this.getEntity();
        if (entity == null) {
            return false;
        }
        return !entity.isRemoved() && ItemStack.isSameItemSameComponents(entity.getItem(), this.stack);
    }

    @Override
    public boolean applyToPos(ServerLevel level, BlockPos pos) {
        ItemEntity entity = this.getEntity();
        if (entity == null || !this.isStillValid(level)) {
            return false;
        }

        ItemStack stack = entity.getItem();
        ItemStack result = ITargetPointer.placeToPos(level, pos, stack);
        if (ItemStack.matches(stack, result)) {
            return false;
        }

        entity.setItem(result);
        if (entity.getItem().isEmpty()) {
            entity.discard();
        }
        return true;
    }

    public static class Type implements ITargetPointer.Type<ItemEntityPointer> {
        public static final Codec<Type> CODEC = ModRegistries.TARGET_POINTER_TYPE_REGISTRY.byNameCodec()
            .flatXmap(
                raw -> raw instanceof Type type
                       ? DataResult.success(type)
                       : DataResult.error(() -> "Cannot cast %s to ItemEntityPointer.Type".formatted(raw.getClass().getSimpleName())),
                DataResult::success
            );
        public static final StreamCodec<RegistryFriendlyByteBuf, Type> STREAM_CODEC = ByteBufCodecs.registry(
            ModRegistries.TARGET_POINTER_TYPE_KEY
        ).map(Util::cast, Function.identity());
        public static final MapCodec<ItemEntityPointer> POINTER_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Type.CODEC
                .fieldOf("type")
                .forGetter(ItemEntityPointer::getType),
            UUIDUtil.CODEC
                .fieldOf("id")
                .forGetter(ItemEntityPointer::getId),
            ItemStack.OPTIONAL_CODEC
                .fieldOf("stack")
                .forGetter(ItemEntityPointer::getStack)
        ).apply(inst, ItemEntityPointer::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ItemEntityPointer> POINTER_STREAM_CODEC = StreamCodec.composite(
            Type.STREAM_CODEC,
            ItemEntityPointer::getType,
            UUIDUtil.STREAM_CODEC,
            ItemEntityPointer::getId,
            ItemStack.OPTIONAL_STREAM_CODEC,
            ItemEntityPointer::getStack,
            ItemEntityPointer::new
        );

        private final @Nullable Predicate<ItemStack> filter;

        public Type(Predicate<ItemStack> filter) {
            this.filter = filter;
        }

        public Type() {
            this.filter = null;
        }

        @Override
        public MapCodec<ItemEntityPointer> codec() {
            return Type.POINTER_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ItemEntityPointer> streamCodec() {
            return Type.POINTER_STREAM_CODEC;
        }

        @Override
        public @Nullable ItemEntityPointer point(Level level, BlockPos pos, Direction facing) {
            List<ItemEntity> entities;
            if (this.filter == null) {
                entities = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos));
            } else {
                entities = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos), entity -> this.filter.test(entity.getItem()));
            }
            if (entities.isEmpty()) {
                return null;
            }
            return new ItemEntityPointer(this, entities.getFirst());
        }
    }
}
