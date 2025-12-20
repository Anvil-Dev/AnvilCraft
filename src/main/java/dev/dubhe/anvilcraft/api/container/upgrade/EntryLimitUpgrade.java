package dev.dubhe.anvilcraft.api.container.upgrade;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.container.upgrade.level.EntryLimitLevel;
import dev.dubhe.anvilcraft.init.sc.ModUpgradeTypes;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@Getter
public class EntryLimitUpgrade implements IUpgrade<EntryLimitUpgrade> {
    private EntryLimitLevel level = EntryLimitLevel.MIN;

    public EntryLimitUpgrade() {
    }

    private EntryLimitUpgrade(EntryLimitLevel level) {
        this.level = level;
    }

    @Override
    public Type getType() {
        return ModUpgradeTypes.ENTRY_LIMIT.get();
    }

    @Override
    public boolean tryUpgrade(Player player, ItemStack stack) {
        if (this.level == this.level.max()) return false;
        EntryLimitLevel newLevel = EntryLimitLevel.values()[this.level.ordinal() + 1];
        if (!newLevel.canUpgrade(player, stack)) return false;
        this.level = newLevel;
        return true;
    }

    @Override
    public void sync(EntryLimitUpgrade upgrade) {
        this.level = upgrade.level;
    }

    public static class Type implements IUpgrade.Type<EntryLimitUpgrade> {
        public static final MapCodec<EntryLimitUpgrade> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            EntryLimitLevel.CODEC
                .fieldOf("level")
                .forGetter(EntryLimitUpgrade::getLevel)
        ).apply(inst, EntryLimitUpgrade::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, EntryLimitUpgrade> STREAM_CODEC = StreamCodec.composite(
            EntryLimitLevel.STREAM_CODEC,
            EntryLimitUpgrade::getLevel,
            EntryLimitUpgrade::new
        );

        @Override
        public EntryLimitUpgrade create() {
            return new EntryLimitUpgrade();
        }

        @Override
        public MapCodec<EntryLimitUpgrade> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EntryLimitUpgrade> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
