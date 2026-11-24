package dev.dubhe.anvilcraft.block.entity;

import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.cache.ItemResourceHandlerCache;
import dev.anvilcraft.lib.v2.recipe.event.InWorldRecipeEvent;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.predicate.item.HasItemIngredient;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeData;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.block.ICauldron;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.event.LargeCauldronEvent;
import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.LargeCauldronFluidHandler;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.LargeCauldronInputHandler;
import dev.dubhe.anvilcraft.api.recipe.CauldronProcessing;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.workstation.GiantAnvilBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluidTags;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.FluidMixingRecipe;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import dev.dubhe.anvilcraft.recipe.LiquidEnchantmentCauldronRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.DamageAnvil;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.util.RecipeOutputTracker;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import dev.dubhe.anvilcraft.util.EntityUtil;
import dev.dubhe.anvilcraft.util.ExperienceMending;
import dev.dubhe.anvilcraft.util.FireReforgingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class LargeCauldronBlockEntity extends BlockEntity
    implements IItemResourceHandlerHolder, ItemResourceHandlerCache, IFluidResourceHandlerHolder, ICauldron {
    @Override
    public boolean supportsMultipleFluidOutputs() {
        return true;
    }

    public static final int OUTPUT_SLOTS = 32;
    private static final int[][] INPUT_SLOT_OFFSETS = {
        {-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };
    private static final int[][] FOOTPRINT_OFFSETS = {
        {0, 0}, {-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };
    private static final double CONTENT_HEIGHT = 2.25;
    private static final int MAX_EJECT_FALL_DISTANCE = 8;
    private static long ignitionTagVersion;
    private static final ResourceHandler<ItemResource> EMPTY_HANDLER = new EmptyItemHandler();
    private static final InWorldRecipeData<Map<Long, FluidRecipeState>> FLUID_RECIPE_STATES =
        InWorldRecipeData.of(AnvilCraft.of("large_cauldron_fluid_states"), (context, key) -> new HashMap<>());
    private static final Identifier FLUID_RECIPE_ACCEPTOR = AnvilCraft.of("large_cauldron_fluid_acceptor");

    private final LargeCauldronInputHandler input = new LargeCauldronInputHandler(this::ignitionContentsChanged);
    private final ItemStacksResourceHandler output = new ItemStacksResourceHandler(LargeCauldronBlockEntity.OUTPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot, ItemStack previousContents) {
            LargeCauldronBlockEntity.this.contentsChanged();
        }
    };
    private final LargeCauldronFluidHandler fluids = new LargeCauldronFluidHandler(this::fluidsChanged);
    private final ResourceHandler<ItemResource> selectedInput = new SelectedInputHandler();
    private final Set<Integer> processingOutputInputs = new HashSet<>();
    private int processingSlot = -1;
    private boolean processingOutput;
    private int lastImpactEntityId = Integer.MIN_VALUE;
    private long lastImpactGameTime = Long.MIN_VALUE;
    private long recipePreviewGameTime = Long.MIN_VALUE;
    private List<RecipePreview> recipePreviewCache = List.of();
    private boolean droppedContents;
    private boolean ignited;
    private boolean manuallyExtinguished;
    private long checkedIgnitionTagVersion = -1;

    public LargeCauldronBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static int inputSlotForPart(Cube3x3PartHalf part) {
        for (int i = 0; i < LargeCauldronBlockEntity.INPUT_SLOT_OFFSETS.length; i++) {
            if (LargeCauldronBlockEntity.INPUT_SLOT_OFFSETS[i][0] == part.getOffsetX()
                && LargeCauldronBlockEntity.INPUT_SLOT_OFFSETS[i][1] == part.getOffsetZ()) return i;
        }
        return -1;
    }

    public static @Nullable LargeCauldronBlockEntity getMain(
        Level level,
        BlockPos pos,
        BlockState state
    ) {
        if (!(state.getBlock() instanceof LargeCauldronBlock block)) return null;
        BlockEntity entity = level.getBlockEntity(block.getMainPartPos(pos, state));
        return entity instanceof LargeCauldronBlockEntity cauldron ? cauldron : null;
    }

    public LargeCauldronBlockEntity getMainPart() {
        if (!(this.getBlockState().getBlock() instanceof LargeCauldronBlock block) || this.level == null) return this;
        BlockPos mainPos = block.getMainPartPos(this.worldPosition, this.getBlockState());
        BlockEntity entity = this.level.getBlockEntity(mainPos);
        return entity instanceof LargeCauldronBlockEntity cauldron ? cauldron : this;
    }

    public boolean isMainPart() {
        return this.getBlockState().getBlock() instanceof LargeCauldronBlock block
               && block.isMainPart(this.getBlockState());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            FluidNetworkManager.INSTANCE.addContainer(this.level, this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null && !this.level.isClientSide()) {
            FluidNetworkManager.INSTANCE.removeContainer(this.level, this.worldPosition);
        }
        super.setRemoved();
    }

    public static void serverTick(
        Level level,
        BlockPos pos,
        BlockState state,
        LargeCauldronBlockEntity entity
    ) {
        if (!entity.isMainPart()) return;
        entity.absorbFluidSources(level);
        if (entity.checkedIgnitionTagVersion != LargeCauldronBlockEntity.ignitionTagVersion) {
            entity.checkedIgnitionTagVersion = LargeCauldronBlockEntity.ignitionTagVersion;
            entity.refreshIgnited();
        }
        entity.applyFluidEffects((ServerLevel) level);
        entity.hurtEntitiesInsideFromCampfire((ServerLevel) level);
        entity.reforgeItemsInLava(level);
        entity.mendItems((ServerLevel) level);
        NeoForge.EVENT_BUS.post(new LargeCauldronEvent.ServerTick((ServerLevel) level, entity));
    }

    private void absorbFluidSources(Level level) {
        BlockPos.MutableBlockPos sourcePos = new BlockPos.MutableBlockPos();
        for (int[] offset : LargeCauldronBlockEntity.FOOTPRINT_OFFSETS) {
            sourcePos.setWithOffset(this.worldPosition, offset[0], 2, offset[1]);
            FluidState fluidState = level.getFluidState(sourcePos);
            if (fluidState.isEmpty() || !fluidState.isSource()) continue;
            BlockState sourceState = level.getBlockState(sourcePos);
            if (!(sourceState.getBlock() instanceof BucketPickup bucketPickup)) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = this.fluids.insert(
                    FluidResource.of(fluidState.getType()),
                    FluidType.BUCKET_VOLUME,
                    transaction
                );
                if (inserted != FluidType.BUCKET_VOLUME) continue;
                if (bucketPickup.pickupBlock(null, level, sourcePos.immutable(), sourceState).isEmpty()) continue;
                transaction.commit();
            }
        }
    }

    @Override
    public ResourceHandler<ItemResource> getItemHandler() {
        return this.getMainPart().output;
    }

    @Override
    public ResourceHandler<ItemResource> getInput() {
        LargeCauldronBlockEntity main = this.getMainPart();
        return main.processingSlot < 0 ? LargeCauldronBlockEntity.EMPTY_HANDLER : main.selectedInput;
    }

    @Override
    public ResourceHandler<ItemResource> getOutput() {
        LargeCauldronBlockEntity main = this.getMainPart();
        // Keep recipe input and output caches from snapshotting the same inventory during output reprocessing.
        return main.processingOutput ? LargeCauldronBlockEntity.EMPTY_HANDLER : main.output;
    }

    @Override
    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.getMainPart().fluids;
    }

    public LargeCauldronInputHandler getInputHandler() {
        return this.getMainPart().input;
    }

    public ItemStacksResourceHandler getOutputHandler() {
        return this.getMainPart().output;
    }

    public LargeCauldronFluidHandler getFluids() {
        return this.getMainPart().fluids;
    }

    public FluidStack getTopFluid() {
        FluidStack fluid = this.getMainPart().topFluid();
        return fluid.isEmpty() ? FluidStack.EMPTY : fluid.copy();
    }

    public boolean isIgnited() {
        return this.getMainPart().ignited;
    }

    /** 标签重载后，已放置的大锅会在下次服务端更新时使用新标签。 */
    public static void invalidateIgnitionTags() {
        LargeCauldronBlockEntity.ignitionTagVersion++;
    }

    /** 锹熄灭后，原有点火物可以留在锅内，直到下一次投料、换液或主动点燃。 */
    public void extinguish() {
        LargeCauldronBlockEntity main = this.getMainPart();
        main.manuallyExtinguished = true;
        main.setIgnited(false);
    }

    public void setIgnited(boolean ignited) {
        LargeCauldronBlockEntity main = this.getMainPart();
        boolean next = ignited && main.canIgniteTopFluid();
        if (main.ignited == next) {
            return;
        }
        if (next) {
            main.manuallyExtinguished = false;
        }
        main.ignited = next;
        main.contentsChanged();
        if (next && main.level != null && main.hasEightDistinctFluids()) {
            TriggerUtil.largeCauldronCocktail(main.level, main.worldPosition);
        }
    }

    /** 八个储罐全部非空且流体种类互不相同。 */
    private boolean hasEightDistinctFluids() {
        Set<Fluid> fluids = new HashSet<>();
        for (int tank = 0; tank < LargeCauldronFluidHandler.TANK_COUNT; tank++) {
            FluidStack stack = this.fluids.getFluidInTank(tank);
            if (stack.isEmpty()) return false;
            fluids.add(stack.getFluid());
        }
        return fluids.size() == LargeCauldronFluidHandler.TANK_COUNT;
    }

    public ResourceHandler<ItemResource> getAutomationItemHandler(@Nullable Direction side) {
        LargeCauldronBlockEntity main = this.getMainPart();
        Cube3x3PartHalf part = this.getBlockState().getValue(LargeCauldronBlock.HALF);
        boolean outputSide = side == Direction.DOWN
                             || (side != Direction.UP && part.getOffsetY() == 0);
        if (outputSide) return new OutputOnlyHandler(main.output);
        boolean extractPreferred = side != null
                                   && side.getAxis().isHorizontal()
                                   && part.getOffsetY() > 0;
        return new PreferredInputHandler(main.input, LargeCauldronBlockEntity.inputSlotForPart(part), extractPreferred);
    }

    public ResourceHandler<FluidResource> getAutomationFluidHandler(@Nullable Direction side) {
        LargeCauldronFluidHandler handler = this.getMainPart().fluids;
        if (side == Direction.DOWN) return handler.bottomAccess();
        return handler.topAccess();
    }

    public boolean interactWithFluid(Player player, InteractionHand hand, BlockHitResult hit) {
        if (this.level == null) return false;
        LargeCauldronBlockEntity main = this.getMainPart();
        double minY = main.worldPosition.getY() - 0.5;
        double fraction = Math.max(0.0, Math.min(1.0, (hit.getLocation().y - minY) / 2.25));
        int accessible = Math.max(1, (int) Math.ceil(fraction * LargeCauldronFluidHandler.totalCapacity()));
        ResourceHandler<FluidResource> handler = main.fluids.sideAccess(accessible);
        try (Transaction transaction = Transaction.openRoot()) {
            boolean success = FluidUtil.interactWithFluidHandler(
                player,
                hand,
                this.worldPosition,
                handler,
                transaction
            );
            if (success) transaction.commit();
            return success;
        }
    }

    public boolean clearFluids() {
        LargeCauldronBlockEntity main = this.getMainPart();
        if (main.fluids.getTotalAmount() == 0) return false;
        if (main.level != null && !main.level.isClientSide()) {
            main.fluids.setFluids(List.of());
            main.setIgnited(false);
        }
        return true;
    }

    public boolean insertFromHand(ItemStack held, int preferredSlot) {
        ItemStack remainder = LargeCauldronBlockEntity.insertItem(new PreferredInputHandler(this.input, preferredSlot), held.copy());
        int inserted = held.getCount() - remainder.getCount();
        if (inserted <= 0) return false;
        held.shrink(inserted);
        return true;
    }

    public boolean extractItemsToHand(Player player, InteractionHand hand, int inputSlot) {
        LargeCauldronBlockEntity main = this.getMainPart();
        boolean extractOutputs = inputSlot < 0;
        if (extractOutputs ? main.outputIsEmpty() : main.input.getStackInSlot(inputSlot).isEmpty()) return false;
        if (player.level().isClientSide()) return true;

        List<ItemStack> extracted = new ArrayList<>();
        if (extractOutputs) {
            for (int slot = 0; slot < main.output.size(); slot++) {
                ItemStack stack;
                while (!(stack = LargeCauldronBlockEntity.extractItem(main.output, slot, Integer.MAX_VALUE)).isEmpty()) {
                    extracted.add(stack);
                }
            }
        } else {
            ItemStack stack;
            while (!(stack = LargeCauldronBlockEntity.extractItem(main.input, inputSlot, Integer.MAX_VALUE)).isEmpty()) {
                extracted.add(stack);
            }
        }
        if (extracted.isEmpty()) return false;
        ItemStack first = extracted.getFirst();
        player.setItemInHand(hand, first.split(first.getMaxStackSize()));
        for (ItemStack stack : extracted) {
            while (!stack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(stack.split(stack.getMaxStackSize()));
            }
        }
        return true;
    }

    private boolean outputIsEmpty() {
        for (int slot = 0; slot < this.output.size(); slot++) {
            if (this.output.getAmountAsLong(slot) > 0) {
                return false;
            }
        }
        return true;
    }

    public void absorbItem(ItemEntity entity, int preferredSlot) {
        if (!entity.anvilcraft$isAdsorbable() || entity.isRemoved()) return;
        ItemStack stack = entity.getItem();
        ItemStack remainder = LargeCauldronBlockEntity.insertItem(
            new PreferredInputHandler(this.getMainPart().input, preferredSlot),
            stack.copy()
        );
        if (remainder.getCount() == stack.getCount()) return;
        if (remainder.isEmpty()) {
            entity.discard();
        } else {
            entity.setItem(remainder);
        }
    }

    public void ejectOverflow(ItemEntity entity) {
        Level level = this.level;
        if (level == null) return;
        BlockPos origin = this.getMainPart().worldPosition.above(2);
        for (int distance = 2; distance <= 4; distance++) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos pos = origin.relative(direction, distance);
                if (!canEjectOverflowAt(level, pos)) continue;
                entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                entity.setDeltaMovement(0.0, 0.0, 0.0);
                entity.anvilcraft$setIsAdsorbable(true);
                return;
            }
        }
    }

    private static boolean canEjectOverflowAt(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        if (!isEjectPassable(level.getBlockState(pos), level, pos)) return false;
        BlockPos floor = pos.below();
        for (int fall = 0; fall < MAX_EJECT_FALL_DISTANCE && floor.getY() >= level.getMinY(); fall++) {
            BlockState state = level.getBlockState(floor);
            if (isEjectPassable(state, level, floor)) {
                floor = floor.below();
                continue;
            }
            return !state.is(BlockTags.CAULDRONS) && !state.getCollisionShape(level, floor).isEmpty();
        }
        return false;
    }

    private static boolean isEjectPassable(BlockState state, Level level, BlockPos pos) {
        return !state.is(BlockTags.CAULDRONS)
               && state.getFluidState().isEmpty()
               && !state.is(BlockTags.FIRE)
               && state.getCollisionShape(level, pos).isEmpty();
    }

    public ItemStack insertRecipeOutput(ItemStack stack) {
        LargeCauldronBlockEntity main = this.getMainPart();
        if (!main.processingOutput) return LargeCauldronBlockEntity.insertItem(main.output, stack);
        ItemStack remainder = stack;
        for (int slot = 0; slot < main.output.size() && !remainder.isEmpty(); slot++) {
            if (main.processingOutputInputs.contains(slot)) continue;
            remainder = LargeCauldronBlockEntity.insertItem(main.output, slot, remainder);
        }
        return remainder;
    }

    public boolean hasInputMatching(Predicate<ItemStack> predicate) {
        LargeCauldronInputHandler handler = this.getMainPart().input;
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty() && predicate.test(stack)) return true;
        }
        return false;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (state.getBlock() instanceof LargeCauldronBlock block && block.isMainPart(state)) {
            this.dropContents();
        }
    }

    public void dropContents() {
        LargeCauldronBlockEntity main = this.getMainPart();
        if (main.droppedContents || main.level == null) return;
        main.droppedContents = true;
        for (int slot = 0; slot < main.input.size(); slot++) {
            ItemStack stack;
            while (!(stack = LargeCauldronBlockEntity.extractItem(main.input, slot, Integer.MAX_VALUE)).isEmpty()) {
                while (!stack.isEmpty()) {
                    Block.popResource(main.level, main.worldPosition, stack.split(stack.getMaxStackSize()));
                }
            }
        }
        for (int slot = 0; slot < main.output.size(); slot++) {
            ItemStack stack = LargeCauldronBlockEntity.extractItem(main.output, slot, Integer.MAX_VALUE);
            while (!stack.isEmpty()) {
                Block.popResource(main.level, main.worldPosition, stack.split(stack.getMaxStackSize()));
            }
        }
    }

    public int getLightLevel() {
        LargeCauldronBlockEntity main = this.getMainPart();
        if (main.ignited) return 15;
        LargeCauldronFluidHandler handler = main.fluids;
        int maxLight = 0;
        for (int i = 0; i < handler.size(); i++) {
            FluidStack fluid = handler.getFluidInTank(i);
            if (!fluid.isEmpty()) maxLight = Math.max(maxLight, fluid.getFluidType().getLightLevel(fluid));
        }
        float fill = (float) handler.getTotalAmount()
                     / (LargeCauldronFluidHandler.TANK_COUNT * LargeCauldronFluidHandler.tankCapacity());
        return Math.round(maxLight * fill);
    }

    private void ignitionContentsChanged() {
        this.checkedIgnitionTagVersion = -1;
        this.manuallyExtinguished = false;
        this.contentsChanged();
    }

    private void fluidsChanged() {
        this.ignitionContentsChanged();
        if (this.level == null || !this.isMainPart()) return;
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            BlockPos pos = this.worldPosition.offset(part.getOffsetX(), part.getOffsetY() - 1, part.getOffsetZ());
            this.level.updateNeighbourForOutputSignal(pos, this.getBlockState().getBlock());
        }
    }

    private void contentsChanged() {
        if (!this.isMainPart()) return;
        this.recipePreviewGameTime = Long.MIN_VALUE;
        this.setChanged();
        if (this.level == null) return;
        this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.isMainPart()) return;
        this.input.serialize(output.child("Inputs"));
        this.output.serialize(output.child("Outputs"));
        this.fluids.serialize(output.child("Fluids"));
        output.putBoolean("Ignited", this.ignited);
        output.putBoolean("ManuallyExtinguished", this.manuallyExtinguished);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        if (!this.isMainPart()) return;
        this.input.deserialize(input.childOrEmpty("Inputs"));
        this.output.deserialize(input.childOrEmpty("Outputs"));
        this.fluids.deserialize(input.childOrEmpty("Fluids"));
        this.ignited = input.getBooleanOr("Ignited", false) && this.canIgniteTopFluid();
        this.manuallyExtinguished = input.getBooleanOr("ManuallyExtinguished", false);
        this.recipePreviewGameTime = Long.MIN_VALUE;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (this.isMainPart()) {
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
            this.input.serialize(output.child("Inputs"));
            this.output.serialize(output.child("Outputs"));
            this.fluids.serialize(output.child("Fluids"));
            tag.merge(output.buildResult());
            tag.putBoolean("Ignited", this.ignited);
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean handleGiantAnvilImpact(AnvilEvent.OnLand event) {
        LargeCauldronBlockEntity main = this.getMainPart();
        Level level = main.level;
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        BlockState landedAnvilState = NeoForge.EVENT_BUS.post(new LargeCauldronEvent.GiantAnvilImpact(
            level, event, main, level.getBlockState(event.getPos()))).getLandedAnvilState();
        if (!(landedAnvilState.getBlock() instanceof GiantAnvilBlock giantAnvil)
            || !giantAnvil.getMainPartPos(event.getPos(), landedAnvilState).equals(main.worldPosition.above(3))) {
            return true;
        }
        int entityId = event.getEntity().getId();
        long gameTime = level.getGameTime();
        if (main.lastImpactEntityId == entityId && main.lastImpactGameTime == gameTime) {
            return true;
        }
        main.lastImpactEntityId = entityId;
        main.lastImpactGameTime = gameTime;

        BlockPos base = main.worldPosition.below();
        List<BlockPos> helpers = main.findActiveHelpers(base);
        List<RecipePass> itemRecipePasses = helpers.isEmpty()
            ? List.of(RecipePass.ALL)
            : List.of(RecipePass.NON_COMPRESSION, RecipePass.COMPRESSION_ONLY);
        final List<FluidStack> initialFluids = main.fluids.copyFluids();
        List<Integer> initialOutputSlots = new ArrayList<>();
        for (int slot = 0; slot < main.output.size(); slot++) {
            if (main.output.getAmountAsLong(slot) > 0) {
                initialOutputSlots.add(slot);
            }
        }
        int maxProcessEfficiency = AnvilCraft.CONFIG.machines.largeCauldronMaxProcessEfficiency;
        int processed = main.processInputs(serverLevel, base, helpers, event, itemRecipePasses, maxProcessEfficiency);
        List<BlockPos> centerCandidates = helpers.isEmpty()
            ? List.of(base.below())
            : LargeCauldronBlockEntity.orderedHelpers(base, helpers, base);
        for (RecipePass recipePass : itemRecipePasses) {
            for (int slot : initialOutputSlots) {
                if (processed >= maxProcessEfficiency) {
                    break;
                }
                if (main.output.getAmountAsLong(slot) == 0) {
                    continue;
                }
                RecipeExecution execution = main.tryProcessItemGroup(
                    serverLevel,
                    base,
                    centerCandidates,
                    event.getEntity(),
                    slot,
                    true,
                    recipePass
                );
                if (!execution.executed()) {
                    continue;
                }
                if (execution.damageAnvil()) {
                    event.setAnvilDamage(true);
                }
                processed++;
            }
            if (processed >= maxProcessEfficiency) {
                break;
            }
        }

        if (main.fluids.hasSameFluids(initialFluids)) {
            while (processed < maxProcessEfficiency) {
                if (main.tryProcessFluidMixingRecipe(serverLevel)) {
                    processed++;
                    continue;
                }
                RecipeExecution execution = main.tryProcessFluidRecipe(
                    serverLevel,
                    base,
                    centerCandidates,
                    event.getEntity(),
                    RecipePass.NON_COMPRESSION
                );
                if (!execution.executed()) {
                    break;
                }
                if (execution.damageAnvil()) {
                    event.setAnvilDamage(true);
                }
                processed++;
            }
        }
        return true;
    }

    private int processInputs(
        ServerLevel level,
        BlockPos base,
        List<BlockPos> helpers,
        AnvilEvent.OnLand event,
        List<RecipePass> itemRecipePasses,
        int maxProcessEfficiency
    ) {
        if (this.input.isEmpty()) {
            return 0;
        }
        int processed = 0;
        boolean[] specialRecipeSlots = new boolean[this.input.size()];
        for (int slot = 0; slot < this.input.size() && processed < maxProcessEfficiency; slot++) {
            if (!this.tryProcessLiquidEnchantmentRecipe(level, base, slot)) {
                continue;
            }
            specialRecipeSlots[slot] = true;
            processed++;
        }
        for (RecipePass recipePass : itemRecipePasses) {
            boolean madeProgress;
            do {
                madeProgress = false;
                // 每组加工都可能消耗其他槽位的原料，需要重新按配方优先级排序。
                for (int slot : LargeCauldronBlockEntity.orderedInputSlots(level, this.input, recipePass)) {
                    if (processed >= maxProcessEfficiency) {
                        break;
                    }
                    if (specialRecipeSlots[slot]) {
                        continue;
                    }
                    BlockPos slotPos = LargeCauldronBlockEntity.positionForInputSlot(base, slot);
                    List<BlockPos> candidates = helpers.isEmpty()
                        ? List.of(slotPos.below())
                        : LargeCauldronBlockEntity.orderedHelpers(slotPos, helpers, base);
                    RecipeExecution execution = this.tryProcessItemGroup(
                        level,
                        base,
                        candidates,
                        event.getEntity(),
                        slot,
                        false,
                        recipePass
                    );
                    if (!execution.executed()) {
                        continue;
                    }
                    if (execution.damageAnvil()) {
                        event.setAnvilDamage(true);
                    }
                    processed++;
                    madeProgress = true;
                    break;
                }
            } while (madeProgress && processed < maxProcessEfficiency);
            if (processed >= maxProcessEfficiency) {
                break;
            }
        }

        return processed;
    }

    private boolean tryProcessLiquidEnchantmentRecipe(ServerLevel level, BlockPos base, int slot) {
        if (this.fluids.getLayerCount() == 0 || this.input.getAmountAsLong(slot) == 0) {
            return false;
        }
        ItemStack starting = this.input.getStackInSlot(slot);
        int itemBudget = starting.getMaxStackSize();
        int consumed = 0;
        boolean processed = false;
        while (consumed < itemBudget) {
            ItemStack item = LargeCauldronBlockEntity.getStack(this.input, slot);
            if (item.isEmpty()) {
                break;
            }
            Optional<BlockPos> heatingHelper = item.is(Items.LAPIS_LAZULI) ? this.findActiveHeatingHelper(base, slot) : Optional.empty();
            var matched = LiquidEnchantmentCauldronRecipe.match(
                this.fluids.copyFluids(),
                item,
                heatingHelper.isPresent()
            );
            if (matched.isEmpty()) {
                break;
            }
            LiquidEnchantmentCauldronRecipe.Result result = matched.get();
            if (!result.itemResult().isEmpty() && !LargeCauldronBlockEntity.canInsertItem(this.output, result.itemResult())) {
                break;
            }

            this.fluids.setFluids(result.fluids());
            LargeCauldronBlockEntity.extractItem(this.input, slot, result.itemCost());
            if (!result.itemResult().isEmpty()) {
                LargeCauldronBlockEntity.insertItem(this.output, result.itemResult());
            }
            if (result.consumesHeat()) {
                LargeCauldronBlockEntity.consumeHeatingFuel(level, heatingHelper.orElseThrow());
            }
            consumed += result.itemCost();
            processed = true;
        }
        return processed;
    }

    private Optional<BlockPos> findActiveHeatingHelper(BlockPos base, int inputSlot) {
        Level level = this.level;
        if (level == null) return Optional.empty();
        List<BlockPos> heaters = new ArrayList<>();
        for (int slot = 0; slot < LargeCauldronBlockEntity.FOOTPRINT_OFFSETS.length; slot++) {
            BlockPos helper = LargeCauldronBlockEntity.positionForFootprint(base, slot).below();
            if (LargeCauldronBlockEntity.isActiveHeatingHelper(level.getBlockState(helper))) heaters.add(helper);
        }
        BlockPos inputPos = LargeCauldronBlockEntity.positionForInputSlot(base, inputSlot);
        return LargeCauldronBlockEntity.orderedHelpers(inputPos, heaters, base).stream().findFirst();
    }

    private static boolean isActiveHeatingHelper(BlockState state) {
        return CauldronProcessing.matches(CauldronProcessing.SUPER_HEATING, state);
    }

    private static void consumeHeatingFuel(ServerLevel level, BlockPos helper) {
        if (level.getBlockEntity(helper) instanceof BurningHeaterBlockEntity heater) {
            heater.consumeBurnTime(240 * 20);
        }
    }

    private boolean tryProcessFluidMixingRecipe(ServerLevel level) {
        List<FluidStack> storedFluids = this.fluids.copyFluids();
        for (RecipeHolder<FluidMixingRecipe> holder
            : RecipesRecord.getRecipes(level).byType(ModRecipeTypes.FLUID_MIXING.get())) {
            FluidMixingRecipe recipe = holder.value();
            int maximumBatches = recipe.getMaximumBatches(storedFluids);
            if (maximumBatches <= 0) continue;
            List<FluidStack> mixedFluids = recipe.consumesMaximum()
                ? LargeCauldronBlockEntity.findLargestFluidMixingResult(recipe, storedFluids, maximumBatches)
                : LargeCauldronBlockEntity.simulateFluidMixing(recipe, storedFluids, 1);
            if (mixedFluids == null) continue;

            if (!this.mixingOutputsFit(recipe.getItemResults())) {
                continue;
            }

            this.fluids.setFluids(mixedFluids);
            for (ItemStack result : recipe.getItemResults()) {
                ItemStack output = NeoForge.EVENT_BUS.post(new LargeCauldronEvent.MixingOutput(this, result.copy())).getResult();
                LargeCauldronBlockEntity.insertItem(this.output, output);
            }
            return true;
        }
        return false;
    }

    private boolean mixingOutputsFit(List<ItemStack> results) {
        if (results.isEmpty()) {
            return true;
        }
        var simulated = new ItemStacksResourceHandler(this.output.size());
        for (int slot = 0; slot < this.output.size(); slot++) {
            int count = this.output.getAmountAsInt(slot);
            if (count > 0) {
                simulated.set(slot, this.output.getResource(slot), count);
            }
        }
        for (ItemStack result : results) {
            ItemStack output = NeoForge.EVENT_BUS.post(new LargeCauldronEvent.MixingOutput(this, result.copy())).getResult();
            if (!insertItem(simulated, output).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static @Nullable List<FluidStack> findLargestFluidMixingResult(
        FluidMixingRecipe recipe,
        List<FluidStack> storedFluids,
        int maximumBatches
    ) {
        int low = 1;
        int high = maximumBatches;
        List<FluidStack> best = null;
        while (low <= high) {
            int middle = low + (high - low) / 2;
            List<FluidStack> candidate = LargeCauldronBlockEntity.simulateFluidMixing(recipe, storedFluids, middle);
            if (candidate == null) {
                high = middle - 1;
            } else {
                best = candidate;
                low = middle + 1;
            }
        }
        return best;
    }

    private static @Nullable List<FluidStack> simulateFluidMixing(
        FluidMixingRecipe recipe,
        List<FluidStack> storedFluids,
        int batches
    ) {
        var remainingFluids = recipe.consume(storedFluids, batches);
        var fluidResults = recipe.getFluidResults(batches);
        if (remainingFluids.isEmpty() || fluidResults.isEmpty()) return null;

        LargeCauldronFluidHandler simulatedFluids = new LargeCauldronFluidHandler(() -> {});
        simulatedFluids.setFluids(remainingFluids.get());
        for (FluidStack result : fluidResults.get()) {
            int filled = LargeCauldronBlockEntity.insertFluid(simulatedFluids, result);
            if (filled != result.getAmount()) return null;
        }
        return simulatedFluids.copyFluids();
    }

    private RecipeExecution tryProcessItemGroup(
        ServerLevel level,
        BlockPos base,
        List<BlockPos> candidates,
        Entity anvil,
        int slot,
        boolean outputSource,
        RecipePass recipePass
    ) {
        for (BlockPos helper : candidates) {
            final Vec3 contextPos = new Vec3(helper.getX() + 0.5, base.getY() + 1.0, helper.getZ() + 0.5);
            this.processingSlot = slot;
            this.processingOutput = outputSource;
            this.processingOutputInputs.clear();
            if (outputSource) {
                for (int outputSlot = 0; outputSlot < this.output.size(); outputSlot++) {
                    if (this.output.getAmountAsLong(outputSlot) > 0) {
                        this.processingOutputInputs.add(outputSlot);
                    }
                }
            }
            RecipeExecution execution;
            try {
                execution = this.triggerRecipeGroup(
                    level,
                    contextPos,
                    anvil,
                    slot,
                    outputSource,
                    recipePass
                );
            } finally {
                this.processingSlot = -1;
                this.processingOutput = false;
                this.processingOutputInputs.clear();
            }
            if (execution.executed()) {
                return execution;
            }
        }
        return RecipeExecution.EMPTY;
    }

    private RecipeExecution tryProcessFluidRecipe(
        ServerLevel level,
        BlockPos base,
        List<BlockPos> candidates,
        Entity anvil,
        RecipePass recipePass
    ) {
        var recipes = LargeCauldronRecipes.get(level).candidates(ItemStack.EMPTY);
        if (recipes.isEmpty()) {
            return RecipeExecution.EMPTY;
        }
        for (BlockPos helper : candidates) {
            Vec3 contextPos = new Vec3(helper.getX() + 0.5, base.getY() + 1.0, helper.getZ() + 0.5);
            RecipeExecution execution = triggerOneRecipe(level, contextPos, anvil, recipes, ItemStack.EMPTY, recipePass);
            if (execution.executed()) {
                return execution;
            }
        }
        return RecipeExecution.EMPTY;
    }

    private RecipeExecution triggerRecipeGroup(
        ServerLevel level,
        Vec3 contextPos,
        Entity anvil,
        int slot,
        boolean outputSource,
        RecipePass recipePass
    ) {
        ResourceHandler<ItemResource> source = outputSource ? this.output : this.input;
        ItemStack starting = LargeCauldronBlockEntity.getStack(source, slot);
        if (starting.isEmpty()) {
            return RecipeExecution.EMPTY;
        }
        int itemBudget = starting.getMaxStackSize();
        int consumed = 0;
        boolean executed = false;
        boolean damageAnvil = false;
        while (consumed < itemBudget) {
            ItemStack processingInput = LargeCauldronBlockEntity.getStack(source, slot);
            if (processingInput.isEmpty()) {
                break;
            }
            RecipeExecution current = LargeCauldronBlockEntity.triggerOneRecipe(
                level,
                contextPos,
                anvil,
                LargeCauldronRecipes.get(level).candidates(processingInput),
                processingInput,
                recipePass
            );
            if (!current.executed()) {
                break;
            }
            executed = true;
            damageAnvil |= current.damageAnvil();
            int consumedNow = processingInput.getCount() - source.getAmountAsInt(slot);
            if (consumedNow <= 0) {
                break;
            }
            consumed += consumedNow;
        }
        return new RecipeExecution(executed, damageAnvil);
    }

    private static RecipeExecution triggerOneRecipe(
        ServerLevel level,
        Vec3 contextPos,
        Entity anvil,
        List<LargeCauldronRecipes.Candidate> candidates,
        ItemStack processingInput,
        RecipePass recipePass
    ) {
        InWorldRecipeContext context = null;
        for (var candidate : candidates) {
            var holder = candidate.holder();
            InWorldRecipe recipe = holder.value();
            if (!recipePass.accepts(recipe) || !candidate.matchesInput(processingInput)) {
                continue;
            }
            if (context == null) {
                context = new InWorldRecipeContext(level, contextPos, anvil);
            }
            if (!recipe.matches(context, level)) {
                continue;
            }
            recipe.assemble(context);
            NeoForge.EVENT_BUS.post(
                new InWorldRecipeEvent(recipe.getType(), holder.id().identifier(), recipe, context)
            );
            boolean damageAnvil = context.get(DamageAnvil.DAMAGE_ANVIL);
            GiantAnvilBlock.SUPPRESS_DROPS.set(true);
            try {
                context.accept();
            } finally {
                GiantAnvilBlock.SUPPRESS_DROPS.set(false);
            }
            return new RecipeExecution(true, damageAnvil);
        }
        return RecipeExecution.EMPTY;
    }

    /// 按可匹配配方的最高优先级排列非空输入槽，避免槽位顺序盖过配方优先级
    private static List<Integer> orderedInputSlots(
        ServerLevel level,
        ResourceHandler<ItemResource> input,
        RecipePass recipePass
    ) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < input.size(); slot++) {
            if (input.getAmountAsLong(slot) > 0) {
                slots.add(slot);
            }
        }
        if (slots.size() > 1) {
            var recipes = LargeCauldronRecipes.get(level);
            int[] priorities = new int[input.size()];
            for (int slot : slots) {
                ItemStack stack = getStack(input, slot);
                int priority = Integer.MIN_VALUE;
                for (var candidate : recipes.candidates(stack)) {
                    var recipe = candidate.holder().value();
                    if (recipePass.accepts(recipe) && candidate.matchesInput(stack)) {
                        priority = Math.max(priority, recipe.priority());
                    }
                }
                priorities[slot] = priority;
            }
            // 槽位按编号加入，稳定排序让同优先级配方继续按槽位编号加工。
            slots.sort(Comparator.comparingInt((Integer slot) -> priorities[slot]).reversed());
        }
        return slots;
    }

    public List<RecipePreview> getRecipePreviews() {
        LargeCauldronBlockEntity main = this.getMainPart();
        if (main != this) return main.getRecipePreviews();
        if (this.level == null) return List.of();
        long gameTime = this.level.getGameTime();
        if (this.recipePreviewGameTime != Long.MIN_VALUE
            && gameTime >= this.recipePreviewGameTime
            && gameTime - this.recipePreviewGameTime < 10) {
            return this.recipePreviewCache;
        }

        RecipeMap currentRecipes = RecipesRecord.getRecipes(this.level);
        if (currentRecipes == null) {
            return List.of();
        }
        BlockPos base = this.worldPosition.below();
        List<BlockPos> helpers = this.findActiveHelpers(base);
        var recipes = LargeCauldronRecipes.Previews.get(currentRecipes, !helpers.isEmpty());
        List<FluidStack> fluidStacks = this.fluids.copyFluids();
        List<ItemStack> inputStacks = copyItemStacks(this.input);
        List<ItemStack> outputStacks = copyItemStacks(this.output);
        List<RecipePreview> previews = new ArrayList<>();
        List<BlockPos> centerCandidates = helpers.isEmpty()
            ? List.of(base.below())
            : orderedHelpers(base, helpers, base);
        int groups = AnvilCraft.CONFIG.machines.largeCauldronMaxProcessEfficiency;
        List<RecipePass> passes = helpers.isEmpty()
            ? List.of(RecipePass.ALL) : List.of(RecipePass.NON_COMPRESSION, RecipePass.COMPRESSION_ONLY);
        for (boolean fromOutput : List.of(false, true)) {
            List<ItemStack> source = fromOutput ? outputStacks : inputStacks;
            for (RecipePass pass : passes) {
                List<Integer> slots = new ArrayList<>();
                for (int slot = 0; slot < source.size(); slot++) {
                    if (!source.get(slot).isEmpty()) {
                        slots.add(slot);
                    }
                }
                while (groups > 0 && !slots.isEmpty()) {
                    int slot = fromOutput ? slots.removeFirst() : nextPreviewSlot(slots, source, recipes, pass);
                    var candidates = recipes.forItem(source.get(slot));
                    List<BlockPos> helpersForSlot = centerCandidates;
                    if (!fromOutput) {
                        BlockPos pos = positionForInputSlot(base, slot);
                        helpersForSlot = helpers.isEmpty() ? List.of(pos.below()) : orderedHelpers(pos, helpers, base);
                    }
                    for (BlockPos helper : helpersForSlot) {
                        int before = source.get(slot).getCount();
                        if (this.previewGroup(previews, candidates, source, fluidStacks, slot, fromOutput, helper, base, pass, groups)) {
                            groups--;
                            if (!fromOutput && !source.get(slot).isEmpty() && before > source.get(slot).getCount()) {
                                slots.add(slot);
                            }
                            break;
                        }
                    }
                }
            }
        }
        if (groups > 0 && this.fluids.hasSameFluids(fluidStacks)) {
            for (BlockPos helper : centerCandidates) {
                if (this.previewGroup(previews, recipes.fluids(), List.of(), fluidStacks, -1, false, helper, base,
                    RecipePass.NON_COMPRESSION, groups)) {
                    break;
                }
            }
        }
        this.recipePreviewGameTime = gameTime;
        this.recipePreviewCache = List.copyOf(previews);
        return this.recipePreviewCache;
    }

    private static int nextPreviewSlot(
        List<Integer> slots, List<ItemStack> source, LargeCauldronRecipes.Previews recipes, RecipePass pass
    ) {
        int selected = 0;
        int best = Integer.MIN_VALUE;
        for (int i = 0; i < slots.size(); i++) {
            int slot = slots.get(i);
            var candidates = recipes.forItem(source.get(slot));
            int priority = candidates.stream().filter(holder -> pass.accepts(holder.value()))
                .mapToInt(holder -> holder.value().priority()).max().orElse(Integer.MIN_VALUE);
            if (priority > best || priority == best && slot < slots.get(selected)) {
                selected = i;
                best = priority;
            }
        }
        return slots.remove(selected);
    }

    private boolean previewGroup(
        List<RecipePreview> previews,
        List<RecipeHolder<InWorldRecipe>> recipes,
        List<ItemStack> source,
        List<FluidStack> fluids,
        int slot,
        boolean outputSource,
        BlockPos helper,
        BlockPos base,
        RecipePass pass,
        int groups
    ) {
        Level level = this.level;
        if (level == null || recipes.isEmpty()) {
            return false;
        }
        boolean skipCompression = slot >= 0 && !outputSource
            && RecipeOutputTracker.requiresRecipeOutputForCompression(level.getBlockState(helper));
        Vec3 contextPos = new Vec3(helper.getX() + 0.5, base.getY() + 1, helper.getZ() + 0.5);
        Map<RecipeHolder<InWorldRecipe>, Consumption> totals = new LinkedHashMap<>();
        int budget = slot >= 0 ? source.get(slot).getMaxStackSize() : groups;
        while (budget > 0) {
            int used = 0;
            for (var holder : recipes) {
                InWorldRecipe recipe = holder.value();
                if (!pass.accepts(recipe) || skipCompression && recipe instanceof ItemCompressRecipe
                    || slot >= 0 && !LargeCauldronRecipes.matchesAnchor(recipe, source.get(slot))) {
                    continue;
                }
                var match = CauldronPreviewMatcher.match(recipe, new PreviewState(source, slot, fluids, contextPos));
                if (match == null || (slot >= 0 && !match.usedPrimarySlot)) {
                    continue;
                }
                if (!match.consumed.isEmpty() || !match.fluidAmounts.isEmpty()) {
                    var total = totals.computeIfAbsent(holder, ignored -> new Consumption());
                    match.consumed.forEach((index, count) -> {
                        ItemStack before = source.get(index);
                        source.set(index, before.copyWithCount(before.getCount() - count));
                        total.items.merge(index, count, Integer::sum);
                    });
                    match.fluidAmounts.forEach((fluid, amount) -> total.fluids.merge(fluid, amount, Integer::sum));
                }
                for (int layer = 0; layer < fluids.size(); layer++) {
                    fluids.set(layer, match.fluids.get(layer));
                }
                used = slot < 0 ? 1 : match.consumed.getOrDefault(slot, 0);
                break;
            }
            if (used == 0) {
                break;
            }
            budget -= used;
        }
        totals.forEach((holder, total) -> previews.add(RecipePreview.of(holder, outputSource, total.items, total.fluids)));
        return !totals.isEmpty();
    }

    private static class Consumption {
        private final Map<Integer, Integer> items = new HashMap<>();
        private final Map<FluidResource, Integer> fluids = new HashMap<>();
    }

    private static List<ItemStack> copyItemStacks(ResourceHandler<ItemResource> source) {
        List<ItemStack> result = new ArrayList<>(source.size());
        for (int slot = 0; slot < source.size(); slot++) {
            result.add(LargeCauldronBlockEntity.getStack(source, slot));
        }
        return result;
    }

    private boolean applyPreviewPredicate(
        IRecipePredicate<?> predicate,
        PreviewState state,
        Vec3 contextPos
    ) {
        Level level = this.level;
        if (level == null) return false;
        switch (predicate) {
            case HasItemIngredient itemIngredient -> {
                return state.reserve(itemIngredient);
            }
            case HasCauldron cauldron -> {
                if ((cauldron.ignited() && !this.ignited) || !this.targetsThisCauldron(cauldron, contextPos)) {
                    return false;
                }
                int layer = findSourceTank(state.fluids, cauldron);
                FluidStack source = layer < 0 ? FluidStack.EMPTY : state.fluids.get(layer);
                if (!applyFluidPredicate(state.fluids, cauldron, cauldron.chance() <= 0)) {
                    return false;
                }
                if (!source.isEmpty() && cauldron.consume() > 0 && cauldron.chance() > 0) {
                    state.fluidAmounts.merge(FluidResource.of(source), cauldron.consume(), Integer::sum);
                }
                return true;
            }
            default -> {
                return CauldronPreviewMatcher.matchesWorld(predicate, level, contextPos, ModBlocks.GIANT_ANVIL.getDefaultState());
            }
        }
    }

    private boolean targetsThisCauldron(HasCauldron predicate, Vec3 contextPos) {
        Level level = this.level;
        if (level == null) return false;
        BlockPos pos = BlockPos.containing(contextPos.add(predicate.offset()));
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof LargeCauldronBlock block
               && block.getMainPartPos(pos, state).equals(this.worldPosition);
    }

    private List<BlockPos> findActiveHelpers(BlockPos base) {
        Level level = this.level;
        if (level == null) return List.of();
        List<BlockPos> result = new ArrayList<>();
        for (int slot = 0; slot < LargeCauldronBlockEntity.FOOTPRINT_OFFSETS.length; slot++) {
            BlockPos helper = LargeCauldronBlockEntity.positionForFootprint(base, slot).below();
            if (LargeCauldronBlockEntity.isActiveRecipeHelper(level.getBlockState(helper))) result.add(helper);
        }
        return result;
    }

    private static boolean isActiveRecipeHelper(BlockState state) {
        return CauldronProcessing.isActive(state);
    }

    private static List<BlockPos> orderedHelpers(BlockPos slotPos, List<BlockPos> helpers, BlockPos base) {
        return helpers.stream().sorted(Comparator
            .comparingInt((BlockPos pos) -> LargeCauldronBlockEntity.horizontalDistanceSquared(pos, slotPos))
            .thenComparingInt(pos -> LargeCauldronBlockEntity.horizontalDistanceSquared(pos, base))
            .thenComparingInt(pos -> LargeCauldronBlockEntity.slotForPosition(base, pos.above())))
            .toList();
    }

    private static int horizontalDistanceSquared(BlockPos first, BlockPos second) {
        int dx = first.getX() - second.getX();
        int dz = first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    private static int slotForPosition(BlockPos base, BlockPos pos) {
        int dx = pos.getX() - base.getX();
        int dz = pos.getZ() - base.getZ();
        for (int slot = 0; slot < LargeCauldronBlockEntity.FOOTPRINT_OFFSETS.length; slot++) {
            if (LargeCauldronBlockEntity.FOOTPRINT_OFFSETS[slot][0] == dx
                && LargeCauldronBlockEntity.FOOTPRINT_OFFSETS[slot][1] == dz) {
                return slot;
            }
        }
        return LargeCauldronBlockEntity.FOOTPRINT_OFFSETS.length;
    }

    private static BlockPos positionForInputSlot(BlockPos base, int slot) {
        return base.offset(LargeCauldronBlockEntity.INPUT_SLOT_OFFSETS[slot][0], 0, LargeCauldronBlockEntity.INPUT_SLOT_OFFSETS[slot][1]);
    }

    private static BlockPos positionForFootprint(BlockPos base, int slot) {
        return base.offset(LargeCauldronBlockEntity.FOOTPRINT_OFFSETS[slot][0], 0, LargeCauldronBlockEntity.FOOTPRINT_OFFSETS[slot][1]);
    }

    private void applyFluidEffects(ServerLevel level) {
        int totalAmount = this.fluids.getTotalAmount();
        if (totalAmount <= 0) return;

        AABB contentArea = this.contentArea();
        double fluidTop = contentArea.minY
                          + LargeCauldronBlockEntity.CONTENT_HEIGHT * totalAmount / LargeCauldronFluidHandler.totalCapacity();
        AABB fluidArea = new AABB(
            contentArea.minX,
            contentArea.minY,
            contentArea.minZ,
            contentArea.maxX,
            fluidTop,
            contentArea.maxZ
        );
        for (Entity entity : level.getEntitiesOfClass(Entity.class, fluidArea)) {
            if (this.ignited) {
                if (!entity.fireImmune()) {
                    entity.setRemainingFireTicks(entity.getRemainingFireTicks() + 1);
                    if (entity.getRemainingFireTicks() == 0) entity.igniteForSeconds(8.0F);
                }
                EntityUtil.hurt(entity, level.damageSources().inFire(),
                    NeoForge.EVENT_BUS.post(new LargeCauldronEvent.FluidDamage(this, this.getTopFluid(), 4.0F)).getDamage());
                continue;
            }
            boolean touchesLava = false;
            FluidStack extinguishingFluid = FluidStack.EMPTY;
            FluidStack experienceFluid = FluidStack.EMPTY;
            double layerMinY = contentArea.minY;
            for (FluidStack fluid : this.fluids.copyFluids()) {
                if (fluid.isEmpty()) continue;
                double layerMaxY = layerMinY
                    + LargeCauldronBlockEntity.CONTENT_HEIGHT * fluid.getAmount() / LargeCauldronFluidHandler.totalCapacity();
                if (entity.getBoundingBox().intersects(
                    contentArea.minX, layerMinY, contentArea.minZ, contentArea.maxX, layerMaxY, contentArea.maxZ
                )) {
                    touchesLava |= fluid.is(Fluids.LAVA);
                    if (fluid.is(ModFluids.EXP_FLUID)) {
                        experienceFluid = fluid;
                    }
                    if (extinguishingFluid.isEmpty() && entity.canFluidExtinguish(fluid.getFluidType())) {
                        extinguishingFluid = fluid;
                    }
                }
                layerMinY = layerMaxY;
            }

            if (!experienceFluid.isEmpty() && entity instanceof ItemEntity item) {
                ItemStack stack = item.getItem().copy();
                if (this.mendItem(stack, level)) {
                    item.setItem(stack);
                }
            }
            if (touchesLava) entity.lavaHurt();
            if (!extinguishingFluid.isEmpty() && entity.isOnFire()) {
                entity.clearFire();
                boolean creativePlayer = entity instanceof Player player && player.isCreative();
                if (!creativePlayer && entity.mayInteract(level, this.worldPosition)) {
                    LargeCauldronBlockEntity.extractFluid(this.fluids, extinguishingFluid, 250);
                }
            } else if (entity instanceof Player player && !experienceFluid.isEmpty()) {
                int buckets = Math.min(9, experienceFluid.getAmount() / FluidType.BUCKET_VOLUME);
                if (buckets > 0) {
                    int amount = buckets * FluidType.BUCKET_VOLUME;
                    int extracted = LargeCauldronBlockEntity.extractFluid(this.fluids, experienceFluid, amount);
                    player.giveExperiencePoints(extracted / FluidType.BUCKET_VOLUME * AnvilCraft.CONFIG.world.expFluidXpPerBlock);
                }
            }
        }
    }

    private void mendItems(ServerLevel level) {
        if (this.fluids.getLayerCount() == 0) {
            return;
        }
        for (int slot = 0; slot < this.input.size(); slot++) {
            ItemStack stack = this.input.getStackInSlot(slot);
            if (!stack.isDamaged()) {
                continue;
            }
            FluidStack fluid = this.experienceFluid();
            if (fluid.isEmpty()) {
                return;
            }
            int[] requiredFluid = {0};
            if (!ExperienceMending.repair(stack, level, fluid.getAmount(), amount -> {
                requiredFluid[0] = amount;
                return amount;
            })) {
                continue;
            }
            try (Transaction transaction = Transaction.openRoot()) {
                ItemResource original = this.input.getResource(slot);
                int count = stack.getCount();
                if (this.input.extract(slot, original, count, transaction) != count
                    || this.input.insert(ItemResource.of(stack), count, transaction) != count
                    || this.fluids.extract(FluidResource.of(fluid), requiredFluid[0], transaction) != requiredFluid[0]) {
                    continue;
                }
                transaction.commit();
            }
        }
    }

    private boolean mendItem(ItemStack stack, ServerLevel level) {
        FluidStack fluid = this.experienceFluid();
        if (fluid.isEmpty()) {
            return false;
        }
        return ExperienceMending.repair(stack, level, fluid.getAmount(),
            amount -> LargeCauldronBlockEntity.extractFluid(this.fluids, fluid, amount));
    }

    private FluidStack experienceFluid() {
        for (int tank = 0; tank < this.fluids.getLayerCount(); tank++) {
            FluidStack fluid = this.fluids.getFluidInTank(tank);
            if (fluid.is(ModFluids.EXP_FLUID)) {
                return fluid;
            }
        }
        return FluidStack.EMPTY;
    }

    private void reforgeItemsInLava(Level level) {
        boolean hasLava = false;
        for (int tank = 0; tank < this.fluids.getLayerCount(); tank++) {
            if (this.fluids.getResource(tank).is(Fluids.LAVA)) {
                hasLava = true;
                break;
            }
        }
        if (!hasLava) return;

        for (int slot = 0; slot < this.input.size(); slot++) {
            this.input.mutateStackInSlot(
                slot,
                stack -> FireReforgingUtil.repair(
                    stack,
                    AnvilCraft.CONFIG.equipment.fireReforgingRepairPerTick,
                    level,
                    this.worldPosition
                )
            );
        }
    }

    private FluidStack topFluid() {
        int count = this.fluids.getLayerCount();
        return count == 0 ? FluidStack.EMPTY : this.fluids.getFluidInTank(count - 1);
    }

    private boolean canIgniteTopFluid() {
        return this.topFluid().is(ModFluidTags.IGNITABLE);
    }

    private void refreshIgnited() {
        if (this.ignited) {
            if (this.canIgniteTopFluid()) {
                return;
            }
            this.setIgnited(false);
        }
        if (this.manuallyExtinguished) {
            return;
        }
        for (int slot = 0; slot < this.input.size(); slot++) {
            ItemResource resource = this.input.getResource(slot);
            boolean consumed = resource.is(ModItemTags.FIRE_STARTER);
            if (consumed || resource.is(ModItemTags.UNBROKEN_FIRE_STARTER)) {
                if (this.tryIgnite(false) && consumed) {
                    LargeCauldronBlockEntity.extractItem(this.input, slot, 1);
                }
                return;
            }
        }
    }

    /** 客户端只检查可点燃的流体层，服务端点燃时才调整层序。 */
    public boolean tryIgnite(boolean simulate) {
        LargeCauldronBlockEntity main = this.getMainPart();
        if (main.ignited) {
            return false;
        }
        int topLayer = main.fluids.getLayerCount() - 1;
        for (int layer = topLayer; layer >= 0; layer--) {
            FluidResource fluid = main.fluids.getResource(layer);
            if (!fluid.getFluid().is(ModFluidTags.IGNITABLE)) {
                continue;
            }
            if (!simulate) {
                if (layer < topLayer) {
                    List<FluidStack> layers = main.fluids.copyFluids();
                    layers.add(layers.remove(layer));
                    main.fluids.setFluids(layers);
                }
                main.setIgnited(true);
            }
            return true;
        }
        return false;
    }

    private AABB contentArea() {
        return CauldronUtil.getInnerArea(this.worldPosition, this.getBlockState());
    }

    private void hurtEntitiesInsideFromCampfire(ServerLevel level) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean normalCampfire = false;
        boolean soulCampfire = false;
        for (int[] offset : LargeCauldronBlockEntity.FOOTPRINT_OFFSETS) {
            BlockState state = level.getBlockState(pos.setWithOffset(this.worldPosition, offset[0], -2, offset[1]));
            if (CampfireBlock.isLitCampfire(state)) {
                if (state.is(Blocks.SOUL_CAMPFIRE)) {
                    soulCampfire = true;
                    break;
                }
                normalCampfire |= state.is(Blocks.CAMPFIRE);
            }
        }
        if (!normalCampfire && !soulCampfire) return;

        AABB inside = this.contentArea();
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, inside)) {
            if (living.fireImmune() || living.isSteppingCarefully()) continue;
            EntityUtil.hurt(living, level.damageSources().inFire(), soulCampfire ? 2.0F : 1.0F);
        }
    }

    public boolean testFluidRecipe(InWorldRecipeContext context, HasCauldron predicate) {
        LargeCauldronBlockEntity main = this.getMainPart();
        if (predicate.ignited() && !main.ignited) return false;
        FluidRecipeState state = LargeCauldronBlockEntity.fluidState(context, main);
        return applyFluidPredicate(state.fluids, predicate, true);
    }

    public void snapshotFluidRecipe(InWorldRecipeContext context, HasCauldron predicate) {
        LargeCauldronBlockEntity main = this.getMainPart();
        FluidRecipeState state = LargeCauldronBlockEntity.fluidState(context, main);
        state.rollback.push(new ArrayList<>(state.fluids));
        if (context.getLevel().getRandom().nextFloat() <= predicate.chance()) {
            LargeCauldronBlockEntity.applyFluidPredicate(state.fluids, predicate, false);
        }
    }

    public void rollbackFluidRecipe(InWorldRecipeContext context) {
        FluidRecipeState state = LargeCauldronBlockEntity.fluidState(context, this.getMainPart());
        if (!state.rollback.isEmpty()) state.fluids = state.rollback.pop();
    }

    public void clearFluidRecipeStack(InWorldRecipeContext context) {
        LargeCauldronBlockEntity.fluidState(context, this.getMainPart()).rollback.clear();
    }

    public void acceptFluidRecipe(InWorldRecipeContext context) {
        context.putAcceptor(LargeCauldronBlockEntity.FLUID_RECIPE_ACCEPTOR, LargeCauldronBlockEntity::commitFluidRecipes);
    }

    private static FluidRecipeState fluidState(InWorldRecipeContext context, LargeCauldronBlockEntity cauldron) {
        Map<Long, FluidRecipeState> states = context.computeIfAbsent(LargeCauldronBlockEntity.FLUID_RECIPE_STATES);
        return states.computeIfAbsent(cauldron.worldPosition.asLong(), ignored ->
            new FluidRecipeState(cauldron.fluids.copyFluids()));
    }

    private static void commitFluidRecipes(InWorldRecipeContext context) {
        Map<Long, FluidRecipeState> states = context.computeIfAbsent(LargeCauldronBlockEntity.FLUID_RECIPE_STATES);
        for (Map.Entry<Long, FluidRecipeState> entry : states.entrySet()) {
            BlockEntity entity = context.getLevel().getBlockEntity(BlockPos.of(entry.getKey()));
            if (entity instanceof LargeCauldronBlockEntity cauldron) {
                cauldron.getMainPart().fluids.setFluids(entry.getValue().fluids);
            }
        }
    }

    private static boolean applyFluidPredicate(List<FluidStack> fluids, HasCauldron predicate, boolean simulate) {
        int source = findSourceTank(fluids, predicate);
        if (predicate.hasCheck() && source < 0) return false;
        int sourceAmount = source < 0 ? 0 : fluids.get(source).getAmount();
        if (predicate.consume() > sourceAmount) return false;

        if (simulate) {
            if (predicate.transforms().isEmpty()) {
                return true;
            }
            // 消耗和产出仅替换列表元素，模拟时可以共享各层的 FluidStack。
            fluids = new ArrayList<>(fluids);
        }
        if (source >= 0 && predicate.consume() > 0) {
            int remaining = sourceAmount - predicate.consume();
            fluids.set(source, remaining == 0 ? FluidStack.EMPTY : fluids.get(source).copyWithAmount(remaining));
        }
        for (var template : predicate.transforms()) {
            FluidStack transform = template.create();
            int target = findTank(fluids, transform);
            int targetAmount = target < 0 ? 0 : fluids.get(target).getAmount();
            if (targetAmount + transform.getAmount() > LargeCauldronFluidHandler.tankCapacity()) return false;
            if (target < 0) target = findEmptyTank(fluids);
            if (target < 0) return false;
            FluidStack produced = transform.copyWithAmount(targetAmount + transform.getAmount());
            fluids.set(target, produced);
        }
        return true;
    }

    private static int findSourceTank(List<FluidStack> fluids, HasCauldron predicate) {
        if (predicate.requiresEmptyCauldron()) {
            for (FluidStack fluid : fluids) {
                if (!fluid.isEmpty()) return -1;
            }
            return 0;
        }
        if (!predicate.hasCheck()) return -1;
        for (int i = 0; i < fluids.size(); i++) {
            FluidStack fluid = fluids.get(i);
            if (predicate.matchesFluid(fluid)) return i;
        }
        return -1;
    }

    private static int findTank(List<FluidStack> fluids, FluidStack target) {
        for (int i = 0; i < fluids.size(); i++) {
            FluidStack fluid = fluids.get(i);
            if (FluidStack.isSameFluidSameComponents(fluid, target)) return i;
        }
        return -1;
    }

    private static int findEmptyTank(List<FluidStack> fluids) {
        for (int i = 0; i < fluids.size(); i++) {
            if (fluids.get(i).isEmpty()) return i;
        }
        return -1;
    }

    private enum RecipePass {
        ALL,
        NON_COMPRESSION,
        COMPRESSION_ONLY;

        private boolean accepts(InWorldRecipe recipe) {
            boolean compression = recipe instanceof ItemCompressRecipe;
            return switch (this) {
                case ALL -> true;
                case NON_COMPRESSION -> !compression;
                case COMPRESSION_ONLY -> compression;
            };
        }
    }

    private record RecipeExecution(boolean executed, boolean damageAnvil) {
        private static final RecipeExecution EMPTY = new RecipeExecution(false, false);
    }

    /** 一次落砧加工中从各物品槽和液层取用的原料，用于在原料所在行标注加工类型。 */
    public record RecipePreview(
        Identifier recipeId,
        String categoryPath,
        Map<Integer, Integer> inputCounts,
        Map<Integer, Integer> outputCounts,
        Map<FluidResource, Integer> fluidAmounts
    ) {
        static RecipePreview of(
            RecipeHolder<InWorldRecipe> holder, boolean fromOutput, Map<Integer, Integer> counts, Map<FluidResource, Integer> amounts
        ) {
            var recipeId = holder.id().identifier();
            var typeId = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
            String category = typeId == null ? "in_world_recipe" : typeId.getPath();
            if (category.equals("in_world_recipe")) {
                String path = recipeId.getPath();
                int separator = path.indexOf('/');
                category = separator < 0 ? path : path.substring(0, separator);
            }
            return new RecipePreview(recipeId, category, fromOutput ? Map.of() : Map.copyOf(counts),
                fromOutput ? Map.copyOf(counts) : Map.of(), Map.copyOf(amounts));
        }
    }

    private final class PreviewState implements CauldronPreviewMatcher.State<PreviewState> {
        private final Vec3 contextPos;
        private final List<ItemStack> items;
        private final int primarySlot;
        private boolean usedPrimarySlot;
        private final List<FluidStack> fluids;
        private final Map<Integer, Integer> consumed = new HashMap<>();
        private final Map<FluidResource, Integer> fluidAmounts = new HashMap<>();

        private PreviewState(List<ItemStack> items, int primarySlot, List<FluidStack> fluids, Vec3 contextPos) {
            this.contextPos = contextPos;
            // 每次模拟只替换列表中的栈，候选配方和回溯分支可以共享尚未消耗的物品与流体栈。
            this.items = new ArrayList<>(items);
            this.primarySlot = primarySlot;
            this.fluids = new ArrayList<>(fluids);
        }

        @Override
        public PreviewState copy() {
            var state = new PreviewState(this.items, this.primarySlot, this.fluids, this.contextPos);
            state.usedPrimarySlot = this.usedPrimarySlot;
            state.consumed.putAll(this.consumed);
            state.fluidAmounts.putAll(this.fluidAmounts);
            return state;
        }

        @Override
        public boolean apply(IRecipePredicate<?> predicate) {
            return LargeCauldronBlockEntity.this.applyPreviewPredicate(predicate, this, this.contextPos);
        }

        private boolean reserve(HasItemIngredient ingredient) {
            int needed = ingredient.getItem().count();
            for (int i = 0; i < this.items.size() && needed > 0; i++) {
                int slot = i == 0 ? this.primarySlot : i <= this.primarySlot ? i - 1 : i;
                ItemStack stack = this.items.get(slot);
                if (stack.isEmpty() || !ingredient.getItem().testIgnoreCount(stack)) {
                    continue;
                }
                int count = Math.min(stack.getCount(), needed);
                this.items.set(slot, stack.copyWithCount(stack.getCount() - count));
                if (ingredient.isConsume()) {
                    this.consumed.merge(slot, count, Integer::sum);
                }
                this.usedPrimarySlot |= slot == this.primarySlot;
                needed -= count;
            }
            return needed == 0;
        }
    }

    private static class FluidRecipeState {
        private List<FluidStack> fluids;
        private final Deque<List<FluidStack>> rollback = new ArrayDeque<>();

        private FluidRecipeState(List<FluidStack> fluids) {
            this.fluids = fluids;
        }
    }

    private static ItemStack getStack(ResourceHandler<ItemResource> handler, int slot) {
        ItemResource resource = handler.getResource(slot);
        return resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(handler.getAmountAsInt(slot));
    }

    private static ItemStack insertItem(ResourceHandler<ItemResource> handler, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = handler.insert(ItemResource.of(stack), stack.getCount(), transaction);
            if (inserted > 0) transaction.commit();
            return inserted == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
        }
    }

    private static ItemStack insertItem(ResourceHandler<ItemResource> handler, int slot, ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = handler.insert(slot, ItemResource.of(stack), stack.getCount(), transaction);
            if (inserted > 0) transaction.commit();
            return inserted == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - inserted);
        }
    }

    private static boolean canInsertItem(ResourceHandler<ItemResource> handler, ItemStack stack) {
        if (stack.isEmpty()) return true;
        try (Transaction transaction = Transaction.openRoot()) {
            return handler.insert(ItemResource.of(stack), stack.getCount(), transaction) == stack.getCount();
        }
    }

    private static ItemStack extractItem(ResourceHandler<ItemResource> handler, int slot, int amount) {
        ItemResource resource = handler.getResource(slot);
        if (resource.isEmpty()) return ItemStack.EMPTY;
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = handler.extract(slot, resource, amount, transaction);
            if (extracted > 0) transaction.commit();
            return extracted == 0 ? ItemStack.EMPTY : resource.toStack(extracted);
        }
    }

    private static int insertFluid(ResourceHandler<FluidResource> handler, FluidStack stack) {
        if (stack.isEmpty()) return 0;
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = handler.insert(FluidResource.of(stack), stack.getAmount(), transaction);
            if (inserted > 0) transaction.commit();
            return inserted;
        }
    }

    private static int extractFluid(ResourceHandler<FluidResource> handler, FluidStack stack, int amount) {
        if (stack.isEmpty()) return 0;
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = handler.extract(FluidResource.of(stack), amount, transaction);
            if (extracted > 0) transaction.commit();
            return extracted;
        }
    }

    private class SelectedInputHandler implements ResourceHandler<ItemResource> {
        private ResourceHandler<ItemResource> delegate() {
            return LargeCauldronBlockEntity.this.processingOutput
                ? LargeCauldronBlockEntity.this.output
                : LargeCauldronBlockEntity.this.input;
        }

        @Override
        public int size() {
            return this.delegate().size();
        }

        private int slot(int viewSlot) {
            int selected = LargeCauldronBlockEntity.this.processingSlot;
            if (selected < 0 || selected >= this.size()) return viewSlot;
            if (viewSlot == 0) return selected;
            int unselected = viewSlot - 1;
            return unselected >= selected ? unselected + 1 : unselected;
        }

        @Override
        public ItemResource getResource(int index) {
            return this.delegate().getResource(this.slot(index));
        }

        @Override
        public long getAmountAsLong(int index) {
            return this.delegate().getAmountAsLong(this.slot(index));
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return this.delegate().getCapacityAsLong(this.slot(index), resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return this.delegate().isValid(this.slot(index), resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return this.delegate().insert(this.slot(index), resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return this.delegate().extract(this.slot(index), resource, amount, transaction);
        }
    }

    private static class PreferredInputHandler implements ResourceHandler<ItemResource> {
        private final LargeCauldronInputHandler delegate;
        private final int[] order;
        private final int extractSlot;

        private PreferredInputHandler(LargeCauldronInputHandler delegate, int preferred) {
            this(delegate, preferred, false);
        }

        private PreferredInputHandler(LargeCauldronInputHandler delegate, int preferred, boolean extractPreferred) {
            this.delegate = delegate;
            this.extractSlot = extractPreferred ? preferred : -1;
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < delegate.size(); i++) slots.add(i);
            slots.sort(Comparator
                .comparingInt((Integer slot) -> PreferredInputHandler.distanceFromPreferred(slot, preferred))
                .thenComparingInt(Integer::intValue));
            this.order = slots.stream().mapToInt(Integer::intValue).toArray();
        }

        private static int distanceFromPreferred(int slot, int preferred) {
            int preferredX = preferred < 0 ? 0 : LargeCauldronBlockEntity.INPUT_SLOT_OFFSETS[preferred][0];
            int preferredZ = preferred < 0 ? 0 : LargeCauldronBlockEntity.INPUT_SLOT_OFFSETS[preferred][1];
            int dx = LargeCauldronBlockEntity.INPUT_SLOT_OFFSETS[slot][0] - preferredX;
            int dz = LargeCauldronBlockEntity.INPUT_SLOT_OFFSETS[slot][1] - preferredZ;
            return dx * dx + dz * dz;
        }

        @Override
        public int size() {
            return this.order.length;
        }

        @Override
        public ItemResource getResource(int index) {
            return this.delegate.getResource(this.order[index]);
        }

        @Override
        public long getAmountAsLong(int index) {
            return this.delegate.getAmountAsLong(this.order[index]);
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return this.delegate.getCapacityAsLong(this.order[index], resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return this.delegate.isValid(this.order[index], resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return this.delegate.insert(this.order[index], resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            int delegateSlot = this.order[index];
            return delegateSlot == this.extractSlot
                ? this.delegate.extract(delegateSlot, resource, amount, transaction)
                : 0;
        }
    }

    private record OutputOnlyHandler(ResourceHandler<ItemResource> delegate) implements ResourceHandler<ItemResource> {
        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public ItemResource getResource(int index) {
            return this.delegate.getResource(index);
        }

        @Override
        public long getAmountAsLong(int index) {
            return this.delegate.getAmountAsLong(index);
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return this.delegate.getCapacityAsLong(index, resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return false;
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            return this.delegate.extract(index, resource, amount, transaction);
        }
    }

    private static class EmptyItemHandler implements ResourceHandler<ItemResource> {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public ItemResource getResource(int index) {
            return ItemResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            return 0;
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return 0;
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            return false;
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            return 0;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            return 0;
        }
    }
}
