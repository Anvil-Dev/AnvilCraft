package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record SavedEntity(CompoundTag tag, boolean isMonster) {
    public static final Codec<SavedEntity> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        CompoundTag.CODEC
            .fieldOf("tag")
            .forGetter(SavedEntity::tag),
        Codec.BOOL
            .fieldOf("isMonster")
            .forGetter(SavedEntity::isMonster)
    ).apply(ins, SavedEntity::new));

    public static final StreamCodec<ByteBuf, SavedEntity> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.COMPOUND_TAG,
        SavedEntity::tag,
        ByteBufCodecs.BOOL,
        SavedEntity::isMonster,
        SavedEntity::new
    );

    @Nullable
    public Entity toEntity(Level level) {
        Optional<EntityType<?>> optional = EntityType.by(tag);
        if (optional.isEmpty()) return null;
        EntityType<?> type = optional.get();
        Entity entity = type.create(level);
        if (entity == null) return null;
        entity.load(tag);
        return entity;
    }

    public static SavedEntity fromMob(Mob entity) {
        CompoundTag entityTag = new CompoundTag();
        entity.saveAsPassenger(entityTag);
        entityTag.remove(Entity.UUID_TAG);
        return new SavedEntity(entityTag, !entity.getType().getCategory().isFriendly());
    }
}
