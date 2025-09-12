package dev.dubhe.anvilcraft.integration.ponder.scene.recipe;


import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.block.TransmissionPoleBlock;
import dev.dubhe.anvilcraft.block.state.Vertical3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.integration.ponder.AnvilCraftPonderTags;
import dev.dubhe.anvilcraft.integration.ponder.api.AnvilCraftSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.WorldInstructions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class HeaterScene {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<Item> helper = registrationHelper.withKeyFunction(BuiltInRegistries.ITEM::getKey);
        helper.forComponents(ModBlocks.HEATER.asItem())
            .addStoryBoard("platform/5x", HeaterScene::crafting, AnvilCraftPonderTags.PROCESSING_COMPONENTS);
    }

    private static void crafting(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("heater", "Use heater to execute the high-heat recipe");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();

        // Start Create Heater Block
        BlockPos heaterBlockPos = util.grid().at(2, 1, 2);
        builder.world().setBlock(heaterBlockPos, ModBlocks.HEATER.getDefaultState(), true);
        builder.world().showSection(util.select().position(heaterBlockPos), Direction.NORTH);
        builder.idle(20);

        BlockPos transmissionPolePos = util.grid().at(4, 1, 2);
        placeTransmissionPole(builder.world(), util, transmissionPolePos);
        builder.overlay()
            .showText(20)
            .text("It requires 16 kW of power to work in the grid.")
            .pointAt(util.vector().blockSurface(heaterBlockPos, Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(30);

        // Let's Light On
        builder.world().modifyBlock(transmissionPolePos.above(2), state -> state.setValue(HeaterBlock.OVERLOAD, false), false);
        builder.world().modifyBlock(heaterBlockPos, state -> state.setValue(HeaterBlock.OVERLOAD, false), false);
        builder.idle(10);

        builder.overlay()
            .showText(20)
            .text("The heater is now working properly.")
            .pointAt(heaterBlockPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(30);

        BlockPos cauldronBlockPos = heaterBlockPos.above(1);

        builder.world().setBlock(cauldronBlockPos, Blocks.CAULDRON.defaultBlockState(), true);
        builder.world().showIndependentSection(util.select().position(cauldronBlockPos), Direction.NORTH);
        builder.idle(20);

        BlockPos inputItemPos = heaterBlockPos.above(2);
        builder.overlay()
            .showText(20)
            .text("Drop some raw minerals into the cauldron...")
            .pointAt(util.vector().blockSurface(inputItemPos, Direction.DOWN))
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(10);

        ElementLink<EntityElement> inputItemEntityLink = builder.world()
            .createItemEntity(inputItemPos, Items.RAW_IRON.getDefaultInstance());
        builder.overlay().showControls(util.vector().of(2.5, 3.5, 2.5), Pointing.DOWN, 20).withItem(new ItemStack(Items.RAW_IRON));
        builder.idle(30);

        BlockPos anvilBlockPos = heaterBlockPos.above(3);
        builder.overlay()
            .showText(40)
            .text("Use an anvil or the Anvil Hammer to strike the cauldron.")
            .pointAt(util.vector().blockSurface(anvilBlockPos, Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();
        builder.overlay()
            .showControls(cauldronBlockPos.east().getCenter(), Pointing.RIGHT, 25)
            .leftClick()
            .withItem(ModItems.ANVIL_HAMMER.asStack());
        builder.world().setBlock(anvilBlockPos, Blocks.ANVIL.defaultBlockState(), true);
        ElementLink<WorldSectionElement> anvilLink = builder.world()
            .showIndependentSection(util.select().position(anvilBlockPos), Direction.DOWN);
        builder.idle(20);

        builder.world().falldownSection(anvilLink);
        builder.world().replaceItemEntity(cauldronBlockPos, new ItemStack(Items.IRON_INGOT, 2), inputItemEntityLink);
        builder.idle(4);

        builder.world().riseSection(anvilLink);
        builder.overlay()
            .showText(40)
            .text("It will execute the high-heat recipe")
            .pointAt(util.vector().blockSurface(cauldronBlockPos, Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();
        builder.overlay()
            .showControls(cauldronBlockPos.east().getCenter(), Pointing.RIGHT, 30)
            .withItem(new ItemStack(Items.IRON_INGOT, 2));
        builder.idle(30);

        builder.markAsFinished();
    }

    private static void placeTransmissionPole(WorldInstructions world, SceneBuildingUtil util, BlockPos bottomPos) {
        BlockState baseState = ModBlocks.TRANSMISSION_POLE.getDefaultState();
        Vertical3PartHalf[] parts = {
            Vertical3PartHalf.BOTTOM,
            Vertical3PartHalf.MID,
            Vertical3PartHalf.TOP
        };

        for (int i = 0; i < parts.length; i++) {
            BlockPos pos = bottomPos.above(i);
            BlockState state = baseState.trySetValue(TransmissionPoleBlock.HALF, parts[i]);

            if (parts[i] == Vertical3PartHalf.TOP) {
                state = state.trySetValue(TransmissionPoleBlock.OVERLOAD, true);
            }

            world.setBlock(pos, state, false);
            world.showIndependentSection(util.select().position(pos), Direction.NORTH);
        }
    }

}
