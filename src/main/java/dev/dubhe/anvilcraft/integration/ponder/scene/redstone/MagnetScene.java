package dev.dubhe.anvilcraft.integration.ponder.scene.redstone;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.dubhe.anvilcraft.block.MagnetBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.integration.ponder.AnvilCraftPonderTags;
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
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;


public class MagnetScene {
    public static void register(@NotNull PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> helper = registrationHelper.withKeyFunction(RegistryEntry::getId);
        helper.forComponents(
                ModBlocks.MAGNET_BLOCK,
                ModBlocks.HOLLOW_MAGNET_BLOCK,
                ModBlocks.FERRITE_CORE_MAGNET_BLOCK
            )
            .addStoryBoard(
                "platform/555",
                MagnetScene::thunder,
                AnvilCraftPonderTags.MAGNET_BLOCK)
            .addStoryBoard(
                "platform/555",
                MagnetScene::magnetizeIngot,
                AnvilCraftPonderTags.MAGNET_BLOCK
            ).addStoryBoard(
                "platform/555",
                MagnetScene::attractAnvil,
                AnvilCraftPonderTags.MAGNET_BLOCK,
                AnvilCraftPonderTags.ANVIL
            )
            .addStoryBoard(
                "platform/555",
                MagnetScene::rubCopperBlock,
                AnvilCraftPonderTags.MAGNET_BLOCK);
    }

    private static void thunder(@NotNull SceneBuilder scene, @NotNull SceneBuildingUtil util) {
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

        BlockPos textPos = util.grid().at(2, 2, 2); // 避雷针放在结构正上方中央
        scene.overlay().showText(60)
            .text("The magnets were produced.")
            .pointAt(util.vector().topOf(textPos))
            .attachKeyFrame()
            .placeNearTarget();

        scene.idle(30); // 等待文本显示完毕

        scene.markAsFinished(); // 标记场景结束
    }

    private static void magnetizeIngot(@NotNull SceneBuilder scene, @NotNull SceneBuildingUtil util) {
        scene.title("magnet_magnetize_ingot", "Get a magnet ingot through a hollow magnet block");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        // 放置空心磁铁矿
        BlockPos magnetPos = util.grid().at(2, 2, 2); // 将方块放在中心偏下的位置
        Selection magnetArea = util.select().position(magnetPos);
        scene.world().setBlock(magnetPos, ModBlocks.HOLLOW_MAGNET_BLOCK.getDefaultState(), false);
        scene.world().showIndependentSection(magnetArea, Direction.DOWN); // 从下往上显示
        scene.idle(15);

        // 循环创建4个铁锭
        Vec3 dropItemPos = new Vec3(2.5, 4, 2.5);
        Vec3 changeItemPos = new Vec3(2.5, 2.8, 2.5);
        ItemStack ironIngotItem = new ItemStack(Items.IRON_INGOT, 1);
        ItemStack magnetIngotItem = new ItemStack(ModItems.MAGNET_INGOT.asItem(), 1);
        ElementLink<EntityElement> ironBockItemLink = null;

        scene.overlay().showText(60)
            .text("A player throws a iron ingot at a time.")
            .pointAt(dropItemPos)
            .attachKeyFrame()
            .placeNearTarget();
        for (int i = 0; i < 3; i++) {
            ironBockItemLink = scene.world().createItemEntity(dropItemPos, Vec3.ZERO, ironIngotItem);
            scene.idle(20);
        }

        scene.overlay().showText(60)
            .text("Iron ingots have a probability of being magnetized.")
            .pointAt(changeItemPos)
            .attachKeyFrame()
            .placeNearTarget();
        scene.world().modifyEntity(ironBockItemLink, entity -> entity.setPos(2.5, -100, 2.5));
        scene.world().createItemEntity(changeItemPos, Vec3.ZERO, magnetIngotItem);
        scene.idle(30); // 等待最后一个铁锭穿过

        scene.markAsFinished(); // 标记场景结束
    }

