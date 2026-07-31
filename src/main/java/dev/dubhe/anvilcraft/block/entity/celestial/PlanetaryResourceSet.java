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
 * 天体生成的全部资源集合，与 {@link CelestialBodyData} 一同保存在方块实体 NBT 中。
 */
public class PlanetaryResourceSet {

    /** 带权重的物品资源条目。 */
    public record WeightedItemStack(Identifier itemId, int weight) {
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", this.itemId.toString());
            tag.putInt("weight", this.weight);
            return tag;
        }

        public static WeightedItemStack fromTag(CompoundTag tag) {
            Identifier id = Identifier.parse(tag.getStringOr("id", "minecraft:air"));
            int weight = tag.getIntOr("weight", 0);
            return new WeightedItemStack(id, weight);
        }
    }

    /** 带权重的流体资源条目。 */
    public record WeightedFluidStack(Identifier fluidId, int weight) {
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", this.fluidId.toString());
            tag.putInt("weight", this.weight);
            return tag;
        }

        public static WeightedFluidStack fromTag(CompoundTag tag) {
            Identifier id = Identifier.parse(tag.getStringOr("id", "minecraft:air"));
            int weight = tag.getIntOr("weight", 0);
            return new WeightedFluidStack(id, weight);
        }
    }

    // === 数据字段 ===

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

    // === 只读访问 ===

    public List<WeightedItemStack> getMinerals() {
        return Collections.unmodifiableList(this.minerals);
    }

    public List<WeightedFluidStack> getFluids() {
        return Collections.unmodifiableList(this.fluids);
    }

    public List<WeightedItemStack> getGiantItems() {
        return Collections.unmodifiableList(this.giantItems);
    }

    public List<WeightedFluidStack> getGiantFluids() {
        return Collections.unmodifiableList(this.giantFluids);
    }

    public List<WeightedItemStack> getBiologicalItems() {
        return Collections.unmodifiableList(this.biologicalItems);
    }

    public List<WeightedFluidStack> getBiologicalFluids() {
        return Collections.unmodifiableList(this.biologicalFluids);
    }

    public List<WeightedItemStack> getOfferings() {
        return Collections.unmodifiableList(this.offerings);
    }

    public List<WeightedItemStack> getWastelandItems() {
        return Collections.unmodifiableList(this.wastelandItems);
    }

    public boolean hasCivilization() {
        return this.hasCivilization;
    }

    public boolean isEmpty() {
        return this.minerals.isEmpty()
            && this.fluids.isEmpty()
            && this.giantItems.isEmpty()
            && this.giantFluids.isEmpty()
            && this.biologicalItems.isEmpty()
            && this.biologicalFluids.isEmpty()
            && this.offerings.isEmpty()
            && this.wastelandItems.isEmpty();
    }

    // === 包内写入方法，仅供资源生成器使用 ===

    void addMineral(WeightedItemStack entry) {
        this.minerals.add(entry);
    }

    void addFluid(WeightedFluidStack entry) {
        this.fluids.add(entry);
    }

    void addGiantItem(WeightedItemStack entry) {
        this.giantItems.add(entry);
    }

    void addGiantFluid(WeightedFluidStack entry) {
        this.giantFluids.add(entry);
    }

    void addBiologicalItem(WeightedItemStack entry) {
        this.biologicalItems.add(entry);
    }

    void addBiologicalFluid(WeightedFluidStack entry) {
        this.biologicalFluids.add(entry);
    }

    void addOffering(WeightedItemStack entry) {
        this.offerings.add(entry);
    }

    void addWastelandItem(WeightedItemStack entry) {
        this.wastelandItems.add(entry);
    }

    void setHasCivilization() {
        this.hasCivilization = true;
    }

    void setWasteland() {
        this.isWasteland = true;
    }

    // === NBT 序列化 ===

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        if (!this.minerals.isEmpty()) {
            tag.put("minerals", PlanetaryResourceSet.writeItemList(this.minerals));
        }
        if (!this.fluids.isEmpty()) {
            tag.put("fluids", PlanetaryResourceSet.writeFluidList(this.fluids));
        }
        if (!this.giantItems.isEmpty()) {
            tag.put("giantItems", PlanetaryResourceSet.writeItemList(this.giantItems));
        }
        if (!this.giantFluids.isEmpty()) {
            tag.put("giantFluids", PlanetaryResourceSet.writeFluidList(this.giantFluids));
        }
        if (!this.biologicalItems.isEmpty()) {
            tag.put("biologicalItems", PlanetaryResourceSet.writeItemList(this.biologicalItems));
        }
        if (!this.biologicalFluids.isEmpty()) {
            tag.put("biologicalFluids", PlanetaryResourceSet.writeFluidList(this.biologicalFluids));
        }
        if (!this.offerings.isEmpty()) {
            tag.put("offerings", PlanetaryResourceSet.writeItemList(this.offerings));
        }
        if (!this.wastelandItems.isEmpty()) {
            tag.put("wastelandItems", PlanetaryResourceSet.writeItemList(this.wastelandItems));
        }
        tag.putBoolean("hasCivilization", this.hasCivilization);
        tag.putBoolean("isWasteland", this.isWasteland);
        return tag;
    }

    public static PlanetaryResourceSet fromTag(CompoundTag tag) {
        PlanetaryResourceSet set = new PlanetaryResourceSet();
        tag.getList("minerals").ifPresent(listTag -> PlanetaryResourceSet.readItemList(listTag, set.minerals));
        tag.getList("fluids").ifPresent(listTag -> PlanetaryResourceSet.readFluidList(listTag, set.fluids));
        tag.getList("giantItems").ifPresent(listTag -> PlanetaryResourceSet.readItemList(listTag, set.giantItems));
        tag.getList("giantFluids").ifPresent(listTag -> PlanetaryResourceSet.readFluidList(listTag, set.giantFluids));
        tag.getList("biologicalItems").ifPresent(listTag -> PlanetaryResourceSet.readItemList(listTag, set.biologicalItems));
        tag.getList("biologicalFluids").ifPresent(listTag -> PlanetaryResourceSet.readFluidList(listTag, set.biologicalFluids));
        tag.getList("offerings").ifPresent(listTag -> PlanetaryResourceSet.readItemList(listTag, set.offerings));
        tag.getList("wastelandItems").ifPresent(listTag -> PlanetaryResourceSet.readItemList(listTag, set.wastelandItems));
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
