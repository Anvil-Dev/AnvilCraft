package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

import java.util.List;
import java.util.Optional;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Categories implements ValueIOSerializable {
    public static final MapCodec<Categories> CODEC = CodecUtil.mapCodec(
        ICategory.CODEC
            .listOf()
            .fieldOf("custom")
            .forGetter(Categories::getCustom),
        ICategory.CODEC
            .listOf()
            .fieldOf("enabled")
            .forGetter(Categories::getEnabled),
        Categories::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, Categories> STREAM_CODEC = StreamCodec.composite(
        ICategory.STREAM_CODEC.apply(ByteBufCodecs.list()),
        Categories::getCustom,
        ICategory.STREAM_CODEC.apply(ByteBufCodecs.list()),
        Categories::getEnabled,
        Categories::new
    );
    private List<ICategory> custom;
    private List<ICategory> enabled;

    public void enable(ICategory category) {
        this.enabled.add(category);
    }

    public void enable(int index, ICategory category) {
        this.enabled.add(index, category);
    }

    public void pinToTop(int index) {
        this.enabled.addFirst(this.enabled.remove(index));
    }

    public void addCustom(ICategory category) {
        this.custom.add(category);
    }

    public void addCustom(ItemStack filter) {
        this.custom.add(FilterCategory.from(filter));
    }

    public void sync(Categories categories) {
        this.custom = categories.custom;
        this.enabled = categories.enabled;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("categories", Categories.CODEC.codec(), this);
    }

    @Override
    public void deserialize(ValueInput input) {
        Optional<Categories> categoriesOp = input.read("categories", Categories.CODEC.codec());
        if (categoriesOp.isEmpty()) {
            return;
        }
        Categories categories = categoriesOp.get();

        this.custom = categories.custom;
        this.enabled = categories.enabled;
    }
}
