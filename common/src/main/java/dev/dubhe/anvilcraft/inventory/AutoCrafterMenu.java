package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.api.depository.ItemDepositorySlot;
import dev.dubhe.anvilcraft.block.entity.AutoCrafterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.IFilterBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.inventory.component.ReadOnlySlot;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@SuppressWarnings("resource")
@Getter
public class AutoCrafterMenu extends BaseMachineMenu implements IFilterMenu {
    public final AutoCrafterBlockEntity blockEntity;
    private final Slot resultSlot;
    private final Level level;

    public AutoCrafterMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, @NotNull FriendlyByteBuf extraData) {
        this(menuType, containerId, inventory, inventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public AutoCrafterMenu(MenuType<?> menuType, int containerId, Inventory inventory, BlockEntity blockEntity) {
        super(menuType, containerId, inventory);
        AutoCrafterMenu.checkContainerSize(inventory, 9);

        this.blockEntity = (AutoCrafterBlockEntity) blockEntity;
        this.level = inventory.player.level();

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                this.addSlot(new ItemDepositorySlot(this.blockEntity.getDepository(), i * 3 + j, 26 + j * 18, 18 + i * 18));
            }
        }

        this.addSlot(resultSlot = new ReadOnlySlot(new SimpleContainer(1), 0, 8 + 7 * 18, 18 + 2 * 18));
        this.onChanged();
    }

    @Override
    public void setDirection(Direction direction) {
        blockEntity.setDirection(direction);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.AUTO_CRAFTER.get());
    }

    @Override
    public IFilterBlockEntity getFilterBlockEntity() {
        return this.blockEntity;
    }

    @Override
    public void setItem(int slotId, int stateId, @NotNull ItemStack stack) {
        super.setItem(slotId, stateId, stack);
        this.onChanged();
    }

    private void onChanged() {
        if (!level.isClientSide) return;
        RecipeManager recipeManager = level.getRecipeManager();
        Optional<CraftingRecipe> recipe = recipeManager.getRecipeFor(RecipeType.CRAFTING, blockEntity.getCraftingContainer(), level);
        if (recipe.isEmpty()) return;
        ItemStack resultItem = recipe.get().getResultItem(level.registryAccess());
        this.resultSlot.set(resultItem);
    }
}
