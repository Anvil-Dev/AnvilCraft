package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class CelestialForgingAnvilBlockEntity extends BlockEntity {
    @Getter
    private int preRotation = 0;
    @Getter
    private int rotation = 0;

    @Getter
    @Setter
    private boolean isAmplify = false;

    // === Phase 5 stubs — populated in Phase 6 ===
    @Getter
    private final CfaMegastructureManager megastructureManager = new CfaMegastructureManager();

    public CelestialForgingAnvilBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public void tick() {
        if (this.rotation == 360) this.rotation = 0;
        this.preRotation = this.rotation;
        this.rotation += 3;
    }

    // === NBT ===

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("amplified", this.isAmplify);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.isAmplify = input.getBooleanOr("amplified", false);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("amplified", this.isAmplify);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // === Phase 5+6 stubs ===

    public void syncLogisticsOnChange(BlockPos interfacePos, int changedSlot) {
        // Phase 6
    }

    @Nullable
    public CelestialBodyData getCelestialBodyData() { return null; }

    @Nullable
    public PlanetaryResourceSet getPlanetaryResourceSet() { return null; }

    public List<CelestialRefactorOption> getClientVisibleOptions() { return List.of(); }

    @Nullable
    public CelestialRefactorOption getActiveMegastructureOption() { return null; }

    public int getActiveMegastructureIndex() { return -1; }

    public boolean isAcceleratorActive() { return false; }

    public int getAcceleratorStage() { return 0; }

    public boolean isAmplifierPresent() { return false; }

    public boolean isPowerInsufficient() { return false; }

    public int getStellarMass() { return 0; }

    public int getAgeAnvilCount() { return 0; }

    public void setAgeAnvilCount(int count) { /* Phase 6 */ }

    public void setStellarMass(int mass) { /* Phase 6 */ }

    public void setCelestialBodyData(CelestialBodyData data) { /* Phase 6 */ }

    public void setPlanetaryResourceSet(@Nullable PlanetaryResourceSet set) { /* Phase 6 */ }

    @Nullable
    public PowerGrid getGrid() { return null; }

    public PowerComponentType getComponentType() { return PowerComponentType.CONSUMER; }

    public Map<BlockPos, CelestialForgingAnvilLaserInterfaceBlockEntity> getLaserInterfacesMap() { return Map.of(); }

    public Map<BlockPos, CelestialForgingAnvilLogisticsInterfaceBlockEntity> getLogisticsInterfacesMap() { return Map.of(); }
}
