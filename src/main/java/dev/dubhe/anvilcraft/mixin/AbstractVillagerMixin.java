package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.init.entity.ModTradeSets;
import dev.dubhe.anvilcraft.init.entity.ModVillagerTrades;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractVillager.class)
public class AbstractVillagerMixin {
    @WrapOperation(
        method = "addOffersFromTradeSet",
        at = {
            @At(value = "INVOKE",
                target = "Lnet/minecraft/world/entity/npc/villager/AbstractVillager;addOffersFromItemListings("
                    + "Lnet/minecraft/world/level/storage/loot/LootContext;Lnet/minecraft/world/item/trading/MerchantOffers;"
                    + "Lnet/minecraft/core/HolderSet;I)V"),
            @At(value = "INVOKE",
                target = "Lnet/minecraft/world/entity/npc/villager/AbstractVillager;addOffersFromItemListingsWithoutDuplicates("
                    + "Lnet/minecraft/world/level/storage/loot/LootContext;Lnet/minecraft/world/item/trading/MerchantOffers;"
                    + "Lnet/minecraft/core/HolderSet;I)V")
        }
    )
    private void addJewelerTrades(
        LootContext context, MerchantOffers offers, HolderSet<VillagerTrade> candidates, int count,
        Operation<Void> original, @Local(argsOnly = true) ResourceKey<TradeSet> tradeSet
    ) {
        HolderSet<VillagerTrade> randomCandidates = candidates;
        // 固定交易已列入数据集合，随机抽选时排除以免重复。
        if (tradeSet.equals(ModTradeSets.JEWELER_LEVEL_3)) {
            randomCandidates = HolderSet.direct(candidates.stream()
                .filter(trade -> !trade.is(ModVillagerTrades.EMERALD_FOR_ROYAL_STEEL_TEMPLATE)).toList());
        } else if (tradeSet.equals(ModTradeSets.JEWELER_LEVEL_4)) {
            AbstractVillagerMixin.addGuaranteedTrade(context, offers, ModVillagerTrades.NAUTILUS_SHELL_FOR_EMERALD);
            randomCandidates = HolderSet.direct(candidates.stream()
                .filter(trade -> !trade.is(ModVillagerTrades.NAUTILUS_SHELL_FOR_EMERALD)).toList());
        }
        original.call(context, offers, randomCandidates, count);
        if (tradeSet.equals(ModTradeSets.JEWELER_LEVEL_3)) {
            AbstractVillagerMixin.addGuaranteedTrade(context, offers, ModVillagerTrades.EMERALD_FOR_ROYAL_STEEL_TEMPLATE);
        }
    }

    @Unique
    private static void addGuaranteedTrade(LootContext context, MerchantOffers offers, ResourceKey<VillagerTrade> trade) {
        context.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE).getOptional(trade.identifier())
            .map(value -> value.getOffer(context))
            .ifPresent(offers::add);
    }
}
