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
        this.createSlot("ionocraft_backpack")
            .addCosmetic(true)
            .icon(AnvilCraft.of("item/empty_slot_ionocraft_backpack"));

        this.createEntities("ionocraft_backpack")
            .addPlayer()
            .addSlots("ionocraft_backpack");

        this.createEntities("goggles")
            .addPlayer()
            .addSlots("head");

        this.createEntities("charms")
            .addPlayer()
            .addSlots("charm");
    }
}
