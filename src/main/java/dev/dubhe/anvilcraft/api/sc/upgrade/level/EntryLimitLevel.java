package dev.dubhe.anvilcraft.api.sc.upgrade.level;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.recipe.util.CodecUtil;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public enum EntryLimitLevel implements IUpgradeLevel<EntryLimitLevel> {
    MIN(54),
    ONE(108, ModBlocks.NESTING_SHULKER_BOX.asStack(), 1, ModBlockTags.ANVIL_TIER_0),
    TWO(216, ModBlocks.SPACE_OVERCOMPRESSOR.asStack(), 1, ModBlockTags.ANVIL_TIER_1),
    THREE(864, ModBlocks.CONFINED_SPACE_ANVILON.asStack(), 64, ModBlockTags.ANVIL_TIER_2),
    FOUR(Integer.MAX_VALUE, ModBlocks.SINGULARITY_CRYSTAL.asStack(), 4, ModBlockTags.ANVIL_TIER_3)
    ;

    public static final Codec<EntryLimitLevel> CODEC = StringRepresentable.fromEnum(EntryLimitLevel::values);
    public static final StreamCodec<ByteBuf, EntryLimitLevel> STREAM_CODEC = CodecUtil.enumStreamCodec(EntryLimitLevel.class);
    @Getter
    private final int limit;
    private final ItemStack material;
    @Getter
    private final int consumedCount;
    private final @Nullable TagKey<Block> anvilTag;
    private List<ItemStack> toolsCache;

    EntryLimitLevel(int limit) {
        this.limit = limit;
        this.material = ItemStack.EMPTY;
        this.consumedCount = 0;
        this.anvilTag = null;
    }

    EntryLimitLevel(int limit, ItemStack material, int consumedCount, TagKey<Block> anvilTag) {
        this.limit = limit;
        this.material = material;
        this.consumedCount = consumedCount;
        this.anvilTag = anvilTag;
    }

    @Override
    public List<ItemStack> getMaterial() {
        return List.of(this.material);
    }

    @Override
    public boolean isMaterial(ItemStack material) {
        return ItemStack.isSameItemSameComponents(this.material, material);
    }

    @Override
    public List<ItemStack> getTool() {
        if (this.toolsCache != null) return this.toolsCache;
        if (this.anvilTag == null) return this.toolsCache = ImmutableList.of();
        ImmutableList.Builder<ItemStack> builder = ImmutableList.builder();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(ModItemTags.ANVIL_HAMMER)) {
            Item item = holder.value();
            if (!(item instanceof AnvilHammerItem hammer)) continue;
            // noinspection deprecation
            if (!hammer.getAnvil().builtInRegistryHolder().is(this.anvilTag)) continue;
            builder.add(item.getDefaultInstance());
        }
        return this.toolsCache = builder.build();
    }

    @Override
    public boolean isTool(ItemStack tool) {
        if (this.anvilTag == null) return true;
        for (ItemStack stack : this.getTool()) {
            if (stack.is(tool.getItem())) return true;
        }
        return IUpgradeLevel.isAnvilHammer(tool, this.anvilTag);
    }

    @Override
    public EntryLimitLevel prev() {
        return switch (this) {
            case MIN -> null;
            case ONE -> MIN;
            case TWO -> ONE;
            case THREE -> TWO;
            case FOUR -> THREE;
        };
    }

    @Override
    public EntryLimitLevel next() {
        return switch (this) {
            case MIN -> ONE;
            case ONE -> TWO;
            case TWO -> THREE;
            case THREE -> FOUR;
            case FOUR -> null;
        };
    }

    @Override
    public EntryLimitLevel min() {
        return EntryLimitLevel.MIN;
    }

    @Override
    public EntryLimitLevel max() {
        return EntryLimitLevel.FOUR;
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getTypeId() {
        return "entry_limit";
    }
}
