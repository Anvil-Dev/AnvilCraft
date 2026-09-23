package dev.dubhe.anvilcraft.client.support;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.DefinitionSerialization;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.StructureScannerBlockEntity;
import dev.dubhe.anvilcraft.building.BlueprintNormalizer;
import dev.dubhe.anvilcraft.building.ConstructionBlueprintException;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.building.StructureSnapshotCodec;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.item.property.component.StoredItem;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockConversionRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockUtil;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import dev.dubhe.anvilcraft.util.StructureSaveUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class DiskDisplaySupport {
    private static final Cache<CompoundTag, Map<StructureDiskData, ItemStack>> RESULTS =
        CacheBuilder.newBuilder().weakKeys().maximumSize(64).build();
    private static final Map<Integer, List<Pattern>> PATTERNS = new HashMap<>();
    private static boolean recipesLoaded;
    private static @Nullable RecipeMap recipes;

    private DiskDisplaySupport() {
    }

    public static ItemStack getImportedDisplay(StructureSnapshot snapshot) {
        var level = Minecraft.getInstance().level;
        if (level == null) return ItemStack.EMPTY;
        if (!recipesLoaded) {
            if (recipes == null) return ItemStack.EMPTY;
            reloadRecipes(recipes);
        }
        List<Pattern> choices = PATTERNS.get(snapshot.size().getX());
        if (choices == null) return ItemStack.EMPTY;
        StructureDiskData data = new StructureDiskData("", "", new UUID(0, 0), Direction.NORTH,
            snapshot.size().getX(), snapshot.size().getY(), snapshot.size().getZ(), false);
        return matchStructure(data, StructureSnapshotCodec.write(snapshot), level.registryAccess(), choices);
    }

    public static ItemStack getScannedDisplay(StructureScannerBlockEntity scanner) {
        var level = scanner.getLevel();
        if (level == null || !scanner.isScanComplete()) return ItemStack.EMPTY;
        if (!recipesLoaded) {
            if (recipes == null) return ItemStack.EMPTY;
            reloadRecipes(recipes);
        }
        try {
            var snapshot = StructureSaveUtil.buildSnapshot(scanner, scanner.getScannedBlocks()).snapshot();
            return getImportedDisplay(snapshot);
        } catch (IllegalArgumentException exception) {
            return ItemStack.EMPTY;
        }
    }

    public static ItemStack getDisplay(ItemStack stack) {
        StoredItem marker = stack.get(ModComponents.DISPLAY_ITEM);
        if (stack.is(ModItems.STRUCTURE_DISK) && marker != null) return marker.stored();
        if (stack.is(ModItems.DISK)) return recordedBlock(stack);
        if (!stack.is(ModItems.STRUCTURE_DISK)) return ItemStack.EMPTY;
        var level = Minecraft.getInstance().level;
        StructureDiskData data = stack.get(ModComponents.STRUCTURE_DISK_DATA);
        if (level == null || data == null) return ItemStack.EMPTY;
        if (!recipesLoaded) {
            if (recipes == null) return ItemStack.EMPTY;
            reloadRecipes(recipes);
        }
        Optional<CompoundTag> cached = StructureLoadUtil.getStructureNbtForPreview(level, data);
        if (cached.isEmpty()) return ItemStack.EMPTY;
        CompoundTag tag = cached.orElseThrow();
        Map<StructureDiskData, ItemStack> results = RESULTS.getIfPresent(tag);
        if (results == null) {
            results = new HashMap<>();
            RESULTS.put(tag, results);
        }
        // 同一文件仅缓存少量元数据变体，且不强引用大型结构 NBT。
        if (results.size() >= 32 && !results.containsKey(data)) results.clear();
        return results.computeIfAbsent(data, ignored -> {
            try {
                return getImportedDisplay(BlueprintNormalizer.load(tag, level.registryAccess(), data.direction(), data.upsideDown()));
            } catch (ConstructionBlueprintException | IllegalArgumentException exception) {
                return ItemStack.EMPTY;
            }
        });
    }

    public static ItemStack recordedBlock(ItemStack stack) {
        var data = stack.get(ModComponents.DISK_DATA);
        if (data == null) return ItemStack.EMPTY;
        var source = Identifier.tryParse(data.tag().getStringOr("StoredFrom", ""));
        if (source == null) return ItemStack.EMPTY;
        var type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getOptional(source).orElse(null);
        if (type == null) return ItemStack.EMPTY;
        var blockId = Identifier.tryParse(data.tag().getStringOr("StoredBlock", ""));
        if (blockId != null) {
            var block = BuiltInRegistries.BLOCK.getOptional(blockId).orElse(Blocks.AIR);
            if (type.isValid(block.defaultBlockState())) return block.asItem().getDefaultInstance();
        }
        var sameName = BuiltInRegistries.BLOCK.getOptional(source).orElse(Blocks.AIR);
        if (type.isValid(sameName.defaultBlockState()) && sameName.asItem() != Items.AIR) return sameName.asItem().getDefaultInstance();
        return type.getValidBlocks().stream().filter(block -> block.asItem() != Items.AIR)
            .min(Comparator.comparing(block -> BuiltInRegistries.BLOCK.getKey(block).toString()))
            .map(block -> block.asItem().getDefaultInstance()).orElse(ItemStack.EMPTY);
    }

    @SubscribeEvent
    public static void recipesUpdated(RecipesReceivedEvent event) {
        recipes = event.getRecipeMap();
        PATTERNS.clear();
        RESULTS.invalidateAll();
        recipesLoaded = false;
    }

    @SubscribeEvent
    public static void tagsUpdated(TagsUpdatedEvent.ClientPacketReceived event) {
        RESULTS.invalidateAll();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        dev.dubhe.anvilcraft.client.building.BlueprintClientFiles.clear();
        PATTERNS.clear();
        RESULTS.invalidateAll();
        recipesLoaded = false;
        recipes = null;
        StructureLoadUtil.clearClientStructureCache();
    }

    static void reloadRecipes(RecipeMap manager) {
        PATTERNS.clear();
        RESULTS.invalidateAll();
        manager.byType(ModRecipeTypes.MULTIBLOCK.get()).forEach(holder ->
            addPattern(holder.value().getPattern(), holder.value().getResult().create()));
        manager.byType(ModRecipeTypes.MULTIBLOCK_CONVERSION.get()).forEach(holder ->
            addPattern(holder.value().getInputPattern(), conversionResult(holder.value())));
        recipesLoaded = true;
    }

    private static void addPattern(MultiblockDefinition definition, ItemStack result) {
        pattern(definition, result).ifPresent(pattern -> PATTERNS.computeIfAbsent(pattern.size, ignored -> new ArrayList<>()).add(pattern));
    }

    public static Optional<Pattern> pattern(MultiblockDefinition definition, ItemStack result) {
        if (result.isEmpty()) return Optional.empty();
        DefinitionSerialization serialized = DefinitionSerialization.fromDefinition(definition);
        int size = serialized.grid().length;
        if (size == 0) return Optional.empty();
        for (String[] layer : serialized.grid()) {
            if (layer.length != size) return Optional.empty();
            for (String row : layer) {
                if (row.length() != size) return Optional.empty();
            }
        }
        return Optional.of(new Pattern(size, serialized, result.copyWithCount(1)));
    }

    public static ItemStack conversionResult(MultiblockConversionRecipe recipe) {
        Block center = recipe.centerOutput();
        if (center != null && center.asItem() != Items.AIR) return center.asItem().getDefaultInstance();
        Set<Item> outputs = new HashSet<>();
        DefinitionSerialization serialized = DefinitionSerialization.fromDefinition(recipe.getOutputPattern());
        for (String[] layer : serialized.grid()) {
            for (String row : layer) {
                for (int i = 0; i < row.length(); i++) {
                    BlockStatePredicate predicate = serialized.mapping().get(row.charAt(i));
                    if (predicate == null) continue;
                    Item item = MultiblockUtil.getDefaultState(predicate).getBlock().asItem();
                    if (item != Items.AIR) outputs.add(item);
                }
            }
        }
        return outputs.size() == 1 ? outputs.iterator().next().getDefaultInstance() : ItemStack.EMPTY;
    }

    public static ItemStack matchStructure(
        StructureDiskData data, CompoundTag tag, HolderLookup.Provider registries, List<Pattern> choices
    ) {
        if (choices.stream().noneMatch(pattern -> pattern.size == data.sizeX())) return ItemStack.EMPTY;
        Optional<Cell[]> parsed = readCells(data, tag, registries);
        if (parsed.isEmpty()) return ItemStack.EMPTY;
        Cell[] cells = parsed.orElseThrow();
        for (Pattern pattern : choices) {
            if (pattern.size != data.sizeX()) continue;
            for (Rotation rotation : Rotation.values()) {
                if (matches(pattern, cells, rotation)) return pattern.result;
            }
        }
        return ItemStack.EMPTY;
    }

    private static Optional<Cell[]> readCells(StructureDiskData data, CompoundTag tag, HolderLookup.Provider registries) {
        int size = data.sizeX();
        ListTag dimensions = tag.getListOrEmpty("size");
        if (size <= 0 || size != data.sizeY() || size != data.sizeZ() || !intTriple(dimensions)
            || dimensions.getInt(0).orElse(-1) != size || dimensions.getInt(1).orElse(-1) != size
            || dimensions.getInt(2).orElse(-1) != size) return Optional.empty();
        ListTag palette = tag.getListOrEmpty("palette");
        List<BlockState> states = new ArrayList<>();
        Rotation normalize = switch (data.direction()) {
            case WEST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        for (int i = 0; i < palette.size(); i++) {
            CompoundTag entry = palette.getCompoundOrEmpty(i);
            Identifier name = Identifier.tryParse(entry.getStringOr("Name", ""));
            if (name == null || !BuiltInRegistries.BLOCK.containsKey(name)) return Optional.empty();
            Block block = BuiltInRegistries.BLOCK.getValue(name);
            CompoundTag properties = entry.getCompoundOrEmpty("Properties");
            for (String key : properties.keySet()) {
                var property = block.getStateDefinition().getProperty(key);
                if (property == null || property.getValue(properties.getStringOr(key, "")).isEmpty()) return Optional.empty();
            }
            states.add(NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), entry).rotate(normalize));
        }
        if ((long) size * size * size > Integer.MAX_VALUE) return Optional.empty();
        Cell[] cells = new Cell[size * size * size];
        Cell air = new Cell(Blocks.AIR.defaultBlockState(), Optional.empty());
        Arrays.fill(cells, air);
        boolean[] occupied = new boolean[cells.length];
        ListTag blocks = tag.getListOrEmpty("blocks");
        if (blocks.size() > cells.length) return Optional.empty();
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag entry = blocks.getCompoundOrEmpty(i);
            ListTag pos = entry.getListOrEmpty("pos");
            if (!intTriple(pos) || !(entry.get("state") instanceof IntTag)) return Optional.empty();
            int x = pos.getInt(0).orElse(-1);
            int y = pos.getInt(1).orElse(-1);
            int z = pos.getInt(2).orElse(-1);
            int state = entry.getIntOr("state", -1);
            if (x < 0 || y < 0 || z < 0 || x >= size || y >= size || z >= size || state < 0 || state >= states.size()) {
                return Optional.empty();
            }
            if (data.upsideDown()) y = size - 1 - y;
            int index = (y * size + z) * size + x;
            if (occupied[index]) return Optional.empty();
            occupied[index] = true;
            Optional<CompoundTag> nbt = entry.getCompound("nbt");
            cells[index] = new Cell(states.get(state), nbt);
        }
        return Optional.of(cells);
    }

    private static boolean intTriple(ListTag list) {
        return list.size() == 3 && list.get(0) instanceof IntTag && list.get(1) instanceof IntTag && list.get(2) instanceof IntTag;
    }

    private static boolean matches(Pattern pattern, Cell[] cells, Rotation rotation) {
        int size = pattern.size;
        String[][] grid = pattern.definition.grid();
        for (int y = 0; y < size; y++) {
            for (int z = 0; z < size; z++) {
                for (int x = 0; x < size; x++) {
                    BlockPos position = MultiblockUtil.rotatePatternToWorld(x, y, z, rotation, size);
                    Cell cell = cells[(position.getY() * size + position.getZ()) * size + position.getX()];
                    char symbol = grid[y][z].charAt(x);
                    if (symbol == ' ') {
                        if (!cell.state.isAir()) return false;
                        continue;
                    }
                    BlockStatePredicate predicate = pattern.definition.mapping().get(symbol);
                    if (predicate == null || !predicate.testOffThread(cell.state.rotate(MultiblockUtil.reverseRotation(rotation)),
                        cell.nbt.orElse(null))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public record Pattern(int size, DefinitionSerialization definition, ItemStack result) {
    }

    private record Cell(BlockState state, Optional<CompoundTag> nbt) {
    }
}
