package dev.dubhe.anvilcraft.mixin.accessor;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
    @Accessor
    int getLife();

    @Accessor
    void setLife(int life);

    @Accessor
    double getBaseDamage();

    @Accessor
    @Nullable
    IntOpenHashSet getPiercingIgnoreEntityIds();

    @Accessor
    void setPiercingIgnoreEntityIds(IntOpenHashSet piercingIgnoreEntityIds);

    @Accessor
    @Nullable
    List<Entity> getPiercedAndKilledEntities();

    @Accessor
    @Nullable
    ItemStack getFiredFromWeapon();
}
