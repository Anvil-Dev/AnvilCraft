package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.building.BuildingRodUndo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TntBlock.class)
abstract class BlueprintTntOriginMixin {
    @WrapOperation(method = "prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;"
        + "Lnet/minecraft/world/entity/LivingEntity;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean anvilcraft$trackPrimed(
        Level level, Entity entity, Operation<Boolean> original, @Local(argsOnly = true) BlockPos pos
    ) {
        boolean added = original.call(level, entity);
        if (added) BuildingRodUndo.spawnedAt(level, pos, entity);
        return added;
    }

    @WrapOperation(method = "wasExploded", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean anvilcraft$trackExploded(
        ServerLevel level, Entity entity, Operation<Boolean> original, @Local(argsOnly = true) BlockPos pos
    ) {
        boolean added = original.call(level, entity);
        if (added) BuildingRodUndo.spawnedAt(level, pos, entity);
        return added;
    }
}
