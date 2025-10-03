package dev.dubhe.anvilcraft.api.container.upgrade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.container.upgrade.level.TransferLevel;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

@Getter(AccessLevel.PRIVATE)
public class Upgrades {
    public static final Codec<Upgrades> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        EntryLimitUpgrade.Type.CODEC
            .fieldOf("entryLimit")
            .forGetter(Upgrades::getEntryLimitUpgrade),
        StackPowerUpgrade.Type.CODEC
            .fieldOf("stackPower")
            .forGetter(Upgrades::getStackPowerUpgrade),
        TransferUpgrade.Type.CODEC
            .fieldOf("transfer")
            .forGetter(Upgrades::getTransferUpgrade)
    ).apply(ins, Upgrades::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Upgrades> STREAM_CODEC = StreamCodec.composite(
        EntryLimitUpgrade.Type.STREAM_CODEC,
        Upgrades::getEntryLimitUpgrade,
        StackPowerUpgrade.Type.STREAM_CODEC,
        Upgrades::getStackPowerUpgrade,
        TransferUpgrade.Type.STREAM_CODEC,
        Upgrades::getTransferUpgrade,
        Upgrades::new
    );
    private final EntryLimitUpgrade entryLimitUpgrade;
    private final StackPowerUpgrade stackPowerUpgrade;
    private final TransferUpgrade transferUpgrade;

    public Upgrades() {
        this(new EntryLimitUpgrade(), new StackPowerUpgrade(), new TransferUpgrade());
    }

    private Upgrades(
        EntryLimitUpgrade entryLimitUpgrade,
        StackPowerUpgrade stackPowerUpgrade,
        TransferUpgrade transferUpgrade
    ) {
        this.entryLimitUpgrade = entryLimitUpgrade;
        this.stackPowerUpgrade = stackPowerUpgrade;
        this.transferUpgrade = transferUpgrade;
    }

    public int getEntryLimit() {
        return this.entryLimitUpgrade.getLevel().getLimit();
    }

    public int getStackPower() {
        return this.stackPowerUpgrade.getLevel().getPower();
    }

    public TransferLevel getTransfer() {
        return this.transferUpgrade.getLevel();
    }

    public void sync(Upgrades upgrades) {
        this.entryLimitUpgrade.sync(upgrades.entryLimitUpgrade);
        this.stackPowerUpgrade.sync(upgrades.stackPowerUpgrade);
        this.transferUpgrade.sync(upgrades.transferUpgrade);
    }
}
