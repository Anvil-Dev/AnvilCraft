package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.BigRedButtonBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BigRedButtonBlockEntity extends BlockEntity {
    private static final int HOLD_TIMEOUT = 20;
    private final Map<UUID, Long> holders = new HashMap<>();
    private float previousProgress;
    private float progress;

    public BigRedButtonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIG_RED_BUTTON.get(), pos, state);
        this.progress = state.getValue(BigRedButtonBlock.PRESSED) ? 1 : 0;
        this.previousProgress = this.progress;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide && this.getBlockState().getValue(BigRedButtonBlock.PRESSED)) {
            this.level.scheduleTick(this.worldPosition, this.getBlockState().getBlock(), 1);
        }
    }

    public void press(Player player) {
        if (this.level == null || this.level.isClientSide || !player.isAlive() || player.isSpectator()) return;
        this.holders.put(player.getUUID(), this.level.getGameTime());
        this.setPressed(true);
        this.level.scheduleTick(this.worldPosition, this.getBlockState().getBlock(), 1);
    }

    public void release(Player player) {
        if (this.level == null || this.level.isClientSide) return;
        this.holders.remove(player.getUUID());
        this.level.scheduleTick(this.worldPosition, this.getBlockState().getBlock(), 1);
    }

    public void checkPressed() {
        if (this.level == null || this.level.isClientSide) return;
        this.holders.entrySet().removeIf(entry -> {
            Player player = this.level.getPlayerByUUID(entry.getKey());
            return player == null || !player.isAlive() || player.isSpectator()
                || !player.canInteractWithBlock(this.worldPosition, 1.0)
                || this.level.getGameTime() - entry.getValue() > HOLD_TIMEOUT;
        });
        this.setPressed(!this.holders.isEmpty());
        if (!this.holders.isEmpty()) {
            this.level.scheduleTick(this.worldPosition, this.getBlockState().getBlock(), 1);
        }
    }

    private void setPressed(boolean pressed) {
        if (this.level == null) return;
        BlockState state = this.getBlockState();
        if (state.getValue(BigRedButtonBlock.PRESSED) == pressed) return;
        this.level.setBlock(this.worldPosition, state.setValue(BigRedButtonBlock.PRESSED, pressed), 3);
        ((BigRedButtonBlock) state.getBlock()).updateNeighbours(state, this.level, this.worldPosition);
        this.level.playSound(
            null, this.worldPosition, pressed ? SoundEvents.STONE_BUTTON_CLICK_ON : SoundEvents.STONE_BUTTON_CLICK_OFF,
            SoundSource.BLOCKS, 0.3f, pressed ? 0.6f : 0.5f
        );
        this.level.gameEvent(null, pressed ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, this.worldPosition);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, BigRedButtonBlockEntity button) {
        button.previousProgress = button.progress;
        button.progress = Mth.approach(button.progress, state.getValue(BigRedButtonBlock.PRESSED) ? 1 : 0, 0.5f);
    }

    public float getPressProgress(float partialTick) {
        float value = Mth.lerp(partialTick, this.previousProgress, this.progress);
        return value * value * (3 - 2 * value);
    }
}
