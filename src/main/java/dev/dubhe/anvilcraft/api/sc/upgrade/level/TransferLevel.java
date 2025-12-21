package dev.dubhe.anvilcraft.api.sc.upgrade.level;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.util.CodecUtil;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.util.InventoryUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Locale;
import java.util.function.Predicate;

public enum TransferLevel implements IUpgradeLevel {
    MIN(),
    ONE(ModBlocks.CHUTE.asStack(16), ModBlockTags.ANVIL_TIER_0),
    TWO(ModBlocks.CONFINED_TIME_ANVILON.asStack(16), ModBlockTags.ANVIL_TIER_1),
    THREE(Blocks.ENDER_CHEST.asItem().getDefaultInstance().copyWithCount(16), ModBlockTags.ANVIL_TIER_2),
    FOUR(ModBlocks.SINGULARITY_CRYSTAL.asStack(4), ModBlockTags.ANVIL_TIER_3)
    ;

    public static final Codec<TransferLevel> CODEC = StringRepresentable.fromEnum(TransferLevel::values);
    public static final StreamCodec<ByteBuf, TransferLevel> STREAM_CODEC = CodecUtil.enumStreamCodec(TransferLevel.class);
    private final ItemStack upgrade;
    private final Predicate<ItemStack> filter;

    TransferLevel() {
        this.upgrade = ItemStack.EMPTY;
        this.filter = stack -> false;
    }

    TransferLevel(ItemStack upgrade, TagKey<Block> anvilTag) {
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
        return TransferLevel.FOUR;
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public Component getDesc() {
        return Component.translatable("command.anvilcraft.storage.info.transfer.desc." + this.getSerializedName());
    }
}
