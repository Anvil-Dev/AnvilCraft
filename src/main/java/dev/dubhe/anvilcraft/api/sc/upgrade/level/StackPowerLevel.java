package dev.dubhe.anvilcraft.api.sc.upgrade.level;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.util.CodecUtil;
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

public enum StackPowerLevel implements IUpgradeLevel<StackPowerLevel> {
    MIN(4),
    ONE(16, ModBlocks.NESTING_SHULKER_BOX.asStack(), 1, ModBlockTags.ANVIL_TIER_0),
    TWO(64, ModBlocks.SPACE_OVERCOMPRESSOR.asStack(), 1, ModBlockTags.ANVIL_TIER_1),
    THREE(512, ModBlocks.CONFINED_SPACE_ANVILON.asStack(), 64, ModBlockTags.ANVIL_TIER_2),
    FOUR(Integer.MAX_VALUE, ModBlocks.SINGULARITY_CRYSTAL.asStack(), 4, ModBlockTags.ANVIL_TIER_3)
    ;

    public static final Codec<StackPowerLevel> CODEC = StringRepresentable.fromEnum(StackPowerLevel::values);
    public static final StreamCodec<ByteBuf, StackPowerLevel> STREAM_CODEC = CodecUtil.enumStreamCodec(StackPowerLevel.class);
    @Getter
    private final int power;
    private final ItemStack material;
    @Getter
    private final int consumedCount;
    private final @Nullable TagKey<Block> anvilTag;
    private List<ItemStack> toolsCache;

    StackPowerLevel(int power) {
        this.power = power;
        this.material = ItemStack.EMPTY;
        this.consumedCount = 0;
        this.anvilTag = null;
    }

    StackPowerLevel(int power, ItemStack material, int consumedCount, TagKey<Block> anvilTag) {
        this.power = power;
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
    public StackPowerLevel prev() {
        return switch (this) {
            case MIN -> null;
            case ONE -> MIN;
            case TWO -> ONE;
            case THREE -> TWO;
            case FOUR -> THREE;
        };
    }

    @Override
    public StackPowerLevel next() {
        return switch (this) {
            case MIN -> ONE;
            case ONE -> TWO;
            case TWO -> THREE;
            case THREE -> FOUR;
            case FOUR -> null;
        };
    }

    @Override
    public StackPowerLevel min() {
        return StackPowerLevel.MIN;
    }

    @Override
    public StackPowerLevel max() {
        return StackPowerLevel.FOUR;
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getTypeId() {
        return "stack_power";
    }
}
