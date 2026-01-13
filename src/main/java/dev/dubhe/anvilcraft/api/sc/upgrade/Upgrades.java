package dev.dubhe.anvilcraft.api.sc.upgrade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.sc.upgrade.level.EntryLimitLevel;
import dev.dubhe.anvilcraft.api.sc.upgrade.level.StackPowerLevel;
import dev.dubhe.anvilcraft.api.sc.upgrade.level.TransferLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Getter
public class Upgrades {
    public static final MapCodec<Upgrades> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Upgrade.codec(EntryLimitLevel.CODEC, "entryLimit", EntryLimitLevel.MIN)
            .forGetter(Upgrades::getEntryLimitUpgrade),
        Upgrade.codec(StackPowerLevel.CODEC, "stackPower", StackPowerLevel.MIN)
            .forGetter(Upgrades::getStackPowerUpgrade),
        Upgrade.codec(TransferLevel.CODEC, "transfer", TransferLevel.MIN)
            .forGetter(Upgrades::getTransferUpgrade),
        UUIDUtil.CODEC
            .optionalFieldOf("owner")
            .forGetter(Upgrades::getOwnerOp),
        Codec.BOOL
            .optionalFieldOf("share", false)
            .forGetter(Upgrades::isShare)
    ).apply(ins, Upgrades::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Upgrades> STREAM_CODEC = StreamCodec.composite(
        Upgrade.streamCodec(EntryLimitLevel.STREAM_CODEC),
        Upgrades::getEntryLimitUpgrade,
        Upgrade.streamCodec(StackPowerLevel.STREAM_CODEC),
        Upgrades::getStackPowerUpgrade,
        Upgrade.streamCodec(TransferLevel.STREAM_CODEC),
        Upgrades::getTransferUpgrade,
        ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
        Upgrades::getOwnerOp,
        ByteBufCodecs.BOOL,
        Upgrades::isShare,
        Upgrades::new
    );
    private final Upgrade<EntryLimitLevel> entryLimitUpgrade;
    private final Upgrade<StackPowerLevel> stackPowerUpgrade;
    private final Upgrade<TransferLevel> transferUpgrade;
    @Setter
    private @Nullable UUID owner;
    @Setter
    private boolean share;

    public Upgrades() {
        this.entryLimitUpgrade = new Upgrade<>(EntryLimitLevel.MIN);
        this.stackPowerUpgrade = new Upgrade<>(StackPowerLevel.MIN);
        this.transferUpgrade = new Upgrade<>(TransferLevel.MIN);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Upgrades(
        Upgrade<EntryLimitLevel> entryLimitUpgrade,
        Upgrade<StackPowerLevel> stackPowerUpgrade,
        Upgrade<TransferLevel> transferUpgrade,
        Optional<UUID> owner,
        boolean share
    ) {
        this.entryLimitUpgrade = entryLimitUpgrade;
        this.stackPowerUpgrade = stackPowerUpgrade;
        this.transferUpgrade = transferUpgrade;
        this.owner = owner.orElse(null);
        this.share = share;
    }

    private Optional<UUID> getOwnerOp() {
        return Optional.ofNullable(this.owner);
    }

    public Upgrade<?> getUpgrade(int index) {
        return switch (index) {
            case 0 -> this.getEntryLimitUpgrade();
            case 1 -> this.getStackPowerUpgrade();
            case 2 -> this.getTransferUpgrade();
            default -> throw new IllegalStateException("Unexpected value: " + index);
        };
    }

    public int getEntryLimit() {
        return this.entryLimitUpgrade.getNow().getLimit();
    }

    public EntryLimitLevel getEntryLevel() {
        return this.entryLimitUpgrade.getNow();
    }

    public int getMaxStackSize(ItemStack stack) {
        return stack.getMaxStackSize() * this.getStackPower();
    }

    public int getStackPower() {
        return this.stackPowerUpgrade.getNow().getPower();
    }

    public StackPowerLevel getStackLevel() {
        return this.stackPowerUpgrade.getNow();
    }

    public TransferLevel getTransfer() {
        return this.transferUpgrade.getNow();
    }

    public void sync(Upgrades upgrades) {
        this.entryLimitUpgrade.sync(upgrades.entryLimitUpgrade);
        this.stackPowerUpgrade.sync(upgrades.stackPowerUpgrade);
        this.transferUpgrade.sync(upgrades.transferUpgrade);
        Optional.ofNullable(upgrades.owner).ifPresent(id -> this.owner = id);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Upgrades upgrades)) return false;
        return this.isShare() == upgrades.isShare()
               && Objects.equals(this.getEntryLimitUpgrade(), upgrades.getEntryLimitUpgrade())
               && Objects.equals(this.getStackPowerUpgrade(), upgrades.getStackPowerUpgrade())
               && Objects.equals(this.getTransferUpgrade(), upgrades.getTransferUpgrade())
               && Objects.equals(this.getOwner(), upgrades.getOwner());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.entryLimitUpgrade, this.stackPowerUpgrade, this.transferUpgrade);
    }
}
