package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class RedstoneDiceBlockEntity extends BlockEntity {
    public static final int ROLL_TICKS = 8;
    private static final int[] UNIFORM_OUTCOMES = {
        111, 112, 122, 123, 124, 224, 234, 235, 245, 345, 346, 446, 456, 466, 566, 666
    };
    @Getter
    private boolean uniform = true;
    private boolean powered;
    private boolean rolling;
    private int previousFaces = 111;
    private int faces = 111;
    @Getter
    private int output;
    private long rollStart = -1;

    public RedstoneDiceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REDSTONE_DICE.get(), pos, state);
    }

    public void setUniform(boolean uniform) {
        if (this.level == null || this.level.isClientSide || this.uniform == uniform) return;
        this.uniform = uniform;
        this.sync();
    }

    public int getFace(int index, boolean previous) {
        int outcome = previous ? this.previousFaces : this.faces;
        return outcome / (index == 0 ? 1 : index == 1 ? 10 : 100) % 10;
    }

    public int getScenario(boolean previous) {
        return this.getFace(0, previous) + this.getFace(1, previous) + this.getFace(2, previous) - 3;
    }

    public float getRollProgress(float partialTick) {
        if (this.level == null || this.rollStart < 0) return 1;
        return Mth.clamp((this.level.getGameTime() - this.rollStart + partialTick) / ROLL_TICKS, 0, 1);
    }

    public void checkInput() {
        if (this.level == null || this.level.isClientSide) return;
        boolean input = this.level.hasNeighborSignal(this.worldPosition);
        if (input == this.powered) return;
        this.powered = input;
        this.setChanged();
        if (input) this.roll();
    }

    public void roll() {
        if (this.level == null || this.level.isClientSide || this.rolling) return;
        this.previousFaces = this.faces;
        if (this.uniform) {
            this.faces = UNIFORM_OUTCOMES[this.level.random.nextInt(16)];
        } else {
            this.faces = (this.level.random.nextInt(6) + 1) * 100
                + (this.level.random.nextInt(6) + 1) * 10 + this.level.random.nextInt(6) + 1;
        }
        this.rolling = true;
        this.rollStart = this.level.getGameTime();
        this.level.scheduleTick(this.worldPosition, this.getBlockState().getBlock(), ROLL_TICKS);
        this.sync();
    }

    public void finishRoll() {
        if (this.level == null || this.level.isClientSide || !this.rolling) return;
        long remaining = this.rollStart + ROLL_TICKS - this.level.getGameTime();
        if (remaining > 0) {
            this.level.scheduleTick(this.worldPosition, this.getBlockState().getBlock(), (int) remaining);
            return;
        }
        this.output = this.getScenario(false);
        // Keep the roll locked while output notifications propagate back through adjacent wires.
        this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
        this.checkInput();
        this.rolling = false;
        this.sync();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level == null || this.level.isClientSide) return;
        if (this.rolling) {
            int delay = (int) Mth.clamp(this.rollStart + ROLL_TICKS - this.level.getGameTime(), 1, ROLL_TICKS);
            this.level.scheduleTick(this.worldPosition, this.getBlockState().getBlock(), delay);
        }
        this.checkInput();
    }

    private void sync() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Uniform", this.uniform);
        tag.putBoolean("Powered", this.powered);
        tag.putBoolean("Rolling", this.rolling);
        tag.putInt("PreviousFaces", this.previousFaces);
        tag.putInt("Faces", this.faces);
        tag.putInt("Output", this.output);
        tag.putLong("RollStart", this.rollStart);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.uniform = !tag.contains("Uniform") || tag.getBoolean("Uniform");
        this.powered = tag.getBoolean("Powered");
        this.rolling = tag.getBoolean("Rolling");
        this.previousFaces = validFaces(tag.getInt("PreviousFaces"));
        this.faces = validFaces(tag.getInt("Faces"));
        this.output = Mth.clamp(tag.getInt("Output"), 0, 15);
        this.rollStart = tag.contains("RollStart") ? tag.getLong("RollStart") : -1;
    }

    private static int validFaces(int value) {
        return value >= 111 && value <= 666 && value % 10 >= 1 && value % 10 <= 6
            && value / 10 % 10 >= 1 && value / 10 % 10 <= 6 ? value : 111;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
