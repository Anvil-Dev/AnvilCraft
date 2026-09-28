package dev.dubhe.anvilcraft.porting;

import com.google.gson.GsonBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CreativeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.entity.FluidTankMinecartEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SingleTankRenderClientScene {
    private static final BlockPos POS = new BlockPos(0, 162, 0);
    private static final String[] CASES = {"empty", "water-one", "water-250", "water-full", "gas-quarter", "milk-half",
        "creative-water", "creative-gas", "creative-milk"};
    private static final Map<String, Object> RESULTS = new LinkedHashMap<>();
    private static int index;
    private static int stage;
    private static volatile int cartId = -1;
    private static long next;
    private static long deadline;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (deadline == 0) {
            deadline = System.currentTimeMillis() + 180000;
            client.options.guiScale().set(2);
            client.resizeGui();
        }
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Single tank scene " + index + ":" + stage);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        if (stage == 0) {
            stage = 1;
            next = System.currentTimeMillis() + 5000;
            client.setScreen(null);
            cartId = -1;
            client.getSingleplayerServer().execute(() -> prepare(client));
        } else if (stage == 1) {
            if (cartId < 0 || !(client.level.getEntity(cartId) instanceof FluidTankMinecartEntity cart)
                || !client.levelRenderer.isSectionCompiledAndVisible(POS)) return;
            RESULTS.put(CASES[index], SingleTankRenderProbe.capture(client, POS, cart));
            AnvilCraft.LOGGER.info("PORT_SINGLE_TANK_RENDER_CASE: {}", CASES[index]);
            capturing = true;
            Screenshot.grab(client.gameDirectory, "single-tank-26.1-" + CASES[index] + ".png", client.getMainRenderTarget(), 1,
                message -> client.execute(() -> {
                    capturing = false;
                    if (++index == CASES.length) {
                        stage = 2;
                        next = System.currentTimeMillis() + 3000;
                        client.setScreen(new LargeTankItemGallery(SingleTankRenderProbe.ITEMS));
                    } else stage = 0;
                }));
        } else if (stage == 2) {
            stage = 3;
            capturing = true;
            Screenshot.grab(client.gameDirectory, "single-tank-26.1-items.png", client.getMainRenderTarget(), 1,
                message -> client.execute(() -> save(client)));
        }
    }

    private static void prepare(Minecraft client) {
        var server = client.getSingleplayerServer();
        ServerLevel level = server.overworld();
        level.getEntitiesOfClass(FluidTankMinecartEntity.class, new AABB(POS).inflate(10)).forEach(Entity::discard);
        level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
        boolean creative = index >= 6;
        level.setBlockAndUpdate(POS, creative ? ModBlocks.CREATIVE_FLUID_TANK.getDefaultState() : ModBlocks.FLUID_TANK.getDefaultState());
        var fluid = index == 4 || index == 7 ? ModFluids.HYDROGEN.get()
            : index == 5 || index == 8 ? NeoForgeMod.MILK.get() : Fluids.WATER;
        int amount = switch (index) {
            case 0 -> 0;
            case 1 -> 1;
            case 2 -> 250;
            case 4 -> 4000;
            case 5 -> 8000;
            default -> 16000;
        };
        FluidStack contents = amount == 0 ? FluidStack.EMPTY : new FluidStack(fluid, amount);
        var cart = new FluidTankMinecartEntity(ModEntities.FLUID_TANK_MINECART.get(), level);
        cart.setPos(3.5, 162, 0.5);
        cart.setNoGravity(true);
        if (creative) {
            ((CreativeFluidTankBlockEntity) level.getBlockEntity(POS)).getFluidHandler().replaceStacks(List.of(contents));
        }
        try (Transaction tx = Transaction.openRoot()) {
            if (amount > 0) {
                if (!creative) {
                    ((FluidTankBlockEntity) level.getBlockEntity(POS)).getFluidHandler().insert(FluidResource.of(contents), amount, tx);
                }
                cart.getFluidHandler().insert(FluidResource.of(contents), amount, tx);
            }
            tx.commit();
        }
        level.addFreshEntity(cart);
        cartId = cart.getId();
        level.sendBlockUpdated(POS, level.getBlockState(POS), level.getBlockState(POS), 3);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 162 4.5 180 16");
        server.getPlayerList().getPlayers().forEach(player -> {
            player.setNoGravity(true);
            player.getInventory().clearContent();
            player.containerMenu.broadcastChanges();
        });
    }

    private static void save(Minecraft client) {
        try {
            Files.writeString(client.gameDirectory.toPath().resolve("single-tank-render-26.1.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(RESULTS));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        AnvilCraft.LOGGER.info("PORT_SINGLE_TANK_RENDER_PASSED: {} cases", RESULTS.size());
        client.stop();
    }
}
