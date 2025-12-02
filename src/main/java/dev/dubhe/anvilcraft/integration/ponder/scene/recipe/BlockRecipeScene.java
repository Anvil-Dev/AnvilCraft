package dev.dubhe.anvilcraft.integration.ponder.scene.recipe;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.ponder.api.AnvilCraftSceneBuilder;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BlockRecipeScene {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<Item> helper = registrationHelper.withKeyFunction(BuiltInRegistries.ITEM::getKey);
        helper.forComponents(
                Items.ANVIL,
                Items.CHIPPED_ANVIL,
                Items.DAMAGED_ANVIL
            )
            .addStoryBoard("platform/5x", BlockRecipeScene::crafting)
            .addStoryBoard("platform/5x", BlockRecipeScene::processing);
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private static void crafting(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("block_recipe", "Use anvil hit blocks");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();

        BlockPos anvilPos = util.grid().at(2, 3, 2);
        BlockPos downPos = util.grid().at(2, 1, 2);
        BlockPos upPos = util.grid().at(2, 2, 2);
        ElementLink<EntityElement> itemLink;
        builder.world().showSection(util.select().position(upPos), Direction.NORTH);

        // 方块粉碎
        builder.world().setBlock(anvilPos, Blocks.ANVIL.defaultBlockState(), false);
        final ElementLink<WorldSectionElement> anvilLink = builder.world()
            .showIndependentSection(util.select().position(anvilPos), Direction.DOWN);

        builder.world().setBlock(downPos, Blocks.COBBLESTONE.defaultBlockState(), false);
        builder.world().showSection(util.select().position(downPos), Direction.NORTH);
        builder.idle(20);

        builder.world().falldownSection(anvilLink);
        builder.world().setBlock(downPos, Blocks.GRAVEL.defaultBlockState(), true);
        builder.world().riseSection(anvilLink);
        builder.idle(10);

        builder.world().falldownSection(anvilLink);
        builder.world().setBlock(downPos, Blocks.SAND.defaultBlockState(), true);
        builder.world().riseSection(anvilLink);
        builder.idle(10);

        builder.overlay()
            .showText(40)
            .text("When the anvil hits a specific block, the block is crushed")
            .pointAt(downPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(60);
        // 复位
        builder.world().setBlock(downPos, Blocks.AIR.defaultBlockState(), false);
        builder.idle(10);

        // 物品压入方块
        builder.world().setBlock(downPos, Blocks.SHULKER_BOX.defaultBlockState(), false);
        itemLink = builder.world().createItemEntity(upPos.getCenter(), Vec3.ZERO, Items.SHULKER_BOX.getDefaultInstance());
        builder.idle(20);

        builder.world().falldownSection(anvilLink);
        builder.world().setBlock(downPos, ModBlocks.NESTING_SHULKER_BOX.getDefaultState(), true);
        builder.world().removeEntity(itemLink);
        builder.world().riseSection(anvilLink);
        builder.idle(10);

        builder.overlay()
            .showText(60)
            .text("When the anvil hits the block with an item on it, press the item into the block")
            .pointAt(downPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(70);
        // 复位
        builder.world().setBlock(downPos, Blocks.AIR.defaultBlockState(), false);
        builder.idle(10);

        // 方块破坏
        builder.world().setBlock(downPos, Blocks.STONECUTTER.defaultBlockState(), false);
        builder.world().setBlock(upPos, Blocks.STONE.defaultBlockState(), false);
        builder.world().riseSection(anvilLink);
        builder.idle(10);

        builder.world().falldownSection(anvilLink);
        builder.world().setBlock(upPos, Blocks.AIR.defaultBlockState(), true);
        itemLink = builder.world().createItemEntity(upPos.getCenter(), Vec3.ZERO, Items.COBBLESTONE.getDefaultInstance());
        builder.world().riseSection(anvilLink);
        builder.idle(10);

        builder.overlay()
            .showText(60)
            .text("When the anvil hit the block on the stone cutter, the block was destroyed")
            .pointAt(upPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(70);

        builder.world().removeEntity(itemLink);
        // 皇家铁砧: 精准采集
        builder.world().setBlock(anvilPos, ModBlocks.ROYAL_ANVIL.getDefaultState(), false);
        builder.world().setBlock(upPos, Blocks.STONE.defaultBlockState(), false);
        builder.idle(10);

        builder.world().falldownSection(anvilLink);
        builder.world().setBlock(upPos, Blocks.AIR.defaultBlockState(), true);
        itemLink = builder.world().createItemEntity(upPos.getCenter(), Vec3.ZERO, Items.STONE.getDefaultInstance());
        builder.world().riseSection(anvilLink);
        builder.idle(10);

        builder.overlay()
            .showText(40)
            .text("The Royal Anvil can precisely destroy blocks")
            .pointAt(upPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        builder.world().removeEntity(itemLink);
        // 余烬铁砧：熔炼
        builder.world().setBlock(anvilPos, ModBlocks.EMBER_ANVIL.getDefaultState(), false);
        builder.world().setBlock(upPos, Blocks.IRON_ORE.defaultBlockState(), false);
        builder.idle(10);

        builder.world().falldownSection(anvilLink);
        builder.world().setBlock(upPos, Blocks.AIR.defaultBlockState(), true);
        itemLink = builder.world().createItemEntity(upPos.getCenter(), Vec3.ZERO, Items.IRON_INGOT.getDefaultInstance());
        builder.world().riseSection(anvilLink);
        builder.idle(10);

        builder.overlay()
            .showText(40)
            .text("The Ember Anvil can melt blocks")
            .pointAt(upPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        builder.world().removeEntity(itemLink);
        // 复位
        builder.world().setBlock(downPos, Blocks.AIR.defaultBlockState(), false);
        builder.world().setBlock(anvilPos, Blocks.ANVIL.defaultBlockState(), false);
        builder.idle(10);

        // 方块压合
        builder.world().setBlock(downPos, Blocks.ICE.defaultBlockState(), false);
        builder.world().setBlock(upPos, Blocks.ICE.defaultBlockState(), false);
        builder.idle(20);

        builder.world().falldownSection(anvilLink);
        builder.world().setBlock(upPos, Blocks.AIR.defaultBlockState(), false);
        builder.world().setBlock(downPos, Blocks.PACKED_ICE.defaultBlockState(), true);
        builder.idle(3);

        builder.world().falldownSection(anvilLink);
        builder.idle(10);

        builder.overlay()
            .showText(40)
            .text("The anvil can compress blocks")
            .pointAt(downPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);
        // 复位
        builder.world().riseSection(anvilLink, 2);
        builder.world().setBlock(downPos, Blocks.AIR.defaultBlockState(), false);
        builder.idle(10);

        // 方块涂抹
        builder.world().setBlock(downPos, Blocks.COBBLESTONE.defaultBlockState(), false);
        builder.world().setBlock(upPos, Blocks.MOSS_BLOCK.defaultBlockState(), false);
        builder.idle(20);

        builder.world().falldownSection(anvilLink);
        builder.world().setBlock(downPos, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), false);
        builder.idle(10);

        builder.overlay().showText(40)
            .text("The anvil can smear blocks")
            .pointAt(downPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);
        // 复位
        builder.world().setBlock(downPos, Blocks.AIR.defaultBlockState(), false);
        builder.world().riseSection(anvilLink);
        builder.idle(10);

        // 方块压榨
        builder.world().setBlock(downPos, Blocks.CAULDRON.defaultBlockState(), false);
        builder.world().setBlock(upPos, Blocks.SNOW_BLOCK.defaultBlockState(), false);
        builder.idle(20);

        builder.world().falldownSection(anvilLink);
        builder.world().setBlock(upPos, Blocks.ICE.defaultBlockState(), false);
        builder.world().setBlock(downPos, CauldronUtil.getStateFromContentAndLevel(Blocks.POWDER_SNOW_CAULDRON, 1), false);
        builder.idle(10);

        builder.overlay()
            .showText(40)
            .text("The anvil can squeeze blocks")
            .pointAt(downPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);
        // 复位
        builder.world().setBlock(downPos, Blocks.AIR.defaultBlockState(), false);
        builder.world().setBlock(upPos, Blocks.AIR.defaultBlockState(), false);
        builder.world().riseSection(anvilLink);
        builder.idle(10);
    }

    private static void processing(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("block_process", "Use anvil to process");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();

        BlockPos anvilPos = util.grid().at(2, 3, 2);
        BlockPos blockPos = util.grid().at(2, 1, 2);

        // 强制刷怪
        builder.world().setBlock(anvilPos, Blocks.ANVIL.defaultBlockState(), false);
        final ElementLink<WorldSectionElement> anvilLink = builder.world()
            .showIndependentSection(util.select().position(anvilPos), Direction.DOWN);

        builder.world().setBlock(blockPos, Blocks.SPAWNER.defaultBlockState(), false);
        builder.world().showSection(util.select().position(blockPos), Direction.NORTH);
        builder.idle(20);

        for (int i = 0; i < 2; i++) {
            builder.world().falldownSection(anvilLink);
            builder.world().riseSection(anvilLink);
            builder.idle(10);
        }

        // 随机生成很多猪
        builder.world().falldownSection(anvilLink);
        final List<ElementLink<EntityElement>> zombies = spawnZombies(builder, blockPos);
        builder.world().riseSection(anvilLink);
        builder.idle(10);

        builder.overlay()
            .showText(50)
            .text("When the anvil hits the spawner, it will be forced to work")
            .pointAt(blockPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(60);

        builder.overlay()
            .showText(50)
            .text("But there are still constraints, such as light, number of mob")
            .pointAt(blockPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(60);

        for (ElementLink<EntityElement> zombie : zombies) {
            builder.world().removeEntity(zombie);
        }
        zombies.clear();
        builder.idle(10);

        // 高度越高，成功概率越大
        builder.world().riseSection(anvilLink, 3);
        builder.idle(10);

        builder.world().falldownSection(anvilLink, 4);
        zombies.addAll(spawnZombies(builder, blockPos));
        builder.idle(10);

        builder.overlay()
            .showText(60)
            .text("The higher the height of the anvil, the higher the probability of success")
            .pointAt(blockPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(70);
        for (ElementLink<EntityElement> zombie : zombies) {
            builder.world().removeEntity(zombie);
        }
        // 复位
        zombies.clear();
        builder.world().hideSection(util.select().position(blockPos), Direction.NORTH);
        builder.world().riseSection(anvilLink);
        builder.idle(10);

        // 红石EMP
        Selection redstonePos = util.select().fromTo(0, 1, 1, 1, 1, 3);

        // 在每个位置放置红石火把
        builder.world().setBlocks(redstonePos, Blocks.REDSTONE_TORCH.defaultBlockState(), false);
        builder.world().showSection(redstonePos, Direction.NORTH);

        builder.world().setBlock(blockPos, Blocks.REDSTONE_BLOCK.defaultBlockState(), false);
        builder.world().showSection(util.select().position(blockPos), Direction.NORTH);
        builder.idle(20);

        builder.world().falldownSection(anvilLink);
        builder.world().modifyBlocks(redstonePos, state -> state.setValue(RedstoneTorchBlock.LIT, false), false);
        builder.idle(2);
        builder.world().modifyBlocks(redstonePos, state -> state.setValue(RedstoneTorchBlock.LIT, true), false);
        builder.idle(10);

        builder.overlay()
            .showText(100)
            .text("When the anvil strikes the red stone, a red stone EMP occurs, extinguishing the nearby red stone torches for an instant")
            .pointAt(blockPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(110);

        builder.overlay()
            .showText(60)
            .text("The higher the anvil falls, the larger the range")
            .pointAt(anvilPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(70);
        // 复位
        builder.world().setBlocks(redstonePos, Blocks.AIR.defaultBlockState(), false);
        builder.world().hideSection(util.select().position(blockPos), Direction.NORTH);
        builder.world().riseSection(anvilLink, 2);
        builder.idle(10);

        // 宝库重置
        builder.world().setBlock(blockPos, Blocks.VAULT.defaultBlockState(), false);
        builder.world().showSection(util.select().position(blockPos), Direction.NORTH);

        BlockPos leadPos = blockPos.above();
        builder.world().setBlock(leadPos, ModBlocks.LEAD_BLOCK.getDefaultState(), false);
        builder.world().showSection(util.select().position(leadPos), Direction.NORTH);
        builder.idle(20);

        builder.world().falldownSection(anvilLink);
        builder.world().setBlock(leadPos, Blocks.AIR.defaultBlockState(), false);
        builder.world().modifyBlock(blockPos, state -> state.setValue(VaultBlock.STATE, VaultState.ACTIVE), false);
        builder.world().riseSection(anvilLink);
        builder.overlay()
            .showText(40)
            .text("Press the lead into the vault to reset it")
            .pointAt(blockPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        builder.markAsFinished();
    }

    private static List<ElementLink<EntityElement>> spawnZombies(SceneBuilder builder, BlockPos centerPos) {
        List<ElementLink<EntityElement>> mobs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            BlockPos mobPos = centerPos.east(new Random().nextInt(5) - 2).north(new Random().nextInt(5) - 2);
            mobs.add(builder.world().createEntity(world -> {
                Zombie mob = EntityType.ZOMBIE.create(world);
                if (mob != null) {
                    mob.moveTo(mobPos.getBottomCenter());
                }
                return mob;
            }));
            builder.effects().indicateSuccess(mobPos);
        }
        return mobs;
    }
}

