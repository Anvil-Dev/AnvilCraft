package dev.dubhe.anvilcraft.util.dummy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DummyWolf extends Wolf {
    private static final Map<UUID, DummyWolf> CACHE = new HashMap<>();

    public DummyWolf(Level level) {
        super(EntityType.WOLF, level);
    }

    public static @Nullable DummyWolf fromPlayer(Level level, @Nullable Player player) {
        if (player == null) return null;
        UUID id = player.getGameProfile().id();
        DummyWolf cache = DummyWolf.CACHE.get(id);
        if (cache == null) {
            DummyWolf dummy = new DummyWolf(level);
            DummyWolf.CACHE.put(id, dummy);
            cache = dummy;
        }
        cache.setPos(player.position());
        return cache;
    }

    public static void clear(Player player) {
        DummyWolf.CACHE.remove(player.getGameProfile().id());
    }

    public static void clear(Level level) {
        DummyWolf.CACHE.values().removeIf(wolf -> wolf.level() == level);
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
}
