package dev.dubhe.anvilcraft.integration.ponder.scene.recipe;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.block.OilCauldronBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.integration.ponder.AnvilCraftPonderTags;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class CorruptedBeaconScene {
    public static void register(PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> helper = registrationHelper.withKeyFunction(RegistryEntry::getId);
        helper.forComponents(ModBlocks.CORRUPTED_BEACON)
            .addStoryBoard("platform/9x", CorruptedBeaconScene::get, AnvilCraftPonderTags.PROCESSING_COMPONENTS)
            .addStoryBoard("platform/5x", CorruptedBeaconScene::mobTransform, AnvilCraftPonderTags.PROCESSING_COMPONENTS);

        helper.forComponents(ModBlocks.CORRUPTED_BEACON, ModBlocks.GIANT_ANVIL)
            .addStoryBoard("platform/5x", CorruptedBeaconScene::giantAnvil, AnvilCraftPonderTags.PROCESSING_COMPONENTS);

        helper.forComponents(ModBlocks.CORRUPTED_BEACON)
            .addStoryBoard("platform/5x", CorruptedBeaconScene::timeWarp, AnvilCraftPonderTags.PROCESSING_COMPONENTS);
    }

    private static void get(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("get_corrupted_beacon", "Get Corrupted Beacon");
        builder.configureBasePlate(0, 0, 9);
        builder.scaleSceneView(0.6f);
        builder.showBasePlate();

        BlockPos beaconPos = util.grid().at(4, 2, 4);
        builder.world().setBlock(beaconPos, Blocks.BEACON.defaultBlockState(), false);
        builder.world().showSection(util.select().position(beaconPos), Direction.NORTH);

        Selection layerArea = util.select().fromTo(3, 1, 3, 5, 1, 5);
        builder.world().setBlocks(layerArea, ModBlocks.CURSED_GOLD_BLOCK.getDefaultState(), false);
        builder.world().showSection(layerArea, Direction.NORTH);
        builder.idle(20);

        for (int i = 0; i < 3; i++) {
            builder.overlay().showControls(beaconPos.getCenter(), Pointing.RIGHT, 1)
                .withItem(ModItems.CURSED_GOLD_INGOT.asStack());
            builder.idle(10);
        }
        builder.overlay().showControls(beaconPos.getCenter(), Pointing.RIGHT, 1)
            .withItem(ModItems.CURSED_GOLD_INGOT.asStack());
        builder.idle(10);

        builder.world().setBlock(beaconPos, ModBlocks.CORRUPTED_BEACON.getDefaultState(), false);
        builder.idle(10);

        builder.overlay().showText(40)
            .text("Beacons have a probability of being transformed.")
            .pointAt(beaconPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        //更大的基座
        builder.world().hideSection(layerArea, Direction.UP);
        builder.world().hideSection(util.select().position(beaconPos), Direction.UP);
        builder.idle(20);

        for (int i = 1; i < 5; i++) {
            layerArea = util.select().fromTo(4 - i, 5 - i, 4 - i, 4 + i, 5 - i, 4 + i);
            builder.world().setBlocks(layerArea, ModBlocks.CURSED_GOLD_BLOCK.getDefaultState(), false);
            builder.world().showSection(layerArea, Direction.NORTH);
        }
        beaconPos = util.grid().at(4, 5, 4);
        builder.world().setBlock(beaconPos, Blocks.BEACON.defaultBlockState(), false);
        builder.world().showSection(util.select().position(beaconPos), Direction.NORTH);
        builder.idle(20);

        builder.overlay().showControls(beaconPos.getCenter(), Pointing.RIGHT, 5)
            .withItem(ModItems.CURSED_GOLD_INGOT.asStack());
        builder.world().setBlock(beaconPos, ModBlocks.CORRUPTED_BEACON.getDefaultState(), false);
        builder.idle(10);

        builder.overlay().showText(40)
            .text("The bigger the base, the higher the probability.")
            .pointAt(beaconPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        builder.markAsFinished();
    }

    private static void mobTransform(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("mob_transform", "Mob Transform");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();

        BlockPos beaconPos = util.grid().at(2, 2, 2);
        builder.world().setBlock(beaconPos, ModBlocks.CORRUPTED_BEACON.getDefaultState().setValue(CorruptedBeaconBlock.LIT, true), false);
        builder.world().showSection(util.select().position(beaconPos), Direction.NORTH);
        builder.overlay().showLine(0x66222222, new BlockPos(2, 3, 2).getBottomCenter(), new BlockPos(2, 100, 2).getCenter(), 600, 1 / 3f);

        Selection layerArea = util.select().fromTo(3, 1, 3, 1, 1, 1);
        builder.world().setBlocks(layerArea, Blocks.IRON_BLOCK.defaultBlockState(), false);
        builder.world().showSection(layerArea, Direction.NORTH);
        builder.idle(20);

        ElementLink<EntityElement> entity = builder.world().createEntity(world -> {
            Skeleton skeleton = EntityType.SKELETON.create(world);
            if (skeleton != null) {
                skeleton.moveTo(beaconPos.above().getBottomCenter());
            }
            return skeleton;
        });
        builder.idle(20);

        builder.world().modifyEntity(entity, skeleton -> skeleton.remove(Entity.RemovalReason.KILLED));
        builder.world().createEntity(world -> {
            WitherSkeleton witherSkeleton = EntityType.WITHER_SKELETON.create(world);
            if (witherSkeleton != null) {
                witherSkeleton.moveTo(beaconPos.above().getBottomCenter());
                witherSkeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
            }
            return witherSkeleton;
        });
        builder.idle(20);

        builder.overlay().showText(30)
            .text("Some mob can be transformed.")
            .pointAt(beaconPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(40);

        builder.markAsFinished();
    }

    private static void giantAnvil(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("get_giant_anvil", "Get Giant Anvil");
        builder.configureBasePlate(0, 0, 5);
        builder.scaleSceneView(0.4f);
        builder.showBasePlate();
        builder.rotateCameraY(180);
        builder.setSceneOffsetY(-4);

        BlockPos beaconPos = util.grid().at(2, 2, 2);
        builder.world().setBlock(beaconPos, ModBlocks.CORRUPTED_BEACON.getDefaultState().setValue(CorruptedBeaconBlock.LIT, true), false);

        Selection layerArea = util.select().fromTo(3, 1, 3, 1, 1, 1);
        builder.world().setBlocks(layerArea, Blocks.IRON_BLOCK.defaultBlockState(), false);
        builder.world().showSection(layerArea, Direction.NORTH);
        builder.idle(20);

        BlockPos zombiePos = beaconPos.above();
        ElementLink<EntityElement> zombieLink = builder.world().createEntity(world -> {
            Zombie zombie = EntityType.ZOMBIE.create(world);
            if (zombie != null) {
                zombie.moveTo(zombiePos.getBottomCenter());
            }
            return zombie;
        });
        builder.idle(10);

        builder.overlay().showControls(zombiePos.getCenter(), Pointing.RIGHT, 5)
            .rightClick()
            .withItem(Items.ANVIL.getDefaultInstance());
        builder.world().modifyEntity(
            zombieLink, zombie -> {
                if (zombie instanceof Zombie z) {
                    z.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.ANVIL));
                }
            }
        );
        builder.idle(20);

        builder.world().showSection(util.select().position(beaconPos), Direction.NORTH);
        builder.idle(10);

        builder.overlay().showLine(0x66222222, new BlockPos(2, 3, 2).getBottomCenter(), new BlockPos(2, 100, 2).getCenter(), 600, 1 / 3f);
        builder.idle(10);

        builder.world().modifyEntity(zombieLink, zombie -> zombie.remove(Entity.RemovalReason.KILLED));
        builder.world().createEntity(world -> {
            Giant giant = EntityType.GIANT.create(world);
            if (giant != null) {
                giant.moveTo(zombiePos.getBottomCenter());
                giant.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModBlocks.GIANT_ANVIL.asItem()));
            }
            return giant;
        });
        builder.idle(10);

        builder.overlay().showText(100)
            .text(
                "The more anvils you give to zombies, the greater the probability that they will be transformed into giants carrying giant anvils.")
            .pointAt(beaconPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(110);

        builder.markAsFinished();
    }

    private static void timeWarp(SceneBuilder scene, SceneBuildingUtil util) {
        AnvilCraftSceneBuilder builder = new AnvilCraftSceneBuilder(scene);
        builder.title("time_warp", "Time Warp Recipe");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();


        Selection layerArea = util.select().fromTo(3, 0, 3, 1, 0, 1);
        builder.world().setBlocks(layerArea, ModBlocks.CURSED_GOLD_BLOCK.getDefaultState(), false);

        BlockPos beaconPos = util.grid().at(2, 1, 2);
        builder.world().setBlock(beaconPos, ModBlocks.CORRUPTED_BEACON.getDefaultState(), false);
        builder.world().showSection(util.select().position(beaconPos), Direction.NORTH);

        BlockPos cauldronPos = beaconPos.above();
        builder.world().setBlock(cauldronPos, Blocks.CAULDRON.defaultBlockState(), false);
        builder.world().showSection(util.select().position(cauldronPos), Direction.NORTH);

        BlockPos anvilPos = cauldronPos.above(2);
        builder.world().setBlock(anvilPos, Blocks.ANVIL.defaultBlockState(), false);
        ElementLink<WorldSectionElement> anvilLink =
            builder.world().showIndependentSection(util.select().position(anvilPos), Direction.NORTH);
        builder.idle(20);

        ElementLink<EntityElement> item = builder.world().createItemEntity(cauldronPos.above(), Items.OAK_LOG.getDefaultInstance());
        builder.world().falldownSection(anvilLink);
        item = builder.world().replaceItemEntity(cauldronPos, Items.COAL.getDefaultInstance(), item);
        builder.world().riseSection(anvilLink);
        builder.idle(10);
        builder.world().modifyEntity(item, itemEntity -> itemEntity.remove(Entity.RemovalReason.DISCARDED));
        builder.idle(10);

        item = builder.world().createItemEntity(cauldronPos.above(), new ItemStack(Items.PORKCHOP, 64));
        builder.world().falldownSection(anvilLink);
        builder.world().modifyEntity(item, itemEntity -> itemEntity.remove(Entity.RemovalReason.DISCARDED));
        builder.world().setBlock(cauldronPos, ModBlocks.OIL_CAULDRON.getDefaultState().setValue(OilCauldronBlock.LEVEL, 4), false);
        builder.world().riseSection(anvilLink);
        builder.idle(10);

        builder.overlay().showText(40)
            .text("Corrupted Beacons can execute time warp recipe.")
            .pointAt(beaconPos.getCenter())
            .attachKeyFrame()
            .placeNearTarget();
        builder.idle(50);

        builder.markAsFinished();
    }
}
