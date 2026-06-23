package dev.dubhe.anvilcraft.block.entity.celestial;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Container for all resources generated for a celestial body.
 * Stored alongside {@link CelestialBodyData} in the block entity NBT.
 */
public class PlanetaryResourceSet {

    /**
     * A weighted item entry — item identifier with its percentage weight.
     */
    public record WeightedItemStack(Identifier itemId, int weight) {
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", itemId.toString());
            tag.putInt("weight", weight);
            return tag;
        }

        public static WeightedItemStack fromTag(CompoundTag tag) {
            Identifier id = Identifier.parse(tag.getStringOr("id", "minecraft:air"));
            int weight = tag.getIntOr("weight", 0);
            return new WeightedItemStack(id, weight);
        }
    }

    /**
     * A weighted fluid entry — fluid identifier with its percentage weight.
     */
    public record WeightedFluidStack(Identifier fluidId, int weight) {
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", fluidId.toString());
            tag.putInt("weight", weight);
            return tag;
        }

        public static WeightedFluidStack fromTag(CompoundTag tag) {
            Identifier id = Identifier.parse(tag.getStringOr("id", "minecraft:air"));
            int weight = tag.getIntOr("weight", 0);
            return new WeightedFluidStack(id, weight);
        }
    }

    // === Fields ===

    private final List<WeightedItemStack> minerals = new ArrayList<>();
    private final List<WeightedFluidStack> fluids = new ArrayList<>();
    private final List<WeightedItemStack> giantItems = new ArrayList<>();
    private final List<WeightedFluidStack> giantFluids = new ArrayList<>();
    private final List<WeightedItemStack> biologicalItems = new ArrayList<>();
    private final List<WeightedFluidStack> biologicalFluids = new ArrayList<>();
    private final List<WeightedItemStack> offerings = new ArrayList<>();
    private final List<WeightedItemStack> wastelandItems = new ArrayList<>();
    private boolean hasCivilization = false;
    @Getter
    private boolean isWasteland = false;

    // === Getters ===

    public List<WeightedItemStack> getMinerals() {
        return Collections.unmodifiableList(minerals);
    }

    public List<WeightedFluidStack> getFluids() {
        return Collections.unmodifiableList(fluids);
    }

    public List<WeightedItemStack> getGiantItems() {
        return Collections.unmodifiableList(giantItems);
    }

    public List<WeightedFluidStack> getGiantFluids() {
        return Collections.unmodifiableList(giantFluids);
    }

    public List<WeightedItemStack> getBiologicalItems() {
        return Collections.unmodifiableList(biologicalItems);
    }

    public List<WeightedFluidStack> getBiologicalFluids() {
        return Collections.unmodifiableList(biologicalFluids);
    }

    public List<WeightedItemStack> getOfferings() {
        return Collections.unmodifiableList(offerings);
    }

    public List<WeightedItemStack> getWastelandItems() {
        return Collections.unmodifiableList(wastelandItems);
    }

    public boolean hasCivilization() {
        return hasCivilization;
    }

    public boolean isEmpty() {
        return minerals.isEmpty()
            && fluids.isEmpty()
            && giantItems.isEmpty()
            && giantFluids.isEmpty()
            && biologicalItems.isEmpty()
            && biologicalFluids.isEmpty()
            && offerings.isEmpty()
            && wastelandItems.isEmpty();
    }

    // === Mutators (package-private, for PlanetResourceGenerator) ===

    void addMineral(WeightedItemStack entry) {
        minerals.add(entry);
    }

    void addFluid(WeightedFluidStack entry) {
        fluids.add(entry);
    }

    void addGiantItem(WeightedItemStack entry) {
        giantItems.add(entry);
    }

    void addGiantFluid(WeightedFluidStack entry) {
        giantFluids.add(entry);
    }

    void addBiologicalItem(WeightedItemStack entry) {
        biologicalItems.add(entry);
    }

    void addBiologicalFluid(WeightedFluidStack entry) {
        biologicalFluids.add(entry);
    }

    void addOffering(WeightedItemStack entry) {
        offerings.add(entry);
    }

    void addWastelandItem(WeightedItemStack entry) {
        wastelandItems.add(entry);
    }

    void setHasCivilization() {
        this.hasCivilization = true;
    }

    void setWasteland() {
        isWasteland = true;
    }

    // === NBT Serialization ===

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        if (!minerals.isEmpty()) {
            tag.put("minerals", writeItemList(minerals));
        }
        if (!fluids.isEmpty()) {
            tag.put("fluids", writeFluidList(fluids));
        }
        if (!giantItems.isEmpty()) {
            tag.put("giantItems", writeItemList(giantItems));
        }
        if (!giantFluids.isEmpty()) {
            tag.put("giantFluids", writeFluidList(giantFluids));
        }
        if (!biologicalItems.isEmpty()) {
            tag.put("biologicalItems", writeItemList(biologicalItems));
        }
        if (!biologicalFluids.isEmpty()) {
            tag.put("biologicalFluids", writeFluidList(biologicalFluids));
        }
        if (!offerings.isEmpty()) {
            tag.put("offerings", writeItemList(offerings));
        }
        if (!wastelandItems.isEmpty()) {
            tag.put("wastelandItems", writeItemList(wastelandItems));
        }
        tag.putBoolean("hasCivilization", hasCivilization);
        tag.putBoolean("isWasteland", isWasteland);
        return tag;
    }

    public static PlanetaryResourceSet fromTag(CompoundTag tag) {
        PlanetaryResourceSet set = new PlanetaryResourceSet();
        tag.getList("minerals").ifPresent(listTag -> readItemList(listTag, set.minerals));
        tag.getList("fluids").ifPresent(listTag -> readFluidList(listTag, set.fluids));
        tag.getList("giantItems").ifPresent(listTag -> readItemList(listTag, set.giantItems));
        tag.getList("giantFluids").ifPresent(listTag -> readFluidList(listTag, set.giantFluids));
        tag.getList("biologicalItems").ifPresent(listTag -> readItemList(listTag, set.biologicalItems));
        tag.getList("biologicalFluids").ifPresent(listTag -> readFluidList(listTag, set.biologicalFluids));
        tag.getList("offerings").ifPresent(listTag -> readItemList(listTag, set.offerings));
        tag.getList("wastelandItems").ifPresent(listTag -> readItemList(listTag, set.wastelandItems));
        set.hasCivilization = tag.getBooleanOr("hasCivilization", false);
        set.isWasteland = tag.getBooleanOr("isWasteland", false);
        return set;
    }

    private static ListTag writeItemList(List<WeightedItemStack> list) {
        ListTag listTag = new ListTag();
        for (WeightedItemStack entry : list) {
            listTag.add(entry.toTag());
        }
        return listTag;
    }

    private static ListTag writeFluidList(List<WeightedFluidStack> list) {
        ListTag listTag = new ListTag();
        for (WeightedFluidStack entry : list) {
            listTag.add(entry.toTag());
        }
        return listTag;
    }

    private static void readItemList(ListTag listTag, List<WeightedItemStack> target) {
        for (Tag t : listTag) {
            target.add(WeightedItemStack.fromTag((CompoundTag) t));
        }
    }

    private static void readFluidList(ListTag listTag, List<WeightedFluidStack> target) {
        for (Tag t : listTag) {
            target.add(WeightedFluidStack.fromTag((CompoundTag) t));
        }
    }
}
