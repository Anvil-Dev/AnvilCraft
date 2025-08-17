package dev.dubhe.anvilcraft.recipe.anvil.util;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * NBT谓词
 * <p>
 * 用于匹配NBT数据的谓词，支持物品堆栈和实体的NBT数据匹配
 * </p>
 */
@Getter
public class NbtPredicate implements Predicate<Tag> {
    /**
     * NbtPredicate编解码器
     */
    public static final Codec<NbtPredicate> CODEC = TagParser.LENIENT_CODEC.xmap(NbtPredicate::new, NbtPredicate::getTag);

    /**
     * NbtPredicate流编解码器
     */
    public static final StreamCodec<ByteBuf, NbtPredicate> STREAM_CODEC = ByteBufCodecs.COMPOUND_TAG.map(
        NbtPredicate::new,
        NbtPredicate::getTag
    );

    /**
     * NBT标签
     */
    private final CompoundTag tag;

    /**
     * 构造一个NBT谓词
     *
     * @param tag NBT标签
     */
    public NbtPredicate(CompoundTag tag) {
        this.tag = tag;
    }

    @Override
    public boolean test(Tag tag) {
        return tag != null && NbtUtils.compareNbt(this.tag, tag, true);
    }

    /**
     * 测试物品堆栈的NBT数据是否匹配
     *
     * @param stack 物品堆栈
     * @return 是否匹配
     */
    public boolean test(@NotNull ItemStack stack) {
        CustomData customdata = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return customdata.matchedBy(this.tag);
    }

    /**
     * 测试实体的NBT数据是否匹配
     *
     * @param entity 实体
     * @return 是否匹配
     */
    public boolean test(Entity entity) {
        return this.test(getEntityTagToCompare(entity));
    }

    /**
     * 获取实体用于比较的NBT标签
     *
     * @param entity 实体
     * @return NBT标签
     */
    public static @NotNull CompoundTag getEntityTagToCompare(@NotNull Entity entity) {
        CompoundTag compoundtag = entity.saveWithoutId(new CompoundTag());
        if (entity instanceof Player) {
            ItemStack itemstack = ((Player) entity).getInventory().getSelected();
            if (!itemstack.isEmpty()) {
                compoundtag.put("SelectedItem", itemstack.save(entity.registryAccess()));
            }
        }
        return compoundtag;
    }
}