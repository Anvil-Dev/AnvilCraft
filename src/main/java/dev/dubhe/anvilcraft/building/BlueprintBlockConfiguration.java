package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.api.teslatower.TeslaFilter;
import dev.dubhe.anvilcraft.block.entity.ActiveSilencerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.AdvancedComparatorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import dev.dubhe.anvilcraft.block.entity.BaseChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ChargerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.DischargerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ExpCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.HeliostatsBlockEntity;
import dev.dubhe.anvilcraft.block.entity.IFilterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ItemCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ItemDetectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PulseGeneratorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.RedstoneDiceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.StructureScannerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.TeslaTowerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.TradingStationBlockEntity;
import dev.dubhe.anvilcraft.block.entity.batch.BaseBatchCraftingBlockEntity;
import dev.dubhe.anvilcraft.block.entity.batch.BatchCrafterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.AbstractPipeBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.container.FilterOnlyContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 恢复蓝图设置与运行进度；库存由材料系统供应，身份由当前放置重建。 */
public final class BlueprintBlockConfiguration {
    private BlueprintBlockConfiguration() {
    }

    static CompoundTag take(BlockEntity entity, CompoundTag source, HolderLookup.Provider registries) {
        CompoundTag result = new CompoundTag();
        final CompoundTag runtime = BlueprintRuntimeData.take(entity, source);
        if (entity instanceof SignBlockEntity) {
            result = SignDecorationAdapter.sanitize(entity.getBlockState(), source, registries);
            remove(source, "front_text", "back_text", "is_waxed");
        }
        if (entity instanceof IFilterBlockEntity filter && filter.getFilteredItemStackHandler().size() > 0) {
            for (String key : List.of("Inventory", "Depository", "Items")) {
                if (!(source.get(key) instanceof CompoundTag)) continue;
                result.put(key, filtering(source.getCompoundOrEmpty(key), filter.getFilteredItemStackHandler().size(), registries));
                stripFiltering(source.getCompoundOrEmpty(key));
            }
        }
        if ((entity instanceof PulseGeneratorBlockEntity || entity instanceof AdvancedComparatorBlockEntity)
            && (source.get("ExtraData") instanceof CompoundTag)) {
            CompoundTag data = source.getCompoundOrEmpty("ExtraData");
            CompoundTag settings = new CompoundTag();
            bool(data, settings, "OutputMode");
            if (entity instanceof PulseGeneratorBlockEntity) {
                integer(data, settings, "StartMode", 0, 2);
                integer(data, settings, "WaitingTime", 0, 24000);
                integer(data, settings, "SignalDuration", 0, 24000);
                if (settings.getIntOr("WaitingTime", 0) == 0 && settings.getIntOr("SignalDuration", 0) == 0) {
                    settings.putInt("SignalDuration", 1);
                }
            } else {
                integer(data, settings, "CompareMode", 0, 1);
                integer(data, settings, "HighLimit", 0, 15);
                integer(data, settings, "LowLimit", 0, 15);
                bool(data, settings, "RedstoneControl");
            }
            result.put("ExtraData", settings);
            source.remove("ExtraData");
        }
        if (entity instanceof BaseChuteBlockEntity) source.remove("Cooldown");
        if (entity instanceof ChargerBlockEntity || entity instanceof DischargerBlockEntity) {
            remove(source, "TimeLeft", "TimeTotalCache", "PowerValue", "StartupCoolDown", "FeCharging", "FeDischarging", "FeCooldown");
        }
        if (entity instanceof TradingStationBlockEntity) {
            result.put("Filters", filterSamples(source.getCompoundOrEmpty("Filters"), 3, registries));
            for (String key : List.of("AllowPlayer", "AllowVillager", "AllowInput", "AllowOutput")) bool(source, result, key);
            remove(source, "Filters", "Owner");
        }
        if (entity instanceof ItemDetectorBlockEntity) {
            integer(source, result, "Range", 1, 8);
            choice(source, result, "FilterMode", "ANY", "ALL");
            bool(source, result, "OutputInvert");
            result.put("Filter", filterSamples(source.getCompoundOrEmpty("Filter"), 9, registries));
            remove(source, "Filter", "OutputSignal");
        }
        if (entity instanceof ItemCollectorBlockEntity || entity instanceof ExpCollectorBlockEntity) {
            integer(source, result, "Cooldown", 0, 3);
            integer(source, result, "RangeRadius", 0, 3);
            source.remove("cd");
        }
        if (entity instanceof ActiveSilencerBlockEntity) {
            var sounds = ActiveSilencerBlockEntity.CODEC.parse(NbtOps.INSTANCE, source.getCompoundOrEmpty("MutedSound"))
                .result().orElse(List.of());
            result.put("MutedSound", ActiveSilencerBlockEntity.CODEC.encodeStart(NbtOps.INSTANCE,
                sounds.stream().distinct().limit(1024).toList()).getOrThrow());
            source.remove("MutedSound");
        }
        if (entity instanceof TeslaTowerBlockEntity) tesla(source, result);
        if (entity instanceof ControlValveBlockEntity) valve(source, result, registries);
        if (entity instanceof AutoEnchantingTableBlockEntity) enchanting(source, result, registries);
        if (entity instanceof StructureScannerBlockEntity) {
            for (String key : List.of("rangeX", "rangeY", "rangeZ")) integer(source, result, key, 0, 15);
            remove(source, "isScanning", "currentScanLayer", "scannedBlocks", "pendingAutoSave", "autoSaveStructureName");
        }
        if (entity instanceof BaseBatchCraftingBlockEntity) {
            remove(source, "PoweredBefore", "Cooldown", "HasDisplayItemStack", "ResultItemStack");
        }
        if (entity instanceof BatchCrafterBlockEntity) integer(source, result, "Selecting", 0, Integer.MAX_VALUE);
        if (entity instanceof SmartBlockPlacerBlockEntity) smartPlacer(source, result);
        if (entity instanceof RedstoneDiceBlockEntity) {
            bool(source, result, "Uniform");
            remove(source, "Rolling", "PreviousFaces", "Faces", "Output", "RollStart");
        }
        if (entity instanceof CrafterBlockEntity) {
            result.putIntArray("disabled_slots", Arrays.stream(source.getIntArray("disabled_slots").orElseGet(() -> new int[0]))
                .filter(slot -> slot >= 0 && slot < 9).distinct().toArray());
            remove(source, "disabled_slots", "crafting_ticks_remaining", "triggered");
        }
        if (entity instanceof LecternBlockEntity) integer(source, result, "Page", 0, Integer.MAX_VALUE);
        if (entity instanceof HeliostatsBlockEntity && (source.get("Ix") instanceof IntTag)) {
            for (String key : List.of("Ix", "Iy", "Iz")) integer(source, result, key, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }
        if (entity instanceof AbstractPipeBlockEntity) {
            result.put("Valves", checkValves(BlueprintNbt.list(source, "Valves", Tag.TAG_COMPOUND)));
            remove(source, "Valves", "Powered");
        }
        result.merge(runtime);
        return result;
    }

    static void strip(BlockEntity entity, CompoundTag tag, HolderLookup.Provider registries) {
        take(entity, tag, registries);
        if (entity instanceof IFilterBlockEntity filter && filter.getFilteredItemStackHandler().size() > 0) {
            remove(tag, "Inventory", "Depository", "Items");
        }
    }

    private static CompoundTag filtering(CompoundTag source, int size, HolderLookup.Provider registries) {
        CompoundTag result = new CompoundTag();
        result.putBoolean("FilterEnabled", source.getBooleanOr("FilterEnabled", false));
        result.putInt("Size", size);
        ListTag entries = new ListTag();
        for (int slot = 0; slot < size; slot++) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", slot);
            entry.putBoolean("IsEmptySlot", true);
            entry.putInt("SlotLimit", 64);
            for (Tag value : BlueprintNbt.list(source, "Inventory", Tag.TAG_COMPOUND)) {
                CompoundTag saved = (CompoundTag) value;
                if (saved.getIntOr("Slot", 0) != slot) continue;
                entry.putBoolean("Disabled", saved.getBooleanOr("Disabled", false));
                entry.putInt("SlotLimit", saved.contains("SlotLimit") ? Math.clamp(saved.getIntOr("SlotLimit", 0), 1, 64) : 64);
                if (saved.getBooleanOr("SlotFilterEnabled", false)) {
                    ItemStack item = BlueprintNbt.readItem(registries, saved.getCompoundOrEmpty("SlotFilterItem"));
                    if (!item.isEmpty()) {
                        entry.putBoolean("SlotFilterEnabled", true);
                        entry.put("SlotFilterItem", BlueprintNbt.writeItem(registries, item.copyWithCount(1)));
                    }
                }
            }
            entries.add(entry);
        }
        result.put("Inventory", entries);
        return result;
    }

