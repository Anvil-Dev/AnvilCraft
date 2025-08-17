package dev.dubhe.anvilcraft.event.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilHurtEntityEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class AnvilHurtVillagerEventListener {
    /**
     * 侦听铁砧击中村民事件
     *
     * @param event 铁砧伤害实体事件
     */
    @SubscribeEvent
    public static void onAnvilHurtEntity(@NotNull AnvilHurtEntityEvent event) {
        Entity entity = event.getHurtedEntity();
        Level level = event.getLevel();
        if (level.isClientSide()) return;
        if (entity instanceof Villager villager) {
            RandomSource random = level.random;
            VillagerData villageData = villager.getVillagerData();

            villager.releasePoi(MemoryModuleType.HOME);
            villager.releasePoi(MemoryModuleType.JOB_SITE);
            villager.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
            villager.releasePoi(MemoryModuleType.MEETING_POINT);

            if (villageData.getProfession() == VillagerProfession.NITWIT) {
                return;
            }

            if (random.nextDouble() <= 0.2) {
                villageData = villageData.setProfession(VillagerProfession.NITWIT);
            } else {
                villageData = villageData.setProfession(VillagerProfession.NONE).setLevel(1);
                villager.setVillagerXp(0);
            }
            villager.setVillagerData(villageData);
        }
        if (entity instanceof WanderingTrader trader) {
            BlockPos pos = event.getPos();
            VillagerType type = VillagerType.byBiome(level.getBiome(pos));
            VillagerProfession profession = VillagerProfession.NONE;
            RandomSource random = level.random;
            double chance = random.nextDouble();
            if (chance < 0.15) {
                profession = VillagerProfession.NITWIT;
            } else if (chance < 0.25) {
                profession = VillagerProfession.FARMER;
            }
            VillagerData villageData = new VillagerData(type, profession, 1);
            Villager villager = new Villager(EntityType.VILLAGER, level);
            villager.setPos(trader.position());
            villager.setPose(trader.getPose());
            villager.setXRot(trader.getXRot());
            villager.setYRot(trader.getYRot());
            villager.setYHeadRot(trader.getYHeadRot());
            MerchantOffers offers = new MerchantOffers();
            if (profession == VillagerProfession.FARMER) {
                villager.setVillagerXp(250);
                villageData = villageData.setLevel(5);
                villager.setVillagerData(villageData);
                for (MerchantOffer offer : trader.getOffers()) {
                    offers.add(offer.copy());
                }
            }
            villager.setOffers(offers);
            trader.remove(Entity.RemovalReason.DISCARDED);
            villager.finalizeSpawn(
                (ServerLevelAccessor) level,
                level.getCurrentDifficultyAt(villager.blockPosition()),
                MobSpawnType.CONVERSION,
                null
            );
            ((ServerLevel) level).tryAddFreshEntityWithPassengers(villager);
            villager.refreshBrain((ServerLevel) level);
        }
    }
}
