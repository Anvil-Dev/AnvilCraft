package dev.dubhe.anvilcraft.integration.ponder.scene;

import dev.dubhe.anvilcraft.integration.ponder.AnvilCraftPonderTags;
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
import org.jetbrains.annotations.NotNull;

public class AnvilScene {
    public static void register(@NotNull PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<Item> helper = registrationHelper.withKeyFunction(BuiltInRegistries.ITEM::getKey);
        helper.forComponents(
                Items.ANVIL,
                Items.CHIPPED_ANVIL,
                Items.DAMAGED_ANVIL
            )
            .addStoryBoard(
                "platform/555",
                AnvilScene::crafting,
                AnvilCraftPonderTags.ANVIL
            );
    }

    private static void crafting(@NotNull SceneBuilder scene, @NotNull SceneBuildingUtil util) {
        scene.title("anvil", "Use anvil to craft");
        scene.configureBasePlate(0, 0, 5);

        Selection basePlate = util.select().fromTo(0, 0, 0, 5, 0, 5);
        scene.world().showSection(basePlate, Direction.UP);
        scene.idle(20);

        scene.world().setBlock(new BlockPos(2, 1, 2), Blocks.ANVIL.defaultBlockState(), false);
        Selection anvil = util.select().fromTo(2, 1, 2, 2, 1, 2);
        ElementLink<WorldSectionElement> anvilLink = scene.world().showIndependentSection(anvil, Direction.NORTH);

        scene.idle(40);

        scene.overlay().showText(30)
            .text("The anvil is the foundation for all processing in the AnvilCraft")
            .pointAt(util.vector().blockSurface(util.grid().at(2, 1, 2), Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();

        scene.idle(40);

        scene.world().moveSection(anvilLink, new Vec3(0, 1, 0), 7);
        scene.idle(10);
        scene.world().moveSection(anvilLink, new Vec3(0, -1, 0), 5);
        scene.idle(10);

        scene.overlay().showText(30)
            .text("The anvil falls from a height to complete one anvil process.")
            .pointAt(util.vector().blockSurface(util.grid().at(2, 1, 2), Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();

        scene.idle(40);

        scene.markAsFinished();
    }
}
