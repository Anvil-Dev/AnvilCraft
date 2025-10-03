package dev.dubhe.anvilcraft.integration.ponder.scene.recipe;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class StampingScene {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> helper = registrationHelper.withKeyFunction(RegistryEntry::getId);
        helper.forComponents(ModBlocks.STAMPING_PLATFORM)
            .addStoryBoard("platform/5x", StampingScene::crafting, AnvilCraftPonderTags.PROCESSING_COMPONENTS);
    }

    private static void crafting(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("stamping", "Stamp Items");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();

        BlockPos tablePos = util.grid().at(2, 1, 2);
        builder.world().setBlock(tablePos, ModBlocks.STAMPING_PLATFORM.getDefaultState(), false);
        builder.world().showSection(util.select().position(tablePos), Direction.NORTH);

        BlockPos anvilPos = tablePos.above(2);
        builder.world().setBlock(anvilPos, Blocks.ANVIL.defaultBlockState(), false);
        ElementLink<WorldSectionElement> anvilLink = builder.world()
            .showIndependentSection(util.select().position(anvilPos), Direction.DOWN);
        builder.idle(20);

        // 物品冲压
        ElementLink<EntityElement> itemEntity = builder.world().createItemEntity(tablePos.above(), Items.IRON_INGOT.getDefaultInstance());
        builder.world().falldownSection(anvilLink);
        builder.world().replaceItemEntity(tablePos.north().getCenter(),
            Items.HEAVY_WEIGHTED_PRESSURE_PLATE.getDefaultInstance(), itemEntity);
        builder.world().riseSection(anvilLink);
        builder.idle(10);

        builder.overlay()
            .showText(20)
            .text("The stamping platform can stamp items")
            .pointAt(tablePos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(30);

        builder.markAsFinished();
    }
}
