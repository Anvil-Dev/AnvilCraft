package dev.dubhe.anvilcraft.util.dummy;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DummyCat extends Cat {
    public DummyCat(Level level) {
        super(EntityType.CAT, level);
    }

    public static DummyCat fromPlayer(Level level, @Nullable Player player) {
        if (player == null) return null;
        DummyCat cat = new DummyCat(level);
        cat.setPos(player.position());
        return cat;
    }

    @Override
    protected @NotNull AABB getAttackBoundingBox() {
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
}
