package dev.dubhe.anvilcraft.util.dummy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DummyArmadillo extends Armadillo {
    private static final Map<UUID, DummyArmadillo> CACHE = new HashMap<>();

    public DummyArmadillo(Level level) {
        super(EntityType.ARMADILLO, level);
    }

    public static @Nullable DummyArmadillo fromPlayer(Level level, @Nullable Player player) {
        if (player == null) return null;
        UUID id = player.getGameProfile().getId();
        DummyArmadillo cache = DummyArmadillo.CACHE.get(id);
        if (cache == null) {
            cache = new DummyArmadillo(level);
            DummyArmadillo.CACHE.put(id, cache);
        }
        cache.setPos(player.position());
        return cache;
    }

    public static void clear(Player player) {
        DummyArmadillo.CACHE.remove(player.getGameProfile().getId());
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
}
