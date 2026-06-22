package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLaserInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@Getter
public class PenroseSphereHandler extends BaseMegastructureHandler {

    private boolean laserActive = false;

    @Override
    public String name() {
        return "penrose_sphere";
    }

    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        CelestialRefactorOption option = be.getActiveMegastructureOption();
        if (option == null || !name().equals(option.megastructure())) return;
        if (!be.isAmplifierPresent()) {
            if (laserActive) {
                laserActive = false;
                be.setChanged();
                be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
            }
            return;
        }

        int cx = be.getBlockPos().getX();
        int cy = be.getBlockPos().getY();
        int cz = be.getBlockPos().getZ();

        boolean anyLaserInput = false;

        anyLaserInput |= processPenroseLaserPair(be,
            new BlockPos(cx - 1, cy, cz - 2),
            new BlockPos(cx + 1, cy, cz - 2)
        );
        anyLaserInput |= processPenroseLaserPair(be,
            new BlockPos(cx - 1, cy, cz + 2),
            new BlockPos(cx + 1, cy, cz + 2)
        );
        anyLaserInput |= processPenroseLaserPair(be,
            new BlockPos(cx - 2, cy, cz - 1),
            new BlockPos(cx - 2, cy, cz + 1)
        );
        anyLaserInput |= processPenroseLaserPair(be,
            new BlockPos(cx + 2, cy, cz - 1),
            new BlockPos(cx + 2, cy, cz + 1)
        );

        if (laserActive != anyLaserInput) {
            laserActive = anyLaserInput;
            be.setChanged();
            be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        }
    }

    private boolean processPenroseLaserPair(CelestialForgingAnvilBlockEntity be, BlockPos posA, BlockPos posB) {
        if (be.getLevel() == null) return false;
        BlockEntity beA = be.getLevel().getBlockEntity(posA);
        BlockEntity beB = be.getLevel().getBlockEntity(posB);

        boolean hasInput = false;

        if (beA instanceof CelestialForgingAnvilLaserInterfaceBlockEntity laserA
            && beB instanceof CelestialForgingAnvilLaserInterfaceBlockEntity laserB) {

            if (laserA.getReceivedLaserLevel() > 0) {
                hasInput = true;
                if (isLaserInterfaceActive(beB)) {
                    laserB.emitGammaLaser(laserA.getReceivedLaserLevel());
                }
            }

            if (laserB.getReceivedLaserLevel() > 0) {
                hasInput = true;
                if (isLaserInterfaceActive(beA)) {
                    laserA.emitGammaLaser(laserB.getReceivedLaserLevel());
                }
            }
        } else {
            if (beA instanceof CelestialForgingAnvilLaserInterfaceBlockEntity laserA
                && laserA.getReceivedLaserLevel() > 0) {
                hasInput = true;
            }
            if (beB instanceof CelestialForgingAnvilLaserInterfaceBlockEntity laserB
                && laserB.getReceivedLaserLevel() > 0) {
                hasInput = true;
            }
        }

        return hasInput;
    }

    private boolean isLaserInterfaceActive(BlockEntity be) {
        if (be instanceof CelestialForgingAnvilLaserInterfaceBlockEntity laserBe) {
            BlockState state = laserBe.getBlockState();
            if (state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)) {
                return state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE);
            }
        }
        return false;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("penroseSphereLaserActive", laserActive);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        this.laserActive = tag.getBoolean("penroseSphereLaserActive");
    }

    @Override
    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("penroseSphereLaserActive", laserActive);
    }

    @Override
    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.laserActive = tag.getBoolean("penroseSphereLaserActive");
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        this.laserActive = false;
    }
}
