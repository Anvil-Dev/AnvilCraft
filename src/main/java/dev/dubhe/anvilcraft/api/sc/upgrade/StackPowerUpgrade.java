package dev.dubhe.anvilcraft.api.sc.upgrade;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.sc.upgrade.level.StackPowerLevel;
import dev.dubhe.anvilcraft.init.sc.ModUpgradeTypes;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@Getter
public class StackPowerUpgrade implements IUpgrade<StackPowerUpgrade> {
    private StackPowerLevel level = StackPowerLevel.MIN;

    public StackPowerUpgrade() {
    }

    private StackPowerUpgrade(StackPowerLevel level) {
        this.level = level;
    }

    @Override
    public Type getType() {
        return ModUpgradeTypes.STACK_POWER.get();
    }

    @Override
    public boolean tryUpgrade(Player player, ItemStack stack) {
        if (this.level == this.level.max()) return false;
        StackPowerLevel newLevel = StackPowerLevel.values()[this.level.ordinal() + 1];
        if (!newLevel.canUpgrade(player, stack)) return false;
        this.level = newLevel;
        return true;
    }

    @Override
    public void sync(StackPowerUpgrade upgrade) {
        this.level = upgrade.level;
    }

    public static class Type implements IUpgrade.Type<StackPowerUpgrade> {
        public static final MapCodec<StackPowerUpgrade> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            StackPowerLevel.CODEC
                .fieldOf("level")
                .forGetter(StackPowerUpgrade::getLevel)
        ).apply(inst, StackPowerUpgrade::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, StackPowerUpgrade> STREAM_CODEC = StreamCodec.composite(
            StackPowerLevel.STREAM_CODEC,
            StackPowerUpgrade::getLevel,
            StackPowerUpgrade::new
        );

        @Override
        public StackPowerUpgrade create() {
            return new StackPowerUpgrade();
        }

        @Override
        public MapCodec<StackPowerUpgrade> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, StackPowerUpgrade> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
