package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.ChuteBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class FilterPersistenceTests {
    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION,
            registry -> registry.register(AnvilCraft.of("port_filter_persistence"), FilterPersistenceTests::persistence));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_filter"));
        event.registerTest(AnvilCraft.of("port_filter_persistence"), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of("port_filter_persistence")),
            new TestData<>(environment, AnvilCraft.of("port_empty"), 100, 0, true)));
    }

    private static void persistence(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ModBlocks.CHUTE.getDefaultState());
        var chute = helper.getBlockEntity(pos, ChuteBlockEntity.class);
        var chunk = helper.getLevel().getChunkAt(helper.absolutePos(pos));
        chunk.tryMarkSaved();
        chute.setFilterEnabled(true);
        helper.assertTrue(chunk.isUnsaved(), "Filter enable marks the real chunk dirty");
        chunk.tryMarkSaved();
        chute.setSlotDisabled(1, true);
        helper.assertTrue(chunk.isUnsaved(), "Disabling a slot marks the real chunk dirty");
        chunk.tryMarkSaved();
        chute.setSlotLimit(0, 7);
        helper.assertTrue(chunk.isUnsaved(), "Slot limit marks the real chunk dirty");
        chunk.tryMarkSaved();
        helper.assertTrue(chute.setFilter(0, new ItemStack(Items.DIAMOND)) && chunk.isUnsaved(),
            "Accepted filter marks the real chunk dirty");
        chunk.tryMarkSaved();
        helper.assertTrue(!chute.setFilter(0, ItemStack.EMPTY) && !chunk.isUnsaved(), "Rejected empty filter does not mark dirty");
        var tag = chute.saveWithFullMetadata(helper.getLevel().registryAccess());
        var restored = ModBlockEntities.CHUTE.get().create(chute.getBlockPos(), chute.getBlockState());
        restored.setLevel(helper.getLevel());
        restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        helper.assertTrue(restored.isFilterEnabled() && restored.isSlotDisabled(1) && restored.getSlotLimit(0) == 7
            && restored.getFilter(0).is(Items.DIAMOND), "All settings survive native block-entity serialization");
        helper.succeed();
    }
}
