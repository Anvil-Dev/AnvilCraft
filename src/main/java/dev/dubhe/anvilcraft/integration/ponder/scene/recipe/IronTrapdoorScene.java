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
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;

import static net.minecraft.world.item.Items.IRON_TRAPDOOR;

public class IronTrapdoorScene {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<Item> helper = registrationHelper.withKeyFunction(BuiltInRegistries.ITEM::getKey);
        helper.forComponents(IRON_TRAPDOOR)
            .addStoryBoard("platform/5x", IronTrapdoorScene::crafting, AnvilCraftPonderTags.PROCESSING_COMPONENTS);
    }

    private static void crafting(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("iron_trapdoor", "Use Iron Trapdoor to trigger unpack Recipe");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();
        // 创建铁活板门
        BlockPos ironTrapdoorPos = new BlockPos(2, 1, 2);
        builder.world().setBlock(ironTrapdoorPos, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.TOP), false);
        builder.world().showIndependentSection(util.select().position(ironTrapdoorPos), Direction.NORTH);
        // 创建铁砧
        BlockPos anvilPos = new BlockPos(2, 3, 2);
        builder.world().setBlock(anvilPos, Blocks.ANVIL.defaultBlockState(), false);
        ElementLink<WorldSectionElement> anvilLink = builder.world()
            .showIndependentSection(util.select().position(anvilPos), Direction.NORTH);
        builder.idle(20);

        // 执行操作
        ItemStack itemStack = new ItemStack(Blocks.QUARTZ_BLOCK, 1);
        ElementLink<EntityElement> quartzBlock =
            builder.world().createItemEntity(ironTrapdoorPos.above().getCenter(), Vec3.ZERO, itemStack);
        builder.idle(20);

        // 铁砧下落
        builder.world().falldownSection(anvilLink);
        // 拆解石英块
        builder.world().removeEntity(quartzBlock);
        builder.world().createItemEntity(ironTrapdoorPos.getCenter(), Vec3.ZERO, new ItemStack(Items.QUARTZ.asItem(), 4));
        builder.world().riseSection(anvilLink);
        builder.idle(10);

        // 生成文本
        builder.overlay()
            .showText(50)
            .text("You can use the iron trapdoor to disassemble composite items into their components")
            .pointAt(util.vector().blockSurface(ironTrapdoorPos, Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();

        builder.idle(20);

        builder.markAsFinished();
    }
}
