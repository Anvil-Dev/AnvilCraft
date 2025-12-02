package dev.dubhe.anvilcraft.integration.ponder.scene.structure;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.ponder.api.AnvilCraftSceneBuilder;
import net.createmod.catnip.math.Pointing;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class ImpactPileScene {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<Item> helper = registrationHelper.withKeyFunction(
            BuiltInRegistries.ITEM::getKey
        );
        helper.forComponents(
                ModBlocks.IMPACT_PILE.asItem(),
                ModBlocks.MINERAL_FOUNTAIN.asItem()
            )
            .addStoryBoard("platform/5x", ImpactPileScene::impact);
    }

    private static void impact(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("impact_pile", "Use Impact Pile to generate Mineral Fountain");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();
        builder.idle(10);
        BlockPos impactTargetPos = new BlockPos(2, 1, 2);
        BlockPos impactPilePos = new BlockPos(2, 2, 2);
        builder.world().setBlock(impactTargetPos, Blocks.BEDROCK.defaultBlockState(), true);
        builder.world().showIndependentSection(util.select().position(impactTargetPos), Direction.UP);
        builder.world().setBlock(impactPilePos, ModBlocks.IMPACT_PILE.getDefaultState(), true);
        builder.world().showIndependentSection(util.select().position(impactPilePos), Direction.DOWN);
        builder.overlay()
            .showText(20)
            .text("Impact piles are consumables used to create a mineral fountain")
            .pointAt(util.vector().blockSurface(impactPilePos, Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(30);
        builder.overlay()
            .showText(20)
            .text("To work properly, it must be placed no higher than 8 blocks above the world’s bottom")
            .pointAt(util.vector().blockSurface(impactPilePos, Direction.DOWN))
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(30);
        builder.overlay()
            .showText(20)
            .text("And it must be placed on bedrock or deepslate")
            .pointAt(util.vector().blockSurface(impactTargetPos, Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();
        builder.overlay().showControls(util.vector().of(2.75, 2.25, 1.5), Pointing.RIGHT, 20).withItem(new ItemStack(Items.DEEPSLATE));
        builder.idle(30);
        builder.overlay()
            .showText(20)
            .text("Then strike it with an undamaged anvil dropped from at least 20 blocks high")
            .pointAt(util.vector().blockSurface(impactPilePos, Direction.UP))
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(30);
        BlockPos anvilBlockPos = impactPilePos.above(2);
        builder.world().setBlock(anvilBlockPos, Blocks.ANVIL.defaultBlockState(), true);
        Selection anvilSelection = util.select().position(anvilBlockPos);
        ElementLink<WorldSectionElement> anvilLink = builder.world()
            .showIndependentSection(anvilSelection, Direction.UP);
        builder.world().moveSection(anvilLink, new Vec3(0, -1, 0), 2);
        builder.idle(4);
        builder.world().destroyBlock(impactPilePos);
        builder.world().destroyBlock(anvilBlockPos);
        Selection baseLavaSelection = util.select().fromTo(1, 2, 1, 3, 2, 3);
        builder.world().setBlocks(baseLavaSelection, Blocks.LAVA.defaultBlockState(), true);
        builder.world().showIndependentSection(baseLavaSelection, Direction.UP);
        int[][] ranges = {
            {1, 0, 1, 3, 1, 3},
            {2, 2, 1, 2, 3, 3},
            {1, 2, 2, 3, 3, 2}
        };
        for (int[] r : ranges) {
            Selection sel = util.select().fromTo(r[0], r[1], r[2], r[3], r[4], r[5]);
            builder.world().setBlocks(sel, ModBlocks.STURDY_DEEPSLATE.getDefaultState(), true);
            builder.world().showIndependentSection(sel, Direction.UP);
        }
        BlockPos mineralFountainPos = new BlockPos(2, 4, 2);
        builder.world().setBlock(mineralFountainPos, ModBlocks.MINERAL_FOUNTAIN.getDefaultState(), true);
        builder.world().showIndependentSection(util.select().position(mineralFountainPos), Direction.UP);
        builder.overlay()
            .showText(40)
            .text("When the conditions are met, the anvil and the impact pile will be consumed...")
            .pointAt(util.vector().blockSurface(impactPilePos, Direction.UP))
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        builder.overlay()
            .showText(20)
            .text("and a mineral fountain will be generated")
            .pointAt(util.vector().blockSurface(mineralFountainPos, Direction.UP))
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(30);

        builder.markAsFinished();
    }
}
