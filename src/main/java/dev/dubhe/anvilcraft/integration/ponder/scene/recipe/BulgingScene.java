package dev.dubhe.anvilcraft.integration.ponder.scene.recipe;

import dev.dubhe.anvilcraft.integration.ponder.AnvilCraftPonderTags;
import dev.dubhe.anvilcraft.integration.ponder.api.AnvilCraftSceneBuilder;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import net.createmod.catnip.math.Pointing;
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

public class BulgingScene {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<Item> helper = registrationHelper.withKeyFunction(BuiltInRegistries.ITEM::getKey);
        helper.forComponents(Items.CAULDRON)
            .addStoryBoard("platform/5x", BulgingScene::crafting, AnvilCraftPonderTags.PROCESSING_COMPONENTS);
    }

    private static void crafting(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("bulging", "Bulge Items");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();

        BlockPos anvilPos = util.grid().at(2, 3, 2);
        builder.world().setBlock(anvilPos, Blocks.ANVIL.defaultBlockState(), false);
        final ElementLink<WorldSectionElement> anvilLink =
            builder.world().showIndependentSection(util.select().position(anvilPos), Direction.DOWN);

        BlockPos cauldronPos = util.grid().at(2, 1, 2);
        builder.world().setBlock(cauldronPos, CauldronUtil.fullState(Blocks.WATER_CAULDRON), false);
        builder.world().showSection(util.select().position(cauldronPos), Direction.NORTH);
        builder.idle(20);

        ItemStack[] inputs = new ItemStack[] {
            Items.COPPER_BLOCK.getDefaultInstance(),
            Items.FIRE_CORAL.getDefaultInstance(),
            Items.ORANGE_CONCRETE_POWDER.getDefaultInstance(),
        };
        ItemStack[] outputs = new ItemStack[] {
            Items.EXPOSED_COPPER.getDefaultInstance(),
            Items.FIRE_CORAL_BLOCK.getDefaultInstance(),
            Items.ORANGE_CONCRETE.getDefaultInstance(),
        };
        ElementLink<EntityElement> itemEntity;
        for (int i = 0; i < inputs.length; i++) {
            itemEntity = builder.world().createItemEntity(cauldronPos.above(), inputs[i]);
            builder.idle(10);
            builder.world().falldownSection(anvilLink);
            itemEntity = builder.world().replaceItemEntity(cauldronPos, outputs[i], itemEntity);
            builder.world().riseSection(anvilLink);
            builder.overlay().showControls(cauldronPos.east().getCenter(), Pointing.RIGHT, 10)
                .withItem(outputs[i]);
            builder.idle(20);

            builder.world().removeEntity(itemEntity);
        }
        builder.idle(10);

        builder.overlay().showText(60)
            .text("When the cauldron is full of water, it will bulge the items inside")
            .pointAt(util.vector().blockSurface(util.grid().at(2, 1, 2), Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(70);

        builder.markAsFinished();
    }
}
