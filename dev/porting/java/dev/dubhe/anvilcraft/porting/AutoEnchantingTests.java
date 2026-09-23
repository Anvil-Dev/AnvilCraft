package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.fluid.FluidHandlerWrapper;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.AutoEnchantingTableBlock;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class AutoEnchantingTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_auto_enchant_transactions", AutoEnchantingTests::transactions,
        "port_auto_enchant_random", AutoEnchantingTests::random,
        "port_auto_enchant_primer", AutoEnchantingTests::primer,
        "port_auto_enchant_liquid", AutoEnchantingTests::liquid,
        "port_auto_enchant_reload", AutoEnchantingTests::reload,
        "port_auto_enchant_limits", AutoEnchantingTests::limits,
        "port_auto_enchant_container", AutoEnchantingTests::container,
        "port_auto_enchant_removal", AutoEnchantingTests::removal,
        "port_auto_enchant_overlimit", AutoEnchantingTests::overlimit
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_auto_enchanting"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    private static AutoEnchantingTableBlockEntity machine(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.AUTO_ENCHANTING_TABLE.getDefaultState());
        var machine = (AutoEnchantingTableBlockEntity) helper.getLevel().getBlockEntity(pos);
        machine.setGrid(new PowerGrid(helper.getLevel()));
        return machine;
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static void input(AutoEnchantingTableBlockEntity machine, ItemStack stack) {
        machine.getItemHandler().set(0, ItemResource.of(stack), 1);
    }

    private static void tick(AutoEnchantingTableBlockEntity machine, int count) {
        for (int i = 0; i < count; i++) {
            AutoEnchantingTableBlockEntity.tick(machine.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
        }
    }

    private static void fluid(AutoEnchantingTableBlockEntity machine, FluidStack stack) {
        machine.getFluidTank().set(0, FluidResource.of(stack), stack.getAmount());
    }

    private static void shelves(AutoEnchantingTableBlockEntity machine) {
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            machine.getLevel().setBlockAndUpdate(machine.getBlockPos().offset(offset), Blocks.BOOKSHELF.defaultBlockState());
        }
    }

    private static void transactions(GameTestHelper helper) {
        var machine = machine(helper);
        var automation = helper.getLevel().getCapability(Capabilities.Item.BLOCK, machine.getBlockPos(), null);
        helper.assertTrue(automation != null, "Registered automation capability");
        var enchanted = Items.DIAMOND_PICKAXE.getDefaultInstance();
        enchanted.enchant(enchantment(helper, Enchantments.EFFICIENCY), 3);
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(automation.insert(0, ItemResource.of(enchanted), 1, transaction) == 1, "Accept input in transaction");
            helper.assertTrue(machine.getItem(1).isEmpty(), "Insertion cannot pass through before commit");
        }
        helper.assertTrue(machine.getItem(0).isEmpty() && machine.getItem(1).isEmpty(), "Rollback preserves both slots");
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(automation.insert(2, ItemResource.of(ModItems.EMERALD_AMULET.asStack()), 1, transaction) == 0,
                "Automation cannot insert primer");
            automation.insert(0, ItemResource.of(enchanted), 1, transaction);
            transaction.commit();
        }
        helper.assertTrue(machine.getItem(0).isEmpty() && ItemStack.matches(machine.getItem(1), enchanted),
            "Commit passes enchanted input through");
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(automation.extract(0, ItemResource.of(enchanted), 1, transaction) == 0
                && automation.extract(1, ItemResource.of(enchanted), 1, transaction) == 1, "Automation extracts output only");
        }
        helper.assertTrue(!machine.getItem(1).isEmpty(), "Aborted output extraction restores output");
        helper.succeed();
    }

    private static void random(GameTestHelper helper) {
        var machine = machine(helper);
        shelves(machine);
        fluid(machine, new FluidStack(ModFluids.EXP_FLUID.get(), 32000));
        input(machine, Items.BOOK.getDefaultInstance());
        machine.getLevel().setBlockAndUpdate(machine.getBlockPos(),
            machine.getBlockState().setValue(AutoEnchantingTableBlock.POWERED, true));
        tick(machine, 90);
        helper.assertTrue(machine.getInputPower() == 0 && machine.getItem(1).isEmpty(), "Redstone stops work and power consumption");
        machine.getLevel().setBlockAndUpdate(machine.getBlockPos(),
            machine.getBlockState().setValue(AutoEnchantingTableBlock.POWERED, false));
        tick(machine, AnvilCraft.CONFIG.autoEnchantingTableInterval + 2);
        helper.assertTrue(machine.getItem(1).is(Items.ENCHANTED_BOOK)
            && EnchantmentHelper.hasAnyEnchantments(machine.getItem(1)), "Random mode enchants a book");
        helper.assertTrue(machine.getFluid().getAmount() == 32000 - machine.getShelfLevel() * 400, "Shelf-based experience cost");
        helper.succeed();
    }

    private static void primer(GameTestHelper helper) {
        var machine = machine(helper);
        shelves(machine);
        fluid(machine, new FluidStack(ModFluids.EXP_FLUID.get(), 32000));
        machine.getItemHandler().set(2, ItemResource.of(ModItems.EMERALD_AMULET.asStack()), 1);
        var selected = enchantment(helper, Enchantments.MENDING);
        machine.selectEnchantment(selected);
        input(machine, Items.DIAMOND_PICKAXE.getDefaultInstance());
        machine.onMenuOpen();
        tick(machine, 170);
        helper.assertTrue(machine.getItem(1).isEmpty() && machine.getFluid().getAmount() == 32000, "Primer mode pauses while menu is open");
        machine.onMenuClose();
        tick(machine, AnvilCraft.CONFIG.autoEnchantingTableInterval + 2);
        helper.assertTrue(EnchantmentHelper.getEnchantmentsForCrafting(machine.getItem(1)).getLevel(selected) == 1,
            "Primer mode applies selected enchantment through royal anvil rules");
        helper.assertTrue(machine.getFluid().getAmount() == 31600 && machine.getInputPower() == 64, "Primer cost and power");
        machine.getItemHandler().set(2, ItemResource.EMPTY, 0);
        helper.assertTrue(machine.getSelectedEnchantments().isEmpty(), "Removing primer clears remembered selection");
        helper.succeed();
    }

    private static void liquid(GameTestHelper helper) {
        var machine = machine(helper);
        var selected = enchantment(helper, Enchantments.EFFICIENCY);
        var liquid = new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), 100);
        liquid.set(ModComponents.LIQUID_ENCHANTMENT, selected);
        fluid(machine, liquid);
        tick(machine, 1);
        var pickaxe = Items.DIAMOND_PICKAXE.getDefaultInstance();
        pickaxe.enchant(enchantment(helper, Enchantments.UNBREAKING), 1);
        input(machine, pickaxe);
        helper.assertTrue(!machine.getItem(0).isEmpty() && machine.getItem(1).isEmpty(), "Liquid mode retains enchanted input");
        machine.setLiquidLevel(4);
        helper.assertTrue(machine.getInputPower() == 128, "Liquid power adds 64 per existing enchantment");
        tick(machine, AnvilCraft.CONFIG.autoEnchantingTableInterval + 2);
        helper.assertTrue(EnchantmentHelper.getEnchantmentsForCrafting(machine.getItem(1)).getLevel(selected) == 4
            && machine.getFluid().getAmount() == 92, "Liquid level four costs eight mB");
        helper.succeed();
    }

    private static void reload(GameTestHelper helper) {
        var machine = machine(helper);
        var selected = enchantment(helper, Enchantments.EFFICIENCY);
        var liquid = new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), 31);
        liquid.set(ModComponents.LIQUID_ENCHANTMENT, selected);
        fluid(machine, liquid);
        tick(machine, 1);
        var pickaxe = Items.DIAMOND_PICKAXE.getDefaultInstance();
        pickaxe.enchant(selected, 1);
        input(machine, pickaxe);
        machine.setLiquidLevel(3);
        var tag = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
        var loaded = (AutoEnchantingTableBlockEntity) ModBlocks.AUTO_ENCHANTING_TABLE.get()
            .newBlockEntity(machine.getBlockPos(), machine.getBlockState());
        loaded.setLevel(helper.getLevel());
        loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        helper.assertTrue(ItemStack.matches(loaded.getItem(0), pickaxe) && loaded.getItem(1).isEmpty()
            && loaded.getLiquidEnchantmentLevel() == 3 && loaded.getFluid().getAmount() == 31,
            "Loading liquid mode cannot prematurely pass enchanted input to output");
        final var legacy = tag.copy();
        final var inventory = new net.minecraft.nbt.CompoundTag();
        var items = new net.minecraft.nbt.ListTag();
        var savedItem = ItemStack.CODEC.encodeStart(helper.getLevel().registryAccess().createSerializationContext(
            net.minecraft.nbt.NbtOps.INSTANCE), pickaxe).getOrThrow();
        ((net.minecraft.nbt.CompoundTag) savedItem).putByte("Slot", (byte) 0);
        items.add(savedItem);
        inventory.put("Items", items);
        inventory.putInt("Size", 3);
        legacy.put("Inventory", inventory);
        legacy.put("FluidTank", FluidStack.CODEC.encodeStart(helper.getLevel().registryAccess().createSerializationContext(
            net.minecraft.nbt.NbtOps.INSTANCE), liquid).getOrThrow());
        loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), legacy));
        helper.assertTrue(ItemStack.matches(loaded.getItem(0), pickaxe) && loaded.getItem(1).isEmpty()
            && loaded.getFluid().getAmount() == 31, "Source 1.21 handler NBT retains inventory, fluid and liquid-mode input");
        helper.succeed();
    }

    private static void limits(GameTestHelper helper) {
        var silk = enchantment(helper, Enchantments.SILK_TOUCH);
        var fortune = enchantment(helper, Enchantments.FORTUNE);
        var pickaxe = Items.DIAMOND_PICKAXE.getDefaultInstance();
        pickaxe.enchant(fortune, 3);
        helper.assertTrue(AutoEnchantingTableBlockEntity.computeLiquidMaxLevel(pickaxe, ItemStack.EMPTY, silk) == 0,
            "Royal mode rejects incompatible enchantments");
        helper.assertTrue(AutoEnchantingTableBlockEntity.computeLiquidMaxLevel(pickaxe, ModBlocks.EMBER_ANVIL.asStack(), silk) == 1,
            "Ember primer permits conflicts within vanilla cap");
        helper.assertTrue(AutoEnchantingTableBlockEntity.computeLiquidMaxLevel(pickaxe, ModBlocks.TRANSCENDENCE_ANVIL.asStack(), silk)
            == AnvilCraft.CONFIG.liquidEnchantmentMaxLevel, "Transcendence primer permits configured overlimit levels");
        var book = AutoEnchantingTableBlockEntity.computePrimerEnchantResult(Items.BOOK.getDefaultInstance(), List.of(silk, fortune));
        var result = EnchantmentHelper.getEnchantmentsForCrafting(book);
        helper.assertTrue(result.getLevel(silk) == 1 && result.getLevel(fortune) == 3, "Primer books preserve conflicting selections");
        helper.succeed();
    }

    private static void overlimit(GameTestHelper helper) {
        var machine = machine(helper);
        var efficiency = enchantment(helper, Enchantments.EFFICIENCY);
        var liquid = new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), 32000);
        liquid.set(ModComponents.LIQUID_ENCHANTMENT, efficiency);
        fluid(machine, liquid);
        machine.getItemHandler().set(2, ItemResource.of(ModBlocks.TRANSCENDENCE_ANVIL.asStack()), 1);
        tick(machine, 1);
        input(machine, Items.STICK.getDefaultInstance());
        machine.setLiquidLevel(999);
        int selected = AnvilCraft.CONFIG.liquidEnchantmentMaxLevel;
        helper.assertTrue(machine.getLiquidEnchantmentLevel() == selected, "Network level is clamped to configured maximum");
        tick(machine, AnvilCraft.CONFIG.autoEnchantingTableInterval + 2);
        helper.assertTrue(EnchantmentHelper.getEnchantmentsForCrafting(machine.getItem(1)).getLevel(efficiency) == selected
            && machine.getFluid().getAmount() == 32000 - (1 << (selected - 1)), "Overlimit mode supports otherwise incompatible items");
        helper.succeed();
    }

    private static void removal(GameTestHelper helper) {
        var machine = machine(helper);
        input(machine, Items.BOOK.getDefaultInstance());
        machine.getItemHandler().set(1, ItemResource.of(Items.DIAMOND), 1);
        machine.getItemHandler().set(2, ItemResource.of(ModItems.EMERALD_AMULET.asStack()), 1);
        var pos = machine.getBlockPos();
        helper.getLevel().destroyBlock(pos, true);
        var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(pos).inflate(1));
        for (var item : List.of(Items.BOOK, Items.DIAMOND, ModItems.EMERALD_AMULET.get())) {
            int count = drops.stream().map(net.minecraft.world.entity.item.ItemEntity::getItem)
                .filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
            helper.assertTrue(count == 1, "Native removal drops each stored input/output/primer exactly once");
        }
        helper.succeed();
    }

    private static void container(GameTestHelper helper) {
        var machine = machine(helper);
        var wrapper = new FluidHandlerWrapper(machine.getFluidHandler());
        var bucket = ModFluids.EXP_FLUID.get().getBucket().getDefaultInstance();
        helper.assertTrue(wrapper.fillFromItem(bucket, true, null).is(Items.BUCKET) && machine.getFluid().isEmpty(),
            "Simulated fill does not mutate tank");
        helper.assertTrue(wrapper.fillFromItem(bucket, false, null).is(Items.BUCKET) && machine.getFluid().getAmount() == 1000,
            "Bucket fill uses native item replacement");
        helper.assertTrue(wrapper.drainToItem(Items.BUCKET.getDefaultInstance(), true).is(bucket.getItem())
            && machine.getFluid().getAmount() == 1000, "Simulated drain preserves fluid");
        helper.assertTrue(wrapper.drainToItem(Items.BUCKET.getDefaultInstance(), false).is(bucket.getItem())
            && machine.getFluid().isEmpty(), "Bucket drain transfers exactly one bucket");
        fluid(machine, new FluidStack(ModFluids.EXP_FLUID.get(), 31900));
        helper.assertTrue(wrapper.fillFromItem(bucket, false, null) == null && machine.getFluid().getAmount() == 31900,
            "Insufficient capacity rolls back container and fluid");
        helper.succeed();
    }
}
