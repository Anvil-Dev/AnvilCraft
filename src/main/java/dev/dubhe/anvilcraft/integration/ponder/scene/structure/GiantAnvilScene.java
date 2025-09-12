package dev.dubhe.anvilcraft.integration.ponder.scene.structure;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
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
import net.minecraft.world.entity.Entity;
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
            .addStoryBoard("platform/33x", GiantAnvilScene::shockMode, AnvilCraftPonderTags.GIANT_ANVIL);
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
            .text("The shockwave harms any creature not wearing shoes.")
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
        ElementLink<EntityElement> mushroomCow = builder.world().createEntity(w -> {
            MushroomCow entity = EntityType.MOOSHROOM.create(w);
            if (entity == null) return null;
            Vec3 p = util.vector().of(17.5, 1.0, 10.5);
            entity.setPos(p.x, p.y, p.z);
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
            .text("Additionally, shock damage increases with the FallDistance of Giant Anvil.")
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
        builder.title("mode_of_shock", "Blocks around the Heavy Iron Block determines Shock mode.");
        // 地板33x33
        builder.configureBasePlate(0, 0, 33);
        // 摄像机缩放0.6f
        builder.scaleSceneView(0.6f);
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
            .text("Changing the blocks surrounding the Heavy Iron Block can alter the Shock mode.");
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
            .text("For example, resin block can make anvils within the shock range bounce upward once.")
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
            .text("Placing Cursed Gold Blocks on the four sides can add equal additional damage to the shock damage.")
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
            .text("Placing different gem blocks at the four corners can add various types of damage.")
            .pointAt(util.vector().blockSurface(util.grid().at(15, 0, 15), Direction.NORTH))
            .attachKeyFrame()
            .placeNearTarget();
        // 延时60gt
        builder.idle(60);
        // 巨型铁砧下落10m
        builder.world().moveSectionInterpolation(giantAnvilLink, new Vec3(0, -10, 0), Interpolation.acceleration(0.05));
        // XeKr着火冒烟红温倒地
        builder.world().letLivingEntityDie(mushroomCow, util.grid().at(17, 1, 12), true, entity -> entity.setRemainingFireTicks(10));
        // 生成熟牛排物品
        builder.world().createItemEntity(util.vector().of(17, 1, 12), util.vector().of(0, 0.1, 0), Items.COOKED_BEEF.getDefaultInstance());
        // 文本：例如红宝石块可以附加火焰伤害
        builder.overlay()
            .showText(55)
            .text("For example, Ruby Blocks can add fire damage.")
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
            .text("Sapphire Blocks can add freezing damage.")
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
            .text("Topaz Blocks can add lightning damage.")
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
            .text("Void Matter Blocks can add armor-piercing void damage.")
            .pointAt(util.vector().blockSurface(util.grid().at(15, 0, 15), Direction.NORTH))
            .attachKeyFrame()
            .placeNearTarget();
        // 生成钻石套僵尸
        ElementLink<EntityElement> zombie = builder.world().createEntity(w -> {
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
        // 延时30gt
        builder.idle(30);
        // 铁砧下落10m
        builder.world().moveSectionInterpolation(giantAnvilLink, new Vec3(0, -10, 0), Interpolation.acceleration(0.05));
        // 僵尸冒烟红温倒地
        builder.world().letLivingEntityDie(zombie, util.grid().at(17, 1, 12), true);
        // 延时40gt
        builder.idle(40);
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
}
