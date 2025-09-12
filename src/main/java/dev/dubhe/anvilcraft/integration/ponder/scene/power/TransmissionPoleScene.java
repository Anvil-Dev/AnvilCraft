package dev.dubhe.anvilcraft.integration.ponder.scene.power;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.dubhe.anvilcraft.block.TransmissionPoleBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.ponder.AnvilCraftPonderTags;
import dev.dubhe.anvilcraft.integration.ponder.api.AnvilCraftSceneBuilder;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public class TransmissionPoleScene {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> helper = registrationHelper.withKeyFunction(RegistryEntry::getId);
        helper.forComponents(ModBlocks.TRANSMISSION_POLE)
            .addStoryBoard("platform/5x", TransmissionPoleScene::run, AnvilCraftPonderTags.REDSTONE_COMPONENTS);
    }

    public static void run(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);

        builder.title("transmission_pole", "Use Transmission Pole to transmitting electricity");
        builder.configureBasePlate(0, 0, 5);

        Selection basePlate = util.select().fromTo(0, 0, 0, 5, 0, 5);
        builder.world().showSection(basePlate, Direction.UP);
        builder.idle(20);

        Selection pole = builder.world()
            .setMultiPartBlock(
                new BlockPos(4, 1, 0),
                ModBlocks.TRANSMISSION_POLE.getDefaultState().setValue(TransmissionPoleBlock.OVERLOAD, false),
                false
            );
        Selection pole1 = builder.world()
            .setMultiPartBlock(
                new BlockPos(0, 1, 4),
                ModBlocks.TRANSMISSION_POLE.getDefaultState().setValue(TransmissionPoleBlock.OVERLOAD, false),
                false
            );
        builder.world().showIndependentSection(pole, Direction.NORTH);
        builder.world().showIndependentSection(pole1, Direction.NORTH);
        builder.overlay().showTransmitterLine(new BlockPos(4, 3, 0), new BlockPos(0, 3, 4), 600);
        builder.idle(600);
    }
}
