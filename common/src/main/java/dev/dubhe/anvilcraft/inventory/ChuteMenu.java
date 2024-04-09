package dev.dubhe.anvilcraft.inventory;


import dev.dubhe.anvilcraft.api.depository.ItemDepositorySlot;
import dev.dubhe.anvilcraft.block.entity.ChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.IFilterBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class ChuteMenu extends BaseMachineMenu implements IFilterMenu {

    public final ChuteBlockEntity blockEntity;
    private final Level level;

    public ChuteMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, @NotNull FriendlyByteBuf extraData) {
        this(menuType, containerId, inventory, inventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public ChuteMenu(MenuType<?> menuType, int containerId, Inventory inventory, BlockEntity blockEntity) {
        super(menuType, containerId, inventory);
        this.blockEntity = (ChuteBlockEntity) blockEntity;
        this.level = inventory.player.level();

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                this.addSlot(new ItemDepositorySlot(this.blockEntity.getDepository(), i * 3 + j, 62 + j * 18, 18 + i * 18));
            }
        }
    }

    @Override
    public void setDirection(Direction direction) {
        blockEntity.setDirection(direction);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.CHUTE.get());
    }

    @Override
    public IFilterBlockEntity getFilterBlockEntity() {
        return blockEntity;
    }
}
