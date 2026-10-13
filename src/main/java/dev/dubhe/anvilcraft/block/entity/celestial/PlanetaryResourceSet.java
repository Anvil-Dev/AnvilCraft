package dev.dubhe.anvilcraft.block.entity.celestial;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// 为天体生成的所有资源的容器。
/// 与 {@link CelestialBodyData} 一起存储在方块实体NBT中。
public class PlanetaryResourceSet {

    /// 加权物品条目 —— 物品标识符及其百分比权重。
    public record WeightedItemStack(ResourceLocation itemId, int weight) {
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", this.itemId.toString());
            tag.putInt("weight", this.weight);
            return tag;
        }

        public static WeightedItemStack fromTag(CompoundTag tag) {
            ResourceLocation id = ResourceLocation.parse(tag.getString("id"));
            int weight = tag.getInt("weight");
            return new WeightedItemStack(id, weight);
        }
    }

    /// 加权流体条目 —— 流体标识符及其百分比权重。
    public record WeightedFluidStack(ResourceLocation fluidId, int weight) {
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", this.fluidId.toString());
            tag.putInt("weight", this.weight);
            return tag;
        }

        public static WeightedFluidStack fromTag(CompoundTag tag) {
            ResourceLocation id = ResourceLocation.parse(tag.getString("id"));
            int weight = tag.getInt("weight");
            return new WeightedFluidStack(id, weight);
        }
    }

    /// === 字段 ===

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

    /// === 获取器 ===

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

    /// === 修改器（包内可见，供PlanetResourceGenerator使用） ===

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

    /// === NBT序列化 ===

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        if (!this.minerals.isEmpty()) {
            tag.put("minerals", writeItemList(this.minerals));
        }
        if (!this.fluids.isEmpty()) {
            tag.put("fluids", writeFluidList(this.fluids));
        }
        if (!this.giantItems.isEmpty()) {
            tag.put("giantItems", writeItemList(this.giantItems));
        }
        if (!this.giantFluids.isEmpty()) {
            tag.put("giantFluids", writeFluidList(this.giantFluids));
        }
        if (!this.biologicalItems.isEmpty()) {
            tag.put("biologicalItems", writeItemList(this.biologicalItems));
        }
        if (!this.biologicalFluids.isEmpty()) {
            tag.put("biologicalFluids", writeFluidList(this.biologicalFluids));
        }
        if (!this.offerings.isEmpty()) {
            tag.put("offerings", writeItemList(this.offerings));
        }
        if (!this.wastelandItems.isEmpty()) {
            tag.put("wastelandItems", writeItemList(this.wastelandItems));
        }
        tag.putBoolean("hasCivilization", this.hasCivilization);
        tag.putBoolean("isWasteland", this.isWasteland);
        return tag;
    }

    public static PlanetaryResourceSet fromTag(CompoundTag tag) {
        PlanetaryResourceSet set = new PlanetaryResourceSet();
        if (tag.contains("minerals")) {
            readItemList(tag.getList("minerals", Tag.TAG_COMPOUND), set.minerals);
        }
        if (tag.contains("fluids")) {
            readFluidList(tag.getList("fluids", Tag.TAG_COMPOUND), set.fluids);
        }
        if (tag.contains("giantItems")) {
            readItemList(tag.getList("giantItems", Tag.TAG_COMPOUND), set.giantItems);
        }
        if (tag.contains("giantFluids")) {
            readFluidList(tag.getList("giantFluids", Tag.TAG_COMPOUND), set.giantFluids);
        }
        if (tag.contains("biologicalItems")) {
            readItemList(tag.getList("biologicalItems", Tag.TAG_COMPOUND), set.biologicalItems);
        }
        if (tag.contains("biologicalFluids")) {
            readFluidList(tag.getList("biologicalFluids", Tag.TAG_COMPOUND), set.biologicalFluids);
        }
        if (tag.contains("offerings")) {
            readItemList(tag.getList("offerings", Tag.TAG_COMPOUND), set.offerings);
        }
        if (tag.contains("wastelandItems")) {
            readItemList(tag.getList("wastelandItems", Tag.TAG_COMPOUND), set.wastelandItems);
        }
        set.hasCivilization = tag.getBoolean("hasCivilization");
        set.isWasteland = tag.getBoolean("isWasteland");
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
