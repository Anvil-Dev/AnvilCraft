package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.init.ModDispenserBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(ZombieVillager.class)
public abstract class ZombieVillagerMixin extends Zombie {

    @Shadow
    private UUID conversionStarter;

    public ZombieVillagerMixin(EntityType<? extends ZombieVillager> entityType, Level level) {
        super(entityType, level);
    }

    @WrapOperation(
        method = "lambda$finishConversion$0",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/npc/villager/Villager;refreshBrain(Lnet/minecraft/server/level/ServerLevel;)V"
        )
    )
    private void discountForAllPlayers(Villager villager, ServerLevel serverLevel, Operation<Void> original) {
        if (ModDispenserBehavior.ANVILCRAFT_DISPENSER.equals(this.conversionStarter)) {
            serverLevel.getServer()
                .getPlayerList()
                .getPlayers()
                .forEach(p -> serverLevel.onReputationEvent(ReputationEventType.ZOMBIE_VILLAGER_CURED, p, villager));
            this.conversionStarter = null;
        }
        original.call(villager, serverLevel);
    }
}