    private static void stripFiltering(CompoundTag source) {
        source.remove("FilterEnabled");
        for (Tag value : BlueprintNbt.list(source, "Inventory", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) value;
            remove(entry, "SlotFilterEnabled", "SlotFilterItem", "Disabled", "SlotLimit");
        }
    }

    private static CompoundTag filterSamples(CompoundTag source, int size, HolderLookup.Provider registries) {
        var items = new FilterOnlyContainer(null, size);
        for (Tag value : BlueprintNbt.list(source, "Items", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) value;
            int slot = tag.getIntOr("Slot", 0);
            if (slot < 0 || slot >= size) continue;
            ItemStack item = BlueprintNbt.readItem(registries, tag.getCompound("Item").orElse(tag));
            if (!item.isEmpty()) items.setItem(slot, item.copyWithCount(Math.min(item.getCount(), item.getMaxStackSize())));
        }
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        items.serialize(output);
        return output.buildResult();
    }

    private static void tesla(CompoundTag source, CompoundTag result) {
        for (String key : List.copyOf(source.keySet())) {
            int delimiter = key.indexOf("_-_");
            if (delimiter < 0) continue;
            String id = key.substring(0, delimiter);
            if (!TeslaFilter.getFilter(id).getId().isEmpty() && (source.get(key) instanceof StringTag)) {
                String argument = source.getStringOr(key, "");
                if (argument.length() > 256) throw new IllegalArgumentException("Tesla filter argument is too long");
                result.putString(key, argument);
            }
            source.remove(key);
        }
        remove(source, "LastStrikeTime", "TargetEntityUUID", "TargetLightningRod");
    }

