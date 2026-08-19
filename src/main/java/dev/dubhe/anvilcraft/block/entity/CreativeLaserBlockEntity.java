package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.api.rendering.CacheableBERenderingPipeline;
import dev.dubhe.anvilcraft.block.CreativeLaserBlock;
import dev.dubhe.anvilcraft.block.state.LensType;
import dev.dubhe.anvilcraft.init.ModHeaterInfos;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.CreativeLaserMenu;
import dev.dubhe.anvilcraft.network.LaserEmitPacket;
import dev.dubhe.anvilcraft.util.BlockMiningEffect;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@Getter
public class CreativeLaserBlockEntity extends BaseLaserBlockEntity implements MenuProvider {

    private int configuredLevel = 16;
    private LensType lensType = LensType.NONE;
    private boolean gamma = false;

    public static CreativeLaserBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new CreativeLaserBlockEntity(type, pos, blockState);
    }

    public CreativeLaserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public void setConfiguredLevel(int level) {
        this.configuredLevel = Math.clamp(level, 0, 64);
        this.setChanged();
    }

    public void setLensType(LensType lensType) {
        this.lensType = lensType;
        this.setChanged();
    }

    public void setGamma(boolean gamma) {
        this.gamma = gamma;
        this.setChanged();
    }

    @Override
    public boolean isEmittingGamma() {
        return this.gamma;
    }

    @Override
    protected int getBaseLaserLevel() {
        return this.configuredLevel;
    }

    @Override
    public BlockMiningEffect getMiningEffect() {
        return this.lensType.getMiningEffect();
    }

    @Override
    public void onIrradiated(BaseLaserBlockEntity baseLaserBlockEntity) {
    }

    @Override
    public Direction getFacing() {
        return this.getBlockState().getValue(CreativeLaserBlock.FACING);
    }

    @Override
    public float getLaserOffset() {
        return -0.5f;
    }

    @Override
    public void tick(Level level) {
        this.resetState();
        if (level.isClientSide()) {
            super.tick(level);
            return;
        }
        if (this.isRedstoneOff() || this.configuredLevel <= 0) {
            this.cancelLaserEmission();
        } else if (this.gamma) {
            this.emitGammaLaserBeam(getFacing());
        } else {
            this.emitLaser(getFacing());
        }
        if (this.changed && level instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersTrackingChunk(
                serverLevel,
                level.getChunkAt(getBlockPos()).getPos(),
                new LaserEmitPacket(getLaserLevel(), getBlockPos(), this.irradiateBlockPos, this.gamma)
            );
        }
        this.tickCount++;
        if (level instanceof ServerLevel serverLevel
            && this.irradiateBlockPos != null
            && serverLevel.getBlockState(this.irradiateBlockPos).is(ModBlockTags.HEATABLE_BLOCKS)
        ) {
            HeaterManager.addProducer(this.getBlockPos(), serverLevel, ModHeaterInfos.LASER_EMITTER);
        }
    }

    private boolean isRedstoneOff() {
        return this.level != null && this.level.hasNeighborSignal(this.getBlockPos());
    }

    private void cancelLaserEmission() {
        if (this.irradiateBlockPos != null
            && this.level != null
            && this.level.getBlockEntity(this.irradiateBlockPos) instanceof BaseLaserBlockEntity target
        ) {
            target.onCancelingIrradiation(this);
        }
        this.updateIrradiateBlockPos(null);
        this.clearIrradiateSelfLaserBlockSet();
        this.updateLaserLevel(0);
        this.gammaIrradiatingPos = null;
        this.gammaExposureTicks = 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("laserLevel", this.configuredLevel);
        tag.putString("lensType", this.lensType.getSerializedName());
        tag.putBoolean("gamma", this.gamma);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.configuredLevel = tag.getInt("laserLevel");
        this.lensType = Arrays.stream(LensType.values())
            .filter(type -> type.getSerializedName().equals(tag.getString("lensType")))
            .findFirst()
            .orElse(LensType.NONE);
        this.gamma = tag.getBoolean("gamma");
        if (this.level != null && this.level.isClientSide()) {
            CacheableBERenderingPipeline.getInstance().update(this);
        }
    }

    @Override
    public Component getDisplayName() {
        return ModBlocks.CREATIVE_LASER.get().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CreativeLaserMenu(containerId, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.getBlockPos());
    }

    @Override
    public void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(
            player,
            new LaserEmitPacket(getLaserLevel(), getBlockPos(), this.irradiateBlockPos, this.gamma)
        );
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("laserLevel", this.configuredLevel);
        tag.putString("lensType", this.lensType.getSerializedName());
        tag.putBoolean("gamma", this.gamma);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        this.configuredLevel = tag.getInt("laserLevel");
        this.lensType = Arrays.stream(LensType.values())
            .filter(type -> type.getSerializedName().equals(tag.getString("lensType")))
            .findFirst()
            .orElse(LensType.NONE);
        this.gamma = tag.getBoolean("gamma");
        if (this.level != null && this.level.isClientSide()) {
            CacheableBERenderingPipeline.getInstance().update(this);
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && level instanceof ServerLevel serverLevel) {
            Packet<?> packet = getUpdatePacket();
            if (packet != null) {
                for (ServerPlayer player : serverLevel.getChunkSource().chunkMap
                    .getPlayers(serverLevel.getChunkAt(worldPosition).getPos(), false)) {
                    player.connection.send(packet);
                }
            }
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void clientUpdate(@Nullable BlockPos irradiateBlockPos, int laserLevel) {
        this.gamma = false;
        super.clientUpdate(irradiateBlockPos, laserLevel);
    }

    public void clientUpdateGamma(@Nullable BlockPos irradiateBlockPos, int laserLevel) {
        this.gamma = true;
        this.irradiateBlockPos = irradiateBlockPos;
        this.laserLevel = laserLevel;
        CacheableBERenderingPipeline.getInstance().update(this);
    }
}
