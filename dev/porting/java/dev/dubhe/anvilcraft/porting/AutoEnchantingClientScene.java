package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import dev.dubhe.anvilcraft.client.gui.screen.AutoEnchantingTableScreen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;

public final class AutoEnchantingClientScene {
    private static final BlockPos POS = new BlockPos(8, 81, 8);
    private static int stage;
    private static long next;
    private static long deadline;
    private static boolean opening;
    private static boolean capturing;
    private static volatile boolean prepared;
    private static volatile boolean finished;
    private static boolean reloadRequested;
    private static volatile boolean reloaded;
    private static volatile Throwable failure;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (failure != null || System.currentTimeMillis() > deadline) throw new IllegalStateException("Enchanting scene " + stage, failure);
        if (capturing || System.currentTimeMillis() < next) return;
        client.options.guiScale().set(2);
        client.getToastManager().clear();
        client.gui.getChat().clearMessages(false);
        if (stage == 0) {
            server(client, () -> {
                var server = client.getSingleplayerServer();
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 8.5 82 10.5 180 10");
                server.getPlayerList().getPlayers().getFirst().setNoGravity(true);
                var level = server.overworld();
                level.setBlockAndUpdate(POS, ModBlocks.AUTO_ENCHANTING_TABLE.getDefaultState());
                var be = machine(client);
                be.setGrid(new PowerGrid(level));
                for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
                    level.setBlockAndUpdate(POS.offset(offset), Blocks.BOOKSHELF.defaultBlockState());
                }
                be.getItemHandler().set(2, ItemResource.of(ModItems.EMERALD_AMULET.asStack()), 1);
                be.getItemHandler().set(0, ItemResource.of(Items.DIAMOND_PICKAXE), 1);
                be.getFluidTank().set(0, FluidResource.of(ModFluids.EXP_FLUID.get()), 20000);
                tick(be, 1);
                prepared = true;
            });
            advance(1);
            return;
        }
        if (!prepared) return;
        if (stage == 1 && !opening) {
            if (!(client.level.getBlockEntity(POS) instanceof AutoEnchantingTableBlockEntity)) return;
            opening = true;
            open(client);
            next = System.currentTimeMillis() + 900;
            return;
        }
        if (stage == 10) {
            if (!finished) return;
            open(client);
            advance(11);
            return;
        }
        if (stage == 13) {
            capture(client, "world-output", 14);
            return;
        }
        if (stage == 14) {
            AnvilCraft.LOGGER.info(
                "PORT_AUTO_ENCHANTING_UI_PASSED: primer select/ghost, tank buckets, liquid scroll, pause/resume, output");
            client.stop();
            return;
        }
        if (!(client.screen instanceof AutoEnchantingTableScreen screen)) return;
        var be = screen.getMenu().getBlockEntity();
        switch (stage) {
            case 1 -> {
                if (be.getWorkMode() != AutoEnchantingTableBlockEntity.WorkMode.PRIMER || screen.getMenu().getEnchantments().isEmpty()) {
                    return;
                }
                ((EditBox) field(screen, "searchBox")).setValue("Mending");
                advance(2);
            }
            case 2 -> {
                check(((List<?>) field(screen, "filteredIndexes")).size() == 1, "Search narrows to Mending");
                click(screen, 49, 34, 0);
                advance(3);
            }
            case 3 -> {
                if (be.getSelectedEnchantments().isEmpty() || field(screen, "ghostOutput") == null) return;
                var ghost = (ItemStack) field(screen, "ghostOutput");
                check(ghost.is(Items.DIAMOND_PICKAXE) && EnchantmentHelper.hasAnyEnchantments(ghost),
                    "Selected result ghost carries enchantments");
                check(be.getItem(1).isEmpty(), "Open primer menu pauses output");
                capture(client, "primer-ghost", 4);
            }
            case 4 -> {
                if (!reloadRequested) {
                    reloadRequested = true;
                    client.reloadResourcePacks().whenComplete((ignored, error) -> client.execute(() -> {
                        failure = error;
                        reloaded = error == null;
                    }));
                    return;
                }
                if (!reloaded) return;
                check(field(screen, "ghostOutput") instanceof ItemStack ghost && !ghost.isEmpty(),
                    "Open menu and transparent item recover after resource reload");
                AnvilCraft.LOGGER.info("PORT_AUTO_ENCHANTING_RELOAD_PASSED");
                server(client, () -> {
                    var machine = machine(client);
                    machine.getFluidTank().set(0, FluidResource.of(ModFluids.EXP_FLUID.get()), 1000);
                    var menu = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst().containerMenu;
                    menu.setCarried(Items.BUCKET.getDefaultInstance());
                    menu.broadcastChanges();
                });
                advance(5);
            }
            case 5 -> {
                if (!screen.getMenu().getCarried().is(Items.BUCKET) || be.getFluid().getAmount() != 1000) return;
                click(screen, 157, 50, 1);
                advance(6);
            }
            case 6 -> {
                if (!be.getFluid().isEmpty() || !screen.getMenu().getCarried().is(ModFluids.EXP_FLUID.get().getBucket())) return;
                click(screen, 157, 50, 1);
                advance(7);
            }
            case 7 -> {
                if (be.getFluid().getAmount() != 1000 || !screen.getMenu().getCarried().is(Items.BUCKET)) return;
                server(client, () -> {
                    var machine = machine(client);
                    var level = client.getSingleplayerServer().overworld();
                    var fluid = new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), 10000);
                    fluid.set(ModComponents.LIQUID_ENCHANTMENT, level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.EFFICIENCY));
                    machine.getFluidTank().set(0, FluidResource.of(fluid), fluid.getAmount());
                    machine.getItemHandler().set(2, ItemResource.of(ModBlocks.TRANSCENDENCE_ANVIL.asStack()), 1);
                    machine.setGrid(new PowerGrid(level));
                    tick(machine, 1);
                    var menu = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst().containerMenu;
                    menu.setCarried(ItemStack.EMPTY);
                    menu.broadcastChanges();
                });
                advance(8);
            }
            case 8 -> {
                if (be.getWorkMode() != AutoEnchantingTableBlockEntity.WorkMode.LIQUID_ENCHANTMENT || be.computeLiquidMaxLevel() != 15) {
                    return;
                }
                var models = client.getModelManager().getFluidStateModelSet();
                check(dev.dubhe.anvilcraft.client.support.FluidRenderHelper.getModel(models, be.getFluid().getFluid())
                    != ((dev.dubhe.anvilcraft.mixin.accessor.FluidStateModelSetAccessor) models).getMissingModel(),
                    "Liquid enchantment must have a native fluid model");
                for (int i = 0; i < 5; i++) screen.mouseScrolled(screen.getLeftPos() + 50, screen.getTopPos() + 35, 0, 1);
                advance(9);
            }
            case 9 -> {
                if (be.getLiquidEnchantmentLevel() != 5) return;
                capture(client, "liquid-level", 15);
            }
            case 15 -> {
                screen.onClose();
                server(client, () -> {
                    var machine = machine(client);
                    machine.setGrid(new PowerGrid(client.getSingleplayerServer().overworld()));
                    client.getSingleplayerServer().getPlayerList().getPlayers().getFirst().closeContainer();
                    tick(machine, AnvilCraft.CONFIG.autoEnchantingTableInterval + 2);
                    var efficiency = machine.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.EFFICIENCY);
                    check(EnchantmentHelper.getEnchantmentsForCrafting(machine.getItem(1)).getLevel(efficiency) == 5
                        && machine.getFluid().getAmount() == 9984, "Server resumes selected liquid work after closing menu");
                    finished = true;
                });
                advance(10);
            }
            case 11 -> {
                if (be.getItem(1).isEmpty()) return;
                click(screen, 9, 54, 0);
                check(screen.getMenu().getCarried().isEmpty(), "Output remains read-only in GUI");
                capture(client, "output", 12);
            }
            case 12 -> {
                screen.onClose();
                client.player.setPos(11, 84, 13);
                client.player.setYRot(135);
                client.player.setXRot(30);
                advance(13);
            }
            default -> throw new IllegalStateException("Unknown enchanting stage " + stage);
        }
    }

    private static void tick(AutoEnchantingTableBlockEntity machine, int count) {
        for (int i = 0; i < count; i++) {
            AutoEnchantingTableBlockEntity.tick(machine.getLevel(), POS, machine.getBlockState(), machine);
        }
    }

    private static AutoEnchantingTableBlockEntity machine(Minecraft client) {
        return (AutoEnchantingTableBlockEntity) client.getSingleplayerServer().overworld().getBlockEntity(POS);
    }

    private static void open(Minecraft client) {
        server(client, () -> client.getSingleplayerServer().getPlayerList().getPlayers().getFirst().openMenu(machine(client), POS));
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

    private static Object field(Object owner, String name) {
        try {
            var field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void click(AutoEnchantingTableScreen screen, int x, int y, int button) {
        var event = new MouseButtonEvent(screen.getLeftPos() + x, screen.getTopPos() + y, new MouseButtonInfo(button, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int value) {
        stage = value;
        AnvilCraft.LOGGER.info("PORT_AUTO_ENCHANTING_STAGE {}", value);
        next = System.currentTimeMillis() + 1000;
    }

    private static void capture(Minecraft client, String name, int nextStage) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "auto-enchanting-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(nextStage);
            }));
    }
}
