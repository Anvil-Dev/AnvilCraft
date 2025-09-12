package dev.dubhe.anvilcraft.integration.ponder.scene;

import dev.dubhe.anvilcraft.integration.ponder.AnvilCraftPonderTags;
import dev.dubhe.anvilcraft.integration.ponder.api.AnvilCraftSceneBuilder;
import dev.dubhe.anvilcraft.integration.ponder.api.instruction.Interpolation;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class AnvilScene {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<Item> helper = registrationHelper.withKeyFunction(BuiltInRegistries.ITEM::getKey);
        helper.forComponents(
                Items.ANVIL,
                Items.CHIPPED_ANVIL,
                Items.DAMAGED_ANVIL
            )
            .addStoryBoard("platform/5x", AnvilScene::crafting, AnvilCraftPonderTags.ANVIL);
    }

    private static void crafting(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("anvil", "Use anvil to craft");
        builder.configureBasePlate(0, 0, 5);

        Selection basePlate = util.select().fromTo(0, 0, 0, 5, 0, 5);
        builder.world().showSection(basePlate, Direction.UP);
        builder.idle(20);

        builder.world().setBlock(new BlockPos(2, 1, 2), Blocks.ANVIL.defaultBlockState(), false);
        Selection anvil = util.select().fromTo(2, 1, 2, 2, 1, 2);
        ElementLink<WorldSectionElement> anvilLink = scene.world().showIndependentSection(anvil, Direction.NORTH);

        builder.idle(40);

        builder.overlay()
            .showText(30)
            .text("The anvil is the foundation for all processing in the AnvilCraft")
            .pointAt(util.vector().blockSurface(util.grid().at(2, 1, 2), Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();

        builder.idle(40);

        builder.world().moveSectionInterpolation(anvilLink, new Vec3(0, 2, 0), Interpolation.acceleration(0.025));
        builder.world().moveSectionInterpolation(anvilLink, new Vec3(0, -2, 0), Interpolation.acceleration(0.025));

        builder.overlay()
            .showText(30)
            .text("The anvil falls from a height to complete one anvil process.")
            .pointAt(util.vector().blockSurface(util.grid().at(2, 1, 2), Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();

        builder.idle(40);

        builder.markAsFinished();
    }
}
