
package dev.dubhe.anvilcraft.block.entity;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.pointer.ITargetPointer;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.SmartBlockPlacerMenu;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Locale;
import javax.annotation.Nullable;

@Getter
@Setter
public class SmartBlockPlacerBlockEntity extends BlockEntity implements IPowerConsumer, MenuProvider, IDiskCloneable, IItemHandlerHolder {
    private static final int PLACEMENT_INTERVAL = 20;
    private static final int PLACEMENT_DELAY = 6;

    private @Nullable PowerGrid grid;

    private OperationMode operation = OperationMode.PICKUP;
    private TargetMode target = TargetMode.POSITION;
    private ExecutionPhase phase = ExecutionPhase.IDLE;
    /**
     * 当前阶段的执行进度；<br>
     * 仅在 {@link SmartBlockPlacerBlockEntity#phase} 不为 {@link ExecutionPhase#IDLE} 时可用
     */
    private float progress = 0.0F;
    private @Nullable ITargetPointer pointer;

    private ITargetPointer findPointer(ServerLevel level) { // TODO: 事件驱动
        if ()
    }

    // region BlockEntity - Update
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        this.saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    // endregion

    // region BlockEntity - Save
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        tag.put("operation", OperationMode.CODEC.encodeStart(ops, this.operation).getOrThrow());
        tag.put("target", TargetMode.CODEC.encodeStart(ops, this.target).getOrThrow());
        tag.put("phase", ExecutionPhase.CODEC.encodeStart(ops, this.phase).getOrThrow());
        tag.putFloat("progress", this.progress);
        if (this.pointer != null) {
            tag.put("pointer", ITargetPointer.CODEC.encodeStart(ops, this.pointer).getOrThrow());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        this.operation = OperationMode.CODEC.decode(ops, tag.get("operation")).getOrThrow().getFirst();
        this.target = TargetMode.CODEC.decode(ops, tag.get("target")).getOrThrow().getFirst();
        this.phase = ExecutionPhase.CODEC.decode(ops, tag.get("phase")).getOrThrow().getFirst();
        this.progress = tag.getFloat("progress");
        if (tag.contains("pointer")) {
            this.pointer = ITargetPointer.CODEC.decode(ops, tag.get("pointer")).getOrThrow().getFirst();
        }
    }
    // endregion

    // region IPowerConsumer
    @Override
    public int getInputPower() {
        return this.target.getPower();
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.level;
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }
    // endregion

    // region MenuProvider
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.anvilcraft.smart_block_placer");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SmartBlockPlacerMenu(ModMenuTypes.SMART_BLOCK_PLACER.get(), containerId, inventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.getBlockPos());
    }
    // endregion

    // region IDiskCloneable
    @Override
    public void storeDiskData(CompoundTag tag) {
        if (this.getLevel() == null) {
            return;
        }
        RegistryOps<Tag> ops = this.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        tag.put("operation", OperationMode.CODEC.encodeStart(ops, this.operation).getOrThrow());
        tag.put("target", TargetMode.CODEC.encodeStart(ops, this.target).getOrThrow());
        tag.put("phase", ExecutionPhase.CODEC.encodeStart(ops, this.phase).getOrThrow());
        tag.putFloat("progress", this.progress);
        if (this.pointer != null) {
            tag.put("pointer", ITargetPointer.CODEC.encodeStart(ops, this.pointer).getOrThrow());
        }
    }

    @Override
    public void applyDiskData(CompoundTag data) {
        if (this.getLevel() == null) {
            return;
        }
        RegistryOps<Tag> ops = this.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        this.operation = OperationMode.CODEC.decode(ops, data.get("operation")).getOrThrow().getFirst();
        this.target = TargetMode.CODEC.decode(ops, data.get("target")).getOrThrow().getFirst();
        this.phase = ExecutionPhase.CODEC.decode(ops, data.get("phase")).getOrThrow().getFirst();
        this.progress = data.getFloat("progress");
        if (data.contains("pointer")) {
            this.pointer = ITargetPointer.CODEC.decode(ops, data.get("pointer")).getOrThrow().getFirst();
        }
    }
    // endregion

    public enum OperationMode implements StringRepresentable {
        PICKUP,
        MOVE,
        ;

        public static final Codec<OperationMode> CODEC = StringRepresentable.fromEnum(OperationMode::values);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    @Getter
    public enum TargetMode implements StringRepresentable {
        POSITION(16),
        BLUEPRINT(128),
        ;

        public static final Codec<TargetMode> CODEC = StringRepresentable.fromEnum(TargetMode::values);
        private final int power;

        TargetMode(int power) {
            this.power = power;
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    @Getter
    public enum ExecutionPhase implements StringRepresentable {
        IDLE(0.0F),
        PREPARE(0.3F),
        EXTEND(0.4F),
        RESET(0.3F),
        ;

        public static final Codec<ExecutionPhase> CODEC = StringRepresentable.fromEnum(ExecutionPhase::values);
        private final float intervalPercent;

        ExecutionPhase(float intervalPercent) {
            this.intervalPercent = intervalPercent;
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
