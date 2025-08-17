package dev.dubhe.anvilcraft.mixin.accessor;

import net.minecraft.world.level.block.entity.vault.VaultServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;
import java.util.UUID;

@Mixin(VaultServerData.class)
public interface VaultServerDataAccessor {

    @Invoker(value = "markChanged")
    void invoker$markChanged();

    @Accessor(value = "rewardedPlayers")
    Set<UUID> getRewardedPlayers();
}
