package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.ChargerBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public enum ChargerProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof ChargerBlockEntity charger) {
            tag.putInt("charger_timeLeft", charger.getTimeLeft());
            tag.putInt("charger_timeTotalCache", charger.getTimeTotalCache());
            tag.putBoolean("charger_feCharging", charger.isFeCharging());
        }
    }

    @Override
    public Identifier getUid() {
        return AnvilCraft.of("charger_provider");
    }
}
