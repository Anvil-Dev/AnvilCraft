package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.api.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.event.entity.AnvilHurtEntityEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.jetbrains.annotations.NotNull;

public class AnvilHurtVillagerEventListener {
    /**
     * 侦听铁砧击中村民事件
     *
     * @param event 铁砧伤害实体事件
     */
    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onAnvilHurtEntity(@NotNull AnvilHurtEntityEvent event) {
        if (event.getHurtedEntity() instanceof Villager villager) {
            RandomSource random = event.getLevel().random;
            double change = random.nextDouble();
            if (change <= 0.2) {
                villager.setVillagerData(villager.getVillagerData().setProfession(
                    VillagerProfession.NITWIT));
                return;
            }
            VillagerData villagerData = villager.getVillagerData();
            Villager changedVillager = villager.convertTo(EntityType.VILLAGER, true);
            if (changedVillager == null) return;
            if (villagerData.getProfession() == VillagerProfession.NITWIT) {
                changedVillager.setVillagerData(villagerData.setProfession(VillagerProfession.NITWIT));
            } else changedVillager.setVillagerData(villagerData.setProfession(VillagerProfession.NONE));
        }
    }
}
