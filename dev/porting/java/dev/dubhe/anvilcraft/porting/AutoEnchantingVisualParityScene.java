package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import dev.dubhe.anvilcraft.client.gui.screen.AutoEnchantingTableScreen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.AutoEnchantingTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class AutoEnchantingVisualParityScene {
    private static final String VERSION = "26.1";
    private static final String[] NAMES = {"random", "primer-pickaxe", "primer-book", "liquid", "curse", "liquid-15",
        "world-open", "world-closed", "world-curse", "jade-progress", "pickaxe-no-glint", "book-no-glint",
        "pickaxe-solid", "book-solid", "pickaxe-solid-no-glint", "book-solid-no-glint", "binding"};
    private static final BlockPos POS = new BlockPos(8, 81, 8);
    private static boolean creating;
    private static boolean preparing;
    private static volatile boolean prepared;
    private static volatile int supplied = -1;
    private static volatile Throwable failure;
    private static boolean capturing;
    private static int stage;
    private static int phase;
    private static long next;
    private static long deadline;

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void before(RenderFrameEvent.Pre event) {
        if (!Boolean.getBoolean("anvilcraft.portAutoEnchantingVisualParityScene")) return;
        var client = Minecraft.getInstance();
        if (client.level == null || !prepared) return;
        client.level.setTimeFromServer(500);
        if (stage >= 10 && client.screen instanceof AutoEnchantingTableScreen screen) {
            var ghost = (ItemStack) read(screen, "ghostOutput");
            if (ghost != null) ghost.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
        }
        if (client.level.getBlockEntity(POS) instanceof AutoEnchantingTableBlockEntity entity) {
            entity.setBookOpen(stage == 7 ? 0 : 1);
            entity.setBookHeight(stage == 7 ? 1.3F : 1);
            freezeBookPose(client, entity);
        }
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) {
        if (!Boolean.getBoolean("anvilcraft.portAutoEnchantingVisualParityScene")) return;
        var client = Minecraft.getInstance();
        client.options.pauseOnLostFocus = false;
        client.options.guiScale().set(2);
        client.options.fov().set(70);
        client.options.bobView().set(false);
        client.options.glintSpeed().set(0.0);
        client.options.glintStrength().set(stage == 10 || stage == 11 || stage == 14 || stage == 15 ? 0.0 : 0.75);
        if (!creating) {
            if (client.screen == null || client.getOverlay() != null) return;
            creating = true;
            String name = "enchanting-parity-" + System.currentTimeMillis();
            client.createWorldOpenFlows().createFreshLevel(name,
                new LevelSettings(name, GameType.CREATIVE,
                    new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT),
                new WorldOptions(121261L, false, false), WorldPresets::createFlatWorldDimensions, client.screen);
            return;
        }
        if (client.level == null || client.player == null || client.getSingleplayerServer() == null) return;
        if (!preparing) {
            preparing = true;
            deadline = System.currentTimeMillis() + 150000;
            server(client, () -> {
                var server = client.getSingleplayerServer();
                for (int x = 3; x < 14; x++) {
                    for (int z = 3; z < 14; z++) {
                        server.overworld().setBlockAndUpdate(new BlockPos(x, 80, z), Blocks.SMOOTH_STONE.defaultBlockState());
                    }
                }
                server.overworld().setBlockAndUpdate(POS, ModBlocks.AUTO_ENCHANTING_TABLE.getDefaultState());
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 8.5 81 11.5 180 25");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
                server.getPlayerList().getPlayers().getFirst().setNoGravity(true);
                prepared = true;
            });
        }
        if (failure != null || System.currentTimeMillis() > deadline) {
            throw new IllegalStateException("Enchanting parity " + stage, failure);
        }
        if (!prepared || capturing || System.currentTimeMillis() < next) return;
        if (!(client.level.getBlockEntity(POS) instanceof AutoEnchantingTableBlockEntity)) return;
        client.getToastManager().clear();
        client.gui.getChat().clearMessages(false);
        if (stage == NAMES.length) {
            AnvilCraft.LOGGER.info("PORT_ENCHANTING_PARITY_PASSED {}: {} scenes", VERSION, stage);
            client.stop();
            return;
        }
        if (phase == 0) {
            client.setScreen(null);
            if (stage < 6 || stage >= 10) {
                var machine = (AutoEnchantingTableBlockEntity) ModBlocks.AUTO_ENCHANTING_TABLE.get().newBlockEntity(
                    POS, ModBlocks.AUTO_ENCHANTING_TABLE.getDefaultState());
                machine.setLevel(client.level);
                supply(machine, stage == 16 ? 10 : stage >= 10 ? (stage - 10) % 2 + 1 : stage);
                if (stage >= 12 && stage <= 15) {
                    var enchantment = client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
                    var input = (stage % 2 == 0 ? Items.DIAMOND_PICKAXE : Items.BOOK).getDefaultInstance();
                    setItem(machine, 1, AutoEnchantingTableBlockEntity.computePrimerEnchantResult(input, java.util.List.of(enchantment)));
                }
                var menu = new AutoEnchantingTableMenu(null, 123, client.player.getInventory(), machine);
                client.setScreen(new AutoEnchantingTableScreen(menu, client.player.getInventory(), machine.getDisplayName()));
                supplied = stage;
            } else {
                client.options.hideGui = stage != 9;
                client.player.setPos(8.5, 81, 11.5);
                client.player.setYRot(180);
                client.player.setXRot(25);
                int current = stage;
                server(client, () -> {
                    var server = client.getSingleplayerServer();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick freeze");
                    var machine = (AutoEnchantingTableBlockEntity) server.overworld().getBlockEntity(POS);
                    supply(machine, current);
                    machine.syncToClient();
                    supplied = current;
                });
            }
            phase = 1;
            next = System.currentTimeMillis() + 1400;
            return;
        }
        if (supplied != stage) return;
        if (stage >= 6 && stage < 10) {
            var entity = (AutoEnchantingTableBlockEntity) client.level.getBlockEntity(POS);
            var renderer = client.getBlockEntityRenderDispatcher().getRenderer(entity);
            AnvilCraft.LOGGER.info("PORT_ENCHANTING_POSE {} {}: {}", VERSION, NAMES[stage],
                ((java.util.Map<?, ?>) read(renderer, "poses")).get(entity));
        }
        capturing = true;
        Screenshot.grab(client.gameDirectory, "enchanting-parity-" + VERSION + "-" + NAMES[stage] + ".png",
            client.getMainRenderTarget(), 1, message -> client.execute(() -> {
                AnvilCraft.LOGGER.info("PORT_ENCHANTING_PARITY {} {}", VERSION, NAMES[stage]);
                capturing = false;
                stage++;
                phase = 0;
            }));
    }

    private static void supply(AutoEnchantingTableBlockEntity machine, int variant) {
        var registry = machine.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        final var mending = registry.getOrThrow(Enchantments.MENDING);
        var mode = variant == 0 ? AutoEnchantingTableBlockEntity.WorkMode.ENCHANTING
            : variant < 3 ? AutoEnchantingTableBlockEntity.WorkMode.PRIMER : AutoEnchantingTableBlockEntity.WorkMode.LIQUID_ENCHANTMENT;
        set(machine, "workMode", mode);
        setItem(machine, 0, variant == 7 ? ItemStack.EMPTY : (variant == 2 ? Items.BOOK : Items.DIAMOND_PICKAXE).getDefaultInstance());
        setItem(machine, 1, variant == 7 ? Items.DIAMOND_PICKAXE.getDefaultInstance() : ItemStack.EMPTY);
        setItem(machine, 2, variant == 0 ? ItemStack.EMPTY
            : variant < 3 ? ModItems.EMERALD_AMULET.asStack() : ModBlocks.TRANSCENDENCE_ANVIL.asStack());
        FluidStack fluid = new FluidStack(variant < 3 ? ModFluids.EXP_FLUID.get() : ModFluids.LIQUID_ENCHANTMENT.get(), 20000);
        if (variant >= 3) {
            fluid.set(ModComponents.LIQUID_ENCHANTMENT,
                registry.getOrThrow(variant == 10 ? Enchantments.BINDING_CURSE
                    : variant == 4 || variant == 8 ? Enchantments.VANISHING_CURSE : Enchantments.EFFICIENCY));
        }
        setFluid(machine, fluid);
        if (variant >= 3) {
            var extension = (dev.dubhe.anvilcraft.util.LiquidEnchantmentClientFluidTypeExtension)
                net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid.getFluid());
            AnvilCraft.LOGGER.info("PORT_ENCHANTING_FLUID {} {}: {} {}", VERSION, variant,
                Integer.toHexString(extension.getTintColor(fluid)), java.util.Arrays.toString(extension.getLayerColors(fluid)));
        }
        machine.selectEnchantment(mending);
        set(machine, "shelfLevel", 15);
        set(machine, "liquidEnchantmentLevel", variant == 5 ? 15 : 5);
        set(machine, "cooldownTicks", variant == 9 ? 40 : 80);
    }

    private static void setItem(AutoEnchantingTableBlockEntity machine, int slot, ItemStack stack) {
        machine.getItemHandler().set(slot, ItemResource.of(stack), stack.getCount());
    }

    private static void setFluid(AutoEnchantingTableBlockEntity machine, FluidStack stack) {
        machine.getFluidTank().set(0, FluidResource.of(stack), stack.getAmount());
    }

    @SuppressWarnings("unchecked")
    private static void freezeBookPose(Minecraft client, AutoEnchantingTableBlockEntity entity) {
        var renderer = client.getBlockEntityRenderDispatcher().getRenderer(entity);
        try {
            var type = Class.forName(renderer.getClass().getName() + "$BookPose");
            var constructor = type.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            long frame = dev.dubhe.anvilcraft.client.selection.ModelBlockSelection.frame();
            Object[] values = constructor.getParameterCount() == 3
                ? new Object[]{frame, 501F, entity.getBookOpen()}
                : new Object[]{frame, 501F, entity.getBookOpen(), entity.getBookHeight()};
            ((java.util.Map<Object, Object>) read(renderer, "poses")).put(entity, constructor.newInstance(values));
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static Object read(Object owner, String name) {
        try {
            var field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void set(Object owner, String name, Object value) {
        try {
            var field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(owner, value);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void server(Minecraft client, Runnable action) {
        client.getSingleplayerServer().execute(() -> {
            try {
                action.run();
            } catch (Throwable error) {
                failure = error;
            }
        });
    }
}
