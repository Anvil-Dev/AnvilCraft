package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.entity.ThrownHeavyHalberdEntity;
import dev.dubhe.anvilcraft.mixin.accessor.EntityTypeAccessor;
import net.minecraft.advancements.criterion.EntityTypePredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityTypePredicate.class)
abstract class EntityTypePredicateMixin {
    @Shadow
    @Final
    private HolderSet<EntityType<?>> types;

    @ModifyReturnValue(method = "matches", at = @At("RETURN"))
    private boolean anvilcraft$matchThrownHalberd(boolean original, Holder<EntityType<?>> type) {
        return original || this.types instanceof HolderSet.Direct<EntityType<?>> direct
            && direct.size() == 1
            && direct.get(0).value() == EntityType.TRIDENT
            && ((EntityTypeAccessor) type.value()).anvilcraft$getFactory() instanceof ThrownHeavyHalberdEntity.Factory<?>;
    }
}
