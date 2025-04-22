package dev.dubhe.anvilcraft.util;

import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import dev.dubhe.anvilcraft.block.plate.PowerLevelPressurePlateBlock;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.DiodeBlock;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class DataGenUtil {
    public static void powerLevelPressurePlate(
        RegistrateBlockstateProvider provider, ResourceLocation id,
        PowerLevelPressurePlateBlock block, ResourceLocation texture
    ) {
        ModelFile pressurePlate = provider.models().pressurePlate(id.getPath(), texture);
        ModelFile pressurePlateDown = provider.models().pressurePlateDown(id.getPath() + "_down", texture);

        provider.getVariantBuilder(block)
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 0).addModels(new ConfiguredModel(pressurePlate))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 1).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 2).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 3).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 4).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 5).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 6).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 7).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 8).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 9).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 10).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 11).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 12).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 13).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 14).addModels(new ConfiguredModel(pressurePlateDown))
            .partialState().with(PowerLevelPressurePlateBlock.POWER, 15).addModels(new ConfiguredModel(pressurePlateDown));
    }

    public static void diodeBlock(RegistrateBlockstateProvider provider, ResourceLocation id, DiodeBlock block) {
        ModelFile diode = new ModelFile.ExistingModelFile(id.withPrefix("block/"), provider.models().existingFileHelper);
        ModelFile diodeOn = new ModelFile.ExistingModelFile(id.withPrefix("block/").withSuffix("_on"), provider.models().existingFileHelper);

        provider.getVariantBuilder(block)
            .partialState().with(DiodeBlock.FACING, Direction.SOUTH).with(DiodeBlock.POWERED, false).addModels(
                new ConfiguredModel(diode))
            .partialState().with(DiodeBlock.FACING, Direction.WEST).with(DiodeBlock.POWERED, false).addModels(
                new ConfiguredModel(diode, 0, 90, false))
            .partialState().with(DiodeBlock.FACING, Direction.NORTH).with(DiodeBlock.POWERED, false).addModels(
                new ConfiguredModel(diode, 0, 180, false))
            .partialState().with(DiodeBlock.FACING, Direction.EAST).with(DiodeBlock.POWERED, false).addModels(
                new ConfiguredModel(diode, 0, 270, false))
            .partialState().with(DiodeBlock.FACING, Direction.SOUTH).with(DiodeBlock.POWERED, true).addModels(
                new ConfiguredModel(diodeOn))
            .partialState().with(DiodeBlock.FACING, Direction.WEST).with(DiodeBlock.POWERED, true).addModels(
                new ConfiguredModel(diodeOn, 0, 90, false))
            .partialState().with(DiodeBlock.FACING, Direction.NORTH).with(DiodeBlock.POWERED, true).addModels(
                new ConfiguredModel(diodeOn, 0, 180, false))
            .partialState().with(DiodeBlock.FACING, Direction.EAST).with(DiodeBlock.POWERED, true).addModels(
                new ConfiguredModel(diodeOn, 0, 270, false));
    }
}
