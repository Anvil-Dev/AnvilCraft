package dev.dubhe.anvilcraft.building;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.item.BuildingRodItem;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import dev.dubhe.anvilcraft.item.utility.CrabClawItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BuildingRodItemTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_rod_energy", BuildingRodItemTests::energy,
        "port_rod_hands", BuildingRodItemTests::hands,
        "port_rod_reach", BuildingRodItemTests::reach,
        "port_rod_recipes", BuildingRodItemTests::recipes,
        "port_rod_tooltip", BuildingRodItemTests::tooltip,
        "port_rod_undo_guard", BuildingRodItemTests::undo
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_rod_item"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static ServerPlayer player(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PortRodItem"));
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static int energy(ItemStack stack) {
        return stack.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
    }

    private static void energy(GameTestHelper helper) {
        var player = player(helper);
        var rod = ModItems.BUILDING_ROD.asStack();
        player.setItemInHand(InteractionHand.MAIN_HAND, rod);
        helper.assertTrue(!BuildingRodItem.ready(player, rod), "空电无电容时不能开始建造");
        player.setItemSlot(EquipmentSlot.LEGS, ModItems.POCKETS_LEGGINGS.asStack());
        var pockets = PocketInventory.get(player);
        pockets.setItem(0, ModItems.SUPER_CAPACITOR.asStack());
        pockets.setItem(1, ModItems.CAPACITOR.asStack(2));
        helper.assertTrue(BuildingRodItem.ready(player, rod) && energy(rod) == 8_000_000
            && pockets.getItem(0).getCount() == 1 && pockets.getItem(1).getCount() == 1
            && player.getInventory().countItem(ModItems.CAPACITOR_EMPTY.get()) == 1,
            "自动充电应跳过过大的电容，消耗一个普通电容并返还空壳");
        BuildingRodItem.consume(player, rod, 3);
        helper.assertTrue(energy(rod) == 7_999_700 && pockets.getItem(1).getCount() == 1,
            "每方块消耗 100 FE，未耗尽不能提前换电容");
        rod.set(ModComponents.STORED_ENERGY, new StoredEnergy(50));
        BuildingRodItem.consume(player, rod, 1);
        helper.assertTrue(energy(rod) == 8_000_000 && pockets.getItem(1).isEmpty()
            && player.getInventory().countItem(ModItems.CAPACITOR_EMPTY.get()) == 2, "耗尽后应立即从口袋补满");
        player.setGameMode(GameType.CREATIVE);
        BuildingRodItem.consume(player, rod, 4000);
        helper.assertTrue(energy(rod) == 8_000_000 && rod.getItem().getBarWidth(rod) == 13
            && rod.getItem().getBarColor(rod) == 0x7087FF, "创造模式不扣电，电量条应保持源版宽度和颜色");
        helper.succeed();
    }

    private static void hands(GameTestHelper helper) {
        var player = player(helper);
        var rod = ModItems.BUILDING_ROD.asStack();
        var stone = new ItemStack(Items.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, rod);
        player.setItemInHand(InteractionHand.OFF_HAND, stone);
        helper.assertTrue(BuildingRodItem.isHeld(player) && BuildingRodItem.material(player) == stone
            && BuildingRodItem.heldRod(player) == rod && BuildingRodItem.materialHand(player) == InteractionHand.OFF_HAND,
            "主手持杖应从副手读取材料");
        player.setItemInHand(InteractionHand.MAIN_HAND, stone);
        player.setItemInHand(InteractionHand.OFF_HAND, rod);
        helper.assertTrue(BuildingRodItem.material(player) == stone && BuildingRodItem.materialHand(player) == InteractionHand.MAIN_HAND,
            "副手持杖应从主手读取材料");
        helper.assertTrue(BuildingRodItem.isPlacementMaterial(new ItemStack(Items.WATER_BUCKET))
            && BuildingRodItem.isPlacementMaterial(ModItems.FILTER.asStack())
            && !BuildingRodItem.isPlacementMaterial(new ItemStack(Items.BUCKET))
            && !BuildingRodItem.isPlacementMaterial(new ItemStack(Items.COD_BUCKET)), "普通流体桶和过滤器可用，空桶及生物桶不可用");
        helper.succeed();
    }

    private static void reach(GameTestHelper helper) {
        var player = player(helper);
        var block = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        var entity = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        double baseBlock = block.getValue();
        double baseEntity = entity.getValue();
        player.getInventory().setItem(10, ModItems.BUILDING_ROD.asStack());
        BuildingRodItem.updateReach(player);
        helper.assertTrue(block.getValue() == baseBlock + 3 && entity.getValue() == baseEntity + 3,
            "只在背包携带也应获得蟹钳范围效果");
        player.setItemInHand(InteractionHand.MAIN_HAND, ModItems.BUILDING_ROD.asStack());
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE));
        BuildingRodItem.updateReach(player);
        var modifier = block.getModifier(AnvilCraft.of("building_rod_reach"));
        BuildingRodItem.updateReach(player);
        helper.assertTrue(block.getValue() == baseBlock + 15 && entity.getValue() == baseEntity + 3
            && block.getModifier(AnvilCraft.of("building_rod_reach")) == modifier, "建造材料只扩大方块范围，稳定 tick 不应反复替换修饰器");
        player.getInventory().clearContent();
        player.setItemSlot(EquipmentSlot.LEGS, ModItems.POCKETS_LEGGINGS.asStack());
        PocketInventory.get(player).setItem(0, ModItems.BUILDING_ROD.asStack());
        block.addTransientModifier(CrabClawItem.RANGE_ATTRIBUTE_MODIFIER);
        entity.addTransientModifier(CrabClawItem.RANGE_ATTRIBUTE_MODIFIER);
        BuildingRodItem.updateReach(player);
        helper.assertTrue(block.getValue() == baseBlock + 3 && entity.getValue() == baseEntity + 3,
            "口袋建筑杖与已有蟹钳修饰器不能叠加成双倍");
        PocketInventory.get(player).setItem(0, ItemStack.EMPTY);
        block.removeModifier(CrabClawItem.RANGE_ATTRIBUTE_MODIFIER.id());
        entity.removeModifier(CrabClawItem.RANGE_ATTRIBUTE_MODIFIER.id());
        BuildingRodItem.updateReach(player);
        helper.assertTrue(block.getValue() == baseBlock && entity.getValue() == baseEntity, "移除建筑杖后应清除自己的范围增益");
        helper.succeed();
    }

    private static void recipes(GameTestHelper helper) {
        for (boolean charged : new boolean[]{false, true}) {
            var key = ResourceKey.create(Registries.RECIPE, AnvilCraft.of(charged ? "building_rod_charged" : "building_rod"));
            var holder = helper.getLevel().getServer().getRecipeManager().recipeMap().byKey(key);
            helper.assertTrue(holder != null && holder.value() instanceof ShapedRecipe, "两种建筑杖配方都应实际注册");
            var input = CraftingInput.of(3, 3, List.of(
                ModBlocks.SMART_BLOCK_PLACER.asStack(), ModBlocks.MAGNETO_ELECTRIC_CORE_BLOCK.asStack(),
                ModBlocks.SMART_BLOCK_PLACER.asStack(),
                ModBlocks.SMART_BLOCK_PLACER.asStack(), ModItems.ANVIL_HAMMER.asStack(), ModBlocks.SMART_BLOCK_PLACER.asStack(),
                ItemStack.EMPTY, charged ? ModItems.CAPACITOR.asStack() : ModItems.CAPACITOR_EMPTY.asStack(), ItemStack.EMPTY));
            var recipe = (ShapedRecipe) holder.value();
            helper.assertTrue(recipe.matches(input, helper.getLevel()), "配方材料和形状应与源版一致");
            var result = recipe.assemble(input);
            helper.assertTrue(result.is(ModItems.BUILDING_ROD) && energy(result) == (charged ? 8_000_000 : 0),
                "满/空电容配方必须给出不同的初始电量");
        }
        helper.succeed();
    }

    private static void tooltip(GameTestHelper helper) {
        var rod = ModItems.BUILDING_ROD.get().creativeStack();
        var lines = rod.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), null, TooltipFlag.NORMAL);
        helper.assertTrue(lines.size() > 2 && lines.get(1).getContents() instanceof TranslatableContents text
            && text.getKey().equals("tooltip.anvilcraft.property.stored_energy"), "电量行必须直接位于物品名称下方");
        long energyLines = lines.stream().filter(line -> line.getContents() instanceof TranslatableContents text
            && text.getKey().equals("tooltip.anvilcraft.property.stored_energy")).count();
        helper.assertTrue(energyLines == 1 && !dev.dubhe.anvilcraft.api.tooltip.ItemTooltipManager.getShiftMap().containsKey(rod.getItem()),
            "电量提示不能重复，也不能添加已移除的 Shift 专属说明");
        helper.succeed();
    }

    private static void undo(GameTestHelper helper) {
        var player = player(helper);
        var pos = helper.absolutePos(new BlockPos(2, 16, 4));
        var group = new BuildingPlan.Group();
        group.cells.add(new BuildingPlan.Cell(pos, Blocks.STONE.defaultBlockState(), new CompoundTag(), List.of()));
        group.materials.add(new ItemStack(Items.STONE));
        var history = new BuildingRodUndo(player, List.of(group));
        BuildingCommit.quietly(helper.getLevel(), () -> BuildingCommit.set(helper.getLevel(), pos, Blocks.STONE.defaultBlockState()));
        history.finish(player);
        BuildingRodUndo.undo(player);
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.STONE), "未持杖不能通过公开撤销入口修改世界");
        player.setItemInHand(InteractionHand.OFF_HAND, ModItems.BUILDING_ROD.asStack());
        BuildingRodUndo.undo(player);
        helper.assertTrue(helper.getLevel().getBlockState(pos).isAir() && player.getInventory().countItem(Items.STONE) == 1,
            "副手持杖应通过真实入口执行撤销和退款");
        helper.succeed();
    }
}
