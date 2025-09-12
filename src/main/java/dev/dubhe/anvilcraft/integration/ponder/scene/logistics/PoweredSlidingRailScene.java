package dev.dubhe.anvilcraft.integration.ponder.scene.logistics;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.dubhe.anvilcraft.block.entity.MagneticChuteBlockEntity;
import dev.dubhe.anvilcraft.block.sliding.PoweredSlidingRailBlock;
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
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;

public class PoweredSlidingRailScene {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> helper = registrationHelper.withKeyFunction(RegistryEntry::getId);
        helper.forComponents(ModBlocks.POWERED_SLIDING_RAIL)
            .addStoryBoard("platform/9x", PoweredSlidingRailScene::basicOperation)
            .addStoryBoard("platform/9x", PoweredSlidingRailScene::withRailStop);
    }

    private static void basicOperation(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("powered_sliding_rail", "Powered sliding rail basics");
        scene.configureBasePlate(0, 0, 9);
        scene.showBasePlate();
        scene.idle(20);

        int distance = 7;
        BlockPos railStartPos = util.grid().at(1, 1, 4);
        BlockPos railEndPos = railStartPos.east(distance);
        BlockPos poweredRailPos = railStartPos.east(4);
        BlockPos railStopPos = railStartPos.west();
        BlockPos leverPos = poweredRailPos.north();

        Vec3 poweredRailVec = util.vector().centerOf(poweredRailPos);

        Selection poweredRail = util.select().position(poweredRailPos);
        Selection rail = util.select().fromTo(railStartPos, railEndPos).substract(poweredRail);
        Selection lever = util.select().position(leverPos);

        // 放置普通滑轨
        scene.world().setBlocks(rail, ModBlocks.SLIDING_RAIL.getDefaultState(), false);
        scene.world().showSection(rail, Direction.DOWN);
        scene.idle(5);

        // 放置动力滑轨
        scene.world().setBlock(
            poweredRailPos,
            ModBlocks.POWERED_SLIDING_RAIL.getDefaultState().setValue(PoweredSlidingRailBlock.FACING, Direction.WEST),
            false
        );
        scene.world().showSection(poweredRail, Direction.DOWN);
        scene.idle(5);

        // 放置滑轨站
        scene.world().setBlock(railStopPos, ModBlocks.SLIDING_RAIL_STOP.getDefaultState(), false);
        scene.world().showSection(util.select().position(railStopPos), Direction.DOWN);
        scene.idle(5);

        // 放置拉杆
        scene.world().setBlock(leverPos, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR), false);
        scene.world().showSection(lever, Direction.DOWN);
        scene.idle(10);

        // 旋转视角
        scene.rotateCameraY(-45);
        scene.idle(40);

        // 第一部分：无红石信号时的效果
        scene.overlay()
            .showText(60)
            .text("Without redstone signal, powered sliding rail acts like a sliding rail stop for items and blocks")
            .pointAt(poweredRailVec)
            .attachKeyFrame()
            .placeNearTarget();
        scene.idle(70);

        // 添加鹦鹉演示生物不受影响
        ElementLink<ParrotElement> birb = scene.special().createBirb(util.vector().topOf(railEndPos), ParrotPose.FaceCursorPose::new);
        scene.idle(20);

        scene.overlay()
            .showText(40)
            .text("But living entities like parrots are not affected by the powered rail")
            .pointAt(railEndPos.above().getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        scene.idle(50);

        // 移动鹦鹉
        scene.special().moveParrot(birb, util.vector().of(-distance - 1, 0, 0), 40);
        scene.idle(50);

        // 隐藏鹦鹉
        scene.special().hideElement(birb, Direction.UP);
        scene.idle(30);

        scene.overlay()
            .showText(40)
            .text("Items and blocks passing over will be attracted and stopped")
            .pointAt(poweredRailVec)
            .placeNearTarget();
        scene.idle(50);

        // 扔物品
        scene.world().createItemEntity(
            railEndPos.east().above().getBottomCenter(),
            MagneticChuteBlockEntity.getOutputSpeed(Direction.WEST),
            new ItemStack(Items.IRON_INGOT)
        );
        scene.idle(30);

        // 放置方块
        scene.world().setBlock(railEndPos.above(), Blocks.GLASS.defaultBlockState(), false);
        ElementLink<WorldSectionElement> glass = scene.world()
            .showIndependentSection(util.select().position(railEndPos.above()), Direction.DOWN);
        scene.idle(10);

        // 移动方块到动力滑轨上方并停止
        scene.world().moveSection(glass, new Vec3(-3, 0, 0), (int) (3 / SlidingBlockEntity.DEFAULT_MOVEMENT));
        scene.idle(20);

        // 第二部分：通入红石信号的效果
        scene.overlay()
            .showText(60)
            .text("When powered with redstone, it accelerates items and pushes blocks forward")
            .pointAt(poweredRailVec)
            .attachKeyFrame()
            .placeNearTarget();
        scene.idle(70);

        // 激活拉杆
        scene.world().toggleRedstonePower(lever);
        scene.world().toggleRedstonePower(poweredRail);

        // 方块开始移动
        scene.world().moveSection(glass, new Vec3(-5, 0, 0), (int) (5 / SlidingBlockEntity.DEFAULT_MOVEMENT));
        scene.idle(10);

        scene.markAsFinished();
    }

    private static void withRailStop(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("with_rail_stop", "Powered sliding rail with rail stop");
        scene.configureBasePlate(0, 0, 9);
        scene.showBasePlate();
        scene.idle(20);

        int distance = 4;
        BlockPos railStartPos = util.grid().at(1, 1, 4);
        BlockPos railEndPos = railStartPos.east(distance);
        BlockPos poweredRailPos = railEndPos.east();
        BlockPos railStopPos = poweredRailPos.east();
        BlockPos leverPos = poweredRailPos.north();

        Selection poweredRail = util.select().position(poweredRailPos);
        Selection rail = util.select().fromTo(railStartPos, railEndPos);
        Selection lever = util.select().position(leverPos);

        // 放置普通滑轨
        scene.world().setBlocks(rail, ModBlocks.SLIDING_RAIL.getDefaultState(), false);
        scene.world().showSection(rail, Direction.DOWN);
        scene.idle(5);

        // 放置动力滑轨
        scene.world().setBlock(
            poweredRailPos,
            ModBlocks.POWERED_SLIDING_RAIL.getDefaultState().setValue(PoweredSlidingRailBlock.FACING, Direction.WEST),
            false
        );
        scene.world().showSection(poweredRail, Direction.DOWN);
        scene.idle(5);

        // 放置滑轨站
        scene.world().setBlock(railStopPos, ModBlocks.SLIDING_RAIL_STOP.getDefaultState(), false);
        scene.world().showSection(util.select().position(railStopPos), Direction.DOWN);
        scene.idle(5);

        // 放置拉杆
        scene.world().setBlock(leverPos, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR), false);
        scene.world().showSection(lever, Direction.DOWN);
        scene.idle(10);

        // 旋转视角
        scene.rotateCameraY(-45);
        scene.idle(20);

        scene.overlay()
            .showText(60)
            .text("When there's a sliding rail stop behind, it pulls items and blocks from the stop and accelerates them forward")
            .pointAt(util.vector().centerOf(railStopPos))
            .attachKeyFrame()
            .placeNearTarget();
        scene.idle(70);

        // 扔物品
        scene.world().createItemEntity(railStopPos.above().getBottomCenter(), Vec3.ZERO, new ItemStack(Items.IRON_INGOT));
        scene.idle(30);

        // 放置方块
        scene.world().setBlock(railStopPos.above(), Blocks.GLASS.defaultBlockState(), false);
        ElementLink<WorldSectionElement> glass = scene.world()
            .showIndependentSection(util.select().position(railStopPos.above()), Direction.DOWN);
        scene.idle(20);

        scene.world().toggleRedstonePower(lever);
        scene.world().toggleRedstonePower(poweredRail);

        // 移动方块到动力滑轨上方并停止
        scene.world().moveSection(glass, new Vec3(-distance - 3, 0, 0), (int) (distance + 3 / SlidingBlockEntity.DEFAULT_MOVEMENT));
        scene.idle(20);

        scene.markAsFinished();
    }
}
