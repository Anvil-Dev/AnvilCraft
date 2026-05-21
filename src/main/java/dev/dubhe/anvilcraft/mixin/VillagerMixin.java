package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModDataAttachments;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerMixin extends AbstractVillager {
    public VillagerMixin(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(
        method = "updateSpecialPrices",
        at = @At(value = "RETURN")
    )
    private void updateAmuletSpecialPrices(Player player, CallbackInfo ci) {
        // 如果需要不叠加，就加上&& !player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)
        if (player.hasData(ModDataAttachments.DISCOUNT_RATE)) {
            double d = player.getData(ModDataAttachments.DISCOUNT_RATE);
            if (d == 0F) return;
            for (MerchantOffer merchantOffer : this.getOffers()) {
                int k = (int) Math.floor(d * merchantOffer.getBaseCostA().getCount());
                merchantOffer.addToSpecialPriceDiff(-Math.max(k, 1));
            }
        }
    }
}
