package dev.dubhe.anvilcraft.client.selection;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

public final class ModelSelectionBlacklist {
    public static final ResourceLocation LOCATION = AnvilCraft.of("model_selection_blacklist.json");
    private static final Rules EMPTY = new Rules(Set.of(), Set.of(), Set.of());
    private static Rules rules = EMPTY;

    private ModelSelectionBlacklist() {
    }

    public static void reload(ResourceManager resources) {
        Rules loaded = EMPTY;
        for (Resource resource : resources.getResourceStack(LOCATION)) {
            try (Reader reader = resource.openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                loaded = new Rules(
                    readBlocks(json, "disable_precise_picking"),
                    readBlocks(json, "disable_model_outline"),
                    readBlocks(json, "disable_ber_selection")
                );
            } catch (IOException | RuntimeException exception) {
                AnvilCraft.LOGGER.warn("Unable to load {} from {}; keeping lower-priority blacklist", LOCATION,
                    resource.sourcePackId(), exception);
            }
        }
        rules = loaded;
    }

    public static boolean usesOriginalPicking(Block block) {
        return rules.picking().contains(block) || rules.outline().contains(block);
    }

    public static boolean usesOriginalOutline(Block block) {
        return rules.outline().contains(block);
    }

    public static boolean excludesBlockEntity(Block block) {
        return rules.blockEntity().contains(block);
    }

    private static Set<Block> readBlocks(JsonObject json, String name) {
        if (!json.has(name)) return Set.of();
        Set<Block> blocks = new HashSet<>();
        for (JsonElement entry : GsonHelper.getAsJsonArray(json, name)) {
            ResourceLocation id = ResourceLocation.parse(GsonHelper.convertToString(entry, name));
            if (!AnvilCraft.MOD_ID.equals(id.getNamespace())) {
                throw new IllegalArgumentException("Model selection blacklist only supports anvilcraft blocks: " + id);
            }
            blocks.add(BuiltInRegistries.BLOCK.getOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown block in model selection blacklist: " + id)));
        }
        return Set.copyOf(blocks);
    }

    private record Rules(Set<Block> picking, Set<Block> outline, Set<Block> blockEntity) {
    }
}
