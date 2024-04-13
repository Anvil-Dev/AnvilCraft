package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AxeItem.class)
abstract class AxeItemMixin {
    @Inject(
        method = "useOn",
        at = @At(
            value = "INVOKE",
            ordinal = 0,
            target = "Lnet/minecraft/world/level/Level;"
                + "playSound("
                + "Lnet/minecraft/world/entity/player/Player;"
                + "Lnet/minecraft/core/BlockPos;"
                + "Lnet/minecraft/sounds/SoundEvent;"
                + "Lnet/minecraft/sounds/SoundSource;FF"
                + ")V"
        )
    )
    private void useOn(@NotNull UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getPlayer() == null || context.getPlayer().isCreative()) return;
        Direction direction = context.getHorizontalDirection().getOpposite();
        BlockPos pos = context.getClickedPos().relative(direction);
        Vec3 vec3 = pos.getCenter().relative(direction, -0.25);
        Vec3 move = pos.getCenter().subtract(vec3);
        ItemEntity entity = new ItemEntity(
            context.getLevel(),
            vec3.x, vec3.y, vec3.z,
            ModItems.BARK.get().getDefaultInstance(),
            move.x, move.y, move.z
        );
        context.getLevel().addFreshEntity(entity);
    }
}
