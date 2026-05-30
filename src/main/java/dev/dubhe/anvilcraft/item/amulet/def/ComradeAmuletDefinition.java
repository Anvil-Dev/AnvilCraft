package dev.dubhe.anvilcraft.item.amulet.def;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.amulet.def.IAmuletDefinition;
import dev.dubhe.anvilcraft.init.item.ModAmuletDefinitionTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.scores.Team;

public record ComradeAmuletDefinition(ItemStackTemplate item) implements IAmuletDefinition {
    public ComradeAmuletDefinition(ItemLike item) {
        this(new ItemStackTemplate(item.asItem()));
    }

    @Override
    public ItemStack create() {
        return this.item.withCount(1).create();
    }

    @Override
    public boolean mayObtain(ServerPlayer victim, DamageSource source) {
        ServerPlayer murder = Util.castSafely(source.getEntity(), ServerPlayer.class).orElse(null);
        if (murder == null) return false;
        Team victimTeam = victim.getTeam();
        Team murderTeam = murder.getTeam();
        return victimTeam == null ? murderTeam == null : victimTeam.isAlliedTo(murderTeam);
    }

    @Override
    public Type getType() {
        return ModAmuletDefinitionTypes.COMRADE.get();
    }

    public static class Type implements IAmuletDefinition.Type<ComradeAmuletDefinition> {
        public static final MapCodec<ComradeAmuletDefinition> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ItemStackTemplate.CODEC
                .fieldOf("item")
                .forGetter(ComradeAmuletDefinition::item)
        ).apply(inst, ComradeAmuletDefinition::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ComradeAmuletDefinition> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC,
            ComradeAmuletDefinition::item,
            ComradeAmuletDefinition::new
        );

        @Override
        public MapCodec<ComradeAmuletDefinition> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ComradeAmuletDefinition> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
