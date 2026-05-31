package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.amulet.DiscountAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.IAmulet;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

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
        AmuletManager manager = AmuletManager.get(player.registryAccess());
        List<ItemStack> amulets = manager.getAmuletsFromInventory(player);
        for (ItemStack stack : amulets) {
            IAmulet amulet = stack.get(ModComponents.AMULET);
            if (!(amulet instanceof DiscountAmulet(float rate))) continue;
            if (rate <= 0F) return;
            for (MerchantOffer merchantOffer : this.getOffers()) {
                int k = (int) Math.floor(rate * merchantOffer.getBaseCostA().getCount());
                merchantOffer.addToSpecialPriceDiff(-Math.max(k, 1));
            }
            return;
        }
    }
}
