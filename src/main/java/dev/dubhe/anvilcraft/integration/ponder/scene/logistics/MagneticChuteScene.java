package dev.dubhe.anvilcraft.integration.ponder.scene.logistics;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.dubhe.anvilcraft.block.ChuteBlock;
import dev.dubhe.anvilcraft.block.MagneticChuteBlock;
import dev.dubhe.anvilcraft.block.SimpleChuteBlock;
import dev.dubhe.anvilcraft.block.entity.MagneticChuteBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.ponder.api.AnvilCraftSceneBuilder;
import net.createmod.catnip.math.Pointing;
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
import net.minecraft.world.phys.Vec3;

public class MagneticChuteScene {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> helper = registrationHelper.withKeyFunction(RegistryEntry::getId);
        helper.forComponents(ModBlocks.MAGNETIC_CHUTE)
            .addStoryBoard("platform/5x", MagneticChuteScene::basicOperation)
            .addStoryBoard("platform/5x", MagneticChuteScene::chuteConnections)
            .addStoryBoard("platform/5x", MagneticChuteScene::filtering);
    }

    // 基本操作展示：对比普通溜槽和磁性溜槽，展示物品掉落方式的不同
    private static void basicOperation(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("magnetic_chute_basics", "The basics of magnetic chute");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();
        builder.idle(20);

        BlockPos chutePos = util.grid().at(1, 2, 2);
        BlockPos magneticPos = chutePos.offset(2, 0, 0);

        final Vec3 chuteItemVec = util.vector().topOf(chutePos.above());
        final Vec3 magneticItemVec = util.vector().topOf(magneticPos.offset(0, 1, -1));
        final Vec3 chuteDropVec = util.vector().centerOf(chutePos.south());
        final Vec3 magneticDropVec = util.vector().centerOf(magneticPos.south());

        final ItemStack goldIngot = new ItemStack(Items.GOLD_INGOT, 16);

        // 放置普通溜槽
        builder.world().setBlock(chutePos, ModBlocks.CHUTE.getDefaultState().setValue(ChuteBlock.FACING, Direction.SOUTH), false);
        Selection chute = util.select().position(chutePos);
        builder.world().showSection(chute, Direction.DOWN);
        builder.idle(5);

        // 放置磁性溜槽，朝南方向
        builder.world()
            .setBlock(magneticPos, ModBlocks.MAGNETIC_CHUTE.getDefaultState().setValue(MagneticChuteBlock.FACING, Direction.SOUTH), false);
        Selection magnetic = util.select().position(magneticPos);
        builder.world().showSection(magnetic, Direction.DOWN);
        builder.idle(5);

        builder.overlay()
            .showText(40)
            .text("A normal chute drops items directly downward")
            .pointAt(util.vector().centerOf(chutePos))
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        builder.overlay()
            .showText(40)
            .text("Magnetic chutes attract items from behind and propel them forward")
            .pointAt(util.vector().centerOf(magneticPos))
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        // 向普通溜槽添加物品
        ItemStack ironIngot = new ItemStack(Items.IRON_INGOT, 16);
        ElementLink<EntityElement> chuteItems = builder.world().createItemEntity(chuteItemVec, Vec3.ZERO, ironIngot);
        builder.idle(8);

        // 普通溜槽吸收物品
        builder.world().removeEntity(chuteItems);
        builder.idle(10);

        // 普通溜槽侧向输出物品
        builder.world().createItemEntity(chuteDropVec, Vec3.ZERO, ironIngot);
        builder.idle(30);

        // 向磁性溜槽添加物品
        ElementLink<EntityElement> magneticItems = builder.world().createItemEntity(magneticItemVec, Vec3.ZERO, goldIngot);
        builder.idle(8);

        // 磁性溜槽吸收物品
        builder.world().removeEntity(magneticItems);
        builder.idle(16);

        // 磁性溜槽朝前方投掷物品
        builder.world().createItemEntity(magneticDropVec, MagneticChuteBlockEntity.getOutputSpeed(Direction.SOUTH), goldIngot);

        builder.overlay()
            .showText(40)
            .text("When items are ejected, they're given forward momentum")
            .pointAt(magneticDropVec)
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        builder.markAsFinished();
    }

    // 溜槽连接演示：演示磁性溜槽的连接关系
    private static void chuteConnections(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("magnetic_chute_connections", "Magnetic chute connections");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(20);

        final BlockPos magneticPos = util.grid().at(1, 2, 2);
        final BlockPos chutePos = magneticPos.east();
        final BlockPos magnetic2Pos = magneticPos.east();

        // 演示1：磁性溜槽 + 普通溜槽
        scene.world()
            .setBlock(magneticPos, ModBlocks.MAGNETIC_CHUTE.getDefaultState().setValue(MagneticChuteBlock.FACING, Direction.EAST), false);
        Selection magnetic = util.select().position(magneticPos);
        scene.world().showSection(magnetic, Direction.DOWN);
        scene.idle(20);

        // 放置普通溜槽在磁性溜槽的输出方向
        scene.world().setBlock(chutePos, ModBlocks.CHUTE.getDefaultState().setValue(ChuteBlock.FACING, Direction.DOWN), false);
        final ElementLink<WorldSectionElement> chuteLink = scene.world().showIndependentSection(
            util.select().position(chutePos),
            Direction.DOWN
        );
        scene.idle(10);

        // 普通溜槽变为简易溜槽
        scene.world().modifyBlock(
            chutePos,
            state -> ModBlocks.SIMPLE_CHUTE.getDefaultState().setValue(SimpleChuteBlock.FACING, Direction.DOWN),
            false
        );
        scene.idle(20);

        scene.overlay()
            .showText(40)
            .text("Regular chutes still transform into simple chutes when connected to magnetic chutes")
            .pointAt(util.vector().centerOf(chutePos))
            .attachKeyFrame()
            .placeNearTarget();
        scene.idle(50);

        // 演示2：清除之前的方块
        scene.world().hideIndependentSection(chuteLink, Direction.UP);
        scene.idle(20);

        // 放置磁性溜槽替换简易溜槽
        scene.world()
            .setBlock(magnetic2Pos, ModBlocks.MAGNETIC_CHUTE.getDefaultState().setValue(MagneticChuteBlock.FACING, Direction.DOWN), false);
        scene.world().showSection(util.select().position(magnetic2Pos), Direction.DOWN);
        scene.idle(20);

        scene.overlay()
            .showText(40)
            .text("But magnetic chutes don't transform into simple chutes when connected to other magnetic chutes")
            .pointAt(magnetic2Pos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        scene.idle(50);

        scene.markAsFinished();
    }

    // 过滤功能演示：演示磁性溜槽的物品过滤功能
    private static void filtering(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("magnetic_chute_filtering", "Magnetic chute item filtration");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();
        builder.idle(20);

        BlockPos magneticPos = util.grid().at(2, 2, 2);
        final Vec3 itemDropPos = util.vector().topOf(magneticPos.above());
        final Vec3 targetItemPos = util.vector().topOf(magneticPos.below());

        // 放置磁性溜槽
        builder.world()
            .setBlock(magneticPos, ModBlocks.MAGNETIC_CHUTE.getDefaultState().setValue(MagneticChuteBlock.FACING, Direction.DOWN), false);
        Selection magnetic = util.select().position(magneticPos);
        builder.world().showSection(magnetic, Direction.DOWN);
        builder.idle(20);

        // 演示右键打开UI
        builder.overlay()
            .showControls(util.vector().blockSurface(magneticPos, Direction.WEST), Pointing.RIGHT, 20)
            .rightClick()
            .withItem(Items.AIR.getDefaultInstance());

        builder.overlay()
            .showText(40)
            .text("Right-click the magnetic chute to open the filter settings interface")
            .pointAt(util.vector().blockSurface(magneticPos, Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        // 设置过滤器
        builder.overlay()
            .showControls(util.vector().blockSurface(magneticPos, Direction.WEST), Pointing.DOWN, 20)
            .withItem(Items.DIAMOND.getDefaultInstance());

        builder.overlay()
            .showText(40)
            .text("Set it to allow only diamonds to pass through")
            .pointAt(util.vector().blockSurface(magneticPos, Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        // 钻石（应该通过）
        ItemStack diamond = new ItemStack(Items.DIAMOND, 1);
        ElementLink<EntityElement> diamondItem = builder.world().createItemEntity(itemDropPos, Vec3.ZERO, diamond);
        builder.idle(8);

        // 磁性溜槽吸收钻石
        builder.world().removeEntity(diamondItem);
        builder.idle(16);

        // 钻石从磁性溜槽输出
        builder.world().createItemEntity(targetItemPos, MagneticChuteBlockEntity.getOutputSpeed(Direction.DOWN), diamond);
        builder.idle(20);

        // 铁锭（应该被阻挡）
        builder.world().createItemEntity(itemDropPos, Vec3.ZERO, new ItemStack(Items.IRON_INGOT, 1));
        builder.idle(20);

        builder.overlay()
            .showText(40)
            .text("The magnetic chute will only let the set item pass through and will not absorb items outside of the filter condition")
            .pointAt(util.vector().centerOf(magneticPos))
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        builder.markAsFinished();
    }
}
