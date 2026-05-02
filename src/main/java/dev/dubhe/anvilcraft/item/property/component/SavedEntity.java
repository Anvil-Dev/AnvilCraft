package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jetbrains.annotations.Nullable;

@Slf4j
public record SavedEntity(EntityType<?> type, CompoundTag tag, boolean isMonster) {
    public static final Codec<SavedEntity> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        EntityType.CODEC
            .fieldOf("type")
            .forGetter(SavedEntity::type),
        CompoundTag.CODEC
            .fieldOf("tag")
            .forGetter(SavedEntity::tag),
        Codec.BOOL
            .fieldOf("isMonster")
            .forGetter(SavedEntity::isMonster)
    ).apply(ins, SavedEntity::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, SavedEntity> STREAM_CODEC = StreamCodec.composite(
        EntityType.STREAM_CODEC,
        SavedEntity::type,
        ByteBufCodecs.COMPOUND_TAG,
        SavedEntity::tag,
        ByteBufCodecs.BOOL,
        SavedEntity::isMonster,
        SavedEntity::new
    );

    @Nullable
    public Entity toEntity(Level level) {
        Entity entity = this.type.create(level, EntitySpawnReason.TRIGGERED);
        if (entity == null) return null;
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(entity.problemPath(), log)) {
            entity.load(TagValueInput.create(reporter, level.registryAccess(), this.tag));
        }
        return entity;
    }

    public static SavedEntity fromEntity(Entity entity) {
        CompoundTag entityTag = new CompoundTag();
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(entity.problemPath(), log)) {
            entity.saveAsPassenger(TagValueOutput.createWithContext(reporter, entity.level().registryAccess()));
        }
        entityTag.remove(Entity.TAG_UUID);
        entityTag.remove(Entity.TAG_ID);
        return new SavedEntity(entity.getType(), entityTag, !entity.getType().getCategory().isFriendly());
    }
}
