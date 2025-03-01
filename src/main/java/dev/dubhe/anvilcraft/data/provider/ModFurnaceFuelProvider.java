package dev.dubhe.anvilcraft.data.provider;

import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ModFurnaceFuelProvider extends DataMapProvider {
    public ModFurnaceFuelProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        builder(NeoForgeDataMaps.FURNACE_FUELS).add(ModItems.WOOD_FIBER, fromItemCount(1.5), false);
    }

    private static FurnaceFuel fromItemCount(double count){
        return new FurnaceFuel((int) (count * FurnaceBlockEntity.BURN_TIME_STANDARD));
    }
}
