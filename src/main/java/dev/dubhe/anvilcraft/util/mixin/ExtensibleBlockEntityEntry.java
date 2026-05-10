package dev.dubhe.anvilcraft.util.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.block.entity.IExtensibleBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public record ExtensibleBlockEntityEntry<T extends BlockEntity>(
    IExtensibleBlockEntity<T> extensible,
    BlockState old,
    Operation<Void> removeOperation
) {
    public void remove() {
        BlockEntity extensible = Util.cast(this.extensible);
        this.removeOperation().call(extensible, extensible.getBlockPos(), this.old);
    }

    public void apply(T newBe) {
        this.extensible.extend(newBe);
    }
}
