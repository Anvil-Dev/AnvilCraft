package dev.dubhe.anvilcraft.integration.ponder.scene.logistics;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.dubhe.anvilcraft.block.entity.MagneticChuteBlockEntity;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.ParrotElement;
import net.createmod.ponder.api.element.ParrotPose;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class SlidingRailStopScene {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> helper = registrationHelper.withKeyFunction(RegistryEntry::getId);
        helper.forComponents(ModBlocks.SLIDING_RAIL_STOP)
            .addStoryBoard("platform/9x", SlidingRailStopScene::slidingStop);
    }

    private static void slidingStop(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("sliding_rail_stop", "Basics of sliding rail stop");
        scene.configureBasePlate(0, 0, 9);
        scene.showBasePlate();
        scene.idle(20);

        int distance = 5;
        final BlockPos railStartPos1 = util.grid().at(2, 1, 2);
        final BlockPos railStartPos2 = railStartPos1.south(4);

        final BlockPos railEndPos1 = railStartPos1.east(distance);
        final BlockPos railEndPos2 = railStartPos2.east(distance);

        final BlockPos railStopPos1 = railStartPos1.west();
        final BlockPos railStopPos2 = railStartPos2.west();

        Selection railsSection1 = util.select().fromTo(railStartPos1, railEndPos1);
        Selection railsSection2 = util.select().fromTo(railStartPos2, railEndPos2);

        // 放置滑轨
        scene.world().setBlocks(railsSection1, ModBlocks.SLIDING_RAIL.getDefaultState(), false);
        scene.world().setBlocks(railsSection2, ModBlocks.SLIDING_RAIL.getDefaultState(), false);
        scene.world().showSection(railsSection1, Direction.DOWN);
        scene.world().showSection(railsSection2, Direction.DOWN);
        scene.idle(5);

        // 放置滑轨站
        scene.world().setBlock(railStopPos1, ModBlocks.SLIDING_RAIL_STOP.getDefaultState(), false);
        scene.world().setBlock(railStopPos2, ModBlocks.SLIDING_RAIL_STOP.getDefaultState(), false);
        scene.world().showSection(util.select().fromTo(railStopPos1, railStopPos2), Direction.DOWN);
        scene.idle(5);

        // 旋转视角
        scene.rotateCameraY(-45);
        scene.idle(40);

        scene.overlay()
            .showText(40)
            .text("Sliding rail stops have a powerful suction that can pull in any entity passing above it.")
            .pointAt(util.vector().centerOf(railStopPos1))
            .attachKeyFrame()
            .placeNearTarget();
        scene.idle(50);

        // 扔铁锭
        scene.world().createItemEntity(
            railEndPos1.east().above().getBottomCenter(),
            MagneticChuteBlockEntity.getOutputSpeed(Direction.WEST),
            new ItemStack(Items.IRON_INGOT)
        );
        scene.idle(50);

        // 扔鹦鹉
        ElementLink<ParrotElement> birb = scene.special().createBirb(util.vector().topOf(railEndPos1), ParrotPose.FaceCursorPose::new);
        scene.idle(15);
        scene.special().moveParrot(birb, util.vector().of(-distance - 1, 0, 0), 30);
        scene.idle(40);

        scene.overlay()
            .showText(40)
            .text("Even blocks in sliding")
            .pointAt(util.vector().centerOf(railStopPos2))
            .attachKeyFrame()
            .placeNearTarget();
        scene.idle(50);

        // 放置方块
        scene.world().setBlock(railEndPos2.above(), Blocks.GLASS.defaultBlockState(), false);
        ElementLink<WorldSectionElement> glass = scene.world()
            .showIndependentSection(util.select().position(railEndPos2.above()), Direction.DOWN);
        scene.idle(20);

        // 移动方块
        scene.world().moveSection(glass, new Vec3(-distance - 1, 0, 0), (int) (distance / SlidingBlockEntity.DEFAULT_MOVEMENT));
        scene.idle(40);

        scene.markAsFinished();
    }
}
