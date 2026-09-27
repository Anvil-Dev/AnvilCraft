package dev.dubhe.anvilcraft.util.dummy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DummyCat extends Cat {
    private static final Map<UUID, DummyCat> CACHE = new HashMap<>();

    public DummyCat(Level level) {
        super(EntityType.CAT, level);
    }

    public static @Nullable DummyCat fromEntity(@Nullable LivingEntity entity) {
        if (entity == null) return null;

        UUID id = entity.getUUID();
        DummyCat cache = DummyCat.CACHE.get(id);
        if (cache != null && cache.level() == entity.level()) {
            cache.setPos(entity.position());
            return cache;
        }
        cache = new DummyCat(entity.level());
        cache.setPos(entity.position());
        DummyCat.CACHE.put(id, cache);
        return cache;
    }

    public static void clear(Level level) {
        CACHE.values().removeIf(dummy -> dummy.level() == level);
    }

    public static void clear(Entity entity) {
        DummyCat.CACHE.remove(entity.getUUID());
    }

    @Override
    protected AABB getAttackBoundingBox(double horizontalExpansion) {
        return new AABB(Vec3.ZERO, Vec3.ZERO);
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean mayInteract(ServerLevel level, BlockPos pos) {
        return false;
    }

    @Override
    public boolean mayBeLeashed() {
        return false;
    }
}
