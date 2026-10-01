package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.item.EquipmentAbilities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class EquipmentLiquidBlockMixin {
    @Shadow
    public abstract FluidState getFluidState();

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;"
        + "Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
        at = @At("RETURN"), cancellable = true)
    private void anvilcraft$standOnFluid(BlockGetter level, BlockPos pos, CollisionContext context,
                                        CallbackInfoReturnable<VoxelShape> cir) {
        if (!(context instanceof EntityCollisionContext entityContext)
            || !(entityContext.getEntity() instanceof LivingEntity entity)) return;
        FluidState fluid = this.getFluidState();
        if (!EquipmentAbilities.canStandOnFluid(entity, fluid) || !level.getFluidState(pos.above()).isEmpty()) return;
        VoxelShape surface = Shapes.box(0, 0, 0, 1, fluid.getHeight(level, pos), 1);
        if (context.isAbove(surface, pos, false)) cir.setReturnValue(Shapes.or(cir.getReturnValue(), surface));
    }
}
