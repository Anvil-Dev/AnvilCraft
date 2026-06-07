package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyRandomizer;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CelestialForgingAnvilBlockEntity extends BlockEntity {
    @Getter
    private int preRotation = 0;
    @Getter
    private int rotation = 0;

    @Getter
    private boolean isAmplify = false;

    @Getter
    @Nullable
    private CelestialBodyData celestialBodyData = null;

    @Getter
    private long bodySeed = 0;

    public CelestialForgingAnvilBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Getter
    private int bodyRotation = 0;

    public void tick() {
        if (this.rotation == 360) this.rotation = 0;
        this.preRotation = this.rotation;
        this.rotation += 3;
        this.bodyRotation += 1;
    }

    public void setAmplify(boolean amplify) {
        if (this.isAmplify != amplify) {
            this.isAmplify = amplify;
            if (level != null && !level.isClientSide()) {
                randomizeBody();
            }
            this.setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    public void randomizeBody() {
        if (level != null) {
            this.bodySeed = level.getRandom().nextLong();
            this.celestialBodyData = CelestialBodyRandomizer.randomize(this.isAmplify, level.getRandom());
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide() && celestialBodyData == null) {
            randomizeBody();
            this.setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("amplified", this.isAmplify);
        tag.putLong("bodySeed", this.bodySeed);
        if (celestialBodyData != null) {
            tag.put("celestialBody", celestialBodyData.toTag());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.isAmplify = tag.getBoolean("amplified");
        this.bodySeed = tag.getLong("bodySeed");
        if (tag.contains("celestialBody")) {
            this.celestialBodyData = CelestialBodyData.fromTag(tag.getCompound("celestialBody"));
        } else {
            this.celestialBodyData = null;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("amplified", this.isAmplify);
        tag.putLong("bodySeed", this.bodySeed);
        if (celestialBodyData != null) {
            tag.put("celestialBody", celestialBodyData.toTag());
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider lookupProvider) {
        super.handleUpdateTag(tag, lookupProvider);
        this.isAmplify = tag.getBoolean("amplified");
        this.bodySeed = tag.getLong("bodySeed");
        if (tag.contains("celestialBody")) {
            this.celestialBodyData = CelestialBodyData.fromTag(tag.getCompound("celestialBody"));
        } else {
            this.celestialBodyData = null;
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
