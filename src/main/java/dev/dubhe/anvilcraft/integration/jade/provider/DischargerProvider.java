package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.DischargerBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public enum DischargerProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof DischargerBlockEntity discharger) {
            tag.putInt("discharger_timeLeft", discharger.getTimeLeft());
            tag.putInt("discharger_timeTotalCache", discharger.getTimeTotalCache());
            tag.putBoolean("discharger_feDischarging", discharger.isFeDischarging());
        }
    }

    @Override
    public Identifier getUid() {
        return AnvilCraft.of("discharger_provider");
    }
}
