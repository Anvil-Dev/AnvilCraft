package dev.dubhe.anvilcraft.mixin.accessor;

import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Villager.class)
public interface VillagerAccessor {

    @Accessor
    void setUpdateMerchantTimer(int timer);

    @Accessor
    void setIncreaseProfessionLevelOnUpdate(boolean flag);

    @Invoker
    boolean invokeShouldIncreaseLevel();
}
