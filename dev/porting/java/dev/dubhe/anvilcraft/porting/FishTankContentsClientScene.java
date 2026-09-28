package dev.dubhe.anvilcraft.porting;

import com.google.gson.GsonBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FishTankContentsClientScene {
    private static final BlockPos POS = new BlockPos(0, 162, 0);
    private static final String[] CASES = {"dry-items", "wet-items", "fish", "mixed", "empty-fire", "wet-fire", "hidden-fire"};
    private static final Map<String, Object> RESULTS = new LinkedHashMap<>();
    private static int index;
    private static int stage;
    private static long next;
    private static long deadline;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (deadline == 0) {
            deadline = System.currentTimeMillis() + 180000;
            client.options.hideGui = true;
        }
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Fish contents stage " + index + ":" + stage);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        if (stage == 0) {
            stage = 1;
            next = System.currentTimeMillis() + 4000;
            client.setScreen(null);
            client.getSingleplayerServer().execute(() -> {
                var server = client.getSingleplayerServer();
                var level = server.overworld();
                level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(POS, ModBlocks.FISH_TANK.getDefaultState());
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 162 2.7 180 15");
                server.getPlayerList().getPlayers().forEach(player -> player.setNoGravity(true));
            });
        } else if (stage == 1) {
            if (!(client.level.getBlockEntity(POS) instanceof FishTankBlockEntity tank)) return;
            configure(tank);
            FishTankContentsProbe.hideFire = index == 6;
            RESULTS.put(CASES[index], FishTankContentsProbe.capture(client, tank));
            AnvilCraft.LOGGER.info("PORT_FISH_CONTENTS_CASE: {}", CASES[index]);
            stage = 2;
            next = System.currentTimeMillis() + 250;
        } else if (stage == 2) {
            FishTankContentsProbe.clock(300);
            capturing = true;
            Screenshot.grab(client.gameDirectory, "fish-contents-26.1-" + CASES[index] + ".png", client.getMainRenderTarget(), 1,
                message -> client.execute(() -> {
                    capturing = false;
                    if (++index == CASES.length) save(client);
                    else stage = 0;
                }));
        }
    }

    private static void configure(FishTankBlockEntity tank) {
        boolean wet = index == 1 || index == 2 || index == 3 || index == 5 || index == 6;
        tank.getFluidHandler().set(wet ? FluidResource.of(Fluids.WATER) : FluidResource.EMPTY, wet ? 1000 : 0);
        for (int slot = 0; slot < tank.getItemHandler().size(); slot++) tank.getItemHandler().set(slot, ItemResource.EMPTY, 0);
        tank.getFishes().clear();
        if (index == 0 || index == 1 || index == 3) {
            tank.getItemHandler().set(0, ItemResource.of(Items.STONE), 9);
            tank.getItemHandler().set(1, ItemResource.of(Items.GRANITE), 17);
        }
        if (index == 2 || index == 3) {
            tank.getFishes().add(new FishTankBlockEntity.TropicalFishData(TropicalFish.Pattern.KOB, DyeColor.WHITE, DyeColor.WHITE));
            tank.getFishes().add(new FishTankBlockEntity.TropicalFishData(TropicalFish.Pattern.KOB, DyeColor.WHITE, DyeColor.WHITE));
        }
        tank.setIgnited(index >= 4);
    }

    private static void save(Minecraft client) {
        stage = 3;
        try {
            Files.writeString(client.gameDirectory.toPath().resolve("fish-contents-26.1.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(RESULTS));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        AnvilCraft.LOGGER.info("PORT_FISH_CONTENTS_PASSED: {} cases", RESULTS.size());
        client.stop();
    }
}
