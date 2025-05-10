package dev.dubhe.anvilcraft.mixin.compat;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.crafting.IMolecularAssemblerSupportedPattern;
import appeng.crafting.pattern.AECraftingPattern;
import dev.dubhe.anvilcraft.api.DeferTaskSubmittable;
import dev.dubhe.anvilcraft.api.itemhandler.PollableFilteredItemStackHandler;
import dev.dubhe.anvilcraft.block.entity.BatchCrafterBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

@Mixin(BatchCrafterBlockEntity.class)
public abstract class BatchCrafterBlockEntityMixin
    extends BlockEntity
    implements ICraftingMachine, IMolecularAssemblerSupportedPattern.CraftingGridAccessor, DeferTaskSubmittable<BatchCrafterBlockEntity> {

    @Shadow
    @Final
    private PollableFilteredItemStackHandler itemHandler;

    @Shadow
    protected abstract boolean ejectItems(ItemStack result, List<ItemStack> craftRemaining, Direction direction);

    @Shadow
    @Final
    private CraftingContainer craftingContainer;
    @Unique
    private final Deque<Consumer<BatchCrafterBlockEntity>> deferredTasks = new ArrayDeque<>();


    public BatchCrafterBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        Component name = ModBlocks.BATCH_CRAFTER.asItem().getDescription();
        AEItemKey icon = AEItemKey.of(ModBlocks.BATCH_CRAFTER);
        return new PatternContainerGroup(icon, name, List.of());
    }

    @Override
    public boolean pushPattern(
        IPatternDetails patternDetails,
        KeyCounter[] inputs,
        Direction ejectionDirection
    ) {
        if (patternDetails instanceof AECraftingPattern pattern && this.itemHandler.isEmpty()) {
            pattern.fillCraftingGrid(inputs, this);
            if (level.isClientSide) {
                this.setChanged();
            } else {
                this.level.blockEntityChanged(worldPosition);
                this.submitTask(it -> {
                    ItemStack result = pattern.assemble(this.craftingContainer.asCraftInput(), level);
                    if (result.isEmpty()) return;
                    this.ejectItems(result, List.of(), ejectionDirection);
                    int amount = Math.toIntExact(result.getCount() / pattern.getOutputs().getFirst().amount());
                    for (int i = 0; i < itemHandler.getSlots(); i++) {
                        itemHandler.extractItem(i, amount, false);
                    }
                });
            }
            return true;
        }
        return false;
    }

    @Inject(
        method = "tick",
        at = @At("HEAD")
    )
    void runDeferredTask(Level level, BlockPos pos, CallbackInfo ci) {
        for (Consumer<BatchCrafterBlockEntity> deferredTask : this.deferredTasks) {
            deferredTask.accept((BatchCrafterBlockEntity) (Object) this);
        }
    }

    @Override
    public boolean acceptsPlans() {
        return this.itemHandler.isEmpty();
    }

    @Override
    public void set(int slot, ItemStack stack) {
        this.itemHandler.setStackInSlot(slot, stack);
    }

    @Override
    public void submitTask(Consumer<BatchCrafterBlockEntity> fn) {
        deferredTasks.add(fn);
    }
}
