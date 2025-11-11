package dev.dubhe.anvilcraft.integration.ponder.scene.structure;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.ponder.AnvilCraftPonderTags;
import dev.dubhe.anvilcraft.integration.ponder.api.AnvilCraftSceneBuilder;
import dev.dubhe.anvilcraft.integration.ponder.api.instruction.Interpolation;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class GiantAnvilScene {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> helper = registrationHelper.withKeyFunction(RegistryEntry::getId);
        // 对着这两个方块可以寻思撼地
        helper.forComponents(ModBlocks.GIANT_ANVIL, ModBlocks.HEAVY_IRON_BLOCK)
            // 注册场景：撼地
            .addStoryBoard("platform/33x", GiantAnvilScene::shock, AnvilCraftPonderTags.GIANT_ANVIL)
            // 注册场景：撼地的范围
            .addStoryBoard("platform/33x", GiantAnvilScene::shockRange, AnvilCraftPonderTags.GIANT_ANVIL)
            // 注册场景：撼地的模式
            .addStoryBoard("platform/33x", GiantAnvilScene::shockMode, AnvilCraftPonderTags.GIANT_ANVIL)
            // 注册场景：撼地的模式2
            .addStoryBoard("platform/33x", GiantAnvilScene::shockMode2, AnvilCraftPonderTags.GIANT_ANVIL);
    }

    private static void shock(SceneBuilder scene, SceneBuildingUtil util) {
        // 使用Builder
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        // 场景名：撼地，标题：在重质铁块上触发撼地
        builder.title("shock", "shock on heavy iron block");
        // 地板33x33
        builder.configureBasePlate(0, 0, 33);
        // 摄像机缩放0.6f
        builder.scaleSceneView(0.6f);
        // 下移一格
        builder.setSceneOffsetY(-1);
        // 放置地板
        builder.showBasePlate();
        // 延时10gt
        builder.idle(10);
        // 放置重质铁块
        builder.world().setBlock(new BlockPos(16, 0, 16), ModBlocks.HEAVY_IRON_BLOCK.getDefaultState(), false);
        Selection heavyIronBlock = util.select().position(16, 0, 16);
        builder.world().showSection(heavyIronBlock, Direction.DOWN);
        // 生成XeKr
        ElementLink<EntityElement> mushroomCow = GiantAnvilScene.spawnMushroomCow(util, builder);
        // 延时10gt
        builder.idle(10);
        // 文本：当巨型铁砧落在重质铁块上
        builder.overlay()
            .showText(35)
            .text("When a giant anvil falls onto a heavy iron block")
            .pointAt(util.vector().blockSurface(util.grid().at(16, 1, 16), Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();
        // 延时20gt
        builder.idle(30);
        // 放置巨型铁砧并链接
        builder.world().setBlock(
            new BlockPos(16, 12, 16),
            ModBlocks.GIANT_ANVIL.getDefaultState()
                .setValue(GiantAnvilBlock.HALF, Cube3x3PartHalf.MID_CENTER)
                .setValue(GiantAnvilBlock.CUBE, GiantAnvilCube.CENTER),
            false
        );
        Selection giantAnvil = util.select().position(16, 12, 16);
        ElementLink<WorldSectionElement> giantAnvilLink = builder.world().showIndependentSection(giantAnvil, Direction.DOWN);
        // 巨型铁砧下落10m
        builder.world().moveSectionInterpolation(giantAnvilLink, new Vec3(0, -10, 0), Interpolation.acceleration(0.05));
        // 文本：就会产生撼地冲击向四周扩散
        builder.overlay()
            .showText(40)
            .text("It unleashes a shockwave ripples outward in all directions")
            .pointAt(util.vector().blockSurface(util.grid().at(16, 1, 16), Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();
        // XeKr冒烟红温倒地
        builder.world().letLivingEntityDie(mushroomCow, util.grid().at(17, 1, 12), false);
        // 延时30gt
        builder.idle(30);
        // 文本：冲击波会对不穿鞋的生物造成伤害
        builder.overlay()
            .showText(40)
            .text("The shockwave harms any creature not wearing shoes")
            .pointAt(util.vector().blockSurface(util.grid().at(17, 1, 12), Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();
        // 延时40gt
        builder.idle(40);
        // 标记场景结束
        builder.markAsFinished();
    }

    private static void shockRange(SceneBuilder scene, SceneBuildingUtil util) {
        // 使用builder
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        // 场景名：撼地的范围，标题：巨型铁砧的下落高度决定了撼地的范围
        builder.title("range_of_shock", "The FallDistance of Giant Anvil determines the shock range");
        // 地板33x33
        builder.configureBasePlate(0, 0, 33);
        // 摄像机缩放0.3f
        builder.scaleSceneView(0.3f);
        // 放置地板
        builder.showBasePlate();
        // 延时10gt
        builder.idle(10);
        // 放置重质铁块
        builder.world().setBlock(new BlockPos(16, 0, 16), ModBlocks.HEAVY_IRON_BLOCK.getDefaultState(), false);
        Selection heavyIronBlock = util.select().position(16, 0, 16);
        builder.world().showSection(heavyIronBlock, Direction.DOWN);
        // 生成XeKr
        ElementLink<EntityElement> mushroomCow = GiantAnvilScene.spawnMushroomCow(util, builder);
        // 选择框5x1x5，文本：下落3格高的巨型铁砧可以撼地5x5的范围
        builder.overlay()
            .showOutlineWithText(util.select().fromTo(14, 0, 14, 18, 0, 18), 70)
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(15, 1, 15), Direction.NORTH))
            .text("Giant Anvil falls 3m can shock an area of 25m²");
        // 延时10gt
        builder.idle(40);
        // 放置3高巨型铁砧并链接
        builder.world().setBlock(
            new BlockPos(16, 5, 16),
            ModBlocks.GIANT_ANVIL.getDefaultState()
                .setValue(GiantAnvilBlock.HALF, Cube3x3PartHalf.MID_CENTER)
                .setValue(GiantAnvilBlock.CUBE, GiantAnvilCube.CENTER),
            false
        );
        Selection giantAnvil = util.select().position(16, 5, 16);
        ElementLink<WorldSectionElement> giantAnvilLink = builder.world().showIndependentSection(giantAnvil, Direction.NORTH);
        // 延时20gt
        builder.idle(20);
        // 巨型铁砧下落3m
        builder.world().moveSectionInterpolation(giantAnvilLink, new Vec3(0, -3, 0), Interpolation.acceleration(0.05));
        // 延时30gt
        builder.idle(30);
        // 巨型铁砧上升16m 10gt
        builder.world().moveSection(giantAnvilLink, new Vec3(0, 16, 0), 10);
        // 选择框33x1x33，文本：而下落16格高的巨型铁砧可以撼地33x33的范围
        builder.overlay()
            .showOutlineWithText(util.select().fromTo(0, 0, 0, 32, 0, 32), 70)
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(15, 1, 15), Direction.NORTH))
            .text("And Giant Anvil falls 16m can shock an area of 1089m²");
        // 延时40gt
        builder.idle(40);
        // 巨型铁砧下降16m
        builder.world().moveSectionInterpolation(giantAnvilLink, new Vec3(0, -16, 0), Interpolation.acceleration(0.05));
        // XeKr冒烟红温倒地
        builder.world().letLivingEntityDie(mushroomCow, util.grid().at(17, 1, 10), false);
        // 延时10gt
        builder.idle(10);
        // 文本：同时，撼地的伤害也会随着巨型铁砧的下落高度而增加
        builder.overlay()
            .showText(40)
            .text("Additionally, shock damage increases with the FallDistance of Giant Anvil")
            .pointAt(util.vector().blockSurface(util.grid().at(17, 1, 10), Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();
        // 延时60gt
        builder.idle(60);
        // 标记场景结束
        builder.markAsFinished();
    }

    private static void shockMode(SceneBuilder scene, SceneBuildingUtil util) {
        // 使用builder
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        // 场景名：撼地的模式，标题：重质铁块一圈的方块决定了撼地的模式
        builder.title("mode_of_shock", "Blocks around the Heavy Iron Block determines Shock mode");
        // 地板33x33
        builder.configureBasePlate(0, 0, 33);
        // 摄像机缩放0.6f
        builder.scaleSceneView(0.6f);
        // 下移一格
        builder.setSceneOffsetY(-1);
        // 放置地板
        builder.showBasePlate();
        // 10gt延时
        builder.idle(10);
        // 放置重质铁块
        builder.world().setBlock(new BlockPos(16, 0, 16), ModBlocks.HEAVY_IRON_BLOCK.getDefaultState(), false);
        Selection heavyIronBlock = util.select().position(16, 0, 16);
        builder.world().showSection(heavyIronBlock, Direction.DOWN);
        // 延时20gt
        builder.idle(20);
        // 选择框3x1x3，文本：改变重质铁块周围一圈的方块可以改变撼地的模式
        builder.overlay()
            .showOutlineWithText(util.select().fromTo(15, 0, 15, 17, 0, 17), 50)
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(15, 0, 16), Direction.NORTH))
            .text("Changing the blocks surrounding the Heavy Iron Block can alter the Shock mode");
        // 绕圈先后放置8个树脂块
        Selection resinBlock;
        BlockPos[] path = {
            new BlockPos(15, 0, 15),
            new BlockPos(15, 0, 16),
            new BlockPos(15, 0, 17),
            new BlockPos(16, 0, 17),
            new BlockPos(17, 0, 17),
            new BlockPos(17, 0, 16),
            new BlockPos(17, 0, 15),
            new BlockPos(16, 0, 15)
        };
        for (BlockPos pos : path) {
            builder.world().setBlock(pos, ModBlocks.RESIN_BLOCK.getDefaultState(), false);
            resinBlock = util.select().position(pos);
            builder.world().showSection(resinBlock, Direction.DOWN);
            builder.idle(2);
        }
        // 延时40gt
        builder.idle(40);
        // 文本：例如树脂块可以让撼地冲击弹飞铁砧
        builder.overlay()
            .showText(50)
            .text("For example, resin block can make anvils within the shock range bounce upward once")
            .pointAt(util.vector().blockSurface(util.grid().at(15, 1, 13), Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();
        // 放置铁砧并链接
        builder.world().setBlock(new BlockPos(15, 1, 13), Blocks.ANVIL.defaultBlockState(), false);
        Selection anvil = util.select().position(15, 1, 13);
        ElementLink<WorldSectionElement> anvilLink = builder.world().showIndependentSection(anvil, Direction.NORTH);
        // 放置巨型铁砧并链接
        builder.world().setBlock(
            new BlockPos(16, 12, 16),
            ModBlocks.GIANT_ANVIL.getDefaultState()
                .setValue(GiantAnvilBlock.HALF, Cube3x3PartHalf.MID_CENTER)
                .setValue(GiantAnvilBlock.CUBE, GiantAnvilCube.CENTER),
            false
        );
        Selection giantAnvil = util.select().position(16, 12, 16);
        ElementLink<WorldSectionElement> giantAnvilLink = builder.world().showIndependentSection(giantAnvil, Direction.NORTH);
        // 延时20gt
        builder.idle(20);
        // 巨型铁砧下落10m
        builder.world().moveSectionInterpolation(giantAnvilLink, new Vec3(0, -10, 0), Interpolation.acceleration(0.05));
        // 铁砧上升1m
        builder.world().moveSectionInterpolation(anvilLink, new Vec3(0, 1, 0), Interpolation.acceleration(0.05));
        // 铁砧下降1m
        builder.world().moveSectionInterpolation(anvilLink, new Vec3(0, -1, 0), Interpolation.acceleration(0.05));
        // 延时40gt
        builder.idle(40);
        // 巨型铁砧上升10m
        builder.world().moveSection(giantAnvilLink, new Vec3(0, 10, 0), 10);
        // 生成XeKr
        ElementLink<EntityElement> mushroomCow = GiantAnvilScene.spawnMushroomCow(util, builder);
        // 四边放置诅咒金块
        BlockPos[] cursedGoldPath = {
            new BlockPos(15, 0, 16),
            new BlockPos(16, 0, 15),
            new BlockPos(17, 0, 16),
            new BlockPos(16, 0, 17)
        };
        for (BlockPos pos : cursedGoldPath) {
            builder.world().setBlock(pos, ModBlocks.CURSED_GOLD_BLOCK.getDefaultState(), false);
            Selection block = util.select().position(pos);
            builder.world().showSection(block, Direction.DOWN);
        }
        // 文本：四边放置诅咒金块可以在撼地伤害的基础上附加等量伤害
        builder.overlay()
            .showText(55)
            .text("Placing Cursed Gold Blocks on the four sides can add equal additional damage to the shock damage")
            .pointAt(util.vector().blockSurface(util.grid().at(15, 0, 16), Direction.NORTH))
            .attachKeyFrame()
            .placeNearTarget();
        // 延时60gt
        builder.idle(60);
        // 四角放置红宝石块
        BlockPos[] rubyPath = {
            new BlockPos(15, 0, 15),
            new BlockPos(15, 0, 17),
            new BlockPos(17, 0, 15),
            new BlockPos(17, 0, 17)
        };
        for (BlockPos pos : rubyPath) {
            builder.world().setBlock(pos, ModBlocks.RUBY_BLOCK.getDefaultState(), false);
            Selection block = util.select().position(pos);
            builder.world().showSection(block, Direction.DOWN);
        }
        // 文本：四角放置不同的宝石块可以附加不同类型的伤害
        builder.overlay()
            .showText(55)
            .text("Placing different gem blocks at the four corners can add various types of damage")
            .pointAt(util.vector().blockSurface(util.grid().at(15, 0, 15), Direction.NORTH))
            .attachKeyFrame()
            .placeNearTarget();
        // 延时60gt
        builder.idle(60);
        // 巨型铁砧下落10m
        builder.world().moveSectionInterpolation(giantAnvilLink, new Vec3(0, -10, 0), Interpolation.acceleration(0.05));
        // XeKr着火冒烟红温倒地
        builder.world().letLivingEntityDie(mushroomCow, util.grid().at(17, 1, 12), true, entity -> {
            entity.setRemainingFireTicks(60);
            entity.setSharedFlagOnFire(true);
        });
        // 生成熟牛排物品
        builder.world().createItemEntity(util.vector().of(17, 1, 12), util.vector().of(0, 0.1, 0), Items.COOKED_BEEF.getDefaultInstance());
        // 文本：例如红宝石块可以附加火焰伤害
        builder.overlay()
            .showText(55)
            .text("For example, Ruby Blocks can add fire damage")
            .pointAt(util.vector().blockSurface(util.grid().at(17, 1, 12), Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();
        // 延时10gt
        builder.idle(10);
        // 延时40gt
        builder.idle(50);
        // 巨型铁砧上升10m
        builder.world().moveSection(giantAnvilLink, new Vec3(0, 10, 0), 10);
        // 四角放置蓝宝石块
        for (BlockPos pos : rubyPath) {
            builder.world().setBlock(pos, ModBlocks.SAPPHIRE_BLOCK.getDefaultState(), false);
            Selection block = util.select().position(pos);
            builder.world().showSection(block, Direction.DOWN);
        }
        // 文本：蓝宝石块可以附加冰冻伤害
        builder.overlay()
            .showText(55)
            .text("Sapphire Blocks can add freezing damage")
            .pointAt(util.vector().blockSurface(util.grid().at(15, 0, 15), Direction.NORTH))
            .attachKeyFrame()
            .placeNearTarget();
        // 延时60gt
        builder.idle(60);
        // 四角放置黄玉块
        for (BlockPos pos : rubyPath) {
            builder.world().setBlock(pos, ModBlocks.TOPAZ_BLOCK.getDefaultState(), false);
            Selection block = util.select().position(pos);
            builder.world().showSection(block, Direction.DOWN);
        }
        // 文本：黄玉块可以附加雷击伤害
        builder.overlay()
            .showText(55)
            .text("Topaz Blocks can add lightning damage")
            .pointAt(util.vector().blockSurface(util.grid().at(15, 0, 15), Direction.NORTH))
            .attachKeyFrame()
            .placeNearTarget();
        // 延时60gt
        builder.idle(60);
        // 四角放置虚空物质块
        for (BlockPos pos : rubyPath) {
            builder.world().setBlock(pos, ModBlocks.VOID_MATTER_BLOCK.getDefaultState(), false);
            Selection block = util.select().position(pos);
            builder.world().showSection(block, Direction.DOWN);
        }
        // 文本：虚空物质块可以附加无视护甲的虚空伤害
        builder.overlay()
            .showText(55)
            .text("Void Matter Blocks can add armor-piercing void damage")
            .pointAt(util.vector().blockSurface(util.grid().at(15, 0, 15), Direction.NORTH))
            .attachKeyFrame()
            .placeNearTarget();
        // 生成一群钻石套僵尸
        ElementLink<EntityElement> zombie1 = GiantAnvilScene.spawnZombie(util, builder);
        ElementLink<EntityElement> zombie2 = GiantAnvilScene.spawnZombie(util, builder);
        ElementLink<EntityElement> zombie3 = GiantAnvilScene.spawnZombie(util, builder);
        ElementLink<EntityElement> zombie4 = GiantAnvilScene.spawnZombie(util, builder);
        ElementLink<EntityElement> zombie5 = GiantAnvilScene.spawnZombie(util, builder);
        ElementLink<EntityElement> zombie6 = GiantAnvilScene.spawnZombie(util, builder);
        ElementLink<EntityElement> zombie7 = GiantAnvilScene.spawnZombie(util, builder);
        ElementLink<EntityElement> zombie8 = GiantAnvilScene.spawnZombie(util, builder);
        // 分散放置僵尸
        ElementLink<EntityElement>[] zombies = new ElementLink[]{
            zombie1, zombie2, zombie3, zombie4, zombie5, zombie6, zombie7, zombie8
        };
        Vec3[] positions = {
            util.vector().of(13, 1, 13),
            util.vector().of(19, 1, 13),
            util.vector().of(13, 1, 19),
            util.vector().of(19, 1, 19),
            util.vector().of(16, 1, 12),
            util.vector().of(12, 1, 16),
            util.vector().of(20, 1, 16),
            util.vector().of(16, 1, 20)
        };
        
        for (int i = 0; i < zombies.length; i++) {
            final int index = i;
            builder.world().modifyEntity(zombies[i], entity -> {
                entity.setPos(positions[index]);
                // 让僵尸面向中心点(16, 1, 16)
                Vec3 center = util.vector().of(16, 1, 16);
                Vec3 direction = center.subtract(positions[index]).normalize();
                float yRot = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
                entity.setYRot(yRot);
                entity.setYHeadRot(yRot);
                entity.yRotO = yRot;
                try {
                    var field = entity.getClass().getDeclaredField("yHeadRotO");
                    field.setAccessible(true);
                    field.setFloat(entity, yRot);
                } catch (Exception ignored) {
                }
            });
        }
        // 延时30gt
        builder.idle(30);
        // 铁砧下落10m
        builder.world().moveSectionInterpolation(giantAnvilLink, new Vec3(0, -10, 0), Interpolation.acceleration(0.05));
        // 僵尸同时冒烟红温倒地
        builder.world().letLivingEntitysDie(
            new ElementLink[]{zombie1, zombie2, zombie3, zombie4, zombie5, zombie6, zombie7, zombie8},
            new BlockPos[]{
                util.grid().at(13, 1, 13),
                util.grid().at(19, 1, 13),
                util.grid().at(13, 1, 19),
                util.grid().at(19, 1, 19),
                util.grid().at(16, 1, 12),
                util.grid().at(12, 1, 16),
                util.grid().at(20, 1, 16),
                util.grid().at(16, 1, 20)
            },
            true
        );
        // 文本：用于处死大批怪物非常合适
        builder.overlay()
            .showText(55)
            .text("Very suitable for executing tons of monsters.")
            .pointAt(util.vector().blockSurface(util.grid().at(15, 0, 15), Direction.NORTH))
            .attachKeyFrame()
            .placeNearTarget();
        // 延时55gt
        builder.idle(55);
        // 标记场景结束
        builder.markAsFinished();
    }

    private static void shockMode2(SceneBuilder scene, SceneBuildingUtil util) {
        // 使用builder
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        // 场景名：破坏模式，标题：撼地的另一个模式——破坏模式
        builder.title("dectruction_mode", "Another mode of shock: Destruction Mode");
        // 地板33x33
        builder.configureBasePlate(0, 0, 33);
        // 摄像机缩放0.6f
        builder.scaleSceneView(0.6f);
        // 放置地板
        builder.showBasePlate();
        // 下移一格
        builder.setSceneOffsetY(-1);
        // 10gt延时
        builder.idle(10);
        // 放置重质铁块
        builder.world().setBlock(new BlockPos(16, 0, 16), ModBlocks.HEAVY_IRON_BLOCK.getDefaultState(), false);
        Selection heavyIronBlock = util.select().position(16, 0, 16);
        builder.world().showSection(heavyIronBlock, Direction.DOWN);
        // 延时20gt
        builder.idle(20);
        // 在重质铁块四边依次放置铁砧，方向都朝向重质铁块
        BlockPos[] anvilPositions = {
            new BlockPos(15, 0, 16),
            new BlockPos(16, 0, 15),
            new BlockPos(17, 0, 16),
            new BlockPos(16, 0, 17)
        };

        Direction[] anvilDirections = {
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
            Direction.NORTH
        };

        ElementLink<WorldSectionElement>[] anvilLinks = new ElementLink[4];
        for (int i = 0; i < anvilPositions.length; i++) {
            builder.world().setBlock(anvilPositions[i], Blocks.ANVIL.defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, anvilDirections[i]), false);
            Selection anvilSelection = util.select().position(anvilPositions[i]);
            anvilLinks[i] = builder.world().showIndependentSection(anvilSelection, Direction.DOWN);
        }

        // 四个选择框，框住铁砧，文本：当重质铁块四边为任意铁砧，就会触发撼地的破坏模式
        builder.overlay()
            .showOutlineWithText(util.select().fromTo(15, 0, 16, 17, 0, 16)
                .add(util.select().fromTo(16, 0, 15, 16, 0, 17)), 55)
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(15, 0, 16), Direction.WEST))
            .text("When a heavy iron block is surrounded on four sides by anvils, it triggers the destruction mode of shock.");

        builder.idle(60);

        // 四个选择框，框住四个角，文本：改变四个角的方块可以改变破坏的目标
        builder.overlay()
            .showOutlineWithText(util.select().fromTo(15, 0, 15, 17, 0, 17), 65)
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(15, 0, 15), Direction.NORTH))
            .text("Changing the blocks at the four corners can change the destruction targets");

        // 在四角放置黑曜石
        BlockPos[] cornerPositions = {
            new BlockPos(15, 0, 15),
            new BlockPos(15, 0, 17),
            new BlockPos(17, 0, 15),
            new BlockPos(17, 0, 17)
        };

        for (BlockPos pos : cornerPositions) {
            builder.world().setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), false);
            Selection obsidian = util.select().position(pos);
            builder.world().showSection(obsidian, Direction.DOWN);
        }

        builder.idle(5);

        // 在四角放置草方块
        for (BlockPos pos : cornerPositions) {
            builder.world().setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), false);
        }

        builder.idle(5);

        // 在四角放置干草块
        for (BlockPos pos : cornerPositions) {
            builder.world().setBlock(pos, Blocks.HAY_BLOCK.defaultBlockState(), false);
        }

        builder.idle(5);

        // 在四角放置橡木原木
        for (BlockPos pos : cornerPositions) {
            builder.world().setBlock(pos, Blocks.OAK_LOG.defaultBlockState(), false);
        }

        builder.idle(5);

        // 在四角放置黑曜石
        for (BlockPos pos : cornerPositions) {
            builder.world().setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), false);
        }

        builder.idle(40);

        // 放置大铁砧在高处并链接
        builder.world().setBlock(
            new BlockPos(16, 12, 16),
            ModBlocks.GIANT_ANVIL.getDefaultState()
                .setValue(GiantAnvilBlock.HALF, Cube3x3PartHalf.MID_CENTER)
                .setValue(GiantAnvilBlock.CUBE, GiantAnvilCube.CENTER),
            false
        );
        Selection giantAnvil = util.select().position(16, 12, 16);
        ElementLink<WorldSectionElement> giantAnvilLink = builder.world().showIndependentSection(giantAnvil, Direction.DOWN);

        // 在旁边排成一行放置石头，冰块，金矿石，基岩
        BlockPos[] blockRowPositions = {
            new BlockPos(13, 1, 15),
            new BlockPos(13, 1, 16),
            new BlockPos(13, 1, 17)
        };
        net.minecraft.world.level.block.Block[] blocks = {
            Blocks.STONE,
            Blocks.GOLD_ORE,
            Blocks.ICE
        };

        ElementLink<WorldSectionElement>[] blockRowLinks = new ElementLink[blockRowPositions.length];
        ElementLink<EntityElement>[] itemLinks1 = new ElementLink[2];
        for (int i = 0; i < blockRowPositions.length; i++) {
            builder.world().setBlock(blockRowPositions[i], blocks[i].defaultBlockState(), false);
            Selection blockSelection = util.select().position(blockRowPositions[i]);
            blockRowLinks[i] = builder.world().showIndependentSection(blockSelection, Direction.DOWN);
        }

        // 选择框，文本：底座四角为黑曜石时，撼地会破坏任意可破坏的方块
        builder.overlay()
            .showOutlineWithText(util.select().fromTo(13, 1, 15, 13, 1, 17), 80)
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(13, 1, 15), Direction.WEST))
            .text("When the base corners are obsidian, shock breaks any breakable block.");

        builder.idle(40);

        // 大铁砧下落
        builder.world().moveSectionInterpolation(giantAnvilLink, new Vec3(0, -10, 0), Interpolation.acceleration(0.05));

        // 破坏石头生成圆石掉落物，破坏冰块，破坏金矿石生成粗金掉落物
        builder.world().hideIndependentSection(blockRowLinks[0], Direction.UP);
        builder.world().hideIndependentSection(blockRowLinks[1], Direction.UP);
        builder.world().hideIndependentSection(blockRowLinks[2], Direction.UP);

        builder.world().setBlock(blockRowPositions[0], Blocks.AIR.defaultBlockState(), true);
        builder.world().setBlock(blockRowPositions[1], Blocks.AIR.defaultBlockState(), true);
        builder.world().setBlock(blockRowPositions[2], Blocks.AIR.defaultBlockState(), true);

        itemLinks1[0] = builder.world().createItemEntity(util.vector().of(13, 1, 15), util.vector().of(0, 0.1, 0), 
            new net.minecraft.world.item.ItemStack(Blocks.COBBLESTONE));
        itemLinks1[1] = builder.world().createItemEntity(util.vector().of(13, 1, 16), util.vector().of(0, 0.1, 0), 
            new net.minecraft.world.item.ItemStack(Items.RAW_GOLD, 1));

        builder.idle(40);

        // 大铁砧上升回初始位置
        builder.world().moveSection(giantAnvilLink, new Vec3(0, 6, 0), 8);

        builder.idle(10);

        // 清除掉落物
        for (ElementLink<EntityElement> link : itemLinks1) {
            builder.world().removeEntity(link);
        }

        builder.idle(10);

        // 重质铁块四面铁砧放置成皇家铁砧
        for (int i = 0; i < anvilPositions.length; i++) {
            builder.world().setBlock(anvilPositions[i], ModBlocks.ROYAL_ANVIL.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, anvilDirections[i]), true);
        }

        builder.idle(10);

        // 在旁边排成一行放置石头，冰块，金矿石
        ElementLink<EntityElement>[] itemLinks2 = new ElementLink[3];
        for (int i = 0; i < blockRowPositions.length; i++) {
            builder.world().setBlock(blockRowPositions[i], blocks[i].defaultBlockState(), false);
            blockRowLinks[i] = builder.world().showIndependentSection(util.select().position(blockRowPositions[i]), Direction.DOWN);
        }

        // 选择框，文本：当四边换为皇家铁砧时，撼地会带有精准采集效果
        builder.overlay()
            .showOutlineWithText(util.select().fromTo(13, 1, 15, 13, 1, 17), 65)
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(13, 1, 15), Direction.WEST))
            .text("When the four sides are Royal Anvils, Shock gains Silk Touch effect.");

        builder.idle(40);

        // 大铁砧下落
        builder.world().moveSectionInterpolation(giantAnvilLink, new Vec3(0, -6, 0), Interpolation.acceleration(0.05));

        // 破坏石头生成石头掉落物，破坏冰块生成冰块掉落物，破坏金矿石生成金矿石掉落物
        builder.world().hideIndependentSection(blockRowLinks[0], Direction.UP);
        builder.world().hideIndependentSection(blockRowLinks[1], Direction.UP);
        builder.world().hideIndependentSection(blockRowLinks[2], Direction.UP);

        builder.world().setBlock(blockRowPositions[0], Blocks.AIR.defaultBlockState(), true);
        builder.world().setBlock(blockRowPositions[1], Blocks.AIR.defaultBlockState(), true);
        builder.world().setBlock(blockRowPositions[2], Blocks.AIR.defaultBlockState(), true);
        
        itemLinks2[0] = builder.world().createItemEntity(util.vector().of(13, 1, 15), util.vector().of(0, 0.1, 0), 
            new net.minecraft.world.item.ItemStack(Blocks.STONE));
        itemLinks2[1] = builder.world().createItemEntity(util.vector().of(13, 1, 16), util.vector().of(0, 0.1, 0), 
            new net.minecraft.world.item.ItemStack(Blocks.GOLD_ORE));
        itemLinks2[2] = builder.world().createItemEntity(util.vector().of(13, 1, 17), util.vector().of(0, 0.1, 0), 
            new net.minecraft.world.item.ItemStack(Blocks.ICE));

        builder.idle(40);

        // 大铁砧上升回初始位置
        builder.world().moveSection(giantAnvilLink, new Vec3(0, 6, 0), 8);

        builder.idle(10);

        // 清除掉落物
        for (ElementLink<EntityElement> link : itemLinks2) {
            builder.world().removeEntity(link);
        }

        builder.idle(10);

        // 重质铁块四面皇家铁砧放置成余烬铁砧
        for (int i = 0; i < anvilPositions.length; i++) {
            builder.world().setBlock(anvilPositions[i], ModBlocks.EMBER_ANVIL.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, anvilDirections[i]), true);
        }

        builder.idle(10);

        // 在旁边放置石头，冰块，金矿石
        ElementLink<EntityElement>[] itemLinks3 = new ElementLink[2];
        for (int i = 0; i < blockRowPositions.length; i++) {
            builder.world().setBlock(blockRowPositions[i], blocks[i].defaultBlockState(), false);
            blockRowLinks[i] = builder.world().showIndependentSection(util.select().position(blockRowPositions[i]), Direction.DOWN);
        }

        // 选择框，文本：当四边换为余烬铁砧时，撼地会带有自动熔炼效果
        builder.overlay()
            .showOutlineWithText(util.select().fromTo(13, 1, 15, 13, 1, 17), 65)
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(13, 1, 15), Direction.WEST))
            .text("When the four sides are Ember Anvils, Shock gains Auto-Smelting effect.");

        builder.idle(40);

        // 大铁砧下落
        builder.world().moveSectionInterpolation(giantAnvilLink, new Vec3(0, -6, 0), Interpolation.acceleration(0.05));

        // 破坏石头生成石头掉落物，破坏冰块，破坏金矿石生成金锭掉落物，不破坏基岩
        builder.world().hideIndependentSection(blockRowLinks[0], Direction.UP);
        builder.world().hideIndependentSection(blockRowLinks[1], Direction.UP);
        builder.world().hideIndependentSection(blockRowLinks[2], Direction.UP);

        builder.world().setBlock(blockRowPositions[0], Blocks.AIR.defaultBlockState(), true);
        builder.world().setBlock(blockRowPositions[1], Blocks.AIR.defaultBlockState(), true);
        builder.world().setBlock(blockRowPositions[2], Blocks.AIR.defaultBlockState(), true);

        itemLinks3[0] = builder.world().createItemEntity(util.vector().of(13, 1, 15), util.vector().of(0, 0.1, 0), 
            new net.minecraft.world.item.ItemStack(Blocks.STONE));
        itemLinks3[1] = builder.world().createItemEntity(util.vector().of(13, 1, 16), util.vector().of(0, 0.1, 0), 
            new net.minecraft.world.item.ItemStack(Items.GOLD_INGOT));

        builder.idle(30);

        // 文本指向石头：此处石头被破坏掉落圆石，圆石又被熔炼为石头
        builder.overlay()
            .showText(65)
            .text("Here, stone drops cobblestone when destroyed, which is then smelted back into stone")
            .pointAt(util.vector().blockSurface(util.grid().at(13, 1, 15), Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();

        builder.idle(60);

        // 大铁砧上升回初始位置
        builder.world().moveSection(giantAnvilLink, new Vec3(0, 6, 0), 8);

        builder.idle(10);

        // 清除掉落物
        for (ElementLink<EntityElement> link : itemLinks3) {
            builder.world().removeEntity(link);
        }
        builder.idle(10);

        // 重质铁块四面余烬铁砧放置成超限铁砧
        for (int i = 0; i < anvilPositions.length; i++) {
            builder.world().setBlock(anvilPositions[i], ModBlocks.TRANSCENDENCE_ANVIL.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, anvilDirections[i]), true);
        }

        builder.idle(10);

        // 在旁边排成一行放置石头，冰块，金矿石
        ElementLink<EntityElement>[] itemLinks4 = new ElementLink[2];
        for (int i = 0; i < blockRowPositions.length; i++) {
            builder.world().setBlock(blockRowPositions[i], blocks[i].defaultBlockState(), false);
            blockRowLinks[i] = builder.world().showIndependentSection(util.select().position(blockRowPositions[i]), Direction.DOWN);
        }

        // 选择框，文本：当四边换为超限铁砧时，撼地会带有时运V效果
        builder.overlay()
            .showOutlineWithText(util.select().fromTo(13, 1, 15, 13, 1, 17), 65)
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(13, 1, 15), Direction.WEST))
            .text("When the four sides are transcendence anvils, Shock gains Fortune V effect.");

        builder.idle(40);

        // 大铁砧下落
        builder.world().moveSectionInterpolation(giantAnvilLink, new Vec3(0, -6, 0), Interpolation.acceleration(0.05));

        // 破坏石头生成圆石掉落物，破坏冰块，破坏金矿石生成多个粗金掉落物，不破坏基岩
        builder.world().hideIndependentSection(blockRowLinks[0], Direction.UP);
        builder.world().hideIndependentSection(blockRowLinks[1], Direction.UP);
        builder.world().hideIndependentSection(blockRowLinks[2], Direction.UP);

        builder.world().setBlock(blockRowPositions[0], Blocks.AIR.defaultBlockState(), true);
        builder.world().setBlock(blockRowPositions[1], Blocks.AIR.defaultBlockState(), true);
        builder.world().setBlock(blockRowPositions[2], Blocks.AIR.defaultBlockState(), true);
        
        itemLinks4[0] = builder.world().createItemEntity(util.vector().of(13, 1, 15), util.vector().of(0, 0.1, 0), 
            new net.minecraft.world.item.ItemStack(Blocks.COBBLESTONE));
        itemLinks4[1] = builder.world().createItemEntity(util.vector().of(13, 1, 16), util.vector().of(0, 0.1, 0), 
            new net.minecraft.world.item.ItemStack(Items.RAW_GOLD, 8)); // 时运效果，更多粗金

        builder.idle(40);

        // 大铁砧上升回初始位置
        builder.world().moveSection(giantAnvilLink, new Vec3(0, 6, 0), 8);

        builder.idle(10);

        // 清除掉落物
        for (ElementLink<EntityElement> link : itemLinks4) {
            builder.world().removeEntity(link);
        }

        builder.idle(10);

        // 重质铁块四面超限铁砧放置成幻灵铁砧
        for (int i = 0; i < anvilPositions.length; i++) {
            builder.world().setBlock(anvilPositions[i], ModBlocks.SPECTRAL_ANVIL.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, anvilDirections[i]), true);
        }

        builder.idle(10);

        // 在旁边排成一行放置石头，冰块，金矿石
        ElementLink<EntityElement>[] itemLinks5 = new ElementLink[2]; // 添加物品链接数组
        for (int i = 0; i < blockRowPositions.length; i++) {
            builder.world().setBlock(blockRowPositions[i], blocks[i].defaultBlockState(), false);
            blockRowLinks[i] = builder.world().showIndependentSection(util.select().position(blockRowPositions[i]), Direction.DOWN);
        }

        // 选择框，文本：幻灵铁砧在撼地的底座中，就相当于普通铁砧
        builder.overlay()
            .showOutlineWithText(util.select().fromTo(13, 1, 15, 13, 1, 17), 65)
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(13, 1, 15), Direction.WEST))
            .text("In Shock’s base, a spectral anvil functions as a regular anvil.");

        builder.idle(40);

        // 大铁砧下落
        builder.world().moveSectionInterpolation(giantAnvilLink, new Vec3(0, -6, 0), Interpolation.acceleration(0.05));

        // 破坏石头生成圆石掉落物，破坏冰块，破坏金矿石生成粗金掉落物，不破坏基岩
        builder.world().hideIndependentSection(blockRowLinks[0], Direction.UP);
        builder.world().hideIndependentSection(blockRowLinks[1], Direction.UP);
        builder.world().hideIndependentSection(blockRowLinks[2], Direction.UP);
        // 破坏方块以产生粒子效果
        builder.world().setBlock(blockRowPositions[0], Blocks.AIR.defaultBlockState(), true);
        builder.world().setBlock(blockRowPositions[1], Blocks.AIR.defaultBlockState(), true);
        builder.world().setBlock(blockRowPositions[2], Blocks.AIR.defaultBlockState(), true);
        
        itemLinks5[0] = builder.world().createItemEntity(util.vector().of(13, 1, 15), util.vector().of(0, 0.1, 0), 
            new net.minecraft.world.item.ItemStack(Blocks.COBBLESTONE));
        itemLinks5[1] = builder.world().createItemEntity(util.vector().of(13, 1, 16), util.vector().of(0, 0.1, 0), 
            new net.minecraft.world.item.ItemStack(Items.RAW_GOLD, 1));

        builder.idle(40);

        // 大铁砧上升回初始位置
        builder.world().moveSection(giantAnvilLink, new Vec3(0, 6, 0), 8);

        builder.idle(10);

        // 清除掉落物
        for (ElementLink<EntityElement> link : itemLinks5) {
            builder.world().removeEntity(link);
        }

        builder.idle(10);

        // 四角黑曜石放置成草方块
        for (BlockPos pos : cornerPositions) {
            builder.world().setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), true);
        }

        builder.idle(10);

        // 在旁边地面排成一行放置草方块、菌丝、沙子、耕地，并在上方依次放置虞美人、红蘑菇、枯萎的灌木、成熟的小麦
        // 地面方块
        BlockPos[] groundBlocks = {
            new BlockPos(13, 0, 14),
            new BlockPos(13, 0, 15),
            new BlockPos(13, 0, 16),
            new BlockPos(13, 0, 17)
        };
        net.minecraft.world.level.block.Block[] groundBlockTypes = {
            Blocks.GRASS_BLOCK,
            Blocks.MYCELIUM,
            Blocks.SAND,
            Blocks.FARMLAND
        };
        
        // 上方的植物
        net.minecraft.world.level.block.Block[] plantBlocks = {
            Blocks.POPPY,
            Blocks.RED_MUSHROOM,
            Blocks.DEAD_BUSH,
            Blocks.WHEAT
        };

        ElementLink<WorldSectionElement>[] groundBlockLinks = new ElementLink[groundBlocks.length];
        ElementLink<WorldSectionElement>[] plantBlockLinks = new ElementLink[plantBlocks.length];
        ElementLink<EntityElement>[] itemLinks6 = new ElementLink[4];
        
        for (int i = 0; i < groundBlocks.length; i++) {
            // 放置地面方块
            builder.world().setBlock(groundBlocks[i], groundBlockTypes[i].defaultBlockState(), false);
            groundBlockLinks[i] = builder.world().showIndependentSection(util.select().position(groundBlocks[i]), Direction.UP);

            // 放置植物
            BlockPos plantPos = groundBlocks[i].above();
            net.minecraft.world.level.block.state.BlockState plantState = plantBlocks[i].defaultBlockState();
            if (plantBlocks[i] == Blocks.WHEAT) {
                plantState = plantState.setValue(net.minecraft.world.level.block.CropBlock.AGE, 7);
            } else if (plantBlocks[i] == Blocks.RED_MUSHROOM || plantBlocks[i] == Blocks.DEAD_BUSH) {
                // ↑懒得喷
            }
            builder.world().setBlock(plantPos, plantState, false);
            plantBlockLinks[i] = builder.world().showIndependentSection(util.select().position(plantPos), Direction.UP);
        }

            // 选择框，文本：四角为草方块时，撼地会破坏花草、雪片等附着物
        builder.overlay()
            .showOutlineWithText(util.select().fromTo(13, 0, 14, 13, 1, 17), 65)
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(13, 1, 14), Direction.WEST))
            .text("When the corners are grass blocks, Shock breaks flowers, snow layers, and other attachments.");

        builder.idle(40);

        // 大铁砧下落
        builder.world().moveSectionInterpolation(giantAnvilLink, new Vec3(0, -6, 0), Interpolation.acceleration(0.05));

        // 破坏虞美人生成虞美人掉落物，破坏红蘑菇生成红蘑菇掉落物，破坏枯萎的灌木生成木棍掉落物，破坏成熟的小麦生成小麦掉落物
        for (int i = 0; i < plantBlockLinks.length; i++) {
            builder.world().setBlock(groundBlocks[i].above(), Blocks.AIR.defaultBlockState(), true);
        }
        itemLinks6[0] = builder.world().createItemEntity(util.vector().of(13, 1, 14), util.vector().of(0, 0.1, 0), 
            new net.minecraft.world.item.ItemStack(Blocks.POPPY));
        itemLinks6[1] = builder.world().createItemEntity(util.vector().of(13, 1, 15), util.vector().of(0, 0.1, 0), 
            new net.minecraft.world.item.ItemStack(Blocks.RED_MUSHROOM));
        itemLinks6[2] = builder.world().createItemEntity(util.vector().of(13, 1, 16), util.vector().of(0, 0.1, 0), 
            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
        itemLinks6[3] = builder.world().createItemEntity(util.vector().of(13, 1, 17), util.vector().of(0, 0.1, 0), 
            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WHEAT));

        builder.idle(40);

        // 大铁砧上升回初始位置
        builder.world().moveSection(giantAnvilLink, new Vec3(0, 6, 0), 8);

        builder.idle(10);

        // 清除掉落物
        for (ElementLink<EntityElement> link : itemLinks6) {
            builder.world().removeEntity(link);
        }

        // 白色钢筋混凝土位置1
        BlockPos[] whiteConcretePositions1 = {
            new BlockPos(13, 0, 14),
            new BlockPos(13, 0, 16)
        };

        // 淡灰色钢筋混凝土位置1
        BlockPos[] lightGrayConcretePositions1 = {
            new BlockPos(13, 0, 15),
            new BlockPos(13, 0, 17)
        };

        // 放置白色钢筋混凝土
        for (BlockPos pos : whiteConcretePositions1) {
            builder.world().setBlock(pos, ModBlocks.REINFORCED_CONCRETES.get(Color.WHITE).getDefaultState(), false);
            builder.world().showIndependentSection(util.select().position(pos), Direction.UP);
        }

        // 放置淡灰色钢筋混凝土
        for (BlockPos pos : lightGrayConcretePositions1) {
            builder.world().setBlock(pos, ModBlocks.REINFORCED_CONCRETES.get(Color.LIGHT_GRAY).getDefaultState(), false);
            builder.world().showIndependentSection(util.select().position(pos), Direction.UP);
        }

        builder.idle(10);

        // 文本指向幻灵铁砧：不论选择何种破坏目标，破坏附带的效果都会因底座铁砧的不同而改变
        builder.overlay()
            .showText(55)
            .text("Regardless of the chosen target, the breaking effect changes with the base anvils.")
            .pointAt(util.vector().blockSurface(util.grid().at(15, 1, 16), Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();

        builder.idle(60);

        // 四角草方块放置成干草块
        for (BlockPos pos : cornerPositions) {
            builder.world().setBlock(pos, Blocks.HAY_BLOCK.defaultBlockState(), true);
        }

        builder.idle(10);

        // 在旁边排成一行放置耕地泥土
        net.minecraft.world.level.block.Block[] farmBlocks = {
            Blocks.FARMLAND,
            Blocks.FARMLAND,
            Blocks.FARMLAND,
            Blocks.DIRT
        };
        
        // 上方的作物成熟的小麦、半熟的小麦、南瓜秧、南瓜
        net.minecraft.world.level.block.Block[] cropBlocks = {
            Blocks.WHEAT,
            Blocks.WHEAT,
            Blocks.PUMPKIN_STEM,
            Blocks.PUMPKIN
        };

        // 作物状态
        int[] cropAges = {7, 3, 7, 0}; // 成熟小麦、半熟小麦、成熟南瓜茎、南瓜

        for (int i = 0; i < groundBlocks.length; i++) {
            // 放置地面方块
            builder.world().setBlock(groundBlocks[i], farmBlocks[i].defaultBlockState(), false);
            
            // 放置作物
            BlockPos cropPos = groundBlocks[i].above();
            if (cropBlocks[i] == Blocks.PUMPKIN_STEM) {
                net.minecraft.world.level.block.state.BlockState pumpkinStemState = cropBlocks[i].defaultBlockState()
                    .setValue(net.minecraft.world.level.block.CropBlock.AGE, cropAges[i]);
                builder.world().setBlock(cropPos, pumpkinStemState, false);
            } else {
                net.minecraft.world.level.block.state.BlockState cropState = cropBlocks[i].defaultBlockState();
                if (cropBlocks[i] == Blocks.WHEAT) {
                    cropState = cropState.setValue(net.minecraft.world.level.block.CropBlock.AGE, cropAges[i]);
                }
                builder.world().setBlock(cropPos, cropState, false);
            }
        }

        // 在另一边竖着放置3块丛林原木，每块原木的西面放置成熟的可可豆
        BlockPos[] jungleLogPositions = {
            new BlockPos(19, 1, 17),
            new BlockPos(19, 2, 17),
            new BlockPos(19, 3, 17)
        };
        
        ElementLink<WorldSectionElement>[] jungleLogLinks = new ElementLink[jungleLogPositions.length];
        ElementLink<WorldSectionElement>[] cocoaLinks = new ElementLink[jungleLogPositions.length];
        ElementLink<EntityElement>[] itemLinks7 = new ElementLink[5];
        
        for (int i = 0; i < jungleLogPositions.length; i++) {
            builder.world().setBlock(jungleLogPositions[i], Blocks.JUNGLE_LOG.defaultBlockState(), false);
            jungleLogLinks[i] = builder.world().showIndependentSection(util.select().position(jungleLogPositions[i]), Direction.UP);

            BlockPos cocoaPos = jungleLogPositions[i].west();
            builder.world().setBlock(cocoaPos, Blocks.COCOA.defaultBlockState()
                .setValue(net.minecraft.world.level.block.CocoaBlock.AGE, 2)
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.EAST), false);
            cocoaLinks[i] = builder.world().showIndependentSection(util.select().position(cocoaPos), Direction.UP);
        }

        // 另一边放置2灵魂沙2草方块，灵魂沙上成熟的地狱疣，草方块上成熟的浆果丛
        BlockPos[] netherFarmBlocks = {
            new BlockPos(19, 0, 13),
            new BlockPos(19, 0, 14),
            new BlockPos(19, 0, 15),
            new BlockPos(19, 0, 16)
        };
        net.minecraft.world.level.block.Block[] netherGroundBlocks = {
            Blocks.SOUL_SAND,
            Blocks.SOUL_SAND,
            Blocks.GRASS_BLOCK,
            Blocks.GRASS_BLOCK
        };

        ElementLink<WorldSectionElement>[] netherPlantLinks = new ElementLink[netherFarmBlocks.length];
        
        for (int i = 0; i < netherFarmBlocks.length; i++) {
            builder.world().setBlock(netherFarmBlocks[i], netherGroundBlocks[i].defaultBlockState(), false);
            
            // 放置植物
            BlockPos plantPos = netherFarmBlocks[i].above();
            net.minecraft.world.level.block.Block plantBlock = (i < 2) ? Blocks.NETHER_WART : Blocks.SWEET_BERRY_BUSH;
            builder.world().setBlock(plantPos, plantBlock.defaultBlockState()
                .setValue(plantBlock == Blocks.NETHER_WART
                          ?
                    net.minecraft.world.level.block.NetherWartBlock.AGE :
                    net.minecraft.world.level.block.SweetBerryBushBlock.AGE, 3), false);
            netherPlantLinks[i] = builder.world().showIndependentSection(util.select().position(plantPos), Direction.UP);
        }

        // 选择框，文本：四角为干草块时，撼地会破坏各种农作物并自动补种
        builder.overlay()
            .showOutlineWithText(util.select().fromTo(13, 0, 14, 13, 1, 17)
                .add(util.select().fromTo(19, 1, 17, 19, 3, 17))
                .add(util.select().fromTo(19, 0, 13, 19, 1, 16)), 65)
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(13, 1, 14), Direction.WEST))
            .text("Hay blocks at the corners make Shock harvest crops and replant them automatically.");

        builder.idle(40);

        // 大铁砧下落
        builder.world().moveSectionInterpolation(giantAnvilLink, new Vec3(0, -6, 0), Interpolation.acceleration(0.05));

        // 小麦(成熟) -> 小麦(刚种下)
        builder.world().setBlock(groundBlocks[0].above(), Blocks.WHEAT.defaultBlockState()
            .setValue(net.minecraft.world.level.block.CropBlock.AGE, 0), false);
        itemLinks7[0] = builder.world().createItemEntity(util.vector().of(13, 1, 14), util.vector().of(0, 0.1, 0), 
            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WHEAT));
        
        // 半熟的小麦和南瓜秧不变

        // 破坏南瓜
        builder.world().setBlock(groundBlocks[3].above(), Blocks.AIR.defaultBlockState(), true);
        itemLinks7[1] = builder.world().createItemEntity(util.vector().of(13, 1, 17), util.vector().of(0, 0.1, 0), 
            new net.minecraft.world.item.ItemStack(Blocks.PUMPKIN));
        
        // 可可豆全部变为刚种下的可可豆且掉落可可豆掉落物
        for (int i = 0; i < cocoaLinks.length; i++) {
            builder.world().setBlock(jungleLogPositions[i].west(), Blocks.COCOA.defaultBlockState()
                .setValue(net.minecraft.world.level.block.CocoaBlock.AGE, 0)
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.EAST), true);
            itemLinks7[2] = builder.world().createItemEntity(util.vector().of(19, 1, 16), util.vector().of(0, 0.1, 0),
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COCOA_BEANS));
        }
        
        // 地狱疣全部变为刚种下的地狱疣且掉落地狱疣掉落物，浆果丛变为刚种下的浆果丛且掉落浆果掉落物
        for (int i = 0; i < netherPlantLinks.length; i++) {
            builder.world().setBlock(netherFarmBlocks[i].above(), Blocks.AIR.defaultBlockState(), true);
            if (i < 2) {
                // 地狱疣
                builder.world().setBlock(netherFarmBlocks[i].above(), Blocks.NETHER_WART.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.NetherWartBlock.AGE, 0), false);
                itemLinks7[3] = builder.world().createItemEntity(util.vector().of(19, 1, 13 + i), util.vector().of(0, 0.1, 0),
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHER_WART));
            } else {
                // 浆果丛
                builder.world().setBlock(netherFarmBlocks[i].above(), Blocks.SWEET_BERRY_BUSH.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.SweetBerryBushBlock.AGE, 0), false);
                itemLinks7[4] = builder.world().createItemEntity(util.vector().of(19, 1, 13 + i), util.vector().of(0, 0.1, 0),
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SWEET_BERRIES));
            }
        }

        builder.idle(40);

        // 大铁砧上升回初始位置
        builder.world().moveSection(giantAnvilLink, new Vec3(0, 6, 0), 8);

        builder.idle(10);

        // 清除掉落物
        for (ElementLink<EntityElement> link : itemLinks7) {
                builder.world().removeEntity(link);
        }

        // 移走丛林原木
        for (ElementLink<WorldSectionElement> jungleLogLink : jungleLogLinks) {
            builder.world().hideIndependentSection(jungleLogLink, Direction.UP);
        }
        
        // 移走可可豆
        for (ElementLink<WorldSectionElement> cocoaLink : cocoaLinks) {
            builder.world().hideIndependentSection(cocoaLink, Direction.UP);
        }
        
        // 移走植物
        for (ElementLink<WorldSectionElement> plantBlockLink : plantBlockLinks) {
            builder.world().hideIndependentSection(plantBlockLink, Direction.UP);
        }
        
        // 移走地狱疣和浆果丛
        for (ElementLink<WorldSectionElement> netherPlantLink : netherPlantLinks) {
            builder.world().hideIndependentSection(netherPlantLink, Direction.UP);
        }

        // 白色钢筋混凝土位置2
        BlockPos[] whiteConcretePositions2 = {
            new BlockPos(13, 0, 14),
            new BlockPos(13, 0, 16),
            new BlockPos(19, 0, 14),
            new BlockPos(19, 0, 16)
        };
        
        // 淡灰色钢筋混凝土位置2
        BlockPos[] lightGrayConcretePositions2 = {
            new BlockPos(13, 0, 15),
            new BlockPos(13, 0, 17),
            new BlockPos(19, 0, 13),
            new BlockPos(19, 0, 15)
        };
        
        // 放置白色钢筋混凝土
        for (BlockPos pos : whiteConcretePositions2) {
            builder.world().setBlock(pos, ModBlocks.REINFORCED_CONCRETES.get(Color.WHITE).getDefaultState(), false);
            builder.world().showIndependentSection(util.select().position(pos), Direction.UP);
        }
        
        // 放置淡灰色钢筋混凝土
        for (BlockPos pos : lightGrayConcretePositions2) {
            builder.world().setBlock(pos, ModBlocks.REINFORCED_CONCRETES.get(Color.LIGHT_GRAY).getDefaultState(), false);
            builder.world().showIndependentSection(util.select().position(pos), Direction.UP);
        }

        builder.idle(10);

        // 四角干草块放置成原木
        for (BlockPos pos : cornerPositions) {
            builder.world().setBlock(pos, Blocks.OAK_LOG.defaultBlockState(), false);
        }

        builder.idle(10);

        // 在旁边放置一棵树
        BlockPos[] treeLogPositions = {
            new BlockPos(13, 1, 15),
            new BlockPos(13, 2, 15),
            new BlockPos(13, 3, 15),
            new BlockPos(13, 4, 15),
            new BlockPos(13, 5, 15)
        };
        BlockPos[] treeLeafPositions = {
            new BlockPos(12, 4, 15),
            new BlockPos(12, 5, 15),
            new BlockPos(13, 6, 15),
            new BlockPos(14, 4, 15),
            new BlockPos(14, 5, 15)
        };
        
        ElementLink<WorldSectionElement>[] treeLogLinks = new ElementLink[treeLogPositions.length];
        ElementLink<WorldSectionElement>[] treeLeafLinks = new ElementLink[treeLeafPositions.length];
        
        for (int i = 0; i < treeLogPositions.length; i++) {
            builder.world().setBlock(treeLogPositions[i], Blocks.OAK_LOG.defaultBlockState(), false);
            treeLogLinks[i] = builder.world().showIndependentSection(util.select().position(treeLogPositions[i]), Direction.UP);
        }
        
        for (int i = 0; i < treeLeafPositions.length; i++) {
            builder.world().setBlock(treeLeafPositions[i], Blocks.OAK_LEAVES.defaultBlockState(), false);
            treeLeafLinks[i] = builder.world().showIndependentSection(util.select().position(treeLeafPositions[i]), Direction.UP);
        }

        // 另一边放水和甘蔗
        BlockPos[] waterPositions = {
            new BlockPos(15, 0, 13),
            new BlockPos(16, 0, 13),
            new BlockPos(17, 0, 13)
        };
        ElementLink<WorldSectionElement>[] waterLinks = new ElementLink[waterPositions.length];
        
        for (int i = 0; i < waterPositions.length; i++) {
            builder.world().setBlock(waterPositions[i], Blocks.WATER.defaultBlockState(), false);
            waterLinks[i] = builder.world().showIndependentSection(util.select().position(waterPositions[i]), Direction.UP);
        }
        
        // 放置沙子和甘蔗
        builder.world().setBlock(new BlockPos(14, 0, 13), Blocks.SAND.defaultBlockState(), false);
        builder.world().showIndependentSection(util.select().position(new BlockPos(14, 0, 13)), Direction.UP);

        BlockPos[] sugarcanePositions = {
            new BlockPos(14, 1, 13),
            new BlockPos(14, 2, 13),
            new BlockPos(14, 3, 13),
            new BlockPos(18, 0, 13),
            new BlockPos(18, 1, 13),
            new BlockPos(18, 2, 13)
        };
        ElementLink<WorldSectionElement>[] sugarcaneLinks = new ElementLink[sugarcanePositions.length];
        for (int i = 0; i < sugarcanePositions.length; i++) {
            builder.world().setBlock(sugarcanePositions[i], Blocks.SUGAR_CANE.defaultBlockState(), false);
            sugarcaneLinks[i] = builder.world().showIndependentSection(util.select().position(sugarcanePositions[i]), Direction.UP);
        }

        // 选择框，文本：四角为原木时，撼地会破坏整棵树，以及菌柄、仙人掌、甘蔗、紫颂植株等大型植物
        builder.overlay()
            .showOutlineWithText(util.select().fromTo(14, 1, 15, 12, 6, 15)
                .add(util.select().fromTo(14, 1, 13, 14, 3, 13))
                .add(util.select().fromTo(18, 1, 13, 18, 2, 13)), 65)
            .attachKeyFrame()
            .placeNearTarget()
            .pointAt(util.vector().blockSurface(util.grid().at(12, 1, 13), Direction.WEST))
            .text("When the corners are logs, Shock breaks entire trees, as well as huge fungi stems, cacti, sugar canes, chorus plants, and other large plants.");

        builder.idle(40);

        // 大铁砧下落
        builder.world().moveSectionInterpolation(giantAnvilLink, new Vec3(0, -6, 0), Interpolation.acceleration(0.05));

        // 破坏整棵树
        for (int i = 0; i < treeLogLinks.length; i++) {
            builder.world().setBlock(treeLogPositions[i], Blocks.AIR.defaultBlockState(), true);
        }
        for (int i = 0; i < treeLeafLinks.length; i++) {
            builder.world().setBlock(treeLeafPositions[i], Blocks.AIR.defaultBlockState(), true);
        }
        
        // 破坏甘蔗
        for (BlockPos sugarcanePosition : sugarcanePositions) {
            if (sugarcanePosition.getY() >= 1) {
                builder.world().setBlock(sugarcanePosition, Blocks.AIR.defaultBlockState(), true);
            }
        }

        builder.idle(30);

        // 文本指向甘蔗：对于持续生长的作物，种植在撼地平面以下以防它们被连根拔起
        builder.overlay()
            .showText(55)
            .text("For continuously growing crops, plant them one block below Shock’s plane to prevent being uprooted.")
            .pointAt(util.vector().blockSurface(util.grid().at(18, 0, 13), Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();

        builder.idle(60);

        // 标记场景结束
        builder.markAsFinished();
    }

    private static ElementLink<EntityElement> spawnMushroomCow(SceneBuildingUtil util, AnvilCraftSceneBuilder builder) {
        return builder.world().createEntity(w -> {
            MushroomCow entity = EntityType.MOOSHROOM.create(w);
            if (entity == null) return null;
            Vec3 p = util.vector().of(17.5, 1.0, 12.5);
            entity.setPos(p);
            entity.xo = p.x;
            entity.yo = p.y;
            entity.zo = p.z;
            WalkAnimationState animation = entity.walkAnimation;
            animation.update(-animation.position(), 1);
            animation.setSpeed(1);
            entity.yRotO = 210;
            entity.setYRot(210);
            entity.yHeadRotO = 210;
            entity.yHeadRot = 210;
            return entity;
        });
    }

    private static ElementLink<EntityElement> spawnZombie(SceneBuildingUtil util, AnvilCraftSceneBuilder builder) {
        return builder.world().createEntity(w -> {
            Zombie entity = EntityType.ZOMBIE.create(w);
            if (entity == null) return null;
            Vec3 p = util.vector().of(17.5, 1.0, 12.5);
            entity.setPos(p);
            entity.xo = p.x;
            entity.yo = p.y;
            entity.zo = p.z;
            //  装备钻石盔甲
            entity.setItemSlot(
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.item.Items.DIAMOND_HELMET.getDefaultInstance()
            );
            entity.setItemSlot(
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.item.Items.DIAMOND_CHESTPLATE.getDefaultInstance()
            );
            entity.setItemSlot(
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.item.Items.DIAMOND_LEGGINGS.getDefaultInstance()
            );
            entity.setItemSlot(
                net.minecraft.world.entity.EquipmentSlot.FEET,
                net.minecraft.world.item.Items.DIAMOND_BOOTS.getDefaultInstance()
            );
            return entity;
        });
    }
}