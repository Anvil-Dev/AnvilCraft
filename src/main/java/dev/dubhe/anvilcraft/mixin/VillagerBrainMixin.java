package dev.dubhe.anvilcraft.mixin;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.entity.ai.behavior.TradeAtStationBehavior;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(Villager.class)
public abstract class VillagerBrainMixin {
    @Inject(
        method = "registerBrainGoals",
        at = @At("TAIL")
    )
    private void anvilcraft$addTradingStationBehavior(Brain<Villager> brain, CallbackInfo ci) {
        if (((Villager) (Object) this).isBaby()) return;
        brain.addActivityWithConditions(
            Activity.WORK,
            ImmutableList.of(Pair.of(6, (BehaviorControl<? super Villager>) new TradeAtStationBehavior())),
            Set.of(Pair.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT))
        );
    }
}
