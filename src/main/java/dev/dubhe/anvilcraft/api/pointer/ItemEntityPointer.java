package dev.dubhe.anvilcraft.api.pointer;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.block.BlockPlacementRules;
import dev.dubhe.anvilcraft.init.ModRegistries;
import dev.dubhe.anvilcraft.util.BlockPlacementUtil;
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
import net.minecraft.world.level.block.state.BlockState;
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

    public ItemEntityPointer(Type type, UUID id, ItemStack stack) {
        this.id = id;
        this.stack = stack.copy();
        this.type = type;
    }

    private ItemEntityPointer(Type type, ItemEntity entity) {
        this(type, entity.getUUID(), entity.getItem());
    }

    @Nullable
    public ItemEntity getEntity(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntities().get(this.id);
        if (!(entity instanceof ItemEntity item)
            || item.level() != level
            || !serverLevel.isPositionEntityTicking(item.blockPosition())) {
            return null;
        }
        return item;
    }

    @Override
    public boolean isStillValid(Level level) {
        ItemEntity entity = this.getEntity(level);
        if (entity == null) {
            return false;
        }
        return !entity.isRemoved() && ItemStack.isSameItemSameComponents(entity.getItem(), this.stack);
    }

    @Override
    public boolean matches(Level level, BlockState requiredState) {
        return BlockPlacementRules.getPlacementItemCount(level.registryAccess(), requiredState, this.stack) > 0;
    }

    @Override
    public Either<ItemStack, BlockState> getDisplayedBlock() {
        return Either.left(this.stack.copyWithCount(1));
    }

    @Override
    public boolean applyToPos(ServerLevel level, BlockPos pos) {
        return this.placeToPos(level, pos, null);
    }

    @Override
    public boolean applyToPos(ServerLevel level, BlockPos pos, BlockState requiredState) {
        if (!this.matches(level, requiredState)) {
            return false;
        }
        return this.placeToPos(level, pos, requiredState);
    }

    private boolean placeToPos(ServerLevel level, BlockPos pos, @Nullable BlockState requiredState) {
        ItemEntity entity = this.getEntity(level);
        if (entity == null
            || entity.isRemoved()
            || !ItemStack.isSameItemSameComponents(entity.getItem(), this.stack)) {
            return false;
        }

        int requiredCount = requiredState == null
            ? 1
            : BlockPlacementRules.getPlacementItemCount(level.registryAccess(), requiredState, this.stack);
        if (requiredCount < 0) {
            return false;
        }
        List<ItemEntity> sourceEntities = this.getSourceEntities(level, entity);
        if (sourceEntities.stream().mapToInt(source -> source.getItem().getCount()).sum() < requiredCount) {
            return false;
        }

        ItemStack stack = this.stack.copyWithCount(requiredCount);
        int initialCount = stack.getCount();
        ItemStack result = BlockPlacementUtil.placeBlock(level, pos, stack, requiredState);
        int consumed = initialCount - result.getCount();
        if (consumed <= 0) {
            return false;
        }

        int remainingCount = requiredCount;
        for (ItemEntity source : sourceEntities) {
            ItemStack remaining = source.getItem().copy();
            int consumedFromEntity = Math.min(remainingCount, remaining.getCount());
            remaining.shrink(consumedFromEntity);
            remainingCount -= consumedFromEntity;
            if (remaining.isEmpty()) {
                source.discard();
            } else {
                source.setItem(remaining);
            }
            if (remainingCount == 0) {
                break;
            }
        }
        return remainingCount == 0;
    }

    private List<ItemEntity> getSourceEntities(ServerLevel level, ItemEntity entity) {
        return level.getEntitiesOfClass(
            ItemEntity.class,
            new AABB(entity.blockPosition()),
            source -> source.isAlive() && ItemStack.isSameItemSameComponents(source.getItem(), this.stack)
        );
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
        public @Nullable ItemEntityPointer point(
            Level level,
            BlockPos pos,
            Direction facing,
            @Nullable BlockState requiredState
        ) {
            if (!(level instanceof ServerLevel serverLevel)) {
                return null;
            }
            List<ItemEntity> entities;
            if (this.filter == null) {
                entities = serverLevel.getEntitiesOfClass(ItemEntity.class, new AABB(pos));
            } else {
                entities = serverLevel.getEntitiesOfClass(
                    ItemEntity.class,
                    new AABB(pos),
                    entity -> this.filter.test(entity.getItem())
                );
            }
            if (entities.isEmpty()) {
                return null;
            }
            for (ItemEntity entity : entities) {
                if (!serverLevel.isPositionEntityTicking(entity.blockPosition())) {
                    continue;
                }
                ItemEntityPointer pointer = new ItemEntityPointer(this, entity);
                if (requiredState == null) {
                    return pointer;
                }
                int requiredCount = BlockPlacementRules.getPlacementItemCount(
                    level.registryAccess(),
                    requiredState,
                    entity.getItem()
                );
                int availableCount = pointer.getSourceEntities(serverLevel, entity)
                    .stream()
                    .mapToInt(source -> source.getItem().getCount())
                    .sum();
                if (requiredCount > 0 && pointer.matches(level, requiredState) && availableCount >= requiredCount) {
                    return pointer;
                }
            }
            return null;
        }
    }
}
