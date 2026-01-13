package dev.dubhe.anvilcraft.util.sc;

import dev.dubhe.anvilcraft.block.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.saved.sc.client.ClientSCStorages;
import dev.dubhe.anvilcraft.saved.sc.server.ServerSCStorages;
import dev.dubhe.anvilcraft.util.Util;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class SCBESyncer {
    private final BlockPos mainPos;
    @Getter
    private UUID storageId;
    private int openCount;

    public SCBESyncer(BlockPos mainPos) {
        this.mainPos = mainPos;
    }

    public void setStorageId(UUID storageId) {
        if (this.storageId == null) this.storageId = storageId;
        if (Util.isServer()) {
            ServerSCStorages.get().create(storageId);
        } else {
            ClientSCStorages.create(storageId);
        }
    }

    public void someoneOpened(Level level, BlockState state) {
        int i = this.openCount++;
        if (i == 0) {
            ShulkerContainerBlock.updateState(state.getBlock(), level, this.mainPos, ShulkerContainerBlock.OPENED, true, Block.UPDATE_ALL);
            SCBESyncer.playSound(level, this.mainPos, state, SoundEvents.SHULKER_BOX_OPEN);
        }
    }

    public void someoneClosed(Level level, BlockState state) {
        this.openCount--;
        if (this.openCount == 0) {
            ShulkerContainerBlock.updateState(state.getBlock(), level, this.mainPos, ShulkerContainerBlock.OPENED, false, Block.UPDATE_ALL);
            SCBESyncer.playSound(level, this.mainPos, state, SoundEvents.SHULKER_BOX_CLOSE);
        }
    }

    static void playSound(Level level, BlockPos pos, BlockState state, SoundEvent sound) {
        level.playSound(
            null,
            state.getValue(ShulkerContainerBlock.HALF).toMain(pos),
            sound,
            SoundSource.BLOCKS,
            0.5F,
            level.random.nextFloat() * 0.1F + 0.9F
        );
    }
}
