package dev.dubhe.anvilcraft.util.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.dubhe.anvilcraft.api.block.entity.IConvertableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public record ConvertableBlockEntityEntry<T extends BlockEntity>(
    IConvertableBlockEntity<T> convertable,
    BlockPos pos,
    BlockState old,
    Operation<Void> removeOperation
) {
    public void apply(T newBe) {
        this.convertable().convertTo(newBe);
        this.remove();
    }

    public void remove() {
        this.removeOperation().call(this.convertable(), this.pos(), this.old());
    }
}
