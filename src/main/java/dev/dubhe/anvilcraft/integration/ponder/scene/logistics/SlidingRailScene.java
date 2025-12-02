package dev.dubhe.anvilcraft.integration.ponder.scene.logistics;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.dubhe.anvilcraft.block.MagneticChuteBlock;
import dev.dubhe.anvilcraft.block.entity.MagneticChuteBlockEntity;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.ponder.api.AnvilCraftSceneBuilder;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
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
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;

public class SlidingRailScene {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> helper = registrationHelper.withKeyFunction(RegistryEntry::getId);
        helper.forComponents(ModBlocks.SLIDING_RAIL)
            .addStoryBoard("platform/9x", SlidingRailScene::itemSliding)
            .addStoryBoard("platform/9x", SlidingRailScene::blockSliding);
    }

    // 演示物品在滑轨上滑行
    private static void itemSliding(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("sliding_rail_items", "Items on sliding rails");
        builder.configureBasePlate(0, 0, 9);
        builder.showBasePlate();
        builder.idle(20);

        int distance = 5;
        final BlockPos railStartPos = util.grid().at(1, 1, 4);
        final BlockPos railEndPos = railStartPos.east(distance);
        final Selection railsSection = util.select().fromTo(railStartPos, railEndPos);
        final BlockPos chutePos = railEndPos.east();
        final Vec3 chuteInputPos = util.vector().topOf(chutePos).add(1, 1, 0);
        final Vec3 railItemPos = util.vector().centerOf(chutePos.west());
        final ItemStack ironIngots = new ItemStack(Items.IRON_INGOT, 64);

        // 创建一条长滑轨
        builder.world().setBlocks(railsSection, ModBlocks.SLIDING_RAIL.getDefaultState(), false);
        builder.world().showSection(railsSection, Direction.DOWN);
        builder.idle(10);

        // 放置磁性溜槽在滑轨旁边
        builder.world()
            .setBlock(chutePos, ModBlocks.MAGNETIC_CHUTE.getDefaultState().setValue(MagneticChuteBlock.FACING, Direction.WEST), false);
        builder.world().showSection(util.select().position(chutePos), Direction.DOWN);
        builder.idle(10);

        // 旋转视角
        builder.rotateCameraY(-45);
        builder.idle(40);

        builder.overlay()
            .showText(40)
            .text("Sliding rails have extremely smooth surfaces that allow items to slide without friction")
            .pointAt(railsSection.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        // 向磁性溜槽添加物品
        ElementLink<EntityElement> chuteItems = builder.world().createItemEntity(chuteInputPos, Vec3.ZERO, ironIngots);
        builder.idle(8);
        builder.world().removeEntity(chuteItems);
        builder.idle(16);

        // 输出物品
        builder.world().createItemEntity(railItemPos, MagneticChuteBlockEntity.getOutputSpeed(Direction.WEST), ironIngots);

        builder.overlay()
            .showText(40)
            .text("Items can slide infinitely far until they reach the end of the rail or are collected")
            .pointAt(railItemPos)
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        builder.markAsFinished();
    }

    // 演示方块在滑轨上滑行
    private static void blockSliding(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("sliding_rail_blocks", "Blocks on sliding rails");
        builder.configureBasePlate(0, 0, 9);
        builder.showBasePlate();
        builder.idle(20);

        int distance = 6;
        final BlockPos railStartPos = util.grid().at(1, 1, 4);
        final BlockPos railEndPos = railStartPos.east(distance);
        final BlockPos pistonPos = railEndPos.east().above();
        final BlockPos pistonHeadPos = pistonPos.above();
        final BlockPos leverPos = pistonPos.below();
        final BlockPos slimePos = pistonPos.west();
        final BlockPos glassPos = slimePos.above();

        final Selection rail = util.select().fromTo(railStartPos, railEndPos);
        final Selection lever = util.select().position(leverPos);

        // 创建一条长滑轨
        builder.world().setBlocks(rail, ModBlocks.SLIDING_RAIL.getDefaultState(), false);
        builder.world().showSection(rail, Direction.DOWN);
        builder.idle(5);

        // 在滑轨一端放置活塞
        builder.world().setBlock(pistonPos, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.WEST), false);
        builder.world().showSection(util.select().position(pistonPos), Direction.DOWN);
        builder.idle(5);

        // 在活塞下放置拉杆
        builder.world().setBlock(leverPos, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR), false);
        builder.world().showSection(lever, Direction.DOWN);
        builder.idle(5);

        // 在滑轨上放置方块
        builder.world().setBlock(slimePos, Blocks.GLASS.defaultBlockState(), false);
        final ElementLink<WorldSectionElement> glass = builder.world()
            .showIndependentSection(util.select().position(slimePos), Direction.DOWN);
        builder.idle(5);

        builder.overlay()
            .showText(40)
            .text("Blocks can also slide on sliding rails when pushed by pistons")
            .pointAt(slimePos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        // 激活杠杆和活塞
        builder.world().toggleRedstonePower(lever);
        builder.effects().indicateRedstone(leverPos);
        builder.world().modifyBlock(pistonPos, state -> state.setValue(PistonBaseBlock.EXTENDED, true), false);

        // 放置活塞头至活塞处
        builder.world()
            .setBlock(pistonHeadPos, Blocks.PISTON_HEAD.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.WEST), false);
        ElementLink<WorldSectionElement> pistonHead = builder.world()
            .showIndependentSectionImmediately(util.select().position(pistonHeadPos));
        builder.world().moveSection(pistonHead, new Vec3(0, -1, 0), 0);

        // 推出活塞头和其他方块
        Vec3 offset = new Vec3(-1, 0, 0);
        builder.world().moveSection(pistonHead, offset, 2);
        builder.world().moveSection(glass, offset, 2);
        builder.idle(4);

        // 其他方块继续滑动
        builder.world().moveSection(glass, new Vec3(-distance, 0, 0), (int) (distance / SlidingBlockEntity.DEFAULT_MOVEMENT));
        builder.idle(30);

        // 移除其他方块
        builder.world().hideIndependentSection(glass, Direction.UP);
        builder.idle(20);

        // 恢复拉杆，活塞头，活塞
        builder.world().toggleRedstonePower(lever);
        builder.world().moveSection(pistonHead, offset.reverse(), 2);
        builder.idle(2);
        builder.world().modifyBlock(pistonPos, state -> state.setValue(PistonBaseBlock.EXTENDED, false), false);
        builder.idle(20);

        // 在滑轨上放置结构
        builder.world().setBlock(slimePos, Blocks.SLIME_BLOCK.defaultBlockState(), false);
        builder.world().setBlock(glassPos, Blocks.GLASS.defaultBlockState(), false);
        final ElementLink<WorldSectionElement> structure = builder.world()
            .showIndependentSection(util.select().fromTo(slimePos, glassPos), Direction.DOWN);
        builder.idle(5);

        builder.overlay()
            .showText(40)
            .text("Slime blocks can stick multiple blocks together to form a sliding structure")
            .pointAt(util.vector().centerOf(slimePos))
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        // 激活杠杆和活塞
        builder.world().toggleRedstonePower(lever);
        builder.effects().indicateRedstone(leverPos);
        builder.world().modifyBlock(pistonPos, state -> state.setValue(PistonBaseBlock.EXTENDED, true), false);

        // 推出活塞头和结构
        builder.world().moveSection(pistonHead, offset, 2);
        builder.world().moveSection(structure, offset, 2);
        builder.idle(4);

        // 结构继续滑动
        builder.world().moveSection(structure, new Vec3(-distance, 0, 0), (int) (distance / SlidingBlockEntity.DEFAULT_MOVEMENT));
        builder.idle(30);

        // 移除结构
        builder.world().hideIndependentSection(structure, Direction.UP);
        builder.idle(20);

        // 恢复拉杆，活塞头，活塞
        builder.world().toggleRedstonePower(lever);
        builder.world().moveSection(pistonHead, offset.reverse(), 2);
        builder.idle(2);
        builder.world().modifyBlock(pistonPos, state -> state.setValue(PistonBaseBlock.EXTENDED, false), false);
        builder.idle(20);

        // 放置铁砧
        builder.world().setBlock(slimePos, Blocks.ANVIL.defaultBlockState(), false);
        builder.world().showSection(util.select().position(slimePos), Direction.DOWN);
        builder.idle(10);

        builder.overlay()
            .showText(40)
            .text("However, blocks that cannot be pushed by pistons also cannot slide on rails")
            .pointAt(util.vector().centerOf(slimePos))
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        // 仅激活拉杆
        builder.world().toggleRedstonePower(lever);
        builder.effects().indicateRedstone(leverPos);
        builder.idle(10);

        builder.markAsFinished();
    }
}
