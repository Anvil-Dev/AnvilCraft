package dev.dubhe.anvilcraft.data;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class AnvilCraftDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(MyBlockTagGenerator::new);
        pack.addProvider(MyItemTagGenerator::new);
        pack.addProvider(MyRecipesGenerator::new);
    }
}