    private static void valve(CompoundTag source, CompoundTag result, HolderLookup.Provider registries) {
        integer(source, result, "MaxRate", 0, ControlValveBlockEntity.MAX_RATE);
        integer(source, result, "Facing", 0, 5);
        ListTag filters = new ListTag();
        for (Tag value : BlueprintNbt.list(source, "Filters", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) value;
            int slot = tag.getIntOr("Slot", 0);
            if (slot < 0 || slot >= ControlValveBlockEntity.FILTER_SLOT_COUNT) continue;
            FluidStack fluid = FluidStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE),
                tag.getCompoundOrEmpty("Fluid")).result().orElse(FluidStack.EMPTY);
            if (fluid.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", slot);
            entry.put("Fluid", FluidStack.CODEC.encodeStart(
                registries.createSerializationContext(NbtOps.INSTANCE), fluid.copyWithAmount(1)).getOrThrow());
            filters.add(entry);
        }
        result.put("Filters", filters);
        source.remove("Filters");
    }

    private static void smartPlacer(CompoundTag source, CompoundTag result) {
        choice(source, result, "operation", "pickup", "move");
        choice(source, result, "target", "position", "blueprint");
        choice(source, result, "placement", "skip", "wait");
        integer(source, result, "selectedLayer", 0, SmartBlockPlacerBlockEntity.POSITION_GRID_SIZE - 1);
        byte[] stored = (source.get("layerPositions") instanceof ByteArrayTag)
            ? source.getByteArray("layerPositions").orElseGet(() -> new byte[0])
            : source.getByteArray("positionMarks").orElseGet(() -> new byte[0]);
        byte[] positions = Arrays.copyOf(stored, SmartBlockPlacerBlockEntity.POSITION_COUNT);
        if ((source.get("layerPositions") instanceof CompoundTag)) {
            for (int layer = 0; layer < SmartBlockPlacerBlockEntity.POSITION_GRID_SIZE; layer++) {
                for (int position : source.getCompoundOrEmpty("layerPositions").getIntArray("layer_" + layer).orElseGet(() -> new int[0])) {
                    if (position >= 0 && position < SmartBlockPlacerBlockEntity.POSITIONS_PER_LAYER) {
                        positions[layer * SmartBlockPlacerBlockEntity.POSITIONS_PER_LAYER + position] = 1;
                    }
                }
            }
        }
        for (int i = 0; i < positions.length; i++) positions[i] = positions[i] == 0 ? (byte) 0 : (byte) 1;
        result.putByteArray("layerPositions", positions);
        remove(source, "layerPositions", "positionMarks", "currentPlacementIndex", "phase", "progress", "blueprintStates",
            "loadedStructureName", "invalidStructure", "missingBlock", "currentHeldBlock");
    }

    private static ListTag checkValves(ListTag source) {
        ListTag result = new ListTag();
        Set<Integer> faces = new HashSet<>();
        for (Tag value : source) {
            CompoundTag entry = (CompoundTag) value;
            int face = entry.getIntOr("Face", 0);
            int flow = entry.getIntOr("Flow", 0);
            if (face < 0 || face > 5 || flow < 0 || flow > 5 || !faces.add(face)
                || Direction.from3DDataValue(face).getAxis() != Direction.from3DDataValue(flow).getAxis()) {
                throw new IllegalArgumentException("Invalid blueprint check valve direction");
            }
            CompoundTag valve = new CompoundTag();
            valve.putInt("Face", face);
            valve.putInt("Flow", flow);
            result.add(valve);
        }
        return result;
    }

    static List<ItemStack> materials(CompoundTag config) {
        int count = BlueprintNbt.list(config, "Valves", Tag.TAG_COMPOUND).size();
        return count == 0 ? List.of() : List.of(ModItems.CHECK_VALVE.asStack(count));
    }

    public static void transform(
        CompoundTag config, BlueprintPlacement placement, @Nullable BlockPos sourceOrigin, StructureSnapshot snapshot
    ) {
        if ("minecraft:piston".equals(config.getStringOr("id", ""))) {
            var moved = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, config.getCompoundOrEmpty("blockState"));
            config.put("blockState", NbtUtils.writeBlockState(placement.stateOf(moved)));
            Direction facing = Direction.from3DDataValue(config.getIntOr("facing", 0));
            config.putInt("facing", placement.rotation().rotate(placement.mirror().mirror(facing)).get3DDataValue());
        }
        if (config.contains("Facing")) {
            Direction facing = Direction.from3DDataValue(config.getIntOr("Facing", 0));
            config.putInt("Facing", placement.rotation().rotate(placement.mirror().mirror(facing)).get3DDataValue());
        }
        for (Tag value : BlueprintNbt.list(config, "Valves", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) value;
            for (String key : List.of("Face", "Flow")) {
                Direction direction = Direction.from3DDataValue(tag.getIntOr(key, 0));
                tag.putInt(key, placement.rotation().rotate(placement.mirror().mirror(direction)).get3DDataValue());
            }
        }
        if (config.contains("layerPositions") && placement.mirror() != Mirror.NONE) {
            byte[] source = Arrays.copyOf(config.getByteArray("layerPositions").orElseGet(() -> new byte[0]),
                SmartBlockPlacerBlockEntity.POSITION_COUNT);
            byte[] mirrored = source.clone();
            int side = SmartBlockPlacerBlockEntity.POSITION_GRID_SIZE;
            for (int i = 0; i < source.length; i++) mirrored[i - i % side + side - 1 - i % side] = source[i];
            config.putByteArray("layerPositions", mirrored);
        }
        if (!config.contains("Ix")) return;
        BlockPos target = new BlockPos(config.getIntOr("Ix", 0), config.getIntOr("Iy", 0), config.getIntOr("Iz", 0));
        if (sourceOrigin != null) {
            BlockPos local = target.subtract(sourceOrigin);
            if (local.getX() >= 0 && local.getY() >= 0 && local.getZ() >= 0 && local.getX() < snapshot.size().getX()
                && local.getY() < snapshot.size().getY() && local.getZ() < snapshot.size().getZ()) {
                BlockPos world = placement.worldOf(local);
                config.putInt("Ix", world.getX());
                config.putInt("Iy", world.getY());
                config.putInt("Iz", world.getZ());
                return;
            }
        }
        remove(config, "Ix", "Iy", "Iz");
    }

    @Nullable
    public static BlockPos sourceOrigin(StructureSnapshot snapshot) {
        BlockPos origin = null;
        for (var entry : snapshot.blocks()) {
            CompoundTag tag = entry.nbt().orElse(null);
            if (tag == null || !(tag.get("x") instanceof IntTag)
                || !(tag.get("y") instanceof IntTag) || !(tag.get("z") instanceof IntTag)) continue;
            BlockPos candidate = new BlockPos(tag.getIntOr("x", 0), tag.getIntOr("y", 0), tag.getIntOr("z", 0)).subtract(entry.pos());
            if (origin != null && !origin.equals(candidate)) return null;
            origin = candidate;
        }
        return origin;
    }

    static void apply(BlockEntity entity, CompoundTag settings, ServerPlayer player) {
        if (settings.isEmpty() && !(entity instanceof TradingStationBlockEntity)) return;
        CompoundTag tag = entity.saveWithFullMetadata(player.registryAccess());
        tag.merge(settings.copy());
        if (entity instanceof TradingStationBlockEntity) tag.store("Owner", UUIDUtil.CODEC, player.getUUID());
        var input = TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), tag);
        if (entity instanceof PulseGeneratorBlockEntity pulse) pulse.loadBlueprint(input);
        else entity.loadWithComponents(input);
    }

    static void afterContents(BlockEntity entity, CompoundTag settings, ServerPlayer player) {
        CompoundTag runtime = BlueprintRuntimeData.take(entity, settings.copy());
        if (!runtime.isEmpty()) {
            CompoundTag restored = entity.saveWithFullMetadata(player.registryAccess()).merge(runtime);
            var input = TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), restored);
            if (entity instanceof PulseGeneratorBlockEntity pulse) pulse.loadBlueprint(input);
            else entity.loadWithComponents(input);
        }
        if (entity instanceof LecternBlockEntity lectern && !lectern.getBook().isEmpty() && settings.contains("Page")) {
            apply(entity, settings, player);
        }
        if (entity instanceof AutoEnchantingTableBlockEntity enchanting) {
            enchanting.setLiquidLevel(settings.getIntOr("LiquidEnchantmentLevel", 0));
        }
        if (entity instanceof BatchCrafterBlockEntity crafter) crafter.setSelecting(settings.getIntOr("Selecting", 0));
        if (entity instanceof SmartBlockPlacerBlockEntity placer) {
            placer.applyDiskData(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), settings));
        }
    }

    private static void enchanting(CompoundTag source, CompoundTag result, HolderLookup.Provider registries) {
        choice(source, result, "WorkMode", Arrays.stream(AutoEnchantingTableBlockEntity.WorkMode.values())
            .map(AutoEnchantingTableBlockEntity.WorkMode::getSerializedName).toArray(String[]::new));
        integer(source, result, "LiquidEnchantmentLevel", 0, 255);
        ListTag selected = new ListTag();
        Set<String> seen = new HashSet<>();
        for (Tag value : source.getListOrEmpty("SelectedEnchantments")) {
            String id = value.asString().orElse("");
            Identifier key = Identifier.tryParse(id);
            if (key != null && seen.add(id) && registries.lookupOrThrow(Registries.ENCHANTMENT)
                .get(ResourceKey.create(Registries.ENCHANTMENT, key)).isPresent()) {
                selected.add(StringTag.valueOf(id));
            }
        }
        result.put("SelectedEnchantments", selected);
        remove(source, "SelectedEnchantments", "CooldownTicks", "ShelfLevel");
    }

    private static void integer(CompoundTag source, CompoundTag target, String key, int min, int max) {
        if (!(source.get(key) instanceof NumericTag)) return;
        target.putInt(key, Math.clamp(source.getIntOr(key, 0), min, max));
        source.remove(key);
    }

    private static void bool(CompoundTag source, CompoundTag target, String key) {
        if (!(source.get(key) instanceof NumericTag)) return;
        target.putBoolean(key, source.getBooleanOr(key, false));
        source.remove(key);
    }

    private static void choice(CompoundTag source, CompoundTag target, String key, String... values) {
        if (!source.contains(key)) return;
        String value = source.getStringOr(key, "");
        target.putString(key, Arrays.asList(values).contains(value) ? value : values[0]);
        source.remove(key);
    }

    private static void remove(CompoundTag tag, String... keys) {
        for (String key : keys) tag.remove(key);
    }
}
