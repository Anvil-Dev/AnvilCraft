package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.block.IDamagingHeater;
import dev.dubhe.anvilcraft.block.BurningHeaterBlock;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.block.NeutronIrradiatorBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

public class HeaterUtil {
    public static boolean isActiveHeatSource(BlockState state) {
        if (CampfireBlock.isLitCampfire(state) && state.is(Blocks.CAMPFIRE)) return true;
        if (state.is(ModBlocks.HEATER)) return ((HeaterBlock) state.getBlock()).isActive(state);
        if (state.is(ModBlocks.BURNING_HEATER)) return state.getValue(BurningHeaterBlock.LEVEL) > 0;
        if (state.is(ModBlocks.CORRUPTED_BEACON)) return state.getValue(CorruptedBeaconBlock.LIT);
        return state.getBlock() instanceof NeutronIrradiatorBlock;
    }

    public static boolean hurtEntity(Level level, BlockState state, Entity entity) {
        if (!(state.getBlock() instanceof IDamagingHeater heater)
            || !heater.isActive(state)
            || entity.isSteppingCarefully()
            || !(entity instanceof LivingEntity)) {
            return false;
        }
        entity.hurt(ModDamageTypes.heaterBurn(level), AnvilCraft.CONFIG.world.heaterDamage);
        return true;
    }
}
