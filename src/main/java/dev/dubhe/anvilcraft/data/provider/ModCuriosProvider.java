package dev.dubhe.anvilcraft.data.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import top.theillusivec4.curios.api.CuriosDataProvider;

import java.util.concurrent.CompletableFuture;

public class ModCuriosProvider extends CuriosDataProvider {
    public ModCuriosProvider(PackOutput output, ExistingFileHelper fileHelper, CompletableFuture<HolderLookup.Provider> registries) {
        super(AnvilCraft.MOD_ID, output, fileHelper, registries);
    }

    @Override
    public void generate(HolderLookup.Provider registries, ExistingFileHelper fileHelper) {
        createSlot("ionocraft_backpack")
            .addCosmetic(true)
            .icon(AnvilCraft.of("slot/empty_ionocraft_backpack_slot"));

        createEntities("ionocraft_backpack")
            .addPlayer()
            .addSlots("ionocraft_backpack");
    }
}
