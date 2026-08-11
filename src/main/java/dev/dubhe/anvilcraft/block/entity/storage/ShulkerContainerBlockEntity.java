package dev.dubhe.anvilcraft.block.entity.storage;

import dev.dubhe.anvilcraft.block.container.storage.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.saved.storage.StorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ShulkerContainerBlockEntity extends StorageBlockEntity {
    private final Set<UUID> openers = new HashSet<>();
    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState blockState) {
            ShulkerContainerBlockEntity.setOpened(level, pos, blockState, true);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState blockState) {
            ShulkerContainerBlockEntity.setOpened(level, pos, blockState, false);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState blockState, int previous, int current) {
        }

        @Override
        public boolean isOwnContainer(Player player) {
            return ShulkerContainerBlockEntity.this.openers.contains(player.getUUID());
        }
    };

    public ShulkerContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, StorageType.SHULKER_CONTAINER);
    }

    public void setOpen(ServerPlayer player, boolean opened) {
        if (this.remove || this.level == null || player.isSpectator()) {
            return;
        }
        if (opened) {
            if (this.openers.add(player.getUUID())) {
                this.openersCounter.incrementOpeners(
                    player,
                    this.level,
                    this.worldPosition,
                    this.getBlockState()
                );
            }
        } else if (this.openers.remove(player.getUUID())) {
            this.openersCounter.decrementOpeners(player, this.level, this.worldPosition, this.getBlockState());
        }
    }

    public void recheckOpeners() {
        if (this.remove || this.level == null) {
            return;
        }
        Set<UUID> nearbyOpeners = new HashSet<>();
        for (Player player : this.level.players()) {
            if (!player.hasContainerOpen()) continue;
            double interactionRange = player.blockInteractionRange();
            if (player.distanceToSqr(this.worldPosition.getCenter()) < interactionRange * interactionRange) {
                nearbyOpeners.add(player.getUUID());
            }
        }
        this.openers.retainAll(nearbyOpeners);
        this.openersCounter.recheckOpeners(this.level, this.worldPosition, this.getBlockState());
        if (this.openersCounter.getOpenerCount() == 0) {
            ShulkerContainerBlockEntity.setOpened(this.level, this.worldPosition, this.getBlockState(), false);
        }
    }

    private static void setOpened(Level level, BlockPos pos, BlockState state, boolean opened) {
        if (state.getBlock() instanceof ShulkerContainerBlock block) {
            block.setOpened(level, pos, opened);
        }
    }
}
