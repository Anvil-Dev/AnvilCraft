package dev.dubhe.anvilcraft.api.sc.upgrade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.sc.upgrade.level.IUpgradeLevel;
import dev.dubhe.anvilcraft.network.multiple.ShulkerContainerPackets;
import dev.dubhe.anvilcraft.saved.sc.SCStorage;
import dev.dubhe.anvilcraft.util.InventoryUtil;
import dev.dubhe.anvilcraft.util.PlayerUtil;
import io.netty.buffer.ByteBuf;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

@Getter
@EqualsAndHashCode
public class Upgrade<T extends Enum<T> & IUpgradeLevel<T>> {
    private T now;
    private int progress;

    public Upgrade(T now) {
        this(now, 0);
    }

    private Upgrade(T now, int progress) {
        this.now = now;
        this.progress = progress;
    }

    public @Nullable T getNext() {
        return this.now.next();
    }

    public UpgradeResult canUpgrade(Player player, ItemStack material) {
        if (this.getNext() == null) return UpgradeResult.ALREADY_MAX;
        if (player.hasInfiniteMaterials()) return UpgradeResult.CAN_UPGRADE;

        boolean isMaterial = this.getNext().isMaterial(material);
        boolean isTool = InventoryUtil.hasItem(player.getInventory(), this.getNext()::isTool);

        if (!isMaterial && !isTool) {
            return UpgradeResult.NO_ANY;
        } else if (!isMaterial) {
            return UpgradeResult.NO_MATERIAL;
        } else if (!isTool) {
            return UpgradeResult.NO_TOOL;
        } else {
            return UpgradeResult.CAN_UPGRADE;
        }
    }

    public ItemStack upgrade(Player player, ItemStack material, SCStorage storage) {
        if (PlayerUtil.isClient(player)) return material;
        if (player.hasInfiniteMaterials() && this.getNext() != null) {
            this.progress = 0;
            this.now = this.getNext();
            PacketDistributor.sendToAllPlayers(new ShulkerContainerPackets.UpgradesSync(storage.getId(), storage.getUpgrades()));
            return ItemStack.EMPTY;
        }
        if (this.canUpgrade(player, material) != UpgradeResult.CAN_UPGRADE) return material;
        int progress = material.getCount() + this.progress;
        if (progress < this.getNext().getConsumedCount()) {
            this.progress += material.getCount();
            PacketDistributor.sendToAllPlayers(new ShulkerContainerPackets.UpgradesSync(storage.getId(), storage.getUpgrades()));
            return ItemStack.EMPTY;
        } else {
            this.progress = 0;
            var result = ItemStack.EMPTY;
            if (progress > this.getNext().getConsumedCount()) {
                result = material.copyWithCount(material.getCount() - (progress - this.getNext().getConsumedCount()));
            }
            this.now = this.getNext();
            PacketDistributor.sendToAllPlayers(new ShulkerContainerPackets.UpgradesSync(storage.getId(), storage.getUpgrades()));
            return result;
        }
    }

    public void sync(Upgrade<T> progress) {
        this.now = progress.now;
        this.progress = progress.progress;
    }

    public static <T extends Enum<T> & IUpgradeLevel<T>> MapCodec<Upgrade<T>> codec(Codec<T> codec, String name, T min) {
        return RecordCodecBuilder.<Upgrade<T>>mapCodec(ins -> ins.group(
            codec
                .optionalFieldOf("level", min)
                .forGetter(Upgrade::getNow),
            Codec.INT
                .optionalFieldOf("progress", 0)
                .forGetter(Upgrade::getProgress)
        ).apply(ins, Upgrade::new)).codec().optionalFieldOf(name, new Upgrade<>(min));
    }

    public static <B extends ByteBuf, T extends Enum<T> & IUpgradeLevel<T>> StreamCodec<B, Upgrade<T>> streamCodec(
        StreamCodec<B, T> codec
    ) {
        return StreamCodec.composite(
            codec,
            Upgrade::getNow,
            ByteBufCodecs.VAR_INT,
            Upgrade::getProgress,
            Upgrade::new
        );
    }
}
