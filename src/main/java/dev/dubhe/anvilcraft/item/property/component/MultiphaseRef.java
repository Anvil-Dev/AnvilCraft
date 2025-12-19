package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.uuid.CreateOnFirstUuidProvider;
import dev.dubhe.anvilcraft.api.uuid.DirectUuidProvider;
import dev.dubhe.anvilcraft.api.uuid.IUuidProvider;
import dev.dubhe.anvilcraft.api.uuid.NoUuidProvider;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.network.multiple.MultiphasePackets;
import dev.dubhe.anvilcraft.saved.multiphase.Multiphase;
import dev.dubhe.anvilcraft.saved.multiphase.Multiphases;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Function;

public record MultiphaseRef(IUuidProvider id) {
    // TODO: 兼容性支持结束后将此常量重命名为 CODEC
    public static final MapCodec<MultiphaseRef> TRUE_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        IUuidProvider.CODEC
            .fieldOf("id")
            .forGetter(MultiphaseRef::id)
    ).apply(ins, MultiphaseRef::new));
    // TODO: 兼容性支持结束后移除此常量
    public static final MapCodec<MultiphaseRef> CODEC = Codec.mapEither(TRUE_CODEC, Multiphase.CODEC)
        .xmap(
            either -> either.map(Function.identity(), MultiphaseRef::new),
            Either::left
        );
    public static final StreamCodec<RegistryFriendlyByteBuf, MultiphaseRef> STREAM_CODEC = StreamCodec.composite(
        IUuidProvider.STREAM_CODEC,
        MultiphaseRef::id,
        MultiphaseRef::new
    );

    public MultiphaseRef() {
        this(new NoUuidProvider());
    }

    public MultiphaseRef(UUID id) {
        this(new DirectUuidProvider(id));
    }

    public MultiphaseRef(Multiphase multiphase) {
        this(Multiphases.get().put(multiphase));
    }

    public MultiphaseRef(Component name) {
        this(new CreateOnFirstUuidProvider(() -> Multiphases.get().put(Multiphase.make(name))));
    }

    public @Nullable Multiphase toMultiphase() {
        if (this.isEmpty()) return null;
        return Multiphases.get().getOrCreate(this.id.get());
    }

    public void discard(RegistryAccess registries) {
        if (this.isEmpty()) return;
        Multiphases.get().discard(this.id.get(), registries);
    }

    public void applyToStack(ItemStack stack) {
        if (this.isEmpty()) return;
        this.toMultiphase().applyToStack(stack);
    }

    public void cyclePhases(ItemStack stack) {
        if (this.isEmpty()) return;
        this.toMultiphase().cyclePhases(stack);
    }

    public void cyclePhases(ItemStack stack, byte index) {
        if (this.isEmpty()) return;
        this.toMultiphase().cyclePhases(stack, index);
    }

    public boolean isEmpty() {
        return !this.id.canGet();
    }

    public static void tick(Player player) {
        if (!Util.isClient()) return;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);
            if (stack.has(ModComponents.MULTIPHASE) && stack.get(ModComponents.MULTIPHASE).isEmpty()) {
                PacketDistributor.sendToServer(new MultiphasePackets.RefSync(i));
            }
        }
    }
}
