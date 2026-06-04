package dev.dubhe.anvilcraft.block.entity;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.network.util.NetworkUtil;
import dev.anvilcraft.lib.v2.util.DistExecutor;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.sound.ISoundEventListener;
import dev.dubhe.anvilcraft.api.sound.SoundHelper;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import dev.dubhe.anvilcraft.block.utility.ActiveSilencerBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.ActiveSilencerMenu;
import dev.dubhe.anvilcraft.network.SilencerSyncPacket;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ActiveSilencerBlockEntity
    extends BlockEntity
    implements MenuProvider, ISoundEventListener, IDiskCloneable, IHasAffectRange {
    public static final Codec<List<Identifier>> CODEC =
        Identifier.CODEC.listOf().fieldOf("mutedSound").codec();

    @Getter
    private final Set<Identifier> muting = new CopyOnWriteArraySet<>();

    private final AABB range;

    /// 主动消音器
    public ActiveSilencerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.range = AABB.ofSize(Vec3.atCenterOf(pos), 31, 31, 31);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ValueOutput.TypedOutputList<Identifier> muted = output.list("MutedSounds", Identifier.CODEC);
        for (Identifier sound : this.muting) {
            muted.add(sound);
        }
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.listOrEmpty("MutedSounds", Identifier.CODEC).forEach(this.muting::add);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag muted = new ListTag();
        for (Identifier identifier : this.muting) {
            muted.add(StringTag.valueOf(identifier.toShortString()));
        }
        tag.put("MutedSounds", muted);
        return tag;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        DistExecutor.run(Dist.CLIENT, () -> () -> SoundHelper.INSTANCE.unregister(this.level, this));
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        DistExecutor.run(Dist.CLIENT, () -> () -> SoundHelper.INSTANCE.register(level, this));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.anvilcraft.active_silencer.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        if (player.isSpectator()) return null;
        return new ActiveSilencerMenu(ModMenuTypes.ACTIVE_SILENCER.get(), i, inventory, this);
    }

    /// 添加声音
    public void addSound(Identifier soundId) {
        this.muting.add(soundId);
        this.setChanged();
    }

    public void removeSound(Identifier soundId) {
        this.muting.remove(soundId);
        this.setChanged();
    }

    @Override
    public boolean shouldMute(Identifier sound, Vec3 pos) {
        if (getBlockState().getValue(ActiveSilencerBlock.POWERED)) return true;
        boolean inRange = this.range.contains(pos);
        boolean inList = this.muting.contains(sound);
        return inRange && inList;
    }

    public void sync(List<Identifier> sounds) {
        this.muting.clear();
        this.muting.addAll(sounds);
    }

    public void sync(Player player, List<Identifier> sounds) {
        this.sync(sounds);
        if (!(this.getLevel() instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) return;
        NetworkUtil.sendToAllPlayersInDimensionExcluded(
            serverLevel,
            serverPlayer,
            new SilencerSyncPacket(this.getBlockPos(), List.copyOf(this.muting))
        );
    }

    @Override
    public void storeDiskData(ValueOutput output) {
        ValueOutput.TypedOutputList<Identifier> muted = output.list("MutedSounds", Identifier.CODEC);
        for (Identifier identifier : this.muting) {
            muted.add(identifier);
        }
    }

    @Override
    public void applyDiskData(ValueInput input) {
        input.listOrEmpty("MutedSounds", Identifier.CODEC).forEach(this.muting::add);
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public AABB shape() {
        return this.range;
    }
}
