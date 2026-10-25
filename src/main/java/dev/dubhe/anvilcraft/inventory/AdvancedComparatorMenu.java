package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.api.menu.MenuBlockEntityLookup;
import dev.dubhe.anvilcraft.block.entity.AdvancedComparatorBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class AdvancedComparatorMenu extends AbstractContainerMenu {
    @Getter
    private final AdvancedComparatorBlockEntity blockEntity;
    private final Level level;
    @Getter
    private AdvancedComparatorBlockEntity.Mode compareMode;
    @Getter
    private boolean outputInvert;
    @Getter
    private boolean redstoneControl;
    @Getter
    @Setter
    private int highLimit;
    @Getter
    @Setter
    private int lowLimit;

    public AdvancedComparatorMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, BlockEntity machine) {
        super(menuType, containerId);
        this.blockEntity = (AdvancedComparatorBlockEntity) machine;
        this.level = inventory.player.level();
        this.compareMode = this.blockEntity.getCompareMode();
        this.outputInvert = this.blockEntity.isOutputInvert();
        this.redstoneControl = this.blockEntity.isRedstoneControl();
        this.highLimit = this.blockEntity.getHighLimit();
        this.lowLimit = this.blockEntity.getLowLimit();
    }

    public AdvancedComparatorMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(menuType, containerId, inventory, readComparator(inventory, extraData));
    }

    private static AdvancedComparatorBlockEntity readComparator(Inventory inventory, FriendlyByteBuf extraData) {
        BlockEntity found = MenuBlockEntityLookup.find(
            inventory.player.level(),
            extraData.readBlockPos(),
            AdvancedComparatorBlockEntity.class
        );
        return Objects.requireNonNull(
            found instanceof AdvancedComparatorBlockEntity comparator
                ? comparator.readDataNbt(Objects.requireNonNull(extraData.readNbt()))
                : null
        );
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(
            ContainerLevelAccess.create(this.level, this.blockEntity.getBlockPos()),
            player,
            ModBlocks.ADVANCED_COMPARATOR.get()
        );
    }

    public void setCompareMode(byte mode) {
        this.compareMode = AdvancedComparatorBlockEntity.Mode.fromIndex(mode);
    }

    public void setOutputInvert(boolean isInvert) {
        this.outputInvert = isInvert;
    }

    public void setRedstoneControl(boolean redstoneControl) {
        this.redstoneControl = redstoneControl;
    }
}
