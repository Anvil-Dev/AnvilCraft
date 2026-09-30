package dev.dubhe.anvilcraft.porting;

import com.google.gson.GsonBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.JewelCraftingMenu;
import dev.dubhe.anvilcraft.inventory.component.jewel.JewelInputSlot;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JewelClientScene {
    private static final List<Item> SAMPLES = List.of(Items.FLOWER_BANNER_PATTERN, Items.MUSIC_DISC_CREATOR,
        Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.ANGLER_POTTERY_SHERD, Items.TOTEM_OF_UNDYING, Items.HEAVY_CORE);
    private static final Map<String, Object> RESULTS = new LinkedHashMap<>();
    private static int index;
    private static int stage;
    private static volatile int prepared = -1;
    private static boolean capturing;
    private static long next;
    private static long deadline;

    public static void frame(Minecraft client) {
        if (stage == 3) return;
        if (deadline == 0) deadline = System.currentTimeMillis() + 240000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Jewel client timed out at " + index);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        if (RecipesRecord.CLIENTSIDE == null || RecipesRecord.CLIENTSIDE.values().isEmpty()) return;
        if (stage == 0) {
            client.options.guiScale().set(2);
            client.resizeGui();
            stage = 1;
            client.getSingleplayerServer().execute(() -> prepare(client));
            next = System.currentTimeMillis() + 1200;
        } else if (stage == 1) {
            if (prepared != index || !(client.player.containerMenu instanceof JewelCraftingMenu menu)) return;
            var sample = SAMPLES.get(index / 2);
            if (!menu.getSlot(1).getItem().is(sample)) return;
            boolean filled = index % 2 == 1;
            var result = menu.getSlot(0).getItem();
            if (filled && !result.is(sample)) return;
            if (!filled && !result.isEmpty()) throw new IllegalStateException("Unfunded jewel recipe produced output");
            boolean curse = result.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                .getLevel(client.level.registryAccess().holderOrThrow(Enchantments.VANISHING_CURSE)) > 0;
            if (curse != (filled && index / 2 >= 4)) throw new IllegalStateException("Wrong visible curse flag");
            if (result.get(DataComponents.CUSTOM_NAME) != null) throw new IllegalStateException("Prototype name leaked to preview");
            var counts = java.util.stream.IntStream.range(2, 6)
                .map(slot -> ((JewelInputSlot) menu.getSlot(slot)).getHintCount()).boxed().toList();
            RESULTS.put(BuiltInRegistries.ITEM.getKey(sample).toString() + (filled ? "-filled" : "-hints"),
                Map.of("counts", counts, "resultCount", result.getCount(), "curse", curse,
                    "sampleCount", menu.getSlot(1).getItem().getCount()));
            stage = 2;
            next = System.currentTimeMillis() + 300;
        } else if (stage == 2) {
            capturing = true;
            Screenshot.grab(client.gameDirectory, "jewel-source-26.1-" + index + ".png", client.getMainRenderTarget(), 1,
                message -> client.execute(() -> {
                    capturing = false;
                    if (++index < SAMPLES.size() * 2) stage = 0;
                    else save(client);
                }));
        }
    }

    private static void prepare(Minecraft client) {
        var server = client.getSingleplayerServer();
        final var level = server.overworld();
        var player = server.getPlayerList().getPlayers().getFirst();
        player.closeContainer();
        player.getInventory().clearContent();
        player.setNoGravity(true);
        BlockPos pos = new BlockPos(0, 162, 0);
        level.setBlockAndUpdate(pos, ModBlocks.JEWEL_CRAFTING_TABLE.getDefaultState());
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 162 2.5 180 0");
        player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new JewelCraftingMenu(ModMenuTypes.JEWEL_CRAFTING.get(),
            id, inventory, ContainerLevelAccess.create(level, pos)), Component.literal("Jewel crafting")));
        var menu = (JewelCraftingMenu) player.containerMenu;
        var sample = new ItemStack(SAMPLES.get(index / 2));
        sample.set(DataComponents.CUSTOM_NAME, Component.literal("Prototype"));
        menu.getSlot(1).set(sample);
        var recipe = menu.findRecipeBySource(sample).value();
        if (index % 2 == 1) {
            for (int slot = 0; slot < recipe.mergedIngredients().size(); slot++) {
                var entry = recipe.mergedIngredients().get(slot);
                menu.getSlot(slot + 2).set(new ItemStack(entry.getKey().getValues().get(0).value(), entry.getIntValue()));
            }
        }
        menu.broadcastChanges();
        prepared = index;
    }

    private static void save(Minecraft client) {
        stage = 3;
        try {
            Files.writeString(client.gameDirectory.toPath().resolve("jewel-source-26.1.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(RESULTS));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        AnvilCraft.LOGGER.info("PORT_JEWEL_CLIENT_PASSED: {} scenarios", RESULTS.size());
        client.stop();
    }
}
