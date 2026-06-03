package dev.dubhe.anvilcraft.api.block.entity;

import net.minecraft.core.Holder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public interface IConvertableBlockEntity<T extends BlockEntity> {
    Holder<BlockEntityType<?>> targetTypeHolder();

    void convertTo(T newBe);
}
