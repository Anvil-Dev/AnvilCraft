package dev.dubhe.anvilcraft.mixin.portal;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.event.EntityThroughPortalEvent;
import dev.dubhe.anvilcraft.api.portal.PortalType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.Portal;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NetherPortalBlock.class)
abstract class NetherPortalBlockMixin {
    @WrapOperation(
        method = "entityInside",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;setAsInsidePortal("
                     + "Lnet/minecraft/world/level/block/Portal;"
                     + "Lnet/minecraft/core/BlockPos;"
                     + ")V"
        )
    )
    private void fallBlockEntityInside(
        Entity instance,
        Portal portal,
        BlockPos pos,
        Operation<Void> original,
        @Local(argsOnly = true) Level level
    ) {
        NetherPortalBlock block = Util.cast(this);
        EntityThroughPortalEvent event = NeoForge.EVENT_BUS.post(new EntityThroughPortalEvent(
            level,
            instance,
            new PortalType(block)
        ));
        if (event.isCanceled()) return;
        original.call(event.getEntity(), portal, pos);
    }
}
