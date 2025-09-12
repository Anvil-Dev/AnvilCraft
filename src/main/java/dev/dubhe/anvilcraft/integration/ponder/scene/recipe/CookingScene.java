package dev.dubhe.anvilcraft.integration.ponder.scene.recipe;

import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.integration.ponder.AnvilCraftPonderTags;
import dev.dubhe.anvilcraft.integration.ponder.api.AnvilCraftSceneBuilder;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class CookingScene {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<Item> helper = registrationHelper.withKeyFunction(BuiltInRegistries.ITEM::getKey);
        helper.forComponents(Items.CAULDRON, Items.CAMPFIRE)
            .addStoryBoard("platform/5x", CookingScene::crafting, AnvilCraftPonderTags.PROCESSING_COMPONENTS);
    }

    private static void crafting(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("cooking", "Cooking");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();

        BlockPos anvilPos = util.grid().at(2, 4, 2);
        builder.world().setBlock(anvilPos, Blocks.ANVIL.defaultBlockState(), false);
        ElementLink<WorldSectionElement> anvilLink = builder.world()
            .showIndependentSection(util.select().position(anvilPos), Direction.DOWN);

        BlockPos cauldronPos = util.grid().at(2, 2, 2);
        builder.world().setBlock(cauldronPos, Blocks.CAULDRON.defaultBlockState(), false);
        builder.world().showSection(util.select().position(cauldronPos), Direction.NORTH);

        BlockPos campfirePos = util.grid().at(2, 1, 2);
        builder.world().setBlock(campfirePos, Blocks.CAMPFIRE.defaultBlockState(), false);
        builder.world().showSection(util.select().position(campfirePos), Direction.NORTH);
        builder.idle(20);

        // 开始烹饪
        ElementLink<EntityElement> itemLink;
        itemLink = builder.world().createItemEntity(cauldronPos.above().getCenter(), Vec3.ZERO, ModItems.RESIN.asStack());
        builder.idle(10);

        builder.world().falldownSection(anvilLink);
        builder.world().removeEntity(itemLink);
        itemLink = builder.world().createItemEntity(cauldronPos.getCenter(), Vec3.ZERO, ModItems.HARDEND_RESIN.asStack());
        builder.world().riseSection(anvilLink);
        builder.idle(10);

        builder.overlay()
            .showText(40)
            .text("Using the cauldron with the campfire to process items.")
            .pointAt(cauldronPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        builder.world().removeEntity(itemLink);
        builder.world().hideSection(util.select().position(cauldronPos), Direction.NORTH);
        builder.idle(20);

        // 有水烹饪
        builder.world().setBlock(cauldronPos, CauldronUtil.fullState(Blocks.WATER_CAULDRON), false);
        builder.world().showSection(util.select().position(cauldronPos), Direction.NORTH);
        builder.idle(10);

        itemLink = builder.world().createItemEntity(cauldronPos.above().getCenter(), Vec3.ZERO, ModItems.RESIN.asStack());
        builder.idle(10);

        builder.world().falldownSection(anvilLink);
        builder.world().removeEntity(itemLink);
        builder.world().createItemEntity(cauldronPos.getCenter(), Vec3.ZERO, Items.SLIME_BALL.getDefaultInstance());
        builder.world().riseSection(anvilLink);
        builder.idle(10);

        builder.world().hideSection(util.select().position(cauldronPos), Direction.NORTH);

        builder.overlay()
            .showText(60)
            .text("If there is water in the cauldron, another recipe can be performed.")
            .pointAt(cauldronPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(70);

        builder.markAsFinished();
    }
}
