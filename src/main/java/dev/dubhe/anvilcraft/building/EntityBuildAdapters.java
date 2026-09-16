package dev.dubhe.anvilcraft.building;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/** 实体建造适配器注册表与瞬态排除。 */
public final class EntityBuildAdapters {
    private static final List<EntityBuildAdapter> REGISTRY = new ArrayList<>();

    static {
        VanillaBuildingEntities.register();
    }

    private EntityBuildAdapters() {
    }

    public static void register(EntityBuildAdapter adapter) {
        REGISTRY.add(adapter);
    }

    public static Optional<EntityBuildAdapter> find(EntityType<?> type, CompoundTag nbt) {
        for (EntityBuildAdapter adapter : REGISTRY) {
            if (adapter.matches(type, nbt)) {
                return Optional.of(adapter);
            }
        }
        return Optional.empty();
    }

    public static boolean isTransient(EntityType<?> type) {
        Class<? extends Entity> base = type.getBaseClass();
        return Player.class.isAssignableFrom(base)
            || ItemEntity.class.isAssignableFrom(base)
            || ExperienceOrb.class.isAssignableFrom(base)
            || LightningBolt.class.isAssignableFrom(base)
            || Projectile.class.isAssignableFrom(base)
            || AreaEffectCloud.class.isAssignableFrom(base)
            || FishingHook.class.isAssignableFrom(base)
            || Marker.class.isAssignableFrom(base)
            || type == EntityType.EVOKER_FANGS
            || type == EntityType.INTERACTION;
    }

    @Nullable
    public static Entity spawn(ServerLevel level, BuildingEntityOp op) {
        return findType(op)
            .flatMap(adapter -> Optional.ofNullable(adapter.spawn(level, op)))
            .orElseGet(() -> {
                CompoundTag nbt = op.entityNbt();
                if (nbt == null) {
                    return null;
                }
                CompoundTag copy = nbt.copy();
                copy.remove("UUID");
                return EntityType.create(copy, level).map(entity -> {
                    if (!level.addFreshEntity(entity)) {
                        return null;
                    }
                    return entity;
                }).orElse(null);
            });
    }

    public static ItemStack returnAfterDeliver(ServerLevel level, BuildingEntityOp op) {
        return findType(op)
            .map(adapter -> adapter.returnAfterDeliver(level, op))
            .orElse(op.returnStack().copy());
    }

    public static void insertContents(
        Entity entity,
        List<EntityBuildAdapter.SlotStack> contents,
        HolderLookup.Provider registries
    ) {
        Optional<EntityBuildAdapter> adapter = find(entity.getType(), new CompoundTag());
        if (adapter.isPresent()) {
            adapter.get().insertContents(entity, contents, registries);
            return;
        }
        if (entity instanceof Container container) {
            for (EntityBuildAdapter.SlotStack content : contents) {
                if (content.slot() >= 0 && content.slot() < container.getContainerSize()) {
                    container.setItem(content.slot(), content.stack().copy());
                }
            }
        }
    }

    public static CompoundTag withWorldPos(CompoundTag nbt, Vec3 world) {
        CompoundTag copy = nbt.copy();
        copy.remove("UUID");
        ListTag pos = new ListTag();
        pos.add(DoubleTag.valueOf(world.x));
        pos.add(DoubleTag.valueOf(world.y));
        pos.add(DoubleTag.valueOf(world.z));
        copy.put("Pos", pos);
        return copy;
    }

    private static Optional<EntityBuildAdapter> findType(BuildingEntityOp op) {
        CompoundTag nbt = op.entityNbt();
        if (nbt == null) {
            return Optional.empty();
        }
        return EntityType.by(nbt).flatMap(type -> find(type, nbt));
    }
}
