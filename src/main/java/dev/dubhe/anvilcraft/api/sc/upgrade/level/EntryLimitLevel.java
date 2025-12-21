package dev.dubhe.anvilcraft.api.sc.upgrade.level;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.util.CodecUtil;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.util.InventoryUtil;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Locale;
import java.util.function.Predicate;

public enum EntryLimitLevel implements IUpgradeLevel {
    MIN(54),
    ONE(108, ModBlocks.NESTING_SHULKER_BOX.asStack(1), ModBlockTags.ANVIL_TIER_0),
    TWO(216, ModBlocks.SPACE_OVERCOMPRESSOR.asStack(1), ModBlockTags.ANVIL_TIER_1),
    THREE(864, ModBlocks.CONFINED_SPACE_ANVILON.asStack(64), ModBlockTags.ANVIL_TIER_2),
    FOUR(Integer.MAX_VALUE, ModBlocks.SINGULARITY_CRYSTAL.asStack(4), ModBlockTags.ANVIL_TIER_3)
    ;

    public static final Codec<EntryLimitLevel> CODEC = StringRepresentable.fromEnum(EntryLimitLevel::values);
    public static final StreamCodec<ByteBuf, EntryLimitLevel> STREAM_CODEC = CodecUtil.enumStreamCodec(EntryLimitLevel.class);
    @Getter
    private final int limit;
    private final ItemStack upgrade;
    private final Predicate<ItemStack> filter;

    EntryLimitLevel(int limit) {
        this.limit = limit;
        this.upgrade = ItemStack.EMPTY;
        this.filter = stack -> false;
    }

    EntryLimitLevel(int limit, ItemStack upgrade, TagKey<Block> anvilTag) {
        this.limit = limit;
        this.upgrade = upgrade;
        // noinspection deprecation
        this.filter = stack -> stack.getItem() instanceof AnvilHammerItem hammer
                               && hammer.getAnvil().builtInRegistryHolder().is(anvilTag);
    }

    @Override
    public boolean canUpgrade(Player player, ItemStack upgrade) {
        return upgrade.getCount() == this.upgrade.getCount()
               && ItemStack.isSameItemSameComponents(upgrade, this.upgrade)
               && InventoryUtil.hasItemInCompat(player, this.filter);
    }

    @Override
    public IUpgradeLevel max() {
        return EntryLimitLevel.FOUR;
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
