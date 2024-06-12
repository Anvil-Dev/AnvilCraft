package dev.dubhe.anvilcraft.data.generator.fabric;

import dev.dubhe.anvilcraft.AnvilCraft;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.loader.api.FabricLoader;

import io.github.fabricators_of_create.porting_lib.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class AnvilCraftDatagen implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(@NotNull FabricDataGenerator fabricDataGenerator) {
        Path rootPath =
                FabricLoader.getInstance().getGameDir().normalize().getParent().getParent();
        ExistingFileHelper helper = ExistingFileHelper.withResources(
                rootPath.resolve("common").resolve("src").resolve("main").resolve("resources"),
                rootPath.resolve("fabric").resolve("src").resolve("main").resolve("resources"));
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(ModPoiTagProvider::new);
        AnvilCraft.REGISTRATE.setupDatagen(pack, helper);
    }
}
