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
            tag.putString("id", itemId.toString());
            tag.putInt("weight", weight);
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
            tag.putString("id", fluidId.toString());
            tag.putInt("weight", weight);
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

    /// === 修改器（包内可见，供PlanetResourceGenerator使用） ===

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

    /// === NBT序列化 ===

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
