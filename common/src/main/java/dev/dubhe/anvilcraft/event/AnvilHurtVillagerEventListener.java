package dev.dubhe.anvilcraft.event;

import dev.anvilcraft.lib.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.event.entity.AnvilHurtEntityEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.Level;
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
        Level level = event.getLevel();
        if (event.getHurtedEntity() instanceof Villager villager) {
            RandomSource random = event.getLevel().random;
            double change = random.nextDouble();
            if (change <= 0.2) {
                villager.setVillagerData(villager.getVillagerData().setProfession(
                    VillagerProfession.NITWIT));
                return;
            }
            VillagerData villagerData = villager.getVillagerData();
            if (villagerData.getProfession() == VillagerProfession.NITWIT) {
                villager.setVillagerData(villagerData.setProfession(VillagerProfession.NITWIT));
            } else villager.setVillagerData(villagerData.setProfession(VillagerProfession.NONE));
            villager.releasePoi(MemoryModuleType.HOME);
            villager.releasePoi(MemoryModuleType.JOB_SITE);
            villager.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
            villager.releasePoi(MemoryModuleType.MEETING_POINT);
        }
    }
}
