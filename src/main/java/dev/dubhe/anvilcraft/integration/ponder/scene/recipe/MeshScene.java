package dev.dubhe.anvilcraft.integration.ponder.scene.recipe;

import dev.dubhe.anvilcraft.integration.ponder.AnvilCraftPonderTags;
import dev.dubhe.anvilcraft.integration.ponder.api.AnvilCraftSceneBuilder;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class MeshScene {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<Item> helper = registrationHelper.withKeyFunction(BuiltInRegistries.ITEM::getKey);
        helper.forComponents(Items.SCAFFOLDING)
            .addStoryBoard("platform/5x", MeshScene::crafting, AnvilCraftPonderTags.PROCESSING_COMPONENTS);
    }

    private static void crafting(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("mesh", "Screen Items");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();

        BlockPos tablePos = util.grid().at(2, 1, 2);
        builder.world().setBlock(tablePos, Blocks.SCAFFOLDING.defaultBlockState(), false);
        builder.world().showSection(util.select().position(tablePos), Direction.NORTH);

        BlockPos anvilPos = tablePos.above(2);
        builder.world().setBlock(anvilPos, Blocks.ANVIL.defaultBlockState(), false);
        ElementLink<WorldSectionElement> anvilLink = builder.world()
            .showIndependentSection(util.select().position(anvilPos), Direction.DOWN);
        builder.idle(20);

        // 物品过筛
        ElementLink<EntityElement> itemEntity = builder.world().createItemEntity(tablePos.above(), new ItemStack(Items.SAND, 64));
        builder.world().falldownSection(anvilLink);
        builder.world().replaceItemEntity(tablePos.getCenter(), Items.SAND.getDefaultInstance(), itemEntity);
        builder.world().createItemEntity(tablePos.getCenter(), Vec3.ZERO, Items.CLAY_BALL.getDefaultInstance());
        builder.world().createItemEntity(tablePos.getCenter(), Vec3.ZERO, Items.GOLD_NUGGET.getDefaultInstance());
        builder.world().riseSection(anvilLink);
        builder.idle(10);

        builder.overlay()
            .showText(20)
            .text("Scaffolding can screen items.")
            .pointAt(tablePos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(30);

        builder.markAsFinished();
    }
}
