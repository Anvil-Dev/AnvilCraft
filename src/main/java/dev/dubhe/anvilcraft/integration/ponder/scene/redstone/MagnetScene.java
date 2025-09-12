package dev.dubhe.anvilcraft.integration.ponder.scene.redstone;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.dubhe.anvilcraft.block.MagnetBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.ponder.AnvilCraftPonderTags;
import dev.dubhe.anvilcraft.integration.ponder.api.AnvilCraftSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.phys.Vec3;


public class MagnetScene {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> helper = registrationHelper.withKeyFunction(RegistryEntry::getId);
        helper.forComponents(
                ModBlocks.MAGNET_BLOCK,
                ModBlocks.HOLLOW_MAGNET_BLOCK,
                ModBlocks.FERRITE_CORE_MAGNET_BLOCK
            )
            .addStoryBoard("platform/5x", MagnetScene::thunder, AnvilCraftPonderTags.MAGNET_BLOCK)
            .addStoryBoard("platform/5x", MagnetScene::magnetizeIngot, AnvilCraftPonderTags.MAGNET_BLOCK)
            .addStoryBoard("platform/5x", MagnetScene::attractAnvil, AnvilCraftPonderTags.MAGNET_BLOCK, AnvilCraftPonderTags.ANVIL)
            .addStoryBoard("platform/5x", MagnetScene::rubCopperBlock, AnvilCraftPonderTags.MAGNET_BLOCK);
    }

    private static void thunder(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("magnet_thunder", "Get hollow magnet block");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        // 放置铁块、避雷针
        Selection ironBlockArea = util.select().fromTo(1, 1, 1, 3, 2, 3);
        scene.world().setBlocks(ironBlockArea, Blocks.IRON_BLOCK.defaultBlockState(), true);
        scene.world().showIndependentSection(ironBlockArea, Direction.UP);

        BlockPos lightningRodPos = util.grid().at(2, 3, 2);
        scene.world().setBlock(lightningRodPos, Blocks.LIGHTNING_ROD.defaultBlockState(), false);
        scene.world().showIndependentSection(util.select().position(lightningRodPos), Direction.UP);
        scene.idle(20);

        // 生成闪电
        Vec3 lightningPos = util.vector().centerOf(lightningRodPos.above());
        scene.world().createEntity(world -> {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(world);
            if (bolt != null) {
                bolt.moveTo(lightningPos);
                bolt.setVisualOnly(true);
            }
            return bolt;
        });
        // 磁铁替换铁块
        scene.world().setBlocks(ironBlockArea, ModBlocks.HOLLOW_MAGNET_BLOCK.getDefaultState(), true);

        // 避雷针放在结构正上方中央
        BlockPos textPos = util.grid().at(2, 2, 2);
        scene.overlay()
            .showText(60)
            .text("The magnets were produced.")
            .pointAt(util.vector().topOf(textPos))
            .attachKeyFrame()
            .placeNearTarget();
        // 等待文本显示完毕
        scene.idle(30);
        // 标记场景结束
        scene.markAsFinished();
    }

    private static void magnetizeIngot(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("magnet_magnetize_ingot", "Get a magnet ingot through a hollow magnet block");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        // 放置空心磁铁矿
        BlockPos magnetPos = util.grid().at(2, 1, 2);
        scene.world().setBlock(magnetPos, ModBlocks.HOLLOW_MAGNET_BLOCK.getDefaultState(), false);
        scene.world().showSection(util.select().position(magnetPos), Direction.DOWN);
        scene.idle(20);

        // 填充铁锭
        scene.overlay()
            .showControls(util.vector().centerOf(magnetPos), Pointing.RIGHT, 20)
            .rightClick()
            .withItem(Items.IRON_INGOT.getDefaultInstance());
        scene.idle(20);
        scene.world().setBlock(magnetPos, ModBlocks.FERRITE_CORE_MAGNET_BLOCK.getDefaultState(), true);
        scene.idle(10);

        // 等待时间以变化
        scene.overlay().showText(40).text("Some time later.").pointAt(util.vector().centerOf(magnetPos)).attachKeyFrame().placeNearTarget();
        scene.idle(50);
        scene.world().setBlock(magnetPos, ModBlocks.MAGNET_BLOCK.getDefaultState(), true);
        scene.idle(20);

        // 取出磁铁
        scene.overlay().showControls(util.vector().centerOf(magnetPos), Pointing.RIGHT, 20).rightClick().whileSneaking();

        scene.idle(20);
        scene.world().setBlock(magnetPos, ModBlocks.HOLLOW_MAGNET_BLOCK.getDefaultState(), false);
        scene.overlay()
            .showText(40)
            .text("Took out a magnet ingot.")
            .pointAt(util.vector().centerOf(magnetPos))
            .attachKeyFrame()
            .placeNearTarget();
        scene.idle(40);

        scene.markAsFinished(); // 标记场景结束
    }

    private static void attractAnvil(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("magnet", "Use magnet to attract the anvil");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();
        // 创建锅
        BlockPos cauldronPos = util.grid().at(2, 1, 2);
        builder.world().setBlock(cauldronPos, Blocks.CAULDRON.defaultBlockState(), false);
        builder.world().showSection(util.select().position(cauldronPos), Direction.NORTH);
        builder.idle(5);

        // 创建铁砧
        BlockPos anvilPos = cauldronPos.above();
        builder.world().setBlock(anvilPos, Blocks.ANVIL.defaultBlockState(), false);
        ElementLink<WorldSectionElement> anvilLink = builder.world()
            .showIndependentSection(util.select().position(anvilPos), Direction.NORTH);
        builder.idle(10);

        builder.overlay()
            .showText(40)
            .text("The anvil needs to be lifted and fallen for processing")
            .pointAt(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        // 创建磁铁
        BlockPos magnetPos = anvilPos.above(2);
        builder.world().setBlock(magnetPos, ModBlocks.MAGNET_BLOCK.getDefaultState(), false);
        builder.world().showIndependentSection(util.select().position(magnetPos), Direction.WEST);
        builder.idle(10);

        builder.world().riseSection(anvilLink);

        builder.overlay()
            .showText(40)
            .text("Magnets can attract anvils")
            .pointAt(magnetPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        // 放置红石块
        BlockPos redstonePos = magnetPos.west();
        builder.world().setBlock(redstonePos, Blocks.REDSTONE_BLOCK.defaultBlockState(), false);
        builder.world().showIndependentSection(util.select().position(redstonePos), Direction.WEST);
        builder.idle(10);
        // 磁铁失效
        builder.world().modifyBlock(magnetPos, bs -> bs.setValue(MagnetBlock.LIT, true), false);
        builder.world().falldownSection(anvilLink);

        builder.overlay()
            .showText(40)
            .text("Magnet will stop working when it receives a redstone signal.")
            .pointAt(util.vector().blockSurface(util.grid().at(2, 4, 2), Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        builder.markAsFinished();
    }

    private static void rubCopperBlock(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("magnet_power_generation", "Generate electricity by rubbing a magnet and a copper block");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        // 1. 出现铜块和磁铁
        BlockPos copperBlockPos = new BlockPos(1, 1, 2);
        BlockPos magnetPos = new BlockPos(2, 1, 1);
        scene.world().setBlock(copperBlockPos, Blocks.COPPER_BLOCK.defaultBlockState(), false);
        scene.world().setBlock(magnetPos, ModBlocks.MAGNET_BLOCK.getDefaultState(), false);
        scene.world().showSection(util.select().position(copperBlockPos), Direction.DOWN);
        ElementLink<WorldSectionElement> magnetElement = scene.world()
            .showIndependentSection(util.select().position(magnetPos), Direction.DOWN);
        scene.world().moveSection(magnetElement, new Vec3(0, 0, 1), 5);
        scene.idle(20);

        // 2. 出现活塞，推动磁铁
        BlockPos pistonPos = new BlockPos(2, 1, 3);
        scene.world().setBlock(pistonPos, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.NORTH), false);
        scene.world().showSection(util.select().position(pistonPos), Direction.DOWN);

        scene.overlay()
            .showText(60)
            .text("When a magnet is pushed against adjacent copper blocks, it generates electric charges.")
            .pointAt(util.vector().centerOf(pistonPos))
            .attachKeyFrame()
            .placeNearTarget();
        scene.idle(30);

        scene.world().moveSection(magnetElement, new Vec3(0, 0, -1), 5);
        scene.world().modifyBlock(pistonPos, state -> state.setValue(PistonBaseBlock.EXTENDED, true), false);

        BlockPos pistonHeadPos = new BlockPos(2, 1, 2);
        scene.world()
            .setBlock(pistonHeadPos, Blocks.PISTON_HEAD.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.NORTH), false);
        scene.world().showSection(util.select().position(pistonHeadPos), Direction.NORTH);
        scene.idle(30);

        // 3. 出现集电器
        BlockPos chargeCollectPos = new BlockPos(4, 1, 2);
        scene.world().setBlock(chargeCollectPos, ModBlocks.CHARGE_COLLECTOR.getDefaultState(), false);
        scene.world().showSection(util.select().position(chargeCollectPos), Direction.DOWN);
        scene.overlay()
            .showText(40)
            .text("Use a collector to absorb the electric charges on the magnet.")
            .pointAt(util.vector().centerOf(chargeCollectPos))
            .attachKeyFrame()
            .placeNearTarget();
        scene.idle(50);

        // 铜块生锈
        scene.world().setBlock(copperBlockPos, Blocks.OXIDIZED_COPPER.defaultBlockState(), false);
        scene.overlay()
            .showText(40)
            .text("Copper rust can affect the production of electric charges.")
            .pointAt(util.vector().centerOf(copperBlockPos))
            .attachKeyFrame()
            .placeNearTarget();
        scene.idle(40);

        scene.markAsFinished();
    }
}
