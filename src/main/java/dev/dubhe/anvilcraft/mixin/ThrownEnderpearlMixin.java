package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.reicpe.ModRecipeTriggers;
import dev.dubhe.anvilcraft.recipe.anvil.util.InWorldRecipeContext;
import dev.dubhe.anvilcraft.recipe.anvil.util.InWorldRecipeManager;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownEnderpearl.class)
public abstract class ThrownEnderpearlMixin extends Entity {
    public ThrownEnderpearlMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void triggerRecipeAtSpeedAndHeight(CallbackInfo ci) {
        if (this.isRemoved()) return;
        Level level = this.level();
        if (!(level instanceof ServerLevel serverLevel)) return;
        Vec3 pos = this.position();
        ThrownEnderpearl thiS = Util.cast(this);
        InWorldRecipeManager manager = level.getRecipeManager().anc$getInWorldRecipeManager();
        InWorldRecipeContext context = new InWorldRecipeContext(serverLevel, pos, thiS);
        manager.trigger(ModRecipeTriggers.ON_ENDER_PEARL_TICK.get(), context);
        context.accept();
    }
}
