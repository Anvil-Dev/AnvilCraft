package dev.dubhe.anvilcraft.util.dummy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DummyWolf extends Wolf {
    private static final Map<UUID, DummyWolf> CACHE = new HashMap<>();

    public DummyWolf(Level level) {
        super(EntityType.WOLF, level);
    }

    public static @Nullable DummyWolf fromEntity(@Nullable LivingEntity entity) {
        if (entity == null) return null;

        UUID id = entity.getUUID();
        DummyWolf cache = DummyWolf.CACHE.get(id);
        if (cache != null) {
            cache.setPos(entity.position());
            return cache;
        }
        cache = new DummyWolf(entity.level());
        cache.setPos(entity.position());
        DummyWolf.CACHE.put(id, cache);
        return cache;
    }

    public static void clear(Entity entity) {
        DummyWolf.CACHE.remove(entity.getUUID());
    }

    @Override
    protected AABB getAttackBoundingBox() {
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
    public boolean mayInteract(Level level, BlockPos pos) {
        return false;
    }

    @Override
    public boolean mayBeLeashed() {
        return false;
    }
}
