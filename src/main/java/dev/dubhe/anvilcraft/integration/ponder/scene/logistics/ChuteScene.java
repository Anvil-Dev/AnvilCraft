package dev.dubhe.anvilcraft.integration.ponder.scene.logistics;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.dubhe.anvilcraft.block.ChuteBlock;
import dev.dubhe.anvilcraft.block.SimpleChuteBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.ponder.api.AnvilCraftSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
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
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;

public class ChuteScene {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> helper = registrationHelper.withKeyFunction(RegistryEntry::getId);
        helper.forComponents(ModBlocks.CHUTE)
            .addStoryBoard("platform/5x", ChuteScene::basicOperation)
            .addStoryBoard("platform/5x", ChuteScene::simpleChute)
            .addStoryBoard("platform/5x", ChuteScene::filtering);
    }

    // 基本操作展示：对比漏斗和溜槽，演示物品阻塞
    private static void basicOperation(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("chute_basics", "The basics of chute");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();
        builder.idle(20);

        BlockPos hopperPos = util.grid().at(1, 2, 2);
        BlockPos chutePos = hopperPos.east(2);
        BlockPos chuteTargetPos = chutePos.below();

        final Vec3 hopperItemVec = util.vector().topOf(hopperPos).add(0, 1, 0);
        final Vec3 chuteItemVec = util.vector().topOf(chutePos).add(0, 1, 0);
        final Vec3 targetItemVec = util.vector().topOf(chuteTargetPos);
        final Vec3 itemEntityPos = util.vector().centerOf(chuteTargetPos);

        final ItemStack ironIngot = new ItemStack(Items.IRON_INGOT, 1);
        final ItemStack ironIngots = new ItemStack(Items.IRON_INGOT, 64);
        final ItemStack goldIngot = new ItemStack(Items.GOLD_INGOT, 64);

        final Selection hopper = util.select().position(hopperPos);
        final Selection chute = util.select().position(chutePos);

        // 放置漏斗
        builder.world().setBlock(hopperPos, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN), false);
        builder.world().showIndependentSection(hopper, Direction.DOWN);

        // 普通的漏斗一次只能传输一个物品
        builder.overlay().showText(40).text("A normal funnel can only transmit one item at a time")
            .pointAt(util.vector().centerOf(hopperPos)).attachKeyFrame().placeNearTarget();
        builder.idle(50);

        // 放置溜槽
        builder.world().setBlock(chutePos, ModBlocks.CHUTE.getDefaultState().setValue(ChuteBlock.FACING, Direction.DOWN), false);
        builder.world().showIndependentSection(chute, Direction.DOWN);

        // 而溜槽可以一次性传输一组物品
        builder.overlay().showText(40).text("Whereas chutes can transfer a set of items at once")
            .pointAt(util.vector().centerOf(chutePos)).attachKeyFrame().placeNearTarget();
        builder.idle(50);

        // 向漏斗添加几个物品
        for (int i = 0; i < 3; i++) {
            ElementLink<EntityElement> hopperItems = builder.world().createItemEntity(hopperItemVec, Vec3.ZERO, ironIngot);
            builder.idle(8);
            // 漏斗吸收物品
            builder.world().removeEntity(hopperItems);
        }
        builder.idle(20);

        // 向溜槽添加一组物品
        ElementLink<EntityElement> chuteItem1 = builder.world().createItemEntity(chuteItemVec, Vec3.ZERO, ironIngots);
        builder.idle(8);

        // 溜槽吸收物品
        builder.world().removeEntity(chuteItem1);
        builder.idle(8);

        // 溜槽一次性输出一组物品
        final ElementLink<EntityElement> chuteItem2 = builder.world().createItemEntity(targetItemVec, Vec3.ZERO, ironIngots);
        builder.idle(8);

        // 并且可以将物品作为掉落物投掷出来
        builder.overlay().showText(40).text("and can be thrown as drops")
            .pointAt(util.vector().centerOf(chuteTargetPos).add(0, -0.5, 0)).attachKeyFrame().placeNearTarget();
        builder.idle(50);

        // 在溜槽下方添加一个方块演示阻塞，同时移除此处的物品
        builder.world().setBlock(chuteTargetPos, Blocks.GLASS.defaultBlockState(), false);
        builder.world().removeEntity(chuteItem2);
        builder.world().showSection(util.select().position(chuteTargetPos), Direction.DOWN);
        builder.idle(10);

        builder.overlay()
            .showText(40)
            // 当溜槽输出位置被方块阻挡时，物品会停止输出
            .text("When the chute output position is blocked by a block, the item will stop outputting")
            .pointAt(util.vector().centerOf(chuteTargetPos))
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        // 添加物品到溜槽，但因为被阻塞所以不会输出
        ElementLink<EntityElement> goldItem = builder.world().createItemEntity(chuteItemVec, Vec3.ZERO, goldIngot);
        builder.idle(8);
        builder.world().removeEntity(goldItem);
        builder.idle(20);

        // 移除方块，金锭被输出
        builder.world().destroyBlock(chuteTargetPos);
        builder.idle(8);
        builder.world().createItemEntity(itemEntityPos, Vec3.ZERO, goldIngot);

        builder.idle(10);

        builder.overlay()
            .showText(40)
            // 当溜槽输出位置被方块阻挡时，物品会停止输出
            .text("When there is an identical set of drops under the chute, the item will not continue to be thrown")
            .pointAt(itemEntityPos)
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(30);

        // 添加物品到溜槽，但因为下方有相同物品所以不会输出
        ElementLink<EntityElement> chuteItem3 = builder.world().createItemEntity(chuteItemVec, Vec3.ZERO, goldIngot);
        builder.idle(8);
        builder.world().removeEntity(chuteItem3);
        builder.idle(20);

        builder.markAsFinished();
    }

    // 简易溜槽演示：演示溜槽连接变为简易溜槽，以及红石信号对两种溜槽的不同影响
    private static void simpleChute(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("simple_chute", "Simple chute and normal chute");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();
        builder.idle(20);

        BlockPos chutePos = util.grid().at(1, 2, 2);
        BlockPos leverPos = util.grid().at(1, 1, 2);
        BlockPos topChutePos = util.grid().at(2, 3, 2);
        BlockPos simplePos = util.grid().at(2, 2, 2);

        final Selection simple = util.select().position(simplePos);
        final Selection topChute = util.select().position(topChutePos);
        final Selection lever = util.select().position(leverPos);

        final Vec3 topItemVec = util.vector().topOf(topChutePos).add(0, 1, 0);
        final Vec3 targetItemVec = util.vector().topOf(new BlockPos(2, 1, 2));
        final Vec3 leftItemVec = util.vector().topOf(chutePos).add(0, 1, 0);

        final ItemStack diamonds = new ItemStack(Items.DIAMOND, 16);
        final ItemStack emeralds = new ItemStack(Items.EMERALD, 32);
        final ItemStack redstoneItems = new ItemStack(Items.REDSTONE, 16);
        final ItemStack moreItems = new ItemStack(Items.DIAMOND, 16);

        // 放置一个溜槽
        builder.world().setBlock(chutePos, ModBlocks.CHUTE.getDefaultState().setValue(ChuteBlock.FACING, Direction.EAST), false);
        Selection chute = util.select().position(chutePos);
        builder.world().showIndependentSection(chute, Direction.DOWN);
        builder.idle(20);

        // 放置第二个溜槽（连接到第一个的输出）
        builder.world().setBlock(simplePos, ModBlocks.CHUTE.getDefaultState().setValue(ChuteBlock.FACING, Direction.DOWN), false);
        builder.world().showIndependentSection(simple, Direction.DOWN);
        builder.idle(20);

        // 第二个溜槽变为简易溜槽
        builder.world().modifyBlock(
            simplePos,
            state -> ModBlocks.SIMPLE_CHUTE.getDefaultState().setValue(SimpleChuteBlock.FACING, Direction.DOWN),
            false
        );

        builder.overlay()
            .showText(40)
            // 当一个溜槽连接到另一个溜槽的输出方向，它会变成简易溜槽
            .text("When one chute is connected to the output direction of another chute, it becomes a simple chute")
            .pointAt(util.vector().centerOf(simplePos))
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        // 在简易溜槽上方放置另一个溜槽
        builder.world().setBlock(topChutePos, ModBlocks.CHUTE.getDefaultState().setValue(ChuteBlock.FACING, Direction.DOWN), false);
        // 同时更新简易溜槽为TALL状态
        builder.world()
            .modifyBlock(simplePos, state -> ModBlocks.SIMPLE_CHUTE.getDefaultState().setValue(SimpleChuteBlock.TALL, true), false);
        builder.world().showIndependentSection(topChute, Direction.DOWN);
        builder.idle(20);

        // 向上方溜槽添加物品
        ElementLink<EntityElement> topItem = builder.world().createItemEntity(topItemVec, Vec3.ZERO, diamonds);
        builder.idle(8);

        // 上方溜槽吸收物品
        builder.world().removeEntity(topItem);

        // 简易溜槽输出物品
        builder.idle(16);
        builder.world().createItemEntity(targetItemVec, Vec3.ZERO, diamonds);

        builder.overlay()
            .showText(40)
            // 简易溜槽可以从多个方向接收其他溜槽的物品
            .text("The simple chute can receive items from other chutes from multiple directions")
            .pointAt(util.vector().centerOf(simplePos))
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        // 演示左侧溜槽输送物品到简易溜槽
        ElementLink<EntityElement> leftItem = builder.world().createItemEntity(leftItemVec, Vec3.ZERO, emeralds);
        builder.idle(8);

        // 左侧溜槽吸收物品
        builder.world().removeEntity(leftItem);
        builder.idle(16);

        // 简易溜槽输出物品
        builder.world().createItemEntity(targetItemVec, Vec3.ZERO, emeralds);
        builder.idle(20);

        // 添加红石控制
        builder.world().setBlock(leverPos, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR), false);
        builder.world().showIndependentSection(lever, Direction.DOWN);
        builder.idle(20);

        // 激活杠杆
        builder.world().toggleRedstonePower(lever);
        builder.effects().indicateRedstone(leverPos);

        // 锁定普通溜槽
        builder.world().modifyBlock(chutePos, state -> state.setValue(ChuteBlock.ENABLED, false), false);

        // 普通溜槽会被红石信号锁定
        builder.overlay().showText(40).text("Normal chutes are locked by redstone signals")
            .pointAt(util.vector().centerOf(chutePos)).attachKeyFrame().placeNearTarget();
        builder.idle(50);

        // 添加物品到锁定的溜槽
        builder.world().createItemEntity(leftItemVec, Vec3.ZERO, redstoneItems);
        builder.idle(40);

        // 向上方溜槽添加物品，展示简易溜槽不被锁定
        ElementLink<EntityElement> moreTopItem = builder.world().createItemEntity(topItemVec, Vec3.ZERO, moreItems);
        builder.idle(8);

        // 上方溜槽吸收物品
        builder.world().removeEntity(moreTopItem);
        builder.idle(16);

        // 简易溜槽输出物品
        builder.world().createItemEntity(targetItemVec, Vec3.ZERO, moreItems);

        // 而简易溜槽不会被红石信号锁定
        builder.overlay().showText(40).text("The simple chute will not be locked by the redstone signal")
            .pointAt(util.vector().centerOf(simplePos)).attachKeyFrame().placeNearTarget();
        builder.idle(50);

        builder.markAsFinished();
    }

    // 过滤功能演示：演示溜槽的物品过滤功能
    private static void filtering(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("chute_filtering", "Chute item filtration");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();
        builder.idle(20);

        BlockPos chutePos = util.grid().at(2, 2, 2);

        final Vec3 itemDropPos = util.vector().topOf(chutePos).add(0, 1, 0);
        final Vec3 targetItemPos = util.vector().topOf(util.grid().at(2, 1, 2));

        final ItemStack diamond = new ItemStack(Items.DIAMOND);
        final ItemStack iron = new ItemStack(Items.IRON_INGOT);

        // 放置溜槽
        builder.world().setBlock(chutePos, ModBlocks.CHUTE.getDefaultState().setValue(ChuteBlock.FACING, Direction.DOWN), false);
        Selection chute = util.select().position(chutePos);
        builder.world().showIndependentSection(chute, Direction.DOWN);
        builder.idle(20);

        // 演示右键打开UI
        builder.overlay()
            .showControls(util.vector().blockSurface(chutePos, Direction.WEST), Pointing.RIGHT, 20)
            .rightClick()
            .withItem(Items.AIR.getDefaultInstance());

        // 右键溜槽可以打开过滤设置界面
        builder.overlay().showText(40).text("Right-click the chute to open the filter settings interface")
            .pointAt(util.vector().blockSurface(chutePos, Direction.WEST)).attachKeyFrame().placeNearTarget();
        builder.idle(50);

        // 设置过滤器
        builder.overlay()
            .showControls(util.vector().blockSurface(chutePos, Direction.WEST), Pointing.DOWN, 20)
            .withItem(Items.DIAMOND.getDefaultInstance());

        builder.overlay().showText(40).text("Set it allow only diamonds to pass through") // 设置其只允许钻石通过
            .pointAt(util.vector().blockSurface(chutePos, Direction.WEST)).attachKeyFrame().placeNearTarget();
        builder.idle(50);

        // 钻石（应该通过）
        ElementLink<EntityElement> diamondItem = builder.world().createItemEntity(itemDropPos, Vec3.ZERO, diamond);
        builder.idle(8);

        // 溜槽吸收钻石
        builder.world().removeEntity(diamondItem);
        builder.idle(8);

        // 钻石从溜槽输出
        builder.world().createItemEntity(targetItemPos, Vec3.ZERO, diamond);
        builder.idle(20);

        // 铁锭（应该被阻挡）
        builder.world().createItemEntity(itemDropPos, Vec3.ZERO, iron);
        builder.idle(20);

        builder.overlay()
            .showText(40)
            // 溜槽只会让设定的物品通过，不会吸收过滤条件以外的物品
            .text("The chute will only let the set item pass through and will not absorb items outside of the filter condition")
            .pointAt(util.vector().centerOf(chutePos))
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        builder.markAsFinished();
    }
}
