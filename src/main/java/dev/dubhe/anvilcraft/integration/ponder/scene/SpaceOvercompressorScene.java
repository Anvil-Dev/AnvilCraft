package dev.dubhe.anvilcraft.integration.ponder.scene;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class SpaceOvercompressorScene {
    public static void register(@NotNull PonderSceneRegistrationHelper<ResourceLocation> registrationHelper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> helper = registrationHelper.withKeyFunction(RegistryEntry::getId);
        helper.forComponents(
                ModBlocks.SPACE_OVERCOMPRESSOR
            )
            .addStoryBoard(
                "platform/555",
                SpaceOvercompressorScene::crafting
            );
    }

    private static void crafting(@NotNull SceneBuilder scene, @NotNull SceneBuildingUtil util) {
        scene.title("space_overcompressor", "Use the Space Overcompressor to create the Neutronium Ingot");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        // 创建空间超压器
        BlockPos spaceOvercompressorPos = new BlockPos(2, 2, 2);
        scene.world().setBlock(spaceOvercompressorPos, ModBlocks.SPACE_OVERCOMPRESSOR.getDefaultState(), false);
        Selection spaceOvercompressor = util.select().position(2, 2, 2);
        scene.world().showIndependentSection(spaceOvercompressor, Direction.NORTH);
        // 创建铁砧
        BlockPos anvilPos = new BlockPos(2, 4, 2);
        scene.world().setBlock(anvilPos, Blocks.ANVIL.defaultBlockState(), false);
        Selection anvil = util.select().position(2, 4, 2);
        ElementLink<WorldSectionElement> anvilLink = scene.world().showIndependentSection(anvil, Direction.NORTH);
        scene.idle(10);
        // 在 y=0 层，从 (1,0,1) 到 (3,0,3) 的 3x3 区域内删除原有方块并放置飘浮粉块
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                BlockPos pos = new BlockPos(x, 0, z);
                scene.world().setBlock(pos, ModBlocks.END_DUST.getDefaultState(), true);
            }
        }
        scene.idle(10);

        Vec3 ironBlockPos = new Vec3(2.5, 3.3, 2.5);
        Vec3 ironBlockMotion = new Vec3(0, -0.3, 0);
        ItemStack ironBlockItem = new ItemStack(ModBlocks.HEAVY_IRON_BLOCK, 64);
        // 循环3次，每次砸入物品
        scene.overlay().showText(40)
            .text("Press a large amount of metal items into the Space Overcompressor to accumulate mass.")
            .pointAt(util.vector().blockSurface(util.grid().at(2, 3, 2), Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();
        for (int i = 0; i < 3; i++) {
            // 添加铁块
            ElementLink<EntityElement> ironBockItemLink = scene.world().createItemEntity(ironBlockPos, ironBlockMotion, ironBlockItem);
            scene.idle(5);
            // 铁砧压入
            scene.world().moveSection(anvilLink, new Vec3(0, -1, 0), 2);
            scene.idle(2);
            scene.world().modifyEntity(ironBockItemLink, entity -> entity.setPos(2.5, -100, 2.5));
            scene.idle(6);
            // 铁砧上移
            scene.world().moveSection(anvilLink, new Vec3(0, 1, 0), 3);
            scene.idle(3);
        }
        // 从空间超压器下方掉出中子锭
        scene.overlay().showText(100)
            .text("When the Space Overcompressor has built up enough mass, a neutron ingot will form. It can pass through most blocks, so you'll need something like end dust to stop it.")
            .pointAt(util.vector().blockSurface(util.grid().at(2, 1, 2), Direction.WEST))
            .attachKeyFrame()
            .placeNearTarget();
        ItemStack outputItem = new ItemStack(ModItems.NEUTRONIUM_INGOT.asItem(), 1);
        Vec3 outputPos = new Vec3(2.5, 1.8, 2.5);
        scene.world().createItemEntity(outputPos, Vec3.ZERO, outputItem);

        scene.idle(20); // 等待一会展示结果

        scene.markAsFinished();
    }
}
