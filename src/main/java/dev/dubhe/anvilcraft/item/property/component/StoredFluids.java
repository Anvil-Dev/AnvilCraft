package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Objects;

public record StoredFluids(List<FluidStack> fluids) {
    public static final StoredFluids EMPTY = new StoredFluids(List.of());
    public static final Codec<StoredFluids> CODEC = FluidStack.OPTIONAL_CODEC.listOf().xmap(
        StoredFluids::new,
        StoredFluids::fluids
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, StoredFluids> STREAM_CODEC = FluidStack.OPTIONAL_STREAM_CODEC
        .apply(ByteBufCodecs.list())
        .map(StoredFluids::new, StoredFluids::fluids);

    public StoredFluids {
        fluids = fluids.stream()
            .filter(Objects::nonNull)
            .map(FluidStack::copy)
            .toList();
    }

    public boolean isEmpty() {
        return this.fluids.stream().allMatch(FluidStack::isEmpty);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StoredFluids(List<FluidStack> otherFluids))) return false;
        if (this.fluids.size() != otherFluids.size()) return false;
        for (int i = 0; i < this.fluids.size(); i++) {
            if (!FluidStack.matches(this.fluids.get(i), otherFluids.get(i))) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (FluidStack fluid : this.fluids) {
            int fluidHash = fluid.isEmpty() ? 0 : 31 * FluidStack.hashFluidAndComponents(fluid) + fluid.getAmount();
            result = 31 * result + fluidHash;
        }
        return result;
    }
}
