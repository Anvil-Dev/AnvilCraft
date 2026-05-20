package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.ItemDetectorBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public enum ItemDetectorProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final Identifier UID = AnvilCraft.of("item_detector");

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof ItemDetectorBlockEntity blockEntity) {
            compoundTag.putInt("Range", blockEntity.getRange());
            compoundTag.putInt("FilterMode", blockEntity.getFilterMode().ordinal());
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
