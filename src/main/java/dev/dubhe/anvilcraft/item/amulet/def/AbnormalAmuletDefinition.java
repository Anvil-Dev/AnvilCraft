package dev.dubhe.anvilcraft.item.amulet.def;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.amulet.def.IAmuletDefinition;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypeTags;
import dev.dubhe.anvilcraft.init.item.ModAmuletDefinitionTypes;
import dev.dubhe.anvilcraft.item.abnormal.IAbnormal;
import dev.dubhe.anvilcraft.item.abnormal.ICursed;
import dev.dubhe.anvilcraft.item.abnormal.ILevitation;
import dev.dubhe.anvilcraft.item.abnormal.IRadiation;
import dev.dubhe.anvilcraft.item.abnormal.ISuperHeavy;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;

public record AbnormalAmuletDefinition(ItemStackTemplate item) implements IAmuletDefinition {
    public AbnormalAmuletDefinition(ItemLike item) {
        this(new ItemStackTemplate(item.asItem()));
    }

    @Override
    public ItemStack create() {
        return this.item.withCount(1).create();
    }

    @Override
    public boolean mayObtain(ServerPlayer victim, DamageSource source) {
        if (!source.is(ModDamageTypeTags.ABNORMAL_AMULET_VALID)) {
            return false;
        }
        return IAbnormal.getAbnormalCount(victim, ICursed.class) > 0
               && IAbnormal.getAbnormalCount(victim, ILevitation.class) >= 64
               && IAbnormal.getAbnormalCount(victim, ISuperHeavy.class) > 0
               && IAbnormal.getAbnormalCount(victim, IRadiation.class) >= 1152;
    }

    @Override
    public Type getType() {
        return ModAmuletDefinitionTypes.ABNORMAL.get();
    }

    public static class Type implements IAmuletDefinition.Type<AbnormalAmuletDefinition> {
        public static final MapCodec<AbnormalAmuletDefinition> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ItemStackTemplate.CODEC
                .fieldOf("item")
                .forGetter(AbnormalAmuletDefinition::item)
        ).apply(inst, AbnormalAmuletDefinition::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, AbnormalAmuletDefinition> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC,
            AbnormalAmuletDefinition::item,
            AbnormalAmuletDefinition::new
        );

        @Override
        public MapCodec<AbnormalAmuletDefinition> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AbnormalAmuletDefinition> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