    private static void attractAnvil(@NotNull SceneBuilder scene, @NotNull SceneBuildingUtil util) {
        scene.title("magnet", "Use magnet to attract the anvil");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        // 创建锅
        scene.world().setBlock(new BlockPos(2, 1, 2), Blocks.CAULDRON.defaultBlockState(), false);
        Selection cauldron = util.select().position(2, 1, 2);
        scene.world().showSection(cauldron, Direction.NORTH);
        // 创建铁砧
        scene.world().setBlock(new BlockPos(2, 2, 2), Blocks.ANVIL.defaultBlockState(), false);
        Selection anvil = util.select().position(2, 2, 2);
        ElementLink<WorldSectionElement> anvilLink = scene.world().showIndependentSection(anvil, Direction.NORTH);
        scene.idle(5);

        scene.overlay().showText(40)
            .text("The anvil needs to be lifted and fallen for processing")
            .pointAt(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();
        scene.idle(40);
        // 创建磁铁
        scene.world().setBlock(new BlockPos(2, 4, 2), ModBlocks.MAGNET_BLOCK.getDefaultState(), false);
        Selection magnet = util.select().position(2, 4, 2);
        scene.world().showIndependentSection(magnet, Direction.WEST);
        scene.idle(10);

        scene.world().moveSection(anvilLink, new Vec3(0, 1, 0), 4);
        scene.idle(5);

        scene.overlay().showText(40)
            .text("Magnets can attract anvils")
            .pointAt(util.vector().blockSurface(util.grid().at(2, 3, 2), Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();
        scene.idle(40);
        // 放置红石块使磁铁失效
        scene.world().setBlock(new BlockPos(3, 4, 2), Blocks.REDSTONE_BLOCK.defaultBlockState(), false);
        Selection redstoneBlock = util.select().position(3, 4, 2);
        scene.world().showIndependentSection(redstoneBlock, Direction.WEST);
        scene.idle(10);
        scene.world().modifyBlock(new BlockPos(2, 4, 2),
            bs -> bs.setValue(MagnetBlock.LIT, true),
            false
        );

        scene.world().moveSection(anvilLink, new Vec3(0, -1, 0), 7);
        scene.idle(10);

        scene.overlay().showText(40)
            .text("Magnet will stop working when it receives a redstone signal.")
            .pointAt(util.vector().blockSurface(util.grid().at(2, 4, 2), Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();
        scene.idle(40);

        scene.markAsFinished();
    }

    private static void rubCopperBlock(@NotNull SceneBuilder scene, @NotNull SceneBuildingUtil util) {
        scene.title("magnet_power_generation", "Generate electricity by rubbing a magnet and a copper block");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        // 1. 出现铜块和磁铁
        BlockPos copperBlockPos = new BlockPos(1, 1, 2);
        BlockPos magnetPos = new BlockPos(2, 1, 1);
        scene.world().setBlock(copperBlockPos, Blocks.COPPER_BLOCK.defaultBlockState(), false);
        scene.world().setBlock(magnetPos, ModBlocks.MAGNET_BLOCK.getDefaultState(), false);
        scene.world().showSection(util.select().position(copperBlockPos), Direction.DOWN);
        ElementLink<WorldSectionElement> magnetElement = scene.world().showIndependentSection(util.select().position(magnetPos), Direction.DOWN);
        scene.world().moveSection(magnetElement, new Vec3(0, 0, 1), 5);
        scene.idle(20);

        // 2. 出现活塞，推动磁铁
        BlockPos pistonPos = new BlockPos(2, 1, 3);
        scene.world().setBlock(pistonPos, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.NORTH), false);
        scene.world().showSection(util.select().position(pistonPos), Direction.DOWN);

        scene.overlay().showText(60)
            .text("When a magnet is pushed against adjacent copper blocks, it generates electric charges.")
            .pointAt(util.vector().centerOf(pistonPos))
            .attachKeyFrame()
            .placeNearTarget();
        scene.idle(30);

        scene.world().moveSection(magnetElement, new Vec3(0, 0, -1), 5);
        scene.world().modifyBlock(pistonPos, state -> state.setValue(PistonBaseBlock.EXTENDED, true), false);

        BlockPos pistonHeadPos = new BlockPos(2, 1, 2);
        scene.world().setBlock(pistonHeadPos, Blocks.PISTON_HEAD.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.NORTH), false);
        scene.world().showSection(util.select().position(pistonHeadPos), Direction.NORTH);
        scene.idle(30);

        // 3. 出现集电器
        BlockPos chargeCollectPos = new BlockPos(4, 1, 2);
        scene.world().setBlock(chargeCollectPos, ModBlocks.CHARGE_COLLECTOR.getDefaultState(), false);
        scene.world().showSection(util.select().position(chargeCollectPos), Direction.DOWN);
        scene.overlay().showText(40)
            .text("Use a collector to absorb the electric charges on the magnet.")
            .pointAt(util.vector().centerOf(chargeCollectPos))
            .attachKeyFrame()
            .placeNearTarget();
        scene.idle(50);

        // 铜块生锈
        scene.world().setBlock(copperBlockPos, Blocks.OXIDIZED_COPPER.defaultBlockState(), false);
        scene.overlay().showText(40)
            .text("Copper rust can affect the production of electric charges.")
            .pointAt(util.vector().centerOf(copperBlockPos))
            .attachKeyFrame()
            .placeNearTarget();
        scene.idle(40);

        scene.markAsFinished();
    }
}
