package dev.dubhe.anvilcraft.api.block.entity;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public interface IExtensibleBlockEntity<T extends BlockEntity> {
    BlockEntityType<T> getThatType();

    void extend(T newBe);
}
