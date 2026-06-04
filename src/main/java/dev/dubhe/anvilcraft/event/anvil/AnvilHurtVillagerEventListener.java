package dev.dubhe.anvilcraft.event.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class AnvilHurtVillagerEventListener {
    /// 侦听铁砧击中村民事件
    ///
    /// @param event 铁砧伤害实体事件
    @SubscribeEvent
    public static void onAnvilHurtEntity(AnvilEvent.HurtEntity event) {
        Entity entity = event.getHurtedEntity();
        ServerLevel level = event.getLevel();
        if (level.isClientSide()) return;
        if (entity instanceof Villager villager) {
            final RandomSource random = level.getRandom();

            villager.releasePoi(MemoryModuleType.HOME);
            villager.releasePoi(MemoryModuleType.JOB_SITE);
            villager.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
            villager.releasePoi(MemoryModuleType.MEETING_POINT);

            VillagerData villageData = villager.getVillagerData();

            if (villageData.profession().is(VillagerProfession.NITWIT)) {
                return;
            }

            if (random.nextDouble() <= 0.2) {
                villageData = villageData.withProfession(level.registryAccess(), VillagerProfession.NITWIT);
            } else {
                villageData = villageData
                    .withProfession(level.registryAccess(), VillagerProfession.NONE)
                    .withLevel(1);
                villager.setVillagerXp(0);
            }
            villager.setVillagerData(villageData);
        }
        if (entity instanceof WanderingTrader trader) {
            final BlockPos pos = event.getPos();
            final ResourceKey<VillagerType> typeKey = VillagerType.byBiome(level.getBiome(pos));
            final Holder<VillagerType> type = level.registryAccess().getOrThrow(typeKey);
            ResourceKey<VillagerProfession> professionKey = VillagerProfession.NONE;
            RandomSource random = level.getRandom();
            double chance = random.nextDouble();
            if (chance < 0.15) {
                professionKey = VillagerProfession.NITWIT;
            } else if (chance < 0.25) {
                professionKey = VillagerProfession.FARMER;
            }
            Holder<VillagerProfession> profession = level.registryAccess().getOrThrow(professionKey);
            Villager villager = new Villager(EntityType.VILLAGER, level);
            villager.setPos(trader.position());
            villager.setPose(trader.getPose());
            villager.setXRot(trader.getXRot());
            villager.setYRot(trader.getYRot());
            villager.setYHeadRot(trader.getYHeadRot());
            MerchantOffers offers = new MerchantOffers();
            VillagerData villageData = new VillagerData(type, profession, 1);
            if (professionKey == VillagerProfession.FARMER) {
                villager.setVillagerXp(250);
                villageData = villageData.withLevel(5);
                villager.setVillagerData(villageData);
                for (MerchantOffer offer : trader.getOffers()) {
                    offers.add(offer.copy());
                }
            }
            villager.setOffers(offers);
            trader.remove(Entity.RemovalReason.DISCARDED);
            villager.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(villager.blockPosition()),
                EntitySpawnReason.CONVERSION,
                null
            );
            level.tryAddFreshEntityWithPassengers(villager);
            villager.refreshBrain(level);
        }
    }
}
