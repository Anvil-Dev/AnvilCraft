package dev.dubhe.anvilcraft.integration.curios.data;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import top.theillusivec4.curios.api.CuriosDataProvider;

import java.util.concurrent.CompletableFuture;

public class ModCuriosProvider extends CuriosDataProvider {
    public ModCuriosProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(AnvilCraft.MOD_ID, output, registries);
    }

    @Override
    public void generate(HolderLookup.Provider registries) {
        this.createEntities("goggles")
            .addPlayer()
            .addSlots("head");

        this.createEntities("charms")
            .addPlayer()
            .addSlots("charm");
    }
}
