package dev.dubhe.anvilcraft.item;

import net.minecraft.network.HashedStack;
import net.minecraft.server.level.ServerPlayer;

public interface IInventoryCarriedAware {
    void onCarriedUpdate(HashedStack stack, ServerPlayer serverPlayer);
}
