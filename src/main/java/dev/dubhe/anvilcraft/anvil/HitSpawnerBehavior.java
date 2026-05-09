package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.mixin.accessor.BaseSpawnerAccessor;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

import java.util.Optional;

@Slf4j
public class HitSpawnerBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(
        Level level,
        BlockPos pos,
        BlockState hitBlockState,
        float fallDistance,
        AnvilEvent.OnLand event
    ) {
        if (level instanceof ServerLevel serverLevel) {
            RandomSource randomSource = serverLevel.getRandom();
            float f = randomSource.nextFloat();
            if (fallDistance <= 1) {
                fallDistance = 1.1F;
            }
            if (f <= (1 / fallDistance)) {
                return false;
            }
            if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity blockEntity) {
                BaseSpawner spawner = blockEntity.getSpawner();
                BaseSpawnerAccessor accessor = (BaseSpawnerAccessor) spawner;
                SpawnData spawnData = accessor.invokeGetOrCreateNextSpawnData(level, randomSource, pos);
                spawnEntities(spawnData, serverLevel, pos, randomSource, spawner, accessor);
            }
        }
        return false;
    }

    private void spawnEntities(
        SpawnData spawnData,
        ServerLevel level,
        BlockPos pos,
        RandomSource random,
        BaseSpawner spawner,
        BaseSpawnerAccessor accessor
    ) {
        for (int c = 0; c < accessor.getSpawnCount(); c++) {
            try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(this::toString, log)) {
                ValueInput input = TagValueInput.create(reporter, level.registryAccess(), spawnData.getEntityToSpawn());
                Optional<EntityType<?>> entityType = EntityType.by(input);
                if (entityType.isEmpty()) return;

                Vec3 spawnPos = input.read("Pos", Vec3.CODEC)
                    .orElseGet(
                        () -> new Vec3(
                            pos.getX() + (random.nextDouble() - random.nextDouble()) * accessor.getSpawnRange() + 0.5,
                            pos.getY() + random.nextInt(3) - 1,
                            pos.getZ() + (random.nextDouble() - random.nextDouble()) * accessor.getSpawnRange() + 0.5
                        )
                    );
                if (level.noCollision(entityType.get().getSpawnAABB(spawnPos.x, spawnPos.y, spawnPos.z))) {
                    BlockPos spawnBlockPos = BlockPos.containing(spawnPos);
                    if (spawnData.getCustomSpawnRules().isPresent()) {
                        if (!entityType.get().getCategory().isFriendly() && level.getDifficulty() == Difficulty.PEACEFUL) {
                            continue;
                        }

                        SpawnData.CustomSpawnRules customSpawnRules = spawnData.getCustomSpawnRules().get();
                        if (!customSpawnRules.isValidPosition(spawnBlockPos, level)) {
                            continue;
                        }
                    } else if (!SpawnPlacements.checkSpawnRules(
                        entityType.get(),
                        level,
                        EntitySpawnReason.SPAWNER,
                        spawnBlockPos,
                        level.getRandom()
                    )) {
                        continue;
                    }

                    Entity entity = EntityType.loadEntityRecursive(input, level, EntitySpawnReason.SPAWNER, e -> {
                        e.snapTo(spawnPos.x, spawnPos.y, spawnPos.z, e.getYRot(), e.getXRot());
                        return e;
                    });
                    if (entity == null) return;

                    int nearBy = level.getEntities(
                        EntityTypeTest.forExactClass(entity.getClass()),
                        new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1)
                            .inflate(accessor.getSpawnRange()),
                        EntitySelector.NO_SPECTATORS
                    ).size();
                    if (nearBy >= accessor.getMaxNearbyEntities()) return;

                    entity.snapTo(entity.getX(), entity.getY(), entity.getZ(), random.nextFloat() * 360.0F, 0.0F);
                    if (entity instanceof Mob mob) {
                        if (!EventHooks.checkSpawnPositionSpawner(mob, level, EntitySpawnReason.SPAWNER, spawnData, spawner)) {
                            continue;
                        }

                        boolean hasNoConfiguration = spawnData.getEntityToSpawn().size() == 1
                                                     && spawnData.getEntityToSpawn().getString("id").isPresent();
                        EventHooks.finalizeMobSpawnSpawner(
                            mob,
                            level,
                            level.getCurrentDifficultyAt(entity.blockPosition()),
                            EntitySpawnReason.SPAWNER,
                            null,
                            spawner,
                            hasNoConfiguration
                        );

                        spawnData.getEquipment().ifPresent(mob::equip);
                    }

                    if (!level.tryAddFreshEntityWithPassengers(entity)) return;

                    level.levelEvent(2004, pos, 0);
                    level.gameEvent(entity, GameEvent.ENTITY_PLACE, spawnBlockPos);
                    if (entity instanceof Mob) {
                        ((Mob) entity).spawnAnim();
                    }
                }
            }
        }
    }
}
