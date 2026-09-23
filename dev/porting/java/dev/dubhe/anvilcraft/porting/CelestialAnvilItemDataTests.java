package dev.dubhe.anvilcraft.porting;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.cfa.item.CelestialForgingAnvilBlockItem;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class CelestialAnvilItemDataTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_cfa_equipment", CelestialAnvilItemDataTests::equipment,
        "port_cfa_render_snapshot", CelestialAnvilItemDataTests::snapshot,
        "port_cfa_drop_snapshot", CelestialAnvilItemDataTests::drop,
        "port_cfa_pick_controller", CelestialAnvilItemDataTests::pick,
        "port_cfa_nearby_placement", CelestialAnvilItemDataTests::placement
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_cfa_item_data"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    private static void equipment(GameTestHelper helper) {
        ItemStack stack = ModBlocks.CELESTIAL_FORGING_ANVIL.asStack();
        helper.assertTrue(stack.getMaxStackSize() == 1 && stack.getRarity() == Rarity.EPIC && stack.has(ModComponents.ETERNAL),
            "CFA uses source single-stack, epic and eternal properties");
        var equip = stack.get(DataComponents.EQUIPPABLE);
        helper.assertTrue(equip != null && equip.slot() == EquipmentSlot.HEAD
            && equip.equipSound().equals(ArmorMaterials.NETHERITE.equipSound()), "CFA equips on the head with netherite sound");
        var expected = Map.of(Attributes.ARMOR, 5.0, Attributes.ARMOR_TOUGHNESS, 4.0, Attributes.KNOCKBACK_RESISTANCE, 0.1);
        var modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();
        helper.assertTrue(modifiers.size() == 3 && modifiers.stream().allMatch(entry ->
            entry.slot().test(EquipmentSlot.HEAD) && !entry.slot().test(EquipmentSlot.CHEST)
                && expected.get(entry.attribute()) == entry.modifier().amount()), "Source armor values apply only to head slot");
        helper.assertTrue(stack.get(DataComponents.ENCHANTABLE).value() == ArmorMaterials.NETHERITE.enchantmentValue()
            && stack.is(ItemTags.HEAD_ARMOR_ENCHANTABLE) && stack.is(ItemTags.VANISHING_ENCHANTABLE), "Armor enchanting contract");
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PortCfaWear"));
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.assertTrue(stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction()
            && player.getItemBySlot(EquipmentSlot.HEAD).is(ModBlocks.CELESTIAL_FORGING_ANVIL.asItem()), "Use action actually equips CFA");
        helper.succeed();
    }

    private static void snapshot(GameTestHelper helper) {
        var tag = new CompoundTag();
        var state = new CompoundTag();
        state.putInt("phase", 2);
        tag.put("stellarState", state);
        tag.putString("activeMegastructureId", "anvilcraft:stellar_evolution_accelerator");
        tag.putInt("acceleratorTicksRemaining", 35);
        tag.putBoolean("excavatorLaserActive", true);
        tag.putString("owner", "not rendering data");
        CelestialForgingAnvilBlockItem.saveRenderData(tag, 400);
        var render = tag.getCompoundOrEmpty(CelestialForgingAnvilBlockItem.ITEM_RENDER_DATA);
        helper.assertTrue(render.getLongOr("acceleratorPausedSinceGameTime", -1) == 400
            && render.getBooleanOr("excavatorLaserActive", false) && !render.contains("owner"), "Snapshot whitelist and paused time");
        state.putInt("phase", 9);
        helper.assertTrue(render.getCompoundOrEmpty("stellarState").getIntOr("phase", -1) == 2, "Snapshot owns nested tag copies");
        tag.putLong("acceleratorPausedSinceGameTime", 123);
        CelestialForgingAnvilBlockItem.saveRenderData(tag, 800);
        helper.assertTrue(tag.getCompoundOrEmpty(CelestialForgingAnvilBlockItem.ITEM_RENDER_DATA)
            .getLongOr("acceleratorPausedSinceGameTime", -1) == 123, "Previously paused time is preserved");
        helper.succeed();
    }

    private static CelestialForgingAnvilBlockEntity machine(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(3, 2, 3));
        helper.getLevel().setBlock(pos, ModBlocks.CELESTIAL_FORGING_ANVIL.getDefaultState(), Block.UPDATE_CLIENTS);
        return (CelestialForgingAnvilBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    private static void drop(GameTestHelper helper) {
        final var be = machine(helper);
        var saved = new CompoundTag();
        saved.putLong("bodySeed", 42);
        saved.putString("activeMegastructureId", "anvilcraft:stellar_evolution_accelerator");
        saved.putFloat("stellarPhaseProgress", 0.25F);
        saved.putInt("colliderReservedAnvil", 1);
        saved.putLong("acceleratorPausedSinceGameTime", -1);
        try {
            var field = CelestialForgingAnvilBlockEntity.class.getDeclaredField("cachedDropData");
            field.setAccessible(true);
            field.set(be, saved);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
        be.getAnvilInventory().setItem(0, Items.ANVIL.getDefaultInstance());
        be.getMaterialContainer().setItem(0, new ItemStack(Items.DIAMOND, 3));
        var drops = Block.getDrops(be.getBlockState(), helper.getLevel(), be.getBlockPos(), be);
        var item = drops.stream().filter(stack -> stack.is(ModBlocks.CELESTIAL_FORGING_ANVIL.asItem())).findFirst().orElseThrow();
        var tag = item.get(DataComponents.BLOCK_ENTITY_DATA).copyTagWithoutId();
        var render = tag.getCompoundOrEmpty(CelestialForgingAnvilBlockItem.ITEM_RENDER_DATA);
        helper.assertTrue(tag.getLongOr("bodySeed", 0) == 42 && !tag.contains("activeMegastructureId")
            && !tag.contains("stellarPhaseProgress") && !tag.contains("colliderReservedAnvil")
            && render.getFloatOr("stellarPhaseProgress", 0) == 0.25F
            && render.getLongOr("acceleratorPausedSinceGameTime", -1) == helper.getLevel().getGameTime(),
            "Drop preserves appearance independently of cleared placement runtime");
        helper.assertTrue(drops.stream().filter(stack -> stack.is(Items.ANVIL)).mapToInt(ItemStack::getCount).sum() == 1
            && drops.stream().filter(stack -> stack.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum() == 3,
            "Inventories are dropped separately exactly once");
        helper.succeed();
    }

    private static void placement(GameTestHelper helper) {
        var first = machine(helper);
        var target = first.getBlockPos().east(5);
        helper.getLevel().setBlockAndUpdate(target.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PortCfaPlace"));
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(target.getX(), target.getY(), target.getZ() + 3);
        var stack = ModBlocks.CELESTIAL_FORGING_ANVIL.asStack();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var context = new net.minecraft.world.item.context.BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
            new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(target.below()),
                net.minecraft.core.Direction.UP, target.below(), false));
        var result = ((CelestialForgingAnvilBlockItem) stack.getItem()).place(context);
        helper.assertTrue(result.consumesAction() && helper.getLevel().getBlockState(target).is(ModBlocks.CELESTIAL_FORGING_ANVIL),
            "Source permits nearby non-overlapping CFA placement without the old seven-block radius restriction");
        helper.succeed();
    }

    private static void pick(GameTestHelper helper) {
        final var be = machine(helper);
        var block = ModBlocks.CELESTIAL_FORGING_ANVIL.get();
        var part = Cube323PartHalf.TOP_E;
        var pos = be.getBlockPos().offset(part.getOffset());
        helper.getLevel().setBlock(pos, block.placedState(part, block.defaultBlockState()), Block.UPDATE_CLIENTS);
        helper.assertTrue(block.getPickBlockEntity(helper.getLevel(), pos, helper.getLevel().getBlockState(pos)) == be,
            "Part lookup resolves the controller");
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PortCfaPick"));
        player.setGameMode(GameType.CREATIVE);
        player.setPos(pos.getX(), pos.getY(), pos.getZ() + 2);
        player.connection.handlePickItemFromBlock(new ServerboundPickItemFromBlockPacket(pos, true));
        var picked = player.getMainHandItem();
        helper.assertTrue(picked.is(ModBlocks.CELESTIAL_FORGING_ANVIL.asItem()) && picked.has(DataComponents.BLOCK_ENTITY_DATA),
            "Native server pick packet captures the controller from a non-controller part");
        var render = picked.get(DataComponents.BLOCK_ENTITY_DATA).copyTagWithoutId()
            .getCompoundOrEmpty(CelestialForgingAnvilBlockItem.ITEM_RENDER_DATA);
        helper.assertTrue(render.getLongOr("acceleratorPausedSinceGameTime", -1) == helper.getLevel().getGameTime(),
            "Native pick serializes item appearance data");
        helper.succeed();
    }
}
