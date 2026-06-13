package dev.dubhe.anvilcraft.api.amulet.def;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.util.ISerializer;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import net.minecraft.core.Holder;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;

public interface IAmuletDefinition {
    Codec<IAmuletDefinition> DIRECT_CODEC = ModRegistries.AMULET_DEF_TYPE
        .byNameCodec()
        .dispatch(IAmuletDefinition::getType, Type::codec);
    Codec<Holder<IAmuletDefinition>> CODEC = RegistryFileCodec.create(ModRegistryKeys.AMULET_DEF, IAmuletDefinition.DIRECT_CODEC);
    Codec<IAmuletDefinition> HOLDER_HELPER_CODEC = CODEC.xmap(
        HolderHolder::new,
        value -> value instanceof HolderHolder(Holder<IAmuletDefinition> def) ? def : Holder.direct(value)
    );

    ItemStack create();

    boolean mayObtain(ServerPlayer victim, DamageSource source);

    Type<? extends IAmuletDefinition> getType();

    interface Type<T extends IAmuletDefinition> extends ISerializer<T> {
    }

    @VisibleForDebug
    record HolderHolder(Holder<IAmuletDefinition> def) implements IAmuletDefinition {
        @Override
        public ItemStack create() {
            return this.def.value().create();
        }

        @Override
        public boolean mayObtain(ServerPlayer victim, DamageSource source) {
            return this.def.value().mayObtain(victim, source);
        }

        @Override
        public Type<? extends IAmuletDefinition> getType() {
            return this.def.value().getType();
        }
    }
}
