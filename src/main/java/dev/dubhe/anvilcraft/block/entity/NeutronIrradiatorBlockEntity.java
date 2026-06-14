package dev.dubhe.anvilcraft.block.entity;

import com.google.common.collect.ImmutableMap;
import dev.dubhe.anvilcraft.block.NeutronIrradiatorBlock;
import dev.dubhe.anvilcraft.block.state.IrradiatorType;
import dev.dubhe.anvilcraft.init.ModParticles;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class NeutronIrradiatorBlockEntity extends BlockEntity {

    public static final ImmutableMap<Block, IrradiatorType> IRRADIATOR_TYPE_MAP = ImmutableMap.of(
        ModBlocks.CONFINED_ENERGY_ANVILON.get(), IrradiatorType.ENERGY,
        ModBlocks.CONFINED_MASS_ANVILON.get(), IrradiatorType.MASS,
        ModBlocks.CONFINED_SPACE_ANVILON.get(), IrradiatorType.SPACE,
        ModBlocks.CONFINED_TIME_ANVILON.get(), IrradiatorType.TIME
    );

    public static final ImmutableMap<Block, Supplier<SimpleParticleType>> PARTICLE_TYPE_MAP = ImmutableMap.of(
        ModBlocks.CONFINED_ENERGY_ANVILON.get(), ModParticles.ANVILON_ENERGY,
        ModBlocks.CONFINED_MASS_ANVILON.get(), ModParticles.ANVILON_MASS,
        ModBlocks.CONFINED_SPACE_ANVILON.get(), ModParticles.ANVILON_SPACE,
        ModBlocks.CONFINED_TIME_ANVILON.get(), ModParticles.ANVILON_TIME
    );

    public static final int TYPE_CHECK_THRESHOLD = 6;

    public NeutronIrradiatorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.NEUTRON_IRRADIATOR.get(), pos, blockState);
    }

    public static NeutronIrradiatorBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new NeutronIrradiatorBlockEntity(pos, blockState);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (this.level == null) return;
        boolean baseType = true;
        for (var entry : IRRADIATOR_TYPE_MAP.entrySet()) {
            Block block = entry.getKey();
            IrradiatorType type = entry.getValue();
            int count = 0;
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (this.level.getBlockState(pos.offset(i, 0, j)).is(block)) {
                        count++;
                    }
                }
            }
            if (count >= TYPE_CHECK_THRESHOLD) {
                baseType = false;
                if (state.getValue(NeutronIrradiatorBlock.TYPE) != type) {
                    this.level.setBlockAndUpdate(pos, state.setValue(NeutronIrradiatorBlock.TYPE, type));
                }
                break;
            }
        }
        if (baseType && state.getValue(NeutronIrradiatorBlock.TYPE) != IrradiatorType.NEUTRON) {
            this.level.setBlockAndUpdate(pos, state.setValue(NeutronIrradiatorBlock.TYPE, IrradiatorType.NEUTRON));
        }
    }
}