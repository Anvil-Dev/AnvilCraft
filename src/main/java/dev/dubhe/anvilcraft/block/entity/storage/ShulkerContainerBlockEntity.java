package dev.dubhe.anvilcraft.block.entity.storage;

import dev.dubhe.anvilcraft.block.container.storage.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.init.storage.ModStorageTypes;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
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
        super(type, pos, state, ModStorageTypes.SHULKER_CONTAINER);
    }

    public boolean isEmpty() {
        if (this.getTotalCount() != 0) {
            return false;
        }
        UUID id = this.getId();
        if (id == null) {
            return true;
        }
        return Storages.get().get(this.getId(), ShulkerContainerStorage.class)
            .map(storage -> storage.getItems().getTypeLimit() == ShulkerContainerStorage.DEFAULT_TYPE_LIMIT)
            .orElse(false);
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
        this.openers.removeIf(uuid -> {
            Player player = this.level.getPlayerByUUID(uuid);
            if (player == null || !player.isAlive()) {
                return true;
            }
            double interactionRange = player.blockInteractionRange();
            return player.distanceToSqr(this.getBlockPos().getCenter()) >= interactionRange * interactionRange;
        });
        this.openersCounter.recheckOpeners(this.level, this.getBlockPos(), this.getBlockState());
        if (this.openersCounter.getOpenerCount() == 0) {
            ShulkerContainerBlockEntity.setOpened(this.level, this.getBlockPos(), this.getBlockState(), false);
        }
    }

    private static void setOpened(Level level, BlockPos pos, BlockState state, boolean opened) {
        if (state.getBlock() instanceof ShulkerContainerBlock block) {
            block.setOpened(level, pos, opened);
        }
    }
}
