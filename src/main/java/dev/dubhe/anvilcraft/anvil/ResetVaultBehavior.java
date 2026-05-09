package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.mixin.accessor.VaultServerDataAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class ResetVaultBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(
        ServerLevel level,
        BlockPos hitBlockPos,
        BlockState hitBlockState,
        double fallDistance,
        AnvilEvent.OnLand event
    ) {
        if (!level.getBlockState(hitBlockPos).is(ModBlockTags.STORAGE_BLOCKS_LEAD)) return false;
        BlockPos vaultPos = hitBlockPos.below();
        Optional.ofNullable(level.getBlockEntity(vaultPos))
            .filter(VaultBlockEntity.class::isInstance)
            .map(VaultBlockEntity.class::cast)
            .map(VaultBlockEntity::getServerData)
            .ifPresent(vaultServerData -> {
                level.setBlockAndUpdate(hitBlockPos, Blocks.AIR.defaultBlockState());
                VaultServerDataAccessor vaultServerDataAccessor = (VaultServerDataAccessor) vaultServerData;
                vaultServerDataAccessor.getRewardedPlayers().clear();
                vaultServerDataAccessor.invokeMarkChanged();
            });
        return false;
    }
}
