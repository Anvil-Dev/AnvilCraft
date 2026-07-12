package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.entity.ModVillagerTrades;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(AbstractVillager.class)
public class AbstractVillagerMixin {

    @Unique
    private static final Identifier JEWELER_TRADE_SET = AnvilCraft.of("jeweler/level_3");

    @SuppressWarnings("checkstyle:LineLength")
    @Inject(
        method = "addOffersFromTradeSet",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/trading/TradeSet;calculateNumberOfTrades(Lnet/minecraft/world/level/storage/loot/LootContext;)I"
        )
    )
    void addTrades(
        ServerLevel level,
        MerchantOffers offers,
        ResourceKey<TradeSet> resourceKey,
        CallbackInfo ci,
        @Local LootContext lootContext
    ) {
        if (resourceKey.identifier().equals(JEWELER_TRADE_SET)) {
            Optional<VillagerTrade> tradeSet = level.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE)
                .getOptional(ModVillagerTrades.EMERALD_FOR_ROYAL_STEEL_TEMPLATE.identifier());
            if (tradeSet.isEmpty()) return;
            offers.add(tradeSet.get().getOffer(lootContext));
        }
    }
}
