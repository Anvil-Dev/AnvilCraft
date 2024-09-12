package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.init.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class DiskItem extends Item {

    public DiskItem(Properties properties) {
        super(properties);
    }

    /**
     * 磁盘中是否存储有数据
     */
    public static boolean hasDataStored(ItemStack stack) {
        return stack.has(ModComponents.DISK_DATA);
    }

    public static CompoundTag getData(ItemStack stack) {
        return stack
                .getOrDefault(ModComponents.DISK_DATA, new DiskData(new CompoundTag()))
                .tag();
    }

    /**
     *
     */
    public static CompoundTag createData(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        stack.set(ModComponents.DISK_DATA, new DiskData(tag));
        return tag;
    }

    public static void deleteData(ItemStack stack) {
        stack.remove(ModComponents.DISK_DATA);
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        return hasDataStored(stack);
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack stack,
            @NotNull Item.TooltipContext context,
            @NotNull List<Component> tooltipComponents,
            @NotNull TooltipFlag isAdvanced) {
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);
        if (hasDataStored(stack)) {
            ResourceLocation storedFrom = ResourceLocation.parse(getData(stack).getString("StoredFrom"));
            String name =
                    Component.translatable("block.anvilcraft." + storedFrom.getPath()).getString();
            tooltipComponents.add(Component.translatable("item.anvilcraft.disk.stored_from", name)
                    .withStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY)));
        }
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.PASS;
        if (context.getPlayer().isShiftKeyDown()) {
            return InteractionResult.SUCCESS;
        }
        if (!level.getBlockState(context.getClickedPos()).hasBlockEntity())
            return InteractionResult.PASS;
        BlockEntity blockEntity = level.getBlockEntity(context.getClickedPos());
        if (blockEntity instanceof IDiskCloneable diskCloneable) {
            ItemStack stack = context.getItemInHand();
            if (hasDataStored(stack)) {
                CompoundTag tag = getData(stack);
                if (!tag.getString("StoredFrom")
                        .equals(BuiltInRegistries.BLOCK_ENTITY_TYPE
                                .getKey(blockEntity.getType())
                                .toString())) return InteractionResult.PASS;
                diskCloneable.applyDiskData(tag);
            } else {
                CompoundTag tag = createData(stack);
                tag.putString(
                        "StoredFrom",
                        BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()).toString());
                diskCloneable.storeDiskData(tag);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            Level level, @NotNull Player player, @NotNull InteractionHand usedHand) {
        if (!level.isClientSide && player.isShiftKeyDown()) {
            ItemStack itemStack = player.getItemInHand(usedHand);
            if (hasDataStored(itemStack)) {
                deleteData(itemStack);
            }
            return InteractionResultHolder.success(itemStack);
        }
        return super.use(level, player, usedHand);
    }

    public record DiskData(CompoundTag tag) {
        public static final Codec<DiskData> CODEC = RecordCodecBuilder.create(
                ins -> ins.group(CompoundTag.CODEC.fieldOf("tag").forGetter(DiskData::tag))
                        .apply(ins, DiskData::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, DiskData> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, DiskData::tag, DiskData::new);

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj instanceof DiskData diskData) {
                return tag.equals(diskData.tag);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(tag);
        }
    }
}
