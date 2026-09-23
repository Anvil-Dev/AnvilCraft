package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.entity.WeaponBeamEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 实体建造适配器注册表与瞬态排除。 */
public final class EntityBuildAdapters {
    private static final List<EntityBuildAdapter> REGISTRY = new ArrayList<>();

    static {
        VanillaBuildingEntities.register();
        register(new MagnetizedNodeBuildAdapter());
        register(new DynamicBuildingEntities.Outlet());
        register(new DynamicBuildingEntities());
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

    public static Optional<EntityBuildAdapter> find(Entity entity, CompoundTag nbt) {
        return REGISTRY.stream().filter(adapter -> adapter.matches(entity, nbt)).findFirst();
    }

    public static Optional<Entity> create(CompoundTag tag, Level level) {
        Entity entity = BlueprintLeashes.create(tag, level);
        if (entity != null) BlueprintEntities.restoreMotion(entity, tag);
        return Optional.ofNullable(entity);
    }

    public static boolean isTransient(EntityType<?> type) {
        Class<? extends Entity> base = type.getBaseClass();
        return type == EntityType.PLAYER || type == EntityType.EXPERIENCE_ORB
            || type == EntityType.LIGHTNING_BOLT
            || type == EntityType.AREA_EFFECT_CLOUD || type == EntityType.FISHING_BOBBER || type == EntityType.MARKER
            || Player.class.isAssignableFrom(base)
            || ExperienceOrb.class.isAssignableFrom(base)
            || LightningBolt.class.isAssignableFrom(base)
            || AreaEffectCloud.class.isAssignableFrom(base)
            || FishingHook.class.isAssignableFrom(base)
            || Marker.class.isAssignableFrom(base)
            || type == EntityType.EVOKER_FANGS
            || type == EntityType.INTERACTION;
    }

    public static boolean isTransient(Entity entity) {
        return isTransient(entity.getType()) || entity instanceof Player
            || entity instanceof ExperienceOrb || entity instanceof LightningBolt || entity instanceof WeaponBeamEntity
            || entity instanceof AreaEffectCloud || entity instanceof FishingHook || entity instanceof Marker;
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
                return create(copy, level).map(entity -> {
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
        if (entity instanceof Mob mob) {
            for (var content : contents) {
                if (content.slot() >= 100 && content.slot() < 100 + EquipmentSlot.values().length) {
                    mob.setItemSlot(EquipmentSlot.values()[content.slot() - 100], content.stack().copy());
                } else if (mob instanceof InventoryCarrier carrier
                    && content.slot() >= 0 && content.slot() < carrier.getInventory().getContainerSize()) {
                    carrier.getInventory().setItem(content.slot(), content.stack().copy());
                }
            }
            return;
        }
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
        return BuildingEntityTransform.withWorldPos(nbt, world);
    }

    static CompoundTag normalizeType(CompoundTag tag) {
        CompoundTag normalized = tag.copy();
        String id = normalized.getStringOr("id", "");
        if (id.equals("minecraft:boat") || id.equals("minecraft:chest_boat")) {
            String wood = normalized.getStringOr("Type", "oak");
            if (!List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo").contains(wood)) {
                wood = "oak";
            }
            String suffix = wood.equals("bamboo") ? "raft" : "boat";
            normalized.putString("id", "minecraft:" + wood + (id.endsWith("chest_boat") ? "_chest_" : "_") + suffix);
            normalized.remove("Type");
        }
        return normalized;
    }

    static Optional<EntityType<?>> typeOf(CompoundTag tag) {
        String id = tag.getStringOr("id", "");
        if (id.equals("minecraft:boat") || id.equals("minecraft:chest_boat")) id = normalizeType(tag).getStringOr("id", "");
        return EntityType.byString(id);
    }

    private static Optional<EntityBuildAdapter> findType(BuildingEntityOp op) {
        CompoundTag nbt = op.entityNbt();
        if (nbt == null) {
            return Optional.empty();
        }
        return typeOf(nbt).flatMap(type -> find(type, nbt));
    }
}
