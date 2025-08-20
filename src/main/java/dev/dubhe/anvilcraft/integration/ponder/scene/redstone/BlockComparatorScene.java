package dev.dubhe.anvilcraft.integration.ponder.scene.redstone;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.dubhe.anvilcraft.block.BlockComparatorBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.integration.ponder.AnvilCraftPonderTags;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import org.jetbrains.annotations.NotNull;

public class BlockComparatorScene {
    public static void register(@NotNull PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> helper = registrationHelper.withKeyFunction(RegistryEntry::getId);
        helper.forComponents(
                ModBlocks.BLOCK_COMPARATOR
            )
            .addStoryBoard(
                "platform/555",
                BlockComparatorScene::run,
                AnvilCraftPonderTags.REDSTONE_COMPONENTS);

    }

    public static void run(@NotNull SceneBuilder scene, @NotNull SceneBuildingUtil util) {
        scene.title("block_comparator", "Block Comparator");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        // 初始化
        BlockPos bulbAPos = util.grid().at(1, 1, 2);
        scene.world().setBlock(bulbAPos, Blocks.COPPER_BULB.defaultBlockState(), false);
        scene.world().showIndependentSection(util.select().position(bulbAPos), Direction.UP);

        BlockPos comparatorBPos = util.grid().at(2, 1, 2);
        scene.world().setBlock(comparatorBPos, ModBlocks.BLOCK_COMPARATOR.getDefaultState(), false);
        scene.world().showIndependentSection(util.select().position(comparatorBPos), Direction.UP);

        BlockPos lampPos = util.grid().at(2, 1, 3);
        scene.world().setBlock(lampPos, Blocks.REDSTONE_LAMP.defaultBlockState(), false);
        scene.world().showIndependentSection(util.select().position(lampPos), Direction.UP);
        scene.idle(30);

        // 放置另一个灯，改变信号
        BlockPos bulbBPos = util.grid().at(3, 1, 2);
        scene.world().setBlock(bulbBPos, Blocks.COPPER_BULB.defaultBlockState(), false);
        scene.world().showIndependentSection(util.select().position(bulbBPos), Direction.UP);
        scene.world().modifyBlock(bulbBPos, blockState -> blockState.setValue(CopperBulbBlock.LIT, true), false);

        scene.world().modifyBlock(comparatorBPos, blockState -> blockState.setValue(BlockComparatorBlock.POWERED, true), false);
        scene.world().modifyBlock(lampPos, blockState -> blockState.setValue(RedstoneLampBlock.LIT, true), false);

        scene.overlay().showText(40)
            .text("When the blocks on either side of the comparator are the same, it’ll send out a signal.")
            .pointAt(util.vector().centerOf(comparatorBPos))
            .attachKeyFrame()
            .placeNearTarget();
        scene.idle(50);

        // 改变比较器，信号消失
        scene.overlay().showControls(util.vector().centerOf(comparatorBPos), Pointing.RIGHT, 20)
            .rightClick();
        scene.world().modifyBlock(comparatorBPos, blockState -> blockState.setValue(BlockComparatorBlock.PRECISE, true), false);

        scene.world().modifyBlock(comparatorBPos, blockState -> blockState.setValue(BlockComparatorBlock.POWERED, false), false);
        scene.world().modifyBlock(lampPos, blockState -> blockState.setValue(RedstoneLampBlock.LIT, false), false);

        scene.overlay().showText(40)
            .text("Right-click to turn on precise mode—it’ll check super carefully if the blocks are exactly the same.")
            .pointAt(util.vector().centerOf(comparatorBPos))
            .attachKeyFrame()
            .placeNearTarget();

        scene.idle(60);
        // 改变灯，发出信号
        scene.world().modifyBlock(bulbBPos, blockState -> blockState.setValue(CopperBulbBlock.LIT, false), false);

        scene.world().modifyBlock(comparatorBPos, blockState -> blockState.setValue(BlockComparatorBlock.POWERED, true), false);
        scene.world().modifyBlock(lampPos, blockState -> blockState.setValue(RedstoneLampBlock.LIT, true), false);

        scene.idle(20);

        scene.markAsFinished(); // 标记场景结束
    }
}
