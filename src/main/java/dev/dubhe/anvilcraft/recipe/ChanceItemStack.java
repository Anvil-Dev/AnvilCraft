package dev.dubhe.anvilcraft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import org.jetbrains.annotations.Contract;

@Getter
public class ChanceItemStack {
    public static final Codec<ChanceItemStack> CODEC = Codec.lazyInitialized(
        () -> RecordCodecBuilder.create(ins -> ins.group(
            ItemStack.OPTIONAL_CODEC.fieldOf("stack").forGetter(ChanceItemStack::getStack),
            NumberProviders.CODEC.fieldOf("amount").forGetter(ChanceItemStack::getAmount)
        ).apply(ins, ChanceItemStack::new))
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ChanceItemStack> STREAM_CODEC = StreamCodec.composite(
        ItemStack.OPTIONAL_STREAM_CODEC, ChanceItemStack::getStack,
        RecipeUtil.NUMBER_PROVIDER_STREAM_CODEC, ChanceItemStack::getAmount,
        ChanceItemStack::new
    );

    private final ItemStack stack;
    @Setter
    private NumberProvider amount;

    public ChanceItemStack(ItemStack stack, NumberProvider amount) {
        this.stack = stack;
        this.amount = amount;
    }

    public static ChanceItemStack of(ItemStack stack) {
        return new ChanceItemStack(stack, ConstantValue.exactly(1));
    }

    public ChanceItemStack withChance(float chance) {
        setAmount(BinomialDistributionGenerator.binomial(1, chance));
        return this;
    }

    @Contract(" -> new")
    public ChanceItemStack copy() {
        return new ChanceItemStack(stack.copy(), amount);
    }
}
