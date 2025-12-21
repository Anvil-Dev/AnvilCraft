package dev.dubhe.anvilcraft.api.sc.upgrade;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.sc.upgrade.level.TransferLevel;
import dev.dubhe.anvilcraft.init.sc.ModUpgradeTypes;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@Getter
public class TransferUpgrade implements IUpgrade<TransferUpgrade> {
    private TransferLevel level = TransferLevel.MIN;

    public TransferUpgrade() {
    }

    private TransferUpgrade(TransferLevel level) {
        this.level = level;
    }

    @Override
    public Type getType() {
        return ModUpgradeTypes.TRANSFER.get();
    }

    @Override
    public boolean tryUpgrade(Player player, ItemStack stack) {
        if (this.level == this.level.max()) return false;
        TransferLevel newLevel = TransferLevel.values()[this.level.ordinal() + 1];
        if (!newLevel.canUpgrade(player, stack)) return false;
        this.level = newLevel;
        return true;
    }

    @Override
    public void sync(TransferUpgrade upgrade) {
        this.level = upgrade.level;
    }

    public static class Type implements IUpgrade.Type<TransferUpgrade> {
        public static final MapCodec<TransferUpgrade> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            TransferLevel.CODEC
                .fieldOf("level")
                .forGetter(TransferUpgrade::getLevel)
        ).apply(inst, TransferUpgrade::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, TransferUpgrade> STREAM_CODEC = StreamCodec.composite(
            TransferLevel.STREAM_CODEC,
            TransferUpgrade::getLevel,
            TransferUpgrade::new
        );

        @Override
        public TransferUpgrade create() {
            return new TransferUpgrade();
        }

        @Override
        public MapCodec<TransferUpgrade> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TransferUpgrade> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
