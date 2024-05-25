package dev.dubhe.anvilcraft.block.entity;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.sound.SoundEventListener;
import dev.dubhe.anvilcraft.api.sound.SoundHelper;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.ActiveSilencerMenu;
import lombok.Getter;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;


public class ActiveSilencerBlockEntity extends BlockEntity implements MenuProvider, SoundEventListener, IDiskCloneable {
    public static final Codec<List<ResourceLocation>> CODEC =
            ResourceLocation.CODEC.listOf().fieldOf("mutedSound").codec();
    @Getter
    private final Set<ResourceLocation> mutedSound = new CopyOnWriteArraySet<>();

    private final AABB range;

    /**
     * 主动消音器
     */
    public ActiveSilencerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        range = AABB.ofSize(
                Vec3.atCenterOf(pos),
                31,
                31,
                31
        );
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        Tag t = CODEC.encodeStart(NbtOps.INSTANCE, new ArrayList<>(mutedSound))
                .getOrThrow(false, x -> {
                });
        tag.put("MutedSound", t);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        mutedSound.addAll(
                CODEC.decode(NbtOps.INSTANCE, tag.get("MutedSound"))
                        .getOrThrow(false, x -> {
                        }).getFirst()
        );
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        Tag t = CODEC.encodeStart(NbtOps.INSTANCE, new ArrayList<>(mutedSound))
                .getOrThrow(false, x -> {
                });
        tag.put("MutedSound", t);
        return tag;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        SoundHelper.INSTANCE.unregister(this);
    }


    @Override
    public void setLevel(@NotNull Level level) {
        super.setLevel(level);
        SoundHelper.INSTANCE.register(this);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("screen.anvilcraft.active_silencer.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
        return new ActiveSilencerMenu(
                ModMenuTypes.ACTIVE_SILENCER.get(),
                i,
                inventory,
                this
        );
    }

    /**
     * 添加声音
     */
    public void addSound(ResourceLocation soundId) {
        mutedSound.add(soundId);
        this.setChanged();
    }

    public void removeSound(ResourceLocation soundId) {
        mutedSound.remove(soundId);
        this.setChanged();
    }

    @Override
    public boolean shouldPlay(SoundInstance instance) {
        boolean inRange = range.contains(new Vec3(instance.getX(), instance.getY(), instance.getZ()));
        boolean inList = mutedSound.contains(instance.getLocation());
        return !inRange || !inList;
    }


    public void sync(List<ResourceLocation> sounds) {
        this.mutedSound.clear();
        this.mutedSound.addAll(sounds);
    }

    @Override
    public void storeDiskData(CompoundTag tag) {
        Tag t = CODEC.encodeStart(NbtOps.INSTANCE, new ArrayList<>(mutedSound))
                .getOrThrow(false, x -> {
                });
        tag.put("MutedSound", t);
    }

    @Override
    public void applyDiskData(CompoundTag data) {
        mutedSound.addAll(
                CODEC.decode(NbtOps.INSTANCE, data.get("MutedSound"))
                        .getOrThrow(false, x -> {
                        }).getFirst()
        );
    }
}
