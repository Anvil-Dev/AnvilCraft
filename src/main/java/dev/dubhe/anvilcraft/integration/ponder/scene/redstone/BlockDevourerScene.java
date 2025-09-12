package dev.dubhe.anvilcraft.integration.ponder.scene.redstone;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.dubhe.anvilcraft.block.BlockDevourerBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.ponder.AnvilCraftPonderTags;
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
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class BlockDevourerScene {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> helper = registrationHelper.withKeyFunction(RegistryEntry::getId);
        helper.forComponents(ModBlocks.BLOCK_DEVOURER)
            .addStoryBoard("platform/5x", BlockDevourerScene::run, AnvilCraftPonderTags.REDSTONE_COMPONENTS)
            .addStoryBoard("platform/9x", BlockDevourerScene::anvilRun, AnvilCraftPonderTags.POWER_COMPONENTS)
            .addStoryBoard("platform/9x", BlockDevourerScene::anvilFall, AnvilCraftPonderTags.POWER_COMPONENTS);
    }

    private static void run(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("block_devourer", "Block Devourer");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();
        builder.rotateCameraY(-45);
        builder.idle(20);

        Selection wallPos = util.select().fromTo(1, 0, 1, 3, 2, 1);
        builder.world().setBlocks(wallPos, Blocks.IRON_BLOCK.defaultBlockState(), false);
        builder.world().showSection(wallPos, Direction.DOWN);

        BlockPos devourerPos = util.grid().at(2, 1, 2);
        builder.world().setBlock(devourerPos, ModBlocks.BLOCK_DEVOURER.getDefaultState(), false);
        builder.world().showSection(util.select().position(devourerPos), Direction.NORTH);

        BlockPos leverPos = util.grid().at(1, 1, 2);
        builder.world().setBlock(leverPos, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR), false);
        builder.world().showSection(util.select().position(leverPos), Direction.NORTH);
        builder.idle(20);

        // 激活，物品掉于背后
        builder.world().modifyBlock(leverPos, state -> state.setValue(BlockStateProperties.POWERED, true), false);
        builder.effects().indicateRedstone(leverPos);
        builder.world().modifyBlock(devourerPos, state -> state.setValue(BlockDevourerBlock.TRIGGERED, true), false);
        builder.world().setBlocks(wallPos, Blocks.AIR.defaultBlockState(), true);
        ElementLink<EntityElement> item = builder.world().createItemEntity(devourerPos.south(), new ItemStack(Items.IRON_BLOCK, 9));
        builder.idle(20);

        builder.overlay()
            .showOutlineWithText(wallPos, 60)
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(devourerPos.getCenter())
            .text("When the Block Devourer receives the Redstone signal, it will try to destroy the cube in the 3x3 space ahead.");
        builder.idle(70);

        BlockPos chestPos = devourerPos.south();
        builder.world().modifyBlock(devourerPos, state -> state.setValue(BlockDevourerBlock.TRIGGERED, false), false);
        // 删掉物品换上箱子
        builder.world().removeEntity(item);
        builder.world().setBlock(chestPos, Blocks.CHEST.defaultBlockState(), false);
        builder.world().showSection(util.select().position(chestPos), Direction.NORTH);
        builder.world().modifyBlock(leverPos, state -> state.setValue(BlockStateProperties.POWERED, false), false);
        builder.idle(10);

        builder.overlay()
            .showOutlineWithText(util.select().position(chestPos), 80)
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(chestPos.getCenter())
            .text("The Block Devourer can place the destroyed blocks into the chest back at the back.");
        builder.idle(70);

        // 清除石头，说明：方块吞噬器破坏世界基底方块只会有很小一部分产生掉落物
        builder.world().setBlocks(wallPos, Blocks.STONE.defaultBlockState(), false);
        builder.world().hideSection(util.select().position(chestPos), Direction.SOUTH);
        builder.idle(20);

        builder.world().setBlock(chestPos, Blocks.AIR.defaultBlockState(), false);
        builder.world().modifyBlock(leverPos, state -> state.setValue(BlockStateProperties.POWERED, true), false);
        builder.effects().indicateRedstone(leverPos);
        builder.world().modifyBlock(devourerPos, state -> state.setValue(BlockDevourerBlock.TRIGGERED, true), false);
        builder.world().setBlocks(wallPos, Blocks.AIR.defaultBlockState(), true);
        item = builder.world().createItemEntity(devourerPos.south(), new ItemStack(Items.COBBLESTONE));
        builder.idle(20);

        builder.overlay()
            .showText(60)
            .text("Only a very small portion of the world's base blocks will be retained.")
            .pointAt(devourerPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(70);

        builder.world().removeEntity(item);

        builder.markAsFinished();


    }

    private static void anvilRun(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("block_devourer_anvil", "Devour with Anvil");
        builder.configureBasePlate(0, 0, 9);
        builder.showBasePlate();
        builder.scaleSceneView(0.6f);
        builder.rotateCameraY(-45);
        builder.idle(20);

        BlockPos devourerPos = util.grid().at(4, 4, 4);
        builder.world().setBlock(devourerPos, ModBlocks.BLOCK_DEVOURER.getDefaultState(), false);
        builder.world().showSection(util.select().position(devourerPos), Direction.NORTH);

        BlockPos anvilPos = devourerPos.above();
        builder.world().setBlock(anvilPos, Blocks.ANVIL.defaultBlockState(), false);
        ElementLink<WorldSectionElement> anvilLink = builder.world()
            .showIndependentSection(util.select().position(anvilPos), Direction.NORTH);

        for (int i = 1; i <= 3; i++) {
            int r = i + 1;
            Selection wallPos = util.select().fromTo(4 - r, 4 - r, 3, 4 + r, 4 + r, 3);
            builder.world().riseSection(anvilLink, i);
            builder.world().setBlocks(wallPos, Blocks.IRON_BLOCK.defaultBlockState(), false);
            builder.world().showSection(wallPos, Direction.SOUTH);
            builder.idle(20);

            builder.world().falldownSection(anvilLink, i);
            builder.world().setBlocks(wallPos, Blocks.AIR.defaultBlockState(), true);
            builder.world().createItemEntity(devourerPos.south(), new ItemStack(Items.IRON_BLOCK, r * r));
            builder.world().hideSection(wallPos, Direction.NORTH);
            builder.idle(20);

        }
        builder.overlay()
            .showText(100)
            .text("The Block Devourer can destroy larger blocks based on the height from which the anvil falls, with a maximum area of 9x9.")
            .pointAt(devourerPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(110);

        builder.markAsFinished();
    }

    private static void anvilFall(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("block_devourer_anvil_fall", "Falling Block Devourer");
        builder.configureBasePlate(0, 0, 9);
        builder.showBasePlate();
        builder.scaleSceneView(0.6f);
        builder.idle(20);

        Selection bedPos = util.select().fromTo(1, 1, 1, 7, 1, 7);
        builder.world().setBlocks(bedPos, Blocks.BEDROCK.defaultBlockState(), false);
        builder.world().showSection(bedPos, Direction.DOWN);

        Selection stonePos = util.select().fromTo(2, 2, 2, 7, 6, 7);
        builder.world().setBlocks(stonePos, Blocks.STONE.defaultBlockState(), false);
        builder.world().showSection(stonePos, Direction.DOWN);

        BlockPos devourerPos = util.grid().at(4, 7, 4);
        builder.world().setBlock(devourerPos, ModBlocks.BLOCK_DEVOURER.getDefaultState()
            .setValue(BlockDevourerBlock.FACING, Direction.DOWN), false);
        ElementLink<WorldSectionElement> devourerLink = builder.world()
            .showIndependentSection(util.select().position(devourerPos), Direction.DOWN);

        BlockPos anvilPos = devourerPos.above();
        builder.world().setBlock(anvilPos, Blocks.ANVIL.defaultBlockState(), false);
        ElementLink<WorldSectionElement> anvilLink = builder.world()
            .showIndependentSection(util.select().position(anvilPos), Direction.DOWN);
        builder.idle(20);

        builder.world().riseSection(anvilLink);
        builder.idle(10);

        for (int i = 0; i < 5; i++) {
            builder.world().falldownSection(anvilLink);
            builder.world().moveSection(devourerLink, new Vec3(0, -1, 0), 1);
            Selection destroyPos = util.select().fromTo(2, 6 - i, 2, 6, 6 - i, 6);
            builder.world().setBlocks(destroyPos, Blocks.AIR.defaultBlockState(), true);
            builder.idle(4);
        }
        builder.world().falldownSection(anvilLink);
        builder.idle(10);

        builder.overlay()
            .showText(80)
            .text(
                "When a downward-facing block devourer is hit by an anvil, it will move down one block at the same time.")
            .pointAt(devourerPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(90);
    }

}
