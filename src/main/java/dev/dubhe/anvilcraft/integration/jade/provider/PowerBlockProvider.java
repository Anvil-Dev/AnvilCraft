package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public enum PowerBlockProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final Identifier UID = AnvilCraft.of("power_provider");

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof IPowerComponent blockEntity) {
            PowerGrid powerGrid = blockEntity.getGrid();
            if (powerGrid == null) {
                return;
            }
            compoundTag.putInt("generate", powerGrid.getGenerate());
            compoundTag.putInt("consume", powerGrid.getConsume());
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
