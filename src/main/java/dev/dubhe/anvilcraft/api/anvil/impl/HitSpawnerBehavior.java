package dev.dubhe.anvilcraft.api.anvil.impl;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.mixin.accessor.BaseSpawnerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class HitSpawnerBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(
        Level level,
        BlockPos pos,
        BlockState hitBlockState,
        float fallDistance,
        AnvilFallOnLandEvent event
    ) {
        if (level instanceof ServerLevel serverLevel) {
            RandomSource randomSource = serverLevel.getRandom();
            float f = randomSource.nextFloat();
            if (fallDistance < 1) {
                fallDistance = 1.1f;
            }
            if (f <= (1 / fallDistance)) {
                return false;
            }
            if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity blockEntity) {
                BaseSpawner spawner = blockEntity.getSpawner();
                BaseSpawnerAccessor accessor = (BaseSpawnerAccessor) spawner;
                SpawnData spawnData = accessor.invoker$getOrCreateNextSpawnData(level, randomSource, pos);
                spawnEntities(spawnData, serverLevel, pos, randomSource, accessor);
            }
        }
        return false;
    }

    private void spawnEntities(
        SpawnData spawnData,
        ServerLevel serverLevel,
        BlockPos pos,
        RandomSource randomSource,
        @NotNull BaseSpawnerAccessor accessor
    ) {
        for (int i = 0; i < accessor.getSpawnCount(); ++i) {
            CompoundTag compoundTag = spawnData.getEntityToSpawn();
            Optional<EntityType<?>> optional = EntityType.by(compoundTag);
            if (optional.isEmpty()) {
                return;
            }

            ListTag listTag = compoundTag.getList("Pos", 6);
            int size = listTag.size();
            double x;
            double y;
            double z;
            if (size >= 1) {
                x = listTag.getDouble(0);
            } else {
                x = (double) pos.getX()
                    + (randomSource.nextDouble() - randomSource.nextDouble()) * accessor.getSpawnRange()
                    + 0.5;
            }
            if (size >= 2) {
                y = listTag.getDouble(1);
            } else {
                y = pos.getY() + randomSource.nextInt(3) - 1;
            }
            if (size >= 3) {
                z = listTag.getDouble(2);
            } else {
                z = (double) pos.getZ()
                    + (randomSource.nextDouble() - randomSource.nextDouble()) * accessor.getSpawnRange()
                    + 0.5;
            }
            if (serverLevel.noCollision(optional.get().getSpawnAABB(x, y, z))) {
                BlockPos blockPos = BlockPos.containing(x, y, z);
                if (spawnData.getCustomSpawnRules().isPresent()) {
                    if (!optional.get().getCategory().isFriendly()
                        && serverLevel.getDifficulty() == Difficulty.PEACEFUL
                    ) {
                        continue;
                    }

                    SpawnData.CustomSpawnRules customSpawnRules =
                        spawnData.getCustomSpawnRules().get();
                    if (!customSpawnRules
                        .blockLightLimit()
                        .isValueInRange(serverLevel.getBrightness(LightLayer.BLOCK, blockPos))
                        || !customSpawnRules
                        .skyLightLimit()
                        .isValueInRange(serverLevel.getBrightness(LightLayer.SKY, blockPos))
                    ) {
                        continue;
                    }
                } else if (!SpawnPlacements.checkSpawnRules(
                    optional.get(), serverLevel, MobSpawnType.SPAWNER, blockPos, serverLevel.getRandom())
                ) {
                    continue;
                }
                Entity entity = EntityType.loadEntityRecursive(compoundTag, serverLevel, it -> {
                    it.moveTo(x, y, z, it.getYRot(), it.getXRot());
                    return it;
                });
                if (entity == null) {
                    return;
                }
                AABB boundingBox = new AABB(
                    pos.getX(),
                    pos.getY(),
                    pos.getZ(),
                    pos.getX() + 1,
                    pos.getY() + 1,
                    pos.getZ() + 1
                );
                int k = serverLevel
                    .getEntitiesOfClass(entity.getClass(), boundingBox.inflate(accessor.getSpawnRange()))
                    .size();
                if (k >= accessor.getMaxNearbyEntities()) {
                    return;
                }

                entity.moveTo(
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    randomSource.nextFloat() * 360.0F,
                    0.0F
                );
                if (entity instanceof Mob mob) {
                    if (spawnData.getCustomSpawnRules().isEmpty()
                        && !mob.checkSpawnRules(serverLevel, MobSpawnType.SPAWNER)
                        || !mob.checkSpawnObstruction(serverLevel)
                    ) {
                        continue;
                    }

                    if (spawnData.getEntityToSpawn().size() == 1
                        && spawnData.getEntityToSpawn().contains("id", 8)
                    ) {
                        EventHooks.finalizeMobSpawn(
                            (Mob) entity,
                            serverLevel,
                            serverLevel.getCurrentDifficultyAt(entity.blockPosition()),
                            MobSpawnType.SPAWNER,
                            null
                        );
                    }
                }

                if (!serverLevel.tryAddFreshEntityWithPassengers(entity)) {
                    return;
                }

                serverLevel.levelEvent(2004, pos, 0);
                serverLevel.gameEvent(entity, GameEvent.ENTITY_PLACE, blockPos);
                if (entity instanceof Mob) {
                    ((Mob) entity).spawnAnim();
                }
            }
        }
    }
}
