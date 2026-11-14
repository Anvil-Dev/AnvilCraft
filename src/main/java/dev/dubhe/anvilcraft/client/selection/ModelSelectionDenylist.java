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

public final class ModelSelectionDenylist {
    public static final ResourceLocation LOCATION = AnvilCraft.of("model_selection_denylist.json");
    private static Set<Block> blocks = Set.of();

    private ModelSelectionDenylist() {
    }

    public static void reload(ResourceManager resources) {
        Set<Block> loaded = Set.of();
        for (Resource resource : resources.getResourceStack(LOCATION)) {
            try (Reader reader = resource.openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                loaded = readBlocks(json, "disable_ber_selection");
            } catch (IOException | RuntimeException exception) {
                AnvilCraft.LOGGER.warn("Unable to load {} from {}; keeping lower-priority denylist", LOCATION,
                    resource.sourcePackId(), exception);
            }
        }
        blocks = loaded;
    }

    public static boolean excludesBlockEntity(Block block) {
        return blocks.contains(block);
    }

    private static Set<Block> readBlocks(JsonObject json, String name) {
        if (!json.has(name)) return Set.of();
        Set<Block> blocks = new HashSet<>();
        for (JsonElement entry : GsonHelper.getAsJsonArray(json, name)) {
            ResourceLocation id = ResourceLocation.parse(GsonHelper.convertToString(entry, name));
            if (!AnvilCraft.MOD_ID.equals(id.getNamespace())) {
                throw new IllegalArgumentException("Model selection denylist only supports anvilcraft blocks: " + id);
            }
            blocks.add(BuiltInRegistries.BLOCK.getOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown block in model selection denylist: " + id)));
        }
        return Set.copyOf(blocks);
    }
}
